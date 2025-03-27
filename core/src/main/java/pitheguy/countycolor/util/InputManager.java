package pitheguy.countycolor.util;

import com.badlogic.gdx.*;
import pitheguy.countycolor.CountyColor;

public class InputManager {

    public static void setInputProcessor(InputProcessor processor) {
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(new GlobalInputProcessor());
        multiplexer.addProcessor(processor);
        Gdx.input.setInputProcessor(multiplexer);
    }

    private static class GlobalInputProcessor extends InputAdapter {
        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.F3) {
                CountyColor.getInstance().toggleDebugStats();
                return true;
            }
            return false;
        }
    }
}
