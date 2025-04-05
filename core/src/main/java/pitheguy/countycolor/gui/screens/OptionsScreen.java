package pitheguy.countycolor.gui.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import pitheguy.countycolor.gui.components.ToggleButton;
import pitheguy.countycolor.options.Option;
import pitheguy.countycolor.options.Options;
import pitheguy.countycolor.util.InputManager;

public class OptionsScreen implements Screen {
    private final Skin skin = new Skin(Gdx.files.internal("skin/skin.json"));
    private final Game game;
    private final Screen lastScreen;
    private final Stage stage;
    private final OrthographicCamera hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

    public OptionsScreen(Game game, Screen lastScreen) {
        this.game = game;
        this.lastScreen = lastScreen;
        ScreenViewport viewport = new ScreenViewport(hudCamera);
        this.stage = new Stage(viewport);
        initStage();
    }

    private void initStage() {
        Table root = new Table();
        root.setFillParent(true);
        root.top();
        Label title = new Label("Options", skin, "title");
        root.add(title).colspan(2).padTop(20).center().row();
        Label reduceMotionLabel = new Label("Reduce Motion", skin);
        ToggleButton reduceMotionButton = new OptionToggleButton(skin, Options.REDUCE_MOTION);
        root.add(reduceMotionLabel).pad(10).center();
        root.add(reduceMotionButton).pad(10).row();
        Label asyncGridUpdatesLabel = new Label("Async Grid Updates", skin);
        ToggleButton asyncGridUpdatesButton = new OptionToggleButton(skin, Options.ASYNC_GRID_UPDATES);
        root.add(asyncGridUpdatesLabel).pad(10).center();
        root.add(asyncGridUpdatesButton).pad(10).row();
        TextButton doneButton = new TextButton("Done", skin);
        doneButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Options.save();
                game.setScreen(lastScreen);
            }
        });
        root.add(doneButton).colspan(2).padTop(20).center().row();
        stage.addActor(root);
    }

    @Override
    public void show() {
        InputManager.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        hudCamera.setToOrtho(false, width, height);
        hudCamera.update();
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    private static class OptionToggleButton extends ToggleButton {
        public OptionToggleButton(Skin skin, Option<Boolean> option) {
            super(skin, "toggle", option.get());
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    option.set(isChecked());
                }
            });
        }
    }
}
