package de.robv.android.xposed;

/** Stub (samo za kompajliranje). U runtime-u klasu daje LSPosed. */
public final class XSharedPreferences {
    public XSharedPreferences(String packageName, String prefFileName) {}

    public boolean makeWorldReadable() { return false; }

    public boolean hasFileChanged() { return false; }

    public void reload() {}

    public boolean getBoolean(String key, boolean defValue) { return defValue; }
}
