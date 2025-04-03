package pitheguy.countycolor.render.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import pitheguy.countycolor.coloring.CountyData;
import pitheguy.countycolor.render.PolygonCollection;
import pitheguy.countycolor.render.util.RenderUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static pitheguy.countycolor.render.util.RenderConst.RENDER_SIZE;

public class StateRenderer extends RegionRenderer {
    private static final Map<String, String> STATE_TO_ID = new HashMap<>();
    static {
        initStateIdMap();
    }
    private final BooleanSupplier useCachedTexture;
    private final SpriteBatch batch = new SpriteBatch();
    private final String state;
    private FrameBuffer frameBuffer;
    private TextureRegion cachedTexture;


    public StateRenderer(String state, BooleanSupplier useCachedTexture) {
        super("metadata/counties.json", properties -> properties.getString("STATEFP").equals(STATE_TO_ID.get(state)));
        this.state = state;
        this.useCachedTexture = useCachedTexture;
    }

    public void renderState(OrthographicCamera camera, CountyData countyData) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(-RENDER_SIZE, -RENDER_SIZE, RENDER_SIZE * 2, RENDER_SIZE * 2);
        shapeRenderer.end();
        batch.setProjectionMatrix(camera.combined);
        if (useCachedTexture()) {
            if (cachedTexture == null) {
                frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
                frameBuffer.begin();
                renderStateInternal(camera, countyData);
                Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                Texture texture = new Texture(pixmap);
                cachedTexture = new TextureRegion(texture);
                cachedTexture.flip(false, true);
                frameBuffer.end();
                pixmap.dispose();
            }
            batch.begin();
            float renderWidth = RENDER_SIZE * ((float) Gdx.graphics.getWidth() / Gdx.graphics.getHeight());
            batch.draw(cachedTexture, -renderWidth / 2f, -RENDER_SIZE / 2f, renderWidth, RENDER_SIZE);
            batch.end();
        } else renderStateInternal(camera, countyData);
    }

    private boolean useCachedTexture() {
        return useCachedTexture.getAsBoolean() &&
               Gdx.graphics.getHeight() >= RENDER_SIZE &&
               Gdx.graphics.getWidth() >= RENDER_SIZE &&
               Gdx.graphics.getHeight() <= Gdx.graphics.getWidth();
    }

    private void renderStateInternal(OrthographicCamera camera, CountyData countyData) {
        ensureLoadingFinished();
        updateCamera(camera);
        renderBackground();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (String county : shapes.keySet())
            if (countyData.get(county).isCompleted())
                fillSubregion(county, countyData.get(county).mapColor().getColor());
        shapeRenderer.end();
        renderRegion(camera, true, true);
    }

    public void invalidateCache() {
        if (cachedTexture != null) cachedTexture.getTexture().dispose();
        cachedTexture = null;
    }

    private static void initStateIdMap() {
        String[] mappings = Gdx.files.internal("metadata/state_ids.txt").readString().split("\n");
        for (String mapping : mappings) {
            String[] parts = mapping.split("=");
            STATE_TO_ID.put(parts[0], parts[1]);
        }
    }

    public static String getIdForState(String state) {
        return STATE_TO_ID.get(state);
    }

    @Override
    protected void postProcessShapes(Map<String, PolygonCollection> shapes) {
        if (state.equals("Alaska")) for (PolygonCollection polygons : shapes.values()) RenderUtil.fixRollover(polygons);
    }

    public void dispose() {
        super.dispose();
        batch.dispose();
        if (cachedTexture != null) cachedTexture.getTexture().dispose();
        if (frameBuffer != null) frameBuffer.dispose();
    }
}
