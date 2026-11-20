package patch.kalendar;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * Provjera da li TTF font zaista ima potrebna slova: cita se tabela "cmap" iz samog fajla (bez Android API-ja).
 * <p>
 * Paint.hasGlyph() nije pouzdan za ovo: Typeface.createFromFile() uz font dodaje sistemske rezervne fontove, pa
 * hasGlyph() vrati "true" i kad ga font nema (slovo bi se tada iscrtalo iz sistemskog fonta, ne iz izabranog).
 */
final class FontGlyphs {
    private FontGlyphs() {}

    /** Ж Ђ Ћ Џ Ј Љ Њ i odgovarajuca mala slova (sva ćirilična slova koja srpski tekst koristi, a ruski ne mora). */
    static final int[] SERBIAN = {
            0x0416, 0x0402, 0x040B, 0x040F, 0x0408, 0x0409, 0x040A,
            0x0436, 0x0452, 0x045B, 0x045F, 0x0458, 0x0459, 0x045A };

    /** TRUE: font ima sve znakove; FALSE: nekog nema; null: tabela cmap se ne moze procitati (nepoznat format). */
    static Boolean hasAll(File f, int[] cps) {
        try {
            RandomAccessFile r = new RandomAccessFile(f, "r");
            try {
                byte[] cmap = readCmap(r);
                if (cmap == null) return null;
                return check(cmap, cps);
            } finally {
                r.close();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    /** Isto za tekst (razmaci i tacke se ne provjeravaju). */
    static Boolean hasAllOf(File f, String text) {
        int[] tmp = new int[text.length()];
        int n = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == ' ' || cp == '.') continue;
            tmp[n++] = cp;
        }
        int[] cps = new int[n];
        System.arraycopy(tmp, 0, cps, 0, n);
        return hasAll(f, cps);
    }

    private static byte[] readCmap(RandomAccessFile r) throws Exception {
        long base = 0;
        byte[] head = new byte[12];
        r.seek(0);
        r.readFully(head);
        ByteBuffer h = ByteBuffer.wrap(head);
        if (h.getInt(0) == 0x74746366) {   // "ttcf": kolekcija, uzima se prvi font
            base = h.getInt(12) & 0xffffffffL;
            r.seek(base);
            r.readFully(head);
            h = ByteBuffer.wrap(head);
        }
        int num = h.getShort(4) & 0xffff;
        byte[] dir = new byte[num * 16];
        r.seek(base + 12);
        r.readFully(dir);
        ByteBuffer d = ByteBuffer.wrap(dir);
        for (int i = 0; i < num; i++) {
            if (d.getInt(i * 16) == 0x636d6170) {   // "cmap"
                long off = d.getInt(i * 16 + 8) & 0xffffffffL;
                int len = d.getInt(i * 16 + 12);
                if (len <= 0 || len > 4 * 1024 * 1024) return null;
                byte[] t = new byte[len];
                r.seek(off);
                r.readFully(t);
                return t;
            }
        }
        return null;
    }

    private static Boolean check(byte[] cmap, int[] cps) {
        ByteBuffer b = ByteBuffer.wrap(cmap);
        int n = b.getShort(2) & 0xffff;
        boolean any = false;
        boolean[] found = new boolean[cps.length];
        for (int i = 0; i < n; i++) {
            int platform = b.getShort(4 + i * 8) & 0xffff;
            int enc = b.getShort(6 + i * 8) & 0xffff;
            int off = b.getInt(8 + i * 8);
            boolean unicode = platform == 0 || (platform == 3 && (enc == 1 || enc == 10));
            if (!unicode || off < 0 || off + 4 > cmap.length) continue;
            int fmt = b.getShort(off) & 0xffff;
            if (fmt != 4 && fmt != 6 && fmt != 12) continue;
            any = true;
            for (int k = 0; k < cps.length; k++) {
                if (!found[k] && lookup(b, off, fmt, cps[k])) found[k] = true;
            }
        }
        if (!any) return null;
        for (boolean ok : found) if (!ok) return Boolean.FALSE;
        return Boolean.TRUE;
    }

    private static boolean lookup(ByteBuffer b, int off, int fmt, int cp) {
        try {
            if (fmt == 4) {
                if (cp > 0xffff) return false;
                int segX2 = b.getShort(off + 6) & 0xffff;
                int endPos = off + 14;
                int startPos = endPos + segX2 + 2;
                int deltaPos = startPos + segX2;
                int roPos = deltaPos + segX2;
                for (int s = 0; s < segX2 / 2; s++) {
                    int end = b.getShort(endPos + s * 2) & 0xffff;
                    if (cp > end) continue;
                    int start = b.getShort(startPos + s * 2) & 0xffff;
                    if (cp < start) return false;
                    int delta = b.getShort(deltaPos + s * 2);
                    int ro = b.getShort(roPos + s * 2) & 0xffff;
                    if (ro == 0) return ((cp + delta) & 0xffff) != 0;
                    int g = b.getShort(roPos + s * 2 + ro + (cp - start) * 2) & 0xffff;
                    return g != 0 && ((g + delta) & 0xffff) != 0;
                }
                return false;
            }
            if (fmt == 6) {
                int first = b.getShort(off + 6) & 0xffff;
                int count = b.getShort(off + 8) & 0xffff;
                if (cp < first || cp >= first + count) return false;
                return (b.getShort(off + 10 + (cp - first) * 2) & 0xffff) != 0;
            }
            long groups = b.getInt(off + 12) & 0xffffffffL;
            for (int g = 0; g < groups; g++) {
                int p = off + 16 + g * 12;
                long start = b.getInt(p) & 0xffffffffL;
                long end = b.getInt(p + 4) & 0xffffffffL;
                if (cp < start) return false;
                if (cp <= end) return ((b.getInt(p + 8) & 0xffffffffL) + (cp - start)) != 0;
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}
