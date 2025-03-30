package pitheguy.countycolor.render;

import com.badlogic.gdx.math.Vector2;

import java.util.Objects;

public final class Zoom {
    private final Vector2 center;
    private final float zoom;

    public Zoom(Vector2 center, float zoom) {
        this.center = center;
        this.zoom = zoom;
    }

    public Vector2 center() {
        return center;
    }

    public float zoom() {
        return zoom;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        Zoom that = (Zoom) obj;
        return Objects.equals(this.center, that.center) && this.zoom == that.zoom;
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, zoom);
    }

    @Override
    public String toString() {
        return "Zoom[" +
               "center=" + center + ", " +
               "zoom=" + zoom + ']';
    }

}
