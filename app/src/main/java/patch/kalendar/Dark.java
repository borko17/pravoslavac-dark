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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedBridge;

/** Pomocne funkcije za pretvaranje svijetlih boja u tamnu temu. */
final class Dark {
    private Dark() {}

    static int BG = 0xff121212;
    static int SURFACE = 0xff1a1a18;
    static int BAR = 0xff1f2a3f;
    static int BAR_DARK = 0xff0f1626;
    static int TEXT = 0xffe0dcd0;
    static int ACCENT = 0xff6495ed;
    static int DIVIDER = 0xff333333;
    static int RED_LINE = 0xffdc143c;
    static int ROW = 0xff1f1f1d;
    static int ROW_BORDER = 0xff2f5f99;
    static int TITLE = 0xffff7a66;
    static int HOLIDAY = 0xffff8a7a;
    static int SUB_TEXT = 0xffd5dbe0;
    static int MENU_TEXT = 0xfff58f92;
    static int SUB_TEXT1 = 0xffd5dbe0;
    static int BTN_BG = 0xffb02e31;
    static int BTN_TEXT = 0xffffffff;
    static int BTN_BORDER = 0xffe99a96;
    static int ARROWS = 0xffffffff;
    static int CAL_ICON = 0xff3d8fd1;
    static int DAY_DATE = 0xffe1e6ea;
    static int DAY_NAME = 0xffe1e6ea;
    static int MEAT = 0xffe1e6ea;
    static int FAST = 0xffff8a7a;
    static int TOOLBAR = 0xff212121;
    static int TOOLBAR_TEXT = 0xffffffff;
    static int ARROW_PRESSED = 0xff4a4d55;
    static int MENU_DIVIDER = 0xff353637;
    static int CAL_TITLE_LINE = 0xff2e3034;
    static int RED_BAR = 0xff8b0000;
    static int SCROLL = 0xff8b0000;
    static int WEB_BG = 0xff121212;
    static int WEB_TEXT = 0xffd9d4c7;
    /** Tekst stranica u WebView-u (zitija, knjige, molitve); WEB_TEXT je samo za spisak knjiga i molitvi. */
    static int WEB_BODY = 0xffd9d4c7;
    /** Podesavanja modula: pozadina, tekst i boja prekidaca (ne diraju kalendar). */
    static int SET_BG = 0xff121212;
    static int SET_TEXT = 0xffe1e6ea;
    static int SET_SWITCH = 0xff6495ed;
    static int WEB_HEAD2 = 0xffe6e1d6;
    static int WEB_LINK = 0xff8ab4f8;
    static int WEB_VISITED = 0xffc4a5f0;
    static int WEB_MARK = 0xff77f877;
    static int WEB_LINE = 0xff444444;
    static int MONTH_BAR = 0xff1d1a13;
    static int TAB_TEXT = 0xffffffff;
    static int TAB_LINE = 0xff33b5e5;
    static int SUN_BG = 0xff1b1c1e;
    static int SUN_TEXT = 0xfff07a7d;
    static int MONTH_TEXT = 0xfff07a7d;
    static int CAL_TITLE_BG = 0xff121212;
    static int CAL_TITLE_TEXT = 0xfff07a7d;

    /** Ucitava boje iz postavki (prilagodjena tema) ili podrazumijevane. */
    static void reload() {
        BG = Prefs.color("c_bg");
        SURFACE = Prefs.color("c_surface");
        BAR = Prefs.color("c_bar");
        BAR_DARK = Prefs.color("c_bar_dark");
        TEXT = Prefs.color("c_text");
        ACCENT = Prefs.color("c_accent");
        DIVIDER = Prefs.color("c_divider");
        RED_LINE = Prefs.color("c_red_line");
        ROW = Prefs.color("c_row");
        ROW_BORDER = Prefs.color("c_row_border");
        TITLE = Prefs.color("c_title");
        HOLIDAY = Prefs.color("c_holiday");
        SUB_TEXT = Prefs.color("c_sub_text");
        MENU_TEXT = Prefs.color("c_menu_text");
        SUB_TEXT1 = Prefs.color("c_sub_text1");
        BTN_BG = Prefs.color("c_btn_bg");
        BTN_TEXT = Prefs.color("c_btn_text");
        BTN_BORDER = Prefs.color("c_btn_border");
        ARROWS = Prefs.color("c_arrows");
        CAL_ICON = Prefs.color("c_cal_icon");
        DAY_DATE = Prefs.color("c_day_date");
        DAY_NAME = Prefs.color("c_day_name");
        MEAT = Prefs.color("c_meat");
        FAST = Prefs.color("c_fast");
        TOOLBAR = Prefs.color("c_toolbar");
        TOOLBAR_TEXT = Prefs.color("c_toolbar_text");
        ARROW_PRESSED = Prefs.color("c_arrow_pressed");
        MENU_DIVIDER = Prefs.color("c_menu_divider");
        CAL_TITLE_LINE = Prefs.color("c_cal_title_line");
        RED_BAR = Prefs.color("c_red_bar");
        SCROLL = Prefs.color("c_scroll");
        WEB_BG = Prefs.color("c_web_bg");
        WEB_TEXT = Prefs.color("c_web_text");
        WEB_BODY = Prefs.color("c_web_body");
        SET_BG = Prefs.color("c_set_bg");
        SET_TEXT = Prefs.color("c_set_text");
        SET_SWITCH = Prefs.color("c_switch");
        WEB_HEAD2 = Prefs.color("c_web_head2");
        WEB_LINK = Prefs.color("c_web_link");
        WEB_VISITED = Prefs.color("c_web_visited");
        WEB_MARK = Prefs.color("c_web_mark");
        WEB_LINE = Prefs.color("c_web_line");
        MONTH_BAR = Prefs.color("c_month_bar");
        TAB_TEXT = Prefs.color("c_tab_text");
        TAB_LINE = Prefs.color("c_tab_line");
        SUN_BG = Prefs.color("c_sun_bg");
        SUN_TEXT = Prefs.color("c_sun_text");
        MONTH_TEXT = Prefs.color("c_month_text");
        CAL_TITLE_BG = Prefs.color("c_cal_title_bg");
        CAL_TITLE_TEXT = Prefs.color("c_cal_title_text");
    }

    /** Svijetla tema: pozadina teme je svijetla (tada se originalne svijetle boje ne pretvaraju u tamne). */
    static boolean light() {
        return lum(BG) >= 0.5f;
    }

    /** Mijesa boje a i b (t = udio boje b), alfa = 255. */
    static int mix(int a, int b, float t) {
        int r = Math.round(Color_r(a) * (1f - t) + Color_r(b) * t);
        int g = Math.round(Color_g(a) * (1f - t) + Color_g(b) * t);
        int bl = Math.round(Color_b(a) * (1f - t) + Color_b(b) * t);
        return 0xff000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(bl);
    }

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

