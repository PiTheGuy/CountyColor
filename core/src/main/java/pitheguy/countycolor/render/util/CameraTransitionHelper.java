package pitheguy.countycolor.render.util;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import pitheguy.countycolor.options.Options;

public class CameraTransitionHelper {
    public static final float ZOOM_SPEED = 2f;
    public static final float MOVE_SPEED = 5f;

    private final Game game;
    private final OrthographicCamera camera;
    private Vector2 targetPos = null;
    private float targetZoom;
    private Screen targetScreen = null;
    private boolean dispose;

    public CameraTransitionHelper(Game game, OrthographicCamera camera) {
        this.game = game;
        this.camera = camera;
    }

    public void transition(Vector2 targetPos, float targetZoom, Screen targetScreen, boolean dispose) {
        this.dispose = dispose;
        if (Options.REDUCE_MOTION.get()) {
            if (targetScreen != null) setScreen(targetScreen, dispose);
            else {
                camera.position.set(targetPos.x, targetPos.y, 0);
                camera.zoom = targetZoom;
                camera.update();
            }
            return;
        }
        this.targetPos = targetPos;
        this.targetZoom = targetZoom;
        this.targetScreen = targetScreen;
    }

    public void update(float delta) {
        if (targetPos == null) return;
        Vector2 camPos2D = new Vector2(camera.position.x, camera.position.y);
        Vector2 diff = new Vector2(targetPos).sub(camPos2D);
        camPos2D.mulAdd(diff, Math.min(delta * MOVE_SPEED, 1f));
        camera.position.set(camPos2D.x, camPos2D.y, 0);
        float zoomDiff = targetZoom - camera.zoom;
        camera.zoom += zoomDiff * Math.min(delta * ZOOM_SPEED, 1f);
        if (diff.len() < 1f && Math.abs(zoomDiff) < 0.01f) {
            camera.zoom = targetZoom;
            camera.position.set(targetPos.x, targetPos.y, 0);
            if (targetScreen != null) setScreen(targetScreen, dispose);
            targetPos = null;
            targetScreen = null;
        }
        camera.update();
    }

    public boolean isInTransition() {
        return targetPos != null;
    }

    public void stopTransition() {
        targetPos = null;
        targetScreen = null;
    }

    private void setScreen(Screen targetScreen, boolean dispose) {
        Screen oldScreen = game.getScreen();
        game.setScreen(targetScreen);
        if (dispose) oldScreen.dispose();
    }
}
