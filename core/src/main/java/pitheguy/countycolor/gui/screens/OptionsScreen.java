package pitheguy.countycolor.gui.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import pitheguy.countycolor.gui.components.InfoTooltip;
import pitheguy.countycolor.gui.components.ToggleButton;
import pitheguy.countycolor.options.Option;
import pitheguy.countycolor.options.Options;
import pitheguy.countycolor.util.InputManager;

public class OptionsScreen extends InputAdapter implements Screen {
    private final Skin skin = new Skin(Gdx.files.internal("skin/skin.json"));
    private final Game game;
    private final Screen lastScreen;
    private final Stage stage;
    private final InfoTooltip tooltip;
    private final OrthographicCamera hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

    public OptionsScreen(Game game, Screen lastScreen) {
        this.game = game;
        this.lastScreen = lastScreen;
        ScreenViewport viewport = new ScreenViewport(hudCamera);
        this.stage = new Stage(viewport);
        initStage();
        this.tooltip = new InfoTooltip(skin, false);
    }

    private void initStage() {
        Table root = new Table();
        root.setFillParent(true);
        root.top();
        Label title = new Label("Options", skin, "title");
        root.add(title).colspan(2).padTop(20).center().row();
        addOption(root, "Reduce Motion", Options.REDUCE_MOTION, "Disabled animations and transitions.");
        addOption(root, "Async Grid Updates", Options.ASYNC_GRID_UPDATES, "Improves performance, but may cause artifacts when using larger brush sizes.");
        addOption(root, "Enforce Map Colors", Options.ENFORCE_MAP_COLORS, "Ensures bordering counties never share a color unless no other options are available.");
        addOption(root, "Neighbor Border Colors", Options.NEIGHBOR_BORDER_COLORS, "Displays the color of adjacent counties from other states along shared borders.");
        TextButton doneButton = new TextButton("Done", skin);
        doneButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(lastScreen);
            }
        });
        root.add(doneButton).colspan(2).padTop(20).center().row();
        stage.addActor(root);
    }

    private void addOption(Table root, String text, Option<Boolean> option, String tooltip) {
        Label label = new Label(text, skin);
        ToggleButton button = new OptionToggleButton(skin, option, tooltip);
        root.add(label).pad(10).center();
        root.add(button).pad(10).row();
    }

    @Override
    public void show() {
        InputManager.setInputProcessor(new InputMultiplexer(stage, this));
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
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            game.setScreen(lastScreen);
            return true;
        }
        return false;
    }

    @Override public void hide() {
        Options.save();
        dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}

    private class OptionToggleButton extends ToggleButton {
        public OptionToggleButton(Skin skin, Option<Boolean> option, String tooltipText) {
            super(skin, "toggle", option.get());
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    option.set(isChecked());
                }
            });
            if (tooltipText != null) addListener(new InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    tooltip.show(stage, null, tooltipText);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    tooltip.hide();
                }
            });
        }
    }
}