    /** Prosjecna boja sredine drawable-a (alfa 255); 0 ako je skoro providan ili dode do greske. */
    private static int drawableAvg(Drawable d) {
        try {
            Rect old = d.copyBounds();
            Bitmap bm = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bm);
            d.setBounds(0, 0, 32, 32);
            d.draw(cv);
            d.setBounds(old);
            float r = 0f;
            float g = 0f;
            float b = 0f;
            float wsum = 0f;
            for (int y = 12; y < 20; y++) {
                for (int x = 12; x < 20; x++) {
                    int px = bm.getPixel(x, y);
                    float a = (px >>> 24) / 255f;
                    r += Color_r(px) * a;
                    g += Color_g(px) * a;
                    b += Color_b(px) * a;
                    wsum += a;
                }
            }
            bm.recycle();
            if (wsum / 64f < 0.5f) return 0;
            return 0xff000000 | (clamp(Math.round(r / wsum)) << 16) | (clamp(Math.round(g / wsum)) << 8)
                    | clamp(Math.round(b / wsum));
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Prosjecna svjetlina sredine drawable-a; -1 ako je skoro providan ili dode do greske. */
    private static float drawableLum(Drawable d) {
        int avg = drawableAvg(d);
        return avg == 0 ? -1f : lum(avg);
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

    /**
     * Da li je boja (tacno) jedna od pozadinskih boja same teme. Takve boje je korisnik sam zadao (ukucao ili izabrao),
     * pa se ne smiju ponovo pretvarati kao originalne svijetle boje aplikacije (inace npr. svijetla pozadina menija
     * postane pozadina kalendara).
     */
    static boolean isThemeBg(int c) {
        if ((c >>> 24) != 0xff) return false;
        int rgb = c & 0xffffff;
        return rgb == (BG & 0xffffff) || rgb == (SURFACE & 0xffffff) || rgb == (ROW & 0xffffff)
                || rgb == (WEB_BG & 0xffffff) || rgb == (TOOLBAR & 0xffffff) || rgb == (SUN_BG & 0xffffff)
                || rgb == (MONTH_BAR & 0xffffff) || rgb == (CAL_TITLE_BG & 0xffffff) || rgb == (BAR & 0xffffff)
                || rgb == (BAR_DARK & 0xffffff) || rgb == (BTN_BG & 0xffffff) || rgb == (ARROW_PRESSED & 0xffffff);
    }

    /** Svijetla pozadina -> tamna (zadrzava nijansu). */
    static int mapBg(int c) {
        int a = c >>> 24;
        if (a < 20) return c;
        if (isThemeBg(c)) return c;   // boja teme (zadata u editoru): ne diraj, ma koliko bila svijetla
        float[] hsl = toHsl(c);
        if (hsl[2] < 0.6f) return c;
        if (light()) return (a << 24) | (BG & 0xffffff);   // svijetla tema: svijetla pozadina -> pozadina teme
        return fromHsl(hsl[0], hsl[1] * 0.35f, 0.07f + (1f - hsl[2]) * 0.45f, a);
    }

    /** Tamna boja teksta -> svijetla. */
    static int mapFg(int c) {
        if (lum(c) >= 0.5f) return c;
        int a = c >>> 24;
        float[] hsl = toHsl(c);
        if (hsl[1] < 0.2f) return (a << 24) | (TEXT & 0xffffff);
        // tamnocrveni tekst: tamniji (8B0000, 990000...) -> "Naslovi", svjetliji (CC0000, B22222...) -> "Praznici i postovi"
        if (hsl[1] >= 0.6f && (hsl[0] <= 15f || hsl[0] >= 345f)) {
            return (a << 24) | ((hsl[2] >= 0.36f ? HOLIDAY : TITLE) & 0xffffff);
        }
        if (light()) return c;                                  // svijetla tema: tamni obojeni tekst ostaje
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

    // ------------------------------------------------------------------ nedeljna traka, mesec, naslov kalendara

    /** Kljucevi za View.setTag (id-jevi sa vlastitim "paketom", ne mogu se sudariti sa resursima aplikacije). */
    private static final int TAG_SUN = 0x7e0c0a01;
    private static final int TAG_TITLE = 0x7e0c0a02;

    /** Originalna pozadina nedeljne trake u aplikaciji (svijetlo plavkasta, ~F1F2F4). */
    private static final int ORIG_SUN_BG = 0xfff1f2f4;

    private static int monthId = -2;

    /** Da li je boja (priblizno) originalna pozadina nedeljne trake. */
    static boolean isSunBg(int c) {
        if ((c >>> 24) < 0xf0) return false;
        return Math.abs(Color_r(c) - Color_r(ORIG_SUN_BG)) <= 3
                && Math.abs(Color_g(c) - Color_g(ORIG_SUN_BG)) <= 3
                && Math.abs(Color_b(c) - Color_b(ORIG_SUN_BG)) <= 3;
    }

    private static boolean isMonthBarId(View v) {
        int id = v.getId();
        if (id == View.NO_ID) return false;
        if (monthId == -2) {
            try {
                monthId = v.getResources().getIdentifier("tableRow1", "id", Hook.PKG);
            } catch (Throwable t) {
                monthId = 0;
            }
        }
        return monthId > 0 && id == monthId;
    }

    /** Naslov kalendara: "Pravoslavni crkveni kalendar - za prostu 2026 godinu". */
    static boolean isCalTitleText(CharSequence cs) {
        if (cs == null) return false;
        int n = cs.length();
        if (n < 20 || n > 120) return false;
        String s = cs.toString().toLowerCase(java.util.Locale.ROOT);
        return s.contains("црквени календар") && s.contains("годин");
    }

    /** Kljuc teksta (iz Colors) za TextView u nedeljnoj traci / traci mjeseca / naslovu kalendara / gornjoj traci, ili null. */
    static String roleKey(View tv) {
        View p = tv;
        for (int i = 0; i < 5 && p != null; i++) {
            if (i == 0 && p.getTag(TAG_TITLE) != null) return "c_cal_title_text";
            if (p.getTag(TAG_SUN) != null) return "c_sun_text";
            if (p.getTag(TAG_BAR) != null) return "c_toolbar_text";
            if (isMonthBarId(p)) return "c_month_text";
            Object par = p.getParent();
            p = par instanceof View ? (View) par : null;
        }
        return null;
    }

    /** Boja teksta za kljuc iz roleKey(), ili 0. */
    static int colorOfKey(String k) {
        if (k == null) return 0;
        switch (k) {
            case "c_cal_title_text": return CAL_TITLE_TEXT;
            case "c_sun_text": return SUN_TEXT;
            case "c_toolbar_text": return TOOLBAR_TEXT;
            case "c_month_text": return MONTH_TEXT;
            default: return 0;
        }
    }

    /** Boja teksta za TextView u nedeljnoj traci / traci mjeseca / naslovu kalendara, ili 0 ako nije ni u jednom. */
    static int roleColor(View tv) {
        return colorOfKey(roleKey(tv));
    }

    // ------------------------------------------------------------------ datum, dan i oznake (mrs / postovi) u kalendaru

    static final int TAG_TROLE = 0x7e0c0a0c;

    private static final java.util.regex.Pattern DAY_DATE_RE = java.util.regex.Pattern.compile(
            "^\\d{1,2}\\.\\s*\\p{L}+\\.?\\s*\\(\\d{1,2}\\.\\s*\\p{L}+\\.?\\)\\s*\\d{4}\\.?$");
    private static final java.util.regex.Pattern FAST_RE = java.util.regex.Pattern.compile(
            "^(?!св\\b)[^\\d]{0,24}(вода|уље|уљем|вино|риба|рибу|посно|пост|сир|јаја|млеко|млијеко)[^\\d]{0,24}$");
    private static final java.util.Set<String> DAY_NAMES = new java.util.HashSet<String>(java.util.Arrays.asList(
            "недеља", "недјеља", "понедељак", "понедјељак", "уторак", "среда", "сриједа", "четвртак", "петак", "субота"));

    /**
     * Ključ teksta prema sadržaju (tekst dolazi iz aplikacije): datum u traci dana, dan u istoj traci, oznaka "мрс" ili
     * oznake postova (вода, уље и вино...); null ako tekst nije ništa od toga.
     */
    static String textRole(CharSequence cs) {
        if (cs == null) return null;
        int n = cs.length();
        if (n == 0 || n > 60) return null;
        String s = cs.toString().trim();
        if (DAY_DATE_RE.matcher(s).matches()) return "c_day_date";
        String l = s.toLowerCase(java.util.Locale.ROOT);
        if (DAY_NAMES.contains(l)) return "c_day_name";
        if ("мрс".equals(l)) return "c_meat";
        if (l.length() <= 30 && FAST_RE.matcher(l).matches()) return "c_fast";
        return null;
    }

    static int colorOfRole(String k) {
        if (k == null) return 0;
        switch (k) {
            case "c_day_date": return DAY_DATE;
            case "c_day_name": return DAY_NAME;
            case "c_meat": return MEAT;
            case "c_fast": return FAST;
            default: return 0;
        }
    }

    private static boolean isFixedRole(String k) {
        return "c_meat".equals(k) || "c_fast".equals(k);
    }

    /** Praznicni (svijetlocrveni) datum i dan zadržavaju boju praznika; ostali dobijaju boju svoje stavke. */
    private static boolean isHolidayText(TextView tv) {
        Object o = tv.getTag(TAG_ORIG);
        int orig = o instanceof Integer ? ((Integer) o).intValue() : tv.getTextColors().getDefaultColor();
        return "c_holiday".equals(fgKey(orig)) || (tv.getCurrentTextColor() & 0xffffff) == (HOLIDAY & 0xffffff);
    }

    static final int TAG_SETTINGS = 0x7e0c0a0e;

    static boolean inSettings(View v) {
        if (v == null) return false;
        try {
            for (View p = v; p != null; p = p.getParent() instanceof View ? (View) p.getParent() : null) {
                if (p.getTag(TAG_SETTINGS) != null) return true;
                String name = p.getClass().getName();
                if (name.contains("SettingsUi") || name.contains("ColorPicker")) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** Font i boja za tekst koji je datum, dan ili oznaka; vraća ključ ili null (tada tekst nije ništa od toga). */
    static String applyTextRole(TextView tv, CharSequence cs) {
        if (inSettings(tv)) return null;
        String tk = textRole(cs);
        if (tk == null) {
            if (tv.getTag(TAG_TROLE) != null) tv.setTag(TAG_TROLE, null);
            return null;
        }
        tv.setTag(TAG_TROLE, tk);
        Fonts.apply(tv, tk);
        int rc = colorOfRole(tk);
        if (!isFixedRole(tk) && isHolidayText(tv)) rc = HOLIDAY;
        if (tv.getCurrentTextColor() != rc) {
            applying = true;
            try {
                tv.setTextColor(rc);
            } finally {
                applying = false;
            }
        }
        return tk;
    }

    /** Ključ fonta za tekst koji je (većim dijelom) obojen spanovima praznične / naslovne crvene boje, ili null. */
    static String spanFontKey(CharSequence cs) {
        if (!(cs instanceof Spanned) || cs.length() == 0) return null;
        Spanned sp = (Spanned) cs;
        int hol = 0;
        int tit = 0;
        for (ForegroundColorSpan f : sp.getSpans(0, sp.length(), ForegroundColorSpan.class)) {
            int len = sp.getSpanEnd(f) - sp.getSpanStart(f);
            String k = fgKey(f.getForegroundColor());
            if ("c_holiday".equals(k)) hol += len;
            else if ("c_title".equals(k)) tit += len;
        }
        int half = cs.length() / 2;
        if (hol >= half && hol >= tit && hol > 0) return "c_holiday";
        if (tit >= half && tit > 0) return "c_title";
        return null;
    }

    // ------------------------------------------------------------------ slova na tabovima (font)

    private static final int TAG_TABFONT = 0x7e0c0a0d;

    private static void tabFonts(View v) {
        if (v instanceof TextView) Fonts.apply((TextView) v, "c_tab_text");
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) tabFonts(g.getChildAt(i));
        }
    }

    /** Tabovi se prave poslije prvog crtanja, pa se font primjenjuje i poslije svakog rasporeda trake sa tabovima. */
    static void watchTabFonts(View tabLayout) {
        tabFonts(tabLayout);
        if (tabLayout.getTag(TAG_TABFONT) != null) return;
        tabLayout.setTag(TAG_TABFONT, Boolean.TRUE);
        tabLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                tabFonts(v);
            }
        });
    }

    /** TextView je unutar TabLayout-a (slova na tabovima). */
    static boolean inTabs(View tv) {
        Object p = tv.getParent();
        for (int i = 0; i < 4 && p instanceof View; i++) {
            if (p.getClass().getName().endsWith("TabLayout")) return true;
            p = ((View) p).getParent();
        }
        return false;
    }

    /**
     * Kljuc teksta za izvornu (ili vec temu) boju teksta u kalendaru: obican tamni tekst -> \"Текст календара\",
     * tamnocrveni -> \"Празници и постови\" / \"Текст наслова\" (isto razlikovanje kao u mapFg), ili null.
     */
    static String fgKey(int c) {
        int rgb = c & 0xffffff;
        if (rgb == (TEXT & 0xffffff)) return "c_text";            // resurs \"textcolormain\" je vec zamijenjen
        if (rgb == (HOLIDAY & 0xffffff)) return "c_holiday";
        if (rgb == (TITLE & 0xffffff)) return "c_title";
        if (lum(c) >= 0.5f) return null;
        float[] hsl = toHsl(c);
        if (hsl[1] < 0.2f) return "c_text";
        if (hsl[1] >= 0.6f && (hsl[0] <= 15f || hsl[0] >= 345f)) return hsl[2] >= 0.36f ? "c_holiday" : "c_title";
        return null;
    }

    /** Svi ForegroundColorSpan-ovi u tekstu dobijaju datu boju (tekst traka ima jednu boju). */
    static CharSequence spansToColor(CharSequence cs, int color) {
        if (!(cs instanceof Spanned)) return cs;
        Spanned sp = (Spanned) cs;
        ForegroundColorSpan[] fg = sp.getSpans(0, sp.length(), ForegroundColorSpan.class);
        boolean changed = false;
        for (ForegroundColorSpan f : fg) {
            if (f.getForegroundColor() != color) { changed = true; break; }
        }
        if (!changed) return cs;
        SpannableStringBuilder sb = new SpannableStringBuilder(sp);
        for (ForegroundColorSpan f : sb.getSpans(0, sb.length(), ForegroundColorSpan.class)) {
            int s = sb.getSpanStart(f);
            int e = sb.getSpanEnd(f);
            int fl = sb.getSpanFlags(f);
            sb.removeSpan(f);
            sb.setSpan(new ForegroundColorSpan(color), s, e, fl);
        }
        return sb;
    }

    /** Postavlja boju teksta (i spanova) svim TextView-ovima unutar v (ukljucujuci v). */
    private static void recolorTexts(View v, int color, String fontKey) {
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            if (tv.getCurrentTextColor() != color) tv.setTextColor(color);
            CharSequence t = tv.getText();
            CharSequence f = spansToColor(t, color);
            if (f != t) tv.setText(f);
            Fonts.apply(tv, fontKey);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) recolorTexts(g.getChildAt(i), color, fontKey);
        }
    }

    /** View je pozadina nedeljne trake: boja teme + boja teksta unutra. */
    static void markSunday(View v) {
        boolean first = v.getTag(TAG_SUN) == null;
        v.setTag(TAG_SUN, Boolean.TRUE);
        Drawable d = v.getBackground();
        if (!(d instanceof ColorDrawable) || ((ColorDrawable) d).getColor() != SUN_BG) {
            v.setBackground(new ColorDrawable(SUN_BG));
        }
        if (first) recolorTexts(v, SUN_TEXT, "c_sun_text");
    }

    /**
     * TextView je naslov kalendara: boja teksta + pozadina trake. Pozadina se mijenja samo ako je jasno da pripada
     * naslovu (njegova vlastita, pozadina roditelja kojem je jedino dijete, ili TextView preko cijele sirine);
     * inace ostaje pozadina aplikacije.
     */
    static void styleTitle(TextView tv) {
        tv.setTag(TAG_TITLE, Boolean.TRUE);
        if (tv.getCurrentTextColor() != CAL_TITLE_TEXT) tv.setTextColor(CAL_TITLE_TEXT);
        try {
            View holder = null;
            Object par = tv.getParent();
            if (tv.getBackground() != null) {
                holder = tv;
            } else if (par instanceof ViewGroup && ((ViewGroup) par).getChildCount() == 1
                    && ((ViewGroup) par).getBackground() != null) {
                holder = (ViewGroup) par;
            } else if (par != null && tv.getLayoutParams() != null
                    && tv.getLayoutParams().width == ViewGroup.LayoutParams.MATCH_PARENT) {
                holder = tv;
            }
            if (holder != null) {
                Drawable d = holder.getBackground();
                if (!(d instanceof ColorDrawable) || ((ColorDrawable) d).getColor() != CAL_TITLE_BG) {
                    holder.setBackground(new ColorDrawable(CAL_TITLE_BG));
                }
                holder.setTag(TAG_TITLE, Boolean.TRUE);
            }
            styleTitleLines(holder != null ? holder : tv);
            styleTitleFrame(holder != null ? holder : tv);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /** Linije iznad i ispod trake naslova (tanki susjedni view-ovi) dobijaju boju "Linije oko naslova". */
    static final int TAG_LINE = 0x7e0c0a06;

    private static boolean isThinLine(View s) {
        if (s instanceof TextView) return false;
        if (s instanceof ViewGroup && ((ViewGroup) s).getChildCount() > 0) return false;
        ViewGroup.LayoutParams lp = s.getLayoutParams();
        if (lp == null || lp.height <= 0) return false;
        return lp.height <= Math.round(8f * s.getResources().getDisplayMetrics().density);
    }

    private static boolean hasScrollable(ViewGroup g) {
        if (g.getChildCount() > 4) return true;
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if (c instanceof AbsListView || c instanceof android.widget.ScrollView || c instanceof android.webkit.WebView) {
                return true;
            }
        }
        return false;
    }

    /**
     * Razmak iznad i ispod trake naslova (margina trake ili padding kontejnera) vidi se u boji pozadine kontejnera:
     * to su "linije" oko naslova. Pozadina tog kontejnera dobija boju linija (traka naslova ostaje u svojoj boji).
     */
    private static final int TAG_FRAME = 0x7e0c0a07;

    /** Traka naslova sa "linijama": gornji i donji dio se crtaju bojom linija, sredina bojom pozadine naslova. */
    private static final class FrameDrawable extends Drawable {
        private final android.graphics.Paint paint = new android.graphics.Paint();
        private final int top;
        private final int bottom;

        FrameDrawable(int top, int bottom) {
            this.top = top;
            this.bottom = bottom;
        }

        @Override
        public void draw(Canvas c) {
            Rect b = getBounds();
            paint.setColor(CAL_TITLE_LINE);
            c.drawRect(b.left, b.top, b.right, b.bottom, paint);
            paint.setColor(CAL_TITLE_BG);
            c.drawRect(b.left, b.top + top, b.right, b.bottom - bottom, paint);
        }

        @Override
        public void setAlpha(int alpha) { }

        @Override
        public void setColorFilter(android.graphics.ColorFilter cf) { }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.OPAQUE;
        }
    }

    /**
     * Ako traka naslova ima vertikalne margine, razmak iznad i ispod nje je prazan prostor kroz koji se vidi pozadina
     * kalendara. Margine postaju padding trake, a pozadina trake se crta u tri dijela (linija, naslov, linija), pa
     * izgled ostaje isti, ali linije dobijaju vlastitu boju.
     */
    private static final int TAG_NEXT = 0x7e0c0a08;

    /**
     * Razmak ispod trake naslova cesto pripada sljedecem elementu (margina ili padding liste). Taj razmak se
     * prebacuje na traku naslova (njen donji dio), pa se moze obojiti kao linija; ukupni raspored ostaje isti.
     */
    private static int takeNextGap(View target) {
        if (target.getTag(TAG_NEXT) != null) return 0;
        View t = target;
        for (int level = 0; level < 2 && t != null; level++) {
            Object par = t.getParent();
            if (!(par instanceof ViewGroup)) return 0;
            ViewGroup g = (ViewGroup) par;
            int idx = g.indexOfChild(t);
            if (idx + 1 < g.getChildCount()) {
                View n = g.getChildAt(idx + 1);
                int max = Math.round(12f * n.getResources().getDisplayMetrics().density);
                int gap = 0;
                ViewGroup.LayoutParams lp = n.getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams && ((ViewGroup.MarginLayoutParams) lp).topMargin > 0) {
                    gap = ((ViewGroup.MarginLayoutParams) lp).topMargin;
                    if (gap <= max) {
                        ((ViewGroup.MarginLayoutParams) lp).topMargin = 0;
                        n.setLayoutParams(lp);
                    }
                } else if (n.getPaddingTop() > 0) {
                    gap = n.getPaddingTop();
                    if (gap <= max) n.setPadding(n.getPaddingLeft(), 0, n.getPaddingRight(), n.getPaddingBottom());
                }
                target.setTag(TAG_NEXT, Boolean.TRUE);
                return gap <= max ? gap : 0;
            }
            t = g;
        }
        return 0;
    }

    private static boolean styleTitleMargins(View target) {
        ViewGroup.LayoutParams lp = target.getLayoutParams();
        if (!(lp instanceof ViewGroup.MarginLayoutParams)) return false;
        ViewGroup.MarginLayoutParams mp = (ViewGroup.MarginLayoutParams) lp;
        Object o = target.getTag(TAG_FRAME);
        int[] fm = o instanceof int[] ? (int[]) o : null;
        int extra = takeNextGap(target);
        if (fm == null) {
            if (mp.topMargin <= 0 && mp.bottomMargin <= 0 && extra <= 0) return false;
            fm = new int[] { mp.topMargin, mp.bottomMargin + extra, target.getPaddingLeft(), target.getPaddingTop(),
                    target.getPaddingRight(), target.getPaddingBottom() };
            target.setTag(TAG_FRAME, fm);
            mp.topMargin = 0;
            mp.bottomMargin = 0;
            target.setLayoutParams(mp);
        } else if (extra > 0) {
            fm[1] += extra;
        }
        target.setBackground(new FrameDrawable(fm[0], fm[1]));
        target.setPadding(fm[2], fm[3] + fm[0], fm[4], fm[5] + fm[1]);
        if (fm[1] == 0 && fm[0] > 0) addBottomLine(target, fm[0]);   // donja linija iste debljine kao gornja
        return true;
    }

    private static final int TAG_OVERLAY = 0x7e0c0a09;

    /**
     * Donja linija: razmak ispod trake naslova ne pripada traci (pripada listi ispod), pa se linija iscrtava preko
     * njega, u roditeljskom view-u, odmah ispod trake. Debljina je ista kao gornja linija.
     */
    private static void addBottomLine(final View target, final int thickness) {
        if (target.getTag(TAG_OVERLAY) != null) return;
        try {
            View t = target;
            ViewGroup g = null;
            for (int i = 0; i < 3; i++) {
                Object par = t.getParent();
                if (!(par instanceof ViewGroup)) return;
                g = (ViewGroup) par;
                if (g.getChildCount() > 1) break;
                t = g;
            }
            if (g == null || g.getChildCount() <= 1) return;
            final View edge = t;
            final android.graphics.Paint paint = new android.graphics.Paint();
            Drawable line = new Drawable() {
                @Override
                public void draw(Canvas c) {
                    if (edge.getVisibility() != View.VISIBLE) return;
                    paint.setColor(CAL_TITLE_LINE);
                    float ty = edge.getTranslationY();
                    c.drawRect(edge.getLeft(), edge.getBottom() + ty, edge.getRight(),
                            edge.getBottom() + ty + thickness, paint);
                }

                @Override
                public void setAlpha(int alpha) { }

                @Override
                public void setColorFilter(android.graphics.ColorFilter cf) { }

                @Override
                public int getOpacity() {
                    return android.graphics.PixelFormat.TRANSLUCENT;
                }
            };
            g.getOverlay().add(line);
            target.setTag(TAG_OVERLAY, line);
            g.invalidate();
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void styleTitleFrame(View target) {
        if (styleTitleMargins(target)) return;
        View t = target;
        for (int level = 0; level < 3 && t != null; level++) {
            Object par = t.getParent();
            if (!(par instanceof ViewGroup)) return;
            ViewGroup g = (ViewGroup) par;
            if (hasScrollable(g)) return;                    // kontejner sa listom: ne diramo pozadinu
            boolean gap = g.getPaddingTop() > 0 || g.getPaddingBottom() > 0;
            ViewGroup.LayoutParams lp = t.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mp = (ViewGroup.MarginLayoutParams) lp;
                if (mp.topMargin > 0 || mp.bottomMargin > 0) gap = true;
            }
            if (gap) {
                g.setTag(TAG_LINE, Boolean.TRUE);
                Drawable bg = g.getBackground();
                if (!(bg instanceof ColorDrawable) || ((ColorDrawable) bg).getColor() != CAL_TITLE_LINE) {
                    g.setBackground(new ColorDrawable(CAL_TITLE_LINE));
                }
                return;
            }
            t = g;
        }
    }

    private static void styleTitleLines(View target) {
        View t = target;
        for (int level = 0; level < 2 && t != null; level++) {
            Object par = t.getParent();
            if (!(par instanceof ViewGroup)) return;
            ViewGroup g = (ViewGroup) par;
            int idx = g.indexOfChild(t);
            for (int d = -1; d <= 1; d += 2) {
                int j = idx + d;
                if (j < 0 || j >= g.getChildCount()) continue;
                View s = g.getChildAt(j);
                if (!isThinLine(s)) continue;
                s.setTag(TAG_LINE, Boolean.TRUE);
                Drawable bg = s.getBackground();
                if (!(bg instanceof ColorDrawable) || ((ColorDrawable) bg).getColor() != CAL_TITLE_LINE) {
                    s.setBackground(new ColorDrawable(CAL_TITLE_LINE));
                }
            }
            if (g.getChildCount() == 1) t = g; else return;
        }
    }

    /** Izvorna boja teksta prije zamjene (za bocni meni, gdje se tekst dijeli na meni i podmeni). */
    static final int TAG_ORIG = 0x7e0c0a03;

    /** Postavlja se dok modul sam mijenja boju teksta (da hook za setTextColor ne zamijeni tu boju). */
    static boolean applying;

    /**
     * Boja teksta u bocnom meniju prema izvornoj boji u aplikaciji: tamnocrveni (nazivi grupa) -> "Tekst menija";
     * svijetlocrveni i obican tekst (podstavke) -> "Tekst podmenija". 0 = ne diraj (npr. bijeli tekst).
     */
    static int menuColor(int c, boolean stable) {
        int sub = stable ? SUB_TEXT : SUB_TEXT1;
        if ((c & 0xffffff) == (TEXT & 0xffffff)) return sub;   // resurs "textcolormain" je vec zamijenjen
        if (lum(c) >= 0.5f) return 0;
        float[] hsl = toHsl(c);
        if (hsl[1] < 0.2f) return sub;
        if (hsl[1] >= 0.6f && (hsl[0] <= 15f || hsl[0] >= 345f)) return hsl[2] >= 0.36f ? sub : MENU_TEXT;
        return 0;
    }

    /** Kljuc teksta (iz Colors) za tekst bocnog menija: isto razlikovanje kao menuColor(), ili null. */
    static String menuKey(int c, boolean stable) {
        String sub = stable ? "c_sub_text" : "c_sub_text1";
        if ((c & 0xffffff) == (TEXT & 0xffffff)) return sub;
        if (lum(c) >= 0.5f) return null;
        float[] hsl = toHsl(c);
        if (hsl[1] < 0.2f) return sub;
        if (hsl[1] >= 0.6f && (hsl[0] <= 15f || hsl[0] >= 345f)) return hsl[2] >= 0.36f ? sub : "c_menu_text";
        return null;
    }

    /** ListView sa crvenim razdjelnikom = lista molitvi i knjiga. */
    static final int TAG_BOOKLIST = 0x7e0c0a04;

    /** Tekst stavke liste molitvi i knjiga: sav tekst koji je modul preboji dobija boju "Liste knjiga i molitvi". */
    static void styleBookItem(View v) {
        if (v == null || !Prefs.dark()) return;
        try {
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                int cur = tv.getCurrentTextColor();
                boolean ours = tv.getTag(TAG_ORIG) instanceof Integer || (cur & 0xffffff) == (TEXT & 0xffffff)
                        || (cur & 0xffffff) == (HOLIDAY & 0xffffff) || (cur & 0xffffff) == (TITLE & 0xffffff);
                if (ours) Fonts.apply(tv, "c_web_text");
                if (ours && cur != WEB_TEXT) {
                    applying = true;
                    try {
                        tv.setTextColor(WEB_TEXT);
                    } finally {
                        applying = false;
                    }
                }
            }
            if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) styleBookItem(g.getChildAt(i));
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /**
     * Boji tekst jednog reda bocnog menija (grupe ili podstavke) i svega unutar njega.
     * stable = false: red koji pravi adapter (npr. dok traje animacija otvaranja podmenija) -> "Tekst podmenija 1";
     * stable = true: red koji je vec u listi -> "Tekst podmenija 2".
     */
    static void styleMenu(View v, boolean stable) {
        if (v == null || !Prefs.dark()) return;
        styleMenuTree(v, stable);
        alignMenuRow(v);
    }

    private static void styleMenuTree(View v, boolean stable) {
        if (v == null || !Prefs.dark()) return;
        try {
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                Object o = tv.getTag(TAG_ORIG);
                int orig = o instanceof Integer ? ((Integer) o).intValue() : tv.getTextColors().getDefaultColor();
                if (!(o instanceof Integer)) tv.setTag(TAG_ORIG, Integer.valueOf(orig));   // da drugi prolaz vidi izvornu boju
                int mc = menuColor(orig, stable);
                Fonts.apply(tv, menuKey(orig, stable));
                if (mc != 0 && tv.getCurrentTextColor() != mc) {
                    applying = true;
                    try {
                        tv.setTextColor(mc);
                    } finally {
                        applying = false;
                    }
                }
            }
            if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) styleMenuTree(g.getChildAt(i), stable);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    // ------------------------------------------------------------------ poravnanje ikonica i teksta u meniju

    private static final int TAG_MENUALIGN = 0x7e0c0a31;

    /** Ikonice (i strelicu) reda menija poravnava po vertikali sa sredinom slova glavnog teksta reda. */
    static void alignMenuRow(final View row) {
        if (!(row instanceof ViewGroup)) return;
        try {
            if (row.getTag(TAG_MENUALIGN) == null) {
                row.setTag(TAG_MENUALIGN, Boolean.TRUE);
                row.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                        doAlignMenuRow(v);
                    }
                });
            }
            doAlignMenuRow(row);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void collectRowViews(View v, java.util.List<TextView> tvs, java.util.List<ImageView> ivs) {
        if (v.getVisibility() == View.GONE) return;
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            if (tv.getText() != null && tv.getText().length() > 0) tvs.add(tv);
        } else if (v instanceof ImageView) {
            ivs.add((ImageView) v);
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collectRowViews(g.getChildAt(i), tvs, ivs);
        }
    }

    /** Pomak gornje ivice view-a u odnosu na red (bez translationY). */
    private static int topInRow(View v, View row) {
        int y = 0;
        View c = v;
        while (c != null && c != row) {
            y += c.getTop();
            Object p = c.getParent();
            if (!(p instanceof View)) return Integer.MIN_VALUE;
            View pv = (View) p;
            if (pv != row) y -= pv.getScrollY();
            c = pv;
        }
        return c == row ? y : Integer.MIN_VALUE;
    }

    private static void doAlignMenuRow(View row) {
        try {
            if (!Prefs.dark() || row.getHeight() <= 0) return;
            java.util.List<TextView> tvs = new java.util.ArrayList<TextView>();
            java.util.List<ImageView> ivs = new java.util.ArrayList<ImageView>();
            collectRowViews(row, tvs, ivs);
            if (tvs.isEmpty() || ivs.isEmpty()) return;
            TextView main = tvs.get(0);
            for (TextView t : tvs) {
                if (t.getTextSize() > main.getTextSize()) main = t;
            }
            if (main.getLayout() == null || main.getHeight() <= 0) return;
            int base = main.getBaseline();
            if (base < 0) return;
            int mt = topInRow(main, row);
            if (mt == Integer.MIN_VALUE) return;
            Rect cap = new Rect();
            main.getPaint().getTextBounds("\u041c\u041e", 0, 2, cap);   // visina velikih slova (ћирилица)
            float textCenter = mt + base - cap.height() / 2f;
            for (ImageView iv : ivs) {
                if (iv.getHeight() <= 0) continue;
                if (iv.getHeight() >= row.getHeight() * 0.9f && iv.getWidth() >= row.getWidth() * 0.9f) continue;
                int it = topInRow(iv, row);
                if (it == Integer.MIN_VALUE) continue;
                float iconCenter = it + iv.getHeight() / 2f;
                float dy = textCenter - iconCenter;
                if (Math.abs(iv.getTranslationY() - dy) > 0.5f) iv.setTranslationY(dy);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    // ------------------------------------------------------------------ traka mjeseca: format teksta, centriranje, razmak ikonice

    private static final int TAG_MONTHHOOK = 0x7e0c0a32;
    private static final java.util.regex.Pattern MONTH_RE = java.util.regex.Pattern.compile(
            "^\\s*(\\p{L}+)\\s*[.,]?\\s*(\\d{4})\\s*\\.?\\s*$");

    /** "Новембар.2026" -> "Новембар 2026." */
    static CharSequence fixMonthText(CharSequence cs) {
        try {
            if (cs == null) return cs;
            java.util.regex.Matcher m = MONTH_RE.matcher(cs.toString());
            if (!m.matches()) return cs;
            String want = m.group(1) + " " + m.group(2) + ".";
            if (want.contentEquals(cs)) return cs;
            if (!(cs instanceof Spanned)) return want;
            SpannableStringBuilder sb = new SpannableStringBuilder(cs);
            int n = sb.length();
            int e = m.end(2);
            sb.replace(e, n, ".");
            sb.replace(m.end(1), m.start(2), " ");
            return sb;
        } catch (Throwable t) {
            return cs;
        }
    }

    /** Menja samo prikazani tekst (Новембар.2026 -> Новембар 2026.); tekst u TextView-u ostaje neizmenjen. */
    private static final class MonthFormat implements android.text.method.TransformationMethod {
        private final android.text.method.TransformationMethod inner;

        MonthFormat(android.text.method.TransformationMethod inner) {
            this.inner = inner;
        }

        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            CharSequence r = inner != null ? inner.getTransformation(source, view) : source;
            return fixMonthText(r);
        }

        @Override
        public void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction, Rect previouslyFocusedRect) {
            if (inner != null) inner.onFocusChanged(view, sourceText, focused, direction, previouslyFocusedRect);
        }
    }

    private static View monthRow(View v) {
        View p = v;
        for (int i = 0; i < 6 && p != null; i++) {
            if (isMonthBarId(p)) return p;
            Object par = p.getParent();
            p = par instanceof View ? (View) par : null;
        }
        return null;
    }

    private static void hookMonthBar(final TextView tv) {
        try {
            final View row = monthRow(tv);
            if (row == null) return;
            View.OnLayoutChangeListener l = new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int a, int b, int c, int d, int e, int f, int g, int h) {
                    alignMonthBar(row);
                }
            };
            if (row.getTag(TAG_MONTHHOOK) == null) {
                row.setTag(TAG_MONTHHOOK, Boolean.TRUE);
                row.addOnLayoutChangeListener(l);
            }
            if (tv.getTag(TAG_MONTHHOOK) == null) {
                tv.setTag(TAG_MONTHHOOK, Boolean.TRUE);
                tv.addOnLayoutChangeListener(l);
                if (!(tv.getTransformationMethod() instanceof MonthFormat)) {
                    tv.setTransformationMethod(new MonthFormat(tv.getTransformationMethod()));
                }
            }
            alignMonthBar(row);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static int leftInRow(View v, View row) {
        int x = 0;
        View c = v;
        while (c != null && c != row) {
            x += c.getLeft();
            Object p = c.getParent();
            if (!(p instanceof View)) return Integer.MIN_VALUE;
            View pv = (View) p;
            if (pv != row) x -= pv.getScrollX();
            c = pv;
        }
        return c == row ? x : Integer.MIN_VALUE;
    }

    /** Mjesec i godina tacno na sredini trake; ikonica kalendara odmaknuta od strelice. */
    static void alignMonthBar(View row) {
        try {
            if (!Prefs.dark() || row.getWidth() <= 0) return;
            java.util.List<TextView> tvs = new java.util.ArrayList<TextView>();
            java.util.List<ImageView> ivs = new java.util.ArrayList<ImageView>();
            collectRowViews(row, tvs, ivs);
            if (tvs.isEmpty()) return;
            TextView main = tvs.get(0);
            for (TextView t : tvs) {
                if (t.getTextSize() > main.getTextSize()) main = t;
            }
            ImageView icon = null;
            java.util.List<ImageView> arrows = new java.util.ArrayList<ImageView>();
            for (ImageView iv : ivs) {
                if (iv.getWidth() <= 0 || iv.getDrawable() == null) continue;
                styleMonthImage(iv);
                if ("icon".equals(iv.getTag(TAG_IMGKIND))) icon = iv; else arrows.add(iv);
            }
            float density = row.getResources().getDisplayMetrics().density;

            // razmak ikonice od najblize strelice
            if (icon != null) {
                int il = leftInRow(icon, row);
                if (il != Integer.MIN_VALUE) {
                    float ic = il + icon.getWidth() / 2f;
                    ImageView near = null;
                    float best = Float.MAX_VALUE;
                    for (ImageView a : arrows) {
                        int al = leftInRow(a, row);
                        if (al == Integer.MIN_VALUE) continue;
                        float dist = Math.abs(al + a.getWidth() / 2f - ic);
                        if (dist < best) {
                            best = dist;
                            near = a;
                        }
                    }
                    float tx = 0f;
                    if (near != null) {
                        int al = leftInRow(near, row);
                        float gap;
                        float dir;
                        if (al >= il) {                      // strelica desno od ikonice
                            gap = al - (il + icon.getWidth());
                            dir = -1f;
                        } else {                             // strelica lijevo od ikonice
                            gap = il - (al + near.getWidth());
                            dir = 1f;
                        }
                        float want = 14f * density;
                        if (gap < want) tx = dir * (want - gap);
                    }
                    if (Math.abs(icon.getTranslationX() - tx) > 0.5f) icon.setTranslationX(tx);
                }
            }

            // tekst na sredinu trake
            if (main.getLayout() == null || main.getLineCount() < 1 || main.getWidth() <= 0) return;
            int ml = leftInRow(main, row);
            if (ml == Integer.MIN_VALUE) return;
            android.text.Layout lay = main.getLayout();
            float left = ml + main.getCompoundPaddingLeft() + lay.getLineLeft(0);
            float center = left + lay.getLineWidth(0) / 2f;
            float dx = row.getWidth() / 2f - center;
            // ne smije se nalijegati na ikonicu
            if (icon != null) {
                int il = leftInRow(icon, row);
                if (il != Integer.MIN_VALUE) {
                    float iconL = il + icon.getTranslationX();
                    float iconR = iconL + icon.getWidth();
                    float tl = left + dx;
                    float tr = tl + lay.getLineWidth(0);
                    float pad = 4f * density;
                    if (iconL > center && tr > iconL - pad) dx -= tr - (iconL - pad);
                    else if (iconR < center && tl < iconR + pad) dx += (iconR + pad) - tl;
                }
            }
            if (Math.abs(main.getTranslationX() - dx) > 0.5f) main.setTranslationX(dx);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    // ------------------------------------------------------------------ strelice i ikonica u traci mjeseca

    private static final int TAG_IMGKIND = 0x7e0c0a0a;
    private static final float ICON_HUE = 207f;     // nijansa originalne ikonice kalendara (plava)

    private static boolean inMonthBar(View v) {
        View p = v;
        for (int i = 0; i < 6 && p != null; i++) {
            if (isMonthBarId(p)) return true;
            Object par = p.getParent();
            p = par instanceof View ? (View) par : null;
        }
        return false;
    }

    /** Udio zasicenih (sarenih) piksela drawable-a: ikonica kalendara je sarena, strelice su bijele/sive. */
    private static float colorfulness(Drawable d) {
        try {
            Rect old = d.copyBounds();
            Bitmap bm = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bm);
            d.setBounds(0, 0, 48, 48);
            d.draw(cv);
            d.setBounds(old);
            int opaque = 0;
            int colorful = 0;
            for (int y = 0; y < 48; y++) {
                for (int x = 0; x < 48; x++) {
                    int px = bm.getPixel(x, y);
                    if ((px >>> 24) < 128) continue;
                    opaque++;
                    int r = Color_r(px);
                    int g = Color_g(px);
                    int b = Color_b(px);
                    int mx = Math.max(r, Math.max(g, b));
                    int mn = Math.min(r, Math.min(g, b));
                    if (mx > 60 && (mx - mn) / (float) mx > 0.3f) colorful++;
                }
            }
            bm.recycle();
            return opaque == 0 ? 0f : colorful / (float) opaque;
        } catch (Throwable t) {
            return 0f;
        }
    }

    /** Pomjeranje nijanse (hue rotate) ikonice prema izabranoj boji; siva boja daje crno-bijelu ikonicu. */
    private static android.graphics.ColorFilter iconFilter() {
        float[] hsl = toHsl(CAL_ICON);
        ColorMatrix cm = new ColorMatrix();
        if (hsl[1] < 0.12f) {
            cm.setSaturation(0f);
            return new ColorMatrixColorFilter(cm);
        }
        float rot = hsl[0] - ICON_HUE;
        if (Math.abs(rot) < 1f) return null;
        double a = Math.toRadians(rot);
        float c = (float) Math.cos(a);
        float s = (float) Math.sin(a);
        cm.set(new float[] {
                0.213f + 0.787f * c - 0.213f * s, 0.715f - 0.715f * c - 0.715f * s, 0.072f - 0.072f * c + 0.928f * s, 0, 0,
                0.213f - 0.213f * c + 0.143f * s, 0.715f + 0.285f * c + 0.140f * s, 0.072f - 0.072f * c - 0.283f * s, 0, 0,
                0.213f - 0.213f * c - 0.787f * s, 0.715f - 0.715f * c + 0.715f * s, 0.072f + 0.928f * c + 0.072f * s, 0, 0,
                0, 0, 0, 1, 0 });
        return new ColorMatrixColorFilter(cm);
    }

    /** Strelice (prethodni / sljedeci mjesec) i ikonica kalendara u traci mjeseca. */
    static void styleMonthImage(ImageView iv) {
        try {
            if (!inMonthBar(iv)) return;
            Drawable d = iv.getDrawable();
            if (d == null) return;
            Object k = iv.getTag(TAG_IMGKIND);
            if (k == null) {
                k = colorfulness(d) > 0.08f ? "icon" : "arrow";
                iv.setTag(TAG_IMGKIND, k);
            }
            if ("icon".equals(k)) {
                android.graphics.ColorFilter f = iconFilter();
                if (f == null) iv.clearColorFilter(); else iv.setColorFilter(f);
            } else {
                iv.setColorFilter(ARROWS, android.graphics.PorterDuff.Mode.SRC_IN);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /** Nova slika u ImageView-u: ponovo odredi da li je strelica ili ikonica. */
    static void onImageChanged(ImageView iv) {
        iv.setTag(TAG_IMGKIND, null);
        styleMonthImage(iv);
    }

    private static final int TAG_BAR = 0x7e0c0a0b;

    private static boolean isToolbar(View v) {
        String cn = v.getClass().getName();
        if (cn.endsWith(".Toolbar")) return true;
        int id = v.getId();
        if (id == View.NO_ID) return false;
        try {
            String n = v.getResources().getResourceEntryName(id);
            return "toolbar".equals(n) || "action_bar".equals(n);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Gornja traka aplikacije ("Pravoslavac" + meni): boja "Gornja traka" iz teme. */
    static void styleToolbar(View v) {
        try {
            if (!isToolbar(v)) return;
            v.setTag(TAG_BAR, Boolean.TRUE);
            Drawable d = v.getBackground();
            if (!(d instanceof ColorDrawable) || ((ColorDrawable) d).getColor() != TOOLBAR) {
                v.setBackground(new ColorDrawable(TOOLBAR));
            }
            Object par = v.getParent();                       // ActionBarContainer iza trake
            if (par instanceof View && par.getClass().getName().endsWith("ActionBarContainer")) {
                try {
                    par.getClass().getMethod("setPrimaryBackground", Drawable.class)
                            .invoke(par, new ColorDrawable(TOOLBAR));
                } catch (Throwable ignore) {
                    // nema te metode: dovoljna je pozadina same trake
                }
            }
            try {
                v.getClass().getMethod("setTitleTextColor", int.class).invoke(v, TOOLBAR_TEXT);
            } catch (Throwable ignore) {
                // nema te metode: tekst se boji preko TextView-ova unutar trake
            }
            recolorTexts(v, TOOLBAR_TEXT, "c_toolbar_text");
            v.invalidate();
            Object par2 = v.getParent();
            if (par2 instanceof View) ((View) par2).invalidate();
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /** Odmah primijeni boje gornje trake (pozadina + tekst) na vec otvoren ekran, bez ponovnog pravljenja ekrana. */
    static void refreshToolbar(View root) {
        try {
            if (root == null) return;
            styleToolbar(root);
            if (root instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) root;
                for (int i = 0; i < g.getChildCount(); i++) refreshToolbar(g.getChildAt(i));
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static boolean isMonthBar(View v) {
        return isMonthBarId(v);
    }

    static void fixBackground(View v) {
        Drawable d = v.getBackground();
        if (d == null) return;
        if (v.getTag(TAG_SUN) != null || v.getTag(TAG_TITLE) != null || v.getTag(TAG_LINE) != null
                || v.getTag(TAG_BAR) != null) return;   // vec u bojama teme
        if (d instanceof ColorDrawable) {
            int c = ((ColorDrawable) d).getColor();
            if (c == 0xff8b0000) {                       // crvena traka (npr. naslov mjeseca)
                if (RED_BAR != c) v.setBackgroundColor(RED_BAR);
                return;
            }
            if (c == 0xfffaf5e9 && isMonthBar(v)) {      // traka mjeseca i godine (tableRow1)
                v.setBackgroundColor(MONTH_BAR);
                return;
            }
            if (isSunBg(c)) {                            // nedeljna traka
                markSunday(v);
                return;
            }
            int m = mapBg(c);
            if (m != c) v.setBackgroundColor(m);
            return;
        }
        int avg = drawableAvg(d);
        if (avg != 0 && isSunBg(avg)) {                  // nedeljna traka zadata drawable-om (shape / gradijent)
            markSunday(v);
            return;
        }
        if (avg != 0 && isThemeBg(avg)) return;          // drawable u boji teme
        float dl = avg == 0 ? -1f : lum(avg);
        if (dl < 0.6f) return;
        if (d instanceof BitmapDrawable && ((BitmapDrawable) d).getIntrinsicWidth() > 400) {
            v.setBackgroundColor(BG);
            return;
        }
        if (!light()) d.mutate().setColorFilter(invert());
    }

    /** Boja linije ispod aktivnog taba (TabLayout; novije i starije verzije biblioteke). */
    private static void setTabIndicator(View tabs) {
        try {
            tabs.getClass().getMethod("setSelectedTabIndicatorColor", int.class).invoke(tabs, TAB_LINE);
            return;
        } catch (Throwable ignored) {
        }
        try {
            // starija biblioteka: indikator crta unutrasnji "strip" (polje mTabStrip)
            for (Class<?> c = tabs.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    java.lang.reflect.Field f = c.getDeclaredField("mTabStrip");
                    f.setAccessible(true);
                    Object strip = f.get(tabs);
                    strip.getClass().getMethod("setSelectedIndicatorColor", int.class).invoke(strip, TAB_LINE);
                    return;
                } catch (NoSuchFieldException ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void fixOne(View v) {
        if (inSettings(v)) return;
        fixBackground(v);
        if (v.getClass().getName().endsWith("TabLayout")) {
            try {
                v.getClass().getMethod("setTabTextColors", ColorStateList.class)
                        .invoke(v, ColorStateList.valueOf(TAB_TEXT));
            } catch (Throwable ignored) {
            }
            setTabIndicator(v);
            watchTabFonts(v);
        }
        if (v instanceof ImageView) styleMonthImage((ImageView) v);
        styleToolbar(v);
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            String rk = roleKey(tv);
            int role = colorOfKey(rk);
            if (role == 0 && isCalTitleText(tv.getText())) {
                styleTitle(tv);
                rk = "c_cal_title_text";
                role = CAL_TITLE_TEXT;
            }
            if (role != 0) {
                if (tv.getCurrentTextColor() != role) tv.setTextColor(role);
                CharSequence t = tv.getText();
                CharSequence f = spansToColor(t, role);
                if (f != t) tv.setText(f);
                Fonts.apply(tv, rk);
                return;
            }
            if (applyTextRole(tv, tv.getText()) != null) return;   // datum, dan ili oznaka: vlastiti font i boja
            ColorStateList cur = tv.getTextColors();
            // font: slova na tabovima, inace prema (izvornoj) boji teksta; vec dodijeljen font se samo osvjezi
            String fk = inTabs(tv) ? "c_tab_text" : fgKey(cur.getDefaultColor());
            if (fk != null) Fonts.apply(tv, fk);
            else Fonts.refresh(tv);
            ColorStateList mapped = mapCsl(cur);
            if (mapped != cur) {
                tv.setTag(TAG_ORIG, Integer.valueOf(cur.getDefaultColor()));   // izvorna boja (za tekst menija)
                tv.setTextColor(mapped);
            }
            ColorStateList hint = tv.getHintTextColors();
            if (hint != null) {
                ColorStateList mh = mapCsl(hint);
                if (mh != hint) tv.setHintTextColor(mh);
            }
        }
        if (v instanceof AbsListView) {
            Drawable sel = ((AbsListView) v).getSelector();
            if (sel != null && !light() && drawableLum(sel) >= 0.6f) {
                sel.mutate().setColorFilter(invert());
            }
            if (v instanceof ListView) {
                ListView lv = (ListView) v;
                boolean menu = lv instanceof android.widget.ExpandableListView;
                Drawable div = lv.getDivider();
                if (div instanceof ColorDrawable) {
                    int col = ((ColorDrawable) div).getColor();
                    int repl = 0;
                    if ((col & 0xffffff) == 0xdc143c && !menu) {            // crvene linije lista molitvi i knjiga
                        repl = RED_LINE;
                        lv.setTag(TAG_BOOKLIST, Boolean.TRUE);              // tekst stavki ove liste: "Liste knjiga i molitvi"
                    } else if (lum(col) < 0.12f || (col & 0xffffff) == (DIVIDER & 0xffffff)
                            || (col & 0xffffff) == (MENU_DIVIDER & 0xffffff)) {
                        repl = menu ? MENU_DIVIDER : DIVIDER;               // razdjelnici kalendara / menija
                    }
                    if (repl != 0 && repl != col) {
                        int h = lv.getDividerHeight();
                        lv.setDivider(new ColorDrawable(repl));
                        lv.setDividerHeight(h);
                    }
                }
                if (menu) fixMenuChildDivider((android.widget.ExpandableListView) lv);
            }
        }
    }

    /** Razdjelnik izmedju podstavki menija (ExpandableListView.mChildDivider). */
    private static void fixMenuChildDivider(android.widget.ExpandableListView lv) {
        try {
            Object cd = de.robv.android.xposed.XposedHelpers.getObjectField(lv, "mChildDivider");
            if (cd instanceof ColorDrawable) {
                int col = ((ColorDrawable) cd).getColor();
                if ((lum(col) < 0.12f || (col & 0xffffff) == (DIVIDER & 0xffffff)
                        || (col & 0xffffff) == (MENU_DIVIDER & 0xffffff)) && col != MENU_DIVIDER) {
                    lv.setChildDivider(new ColorDrawable(MENU_DIVIDER));
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static final int TAG_MENUDIV = 0x7e0c0a05;

    /** Jednom po listi menija (poslije izrade): razdjelnici u boji menija, i kad ih aplikacija postavi kodom. */
    static void fixMenuDividers(final android.widget.ExpandableListView lv) {
        if (lv.getTag(TAG_MENUDIV) != null) return;
        lv.setTag(TAG_MENUDIV, Boolean.TRUE);
        lv.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Drawable div = lv.getDivider();
                    if (div instanceof ColorDrawable) {
                        int col = ((ColorDrawable) div).getColor();
                        if (col != MENU_DIVIDER && (lum(col) < 0.12f || (col & 0xffffff) == (DIVIDER & 0xffffff))) {
                            int h = lv.getDividerHeight();
                            lv.setDivider(new ColorDrawable(MENU_DIVIDER));
                            lv.setDividerHeight(h);
                        }
                    }
                    fixMenuChildDivider(lv);
                } catch (Throwable t) {
                    XposedBridge.log(t);
                }
            }
        });
    }

    /** Prolazi kroz hijerarhiju view-ova i popravlja boje. */
    static void walk(View v) {
        if (inSettings(v)) return;
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

    /** Tekst koji se postavlja u TextView: trake kalendara (nedelja, mesec, naslov) imaju svoje boje, ostalo opste. */
    static CharSequence fixText(TextView tv, CharSequence cs) {
        if (inSettings(tv)) return cs;
        String rk = roleKey(tv);
        int role = colorOfKey(rk);
        if (role == 0 && isCalTitleText(cs)) {
            styleTitle(tv);
            rk = "c_cal_title_text";
            role = CAL_TITLE_TEXT;
        }
        if (role != 0) {
            Fonts.apply(tv, rk);
            if ("c_month_text".equals(rk)) {
                hookMonthBar(tv);   // format se menja samo u prikazu; getText() ostaje originalan (aplikacija ga parsira)
            }
            return spansToColor(cs, role);
        }
        if (applyTextRole(tv, cs) == null) {
            String sk = spanFontKey(cs);   // crveni spanovi (praznici / naslovi): font te stavke
            if (sk != null) Fonts.apply(tv, sk);
        }
        return fixSpans(cs);
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

    // ------------------------------------------------------------------ samo fontovi (tema "Православац")
    // Boje aplikacije se ne diraju; ovdje se samo prepoznaje uloga teksta (isto kao gore) i primjenjuje font/velicina.

    /** Svi TextView-ovi unutar v (ukljucujuci v) dobijaju font teksta sa datim kljucem. */
    private static void fontsIn(View v, String key) {
        if (v instanceof TextView) Fonts.apply((TextView) v, key);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) fontsIn(g.getChildAt(i), key);
        }
    }

    /** Pozadina nedeljne trake (originalna boja): samo oznaka i font teksta unutar nje. */
    static void fontSunday(View v) {
        boolean first = v.getTag(TAG_SUN) == null;
        v.setTag(TAG_SUN, Boolean.TRUE);
        if (first) fontsIn(v, "c_sun_text");
    }

    /** Datum, dan ili oznaka (mrs / postovi): samo font. Vraca kljuc ili null. */
    static String fontTextRole(TextView tv, CharSequence cs) {
        if (inSettings(tv)) return null;
        String tk = textRole(cs);
        if (tk == null) {
            if (tv.getTag(TAG_TROLE) != null) tv.setTag(TAG_TROLE, null);
            return null;
        }
        tv.setTag(TAG_TROLE, tk);
        Fonts.apply(tv, tk);
        return tk;
    }

    /** Tekst koji se postavlja u TextView (tema "Православац"): samo font, tekst i boje ostaju netaknuti. */
    static void fontText(TextView tv, CharSequence cs) {
        if (inSettings(tv)) return;
        String rk = roleKey(tv);
        if (rk == null && isCalTitleText(cs)) {
            tv.setTag(TAG_TITLE, Boolean.TRUE);
            rk = "c_cal_title_text";
        }
        if (rk != null) {
            Fonts.apply(tv, rk);
            return;
        }
        if (fontTextRole(tv, cs) == null) {
            String sk = spanFontKey(cs);
            if (sk != null) Fonts.apply(tv, sk);
        }
    }

    /** Red bocnog menija (tema "Православац"): font prema originalnoj boji teksta. */
    static void fontMenu(View v, boolean stable) {
        if (v == null) return;
        try {
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                Fonts.apply(tv, menuKey(tv.getTextColors().getDefaultColor(), stable));
            }
            if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) fontMenu(g.getChildAt(i), stable);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /** Red liste molitvi i knjiga (tema "Православац"): font stavki liste. */
    static void fontBook(View v) {
        if (v == null) return;
        try {
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                if (fgKey(tv.getTextColors().getDefaultColor()) != null) Fonts.apply(tv, "c_web_text");
            }
            if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) fontBook(g.getChildAt(i));
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void fontsOne(View v) {
        if (v.getClass().getName().endsWith("TabLayout")) watchTabFonts(v);
        if (isToolbar(v)) {
            v.setTag(TAG_BAR, Boolean.TRUE);
            fontsIn(v, "c_toolbar_text");
        }
        if (v instanceof ListView && !(v instanceof android.widget.ExpandableListView)) {
            Drawable div = ((ListView) v).getDivider();
            if (div instanceof ColorDrawable && (((ColorDrawable) div).getColor() & 0xffffff) == 0xdc143c) {
                v.setTag(TAG_BOOKLIST, Boolean.TRUE);   // lista molitvi i knjiga
            }
        }
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            String rk = roleKey(tv);
            if (rk == null && isCalTitleText(tv.getText())) {
                tv.setTag(TAG_TITLE, Boolean.TRUE);
                rk = "c_cal_title_text";
            }
            if (rk != null) {
                Fonts.apply(tv, rk);
                return;
            }
            if (fontTextRole(tv, tv.getText()) != null) return;
            String fk = inTabs(tv) ? "c_tab_text" : fgKey(tv.getTextColors().getDefaultColor());
            if (fk != null) Fonts.apply(tv, fk);
            else Fonts.refresh(tv);
        }
    }

    /** Prolaz kroz hijerarhiju view-ova samo zbog fontova (tema "Православац"): ni jedna boja se ne mijenja. */
    static void walkFonts(View v) {
        if (v == null || inSettings(v)) return;
        try {
            fontsOne(v);
            if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) walkFonts(g.getChildAt(i));
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
