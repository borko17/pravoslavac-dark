package patch.kalendar;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Izvrsava se u sistemskom procesu (scope "System Framework"): modul postaje vidljiv aplikacijama, pa Pravoslavac
 * (Android 11+, vidljivost paketa) moze da pozove FontProvider modula, koji radi mrezu za prozor fontova.
 * Hvata AppsFilter.shouldFilterApplication i za ciljni paket modula vraca "ne filtriraj".
 */
final class SystemHook {

    private static final String[] CLASSES = {
            "com.android.server.pm.AppsFilterImpl",
            "com.android.server.pm.AppsFilterBase",
            "com.android.server.pm.AppsFilter",
    };

    private SystemHook() {
    }

    static void install(ClassLoader cl) {
        XC_MethodHook h = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (param.args == null) return;
                    for (Object a : param.args) {
                        if (a != null && isModule(a)) {
                            param.setResult(Boolean.FALSE);   // modul je vidljiv
                            return;
                        }
                    }
                } catch (Throwable ignored) {
                    // nikad ne smije srusiti sistemski proces
                }
            }
        };
        int n = 0;
        for (String c : CLASSES) {
            try {
                Class<?> k = XposedHelpers.findClass(c, cl);
                if (k == null) continue;
                boolean has = false;
                for (Method m : k.getDeclaredMethods()) {
                    if ("shouldFilterApplication".equals(m.getName())) has = true;
                }
                if (!has) continue;
                XposedBridge.hookAllMethods(k, "shouldFilterApplication", h);
                n++;
            } catch (Throwable t) {
                // druga verzija Androida: klasa ne postoji
            }
        }
        XposedBridge.log("PravoslavacDark: vidljivost modula, hookovano klasa: " + n);
    }

    /** Argument je podesavanje (Setting/State) paketa modula, ako mu getPackageName() daje paket modula. */
    private static boolean isModule(Object a) {
        if (a instanceof Number || a instanceof Boolean || a instanceof String) return false;
        try {
            Method m = a.getClass().getMethod("getPackageName");
            Object r = m.invoke(a);
            return FontCatalog.MODULE_APP_ID.equals(r);
        } catch (Throwable t) {
            return false;
        }
    }
}
