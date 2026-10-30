package patch.kalendar;

import android.app.Activity;
import android.content.res.XResources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Xposed/LSPosed modul za aplikaciju "Pravoslavac": pocetni ekran je kalendar + tamna tema.
 */
public class Hook implements IXposedHookLoadPackage, IXposedHookInitPackageResources {

    static final String PKG = "com.pravoslavac";

    /** Boja krem reda u kalendaru koju aplikacija postavlja kodom (svaki drugi red). */
    static final int LIGHT_ROW = 0xfffbf7ea;
    /** Tamnocrvena boja naslova koju aplikacija postavlja kodom (zamjena: Dark.TITLE). */
    static final int DARK_RED = 0xffcc0000;

    /** Dubina ugnijezdenih LayoutInflater.inflate poziva (po niti). */
    static final ThreadLocal<int[]> DEPTH = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            return new int[1];
        }
    };

    static boolean start;
    static boolean inSplash;

    // ------------------------------------------------------------------ load package

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!PKG.equals(lpparam.packageName)) return;
        final ClassLoader cl = lpparam.classLoader;
        Prefs.init(lpparam.appInfo != null ? lpparam.appInfo.dataDir : null);
        Dark.reload();

        // Splash: ne prikazuj pocetni ekran, odmah idi na kalendar.
        hook("com.pravoslavac.Splashscreen", cl, "onCreate", "android.os.Bundle", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // Pri pokretanju otvori kalendar (tekuci mjesec) umjesto zitija za taj dan.
                start = Prefs.openCalendar();
                inSplash = Prefs.skipSplash();
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                inSplash = false;
                // Ako se uvodni ekran prikazuje, a tamna tema je ukljucena: crna pozadina umjesto bijele.
                if (Prefs.dark() && !Prefs.skipSplash()) {
                    try {
                        Activity a = (Activity) param.thisObject;
                        a.getWindow().setBackgroundDrawable(new ColorDrawable(0xff000000));
                        View content = a.findViewById(android.R.id.content);
                        if (content instanceof ViewGroup) {
                            ViewGroup g = (ViewGroup) content;
                            g.setBackgroundColor(0xff000000);
                            if (g.getChildCount() > 0) g.getChildAt(0).setBackgroundColor(0xff000000);
                        }
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                }
            }
        });

        // Prvi put otvori tekuci mjesec/godinu u kalendaru.
        hook("com.pravoslavac.SlidingTabsPravoslavniCrkveniKalendar_i_raspored_citanja_Fragment", cl, "n0",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (start) {
                            start = false;
                            Calendar c = Calendar.getInstance();
                            Integer month = Integer.valueOf(c.get(Calendar.MONTH) + 1);
                            Integer year = Integer.valueOf(c.get(Calendar.YEAR));
                            XposedHelpers.callMethod(param.thisObject, "Y", new Object[] { month, year });
                        }
                    }
                });

        hook("android.os.Handler", cl, "postDelayed", "java.lang.Runnable", long.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (inSplash) {
                    param.args[1] = Long.valueOf(0L);
                }
            }
        });

        hook("android.app.Activity", cl, "setContentView", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (inSplash) {
                    param.setResult(null);
                }
            }
        });

        // WebView: tamna stranica + JS.
        hook("android.webkit.WebViewClient", cl, "onPageFinished", "android.webkit.WebView", "java.lang.String",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            WebView wv = (WebView) param.args[0];
                            if (Prefs.left()) wv.evaluateJavascript(Assets.JUSTIFY_JS, null);
                            if (Prefs.dark()) wv.evaluateJavascript(Assets.darkJs(), null);
                        } catch (Throwable t) {
                            XposedBridge.log(t);
                        }
                    }
                });

        try {
            XC_MethodHook webViewLoad = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        WebView wv = (WebView) param.thisObject;
                        if (Prefs.dark() || Prefs.left()) wv.getSettings().setJavaScriptEnabled(true);
                        if (Prefs.dark()) wv.setBackgroundColor(Dark.WEB_BG);
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                    }
                }
            };
            Class<?> webView = XposedHelpers.findClass("android.webkit.WebView", cl);
            XposedBridge.hookAllMethods(webView, "loadUrl", webViewLoad);
            XposedBridge.hookAllMethods(webView, "loadDataWithBaseURL", webViewLoad);
            XposedBridge.hookAllMethods(webView, "loadData", webViewLoad);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        hookTheme(cl);
        hookInflater(cl);

        // Pozadine postavljene preko resursa (npr. tablerow_border_neparni_dani).
        hook("android.view.View", cl, "setBackgroundResource", int.class, new OddRowHook());

        // Pozadine / boje teksta koje aplikacija postavlja kodom (parseColor).
        XC_MethodHook colorHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!Prefs.dark()) return;
                try {
                    int c = ((Integer) param.args[0]).intValue();
                    if (c == LIGHT_ROW) {
                        param.args[0] = Integer.valueOf(Dark.ROW);
                    } else if (c == DARK_RED) {
                        param.args[0] = Integer.valueOf(Dark.TITLE);
                    }
                } catch (Throwable t) {
                    XposedBridge.log(t);
                }
            }
        };
        hook("android.view.View", cl, "setBackgroundColor", int.class, colorHook);
        hook("android.widget.TextView", cl, "setTextColor", int.class, colorHook);

        hookText(cl);
        hookCss(cl);
        hookSettingsMenu(cl);

        // Lijevo poravnanje: ugasi justifikaciju u nativnim TextView-ovima (Android 8+).
        hook("android.widget.TextView", cl, "setJustificationMode", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (Prefs.left()) param.args[0] = Integer.valueOf(0);
            }
        });
    }

    /** Skracenica: XposedHelpers.findAndHookMethod uz hvatanje gresaka. Zadnji argument je callback. */
    private static void hook(String cls, ClassLoader cl, String method, Object... paramsAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(cls, cl, method, paramsAndCallback);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    // ------------------------------------------------------------------ pojedinacni hook-ovi

    private static void hookTheme(ClassLoader cl) {
        XC_MethodHook h = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                applyTheme((Activity) param.thisObject);
            }
        };
        hook("android.app.Activity", cl, "onCreate", "android.os.Bundle", h);
        hook("androidx.fragment.app.FragmentActivity", cl, "onCreate", "android.os.Bundle", h);
    }

    private static void applyTheme(Activity a) {
        if (!Prefs.dark()) return;
        if (!a.getClass().getName().startsWith(PKG + ".")) return;
        try {
            int id = a.getResources().getIdentifier("Theme.AppCompat", "style", PKG);
            if (id != 0) a.setTheme(id);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void hookInflater(ClassLoader cl) {
        XC_MethodHook h = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                DEPTH.get()[0]++;
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                int[] d = DEPTH.get();
                d[0]--;
                if (d[0] == 0 && Prefs.dark()) {
                    Object r = param.getResult();
                    if (r instanceof View) {
                        Dark.walk((View) r);
                    }
                }
            }
        };
        hook("android.view.LayoutInflater", cl, "inflate", int.class, "android.view.ViewGroup", boolean.class, h);
        hook("android.view.LayoutInflater", cl, "inflate", "org.xmlpull.v1.XmlPullParser", "android.view.ViewGroup",
                boolean.class, h);
    }

    private static void hookText(ClassLoader cl) {
        hook("android.widget.TextView", cl, "setText", CharSequence.class, "android.widget.TextView$BufferType",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!Prefs.dark()) return;
                        try {
                            Object o = param.args[0];
                            if (o instanceof CharSequence) {
                                param.args[0] = Dark.fixSpans((CharSequence) o);
                            }
                        } catch (Throwable t) {
                            XposedBridge.log(t);
                        }
                    }
                });
    }

    /** Dodaje tamni CSS na kraj svakog .css fajla iz assets/Content/Style/. */
    private static void hookCss(ClassLoader cl) {
        hook("android.webkit.WebViewClient", cl, "shouldInterceptRequest", "android.webkit.WebView",
                "android.webkit.WebResourceRequest", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!Prefs.dark()) return;
                        try {
                            String path = ((WebResourceRequest) param.args[1]).getUrl().getPath();
                            if (path == null || !path.endsWith(".css")
                                    || !path.contains("/android_asset/Content/Style/")) {
                                return;
                            }
                            String marker = "/android_asset/";
                            String asset = path.substring(path.indexOf(marker) + marker.length());
                            WebView wv = (WebView) param.args[0];
                            InputStream in = wv.getContext().getAssets().open(asset);
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = in.read(buf)) > 0) {
                                out.write(buf, 0, n);
                            }
                            in.close();
                            out.write(("\n" + Assets.darkCss()).getBytes("UTF-8"));
                            param.setResult(new WebResourceResponse("text/css", "UTF-8",
                                    new ByteArrayInputStream(out.toByteArray())));
                        } catch (Throwable ignored) {
                            // ostavi originalni odgovor
                        }
                    }
                });
    }

    /**
     * setBackgroundResource: okvir danasnjeg dana u kalendaru. Aplikacija danasnjem redu postavlja
     * tablerow_border_neparni_dani (neparan datum, ima i punu pozadinu) ili tablerow_border_parni_dani
     * (paran datum, samo okvir). Oba se zamjenjuju okvirom u boji teme; ostalo se naknadno popravlja.
     */
    private static final class OddRowHook extends XC_MethodHook {
        private int oddId = -1;
        private int evenId = -1;
        private boolean handled;

        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            handled = false;
            if (!Prefs.dark()) return;
            try {
                View v = (View) param.thisObject;
                if (oddId == -1) {
                    oddId = v.getResources().getIdentifier("tablerow_border_neparni_dani", "drawable", PKG);
                    evenId = v.getResources().getIdentifier("tablerow_border_parni_dani", "drawable", PKG);
                }
                int id = ((Integer) param.args[0]).intValue();
                boolean odd = oddId > 0 && id == oddId;
                boolean even = evenId > 0 && id == evenId;
                if (odd || even) {
                    GradientDrawable g = new GradientDrawable();
                    g.setShape(GradientDrawable.RECTANGLE);
                    if (odd) g.setColor(Dark.ROW); // parni: samo okvir, bez pozadine
                    float density = v.getResources().getDisplayMetrics().density;
                    int w = Math.max(1, Math.round(density * 3f));
                    g.setStroke(w, Dark.ROW_BORDER);
                    v.setBackground(g);
                    param.setResult(null);
                    handled = true;
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            if (!Prefs.dark()) return;
            if (handled) { // vec je u bojama teme, ne diraj (inace bi svijetla boja okvira bila invertovana)
                handled = false;
                return;
            }
            try {
                Dark.fixBackground((View) param.thisObject);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    }

    // ------------------------------------------------------------------ resursi

    private static void rc(XResources res, String name, int color) {
        try {
            res.setReplacement(PKG, "color", name, Integer.valueOf(color));
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /** Resursi aplikacije (cuvaju se da bi se boje mogle promijeniti i dok je aplikacija otvorena). */
    private static XResources RES;

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam)
            throws Throwable {
        if (!PKG.equals(resparam.packageName)) return;
        RES = resparam.res;
        reapplyResources();
    }

    /** (Ponovo) postavi zamjene resursa u bojama odabrane teme. Ne radi nista ako je tema iskljucena. */
    static void reapplyResources() {
        XResources res = RES;
        if (res == null || !Prefs.dark()) return;
        Dark.reload();

        rc(res, "primary_material_dark", Dark.BAR);
        rc(res, "primary_dark_material_dark", Dark.BAR_DARK);
        rc(res, "accent_material_dark", Dark.ACCENT);
        rc(res, "colorPrimary", Dark.BAR);
        rc(res, "colorPrimaryDark", Dark.BAR_DARK);
        rc(res, "colorAccent", Dark.ACCENT);
        rc(res, "viewpager_tab_color", Dark.BAR);
        rc(res, "list_background", Dark.SURFACE);
        rc(res, "list_divider", Dark.DIVIDER);
        rc(res, "textcolormain", Dark.TEXT);
        rc(res, "my_color_light", Dark.ACCENT); // pritisnuto stanje dugmadi (mybuttonselector)

        try {
            // pozadina "papir"
            res.setReplacement(PKG, "drawable", "paper", new XResources.DrawableLoader() {
                @Override
                public Drawable newDrawable(XResources r, int id) throws Throwable {
                    return new ColorDrawable(Dark.BG);
                }
            });
            // klizač (scrollbar thumb)
            res.setReplacement(PKG, "drawable", "customscroll", new XResources.DrawableLoader() {
                @Override
                public Drawable newDrawable(XResources r, int id) throws Throwable {
                    GradientDrawable g = new GradientDrawable();
                    g.setShape(GradientDrawable.RECTANGLE);
                    g.setColor(Dark.SCROLL);
                    g.setCornerRadius(4f * r.getDisplayMetrics().density);
                    return g;
                }
            });
            // dugmad / trake
            res.setReplacement(PKG, "drawable", "gradient_bg", new XResources.DrawableLoader() {
                @Override
                public Drawable newDrawable(XResources r, int id) throws Throwable {
                    return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[] { 0xff262626, 0xff1c1c1c });
                }
            });
            res.setReplacement(PKG, "drawable", "gradient_bg_hover", new XResources.DrawableLoader() {
                @Override
                public Drawable newDrawable(XResources r, int id) throws Throwable {
                    return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[] { 0xff2e3f5e, 0xff243350 });
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    // ------------------------------------------------------------------ meni: grupa "Podesavanja"

    /** Naziv nove grupe u bocnom meniju (na kraju liste, bez podstavki). */
    static final String MENU_TITLE = "Подешавања";

    /** Naziv grupe (s60.b) ili null. */
    private static String groupName(Object g) {
        if (g == null) return null;
        if (g instanceof String) return (String) g;
        try {
            Object n = XposedHelpers.getObjectField(g, "b");
            return n != null ? n.toString() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Dodaje grupu "Podesavanja" na kraj liste grupa adaptera bocnog menija (zl0.e). */
    @SuppressWarnings("unchecked")
    private static void addMenuGroup(Object activity) throws Throwable {
        android.widget.ExpandableListView lv =
                (android.widget.ExpandableListView) XposedHelpers.getObjectField(activity, "H0");
        android.widget.ExpandableListAdapter ad = lv.getExpandableListAdapter();
        java.util.List<Object> list = (java.util.List<Object>) XposedHelpers.getObjectField(ad, "e");
        for (Object g : list) {
            if (MENU_TITLE.equals(groupName(g))) return;
        }
        java.lang.reflect.Constructor<?> ctor = list.get(0).getClass().getDeclaredConstructor();
        ctor.setAccessible(true);
        Object g = ctor.newInstance();
        java.lang.reflect.Field f = g.getClass().getDeclaredField("b");
        f.setAccessible(true);
        f.set(g, MENU_TITLE);
        list.add(g);
        hookGroupIcon(ad.getClass());
        if (ad instanceof android.widget.BaseExpandableListAdapter) {
            ((android.widget.BaseExpandableListAdapter) ad).notifyDataSetChanged();
        }
    }

    private static boolean groupIconHooked;

    /** Aplikacija bira ikonu grupe po poziciji; za nasu grupu postavi zupcanik umjesto zaostale ikone. */
    private static synchronized void hookGroupIcon(Class<?> adapterClass) {
        if (groupIconHooked) return;
        groupIconHooked = true;
        XposedBridge.hookAllMethods(adapterClass, "getGroupView", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    int pos = ((Integer) param.args[0]).intValue();
                    android.widget.BaseExpandableListAdapter ad =
                            (android.widget.BaseExpandableListAdapter) param.thisObject;
                    if (!MENU_TITLE.equals(groupName(ad.getGroup(pos)))) return;
                    View v = (View) param.getResult();
                    int id = v.getResources().getIdentifier("tag_img_group", "id", PKG);
                    View iv = id != 0 ? v.findViewById(id) : null;
                    if (iv instanceof android.widget.ImageView) {
                        int px = Math.round(32f * v.getResources().getDisplayMetrics().density);
                        ((android.widget.ImageView) iv).setImageDrawable(new GearDrawable(px));
                    }
                } catch (Throwable t) {
                    XposedBridge.log(t);
                }
            }
        });
    }

    private static void hookSettingsMenu(final ClassLoader cl) {
        // Dodaj grupu poslije gradnje menija.
        hook("com.pravoslavac.MainActivity", cl, "onCreate", "android.os.Bundle", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    addMenuGroup(param.thisObject);
                } catch (Throwable t) {
                    XposedBridge.log(t);
                }
            }
        });

        // Klik na grupu: otvori Podesavanja umjesto originalne obrade.
        hook("android.widget.ExpandableListView", cl, "setOnGroupClickListener",
                android.widget.ExpandableListView.OnGroupClickListener.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        final android.widget.ExpandableListView.OnGroupClickListener orig =
                                (android.widget.ExpandableListView.OnGroupClickListener) param.args[0];
                        if (orig == null) return;
                        param.args[0] = new android.widget.ExpandableListView.OnGroupClickListener() {
                            @Override
                            public boolean onGroupClick(android.widget.ExpandableListView parent, View v,
                                                        int groupPosition, long id) {
                                try {
                                    Object g = parent.getExpandableListAdapter().getGroup(groupPosition);
                                    if (MENU_TITLE.equals(groupName(g)) && parent.getContext() instanceof Activity) {
                                        Activity a = (Activity) parent.getContext();
                                        closeDrawer(a);
                                        SettingsUi.show(a);
                                        return true;
                                    }
                                } catch (Throwable t) {
                                    XposedBridge.log(t);
                                }
                                return orig.onGroupClick(parent, v, groupPosition, id);
                            }
                        };
                    }
                });
    }

    /** Zatvara bocni meni (DrawerLayout) ako ga ima u hijerarhiji aktivnosti. */
    private static void closeDrawer(Activity a) {
        try {
            View root = a.getWindow().getDecorView();
            View d = findByClass(root, "DrawerLayout");
            if (d == null) return;
            Class<?> c = d.getClass();
            try {
                c.getMethod("closeDrawers").invoke(d);
                return;
            } catch (NoSuchMethodException e) {
                // aplikacija je obfuskirana: closeDrawers() = c(), closeDrawers(boolean) = d(boolean)
            }
            try {
                c.getMethod("c").invoke(d);
                return;
            } catch (NoSuchMethodException e) {
                // probaj sljedece
            }
            c.getMethod("d", boolean.class).invoke(d, Boolean.FALSE);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static View findByClass(View v, String simpleName) {
        for (Class<?> c = v.getClass(); c != null; c = c.getSuperclass()) {
            if (c.getSimpleName().equals(simpleName)) return v;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = findByClass(g.getChildAt(i), simpleName);
                if (r != null) return r;
            }
        }
        return null;
    }
}