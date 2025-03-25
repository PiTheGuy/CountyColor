package pitheguy.countycolor;

import com.badlogic.gdx.Game;
import pitheguy.countycolor.gui.CountryScreen;

public class CountyColor extends Game {
    @Override
    public void create() {
        setScreen(new CountryScreen(this));
    }
}
