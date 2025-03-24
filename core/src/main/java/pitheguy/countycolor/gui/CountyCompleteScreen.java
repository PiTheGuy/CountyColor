package pitheguy.countycolor.gui;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import pitheguy.countycolor.render.renderer.CountyRenderer;
import pitheguy.countycolor.render.renderer.StateRenderer;

public class CountyCompleteScreen implements Screen {
    private final Stage stage;
    private final OrthographicCamera camera;
    private final CountyRenderer countyRenderer;

    public CountyCompleteScreen(Game game, String county, String stateId) {
        countyRenderer = new CountyRenderer(county, stateId);
        stage = new Stage();
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.input.setInputProcessor(stage);
        Skin skin = new Skin(Gdx.files.internal("skin.json"));
        TextButton button = new TextButton("Continue", skin);
        button.setSize(200, 60);
        button.setPosition(Gdx.graphics.getWidth() / 2f - button.getWidth() / 2, 100);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new StateScreen(game, StateRenderer.getStateFromId(stateId)));
            }
        });
        stage.addActor(button);
        Label title = new Label("County Complete", skin, "title");
        title.setPosition(Gdx.graphics.getWidth() / 2f - title.getWidth() / 2, Gdx.graphics.getHeight() - title.getHeight());
        stage.addActor(title);
        Label countyName = new Label(county + " County, " + StateRenderer.getStateFromId(stateId), skin);
        countyName.setPosition(Gdx.graphics.getWidth() / 2f - countyName.getWidth() / 2, Gdx.graphics.getHeight() - title.getHeight() - countyName.getHeight());
        stage.addActor(countyName);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
        countyRenderer.renderCountyFilled(camera, 0.5f);
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
