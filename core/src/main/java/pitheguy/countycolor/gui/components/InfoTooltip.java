package pitheguy.countycolor.gui.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;

public class InfoTooltip extends Table {
    private final Label title;
    private final Label text;

    public InfoTooltip(Skin skin) {
        super(skin);
        setBackground("round-white");
        title = new Label("", skin);
        add(title).pad(5).width(200).left();
        row();
        text = new Label("", skin);
        text.setFontScale(0.5f);
        add(text).pad(5).width(200).left();
        pack();
        setVisible(false);
    }

    public void show(Stage stage, String title, String text) {
        this.title.setText(title);
        this.text.setText(text);
        pack();
        Vector2 pos = getTooltipPosition();
        setPosition(pos.x, pos.y);
        setVisible(true);
        stage.addActor(this);
    }

    private Vector2 getTooltipPosition() {
        int screenX = Gdx.input.getX();
        int screenY = Gdx.graphics.getHeight() - Gdx.input.getY();
        float x = screenX + getWidth() > Gdx.graphics.getWidth() ? Gdx.graphics.getWidth() - getWidth() : screenX;
        float y = screenY + getHeight() > Gdx.graphics.getHeight() ? Gdx.graphics.getHeight() - getHeight() : screenY;
        return new Vector2(x, y);
    }

    public void hide() {
        remove();
        setVisible(false);
    }
}
