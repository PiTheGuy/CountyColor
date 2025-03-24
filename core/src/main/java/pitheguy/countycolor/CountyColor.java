package pitheguy.countycolor;

import com.badlogic.gdx.*;
import pitheguy.countycolor.gui.CountryScreen;
import pitheguy.countycolor.gui.StateScreen;

public class CountyColor extends Game {
    @Override
    public void create() {
        setScreen(new CountryScreen(this));
    }
}
