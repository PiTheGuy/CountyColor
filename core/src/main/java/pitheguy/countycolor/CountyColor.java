package pitheguy.countycolor;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pitheguy.countycolor.gui.screens.CountryScreen;
import pitheguy.countycolor.util.DebugFlags;
import pitheguy.countycolor.util.Util;

public class CountyColor extends Game {
    private static CountyColor instance;
    private BitmapFont font;
    private SpriteBatch batch;
    private boolean showDebugStats = false;

    @Override
    public void create() {
        instance = this;
        setScreen(new CountryScreen(this));
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

    public static CountyColor getInstance() {
        return instance;
    }

    public void toggleDebugStats() {
        showDebugStats = !showDebugStats;
    }
}
