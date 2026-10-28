package patch.kalendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Jednostavna paleta za biranje boje (HSV): kvadrat zasićenost/svjetlina + traka nijanse. */
final class ColorPicker {
    private ColorPicker() {}

    interface Listener {
        void onColor(int color);
    }

    static void show(final Context ctx, int initial, final Listener listener) {
        final float[] hsv = new float[3];
        Color.colorToHSV(initial, hsv);

        final View preview = new View(ctx);
        final TextView hex = new TextView(ctx);
        hex.setTextSize(18f);
        hex.setTypeface(Typeface.MONOSPACE);

        final Runnable update = new Runnable() {
            @Override
            public void run() {
                int c = Color.HSVToColor(hsv);
                preview.setBackgroundColor(c);
                hex.setText("#" + Colors.hex(c));
            }
        };

        final SvView sv = new SvView(ctx, hsv, update);
        final HueBar hue = new HueBar(ctx, hsv, update, sv);

        int pad = dp(ctx, 16);
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, 0);

        root.addView(sv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 220)));

        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 36));
        hlp.topMargin = dp(ctx, 12);
        root.addView(hue, hlp);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(ctx, 12);
        row.addView(preview, new LinearLayout.LayoutParams(dp(ctx, 48), dp(ctx, 48)));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.leftMargin = dp(ctx, 12);
        row.addView(hex, tlp);
        root.addView(row, rlp);

        update.run();

        new AlertDialog.Builder(ctx)
                .setView(root)
                .setPositiveButton("U redu", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        listener.onColor(Color.HSVToColor(hsv));
                    }
                })
                .setNegativeButton("Odustani", null)
                .show();
    }

    private static int dp(Context ctx, int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

    /** Kvadrat: x = zasićenost, y = svjetlina (vrijednost). */
    private static final class SvView extends View {
        private final float[] hsv;
        private final Runnable changed;
        private final Paint paint = new Paint();
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);

        SvView(Context ctx, float[] hsv, Runnable changed) {
            super(ctx);
            this.hsv = hsv;
            this.changed = changed;
            ring.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth();
            int h = getHeight();
            int pure = Color.HSVToColor(new float[] { hsv[0], 1f, 1f });
            paint.setShader(new LinearGradient(0, 0, w, 0, Color.WHITE, pure, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, paint);
            paint.setShader(new LinearGradient(0, 0, 0, h, 0x00000000, Color.BLACK, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, paint);

            float x = hsv[1] * w;
            float y = (1f - hsv[2]) * h;
            float r = getResources().getDisplayMetrics().density * 9f;
            ring.setStrokeWidth(getResources().getDisplayMetrics().density * 3f);
            ring.setColor(Color.BLACK);
            c.drawCircle(x, y, r, ring);
            ring.setStrokeWidth(getResources().getDisplayMetrics().density * 1.5f);
            ring.setColor(Color.WHITE);
            c.drawCircle(x, y, r, ring);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            getParent().requestDisallowInterceptTouchEvent(true);
            float x = Math.max(0f, Math.min(getWidth(), e.getX()));
            float y = Math.max(0f, Math.min(getHeight(), e.getY()));
            hsv[1] = x / getWidth();
            hsv[2] = 1f - y / getHeight();
            invalidate();
            changed.run();
            return true;
        }
    }

    /** Traka nijanse (hue 0..360). */
    private static final class HueBar extends View {
        private final float[] hsv;
        private final Runnable changed;
        private final View sv;
        private final Paint paint = new Paint();
        private final Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);

        HueBar(Context ctx, float[] hsv, Runnable changed, View sv) {
            super(ctx);
            this.hsv = hsv;
            this.changed = changed;
            this.sv = sv;
            mark.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth();
            int h = getHeight();
            int[] cols = new int[7];
            for (int i = 0; i < 7; i++) {
                cols[i] = Color.HSVToColor(new float[] { i * 60f, 1f, 1f });
            }
            paint.setShader(new LinearGradient(0, 0, w, 0, cols, null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, paint);

            float x = hsv[0] / 360f * w;
            float d = getResources().getDisplayMetrics().density;
            mark.setStrokeWidth(d * 4f);
            mark.setColor(Color.BLACK);
            c.drawLine(x, 0, x, h, mark);
            mark.setStrokeWidth(d * 2f);
            mark.setColor(Color.WHITE);
            c.drawLine(x, 0, x, h, mark);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            getParent().requestDisallowInterceptTouchEvent(true);
            float x = Math.max(0f, Math.min(getWidth(), e.getX()));
            hsv[0] = Math.min(359.99f, x / getWidth() * 360f);
            invalidate();
            sv.invalidate();
            changed.run();
            return true;
        }
    }
}
