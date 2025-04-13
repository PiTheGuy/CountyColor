package pitheguy.countycolor.render.util;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import pitheguy.countycolor.options.Options;

public class CameraTransitionHelper {
    public static final float ZOOM_SPEED = 5f;
    public static final float MOVE_SPEED = 5f;

    private final Game game;
    private final OrthographicCamera camera;
    private Vector2 targetPos = null;
    private float targetZoom;
    private Runnable onTransitionFinish;
    private boolean zoomingOut;
    private boolean slow;

    public CameraTransitionHelper(Game game, OrthographicCamera camera) {
        this.game = game;
        this.camera = camera;
    }

    public void transition(Vector2 targetPos, float targetZoom, Screen targetScreen, boolean dispose) {
        transition(targetPos, targetZoom, () -> setScreen(targetScreen, dispose));
    }

    public void slowTransition(Vector2 targetPos, float targetZoom, Screen targetScreen, boolean dispose) {
        slowTransition(targetPos, targetZoom, () -> setScreen(targetScreen, dispose));
    }

    public void transition(Vector2 targetPos, float targetZoom, Runnable onTransitionFinish) {
        this.onTransitionFinish = onTransitionFinish;
        if (Options.REDUCE_MOTION.get()) {
            camera.position.set(targetPos.x, targetPos.y, 0);
            camera.zoom = targetZoom;
            camera.update();
            onTransitionFinish.run();
            return;
        }
        this.targetPos = targetPos;
        this.targetZoom = targetZoom;
        this.zoomingOut = targetZoom > camera.zoom;
    }

    public void slowTransition(Vector2 targetPos, float targetZoom, Runnable onTransitionFinish) {
        transition(targetPos, targetZoom, onTransitionFinish);
        slow = true;
    }

    public void update(float delta) {
        if (targetPos == null) return;
        Vector2 camPos2D = new Vector2(camera.position.x, camera.position.y);
        Vector2 diff = targetPos.cpy().sub(camPos2D);
        camPos2D.mulAdd(diff, Math.min(delta * MOVE_SPEED, 1f));
        camera.position.set(camPos2D.x, camPos2D.y, 0);
        float zoomDiff = targetZoom / camera.zoom;
        camera.zoom += (targetZoom - camera.zoom) * Math.min(delta * getZoomSpeed(), 1f);
        if (diff.len() < 0.1f && Math.abs(zoomDiff - 1) < 0.01f) {
            camera.zoom = targetZoom;
            camera.position.set(targetPos.x, targetPos.y, 0);
            if (onTransitionFinish != null) onTransitionFinish.run();
            targetPos = null;
            onTransitionFinish = null;
        }
        camera.update();
    }

    private float getZoomSpeed() {
        if (slow) return 2;
        else if (zoomingOut) return 5;
        else return 3;
    }

    public boolean isInTransition() {
        return targetPos != null;
    }

    public void stopTransition() {
        targetPos = null;
        onTransitionFinish = null;
    }

    private void setScreen(Screen targetScreen, boolean dispose) {
        if (targetScreen == null) return;
        Screen oldScreen = game.getScreen();
        game.setScreen(targetScreen);
        if (dispose) oldScreen.dispose();
    }
}
