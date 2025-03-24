package pitheguy.countycolor.gui;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pitheguy.countycolor.CountyColor;
import pitheguy.countycolor.render.ColoringGrid;
import pitheguy.countycolor.render.Zoom;
import pitheguy.countycolor.render.renderer.StateRenderer;
import pitheguy.countycolor.render.util.CameraTransitionHelper;
import pitheguy.countycolor.util.Util;

import java.util.*;

public class StateScreen implements Screen, InputProcessor {
    private final Game game;
    private final String state;
    private final OrthographicCamera camera;
    private final StateRenderer renderer;
    private final CameraTransitionHelper transitionHelper;
    private final List<String> completedCounties;

    public StateScreen(Game game, String state) {
        this.game = game;
        this.state = state;
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = 1;
        renderer = new StateRenderer(state);
        transitionHelper = new CameraTransitionHelper(game, camera);
        completedCounties = loadCompletedCounties();
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void dispose() {
        renderer.dispose();
    }

    @Override
    public void render(float delta) {
        transitionHelper.update(delta);
        renderer.renderState(camera, completedCounties);
    }

    private Vector3 getMouseWorldCoords() {
        return camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        Vector3 mouseWorld = getMouseWorldCoords();
        String selectedCounty = renderer.getCountyAtCoords(new Vector2(mouseWorld.x, mouseWorld.y));
        if (selectedCounty.isEmpty() || completedCounties.contains(selectedCounty)) return false;
        Zoom zoom = renderer.getTargetZoom(selectedCounty);
        transitionHelper.transition(zoom.center(), zoom.zoom(), new CountyColorScreen(game, selectedCounty, StateRenderer.getIdForState(state)));
        return true;
    }

    private List<String> loadCompletedCounties() {
        FileHandle dataHandle = Gdx.files.local("data.json");
        if (!dataHandle.exists()) return List.of();
        JsonObject json = JsonParser.parseReader(dataHandle.reader()).getAsJsonObject();
        String stateId = StateRenderer.getIdForState(state);
        if (!json.has(stateId)) return List.of();
        JsonObject state = json.get(stateId).getAsJsonObject();
        List<String> completedCounties = new ArrayList<>();
        for (String county : state.keySet()) {
            JsonObject countyJson = state.get(county).getAsJsonObject();
            if (countyJson.has("complete") && countyJson.get("complete").getAsBoolean()) completedCounties.add(county);
        }
        return completedCounties;
    }

    @Override public void resize(int width, int height) {}
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
}
