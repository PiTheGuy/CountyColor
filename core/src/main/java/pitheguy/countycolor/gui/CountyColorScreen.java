package pitheguy.countycolor.gui;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.*;
import pitheguy.countycolor.coloring.ColoringGrid;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.render.renderer.*;
import pitheguy.countycolor.render.util.CameraTransitionHelper;
import pitheguy.countycolor.render.util.RenderConst;
import pitheguy.countycolor.util.Util;

import java.util.Base64;
import java.util.concurrent.*;

public class CountyColorScreen implements Screen, InputProcessor {
    private static final float PIXEL_COUNT_MULTIPLIER = 1f;
    private final Game game;
    private final String county;
    private final String stateId;
    private final OrthographicCamera camera;
    private final OrthographicCamera hudCamera;
    private final ShapeRenderer cursorRenderer = new ShapeRenderer();
    private final CountyRenderer countyRenderer;
    private final ColoringRenderer coloringRenderer = new ColoringRenderer();
    private final Stage stage = new Stage();
    private final BitmapFont font = new BitmapFont();
    private final SpriteBatch batch = new SpriteBatch();
    private final CameraTransitionHelper transitionHelper;
    private final Vector2 lastDrag = new Vector2();
    private final Vector2 lastColor = new Vector2();
    private boolean dragging = false;
    private boolean coloring = false;
    private float maxZoom;
    private float brushSize = 5;
    private ColoringGrid coloringGrid;
    private Future<ColoringGrid> coloringGridFuture;
    private int totalPixels = -1;
    private float timeSinceSave = 0f;
    private boolean inTransition = false;
    private Slider slider;

    public CountyColorScreen(Game game, String county, String stateId, boolean load) {
        this.game = game;
        this.county = county;
        this.stateId = stateId;
        maxZoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = maxZoom;
        camera.update();
        transitionHelper = new CameraTransitionHelper(game, camera);
        if (load) Gdx.input.setInputProcessor(new InputMultiplexer(stage, this));
        if (load) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            coloringGridFuture = executor.submit(this::load);
        } else coloringGrid = new ColoringGrid();
        countyRenderer = new CountyRenderer(county, stateId);
        initStage();
    }

    private void initStage() {
        stage.clear();
        Skin skin = new Skin(Gdx.files.internal("skin.json"));
        slider = new Slider(1, 75, 1, false, skin);
        slider.setSize(200, 20);
        slider.setPosition(Gdx.graphics.getWidth() / 2f - 100, Gdx.graphics.getHeight() - 30);
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                brushSize = slider.getValue();
            }
        });
        stage.addActor(slider);
    }

    public void prepare(MapColor color) {
        coloringGrid.setColor(color);
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, this));
    }

    @Override
    public void render(float delta) {
        if (totalPixels == -1)
            totalPixels = (int) (countyRenderer.computeTotalGridSquares() * PIXEL_COUNT_MULTIPLIER);
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
        stage.act(delta);
        stage.draw();
        timeSinceSave += delta;
        if (timeSinceSave > 10f) {
            saveAsync();
            timeSinceSave = 0;
        }
        if (getCompletion() == 1) {
            saveAsync();
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
            camera.zoom = MathUtils.clamp(camera.zoom * zoomFactor, 0.1f, maxZoom);
            camera.update();
            Vector3 mouseWorldAfter = getMouseWorldCoords();
            Vector3 delta = mouseWorldBefore.sub(mouseWorldAfter);
            camera.position.add(delta.x, delta.y, 0);
        } else {
            brushSize = MathUtils.clamp(brushSize - amountY, 1, 75);
            slider.setValue(brushSize);
        }
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
            if (canColor(currentPos)) coloringGrid.applyBrush(currentPos, brushSize);
        }
    }

    private boolean canColor() {
        Vector3 mouseWorldVec3 = getMouseWorldCoords();
        Vector2 center = new Vector2(mouseWorldVec3.x, mouseWorldVec3.y);
        return canColor(center);
    }

    private boolean canColor(Vector2 pos) {
        int numPoints = brushSize > 30 ? 64 : 32;
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
        coloringGrid.dispose();
        stage.dispose();
        batch.dispose();
        font.dispose();
    }

    private void saveAsync() {
        new Thread(this::save).start();
    }

    private void save() {
        if (coloringGrid.coloredPoints() == 0) return;

        FileHandle dataHandle = Gdx.files.local("data/" + StateRenderer.getStateFromId(stateId) + ".json");
        JsonReader reader = new JsonReader();
        JsonValue root = dataHandle.exists() ? reader.parse(dataHandle) : new JsonValue(JsonValue.ValueType.object);

        if (root.has(county)) root.remove(county);
        JsonValue countyJson = new JsonValue(JsonValue.ValueType.object);
        root.addChild(county, countyJson);

        countyJson.addChild("color", new JsonValue(coloringGrid.getColor().getSerializedName()));

        if (getCompletion() < 1) {
            byte[] compressed = Util.compress(coloringGrid.asBitSet().toByteArray());
            String encoded = Base64.getEncoder().encodeToString(compressed);
            countyJson.addChild("coloredPoints", new JsonValue(encoded));
            countyJson.remove("complete");
        } else {
            countyJson.remove("coloredPoints");
            countyJson.addChild("complete", new JsonValue(true));
        }

        dataHandle.writeString(root.toJson(JsonWriter.OutputType.json), false);
    }

    private ColoringGrid load() {
        FileHandle dataHandle = Gdx.files.local("data/" + StateRenderer.getStateFromId(stateId) + ".json");
        if (!dataHandle.exists()) throw new IllegalStateException("No saved data for county");
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(dataHandle);
        JsonValue countyJson = root.get(county);
        if (countyJson == null) throw new IllegalStateException("No saved data for county");
        if (countyJson.getBoolean("complete", false))
            throw new IllegalStateException("Tried to load an already completed county");
        return ColoringGrid.fromJson(countyJson);
    }


    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        boolean zoomedOut = camera.zoom == maxZoom;
        maxZoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = zoomedOut ? maxZoom : Math.min(camera.zoom, maxZoom);
        camera.update();
        hudCamera.setToOrtho(false, width, height);
        cursorRenderer.setProjectionMatrix(hudCamera.combined);
        batch.setProjectionMatrix(hudCamera.combined);
        initStage();
    }

    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void show() {
        if (coloringGridFuture != null) coloringGrid = Util.getFutureValue(coloringGridFuture);
    }
    @Override public void hide() {}
    @Override public boolean keyDown(int keycode) { return false; }
    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
}
