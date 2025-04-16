package pitheguy.countycolor.gui.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import pitheguy.countycolor.coloring.ColoringGrid;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.metadata.CountyData;
import pitheguy.countycolor.render.renderer.ColoringRenderer;
import pitheguy.countycolor.render.renderer.CountyRenderer;
import pitheguy.countycolor.render.util.*;
import pitheguy.countycolor.util.InputManager;
import pitheguy.countycolor.util.Util;

import java.util.Base64;
import java.util.concurrent.*;

public class CountyColorScreen implements Screen, InputProcessor {
    private final Game game;
    private final CountyData.County county;
    private final OrthographicCamera camera;
    private final OrthographicCamera hudCamera;
    private final ShapeRenderer cursorRenderer = new ShapeRenderer();
    private final ShapeRenderer progressBarRenderer = new ShapeRenderer();
    private final CountyRenderer countyRenderer;
    private final ColoringRenderer coloringRenderer = new ColoringRenderer();
    private final Stage stage;
    private Skin skin;
    private Button backButton;
    private Slider slider;
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
    private boolean dirty = false;
    private Thread saveThread;
    private boolean markedAsComplete = false;

    public CountyColorScreen(Game game, CountyData.County county, boolean load) {
        this.game = game;
        this.county = county;
        maxZoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        ScreenViewport viewport = new ScreenViewport(hudCamera);
        stage = new Stage(viewport);
        camera.zoom = maxZoom;
        camera.update();
        transitionHelper = new CameraTransitionHelper(game, camera);
        if (load) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            coloringGridFuture = executor.submit(this::load);
            executor.shutdown();
        } else coloringGrid = new ColoringGrid();
        countyRenderer = new CountyRenderer(county);
        initStage();
    }

    private void initStage() {
        stage.clear();
        skin = new Skin(Gdx.files.internal("skin/skin.json"));
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
        backButton = new Button(skin);
        Texture menuTexture = new Texture(Gdx.files.internal("icons/menu.png"));
        backButton.add(new Image(menuTexture));
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                saveAsync();
                game.setScreen(new CountyColorMenuScreen(game, CountyColorScreen.this));
            }
        });
        backButton.setSize(40, 40);
        backButton.setPosition(0, Gdx.graphics.getHeight() - backButton.getHeight());
        stage.addActor(backButton);
    }

    public void prepare(MapColor color) {
        coloringGrid.setColor(color);
    }

    @Override
    public void render(float delta) {
        if (totalPixels == -1)
            totalPixels = countyRenderer.computeTotalGridSquares();
        camera.update();
        transitionHelper.update(delta);
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (inTransition) {
            countyRenderer.renderCountyFilled(camera, 1, coloringGrid.getColor());
            return;
        }
        countyRenderer.renderHighlight(camera, delta);
        coloringRenderer.render(coloringGrid, camera);
        countyRenderer.renderCounty(camera);
        renderCursor();
        renderProgressBar();
        stage.act(delta);
        stage.draw();
        timeSinceSave += delta;
        if (timeSinceSave > 10f) {
            saveAsync();
            timeSinceSave = 0;
        }
        if (getCompletion() == 1) onCountyCompleted();
    }

    private void renderCursor() {
        cursorRenderer.begin(ShapeRenderer.ShapeType.Filled);
        cursorRenderer.setColor(canColor() ? coloringGrid.getColor().getColor() : Color.RED);
        cursorRenderer.circle(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), brushSize / camera.zoom);
        cursorRenderer.end();
    }

    private void renderProgressBar() {
        progressBarRenderer.begin(ShapeRenderer.ShapeType.Line);
        progressBarRenderer.setColor(Color.BLACK);
        progressBarRenderer.rect(50, Gdx.graphics.getHeight() - 35, 100, 30);
        progressBarRenderer.end();
        progressBarRenderer.begin(ShapeRenderer.ShapeType.Filled);
        progressBarRenderer.rect(50, Gdx.graphics.getHeight() - 35, getCompletion() * 100, 30);
        progressBarRenderer.end();
        batch.begin();

        String progressString = String.format("%.2f%%", Math.floor(getCompletion() * 10000) / 100);
        float textWidth = RenderUtil.getTextWidth(font, progressString);
        float textHeight = RenderUtil.getTextHeight(font, progressString);
        font.draw(batch, progressString, 100 - textWidth / 2, Gdx.graphics.getHeight() - 8 - textHeight / 2);
        batch.end();
    }

    private void onCountyCompleted() {
        dirty = true; // Mark dirty to force save
        saveAsync();
        addCountyToCompletionFile();
        inTransition = true;
        transitionHelper.slowTransition(new Vector2(0, 0), 2f, new CountyCompleteScreen(game, county, coloringGrid.getColor()), false);
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
        } else if (coloring) {
            lastColor.set(screenX, screenY);
            if (canColor()) {
                Vector3 lastColorWorld = camera.unproject(new Vector3(lastColor.x, lastColor.y, 0));
                applyBrush(lastColorWorld);
            }
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
        Vector2 mouseWorldBefore = RenderUtil.getMouseWorldCoords(camera);
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            float zoomFactor = (amountY < 0) ? 0.9f : 1.1f;
            camera.zoom = MathUtils.clamp(camera.zoom * zoomFactor, 0.1f, maxZoom);
            camera.update();
            Vector2 mouseWorldAfter = RenderUtil.getMouseWorldCoords(camera);
            Vector2 delta = mouseWorldBefore.sub(mouseWorldAfter);
            camera.position.add(delta.x, delta.y, 0);
        } else {
            brushSize = MathUtils.clamp(brushSize - amountY, 1, 100);
            slider.setValue(brushSize);
        }
        return true;
    }

    private void applyBrush(Vector3 lastColor) {
        Vector2 mouse = RenderUtil.getMouseWorldCoords(camera);
        Vector2 currentPos = new Vector2(lastColor.x, lastColor.y);
        int steps = brushSize > 30 ? 1 : 5;
        Vector2 delta = mouse.cpy().sub(currentPos).scl(1f / steps);
        for (int i = 0; i < steps; i++) {
            currentPos.add(delta);
            if (canColor(currentPos)) {
                coloringGrid.applyBrush(currentPos, brushSize);
                dirty = true;
            }
        }
    }

    private boolean canColor() {
        Vector2 center = RenderUtil.getMouseWorldCoords(camera);
        return canColor(center);
    }

    private boolean canColor(Vector2 pos) {
        int numPoints = getNumTestPoints();
        for (int i = 0; i < numPoints; i++) {
            float angle = (float)(i * Math.PI * 2 / numPoints); // 0 to 2π
            float dx = brushSize * (float)Math.cos(angle);
            float dy = brushSize * (float)Math.sin(angle);
            Vector2 offsetPoint = new Vector2(pos.x + dx, pos.y + dy);
            if (!countyRenderer.isCoordinateWithinCounty(offsetPoint)) return false;
        }
        return true;
    }

    private int getNumTestPoints() {
        if (brushSize > 30) return 128;
        else if (brushSize > 10) return 64;
        else return 32;
    }

    public float getCompletion() {
        return markedAsComplete ? 1 : (float) Math.min((double) coloringGrid.coloredPoints() / totalPixels, 1);
    }

    public void markAsComplete() {
        markedAsComplete = true;
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
        progressBarRenderer.dispose();
        skin.dispose();
    }

    private void saveAsync() {
        saveThread = new Thread(this::save);
        saveThread.start();
    }

    public void awaitSave() {
        try {
            if (saveThread != null) saveThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException("Save thread interrupted", e);
        }
    }

    private void save() {
        if (!dirty) return;
        FileHandle dataHandle = Gdx.files.local("data/" + county.getState() + ".json");
        JsonReader reader = new JsonReader();
        JsonValue root = dataHandle.exists() ? reader.parse(dataHandle) : new JsonValue(JsonValue.ValueType.object);
        if (root.has(county.getName())) root.remove(county.getName());
        JsonValue countyJson = new JsonValue(JsonValue.ValueType.object);
        root.addChild(county.getName(), countyJson);
        countyJson.addChild("color", new JsonValue(coloringGrid.getColor().getSerializedName()));
        if (getCompletion() < 1) {
            countyJson.addChild("coloredPoints", new JsonValue(coloringGrid.asEncodedString()));
        }
        countyJson.addChild("completion", new JsonValue(getCompletion()));
        dataHandle.writeString(root.toJson(JsonWriter.OutputType.json), false);
    }

    private void addCountyToCompletionFile() {
        FileHandle handle = Gdx.files.local("data/completed_counties.json");
        JsonReader reader = new JsonReader();
        JsonValue root = handle.exists() ? reader.parse(handle) : new JsonValue(JsonValue.ValueType.object);
        JsonValue state = root.has(county.getState()) ? root.get(county.getState()) : new JsonValue(JsonValue.ValueType.object);
        state.addChild(county.getName(), new JsonValue(coloringGrid.getColor().getSerializedName()));
        if (!root.has(county.getState())) root.addChild(county.getState(), state);
        handle.writeString(root.toJson(JsonWriter.OutputType.json), false);
    }

    private ColoringGrid load() {
        FileHandle dataHandle = Gdx.files.local("data/" + county.getState() + ".json");
        if (!dataHandle.exists()) throw new IllegalStateException("No saved data for county");
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(dataHandle);
        JsonValue countyJson = root.get(county.getName());
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
        progressBarRenderer.setProjectionMatrix(hudCamera.combined);
        batch.setProjectionMatrix(hudCamera.combined);
        stage.getViewport().update(width, height, true);
        slider.setPosition(width / 2f - slider.getWidth() / 2f, height - 30);
        backButton.setPosition(0, Gdx.graphics.getHeight() - backButton.getHeight());
    }

    @Override
    public void show() {
        if (coloringGridFuture != null) coloringGrid = Util.getFutureValue(coloringGridFuture);
        InputManager.setInputProcessor(new InputMultiplexer(stage, this));
    }

    public String getState() {
        return county.getState();
    }

    @Override public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            saveAsync();
            game.setScreen(new CountyColorMenuScreen(game, CountyColorScreen.this));
            return true;
        } else if (keycode == Input.Keys.TAB) {
            countyRenderer.highlightUncoloredAreas();
            return true;
        }
        return false;
    }

    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
}
