package patch.kalendar;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Generator nasumicnih tema (svijetlih ili tamnih) sa zagarantovanim kontrastom izmedju pozadina i slova
 * (WCAG omjer kontrasta). Bez Android/Xposed zavisnosti.
 */
final class RandomTheme {
    private RandomTheme() {}

    static final int ANY = 0;
    static final int LIGHT = 1;
    static final int DARK = 2;

    /** Prefiks naziva tema koje je napravio generator (takve se zamjenjuju novim nasumicnim, ako nisu preimenovane). */
    static final String NAME_PREFIX = "Насумична";

    static final class Result {
        final boolean light;
        final String name;
        final Map<String, String> colors;

        Result(boolean light, String name, Map<String, String> colors) {
            this.light = light;
            this.name = name;
            this.colors = colors;
        }
    }

    // minimalni omjeri kontrasta
    static final double MIN_TEXT = 10.0;     // glavni tekst, naslovi h5/h6
    static final double MIN_WEB_TEXT = 8.0;  // tekst knjiga i molitvi
    static final double MIN_TITLE = 6.0;     // naslovi i praznici
    static final double MIN_LINK = 6.5;      // linkovi
    static final double MIN_VISITED = 5.5;   // posjeceni linkovi
    static final double MIN_TAB = 7.0;       // slova na tabovima
    static final double MIN_ACCENT = 3.0;    // akcent, okvir danasnjeg dana, crvene linije

    static Result generate(Random r, int mode) {
        boolean light;
        if (mode == LIGHT) light = true;
        else if (mode == DARK) light = false;
        else light = r.nextBoolean();
        int dir = light ? -1 : 1; // smjer pomjeranja svjetline kad treba vise kontrasta

        // ---- osnovna nijansa i pozadine
        float h0 = r.nextFloat() * 360f;
        float bgS = light ? 0.20f + r.nextFloat() * 0.40f : 0.08f + r.nextFloat() * 0.27f;
        float bgL = light ? 0.93f + r.nextFloat() * 0.045f : 0.045f + r.nextFloat() * 0.075f;
        int bg = hsl(h0, bgS, bgL);
        int surface = hsl(h0, bgS, clamp(light ? bgL + 0.03f : bgL + 0.025f, 0f, 0.99f));
        int row = hsl(h0, Math.min(1f, bgS + 0.05f), light ? bgL - 0.045f : bgL + 0.04f);
        int month = hsl(h0 + 8f, Math.min(1f, bgS + 0.10f), light ? bgL - 0.075f : bgL + 0.055f);
        int webBg = bg;
        int[] surfaces = { bg, surface, row, webBg, month };

        // ---- trake (tabovi): uvijek tamne, bijela slova (kao u originalnoj aplikaciji)
        float hb = h0 + (r.nextFloat() - 0.5f) * 40f;
        float bs = 0.28f + r.nextFloat() * 0.25f;
        float bl = light ? 0.22f + r.nextFloat() * 0.12f : 0.17f + r.nextFloat() * 0.09f;
        int white = 0xffffffff;
        for (int i = 0; i < 60 && contrast(white, hsl(hb, bs, bl)) < MIN_TAB; i++) bl -= 0.01f;
        int bar = hsl(hb, bs, bl);
        int barDark = hsl(hb, bs, bl * 0.6f);
        int tabText = white;

        // ---- nijanse akcenata
        float ht;
        switch (r.nextInt(4)) {
            case 0: ht = h0 + 30f; break;
            case 1: ht = h0 + 180f; break;
            case 2: ht = h0 + 150f; break;
            default: ht = r.nextFloat() * 360f;
        }
        float hl = r.nextBoolean() ? 195f + r.nextFloat() * 60f : ht + 120f + r.nextFloat() * 120f;
        if (hueDist(ht, hl) < 40f) hl += 90f;
        float hv = hl + 50f + r.nextFloat() * 40f;
        float as = light ? 0.60f + r.nextFloat() * 0.35f : 0.55f + r.nextFloat() * 0.35f;

        // ---- tekstovi
        float ts = 0.05f + r.nextFloat() * 0.15f;
        int text = fit(h0, ts, light ? 0.12f : 0.90f, dir, MIN_TEXT, surfaces);
        int webText = fit(h0, ts, light ? 0.17f : 0.84f, dir, MIN_WEB_TEXT, webBg, surface);
        int head2 = fit(h0, ts, light ? 0.10f : 0.92f, dir, MIN_TEXT, webBg);
        int title = fit(ht, as, light ? 0.32f : 0.70f, dir, MIN_TITLE, surfaces);
        int link = fit(hl, as, light ? 0.32f : 0.72f, dir, MIN_LINK, webBg, surface);
        int visited = fit(hv, as * 0.8f, light ? 0.34f : 0.74f, dir, MIN_VISITED, webBg, surface);

        // ---- akcenti i linije
        int accent = fit(hl, as, light ? 0.40f : 0.62f, dir, MIN_ACCENT, bg, surface, row);
        int rowBorder = fit(hl + 20f, as, light ? 0.40f : 0.60f, dir, MIN_ACCENT, bg, row);
        int redLine = fit(ht, as, light ? 0.40f : 0.58f, dir, MIN_ACCENT, bg, surface, row, webBg);

        // crvena traka (naslov mjeseca): bijela slova na njoj (>= 4.5) i vidljiva na pozadini (>= 1.8);
        // uzima se sredina raspona svjetlina koje zadovoljavaju oba uslova
        float rs = 0.50f + r.nextFloat() * 0.25f;
        float first = -1f, last = -1f, bestL = 0.3f, bestScore = -1f;
        for (int i = 0; i <= 100; i++) {
            float l = 0.10f + i * 0.005f;
            int c = hsl(ht, rs, l);
            double wc = contrast(white, c);
            double bc = minContrast(c, bg, row, webBg);
            if (wc >= 4.5 && bc >= 1.8) {
                if (first < 0f) first = l;
                last = l;
            }
            float score = (float) Math.min(wc / 4.5, bc / 1.8);
            if (score > bestScore) {
                bestScore = score;
                bestL = l;
            }
        }
        int redBar = hsl(ht, rs, first >= 0f ? (first + last) / 2f : bestL);
        int scroll = redBar;

        int divider = blendUntil(bg, text, 0.15f, 1.35, bg, surface, row);
        int webLine = blendUntil(webBg, text, 0.28f, 1.8, webBg);

        // linija ispod aktivnog taba: nijansa akcenta, dovoljno svijetla da se vidi na traci tabova
        float tl = 0.55f;
        int tabLine = hsl(ht, 0.7f, tl);
        for (int i = 0; i < 30 && contrast(tabLine, bar) < 3.0; i++) {
            tl += 0.02f;
            tabLine = hsl(ht, 0.7f, tl);
        }

        Map<String, String> m = new LinkedHashMap<String, String>();
        put(m, "c_bg", bg);
        put(m, "c_surface", surface);
        put(m, "c_web_bg", webBg);
        put(m, "c_row", row);
        put(m, "c_row_border", rowBorder);
        put(m, "c_text", text);
        put(m, "c_title", title);
        put(m, "c_web_text", webText);
        put(m, "c_tab_text", tabText);
        put(m, "c_web_link", link);
        put(m, "c_web_visited", visited);
        put(m, "c_bar", bar);
        put(m, "c_tab_line", tabLine);
        put(m, "c_month_bar", month);
        put(m, "c_scroll", scroll);
        put(m, "c_divider", divider);
        put(m, "c_red_bar", redBar);
        put(m, "c_red_line", redLine);
        put(m, "c_bar_dark", barDark);
        put(m, "c_accent", accent);
        put(m, "c_web_head2", head2);
        put(m, "c_web_line", webLine);

        String tag = Integer.toHexString(0x1000 + r.nextInt(0xfff)).substring(1);
        String name = NAME_PREFIX + " · " + (light ? "светла" : "тамна") + " · " + tag;
        return new Result(light, name, m);
    }

