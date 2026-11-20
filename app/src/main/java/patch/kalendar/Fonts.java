package patch.kalendar;

import android.graphics.Typeface;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XposedBridge;

/**
 * Fontovi po vrsti teksta (izvrsava se u procesu Pravoslavca / Zitija svetih).
 * <p>
 * Fontove preuzima aplikacija modula (ona ima INTERNET) i upisuje u svoje postavke (FontCatalog.PREFS_*), a ovdje se
 * jednom uvezu u podatke aplikacije (files/fonts) i odatle koriste: nativni tekst preko Typeface-a, WebView preko
 * "lazne" putanje asseta (vidi Hook.INTERCEPT i webCss()).
 */
final class Fonts {
    private Fonts() {}

    private static final int TAG_FKEY = 0x7e0c0a21;
    private static final int TAG_FSTYLE = 0x7e0c0a22;
    private static final int TAG_FORIG = 0x7e0c0a23;
    private static final int TAG_FSIZE = 0x7e0c0a24;
    private static final int TAG_ZOOM0 = 0x7e0c0a25;
    private static final int TAG_FLAY = 0x7e0c0a26;
    private static final int TAG_FPAD = 0x7e0c0a27;

    /** Postavlja se dok modul sam mijenja font (da hook za setTypeface to ne "popravlja"). */
    static volatile boolean busy;
    /** Postavlja se dok modul sam pravi Typeface iz fajla (da se ne smatra fontom aplikacije). */
    static volatile boolean creating;

    static boolean isCsFont(String path) {
        if (creating || path == null || path.isEmpty()) return false;
        String l = path.toLowerCase(java.util.Locale.ROOT);
        if (l.contains("miroslav")) return false;
        return l.contains("triodion") || l.contains("ieucs") || l.contains("irmol") || l.contains("skun");
    }

    // fontovi koje aplikacija sama ucitava (iz asseta ili fajla) i njihovi stilovi: crkvenoslovenski fontovi
    private static final Set<Typeface> APP_FACES =
            Collections.synchronizedSet(Collections.newSetFromMap(new java.util.WeakHashMap<Typeface, Boolean>()));

    static void noteAppFace(Typeface t) {
        if (t != null && !creating) APP_FACES.add(t);
    }

    static boolean isAppFace(Typeface t) {
        return t != null && APP_FACES.contains(t);
    }

    /** Stil (podebljano/kurziv) fonta aplikacije je isti font: bilježi se i izvedeni Typeface. */
    static void noteDerived(Typeface base, Typeface result) {
        if (isAppFace(base) && result != null) APP_FACES.add(result);
    }

    private static final class Face {
        Typeface reg;
        Typeface bold;
        final Typeface[] styled = new Typeface[4];
    }

    private static final Map<String, Face> FACES = new HashMap<String, Face>();
    // Preuzeti fontovi se cuvaju u prostoru Pravoslavca (files/fonts): slug-r.ttf, slug-b.ttf i spisak index.txt
    // (red: naziv fonta, tabulator, verzija). Kopira ih tu FontClient iz modula (FontProvider), ne cita se nista iz modula.
    private static final Map<String, Long> LOCAL = new java.util.LinkedHashMap<String, Long>();
    private static long localStamp = -1L;
    private static long localCheck;
    private static boolean migrating;

    private static File indexFile() {
        return new File(dir(), "index.txt");
    }

    private static long stamp(File f) {
        return f.isFile() ? f.lastModified() * 31L + f.length() : 0L;
    }

