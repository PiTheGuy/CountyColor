package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pitheguy.countycolor.coloring.history.ColoringHistory;
import pitheguy.countycolor.coloring.history.HistorySnapshot;

import java.util.ArrayList;
import java.util.List;

import static pitheguy.countycolor.render.util.RenderConst.RENDER_SIZE;

public class ColoringHistoryRenderer {
    public static final float ANIMATION_DURATION = 3;
    private final SpriteBatch batch = new SpriteBatch();
    private final Texture[] textures;
    private float animationTime = 0;

    public ColoringHistoryRenderer(ColoringHistory history) {
        List<HistorySnapshot> snapshots = new ArrayList<>(history.getSnapshots());
        textures = new Texture[snapshots.size()];
        for (int i = 0; i < snapshots.size(); i++)
            textures[i] = snapshots.get(i).getTexture();
    }

    public void render(OrthographicCamera camera, float delta) {
        animationTime += delta;
        if (animationTime >= ANIMATION_DURATION) return;
        int frame = (int) (animationTime / ANIMATION_DURATION * textures.length);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(textures[frame], -RENDER_SIZE / 4f, -RENDER_SIZE / 4f, RENDER_SIZE / 2f, RENDER_SIZE / 2f);
        batch.end();
    }

    public boolean isAnimationFinished() {
        return animationTime >= ANIMATION_DURATION;
    }

    public void restart() {
        animationTime = 0;
    }

    public void dispose() {
        batch.dispose();
    }
}
