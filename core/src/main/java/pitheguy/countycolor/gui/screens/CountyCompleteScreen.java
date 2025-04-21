package pitheguy.countycolor.gui.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.coloring.history.ColoringHistory;
import pitheguy.countycolor.metadata.CountyData;
import pitheguy.countycolor.render.renderer.ColoringHistoryRenderer;
import pitheguy.countycolor.render.renderer.CountyRenderer;
import pitheguy.countycolor.util.InputManager;

public class CountyCompleteScreen implements Screen {
    private final Stage stage;
    private final OrthographicCamera camera;
    private final CountyRenderer countyRenderer;
    private final ColoringHistoryRenderer historyRenderer;
    private final MapColor color;
    private final StateScreen stateScreen;
    private final Skin skin;
    private final CountyData.County county;

    public CountyCompleteScreen(Game game, CountyData.County county, MapColor color, ColoringHistory history) {
        this.color = color;
        this.county = county;
        countyRenderer = new CountyRenderer(county);
        historyRenderer = new ColoringHistoryRenderer(history);
        stage = new Stage();
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stateScreen = new StateScreen(game, county.getState());
        InputManager.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("skin/skin.json"));
        TextButton button = new TextButton("Continue", skin);
        button.setSize(200, 60);
        button.setPosition(Gdx.graphics.getWidth() / 2f - button.getWidth() / 2, 100);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(stateScreen);
            }
        });
        TextButton replayButton = new TextButton("Replay", skin);
        replayButton.setSize(200, 60);
        replayButton.setPosition(Gdx.graphics.getWidth() / 2f - replayButton.getWidth() / 2, 100 + button.getHeight() + 20);
        replayButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                historyRenderer.restart();
            }
        });
        stage.addActor(button);
        stage.addActor(replayButton);

    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (historyRenderer.isAnimationFinished())
            countyRenderer.renderCountyFilled(camera, 0.5f, color);
        else {
            historyRenderer.render(camera, delta);
            countyRenderer.renderCounty(camera, 0.5f);
        }
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
        countyRenderer.dispose();
        skin.dispose();
    }

    @Override
    public void show() {
        countyRenderer.ensureLoadingFinished();
        Label title = new Label((county.isIndependentCity() ? "Independent City" : "County") + " Complete", skin, "title");
        title.setPosition(Gdx.graphics.getWidth() / 2f - title.getWidth() / 2, Gdx.graphics.getHeight() - title.getHeight());
        stage.addActor(title);
        Label countyName = new Label(county.getFullName() + ", " + county.getState(), skin);
        countyName.setPosition(Gdx.graphics.getWidth() / 2f - countyName.getWidth() / 2, Gdx.graphics.getHeight() - title.getHeight() - countyName.getHeight());
        stage.addActor(countyName);
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
