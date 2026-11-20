package patch.kalendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Preuzimanje fontova sa Google Fonts (izvrsava se u aplikaciji modula, koja ima INTERNET dozvolu).
 * Preuzeti TTF se cuva lokalno (za pregled) i upisuje u postavke modula, odakle ga cita Pravoslavac (vidi Fonts).
 */
final class FontStore {
    private FontStore() {}


    static final class HttpError extends IOException {
        final int code;

        HttpError(int code) {
            super("HTTP " + code);
            this.code = code;
        }
    }

    /** Postavke modula (privatne: Pravoslavac fontove dobija preko FontProvider-a, ne citanjem postavki modula). */
    private static SharedPreferences prefs(Context c, String name) {
        return c.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    /** Verzija preuzetog fonta (0 = nije preuzet). */
    static long version(Context c, String family) {
        return prefs(c, FontCatalog.PREFS_INDEX).getLong("v_" + FontCatalog.slug(family), 0L);
    }

    /** Fajl preuzetog fonta (regularni ili podebljani) po "slug"-u, ili null. */
    static File fontFile(Context c, String slug, boolean bold) {
        File f = new File(localDir(c), slug + (bold ? "-b.ttf" : "-r.ttf"));
        return f.isFile() ? f : null;
    }

    private static final String CATALOG_PREFS = "fonts_catalog";
    private static final String CATALOG_URL = "https://fonts.google.com/metadata/fonts";

    /** Spisak fontova sa ćirilicom: osvježen sa Googlea ako je osvježavan, inače ugrađeni (FontCatalog.FAMILIES). */
    static String[] catalog(Context c) {
        String s = c.getSharedPreferences(CATALOG_PREFS, Context.MODE_PRIVATE).getString("list", "");
        String[] a = s.length() == 0 ? FontCatalog.FAMILIES : s.split("\\|");
        List<String> out = new ArrayList<String>();
        for (String f : a) {
            if (!excluded(f)) out.add(f);
        }
        String[] r = out.toArray(new String[0]);
        if (s.length() > 0) Arrays.sort(r, String.CASE_INSENSITIVE_ORDER);
        return r;
    }

    /** Familije koje se nikad ne nude: "Flow ..." (Flow Block, Flow Circular, Flow Rounded) nemaju slova, samo crtice. */
    static boolean excluded(String family) {
        return family.regionMatches(true, 0, "Flow", 0, 4);
    }

    private static final String BAD_KEY = "bad";

    /** Fontovi ciji je pregled pokazao da nemaju sva srpska slova (pamte se, pa se ne nude ni poslije ponovnog pokretanja). */
    static java.util.Set<String> badPreviews(Context c) {
        java.util.Set<String> out = new java.util.HashSet<String>();
        String s = c.getSharedPreferences(CATALOG_PREFS, Context.MODE_PRIVATE).getString(BAD_KEY, "");
        for (String f : s.split("\\|")) {
            if (f.length() > 0) out.add(f);
        }
        return out;
    }

    static void markBad(Context c, String family) {
        java.util.Set<String> bad = badPreviews(c);
        if (!bad.add(family)) return;
        StringBuilder sb = new StringBuilder();
        for (String f : bad) {
            if (sb.length() > 0) sb.append('|');
            sb.append(f);
        }
        c.getSharedPreferences(CATALOG_PREFS, Context.MODE_PRIVATE).edit().putString(BAD_KEY, sb.toString()).commit();
    }

    /** Preuzima spisak svih Google fontova i čuva one sa podskupom "cyrillic". Vraća broj fontova. Blokira. */
    static int refreshCatalog(Context c) throws Exception {
        String json = new String(get(CATALOG_URL), "UTF-8");
        int i = json.indexOf('{');   // odgovor počinje zaštitnim prefiksom )]}'
        if (i < 0) throw new IOException("Neočekivan odgovor Googlea");
        JSONArray arr = new JSONObject(json.substring(i)).getJSONArray("familyMetadataList");
        List<String> out = new ArrayList<String>();
        for (int k = 0; k < arr.length(); k++) {
            JSONObject f = arr.getJSONObject(k);
            JSONArray subs = f.optJSONArray("subsets");
            if (subs == null) continue;
            for (int j = 0; j < subs.length(); j++) {
                if ("cyrillic".equals(subs.optString(j))) {
                    String fam = f.optString("family", "");
                    if (fam.length() > 0 && fam.indexOf('|') < 0) out.add(fam);
                    break;
                }
            }
        }
        if (out.isEmpty()) throw new IOException("Spisak je prazan (format odgovora se promijenio?)");
        java.util.Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        StringBuilder sb = new StringBuilder();
        for (String f : out) {
            if (sb.length() > 0) sb.append('|');
            sb.append(f);
        }
        c.getSharedPreferences(CATALOG_PREFS, Context.MODE_PRIVATE).edit().putString("list", sb.toString()).commit();
        return out.size();
    }

    private static File localDir(Context c) {
        return new File(c.getFilesDir(), "fonts");
    }

    /** Lokalni (regularni) fajl preuzetog fonta, ili null. */
    static File localFile(Context c, String family) {
        File f = new File(localDir(c), FontCatalog.slug(family) + "-r.ttf");
        return f.isFile() ? f : null;
    }

    static List<String> installed(Context c) {
        List<String> out = new ArrayList<String>();
        String s = prefs(c, FontCatalog.PREFS_INDEX).getString("list", "");
        for (String f : s.split("\\|")) {
            if (f.length() > 0) out.add(f);
        }
        return out;
    }

    private static void saveList(Context c, List<String> l, String slug, long ver) {
        StringBuilder sb = new StringBuilder();
        for (String f : l) {
            if (sb.length() > 0) sb.append('|');
            sb.append(f);
        }
        SharedPreferences.Editor e = prefs(c, FontCatalog.PREFS_INDEX).edit().putString("list", sb.toString());
        if (ver != 0L) e.putLong("v_" + slug, ver);
        else e.remove("v_" + slug);
        e.commit();
    }

    // ------------------------------------------------------------------ mreza

    private static byte[] get(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            // nepoznat User-Agent: Google vraca TTF (a ne woff2) fajlove
            c.setRequestProperty("User-Agent", "PravoslavacDark/1.0");
            int code = c.getResponseCode();
            if (code != 200) throw new HttpError(code);
            InputStream in = c.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            in.close();
            return out.toByteArray();
        } finally {
            c.disconnect();
        }
    }

