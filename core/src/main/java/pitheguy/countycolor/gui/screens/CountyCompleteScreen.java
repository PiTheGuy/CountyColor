package pitheguy.countycolor.gui.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import pitheguy.countycolor.coloring.MapColor;
import pitheguy.countycolor.render.renderer.CountyRenderer;
import pitheguy.countycolor.render.renderer.StateRenderer;
import pitheguy.countycolor.util.InputManager;

public class CountyCompleteScreen implements Screen {
    private final Stage stage;
    private final OrthographicCamera camera;
    private final CountyRenderer countyRenderer;
    private final MapColor color;
    private final StateScreen stateScreen;

    public CountyCompleteScreen(Game game, String county, String state, MapColor color) {
        this.color = color;
        countyRenderer = new CountyRenderer(county, state);
        stage = new Stage();
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stateScreen = new StateScreen(game, state);
        InputManager.setInputProcessor(stage);
        Skin skin = new Skin(Gdx.files.internal("skin.json"));
        TextButton button = new TextButton("Continue", skin);
        button.setSize(200, 60);
        button.setPosition(Gdx.graphics.getWidth() / 2f - button.getWidth() / 2, 100);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(stateScreen);
            }
        });
        stage.addActor(button);
        Label title = new Label("County Complete", skin, "title");
        title.setPosition(Gdx.graphics.getWidth() / 2f - title.getWidth() / 2, Gdx.graphics.getHeight() - title.getHeight());
        stage.addActor(title);
        Label countyName = new Label(county + " County, " + state, skin);
        countyName.setPosition(Gdx.graphics.getWidth() / 2f - countyName.getWidth() / 2, Gdx.graphics.getHeight() - title.getHeight() - countyName.getHeight());
        stage.addActor(countyName);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
        countyRenderer.renderCountyFilled(camera, 0.5f, color);
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
