package patch.kalendar;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Jednobojne ikonice (crtaju se kodom, mreza 24x24) za dugmad i podmeni tema. */
final class Icons extends Drawable {
    static final int PLUS = 0;        // nova tema
    static final int DICE_NEW = 1;    // nova nasumicna tema
    static final int REFRESH = 2;     // ponovo generisi nasumicnu temu
    static final int PALETTE = 3;     // uredi boje
    static final int PENCIL = 4;      // preimenuj
    static final int TRASH = 5;       // obrisi

    private final int kind;
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

    Icons(int kind, int color) {
        this.kind = kind;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStrokeWidth(2f);
        stroke.setColor(color);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(color);
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        float s = Math.min(b.width(), b.height()) / 24f;
        c.save();
        c.translate(b.exactCenterX() - 12f * s, b.exactCenterY() - 12f * s);
        c.scale(s, s);
        switch (kind) {
            case PLUS:
                plus(c, 12f, 12f, 7f);
                break;
            case DICE_NEW:
                c.drawRoundRect(new RectF(2.5f, 8.5f, 16.5f, 22.5f), 3f, 3f, stroke);
                c.drawCircle(6.5f, 12.5f, 1.4f, fill);
                c.drawCircle(9.5f, 15.5f, 1.4f, fill);
                c.drawCircle(12.5f, 18.5f, 1.4f, fill);
                plus(c, 19f, 5f, 3.5f);
                break;
            case REFRESH:
                refresh(c);
                break;
            case PALETTE:
                c.drawCircle(12f, 12f, 9f, stroke);
                c.drawCircle(8f, 10.5f, 1.5f, fill);
                c.drawCircle(11.5f, 7.5f, 1.5f, fill);
                c.drawCircle(15.5f, 9f, 1.5f, fill);
                c.drawCircle(16.5f, 13.5f, 1.5f, fill);
                break;
            case PENCIL:
                Path p = new Path();
                p.moveTo(4f, 20f);
                p.lineTo(5f, 15f);
                p.lineTo(15f, 5f);
                p.lineTo(19f, 9f);
                p.lineTo(9f, 19f);
                p.close();
                c.drawPath(p, stroke);
                c.drawLine(12.5f, 7.5f, 16.5f, 11.5f, stroke);
                break;
            default:
                c.drawLine(4f, 7f, 20f, 7f, stroke);
                Path h = new Path();
                h.moveTo(9f, 7f);
                h.lineTo(9f, 4f);
                h.lineTo(15f, 4f);
                h.lineTo(15f, 7f);
                c.drawPath(h, stroke);
                Path body = new Path();
                body.moveTo(6f, 7f);
                body.lineTo(7f, 20f);
                body.lineTo(17f, 20f);
                body.lineTo(18f, 7f);
                c.drawPath(body, stroke);
                c.drawLine(10f, 11f, 10f, 16f, stroke);
                c.drawLine(14f, 11f, 14f, 16f, stroke);
                break;
        }
        c.restore();
    }

    private void plus(Canvas c, float cx, float cy, float arm) {
        c.drawLine(cx, cy - arm, cx, cy + arm, stroke);
        c.drawLine(cx - arm, cy, cx + arm, cy, stroke);
    }

    private void refresh(Canvas c) {
        float r = 8f;
        c.drawArc(new RectF(12f - r, 12f - r, 12f + r, 12f + r), 20f, 290f, false, stroke);
        double a = Math.toRadians(310.0);
        float ex = 12f + r * (float) Math.cos(a);
        float ey = 12f + r * (float) Math.sin(a);
        float tx = (float) -Math.sin(a);
        float ty = (float) Math.cos(a);
        float nx = (float) Math.cos(a);
        float ny = (float) Math.sin(a);
        Path t = new Path();
        t.moveTo(ex + tx * 4f, ey + ty * 4f);
        t.lineTo(ex - tx * 0.5f + nx * 3.6f, ey - ty * 0.5f + ny * 3.6f);
        t.lineTo(ex - tx * 0.5f - nx * 3.6f, ey - ty * 0.5f - ny * 3.6f);
        t.close();
        c.drawPath(t, fill);
    }

    @Override
    public void setAlpha(int alpha) {
        stroke.setAlpha(alpha);
        fill.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        stroke.setColorFilter(cf);
        fill.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
