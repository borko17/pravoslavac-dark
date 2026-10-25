package android.content.res;

import android.graphics.drawable.Drawable;

/** Stub (samo za kompajliranje). */
public class XResources extends Resources {
    public XResources() {
        super(null, null, null);
    }

    public void setReplacement(String fullName, Object replacement) {}

    public void setReplacement(String pkg, String type, String name, Object replacement) {}

    public void setReplacement(int id, Object replacement) {}

    public abstract static class DrawableLoader {
        public DrawableLoader() {}

        public abstract Drawable newDrawable(XResources res, int id) throws Throwable;
    }
}
