package patch.kalendar;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedBridge;

/** Pomocne funkcije za pretvaranje svijetlih boja u tamnu temu. */
final class Dark {
    private Dark() {}

    static final int BG = 0xff121212;
    static final int SURFACE = 0xff1a1a18;
    static final int BAR = 0xff1f2a3f;
    static final int BAR_DARK = 0xff0f1626;
    static final int TEXT = 0xffe0dcd0;
    static final int ACCENT = 0xff6495ed;
    static final int DIVIDER = 0xff333333;

    private static ColorMatrixColorFilter INVERT;

    private static int Color_r(int c) { return (c >> 16) & 255; }
    private static int Color_g(int c) { return (c >> 8) & 255; }
    private static int Color_b(int c) { return c & 255; }

    private static int clamp(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }

    /** Relativna svjetlina boje (0..1). */
    static float lum(int c) {
        return (0.299f * Color_r(c) + 0.587f * Color_g(c) + 0.114f * Color_b(c)) / 255f;
    }

    /** Prosjecna svjetlina sredine drawable-a; -1 ako je skoro providan ili dode do greske. */
    private static float drawableLum(Drawable d) {
        try {
            Rect old = d.copyBounds();
            Bitmap bm = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bm);
            d.setBounds(0, 0, 32, 32);
            d.draw(cv);
            d.setBounds(old);
            float sum = 0f;
            float wsum = 0f;
            for (int y = 12; y < 20; y++) {
                for (int x = 12; x < 20; x++) {
                    int px = bm.getPixel(x, y);
                    float a = (px >>> 24) / 255f;
                    sum += lum(px) * a;
                    wsum += a;
                }
            }
            bm.recycle();
            if (wsum / 64f < 0.5f) return -1f;
            return sum / wsum;
        } catch (Throwable t) {
            return -1f;
        }
    }

    private static ColorMatrixColorFilter invert() {
        if (INVERT == null) {
            ColorMatrix a = new ColorMatrix(new float[] {
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f });
            ColorMatrix b = new ColorMatrix(new float[] {
                -0.574f, 1.43f, 0.144f, 0f, 0f,
                0.426f, 0.43f, 0.144f, 0f, 0f,
                0.426f, 1.43f, -0.856f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f });
            a.postConcat(b);
            INVERT = new ColorMatrixColorFilter(a);
        }
        return INVERT;
    }

    private static float[] toHsl(int c) {
        float r = Color_r(c) / 255f;
        float g = Color_g(c) / 255f;
        float b = Color_b(c) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float l = (max + min) / 2f;
        float d = max - min;
        float h = 0f;
        float s = 0f;
        if (d > 0f) {
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
            if (max == r) {
                h = (g - b) / d + (g < b ? 6 : 0);
            } else if (max == g) {
                h = (b - r) / d + 2f;
            } else {
                h = (r - g) / d + 4f;
            }
            h *= 60f;
        }
        return new float[] { h, s, l };
    }

    private static int fromHsl(float h, float s, float l, int a) {
        float c = (1f - Math.abs(2f * l - 1f)) * s;
        float hp = (h % 360f) / 60f;
        float x = (1f - Math.abs(hp % 2f - 1f)) * c;
        float r;
        float g;
        float b;
        if (hp < 1f) { r = c; g = x; b = 0f; }
        else if (hp < 2f) { r = x; g = c; b = 0f; }
        else if (hp < 3f) { r = 0f; g = c; b = x; }
        else if (hp < 4f) { r = 0f; g = x; b = c; }
        else if (hp < 5f) { r = x; g = 0f; b = c; }
        else { r = c; g = 0f; b = x; }
        float m = l - c / 2f;
        int ri = Math.round((r + m) * 255f);
        int gi = Math.round((g + m) * 255f);
        int bi = Math.round((b + m) * 255f);
        return (a << 24) | (clamp(ri) << 16) | (clamp(gi) << 8) | clamp(bi);
    }

    /** Svijetla pozadina -> tamna (zadrzava nijansu). */
    static int mapBg(int c) {
        int a = c >>> 24;
        if (a < 20) return c;
        float[] hsl = toHsl(c);
        if (hsl[2] < 0.6f) return c;
        return fromHsl(hsl[0], hsl[1] * 0.35f, 0.07f + (1f - hsl[2]) * 0.45f, a);
    }

    /** Tamna boja teksta -> svijetla. */
    static int mapFg(int c) {
        if (lum(c) >= 0.5f) return c;
        int a = c >>> 24;
        float[] hsl = toHsl(c);
        if (hsl[1] < 0.2f) return (a << 24) | 0xe0dcd0;
        return fromHsl(hsl[0], Math.min(1f, hsl[1]) * 0.9f, 0.72f, a);
    }

    static ColorStateList mapCsl(ColorStateList csl) {
        if (csl == null) return null;
        try {
            Field specsF = ColorStateList.class.getDeclaredField("mStateSpecs");
            Field colorsF = ColorStateList.class.getDeclaredField("mColors");
            specsF.setAccessible(true);
            colorsF.setAccessible(true);
            int[][] specs = (int[][]) specsF.get(csl);
            int[] colors = (int[]) colorsF.get(csl);
            int[] out = new int[colors.length];
            boolean changed = false;
            for (int i = 0; i < colors.length; i++) {
                out[i] = mapFg(colors[i]);
                if (out[i] != colors[i]) changed = true;
            }
            if (!changed) return csl;
            return new ColorStateList(specs, out);
        } catch (Throwable t) {
            int def = csl.getDefaultColor();
            int m = mapFg(def);
            if (m != def) return ColorStateList.valueOf(m);
            return csl;
        }
    }

    static void fixBackground(View v) {
        Drawable d = v.getBackground();
        if (d == null) return;
        if (d instanceof ColorDrawable) {
            int c = ((ColorDrawable) d).getColor();
            int m = mapBg(c);
            if (m != c) v.setBackgroundColor(m);
            return;
        }
        if (drawableLum(d) < 0.6f) return;
        if (d instanceof BitmapDrawable && ((BitmapDrawable) d).getIntrinsicWidth() > 400) {
            v.setBackgroundColor(BG);
            return;
        }
        d.mutate().setColorFilter(invert());
    }

    private static void fixOne(View v) {
        fixBackground(v);
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            ColorStateList cur = tv.getTextColors();
            ColorStateList mapped = mapCsl(cur);
            if (mapped != cur) tv.setTextColor(mapped);
            ColorStateList hint = tv.getHintTextColors();
            if (hint != null) {
                ColorStateList mh = mapCsl(hint);
                if (mh != hint) tv.setHintTextColor(mh);
            }
        }
        if (v instanceof AbsListView) {
            Drawable sel = ((AbsListView) v).getSelector();
            if (sel != null && drawableLum(sel) >= 0.6f) {
                sel.mutate().setColorFilter(invert());
            }
            if (v instanceof ListView) {
                ListView lv = (ListView) v;
                Drawable div = lv.getDivider();
                if (div instanceof ColorDrawable) {
                    int col = ((ColorDrawable) div).getColor();
                    if (lum(col) < 0.12f) {
                        int h = lv.getDividerHeight();
                        lv.setDivider(new ColorDrawable(0xff3c3c3c));
                        lv.setDividerHeight(h);
                    }
                }
            }
        }
    }

    /** Prolazi kroz hijerarhiju view-ova i popravlja boje. */
    static void walk(View v) {
        try {
            fixOne(v);
            if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) {
                    walk(g.getChildAt(i));
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /** Popravlja ForegroundColorSpan / BackgroundColorSpan u tekstu. */
    static CharSequence fixSpans(CharSequence cs) {
        if (!(cs instanceof Spanned)) return cs;
        Spanned sp = (Spanned) cs;
        int n = sp.length();
        if (n == 0) return cs;
        ForegroundColorSpan[] fg = sp.getSpans(0, n, ForegroundColorSpan.class);
        BackgroundColorSpan[] bg = sp.getSpans(0, n, BackgroundColorSpan.class);
        boolean changed = false;
        for (ForegroundColorSpan f : fg) {
            if (mapFg(f.getForegroundColor()) != f.getForegroundColor()) { changed = true; break; }
        }
        if (!changed) {
            for (BackgroundColorSpan b : bg) {
                if (mapBg(b.getBackgroundColor()) != b.getBackgroundColor()) { changed = true; break; }
            }
        }
        if (!changed) return cs;
        SpannableStringBuilder sb = new SpannableStringBuilder(sp);
        for (ForegroundColorSpan f : sb.getSpans(0, n, ForegroundColorSpan.class)) {
            int col = f.getForegroundColor();
            int m = mapFg(col);
            if (m != col) {
                int s = sb.getSpanStart(f);
                int e = sb.getSpanEnd(f);
                int fl = sb.getSpanFlags(f);
                sb.removeSpan(f);
                sb.setSpan(new ForegroundColorSpan(m), s, e, fl);
            }
        }
        for (BackgroundColorSpan b : sb.getSpans(0, n, BackgroundColorSpan.class)) {
            int col = b.getBackgroundColor();
            int m = mapBg(col);
            if (m != col) {
                int s = sb.getSpanStart(b);
                int e = sb.getSpanEnd(b);
                int fl = sb.getSpanFlags(b);
                sb.removeSpan(b);
                sb.setSpan(new BackgroundColorSpan(m), s, e, fl);
            }
        }
        return sb;
    }
}
