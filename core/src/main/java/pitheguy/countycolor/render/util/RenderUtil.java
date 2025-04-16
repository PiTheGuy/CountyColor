package pitheguy.countycolor.render.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.ShortArray;
import pitheguy.countycolor.render.PolygonCollection;

import java.util.List;
import java.util.function.BiPredicate;

import static pitheguy.countycolor.render.util.RenderConst.OUTLINE_THICKNESS;
import static pitheguy.countycolor.render.util.RenderConst.RENDER_SIZE;

public class RenderUtil {

    public static boolean pointInPolygon(Vector2 point, List<Vector2> polygon) {
        boolean inside = false;
        int n = polygon.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Vector2 pi = polygon.get(i);
            Vector2 pj = polygon.get(j);
            if ((pi.y > point.y) != (pj.y > point.y) &&
                (point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x)) {
                inside = !inside;
            }
        }
        return inside;
    }

    public static void drawThickPolyline(ShapeRenderer renderer, List<Vector2> points, float thickness) {
        drawThickPolyline(renderer, points, thickness, true);
    }

    public static void drawThickPolyline(ShapeRenderer renderer, List<Vector2> points, float thickness, boolean connect) {
        drawThickPolyline(renderer, points, thickness, connect, (p1, p2) -> true);
    }

    public static void drawThickPolyline(ShapeRenderer renderer, List<Vector2> points, float thickness, boolean connect, BiPredicate<Vector2, Vector2> filter) {
        Vector2 lastV3 = null, lastV4 = null;
        for (int i = 0; i < (connect ? points.size() : points.size() - 1); i++) {
            Vector2 p1 = points.get(i);
            Vector2 p2 = points.get((i + 1) % points.size());
            if (p1.equals(p2)) p2 = points.get((i + 2) % points.size());
            if (!filter.test(p1, p2)) {
                lastV3 = null;
                lastV4 = null;
                continue;
            }
            Vector2 direction = p2.cpy().sub(p1).nor();
            Vector2 perpendicular = new Vector2(-direction.y, direction.x).scl(thickness / 2f);

            Vector2 v1 = p1.cpy().scl(RENDER_SIZE / 2f).add(perpendicular);
            Vector2 v2 = p1.cpy().scl(RENDER_SIZE / 2f).sub(perpendicular);
            Vector2 v3 = p2.cpy().scl(RENDER_SIZE / 2f).sub(perpendicular);
            Vector2 v4 = p2.cpy().scl(RENDER_SIZE / 2f).add(perpendicular);
            renderer.triangle(v1.x, v1.y, v2.x, v2.y, v3.x, v3.y);
            renderer.triangle(v3.x, v3.y, v4.x, v4.y, v1.x, v1.y);
            if (lastV3 != null) {
                Vector2 prevDirection = lastV4.cpy().sub(lastV3).nor();
                float cross = prevDirection.x * direction.y - prevDirection.y * direction.x;
                Vector2 outer = cross > 0 ? v1 : v2;
                Vector2 inner = cross > 0 ? v2 : v1;
                Vector2 prevOuter = cross > 0 ? lastV4 : lastV3;
                Vector2 prevInner = cross > 0 ? lastV3 : lastV4;
                renderer.triangle(prevOuter.x, prevOuter.y, p1.x * RENDER_SIZE / 2, p1.y * RENDER_SIZE / 2, outer.x, outer.y);
                renderer.triangle(prevInner.x, prevInner.y, p1.x * RENDER_SIZE / 2, p1.y * RENDER_SIZE / 2, inner.x, inner.y);
            }
            lastV3 = v3;
            lastV4 = v4;
        }
    }

    public static void drawThickPolylineCulled(OrthographicCamera camera, ShapeRenderer renderer, List<Vector2> points, float thickness, boolean connect) {
        drawThickPolyline(renderer, points, thickness, connect, (p1, p2) -> isVisibleToCamera(camera, p1, p2, true));
    }

    public static boolean isVisibleToCamera(OrthographicCamera camera, Vector2 point1, Vector2 point2, boolean scale) {
        float camMinX = camera.position.x - (camera.viewportWidth * camera.zoom) / 2f;
        float camMaxX = camera.position.x + (camera.viewportWidth * camera.zoom) / 2f;
        float camMinY = camera.position.y - (camera.viewportHeight * camera.zoom) / 2f;
        float camMaxY = camera.position.y + (camera.viewportHeight * camera.zoom) / 2f;
        float scaleAmt = scale ? RENDER_SIZE / 2f : 1;
        float minX = Math.min(point1.x, point2.x);
        float maxX = Math.max(point1.x, point2.x);
        float minY = Math.min(point1.y, point2.y);
        float maxY = Math.max(point1.y, point2.y);
        return maxX * scaleAmt + OUTLINE_THICKNESS >= camMinX && minX * scaleAmt - OUTLINE_THICKNESS <= camMaxX &&
               maxY * scaleAmt + OUTLINE_THICKNESS >= camMinY && minY * scaleAmt - OUTLINE_THICKNESS <= camMaxY;
    }


    public static ShortArray triangulate(List<Vector2> points) {
        float[] vertices = new float[points.size() * 2];
        for (int i = 0; i < points.size(); i++) {
            vertices[i * 2] = points.get(i).x;
            vertices[i * 2 + 1] = points.get(i).y;
        }
        EarClippingTriangulator triangulator = new EarClippingTriangulator();
        return triangulator.computeTriangles(vertices);
    }

    public static void renderFilledPolygon(ShapeRenderer renderer, List<Vector2> points, ShortArray triangles, float scale) {
        float[] vertices = new float[points.size() * 2];
        for (int i = 0; i < points.size(); i++) {
            vertices[i * 2] = points.get(i).x;
            vertices[i * 2 + 1] = points.get(i).y;
        }
        for (int i = 0; i < triangles.size; i += 3) {
            int i1 = triangles.get(i) * 2;
            int i2 = triangles.get(i + 1) * 2;
            int i3 = triangles.get(i + 2) * 2;
            renderer.triangle(
                vertices[i1] * RENDER_SIZE / 2 * scale, vertices[i1 + 1] * RENDER_SIZE / 2 * scale,
                vertices[i2] * RENDER_SIZE / 2 * scale, vertices[i2 + 1] * RENDER_SIZE / 2 * scale,
                vertices[i3] * RENDER_SIZE / 2 * scale, vertices[i3 + 1] * RENDER_SIZE / 2 * scale
            );
        }
    }

    public static float calculatePerimeter(List<Vector2> points) {
        float result = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            Vector2 p1 = points.get(i);
            Vector2 p2 = points.get(i + 1);
            float dx = p2.x - p1.x;
            float dy = p2.y - p1.y;
            result += (float) Math.sqrt(dx * dx + dy * dy);
        }
        Vector2 p1 = points.get(points.size() - 1);
        Vector2 p2 = points.get(0);
        float dx = p2.x - p1.x;
        float dy = p2.y - p1.y;
        result += (float) Math.sqrt(dx * dx + dy * dy);
        return result;
    }

    public static float calculateArea(List<Vector2> points) {
        float area = 0f;
        int n = points.size();
        for (int i = 0; i < n; i++) {
            Vector2 current = points.get(i);
            Vector2 next = points.get((i + 1) % n);  // Wrap around to the first vertex
            area += current.x * next.y - next.x * current.y;
        }
        return Math.abs(area) / 2f;
    }

    public static float getTextWidth(BitmapFont font, String text) {
        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, text);
        return layout.width;
    }

    public static float getTextHeight(BitmapFont font, String text) {
        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, text);
        return layout.height;
    }

    public static void fixRollover(PolygonCollection polygons) {
        for (List<Vector2> points : polygons.getPolygons())
            for (Vector2 point : points)
                if (point.x > 0) point.add(-360, 0);
        polygons.recalculateBounds();
    }

    public static Vector2 getMouseWorldCoords(Camera camera) {
        Vector3 mouseWorld = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        return new Vector2(mouseWorld.x, mouseWorld.y);
    }
}
