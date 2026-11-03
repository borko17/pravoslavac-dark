package patch.kalendar;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Jednostavne ikonice nacrtane kodom (mreza 24x24), da ne trebaju resursi. Boja se zadaje pri pravljenju. */
final class Icons {
    static final int NEW = 0;          // nova tema (+)
    static final int RANDOM_NEW = 1;   // nova nasumicna tema (kocka sa +)
    static final int REGENERATE = 2;   // ponovo generisi (kruzna strelica)
    static final int EDIT_COLORS = 3;  // uredi boje (paleta)
    static final int RENAME = 4;       // preimenuj (olovka)
    static final int DELETE = 5;       // obrisi (korpa)
    static final int SUN = 6;          // svijetla
    static final int MOON = 7;         // tamna
    static final int DROP = 8;         // od moje boje (kap boje)
    static final int DROP_STAR = 9;    // od moje boje: pozadina i praznici (kap + zvijezda)
    static final int OPTIONS = 10;     // opcije teme (klizaci)
    static final int COPY = 11;        // kopiraj temu (dva lista)
    static final int COPY_ALL = 12;    // kopiraj sve teme (tri lista)
    static final int PASTE = 13;       // ucitaj teme iz klipborda (tabla sa strelicom nadolje)

    private Icons() {}

    static Drawable get(int type, int color, int sizePx) {
        return new Icon(type, color, sizePx);
    }

    private static final class Icon extends Drawable {
        private final int type;
        private final int size;
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path p = new Path();
        private final Path p2 = new Path();
        private final RectF r = new RectF();

        Icon(int type, int color, int size) {
            this.type = type;
            this.size = size;
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeJoin(Paint.Join.ROUND);
            line.setStrokeWidth(2f);
            line.setColor(color);
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(color);
        }

        @Override
        public int getIntrinsicWidth() {
            return size;
        }

        @Override
        public int getIntrinsicHeight() {
            return size;
        }

        private void plus(Canvas c, float cx, float cy, float h) {
            c.drawLine(cx - h, cy, cx + h, cy, line);
            c.drawLine(cx, cy - h, cx, cy + h, line);
        }

        /** Kap: spicasti vrh i okruglo dno. */
        private void drop(Canvas c) {
            p.reset();
            p.moveTo(12f, 3f);
            p.cubicTo(12f, 3f, 5.5f, 10f, 5.5f, 14.5f);
            r.set(5.5f, 8f, 18.5f, 21f);
            p.arcTo(r, 180f, -180f);
            p.cubicTo(18.5f, 10f, 12f, 3f, 12f, 3f);
            p.close();
            c.drawPath(p, line);
        }