    private static synchronized Map<String, Long> localIndex() {
        long now = System.currentTimeMillis();
        if (localStamp < 0L || now - localCheck > 1500L) {
            localCheck = now;
            File f = indexFile();
            long st = stamp(f);
            if (st != localStamp) {
                localStamp = st;
                LOCAL.clear();
                if (f.isFile()) {
                    try {
                        java.io.BufferedReader r = new java.io.BufferedReader(
                                new java.io.InputStreamReader(new java.io.FileInputStream(f), "UTF-8"));
                        try {
                            String line;
                            while ((line = r.readLine()) != null) {
                                int t = line.lastIndexOf('\t');
                                if (t <= 0) continue;
                                try {
                                    LOCAL.put(line.substring(0, t), Long.valueOf(Long.parseLong(line.substring(t + 1).trim())));
                                } catch (NumberFormatException ignored) {
                                    // preskoci neispravan red
                                }
                            }
                        } finally {
                            r.close();
                        }
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                }
            }
        }
        return LOCAL;
    }

    private static synchronized void saveLocal() {
        try {
            File d = dir();
            d.mkdirs();
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Long> e : LOCAL.entrySet()) {
                sb.append(e.getKey()).append('\t').append(e.getValue().longValue()).append('\n');
            }
            write(indexFile(), sb.toString().getBytes("UTF-8"));
            localStamp = stamp(indexFile());
            localCheck = System.currentTimeMillis();
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /** Verzija fonta u Pravoslavcu (0 = nije preuzet). */
    static synchronized long localVer(String family) {
        Long v = localIndex().get(family);
        return v == null ? 0L : v.longValue();
    }

    static synchronized List<String> localFamilies() {
        return new ArrayList<String>(localIndex().keySet());
    }

    static synchronized void putLocal(String family, long ver) {
        localIndex();
        LOCAL.put(family, Long.valueOf(ver));
        saveLocal();
        FACES.remove(family);
    }

    /** Brise font iz Pravoslavca (fajlovi i red u spisku). */
    static synchronized void removeLocal(String family) {
        localIndex();
        String slug = FontCatalog.slug(family);
        File d = dir();
        new File(d, slug + "-r.ttf").delete();
        new File(d, slug + "-b.ttf").delete();
        new File(d, slug + ".ver").delete();
        LOCAL.remove(family);
        saveLocal();
        FACES.remove(family);
    }

    static synchronized void clearLocal() {
        for (String f : localFamilies()) removeLocal(f);
    }

    /** Pravi (prazan) spisak ako ga nema, da se prenos iz modula ne ponavlja bez potrebe. */
    static synchronized void touchLocalIndex() {
        localIndex();
        if (!indexFile().isFile()) saveLocal();
    }

    /** Verzija iz stare sheme (.ver pored fajla fonta); 0 ako je nema. */
    static long readVerFile(String slug) {
        return readLong(new File(dir(), slug + ".ver"));
    }

    static void writeVerFile(String slug, long ver) {
        try {
            write(new File(dir(), slug + ".ver"), Long.toString(ver).getBytes("UTF-8"));
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /**
     * Prva upotreba poslije prelaska sa citanja postavki modula: fontovi koje je modul vec preuzeo prenose se u pozadini
     * u Pravoslavac (jednom po procesu; ako nema spiska, a modul nije dostupan, ostaje originalni font).
     */
    private static void migrate() {
        if (migrating || indexFile().exists()) return;
        migrating = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Object app = Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
                    if (app instanceof android.content.Context) {
                        new FontClient((android.content.Context) app).sync();
                    }
                } catch (Throwable t) {
                    XposedBridge.log(t);
                } finally {
                    reset();
                }
            }
        }, "pdk-fonts-sync").start();
    }

