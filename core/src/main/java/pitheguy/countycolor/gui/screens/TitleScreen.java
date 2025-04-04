package pitheguy.countycolor.gui.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import pitheguy.countycolor.render.renderer.TitleScreenCountryRenderer;
import pitheguy.countycolor.render.util.RenderConst;
import pitheguy.countycolor.util.InputManager;

public class TitleScreen implements Screen {
    private final Game game;
    private final TitleScreenCountryRenderer renderer = new TitleScreenCountryRenderer();
    private final OrthographicCamera camera;
    private final Stage stage;
    private final Skin skin = new Skin(Gdx.files.internal("skin/skin.json"));
    private final OrthographicCamera hudCamera;
    private final CountryScreen countryScreen;
    private boolean awaitingLoad = false;
    private Table table;
    private final BitmapFont font = new BitmapFont();
    private final SpriteBatch batch = new SpriteBatch();

    public TitleScreen(Game game) {
        this.game = game;
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.zoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();
        hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        ScreenViewport viewport = new ScreenViewport(hudCamera);
        stage = new Stage(viewport);
        countryScreen = new CountryScreen(game);
        initStage();
    }

    private void initStage() {
        table = new Table(skin);
        Label title = new Label("CountyColor", skin, "title");
        table.add(title).row();
        TextButton startColoringButton = new TextButton("Start Coloring", skin);
        startColoringButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (countryScreen.isRendererReady()) game.setScreen(countryScreen);
                else {
                    startColoringButton.setDisabled(true);
                    startColoringButton.setText("Loading...");
                    awaitingLoad = true;
                }
            }
        });
        table.add(startColoringButton).row();
        TextButton optionsButton = new TextButton("Options", skin); //TODO implement options screen
        table.add(optionsButton).row();
        TextButton quitToDesktopButton = new TextButton("Quit to Desktop", skin);
        quitToDesktopButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        table.add(quitToDesktopButton).row();
        table.setPosition(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
        stage.addActor(table);
    }

    @Override
    public void show() {
        InputManager.setInputProcessor(stage);

    }

    @Override
    public void render(float delta) {
        if (!renderer.isDoneLoading()) {
            renderLoadingText();
            return;
        }
        renderer.render(camera);
        stage.act(delta);
        stage.draw();
        if (awaitingLoad && countryScreen.isRendererReady()) {
            game.setScreen(countryScreen);
        }
    }

    private void renderLoadingText() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        GlyphLayout layout = new GlyphLayout(font, "Loading...");
        float x = (Gdx.graphics.getWidth() - layout.width) / 2;
        float y = (Gdx.graphics.getHeight() + layout.height) / 2;
        font.draw(batch, layout, x, y);
        batch.end();
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        font.dispose();
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.zoom = (float) RenderConst.RENDER_SIZE / Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();
        hudCamera.setToOrtho(false, width, height);
        hudCamera.update();
        stage.getViewport().update(width, height, true);
        table.setPosition(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
        renderer.invalidateCache();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
