package patch.kalendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * Birac boje: tri koncentricna prstena, svaki pun krug (spolja prema unutra: nijansa, zasicenost, svjetlina), i krug u centru
 * koji prikazuje boju i u koji se moze ukucati kod (#rrggbb). Ispod je traka: lijevo pocetna, desno nova boja.
 */
final class ColorPicker {
    private ColorPicker() {}

    interface Listener {
        void onColor(int color);
    }

    /** Pozadina dijaloga. */
    private static final int DIALOG_BG = 0xff303030;

    /** Sredina i debljina prstena (u djelovima poluprecnika): nijansa, zasicenost, svjetlina. */
    private static final float[] RING_R = { 0.885f, 0.67f, 0.46f };
    private static final float[] RING_W = { 0.17f, 0.17f, 0.15f };

    static void show(final Context ctx, int initial, final Listener listener) {
        final float[] hsv = new float[3];
        Color.colorToHSV(initial, hsv);
        final int startColor = 0xff000000 | initial;

        int widthPx = ctx.getResources().getDisplayMetrics().widthPixels;
        final int size = Math.min(dp(ctx, 330), widthPx - dp(ctx, 72));
        final float radius = size / 2f - dp(ctx, 2);

        final boolean[] self = { false }; // true dok birac sam upisuje kod u polje
        final EditText hex = new EditText(ctx);
        final GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setStroke(dp(ctx, 2), 0x66ffffff);
        final View newSwatch = new View(ctx);

        hex.setSingleLine(true);
        hex.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        hex.setFilters(new InputFilter[] { new InputFilter.LengthFilter(7) });
        hex.setTypeface(Typeface.MONOSPACE);
        hex.setTextSize(15f);
        hex.setGravity(Gravity.CENTER);
        hex.setPadding(0, 0, 0, 0);
        hex.setBackground(disc);

        /** Boja kruga u centru, boja slova i nova boja na traci (bez diranja teksta u polju). */
        final Runnable refresh = new Runnable() {
            @Override
            public void run() {
                int c = Color.HSVToColor(hsv);
                disc.setColor(c);
                double lum = (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255.0;
                hex.setTextColor(lum > 0.55 ? 0xff000000 : 0xffffffff);
                newSwatch.setBackgroundColor(c);
            }
        };

        /** Kad se vuce prsten: osvjezi i upisi kod u polje. */
        final Runnable update = new Runnable() {
            @Override
            public void run() {
                refresh.run();
                String t = "#" + Colors.hex(Color.HSVToColor(hsv));
                if (!t.equals(hex.getText().toString())) {
                    self[0] = true;
                    hex.setText(t);
                    self[0] = false;
                }
            }
        };

        final Rings rings = new Rings(ctx, hsv, update);

        FrameLayout wheel = new FrameLayout(ctx);
        wheel.addView(rings, new FrameLayout.LayoutParams(size, size));
        int disk = (int) (0.68f * radius);
        wheel.addView(hex, new FrameLayout.LayoutParams(disk, disk, Gravity.CENTER));

        // traka: lijevo pocetna, desno nova boja
        LinearLayout compare = new LinearLayout(ctx);
        compare.setOrientation(LinearLayout.HORIZONTAL);
        View oldSwatch = new View(ctx);
        oldSwatch.setBackgroundColor(startColor);
        compare.addView(oldSwatch, new LinearLayout.LayoutParams(0, dp(ctx, 26), 1f));
        compare.addView(newSwatch, new LinearLayout.LayoutParams(0, dp(ctx, 26), 1f));

        int pad = dp(ctx, 16);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(pad, pad, pad, 0);
        root.addView(wheel, new LinearLayout.LayoutParams(size, size));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(size, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(ctx, 12);
        root.addView(compare, clp);

        ScrollView scroll = new ScrollView(ctx);
        scroll.addView(root);

        update.run();

        // unos koda boje (#rrggbb): vazeci kod odmah pomjera prstenove
        hex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence x, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence x, int a, int b, int c) {}

            @Override
            public void afterTextChanged(Editable e) {
                if (self[0]) return;
                Integer c = Colors.parse(e.toString());
                if (c == null) return;
                Color.colorToHSV(c.intValue(), hsv);
                refresh.run();
                rings.invalidate();
            }
        });

        AlertDialog dlg = new AlertDialog.Builder(ctx, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setView(scroll)
                .setPositiveButton("U redu", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Integer typed = Colors.parse(hex.getText().toString());
                        listener.onColor(typed != null ? (0xff000000 | typed.intValue()) : Color.HSVToColor(hsv));
                    }
                })
                .setNegativeButton("Odustani", null)
                .create();
        // tastatura se ne otvara sama; kad se otvori, dijalog se smanji umjesto da prekrije birac
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(DIALOG_BG);
        bg.setCornerRadius(dp(ctx, 6));
        dlg.getWindow().setBackgroundDrawable(bg);
        dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dlg.show();
    }

    private static int dp(Context ctx, int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

    /** Tri prstena: 0 = nijansa, 1 = zasicenost, 2 = svjetlina (nelinearno, vise prostora za tamne vrijednosti). Pocetak svakog je na vrhu. */
    private static final class Rings extends View {
        private final float[] hsv;
        private final Runnable changed;
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();
        private final Matrix rot = new Matrix();
        private int active = -1; // prsten koji se trenutno vuce (zakljucan od dodira do podizanja prsta)

        Rings(Context ctx, float[] hsv, Runnable changed) {
            super(ctx);
            this.hsv = hsv;
            this.changed = changed;
            ring.setStyle(Paint.Style.STROKE);
            mark.setStyle(Paint.Style.STROKE);
        }

        private float outerRadius() {
            return Math.min(getWidth(), getHeight()) / 2f - getResources().getDisplayMetrics().density * 2f;
        }

        /** Gradijent oko centra; ugao 0 je pri vrhu, ide u smjeru kazaljke. */
        private SweepGradient sweep(float cx, float cy, int[] colors, float[] pos) {
            SweepGradient g = new SweepGradient(cx, cy, colors, pos);
            rot.setRotate(-90f, cx, cy);
            g.setLocalMatrix(rot);
            return g;
        }

        private void setOval(float cx, float cy, float r) {
            oval.set(cx - r, cy - r, cx + r, cy + r);
        }

        @Override
        protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float R = outerRadius();
            float d = getResources().getDisplayMetrics().density;

            // 0: nijansa (cijeli krug)
            int[] hc = new int[7];
            for (int i = 0; i < 7; i++) hc[i] = Color.HSVToColor(new float[] { i * 60f, 1f, 1f });
            ring.setShader(sweep(cx, cy, hc, null));
            ring.setStrokeCap(Paint.Cap.BUTT);
            ring.setStrokeWidth(RING_W[0] * R);
            setOval(cx, cy, RING_R[0] * R);
            c.drawOval(oval, ring);

            // 1: zasicenost (prati trenutnu svjetlinu, pa tamni zajedno sa prstenom svjetline)
            int[] sc = new int[7];
            for (int i = 0; i < 7; i++) sc[i] = Color.HSVToColor(new float[] { hsv[0], i / 6f, hsv[2] });
            ring.setShader(sweep(cx, cy, sc, null));
            ring.setStrokeWidth(RING_W[1] * R);
            setOval(cx, cy, RING_R[1] * R);
            c.drawOval(oval, ring);

            // 2: svjetlina (v = t*t, pa tamni dio zauzima veci dio luka)
            int[] vc = new int[9];
            for (int i = 0; i < 9; i++) {
                float t = i / 8f;
                vc[i] = Color.HSVToColor(new float[] { hsv[0], hsv[1], t * t });
            }
            ring.setShader(sweep(cx, cy, vc, null));
            ring.setStrokeWidth(RING_W[2] * R);
            setOval(cx, cy, RING_R[2] * R);
            c.drawOval(oval, ring);
            ring.setShader(null);

            // oznake
            for (int i = 0; i < 3; i++) {
                double a = Math.toRadians(markAngle(i));
                float mx = cx + (float) Math.sin(a) * RING_R[i] * R;
                float my = cy - (float) Math.cos(a) * RING_R[i] * R;
                float mr = RING_W[i] * R / 2f + d;
                mark.setStrokeWidth(d * 4f);
                mark.setColor(Color.BLACK);
                c.drawCircle(mx, my, mr, mark);
                mark.setStrokeWidth(d * 2f);
                mark.setColor(Color.WHITE);
                c.drawCircle(mx, my, mr, mark);
            }
        }

        private float markAngle(int i) {
            if (i == 0) return hsv[0];
            if (i == 1) return 360f * hsv[1];
            return 360f * (float) Math.sqrt(hsv[2]);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float R = outerRadius();
            float dx = e.getX() - cx;
            float dy = e.getY() - cy;
            float r = (float) Math.hypot(dx, dy) / R;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (r < 0.36f || r > 1.08f) return false;
                    active = r > 0.78f ? 0 : (r > 0.56f ? 1 : 2);
                    getParent().requestDisallowInterceptTouchEvent(true);
                    apply(dx, dy);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (active < 0) return false;
                    apply(dx, dy);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    active = -1;
                    return true;
                default:
                    return true;
            }
        }

        private void apply(float dx, float dy) {
            // ugao od vrha u smjeru kazaljke, 0..360
            float a = (float) Math.toDegrees(Math.atan2(dx, -dy));
            if (a < 0f) a += 360f;
            // svaki prsten je pun krug: vrh je pocetak (0) i kraj (100%), pa se moze kruziti unaokolo
            if (active == 0) hsv[0] = Math.min(359.99f, a);
            else if (active == 1) hsv[1] = a / 360f;
            else {
                float t = a / 360f;
                hsv[2] = t * t;
            }
            invalidate();
            changed.run();
        }
    }
}
