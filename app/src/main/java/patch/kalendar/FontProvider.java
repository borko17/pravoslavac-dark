package patch.kalendar;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Most izmedju Pravoslavca (nema INTERNET) i modula (ima): prozor za fontove u podesavanjima Pravoslavca
 * poziva ovaj provider, a mrezni dio (spisak, pregled, preuzimanje) radi proces modula (FontStore).
 * Izvrsava se u procesu modula, zato ovdje nema Xposed klasa. Dozvoljeni pozivaoci: Pravoslavac i Zitije svetih.
 */
public final class FontProvider extends ContentProvider {

    private static final String[] ALLOWED = { "com.pravoslavac", "com.zitijesvetih" };

    @Override
    public boolean onCreate() {
        return true;
    }

    private boolean allowed() {
        String p = getCallingPackage();
        if (p == null) return true;   // poziv iz istog procesa
        Context c = getContext();
        if (c != null && p.equals(c.getPackageName())) return true;
        for (String a : ALLOWED) {
            if (a.equals(p)) return true;
        }
        return false;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Bundle out = new Bundle();
        try {
            if (!allowed()) throw new SecurityException("Није дозвољено");
            Context c = getContext();
            if (c == null || method == null) throw new IllegalStateException("Модул није спреман");
            boolean needsArg = "preview".equals(method) || "download".equals(method)
                    || "delete".equals(method) || "bad".equals(method);
            if (needsArg && (arg == null || arg.length() == 0 || arg.length() > 120)) {
                throw new IllegalArgumentException("Нема назива фонта");
            }
            if ("state".equals(method)) {
                FontStore.purgeIncomplete(c);
                out.putStringArray("catalog", FontStore.catalog(c));
                java.util.List<String> inst = FontStore.installed(c);
                out.putStringArray("installed", inst.toArray(new String[0]));
                long[] vers = new long[inst.size()];
                for (int i = 0; i < vers.length; i++) vers[i] = FontStore.version(c, inst.get(i));
                out.putLongArray("vers", vers);
                out.putStringArray("bad", FontStore.badPreviews(c).toArray(new String[0]));
            } else if ("refresh".equals(method)) {
                out.putInt("count", FontStore.refreshCatalog(c));
            } else if ("preview".equals(method)) {
                File f = FontStore.preview(c, arg);
                boolean ok = FontStore.previewComplete(f);
                if (ok) out.putByteArray("data", readAll(f));
                else FontStore.markBad(c, arg);
                out.putBoolean("complete", ok);
            } else if ("download".equals(method)) {
                FontStore.download(c, arg);
                out.putLong("ver", FontStore.version(c, arg));
            } else if ("delete".equals(method)) {
                FontStore.delete(c, arg);
            } else if ("deleteAll".equals(method)) {
                FontStore.deleteAll(c);
            } else if ("bad".equals(method)) {
                FontStore.markBad(c, arg);
            } else {
                throw new IllegalArgumentException("Непозната метода: " + method);
            }
        } catch (Throwable t) {
            out.putString("error", t.getMessage() != null ? t.getMessage() : t.toString());
        }
        return out;
    }

    private static byte[] readAll(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                o.write(buf, 0, n);
            }
            return o.toByteArray();
        } finally {
            in.close();
        }
    }

    /** content://AUTHORITY/font/<slug>/reg|bold: fajl preuzetog fonta (Pravoslavac ga kopira u svoj prostor). */
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!allowed()) throw new SecurityException("Није дозвољено");
        java.util.List<String> seg = uri.getPathSegments();
        if (seg.size() != 3 || !"font".equals(seg.get(0)) || !seg.get(1).matches("[a-z0-9_]+")) {
            throw new FileNotFoundException();
        }
        Context c = getContext();
        File f = c == null ? null : FontStore.fontFile(c, seg.get(1), "bold".equals(seg.get(2)));
        if (f == null) throw new FileNotFoundException();
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