    /** Fontovi preuzeti u Pravoslavac (za izbor u uredjivacu tema). */
    static synchronized List<String> available() {
        List<String> out = new ArrayList<String>();
        try {
            for (String f : localIndex().keySet()) {
                if (f.length() > 0 && hasSerbianLetters(f)) out.add(f);   // samo fontovi koji zaista imaju ђ ћ џ љ њ...
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);   // azbucni red
        return out;
    }

    /** Rezultat provjere srpskih slova po fontu i verziji (kljuc: slug + ":" + verzija). */
    private static final Map<String, Boolean> SERBIAN_OK = new HashMap<String, Boolean>();

    /** Font ima sva srpska slova (Ж Ђ Ћ Џ Ј Љ Њ i mala)? Ako se ne moze provjeriti, smatra se da ima. */
    private static synchronized boolean hasSerbianLetters(String family) {
        try {
            String slug = FontCatalog.slug(family);
            String key = slug + ":" + localVer(family);
            Boolean ok = SERBIAN_OK.get(key);
            if (ok != null) return ok.booleanValue();
            File[] c = cached(family);
            if (c == null) return true;
            Boolean r = FontGlyphs.hasAll(c[0], FontGlyphs.SERBIAN);
            boolean res = r == null || r.booleanValue();
            SERBIAN_OK.put(key, Boolean.valueOf(res));
            return res;
        } catch (Throwable t) {
            return true;
        }
    }

    /** Ponovo ucitaj spisak i fontove (poslije promjene izbora ili novog preuzimanja). */
    static synchronized void reset() {
        FACES.clear();
        localCheck = 0L;
        localStamp = -1L;
    }

    static File dir() {
        return new File(Prefs.dir(), "files/fonts");
    }

    private static long readLong(File f) {
        try {
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f));
            try {
                return Long.parseLong(r.readLine().trim());
            } finally {
                r.close();
            }
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static void write(File f, byte[] data) throws Exception {
        FileOutputStream o = new FileOutputStream(f);
        try {
            o.write(data);
        } finally {
            o.close();
        }
    }

    /** {regularni, podebljani ili null} fajl fonta u Pravoslavcu; null ako fonta nema. */
    private static File[] cached(String family) {
        if (localVer(family) == 0L) {
            migrate();   // mozda je font vec preuzet u modulu (prije ove verzije): prenesi ga u pozadini
            return null;
        }
        String slug = FontCatalog.slug(family);
        File d = dir();
        File reg = new File(d, slug + "-r.ttf");
        File bold = new File(d, slug + "-b.ttf");
        if (!reg.isFile()) return null;
        return new File[] { reg, bold.isFile() ? bold : null };
    }

    private static synchronized Face face(String family) {
        Face f = FACES.get(family);
        if (f == null) {
            f = new Face();
            try {
                File[] c = hasSerbianLetters(family) ? cached(family) : null;
                if (c != null) {
                    creating = true;
                    try {
                        f.reg = Typeface.createFromFile(c[0]);
                        if (c[1] != null) f.bold = Typeface.createFromFile(c[1]);
                    } finally {
                        creating = false;
                    }
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
                f.reg = null;
                f.bold = null;
            }
            FACES.put(family, f);
        }
        return f.reg == null ? null : f;
    }

    /** Typeface fonta izabranog za datu boju/tekst (kljuc iz Colors) u datom stilu, ili null (originalni font). */
    static Typeface get(String key, int style) {
        String fam = Prefs.font(key);
        if (fam == null) return null;
        Face f = face(fam);
        if (f == null) return null;
        int s = style & 3;
        synchronized (Fonts.class) {
            Typeface t = f.styled[s];
            if (t == null) {
                Typeface base = f.reg;
                int rest = s;
                if ((s & Typeface.BOLD) != 0 && f.bold != null) {
                    base = f.bold;
                    rest = (s & Typeface.ITALIC) != 0 ? Typeface.ITALIC : Typeface.NORMAL;   // isto kao s & ~BOLD, ali bez lint upozorenja
                }
                t = rest == 0 ? base : Typeface.create(base, rest);
                f.styled[s] = t;
            }
            return t;
        }
    }

    private static synchronized boolean isOurs(Typeface t) {
        if (t == null) return false;
        for (Face f : FACES.values()) {
            if (f.reg == t || f.bold == t) return true;
            for (Typeface s : f.styled) {
                if (s == t) return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ nativni tekst

    private static void set(TextView tv, Typeface t) {
        busy = true;
        try {
            tv.setTypeface(t);
        } finally {
            busy = false;
        }
    }

    private static void setSize(TextView tv, float px) {
        busy = true;
        try {
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px);
        } finally {
            busy = false;
        }
    }

    /**
     * Tekst sa fiksnom visinom (npr. red sa nazivom dana): font sa velikim vertikalnim razmakom ili visokim slovima
     * odsijeca red. Zato se kod takvih tekstova isključuje dodatni razmak fonta (includeFontPadding), a ako i dalje ne
     * stane, veličina se smanji tačno toliko da jedan red stane u zadatu visinu.
     */
    private static float fitSize(TextView tv, float want, Typeface tf) {
        try {
            ViewGroup.LayoutParams lp = tv.getLayoutParams();
            if (lp == null || lp.height <= 0 || tf == null) return want;
            if (tv.getMaxLines() > 1 && tv.getMaxLines() < 100) return want;   // viselinijski tekst ne diramo
            int avail = lp.height - tv.getCompoundPaddingTop() - tv.getCompoundPaddingBottom();
            if (avail <= 0) return want;
            if (tv.getIncludeFontPadding()) {
                if (tv.getTag(TAG_FPAD) == null) tv.setTag(TAG_FPAD, Boolean.TRUE);
                tv.setIncludeFontPadding(false);
            }
            android.graphics.Paint p = new android.graphics.Paint();
            p.setTypeface(tf);
            p.setTextSize(want);
            android.graphics.Paint.FontMetrics fm = new android.graphics.Paint.FontMetrics();
            p.getFontMetrics(fm);
            float h = fm.descent - fm.ascent;
            return h > avail ? want * avail / h : want;
        } catch (Throwable t) {
            return want;
        }
    }

    /**
     * Rezervna provjera poslije rasporeda: ako je jedan red teksta viši od samog TextView-a (visinu mu je zadao layout),
     * veličina se smanjuje dok red ne stane.
     */
    private static void watchFit(final TextView tv) {
        if (tv.getTag(TAG_FLAY) != null) return;
        tv.setTag(TAG_FLAY, Boolean.TRUE);
        tv.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                try {
                    if (!(tv.getTag(TAG_FKEY) instanceof String)) return;
                    android.text.Layout lay = tv.getLayout();
                    if (lay == null || tv.getLineCount() != 1) return;
                    int avail = tv.getHeight() - tv.getCompoundPaddingTop() - tv.getCompoundPaddingBottom();
                    int need = lay.getHeight();
                    if (avail > 0 && need > avail + 1) {
                        float cur = tv.getTextSize();
                        float next = cur * avail / need;
                        if (next < cur - 0.5f) setSize(tv, next);
                    }
                } catch (Throwable e) {
                    XposedBridge.log(e);
                }
            }
        });
    }

    /** Primjenjuje font i velicinu izabrane za tekst "key" na TextView (ili vraca original ako nije izabrano nista). */
    static void apply(TextView tv, String key) {
        if (tv == null || key == null) return;
        try {
            Object oldKey = tv.getTag(TAG_FKEY);
            if (Prefs.keepSlavonic()) {   // tekst u crkvenoslovenskom fontu aplikacije ostaje kakav jeste
                Object fo = tv.getTag(TAG_FORIG);
                Typeface base = fo instanceof Typeface ? (Typeface) fo : tv.getTypeface();
                if (isAppFace(base)) {
                    if (oldKey != null) restore(tv);
                    return;
                }
            }
            String fam = Prefs.font(key);
            int pct = Prefs.fontSize(key);
            if (fam == null && pct == 100) {
                if (oldKey != null) restore(tv);
                return;
            }
            if (tv.getTag(TAG_FSTYLE) == null) {   // prvi put: zapamti izvorni font, stil i velicinu
                Typeface cur = tv.getTypeface();
                tv.setTag(TAG_FORIG, cur == null ? (Object) Boolean.FALSE : (Object) cur);
                tv.setTag(TAG_FSTYLE, Integer.valueOf(cur == null ? 0 : cur.getStyle()));
                tv.setTag(TAG_FSIZE, Float.valueOf(tv.getTextSize()));
            }
            tv.setTag(TAG_FKEY, key);
            Typeface t;
            if (fam == null) {
                Object o = tv.getTag(TAG_FORIG);
                t = o instanceof Typeface ? (Typeface) o : null;
            } else {
                t = get(key, ((Integer) tv.getTag(TAG_FSTYLE)).intValue());
                if (t == null) {   // izabrani font nije (vise) preuzet: vrati originalni
                    Object o = tv.getTag(TAG_FORIG);
                    t = o instanceof Typeface ? (Typeface) o : null;
                }
            }
            if (t != null && tv.getTypeface() != t) set(tv, t);
            float want = ((Float) tv.getTag(TAG_FSIZE)).floatValue() * pct / 100f;
            want = fitSize(tv, want, tv.getTypeface());
            if (Math.abs(tv.getTextSize() - want) > 0.5f) setSize(tv, want);
            watchFit(tv);
        } catch (Throwable e) {
            XposedBridge.log(e);
        }
    }

    /** Ponovo primijeni font koji je ovom TextView-u vec dodijeljen (npr. poslije izmjene izbora u temi). */
    static void refresh(TextView tv) {
        Object k = tv.getTag(TAG_FKEY);
        if (k instanceof String) apply(tv, (String) k);
    }

    private static void restore(TextView tv) {
        Object o = tv.getTag(TAG_FORIG);
        Object sz = tv.getTag(TAG_FSIZE);
        tv.setTag(TAG_FKEY, null);
        tv.setTag(TAG_FSTYLE, null);
        tv.setTag(TAG_FORIG, null);
        tv.setTag(TAG_FSIZE, null);
        set(tv, o instanceof Typeface ? (Typeface) o : null);
        if (sz instanceof Float) setSize(tv, ((Float) sz).floatValue());
        if (tv.getTag(TAG_FPAD) != null) {
            tv.setTag(TAG_FPAD, null);
            tv.setIncludeFontPadding(true);
        }
    }

    /** Poziva se prije TextView.setTextSize(unit, size): velicina se mnozi izabranim procentom za taj tekst. */
    static void onSetTextSize(TextView tv, Object[] args) {
        Object k = tv.getTag(TAG_FKEY);
        if (!(k instanceof String)) return;
        float px = android.util.TypedValue.applyDimension(((Integer) args[0]).intValue(),
                ((Float) args[1]).floatValue(), tv.getResources().getDisplayMetrics());
        tv.setTag(TAG_FSIZE, Float.valueOf(px));
        args[0] = Integer.valueOf(android.util.TypedValue.COMPLEX_UNIT_PX);
        args[1] = Float.valueOf(fitSize(tv, px * Prefs.fontSize((String) k) / 100f, tv.getTypeface()));
    }

    /** Velicina teksta stranica u WebView-u ("Текст житија, књига и молитви"): zoom teksta WebView-a (pozivati na pocetku ucitavanja). */
    static void applyWebZoom(android.webkit.WebView wv) {
        try {
            android.webkit.WebSettings ws = wv.getSettings();
            Object o = wv.getTag(TAG_ZOOM0);
            int base;
            if (o instanceof Integer) {
                base = ((Integer) o).intValue();
            } else {
                base = ws.getTextZoom();
                wv.setTag(TAG_ZOOM0, Integer.valueOf(base));
            }
            int want = Math.round(base * Prefs.fontSize("c_web_body") / 100f);
            if (ws.getTextZoom() != want) ws.setTextZoom(want);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /** Font svim TextView-ovima unutar v (ukljucujuci v). */
    static void applyTree(View v, String key) {
        if (v instanceof TextView) apply((TextView) v, key);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) applyTree(g.getChildAt(i), key);
        }
    }

    /**
     * Poziva se prije TextView.setTypeface: ako je TextView pod nasim fontom, aplikacijin font se zamjenjuje nasim
     * (uz zadrzan stil: podebljano / kurziv).
     */
    static void onSetTypeface(TextView tv, Object[] args) {
        Object k = tv.getTag(TAG_FKEY);
        if (!(k instanceof String)) return;
        Typeface in = (Typeface) args[0];
        if (isOurs(in)) return;
        if (Prefs.keepSlavonic() && isAppFace(in)) {   // aplikacija postavlja crkvenoslovenski font: pusti ga
            tv.setTag(TAG_FORIG, in);
            return;
        }
        int st = (in == null ? 0 : in.getStyle()) | (args.length > 1 ? ((Integer) args[1]).intValue() : 0);
        tv.setTag(TAG_FSTYLE, Integer.valueOf(st & 3));
        Typeface t = get((String) k, st);
        if (t == null) return;
        args[0] = t;
        if (args.length > 1) args[1] = Integer.valueOf(0);
    }

    // ------------------------------------------------------------------ WebView

    /** CSS familija (npr. "pdk_open_sans,sans-serif") fonta izabranog za tekst "key", ili null. */
    static String cssFamily(String key) {
        String fam = Prefs.font(key);
        if (fam == null || face(fam) == null) return null;
        return "pdk_" + FontCatalog.slug(fam) + ",sans-serif";
    }

    /** {kljuc, selektor za font-family, selektor za velicinu (zoom; null = rjesava se drugacije)}. */
    private static final String[][] WEB_RULES = {
            { "c_web_body", "html,body,body *", null },
            { "c_web_link", "a,a *", "a" },
            { "c_title", "h1,h2,h3,h4,h5,h6,h1 *,h2 *,h3 *,h4 *,h5 *,h6 *,.title,.title *,.naslov,.naslov *,.color-red,.color-red *", "h1,h2,h3,h4,h5,h6,.title,.naslov,.color-red" },
            { "c_btn_text", "[data-dkb][data-dkb],[data-dkb][data-dkb] *", "[data-dkb][data-dkb]" },
            { "c_web_mark", null, null },
    };

    /** CSS sa @font-face i pravilima font-family za tekstove u WebView-u (prazno ako nije izabran nijedan font). */
    static String webCss() {
        try {
            StringBuilder faces = new StringBuilder();
            StringBuilder rules = new StringBuilder();
            Set<String> done = new HashSet<String>();
            for (String[] r : WEB_RULES) {
                String css = cssFamily(r[0]);
                if (css != null) {
                    String fam = Prefs.font(r[0]);
                    if (done.add(fam)) faceDecl(faces, fam);
                    if (r[1] != null) {
                        rules.append(excl(r[1])).append("{font-family:").append(css).append(" !important}\n");
                    }
                }
                int pct = Prefs.fontSize(r[0]);
                if (r[2] != null && pct != 100) {
                    rules.append(r[2]).append("{zoom:").append(pct).append("% !important}\n");
                }
            }
            return faces.toString() + rules;
        } catch (Throwable t) {
            XposedBridge.log(t);
            return "";
        }
    }

    /** Uz "ne mijenjaj crkvenoslovenske fontove": elementi sa crkvenoslovenskim slovima (oznaka data-dkcs) i njina deca se izuzimaju. */
    private static String excl(String selectors) {
        if (!Prefs.keepSlavonic()) return selectors;
        StringBuilder b = new StringBuilder();
        for (String sel : selectors.split(",")) {
            if (b.length() > 0) b.append(',');
            b.append(sel).append(":not([data-dkcs]):not([data-dkcs] *)");
        }
        return b.toString();
    }

    private static void faceDecl(StringBuilder sb, String fam) {
        Face f = face(fam);
        if (f == null) return;
        String slug = FontCatalog.slug(fam);
        String base = "file:///android_asset/" + FontCatalog.WEB_DIR + "/";
        sb.append("@font-face{font-family:pdk_").append(slug).append(";font-weight:400;src:url(")
                .append(base).append(slug).append("-r.ttf)}\n");
        if (f.bold != null) {
            sb.append("@font-face{font-family:pdk_").append(slug).append(";font-weight:700;src:url(")
                    .append(base).append(slug).append("-b.ttf)}\n");
        }
    }

    /** Fajl fonta za "lazni" asset zahtjev (npr. open_sans-r.ttf), ili null. */
    static File webFile(String name) {
        if (name == null || !name.matches("[a-z0-9_]+-[rb]\\.ttf")) return null;
        String slug = name.substring(0, name.length() - 6);
        String fam = null;
        for (String f : available()) {
            if (FontCatalog.slug(f).equals(slug)) fam = f;
        }
        if (fam == null) fam = FontCatalog.bySlug(slug);
        if (fam == null) return null;
        File[] c = cached(fam);
        if (c == null) return null;
        return name.endsWith("-b.ttf") ? c[1] : c[0];
    }
}
