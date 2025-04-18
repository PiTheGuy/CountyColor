package pitheguy.countycolor.gui.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import pitheguy.countycolor.util.InputManager;

public class CountyColorMenuScreen extends InputAdapter implements Screen {
    private final Game game;
    private final CountyColorScreen lastScreen;
    private final Stage stage = new Stage();
    private final Skin skin = new Skin(Gdx.files.internal("skin/skin.json"));

    public CountyColorMenuScreen(Game game, CountyColorScreen lastScreen) {
        this.game = game;
        this.lastScreen = lastScreen;
        initStage();
    }

    private void initStage() {
        stage.clear();
        Table root = new Table();
        TextButton backButton = new TextButton("Back to County", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(lastScreen);
            }
        });
        root.add(backButton).expandX().center().row();
        TextButton returnToStateButton = new TextButton("Return to State", skin);
        returnToStateButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new StateScreen(game, lastScreen.getState()));
                lastScreen.dispose();
                dispose();
            }
        });
        root.add(returnToStateButton).expandX().center().row();
        TextButton exitButton = new TextButton("Save and Exit", skin);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                lastScreen.awaitSave();
                Gdx.app.exit();
            }
        });
        root.add(exitButton).expandX().center().row();
        if (lastScreen.getCompletion() > 0.998) {
            TextButton markAsCompleteButton = new TextButton("Mark as Complete", skin);
            markAsCompleteButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    lastScreen.markAsComplete();
                    game.setScreen(lastScreen);
                    dispose();
                }
            });
            root.add(markAsCompleteButton).expandX().center().row();
        }
        root.setPosition(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
        stage.addActor(root);
    }

    @Override
    public void show() {
        InputManager.setInputProcessor(new InputMultiplexer(stage, this));
    }

    @Override
    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            game.setScreen(lastScreen);
            return true;
        }
        return false;
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
