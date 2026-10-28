package de.robv.android.xposed;

import android.content.SharedPreferences;

import java.io.File;
import java.util.Map;
import java.util.Set;

/** Stub (samo za kompajliranje). U runtime-u klasu daje LSPosed. */
public class XSharedPreferences implements SharedPreferences {
    public XSharedPreferences(String packageName) {}

    public XSharedPreferences(String packageName, String prefFileName) {}

    public XSharedPreferences(File prefFile) {}

    public boolean makeWorldReadable() { return false; }

    public boolean hasFileChanged() { return false; }

    public void reload() {}

    public File getFile() { return null; }

    @Override
    public Map<String, ?> getAll() { return null; }

    @Override
    public String getString(String key, String defValue) { return defValue; }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) { return defValues; }

    @Override
    public int getInt(String key, int defValue) { return defValue; }

    @Override
    public long getLong(String key, long defValue) { return defValue; }

    @Override
    public float getFloat(String key, float defValue) { return defValue; }

    @Override
    public boolean getBoolean(String key, boolean defValue) { return defValue; }

    @Override
    public boolean contains(String key) { return false; }

    @Override
    public Editor edit() { return null; }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
}
