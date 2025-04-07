package pitheguy.countycolor.gui.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import pitheguy.countycolor.coloring.CountyData;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.gui.components.InfoTooltip;
import pitheguy.countycolor.options.Options;
import pitheguy.countycolor.render.Zoom;
import pitheguy.countycolor.render.renderer.StateRenderer;
import pitheguy.countycolor.render.util.CameraTransitionHelper;
import pitheguy.countycolor.render.util.RenderConst;
import pitheguy.countycolor.util.InputManager;
import pitheguy.countycolor.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class StateScreen implements Screen, InputProcessor {
    private final Game game;
    private final String state;
    private final OrthographicCamera camera;
    private final OrthographicCamera hudCamera;
    private final StateRenderer renderer;
    private final CameraTransitionHelper transitionHelper;
    private final Skin skin = new Skin(Gdx.files.internal("skin/skin.json"));
    private float maxZoom;
    private CountyData countyData;
    private final Future<CountyData> countyDataFuture;
    private final Stage stage;
    private boolean pendingColorSelection;
    private String pendingCounty;
    private final InfoTooltip infoTooltip = new InfoTooltip(skin, true);
    private Texture arrowTexture;
    private Button backButton;

    public StateScreen(Game game, String state) {
        this.game = game;
        this.state = state;
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        maxZoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = maxZoom;
        camera.update();
        hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        transitionHelper = new CameraTransitionHelper(game, camera);
        renderer = new StateRenderer(state, () -> camera.zoom == maxZoom);
        countyDataFuture = CountyData.loadAsync(state);
        Viewport viewport = new ScreenViewport(hudCamera);
        stage = new Stage(viewport);
        resetStage();
    }

    @Override
    public void dispose() {
        renderer.dispose();
        stage.dispose();
        skin.dispose();
        arrowTexture.dispose();
    }

    @Override
    public void render(float delta) {
        ensureLoadingFinished();
        transitionHelper.update(delta);
        renderer.renderState(camera, countyData);
        infoTooltip.hide();
        if (pendingCounty == null) {
            Vector3 mouseWorld = getMouseWorldCoords();
            String hoveringCounty = renderer.getSubregionAtCoords(new Vector2(mouseWorld.x, mouseWorld.y));
            if (hoveringCounty != null)
                infoTooltip.show(stage, hoveringCounty, countyData.get(hoveringCounty).getCompletionString());
        }
        stage.act(delta);
        stage.draw();
        if (pendingColorSelection && !transitionHelper.isInTransition()) {
            showColorSelection();
            pendingColorSelection = false;
        }
    }

    private void resetStage() {
        stage.clear();
        backButton = new Button(skin);
        arrowTexture = new Texture(Gdx.files.internal("icons/back.png"));
        Image arrowImage = new Image(arrowTexture);
        backButton.add(arrowImage);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                goBack();
            }
        });
        backButton.setSize(40, 40);
        backButton.setPosition(0, Gdx.graphics.getHeight() - backButton.getHeight());
        stage.addActor(backButton);
    }

    private void goBack() {
        if (pendingCounty == null) game.setScreen(new CountryScreen(game));
        else {
            float targetZoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            resetStage();
            pendingCounty = null;
            pendingColorSelection = false;
            transitionHelper.stopTransition();
            transitionHelper.transition(new Vector2(0, 0), targetZoom, null);
        }
    }

    private void showColorSelection() {
        CountyColorScreen nextScreen = new CountyColorScreen(game, pendingCounty, state, false);
        resetStage();
        Table table = new Table();
        table.setFillParent(true);
        Label label = new Label("Choose a color", skin);
        table.center().add(label).row();
        List<MapColor> colors = getAvailableColors();
        Group group = new Group();
        group.setSize(50 * colors.size(), 50);
        for (int i = 0; i < colors.size(); i++) {
            MapColor color = colors.get(i);
            Button.ButtonStyle tintedStyle = new Button.ButtonStyle(skin.get("colored", Button.ButtonStyle.class));
            tintedStyle.up = skin.newDrawable(tintedStyle.up, color.getColor());
            tintedStyle.down = skin.newDrawable(tintedStyle.down, color.getColor());
            tintedStyle.over = skin.newDrawable(tintedStyle.over, color.getColor());
            Button button = new Button(tintedStyle);
            button.setSize(50, 50);
            button.setPosition(i * 50, 0);
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    nextScreen.prepare(color);
                    game.setScreen(nextScreen);
                }
            });
            group.addActor(button);
        }
        table.add(group);
        stage.addActor(table);
    }

    private List<MapColor> getAvailableColors() {
        List<MapColor> colors = new ArrayList<>(List.of(MapColor.values()));
        if (!Options.ENFORCE_MAP_COLORS.get()) return colors;
        List<String> borderingCounties = renderer.getBorderingCounties(pendingCounty);
        for (String county : borderingCounties) {
            CountyData.Entry entry = countyData.get(county);
            if (!entry.isStarted()) continue;
            colors.remove(entry.mapColor());
        }
        return colors.isEmpty() ? List.of(MapColor.values()) : colors;
    }

    private Vector3 getMouseWorldCoords() {
        return camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        Vector3 mouseWorld = getMouseWorldCoords();
        String selectedCounty = renderer.getSubregionAtCoords(new Vector2(mouseWorld.x, mouseWorld.y));
        if (selectedCounty == null || countyData.get(selectedCounty).isCompleted()) return false;
        Zoom zoom = renderer.getTargetZoom(selectedCounty);
        resetStage();
        if (countyData.get(selectedCounty).isStarted()) {
            CountyColorScreen targetScreen = new CountyColorScreen(game, selectedCounty, state, true);
            transitionHelper.transition(zoom.center(), zoom.zoom(), targetScreen);
        } else {
            transitionHelper.transition(zoom.center(), zoom.zoom(), null);
            pendingColorSelection = true;
            pendingCounty = selectedCounty;
        }
        return true;
    }

    private void ensureLoadingFinished() {
        if (countyData == null) countyData = Util.getFutureValue(countyDataFuture);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        maxZoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = maxZoom;
        camera.update();
        hudCamera.setToOrtho(false, width, height);
        hudCamera.update();
        stage.getViewport().update(width, height, true);
        renderer.invalidateCache();
        backButton.setPosition(0, Gdx.graphics.getHeight() - backButton.getHeight());

    }

    @Override
    public void show() {
        InputManager.setInputProcessor(new InputMultiplexer(stage, this));
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public boolean keyDown(int keycode) { return false; }
    @Override public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.F4) System.out.println(getMouseWorldCoords());
        return false;
    }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }

}
