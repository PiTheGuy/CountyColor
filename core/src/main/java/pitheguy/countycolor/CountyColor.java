package pitheguy.countycolor;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pitheguy.countycolor.gui.screens.TitleScreen;
import pitheguy.countycolor.options.Options;
import pitheguy.countycolor.util.Util;

public class CountyColor extends Game {
    private static CountyColor instance;
    private BitmapFont font;
    private SpriteBatch batch;
    private boolean showDebugStats = false;

    @Override
    public void create() {
        instance = this;
        Options.load();
        setScreen(new TitleScreen(this));
        font = new BitmapFont();
        batch = new SpriteBatch();
    }

    @Override
    public void render() {
        super.render();
        if (showDebugStats) {
            batch.begin();
            font.draw(batch, Util.getMemoryUsageString(), 10, 20);
            font.draw(batch, Gdx.graphics.getFramesPerSecond() + " FPS", 10, 40);
            batch.end();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
        batch.dispose();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(false, width, height);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
    }

    public static CountyColor getInstance() {
        return instance;
    }

    public void toggleDebugStats() {
        showDebugStats = !showDebugStats;
    }
}
