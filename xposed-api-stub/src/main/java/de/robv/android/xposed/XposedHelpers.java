package de.robv.android.xposed;

/** Stub (samo za kompajliranje). */
public final class XposedHelpers {
    private XposedHelpers() {}

    public static Class<?> findClass(String className, ClassLoader classLoader) { return null; }

    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader,
            String methodName, Object... parameterTypesAndCallback) {
        return null;
    }

    public static Object callMethod(Object obj, String methodName, Object... args) { return null; }

    public static Object getObjectField(Object obj, String fieldName) { return null; }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) { return null; }
}
