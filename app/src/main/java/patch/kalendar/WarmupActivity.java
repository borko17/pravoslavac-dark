package patch.kalendar;

import android.app.Activity;
import android.os.Bundle;

/**
 * Nevidljiva aktivnost: Pravoslavac je pokrece pa se odmah zatvara. Android tek poslije pokretanja aktivnosti iz modula
 * otkriva Pravoslavcu paket modula (vidljivost paketa, Android 11+), pa onda radi poziv FontProvider-a.
 */
public final class WarmupActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        finish();
        overridePendingTransition(0, 0);
    }
}