    // ------------------------------------------------------------------ boje i kontrast

    private static void put(Map<String, String> m, String key, int c) {
        m.put(key, Colors.hex(c));
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static float hueDist(float a, float b) {
        float d = Math.abs(((a - b) % 360f + 360f) % 360f);
        return d > 180f ? 360f - d : d;
    }

    /** HSL (h u stepenima, s i l 0..1) -> 0xffRRGGBB */
    static int hsl(float h, float s, float l) {
        h = ((h % 360f) + 360f) % 360f;
        s = clamp(s, 0f, 1f);
        l = clamp(l, 0f, 1f);
        float c = (1f - Math.abs(2f * l - 1f)) * s;
        float hp = h / 60f;
        float x = c * (1f - Math.abs(hp % 2f - 1f));
        float r = 0f, g = 0f, b = 0f;
        if (hp < 1f) { r = c; g = x; }
        else if (hp < 2f) { r = x; g = c; }
        else if (hp < 3f) { g = c; b = x; }
        else if (hp < 4f) { g = x; b = c; }
        else if (hp < 5f) { r = x; b = c; }
        else { r = c; b = x; }
        float m = l - c / 2f;
        int ri = Math.round((r + m) * 255f);
        int gi = Math.round((g + m) * 255f);
        int bi = Math.round((b + m) * 255f);
        return 0xff000000 | (clamp255(ri) << 16) | (clamp255(gi) << 8) | clamp255(bi);
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    private static double lin(int v) {
        double c = v / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    /** Relativna luminansa (WCAG). */
    static double relLum(int c) {
        return 0.2126 * lin((c >> 16) & 255) + 0.7152 * lin((c >> 8) & 255) + 0.0722 * lin(c & 255);
    }

    /** Omjer kontrasta (1..21). */
    static double contrast(int a, int b) {
        double x = relLum(a);
        double y = relLum(b);
        if (x < y) {
            double t = x;
            x = y;
            y = t;
        }
        return (x + 0.05) / (y + 0.05);
    }

    static double minContrast(int c, int... bgs) {
        double m = Double.MAX_VALUE;
        for (int b : bgs) m = Math.min(m, contrast(c, b));
        return m;
    }

    /** Mijesa boje: t = udio boje b. */
    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1f - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1f - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1f - t) + (b & 255) * t);
        return 0xff000000 | (r << 16) | (g << 8) | bl;
    }

    /** Boja nijanse h i zasicenja s; svjetlina pocinje od l i pomjera se (dir: +1 svjetlije, -1 tamnije) dok kontrast prema svim pozadinama nije dovoljan. */
    private static int fit(float h, float s, float l, int dir, double min, int... bgs) {
        int c = hsl(h, s, l);
        for (int i = 0; i < 100 && minContrast(c, bgs) < min; i++) {
            l = clamp(l + dir * 0.01f, 0f, 1f);
            c = hsl(h, s, l);
        }
        return c;
    }

    /** Mijesa pozadinu prema tekstu (udio t, pa sve veci) dok kontrast prema svim pozadinama nije dovoljan. */
    private static int blendUntil(int base, int toward, float t, double min, int... bgs) {
        int c = mix(base, toward, t);
        for (int i = 0; i < 30 && minContrast(c, bgs) < min; i++) {
            t += 0.02f;
            c = mix(base, toward, t);
        }
        return c;
    }
}
