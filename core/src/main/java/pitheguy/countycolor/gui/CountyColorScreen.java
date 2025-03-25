package pitheguy.countycolor.gui;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.google.gson.*;
import pitheguy.countycolor.coloring.ColoringGrid;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.render.renderer.ColoringRenderer;
import pitheguy.countycolor.render.renderer.CountyRenderer;
import pitheguy.countycolor.render.util.CameraTransitionHelper;
import pitheguy.countycolor.render.util.RenderConst;
import pitheguy.countycolor.util.GsonUtil;
import pitheguy.countycolor.util.Util;

import java.util.Base64;
import java.util.BitSet;

public class CountyColorScreen implements Screen, InputProcessor {
    private static final float COMPLETION_PERCENTAGE = 0.99f;
    private final Game game;
    private final String county;
    private final String stateId;
    private final OrthographicCamera camera;
    private final ShapeRenderer cursorRenderer = new ShapeRenderer();
    private final CountyRenderer countyRenderer;
    private final ColoringRenderer coloringRenderer = new ColoringRenderer();
    private final BitmapFont font = new BitmapFont();
    private final SpriteBatch batch = new SpriteBatch();
    private final CameraTransitionHelper transitionHelper;
    private final Vector2 lastDrag = new Vector2();
    private final Vector2 lastColor = new Vector2();
    private boolean dragging = false;
    private boolean coloring = false;
    private float brushSize = 5;
    private final ColoringGrid coloringGrid;
    private int totalPixels = -1;
    private float timeSinceSave = 0f;
    private boolean inTransition = false;

    public CountyColorScreen(Game game, String county, String stateId) {
        this.game = game;
        this.county = county;
        this.stateId = stateId;
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = 1;
        transitionHelper = new CameraTransitionHelper(game, camera);
        Gdx.input.setInputProcessor(this);
        coloringGrid = load();
        countyRenderer = new CountyRenderer(county, stateId);
    }

    public CountyColorScreen(Game game, String county, String stateId, MapColor color) {
        this.game = game;
        this.county = county;
        this.stateId = stateId;
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = 1;
        transitionHelper = new CameraTransitionHelper(game, camera);
        Gdx.input.setInputProcessor(this);
        coloringGrid = new ColoringGrid(color);
        countyRenderer = new CountyRenderer(county, stateId);
    }

