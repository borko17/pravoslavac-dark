package patch.kalendar;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Strana Pravoslavca: trazi od modula (FontProvider) spisak, pregled i preuzimanje fontova, jer Pravoslavac nema INTERNET.
 * Svi pozivi blokiraju (pozivati sa pozadinske niti). Izvrsava se u procesu Pravoslavca.
 */
final class FontClient {

    static final class State {
        String[] catalog = new String[0];
        final Set<String> installed = new HashSet<String>();
        final Set<String> bad = new HashSet<String>();
        final java.util.Map<String, Long> vers = new java.util.HashMap<String, Long>();
    }

    static final class Preview {
        final File file;
        final boolean complete;

        Preview(File file, boolean complete) {
            this.file = file;
            this.complete = complete;
        }
    }

    private final Context ctx;

    FontClient(Context c) {
        this.ctx = c;
    }

    @SuppressWarnings("deprecation")
    private Bundle call(String method, String arg) throws IOException {
        Bundle r;
        try {
            r = ctx.getContentResolver().call(Uri.parse("content://" + FontCatalog.PROVIDER_AUTHORITY), method, arg, null);
        } catch (Throwable t) {
            String m = t.getMessage() != null ? t.getMessage() : "";
            if (m.contains("Unknown URI") || m.contains("Unknown URL") || m.contains("Unknown authority")) {
                throw new IOException("Православац не види провајдер модула (видљивост пакета, Android 11+): " + m);
            }
            throw new IOException(t.getClass().getSimpleName() + ": " + m);
        }
        if (r == null) throw new IOException("модул не одговара");
        String e = r.getString("error");
        if (e != null) throw new IOException(e);
        return r;
    }

    State state() throws IOException {
        Bundle b = call("state", null);
        State s = new State();
        String[] cat = b.getStringArray("catalog");
        if (cat != null) s.catalog = cat;
        String[] in = b.getStringArray("installed");
        long[] vs = b.getLongArray("vers");
        if (in != null) {
            s.installed.addAll(Arrays.asList(in));
            for (int i = 0; i < in.length; i++) {
                s.vers.put(in[i], Long.valueOf(vs != null && i < vs.length ? vs[i] : 0L));
            }
        }
        String[] bad = b.getStringArray("bad");
        if (bad != null) s.bad.addAll(Arrays.asList(bad));
        return s;
    }

    int refresh() throws IOException {
        return call("refresh", null).getInt("count", 0);
    }

    /** Preuzima font (modul) i kopira ga u Pravoslavac. */
    void download(String family) throws IOException {
        long ver = call("download", family).getLong("ver", 0L);
        pull(family, ver == 0L ? System.currentTimeMillis() : ver);
    }

    /** Uskladjuje fontove u Pravoslavcu sa fontovima preuzetim u modulu (prenosi nove, brise obrisane). */
    void sync() throws IOException {
        sync(state());
    }

    void sync(State st) throws IOException {
        IOException first = null;
        for (String fam : st.installed) {
            long v = st.vers.containsKey(fam) ? st.vers.get(fam).longValue() : 0L;
            if (v == 0L || Fonts.localVer(fam) == v) continue;
            String slug = FontCatalog.slug(fam);
            if (new File(Fonts.dir(), slug + "-r.ttf").isFile() && Fonts.readVerFile(slug) == v) {
                Fonts.putLocal(fam, v);   // vec preneseno ranijom verzijom
                continue;
            }
            try {
                pull(fam, v);
            } catch (IOException e) {
                if (first == null) first = e;
            }
        }
        for (String fam : Fonts.localFamilies()) {
            if (!st.installed.contains(fam)) Fonts.removeLocal(fam);
        }
        Fonts.touchLocalIndex();
        Fonts.reset();
        if (first != null) throw first;
    }

    private void copy(Uri uri, File dest) throws IOException {
        File tmp = new File(dest.getPath() + ".tmp");
        java.io.InputStream in;
        try {
            in = ctx.getContentResolver().openInputStream(uri);
        } catch (java.io.FileNotFoundException e) {
            throw e;
        } catch (Throwable t) {
            throw new IOException("модул није доступан");
        }
        if (in == null) throw new IOException("нема фајла фонта");
        try {
            FileOutputStream o = new FileOutputStream(tmp);
            try {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) {
                    o.write(buf, 0, n);
                }
            } finally {
                o.close();
            }
        } finally {
            in.close();
        }
        dest.delete();
        if (!tmp.renameTo(dest)) throw new IOException("не могу да снимим фонт");
    }

    private void pull(String family, long ver) throws IOException {
        String slug = FontCatalog.slug(family);
        File d = Fonts.dir();
        d.mkdirs();
        File reg = new File(d, slug + "-r.ttf");
        File bold = new File(d, slug + "-b.ttf");
        String base = "content://" + FontCatalog.PROVIDER_AUTHORITY + "/font/" + slug + "/";
        copy(Uri.parse(base + "reg"), reg);
        try {
            copy(Uri.parse(base + "bold"), bold);
        } catch (java.io.FileNotFoundException e) {
            bold.delete();   // font nema podebljani stil
        }
        Fonts.writeVerFile(slug, ver);
        Fonts.putLocal(family, ver);
    }

    void delete(String family) throws IOException {
        call("delete", family);
        Fonts.removeLocal(family);
    }

    void deleteAll() throws IOException {
        call("deleteAll", null);
        Fonts.clearLocal();
    }

    void markBad(String family) {
        try {
            call("bad", family);
        } catch (Throwable ignored) {
            // nije bitno: pregled ce se ponoviti
        }
    }

    private File cacheFile(String family) {
        File d = new File(ctx.getCacheDir(), "pdk_fontprev");
        d.mkdirs();
        return new File(d, FontCatalog.slug(family) + ".ttf");
    }

    /** Isjecak fonta za pregled (kes u Pravoslavcu; ako ga nema, trazi se od modula). */
    Preview preview(String family) throws IOException {
        File f = cacheFile(family);
        if (f.isFile() && f.length() > 0) {
            boolean ok = FontStore.previewComplete(f);
            if (!ok) markBad(family);
            return new Preview(ok ? f : null, ok);
        }
        Bundle b = call("preview", family);
        if (!b.getBoolean("complete", true)) return new Preview(null, false);
        byte[] data = b.getByteArray("data");
        if (data == null || data.length == 0) throw new IOException("нема исјечка фонта");
        FileOutputStream o = new FileOutputStream(f);
        try {
            o.write(data);
        } finally {
            o.close();
        }
        return new Preview(f, true);
    }
}
