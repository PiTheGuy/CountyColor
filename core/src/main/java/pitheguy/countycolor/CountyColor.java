package pitheguy.countycolor;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pitheguy.countycolor.gui.CountryScreen;
import pitheguy.countycolor.util.SharedConstants;
import pitheguy.countycolor.util.Util;

public class CountyColor extends Game {
    private BitmapFont font;
    private SpriteBatch batch;

    @Override
    public void create() {
        setScreen(new CountryScreen(this));
        font = new BitmapFont();
        batch = new SpriteBatch();
    }

    @Override
    public void render() {
        super.render();
        if (SharedConstants.SHOW_MEMORY) {
            batch.begin();
            font.draw(batch, Util.getMemoryUsageString(), 10, 20);
            batch.end();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
        batch.dispose();
    }
}
