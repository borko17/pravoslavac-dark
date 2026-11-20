package patch.kalendar;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Ekran modula Pravoslavac Dark: služi za preuzimanje и управљање Google fontovima sa ćirilicom
 * za aplikacije Православац и Житије светих.
 */
public class MainActivity extends Activity {

    private FontPanel fontPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Pravoslavac Dark");

        int pad = Math.round(16 * getResources().getDisplayMetrics().density);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad, pad, pad);
        content.setBackgroundColor(0xff121212);

        TextView title = new TextView(this);
        title.setText("Pravoslavac Dark");
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xffe0e0e0);
        content.addView(title);

        TextView subTitle = new TextView(this);
        subTitle.setText("Преузимање фонтова");
        subTitle.setTextSize(16f);
        subTitle.setTextColor(0xff8ab4f8);
        subTitle.setPadding(0, 0, 0, Math.round(8 * getResources().getDisplayMetrics().density));
        content.addView(subTitle);

        FontPanel.Pal pal = new FontPanel.Pal(
                0xff121212,
                0xffe0e0e0,
                0xff9aa0a6,
                0xff262626,
                0xffe0e0e0,
                0xff3a3a3a,
                true
        );

        fontPanel = new FontPanel(this, pal, null, null);
        View panelView = fontPanel.view();
        content.addView(panelView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xff121212);
        scroll.setFillViewport(true);
        scroll.addView(content);

        scroll.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (fontPanel != null) fontPanel.onScroll();
            }
        });

        setContentView(scroll);
        fontPanel.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fontPanel != null) {
            fontPanel.dispose();
            fontPanel = null;
        }
    }
}
