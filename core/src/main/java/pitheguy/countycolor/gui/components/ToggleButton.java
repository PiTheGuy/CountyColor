package pitheguy.countycolor.gui.components;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class ToggleButton extends TextButton {
    public ToggleButton(Skin skin, String styleName, boolean checked) {
        super(checked ? "ON" : "OFF", skin, styleName);
        setChecked(checked);
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setText(isChecked() ? "ON" : "OFF");
            }
        });
    }


}