        private void dot(Canvas c, float x, float y, float rad) {
            c.drawCircle(x, y, rad, fill);
        }

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            float s = Math.min(b.width(), b.height()) / 24f;
            c.save();
            c.translate(b.left + (b.width() - 24f * s) / 2f, b.top + (b.height() - 24f * s) / 2f);
            c.scale(s, s);
            switch (type) {
                case NEW:
                    plus(c, 12f, 12f, 7f);
                    break;
                case RANDOM_NEW:
                    // kocka sa pet tackica i mali + u gornjem desnom uglu
                    r.set(3f, 8f, 16f, 21f);
                    c.drawRoundRect(r, 3f, 3f, line);
                    dot(c, 6.5f, 11.5f, 1.2f);
                    dot(c, 12.5f, 11.5f, 1.2f);
                    dot(c, 9.5f, 14.5f, 1.2f);
                    dot(c, 6.5f, 17.5f, 1.2f);
                    dot(c, 12.5f, 17.5f, 1.2f);
                    plus(c, 19f, 5f, 3f);
                    break;
                case REGENERATE: {
                    // kruzni luk sa strelicom na kraju (u smjeru kazaljke)
                    r.set(4.5f, 4.5f, 19.5f, 19.5f);
                    float start = 40f;
                    float sweep = 280f;
                    p.reset();
                    p.addArc(r, start, sweep);
                    c.drawPath(p, line);
                    double a = Math.toRadians(start + sweep);
                    float ex = 12f + 7.5f * (float) Math.cos(a);
                    float ey = 12f + 7.5f * (float) Math.sin(a);
                    float tx = -(float) Math.sin(a);   // tangenta (smjer kretanja)
                    float ty = (float) Math.cos(a);
                    float nx = (float) Math.cos(a);    // radijalni pravac
                    float ny = (float) Math.sin(a);
                    p.reset();
                    p.moveTo(ex + tx * 3.4f, ey + ty * 3.4f);
                    p.lineTo(ex + nx * 3.4f, ey + ny * 3.4f);
                    p.lineTo(ex - nx * 3.4f, ey - ny * 3.4f);
                    p.close();
                    c.drawPath(p, fill);
                    break;
                }
                case OPTIONS:
                    // tri klizaca sa tackama
                    c.drawLine(4f, 6f, 20f, 6f, line);
                    c.drawLine(4f, 12f, 20f, 12f, line);
                    c.drawLine(4f, 18f, 20f, 18f, line);
                    c.drawCircle(8f, 6f, 2.4f, fill);
                    c.drawCircle(16f, 12f, 2.4f, fill);
                    c.drawCircle(10f, 18f, 2.4f, fill);
                    break;
                case EDIT_COLORS:
                    // paleta: krug sa tackicama boje
                    c.drawCircle(12f, 12f, 9f, line);
                    dot(c, 8.2f, 10.2f, 1.4f);
                    dot(c, 12f, 7.6f, 1.4f);
                    dot(c, 15.8f, 10.2f, 1.4f);
                    dot(c, 8.8f, 14.8f, 1.4f);
                    break;
                case RENAME:
                    // olovka
                    p.reset();
                    p.moveTo(4f, 20f);
                    p.lineTo(5.2f, 15.2f);
                    p.lineTo(15.7f, 4.7f);
                    p.lineTo(19.3f, 8.3f);
                    p.lineTo(8.8f, 18.8f);
                    p.close();
                    c.drawPath(p, line);
                    c.drawLine(13.6f, 6.8f, 17.2f, 10.4f, line);
                    break;
                case DELETE:
                    // korpa za otpatke
                    c.drawLine(4f, 7f, 20f, 7f, line);
                    p.reset();
                    p.moveTo(9f, 7f);
                    p.lineTo(9f, 4.5f);
                    p.lineTo(15f, 4.5f);
                    p.lineTo(15f, 7f);
                    c.drawPath(p, line);
                    p.reset();
                    p.moveTo(6f, 7f);
                    p.lineTo(7f, 20f);
                    p.lineTo(17f, 20f);
                    p.lineTo(18f, 7f);
                    c.drawPath(p, line);
                    c.drawLine(10.2f, 10.5f, 10.2f, 16.5f, line);
                    c.drawLine(13.8f, 10.5f, 13.8f, 16.5f, line);
                    break;
                case SUN:
                    c.drawCircle(12f, 12f, 4.2f, line);
                    for (int i = 0; i < 8; i++) {
                        double a = Math.toRadians(i * 45.0);
                        float cs = (float) Math.cos(a);
                        float sn = (float) Math.sin(a);
                        c.drawLine(12f + 7.2f * cs, 12f + 7.2f * sn, 12f + 9.6f * cs, 12f + 9.6f * sn, line);
                    }
                    break;
                case MOON:
                    // polumjesec: veliki krug bez manjeg krug
                    p.reset();
                    p.addCircle(12f, 12f, 8.5f, Path.Direction.CW);
                    p2.reset();
                    p2.addCircle(17f, 8.5f, 7f, Path.Direction.CW);
                    p.op(p2, Path.Op.DIFFERENCE);
                    c.drawPath(p, line);
                    break;
                case DROP:
                    drop(c);
                    break;
                case DROP_STAR:
                    c.save();
                    c.translate(-1.5f, 1.5f);
                    drop(c);
                    c.restore();
                    // zvijezda u gornjem desnom uglu
                    p.reset();
                    for (int i = 0; i < 10; i++) {
                        double a = Math.toRadians(-90.0 + i * 36.0);
                        float rr = (i % 2 == 0) ? 3.4f : 1.4f;
                        float x = 19f + rr * (float) Math.cos(a);
                        float y = 5f + rr * (float) Math.sin(a);
                        if (i == 0) p.moveTo(x, y);
                        else p.lineTo(x, y);
                    }
                    p.close();
                    c.drawPath(p, fill);
                    break;
                case COPY:
                    // dva lista: prednji i zadnji (vidljiv samo gornji i desni rub)
                    r.set(3f, 8f, 16f, 21f);
                    c.drawRoundRect(r, 2.5f, 2.5f, line);
                    p.reset();
                    p.moveTo(8f, 8f);
                    p.lineTo(8f, 3f);
                    p.lineTo(21f, 3f);
                    p.lineTo(21f, 16f);
                    p.lineTo(16f, 16f);
                    c.drawPath(p, line);
                    break;
                case COPY_ALL:
                    // tri lista jedan iza drugog
                    r.set(2.5f, 9f, 15f, 21.5f);
                    c.drawRoundRect(r, 2.5f, 2.5f, line);
                    p.reset();
                    p.moveTo(6.5f, 9f);
                    p.lineTo(6.5f, 6f);
                    p.lineTo(18.5f, 6f);
                    p.lineTo(18.5f, 18f);
                    p.lineTo(15f, 18f);
                    c.drawPath(p, line);
                    p.reset();
                    p.moveTo(10f, 6f);
                    p.lineTo(10f, 2.5f);
                    p.lineTo(21.5f, 2.5f);
                    p.lineTo(21.5f, 14f);
                    p.lineTo(18.5f, 14f);
                    c.drawPath(p, line);
                    break;
                case PASTE:
                    // tabla sa stezaljkom i strelicom nadolje (ucitaj)
                    r.set(5f, 5f, 19f, 21f);
                    c.drawRoundRect(r, 2.5f, 2.5f, line);
                    r.set(9f, 3f, 15f, 7f);
                    c.drawRoundRect(r, 1.5f, 1.5f, line);
                    c.drawLine(12f, 10.5f, 12f, 17f, line);
                    c.drawLine(8.8f, 13.8f, 12f, 17f, line);
                    c.drawLine(15.2f, 13.8f, 12f, 17f, line);
                    break;
                default:
                    break;
            }
            c.restore();
        }

        @Override
        public void setAlpha(int alpha) {
            line.setAlpha(alpha);
            fill.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            line.setColorFilter(cf);
            fill.setColorFilter(cf);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
