package pitheguy.countycolor.gui.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import pitheguy.countycolor.coloring.CountyCompletionData;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.gui.components.InfoTooltip;
import pitheguy.countycolor.metadata.CountyData;
import pitheguy.countycolor.options.Options;
import pitheguy.countycolor.render.Zoom;
import pitheguy.countycolor.render.renderer.StateRenderer;
import pitheguy.countycolor.render.util.*;
import pitheguy.countycolor.util.InputManager;
import pitheguy.countycolor.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class StateScreen implements Screen, InputProcessor {
    private final Game game;
    private final String state;
    private final CountryScreen countryScreen;
    private final OrthographicCamera camera;
    private final StateRenderer renderer;
    private final CameraTransitionHelper transitionHelper;
    private final Skin skin = new Skin(Gdx.files.internal("skin/skin.json"));
    private float maxZoom;
    private CountyCompletionData countyCompletionData;
    private final Future<CountyCompletionData> countyDataFuture;
    private final Stage stage;
    private boolean pendingColorSelection;
    private CountyData.County pendingCounty;
    private final InfoTooltip infoTooltip = new InfoTooltip(skin, true);
    private Texture arrowTexture;
    private Button backButton;

    public StateScreen(Game game, String state) {
        this(game, state, new CountryScreen(game));
    }

    public StateScreen(Game game, String state, CountryScreen countryScreen) {
        this.game = game;
        this.state = state;
        this.countryScreen = countryScreen;
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        maxZoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = maxZoom;
        camera.update();
        transitionHelper = new CameraTransitionHelper(game, camera);
        renderer = new StateRenderer(state, () -> camera.zoom == maxZoom, () -> camera.zoom == maxZoom, countryScreen.getCompletedCounties());
        countyDataFuture = CountyCompletionData.loadAsync(state);
        stage = new Stage(new ScreenViewport());
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
        renderer.renderState(camera, countyCompletionData);
        infoTooltip.hide();
        if (pendingCounty == null) {
            CountyData.County hoveringCounty = renderer.getCountyAtCoords(RenderUtil.getMouseWorldCoords(camera));
            if (hoveringCounty != null) {
                String displayName = hoveringCounty.isIndependentCity() && !hoveringCounty.getName().endsWith("*") ? hoveringCounty.getName() + "*" : hoveringCounty.getName();
                String completionText = countyCompletionData.get(hoveringCounty.getName()).getCompletionString();
                String text = (hoveringCounty.isIndependentCity() ? "*Independent City\n" : "") + completionText;
                infoTooltip.show(stage, displayName, text);
            }
        }
        updateCursor();
        stage.act(delta);
        stage.draw();
        if (pendingColorSelection && !transitionHelper.isInTransition()) {
            showColorSelection();
            pendingColorSelection = false;
        }
    }

    private void updateCursor() {
        if (pendingCounty != null || camera.zoom != maxZoom) return;
        CountyData.County hoveringCounty = renderer.getCountyAtCoords(RenderUtil.getMouseWorldCoords(camera));
        if (hoveringCounty == null || countyCompletionData.get(hoveringCounty.getName()).isCompleted())
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        else Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
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
        if (pendingCounty == null) {
            countryScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            countryScreen.zoomOutFromState(state);
            game.setScreen(countryScreen);
        } else {
            float targetZoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            resetStage();
            pendingCounty = null;
            pendingColorSelection = false;
            transitionHelper.stopTransition();
            transitionHelper.transition(new Vector2(0, 0), targetZoom, null, false);
        }
    }

    private void showColorSelection() {
        CountyColorScreen nextScreen = new CountyColorScreen(game, pendingCounty, false);
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
            CountyCompletionData.Entry entry = countyCompletionData.get(county);
            if (!entry.isStarted()) continue;
            colors.remove(entry.mapColor());
        }
        return colors.isEmpty() ? List.of(MapColor.values()) : colors;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        CountyData.County selectedCounty = renderer.getCountyAtCoords(RenderUtil.getMouseWorldCoords(camera));
        if (selectedCounty == null || countyCompletionData.get(selectedCounty.getName()).isCompleted()) return false;
        if (selectedCounty.equals(pendingCounty)) return false;
        Zoom zoom = renderer.getTargetZoom(selectedCounty.getPolygons());
        resetStage();
        if (countyCompletionData.get(selectedCounty.getName()).isStarted()) {
            CountyColorScreen targetScreen = new CountyColorScreen(game, selectedCounty, true);
            transitionHelper.transition(zoom.center(), zoom.zoom(), targetScreen, false);
        } else {
            transitionHelper.transition(zoom.center(), zoom.zoom(), null, false);
            pendingColorSelection = true;
            pendingCounty = selectedCounty;
        }
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        return true;
    }

    private void ensureLoadingFinished() {
        if (countyCompletionData == null) countyCompletionData = Util.getFutureValue(countyDataFuture);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        maxZoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = maxZoom;
        camera.update();
        stage.getViewport().update(width, height, true);
        renderer.invalidateCache();
        backButton.setPosition(0, Gdx.graphics.getHeight() - backButton.getHeight());
    }

    @Override
    public void show() {
        InputManager.setInputProcessor(new InputMultiplexer(stage, this));
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public boolean keyDown(int keycode) { return false; }
    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }

}
