package pitheguy.countycolor.gui;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.render.Zoom;
import pitheguy.countycolor.render.renderer.StateRenderer;
import pitheguy.countycolor.render.util.CameraTransitionHelper;
import pitheguy.countycolor.render.util.RenderConst;

import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class StateScreen implements Screen, InputProcessor {
    private final Game game;
    private final String state;
    private final OrthographicCamera camera;
    private final StateRenderer renderer;
    private final CameraTransitionHelper transitionHelper;
    private final Skin skin = new Skin(Gdx.files.internal("skin.json"));
    private CountyData countyData;
    private final Future<CountyData> countyDataFuture;
    private final Stage stage;
    private boolean pendingColorSelection;
    private String pendingCounty;

    public StateScreen(Game game, String state) {
        this.game = game;
        this.state = state;
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();
        renderer = new StateRenderer(state);
        transitionHelper = new CameraTransitionHelper(game, camera);
        countyDataFuture = loadCountyData();
        stage = new Stage();
        resetStage();
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, this));
    }

    @Override
    public void dispose() {
        renderer.dispose();
        stage.dispose();
        skin.dispose();
    }

    @Override
    public void render(float delta) {
        ensureLoadingFinished();
        transitionHelper.update(delta);
        renderer.renderState(camera, countyData.completed());
        stage.act(delta);
        stage.draw();
        if (pendingColorSelection && !transitionHelper.isInTransition()) {
            showColorSelection();
            pendingColorSelection = false;
        }
    }

    private void resetStage() {
        stage.clear();
        TextButton button = new TextButton("Back", skin);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
               goBack();
            }
        });
        button.setPosition(0, Gdx.graphics.getHeight() - button.getHeight());
        stage.addActor(button);
    }

    private void goBack() {
        if (pendingCounty == null) game.setScreen(new CountryScreen(game));
        else {
            float targetZoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            resetStage();
            pendingCounty = null;
            transitionHelper.stopTransition();
            transitionHelper.transition(new Vector2(0, 0), targetZoom, null);
        }
    }

    private void showColorSelection() {
        CountyColorScreen nextScreen = new CountyColorScreen(game, pendingCounty, StateRenderer.getIdForState(state), false);
        resetStage();
        Group group = new Group();
        Label label = new Label("Choose a color", skin);
        label.setPosition(100 - label.getWidth() / 2, 50);
        group.addActor(label);
        for (int i = 0; i < 4; i++) {
            MapColor color = MapColor.values()[i];
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
        group.setPosition(Gdx.graphics.getWidth() / 2f - 100, Gdx.graphics.getHeight() / 2f - 50);
        stage.addActor(group);
    }

    private Vector3 getMouseWorldCoords() {
        return camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        Vector3 mouseWorld = getMouseWorldCoords();
        String selectedCounty = renderer.getCountyAtCoords(new Vector2(mouseWorld.x, mouseWorld.y));
        if (selectedCounty.isEmpty() || countyData.completed().containsKey(selectedCounty)) return false;
        Zoom zoom = renderer.getTargetZoom(selectedCounty);
        resetStage();
        if (countyData.started().contains(selectedCounty)) {
            CountyColorScreen targetScreen = new CountyColorScreen(game, selectedCounty, StateRenderer.getIdForState(state), true);
            transitionHelper.transition(zoom.center(), zoom.zoom(), targetScreen);
        } else {
            transitionHelper.transition(zoom.center(), zoom.zoom(), null);
            pendingColorSelection = true;
            pendingCounty = selectedCounty;
        }
        return true;
    }

    private Future<CountyData> loadCountyData() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        return executor.submit(() -> {
            FileHandle dataHandle = Gdx.files.local("data/" + state + ".json");
            if (!dataHandle.exists()) return CountyData.EMPTY;

            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(dataHandle);
            Map<String, MapColor> completed = new HashMap<>();
            List<String> started = new ArrayList<>();
            for (JsonValue county = root.child; county != null; county = county.next) {
                String countyName = county.name;
                if (county.getBoolean("complete", false)) {
                    String colorName = county.getString("color");
                    completed.put(countyName, MapColor.fromSerializedName(colorName));
                } else started.add(countyName);
            }

            return new CountyData(completed, started);
        });
    }

    private void ensureLoadingFinished() {
        if (countyData == null) {
            try {
                countyData = countyDataFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.zoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void show() {}
    @Override public void hide() {}
    @Override public boolean keyDown(int keycode) { return false; }
    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }

    private record CountyData(Map<String, MapColor> completed, List<String> started) {
        public static final CountyData EMPTY = new CountyData(Map.of(), List.of());
    }
}
