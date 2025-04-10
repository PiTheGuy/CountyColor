package pitheguy.countycolor.gui.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.gui.components.InfoTooltip;
import pitheguy.countycolor.render.Zoom;
import pitheguy.countycolor.render.renderer.CountryCompletedCountiesRenderer;
import pitheguy.countycolor.render.renderer.CountryRenderer;
import pitheguy.countycolor.render.util.*;
import pitheguy.countycolor.util.InputManager;
import pitheguy.countycolor.util.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class CountryScreen implements Screen, InputProcessor {
    private final Game game;
    private final OrthographicCamera camera;
    private final OrthographicCamera hudCamera;
    private final CountryRenderer renderer;
    private final CountryCompletedCountiesRenderer completedCountiesRenderer;
    private final CameraTransitionHelper transitionHelper;
    private final BitmapFont font = new BitmapFont();
    private final SpriteBatch batch = new SpriteBatch();
    private final Stage stage;
    private final InfoTooltip tooltip = new InfoTooltip(new Skin(Gdx.files.internal("skin/skin.json")), true);
    private final Future<Map<String, Map<String, MapColor>>> completedCounties = loadCompletedCounties();
    public static final Map<String, Integer> COUNTY_COUNTS = new HashMap<>();
    static {
        loadCountyCounts();
    }

    public CountryScreen(Game game) {
        this.game = game;
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float startZoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight() * 2);
        camera.zoom = startZoom;
        camera.update();
        hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        renderer = new CountryRenderer();
        transitionHelper = new CameraTransitionHelper(game, camera);
        completedCountiesRenderer = new CountryCompletedCountiesRenderer(() -> camera.zoom > startZoom / 2);
        Viewport viewport = new ScreenViewport(hudCamera);
        stage = new Stage(viewport);
    }

    @Override
    public void dispose() {
        renderer.dispose();
        completedCountiesRenderer.dispose();
        font.dispose();
        batch.dispose();
        stage.dispose();
        tooltip.getSkin().dispose();
    }

    @Override
    public void render(float delta) {
        transitionHelper.update(delta);
        completedCountiesRenderer.render(camera, completedCounties);
        renderer.renderCountry(camera);
        tooltip.hide();
        String selectedState = renderer.getSubregionAtCoords(RenderUtil.getMouseWorldCoords(camera));
        if (selectedState != null && !transitionHelper.isInTransition())
            tooltip.show(stage, selectedState, getCompletionCountString(selectedState));
        stage.act(delta);
        stage.draw();
    }

    private String getCompletionCountString(String state) {
        if (!completedCounties.isDone()) return "Loading...";
        int completed = Util.getFutureValue(completedCounties).getOrDefault(state, Map.of()).size();
        int total = COUNTY_COUNTS.get(state);
        return String.format("%d / %d Counties Completed", completed, total);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        String selectedState = renderer.getSubregionAtCoords(RenderUtil.getMouseWorldCoords(camera));
        if (selectedState == null) return false;
        Zoom zoom = renderer.getTargetZoom(selectedState);
        transitionHelper.transition(zoom.center(), zoom.zoom(), new StateScreen(game, selectedState), false);
        return true;
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.zoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight() * 2);
        camera.update();
        hudCamera.setToOrtho(false, width, height);
        hudCamera.update();
        stage.getViewport().update(width, height, true);
        completedCountiesRenderer.invalidateCache();
    }

    private static void loadCountyCounts() {
        String[] mappings = Gdx.files.internal("metadata/county_counts.txt").readString().split("\n");
        for (String mapping : mappings) {
            String[] parts = mapping.split("=");
            COUNTY_COUNTS.put(parts[0], Integer.parseInt(parts[1]));
        }
    }

    private Future<Map<String, Map<String, MapColor>>> loadCompletedCounties() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        return executor.submit(() -> {
            FileHandle handle = Gdx.files.internal("data/completed_counties.json");
            if (!handle.exists()) return Map.of();
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(handle);
            Map<String, Map<String, MapColor>> completionCounts = new HashMap<>();
            for (JsonValue state = root.child; state != null; state = state.next) {
                Map<String, MapColor> map = new HashMap<>();
                for (JsonValue county = state.child; county != null; county = county.next)
                    map.put(county.name, MapColor.fromSerializedName(county.asString()));
                completionCounts.put(state.name, map);
            }
            return completionCounts;
        });
    }

    public boolean isRendererReady() {
        return renderer.isDoneLoading() && completedCountiesRenderer.isDoneLoading();
    }

    @Override
    public void show() {
        InputManager.setInputProcessor(this);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.F4) {
            System.out.println(RenderUtil.getMouseWorldCoords(camera));
        }
        return false;
    }
    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }
}
