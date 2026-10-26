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
    static final int DARK_ROW = 0xff1f1f1d;
    /** Tamnocrvena boja naslova koju aplikacija postavlja kodom i njena svijetla zamjena. */
    static final int DARK_RED = 0xffcc0000;
    static final int LIGHT_RED = 0xffff7a66;

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
                            if (Prefs.dark()) wv.evaluateJavascript(Assets.DARK_JS, null);
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
                        if (Prefs.dark()) wv.setBackgroundColor(Dark.BG);
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
                        param.args[0] = Integer.valueOf(DARK_ROW);
                    } else if (c == DARK_RED) {
                        param.args[0] = Integer.valueOf(LIGHT_RED);
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
                            out.write(("\n" + Assets.DARK_CSS).getBytes("UTF-8"));
                            param.setResult(new WebResourceResponse("text/css", "UTF-8",
                                    new ByteArrayInputStream(out.toByteArray())));
                        } catch (Throwable ignored) {
                            // ostavi originalni odgovor
                        }
                    }
                });
    }

    /** setBackgroundResource: red "neparni dani" u kalendaru dobija tamnu pozadinu; ostalo se naknadno popravlja. */
    private static final class OddRowHook extends XC_MethodHook {
        private int oddId = -1;

        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            if (!Prefs.dark()) return;
            try {
                View v = (View) param.thisObject;
                if (oddId == -1) {
                    oddId = v.getResources().getIdentifier("tablerow_border_neparni_dani", "drawable", PKG);
                }
                int id = ((Integer) param.args[0]).intValue();
                if (oddId > 0 && id == oddId) {
                    GradientDrawable g = new GradientDrawable();
                    g.setShape(GradientDrawable.RECTANGLE);
                    g.setColor(DARK_ROW);
                    float density = v.getResources().getDisplayMetrics().density;
                    int w = Math.max(1, Math.round(density * 3f));
                    g.setStroke(w, 0xff2f5f99);
                    v.setBackground(g);
                    param.setResult(null);
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            if (!Prefs.dark()) return;
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

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam)
            throws Throwable {
        if (!PKG.equals(resparam.packageName)) return;
        if (!Prefs.dark()) return;
        XResources res = resparam.res;

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

        try {
            // pozadina "papir"
            res.setReplacement(PKG, "drawable", "paper", new XResources.DrawableLoader() {
                @Override
                public Drawable newDrawable(XResources r, int id) throws Throwable {
                    return new ColorDrawable(Dark.BG);
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
}
