package pitheguy.countycolor.render.util;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ShortArray;

import java.util.List;

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

    public static void drawThickPolyline(ShapeRenderer renderer, List<Vector2> points, float thickness, int size) {
        Vector2 lastV3 = null, lastV4 = null;
        for (int i = 0; i < points.size() - 1; i++) {
            Vector2 p1 = points.get(i);
            Vector2 p2 = points.get(i + 1);
            Vector2 direction = p2.cpy().sub(p1).nor();
            Vector2 perpendicular = new Vector2(-direction.y, direction.x).scl(thickness / 2f);

            Vector2 v1 = p1.cpy().scl(size / 2f).add(perpendicular);
            Vector2 v2 = p1.cpy().scl(size / 2f).sub(perpendicular);
            Vector2 v3 = p2.cpy().scl(size / 2f).sub(perpendicular);
            Vector2 v4 = p2.cpy().scl(size / 2f).add(perpendicular);
            renderer.triangle(v1.x, v1.y, v2.x, v2.y, v3.x, v3.y);
            renderer.triangle(v3.x, v3.y, v4.x, v4.y, v1.x, v1.y);
            if (lastV3 != null) {
                Vector2 prevDirection = lastV4.cpy().sub(lastV3).nor();
                float cross = prevDirection.x * direction.y - prevDirection.y * direction.x;
                Vector2 outer = cross > 0 ? v1 : v2;
                Vector2 inner = cross > 0 ? v2 : v1;
                Vector2 prevOuter = cross > 0 ? lastV4 : lastV3;
                Vector2 prevInner = cross > 0 ? lastV3 : lastV4;
                renderer.triangle(prevOuter.x, prevOuter.y, p1.x * size / 2, p1.y * size / 2, outer.x, outer.y);
                renderer.triangle(prevInner.x, prevInner.y, p1.x * size / 2, p1.y * size / 2, inner.x, inner.y);
            }
            lastV3 = v3;
            lastV4 = v4;
        }
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

    public static void renderFilledPolygon(ShapeRenderer renderer, List<Vector2> points, float scale) {
        renderFilledPolygon(renderer, points, triangulate(points), scale);
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
                vertices[i1] * RenderConst.RENDER_SIZE / 2 * scale, vertices[i1 + 1] * RenderConst.RENDER_SIZE / 2 * scale,
                vertices[i2] * RenderConst.RENDER_SIZE / 2 * scale, vertices[i2 + 1] * RenderConst.RENDER_SIZE / 2 * scale,
                vertices[i3] * RenderConst.RENDER_SIZE / 2 * scale, vertices[i3 + 1] * RenderConst.RENDER_SIZE / 2 * scale
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
}
