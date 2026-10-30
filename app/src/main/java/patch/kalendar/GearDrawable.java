package patch.kalendar;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Ikonica zupcanika (u stilu ikonica bocnog menija: plavo punjenje, tamni obris). */
final class GearDrawable extends Drawable {
    private final int sizePx;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);

    GearDrawable(int sizePx) {
        this.sizePx = sizePx;
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0xff3da5f5);
        line.setStyle(Paint.Style.STROKE);
        line.setColor(0xff1b2a41);
        line.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        float s = Math.min(b.width(), b.height());
        float cx = b.exactCenterX();
        float cy = b.exactCenterY();
        float outer = s * 0.46f;
        float inner = s * 0.34f;
        line.setStrokeWidth(Math.max(1f, s * 0.07f));

        Path p = new Path();
        p.setFillType(Path.FillType.EVEN_ODD);
        int teeth = 8;
        boolean first = true;
        for (int i = 0; i < teeth; i++) {
            double a = i * 2 * Math.PI / teeth;
            double step = 2 * Math.PI / teeth;
            double[][] pts = {
                    { inner, a - step * 0.30 },
                    { outer, a - step * 0.17 },
                    { outer, a + step * 0.17 },
                    { inner, a + step * 0.30 },
            };
            for (double[] q : pts) {
                float x = cx + (float) (q[0] * Math.cos(q[1]));
                float y = cy + (float) (q[0] * Math.sin(q[1]));
                if (first) {
                    p.moveTo(x, y);
                    first = false;
                } else {
                    p.lineTo(x, y);
                }
            }
        }
        p.close();
        p.addCircle(cx, cy, inner * 0.45f, Path.Direction.CW);
        c.drawPath(p, fill);
        c.drawPath(p, line);
    }

    @Override
    public int getIntrinsicWidth() {
        return sizePx;
    }

    @Override
    public int getIntrinsicHeight() {
        return sizePx;
    }

    @Override
    public void setAlpha(int alpha) {
        fill.setAlpha(alpha);
        line.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        fill.setColorFilter(cf);
        line.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}