    private static void write(File f, byte[] data) throws IOException {
        FileOutputStream o = new FileOutputStream(f);
        try {
            o.write(data);
        } finally {
            o.close();
        }
    }

    /**
     * Provjera da font ima sva ćirilična slova koja srpski tekst koristi (Ж Ђ Ћ Џ Ј Љ Њ i mala), citanjem tabele cmap
     * iz fajla (pouzdano na svim verzijama; Paint.hasGlyph() racuna i sistemske rezervne fontove).
     */
    private static boolean hasCyrillic(File f) {
        Boolean r = FontGlyphs.hasAll(f, FontGlyphs.SERBIAN);
        if (r != null) return r.booleanValue();
        // cmap se ne moze procitati: stara provjera (Android 6+)
        if (Build.VERSION.SDK_INT < 23) return true;
        try {
            Paint p = new Paint();
            p.setTypeface(Typeface.createFromFile(f));
            for (String ch : new String[] { "Ж", "Ђ", "Ћ", "Џ", "Ј", "Љ", "Њ", "ж", "ђ", "ћ", "џ", "ј", "љ", "њ" }) {
                if (!p.hasGlyph(ch)) return false;
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Brise preuzete fontove koji nemaju sva srpska slova (npr. preuzete prije stroge provjere). Vraca broj obrisanih. */
    static int purgeIncomplete(Context c) {
        int n = 0;
        for (String fam : installed(c)) {
            File f = localFile(c, fam);
            if (f == null) continue;
            Boolean r = FontGlyphs.hasAll(f, FontGlyphs.SERBIAN);
            if (r != null && !r.booleanValue()) {
                delete(c, fam);
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ pregled prije preuzimanja

    private static File previewDir(Context c) {
        return new File(c.getCacheDir(), "fontprev");
    }

    /** Već preuzet isječak za pregled (iz keša), ili null. */
    static File previewCached(Context c, String family) {
        File f = new File(previewDir(c), FontCatalog.slug(family) + ".ttf");
        return f.isFile() && f.length() > 0 ? f : null;
    }

    /**
     * Preuzima mali isječak fonta samo sa slovima teksta pregleda (nekoliko KB) i čuva ga u kešu; cijeli font se
     * preuzima tek pri izboru. Blokira: pozivati sa pozadinske niti.
     */
    static File preview(Context c, String family) throws Exception {
        File cached = previewCached(c, family);
        if (cached != null) return cached;
        String css = new String(get("https://fonts.googleapis.com/css2?family=" + URLEncoder.encode(family, "UTF-8")
                + "&text=" + URLEncoder.encode(FontCatalog.PREVIEW_TEXT, "UTF-8")), "UTF-8");
        Matcher u = Pattern.compile("url\\(([^)]+)\\)").matcher(css);
        if (!u.find()) throw new IOException("Nema isječka fonta");
        String url = u.group(1).replace("'", "").replace("\"", "").trim();
        byte[] data = get(url);
        File d = previewDir(c);
        d.mkdirs();
        File f = new File(d, FontCatalog.slug(family) + ".ttf");
        write(f, data);
        return f;
    }

    /** Da li isječak sadrži sva slova teksta pregleda (Android 6+; ranije se ne provjerava). */
    static boolean previewComplete(File f) {
        Boolean r = FontGlyphs.hasAllOf(f, FontCatalog.PREVIEW_TEXT);
        if (r != null) return r.booleanValue();
        if (Build.VERSION.SDK_INT < 23) return true;
        try {
            Paint p = new Paint();
            p.setTypeface(Typeface.createFromFile(f));
            String t = FontCatalog.PREVIEW_TEXT;
            for (int i = 0; i < t.length(); i++) {
                char ch = t.charAt(i);
                if (ch == ' ' || ch == '.') continue;
                if (!p.hasGlyph(String.valueOf(ch))) return false;
            }
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /** Preuzima font (blokira: pozivati sa pozadinske niti). Baca izuzetak sa porukom ako ne uspije. */
    static void download(Context c, String family) throws Exception {
        String enc = URLEncoder.encode(family, "UTF-8");
        String base = "https://fonts.googleapis.com/css2?family=" + enc;
        String css;
        try {
            css = new String(get(base + ":wght@400;700"), "UTF-8");
        } catch (HttpError e) {
            if (e.code != 400) throw e;
            css = new String(get(base), "UTF-8");   // font bez podebljanog stila
        }
        if (css.contains("unicode-range")) throw new IOException("Neočekivan format odgovora (woff2)");

        String regUrl = null;
        String boldUrl = null;
        Matcher fm = Pattern.compile("@font-face\\s*\\{([^}]*)\\}").matcher(css);
        while (fm.find()) {
            String b = fm.group(1);
            Matcher u = Pattern.compile("url\\(([^)]+)\\)").matcher(b);
            if (!u.find()) continue;
            String url = u.group(1).replace("'", "").replace("\"", "").trim();
            Matcher st = Pattern.compile("font-style:\\s*(\\w+)").matcher(b);
            if (st.find() && !"normal".equals(st.group(1))) continue;
            int w1 = 400;
            boolean range = false;
            Matcher w = Pattern.compile("font-weight:\\s*(\\d+)(\\s+\\d+)?").matcher(b);
            if (w.find()) {
                w1 = Integer.parseInt(w.group(1));
                range = w.group(2) != null;   // promjenljivi font: jedan fajl za sve tezine
            }
            if (range || w1 == 400) {
                if (regUrl == null) regUrl = url;
            } else if (w1 == 700) {
                if (boldUrl == null) boldUrl = url;
            }
        }
        if (regUrl == null) throw new IOException("Font nema običan stil");
        if (boldUrl != null && boldUrl.equals(regUrl)) boldUrl = null;

        byte[] reg = get(regUrl);
        byte[] bold = boldUrl == null ? null : get(boldUrl);

        String slug = FontCatalog.slug(family);
        File d = localDir(c);
        d.mkdirs();
        File rf = new File(d, slug + "-r.ttf");
        File bf = new File(d, slug + "-b.ttf");
        write(rf, reg);
        if (!hasCyrillic(rf)) {
            rf.delete();
            throw new IOException("Font nema ćirilicu (ili nema sva srpska slova)");
        }
        if (bold != null) write(bf, bold);
        else bf.delete();

        long ver = System.currentTimeMillis();
        SharedPreferences.Editor e = prefs(c, FontCatalog.PREFS_FONT + slug).edit()
                .putString("reg", Base64.encodeToString(reg, Base64.NO_WRAP))
                .putLong("ver", ver);
        if (bold != null) e.putString("bold", Base64.encodeToString(bold, Base64.NO_WRAP));
        else e.remove("bold");
        e.commit();

        List<String> l = installed(c);
        if (!l.contains(family)) l.add(family);
        saveList(c, l, slug, ver);
    }

    /** Brise sve preuzete fontove odjednom. */
    static void deleteAll(Context c) {
        for (String f : installed(c)) delete(c, f);
    }

    /** Brise preuzeti font (lokalno i iz postavki modula). */
    static void delete(Context c, String family) {
        String slug = FontCatalog.slug(family);
        new File(localDir(c), slug + "-r.ttf").delete();
        new File(localDir(c), slug + "-b.ttf").delete();
        prefs(c, FontCatalog.PREFS_FONT + slug).edit().clear().commit();
        try {
            new File(c.getApplicationInfo().dataDir, "shared_prefs/" + FontCatalog.PREFS_FONT + slug + ".xml").delete();
        } catch (Throwable t) {
            // ostaje prazan fajl postavki
        }
        List<String> l = installed(c);
        l.remove(family);
        saveList(c, l, slug, 0L);
    }
}