    @Override
    public void render(float delta) {
        if (totalPixels == -1)
            totalPixels = (int) (countyRenderer.computeTotalGridSquares() * COMPLETION_PERCENTAGE);
        camera.update();
        transitionHelper.update(delta);
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (inTransition) {
            countyRenderer.renderCountyFilled(camera, 1, coloringGrid.getColor());
            return;
        }
        coloringRenderer.render(coloringGrid, camera);
        countyRenderer.renderCounty(camera);
        cursorRenderer.begin(ShapeRenderer.ShapeType.Filled);
        cursorRenderer.setColor(canColor() ? coloringGrid.getColor().getColor() : Color.RED);
        cursorRenderer.circle(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), brushSize / camera.zoom);
        cursorRenderer.end();
        batch.begin();
        font.draw(batch, getProgressString(), 10, Gdx.graphics.getHeight() - 10);
        font.draw(batch, Gdx.graphics.getFramesPerSecond() + " FPS", Gdx.graphics.getWidth() - 100, Gdx.graphics.getHeight() - 10);
        batch.end();
        timeSinceSave += delta;
        if (timeSinceSave > 10f) {
            saveAsync();
            timeSinceSave = 0;
        }
        if (getCompletion() == 1) {
            save();
            inTransition = true;
            transitionHelper.transition(new Vector2(0, 0), 2f, new CountyCompleteScreen(game, county, stateId, coloringGrid.getColor()));
        }
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (dragging) {
            Vector2 current = new Vector2(screenX, screenY);
            Vector2 delta = current.cpy().sub(lastDrag);
            Vector3 worldDelta = camera.unproject(new Vector3(0, 0, 0))
                .sub(camera.unproject(new Vector3(delta.x, delta.y, 0)));
            camera.position.add(worldDelta.x, worldDelta.y, 0);
            lastDrag.set(screenX, screenY);
            return true;
        } else if (coloring && canColor()) {
            Vector3 lastColorWorld = camera.unproject(new Vector3(lastColor.x, lastColor.y, 0));
            applyBrush(lastColorWorld);
            lastColor.set(screenX, screenY);
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.RIGHT) {
            lastDrag.set(screenX, screenY);
            dragging = true;
        } else if (button == Input.Buttons.LEFT) {
            lastColor.set(screenX, screenY);
            coloring = true;
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        dragging = false;
        coloring = false;
        return true;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        Vector3 mouseWorldBefore = getMouseWorldCoords();
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            float zoomFactor = (amountY < 0) ? 0.9f : 1.1f;
            camera.zoom = MathUtils.clamp(camera.zoom * zoomFactor, 0.1f, 1f);
            camera.update();
            Vector3 mouseWorldAfter = getMouseWorldCoords();
            Vector3 delta = mouseWorldBefore.sub(mouseWorldAfter);
            camera.position.add(delta.x, delta.y, 0);
        } else brushSize = MathUtils.clamp(brushSize - amountY, 1, 75);
        return true;
    }

    private Vector3 getMouseWorldCoords() {
        return camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
    }

    private void applyBrush(Vector3 lastColor) {
        Vector3 mouse = getMouseWorldCoords();
        Vector2 currentPos = new Vector2(lastColor.x, lastColor.y);
        int steps = brushSize > 30 ? 1 : 5;
        Vector2 endPos = new Vector2(mouse.x, mouse.y);
        Vector2 delta = endPos.cpy().sub(currentPos).scl(1f / steps);
        for (int i = 0; i < steps; i++) {
            currentPos.add(delta);
            if (canColor(currentPos)) applyBrush0(currentPos);
        }
    }

    private void applyBrush0(Vector2 pos) {
        int effectiveBrushSize = (int) (brushSize * RenderConst.COLORING_RESOLUTION);
        int startX = (int) (pos.x * RenderConst.COLORING_RESOLUTION - effectiveBrushSize);
        int startY = (int) (pos.y * RenderConst.COLORING_RESOLUTION - effectiveBrushSize);
        int endX = (int) (pos.x * RenderConst.COLORING_RESOLUTION + effectiveBrushSize);
        int endY = (int) (pos.y * RenderConst.COLORING_RESOLUTION + effectiveBrushSize);
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                float dx = pos.x * RenderConst.COLORING_RESOLUTION - x;
                float dy = pos.y * RenderConst.COLORING_RESOLUTION - y;
                if (dx * dx + dy * dy < effectiveBrushSize * effectiveBrushSize) {
                    int indexX = x + RenderConst.COLORING_SIZE / 2;
                    int indexY = y + RenderConst.COLORING_SIZE / 2;
                    coloringGrid.set(indexX, indexY);
                }
            }
        }
    }

    private boolean canColor() {
        Vector3 mouseWorldVec3 = getMouseWorldCoords();
        Vector2 center = new Vector2(mouseWorldVec3.x, mouseWorldVec3.y);
        return canColor(center);
    }

    private boolean canColor(Vector2 pos) {
        int numPoints = 32;
        for (int i = 0; i < numPoints; i++) {
            float angle = (float)(i * Math.PI * 2 / numPoints); // 0 to 2π
            float dx = brushSize * (float)Math.cos(angle);
            float dy = brushSize * (float)Math.sin(angle);
            Vector2 offsetPoint = new Vector2(pos.x + dx, pos.y + dy);
            if (!countyRenderer.isCoordinateWithinCounty(offsetPoint, RenderConst.RENDER_SIZE)) return false;
        }
        return true;
    }

    private String getProgressString() {
        double percent = getCompletion() * 100;
        return String.format("Progress: %d / %d (%.2f%%)", Math.min(coloringGrid.coloredPoints(), totalPixels), totalPixels, percent);
    }

    private double getCompletion() {
        return Math.min((double) coloringGrid.coloredPoints() / totalPixels, 1);
    }

    @Override
    public void dispose() {
        cursorRenderer.dispose();
        countyRenderer.dispose();
        coloringRenderer.dispose();
        batch.dispose();
        font.dispose();
    }

    private void saveAsync() {
        new Thread(this::save).start();
    }

    private void save() {
        if (coloringGrid.coloredPoints() == 0) return;
        FileHandle dataHandle = Gdx.files.local("data.json");
        JsonObject json = dataHandle.exists() ? JsonParser.parseReader(dataHandle.reader()).getAsJsonObject() : new JsonObject();
        JsonObject state = GsonUtil.getOrCreateChild(json, stateId);
        JsonObject county = GsonUtil.getOrCreateChild(state, this.county);
        county.addProperty("color", coloringGrid.getColor().getSerializedName());
        if (getCompletion() < 1)
            county.addProperty("coloredPoints", Base64.getEncoder().encodeToString(Util.compress(coloringGrid.asBitSet().toByteArray())));
        else {
            if (county.has("coloredPoints")) county.remove("coloredPoints");
            county.addProperty("complete", true);
        }
        dataHandle.writeString(json.toString(), false);
    }

    private ColoringGrid load() {
        FileHandle dataHandle = Gdx.files.local("data.json");
        if (!dataHandle.exists()) throw new IllegalStateException("No saved data for county");
        JsonObject json = JsonParser.parseReader(dataHandle.reader()).getAsJsonObject();
        if (!json.has(stateId)) throw new IllegalStateException("No saved data for county");
        JsonObject state = json.get(stateId).getAsJsonObject();
        if (!state.has(county)) throw new IllegalStateException("No saved data for county");
        JsonObject county = state.get(this.county).getAsJsonObject();
        if (county.has("complete") && county.get("complete").getAsBoolean())
            throw new IllegalStateException("Tried to load an already completed county");
        return ColoringGrid.fromJson(county);
    }

    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void show() {}
    @Override public void hide() {}
    @Override public boolean keyDown(int keycode) { return false; }
    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
}
