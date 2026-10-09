package app.morphe.extension.chmate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Adapts ChMate's obfuscated Flow without depending on a particular coroutine ABI. */
public final class ReadCountEvents {
    private ReadCountEvents() {}

    /**
     * Unit is an event, not state. Give each emission an identity so stateIn does
     * not discard subsequent read-count changes as equal to its initial Unit.
     * Only the unread-count combine (whose third argument is unused) receives
     * this adapter; other subscribers keep the original Unit events.
     */
    public static Object distinct(Object upstream, String flowClassName) {
        try {
            ClassLoader loader = upstream.getClass().getClassLoader();
            Class<?> flowType = Class.forName(flowClassName, false, loader);
            return Proxy.newProxyInstance(loader, new Class<?>[]{flowType}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) return objectMethod(proxy, method, args);
                if (!method.getName().equals("collect") || args == null || args.length != 2) {
                    return invoke(method, upstream, args);
                }
                Object downstream = args[0];
                Class<?> collectorType = method.getParameterTypes()[0];
                Object collector = Proxy.newProxyInstance(loader, new Class<?>[]{collectorType},
                        (receiver, emit, values) -> {
                            if (emit.getDeclaringClass() == Object.class) {
                                return objectMethod(receiver, emit, values);
                            }
                            if (emit.getName().equals("emit") && values != null && values.length == 2) {
                                // Forward the native continuation and suspended marker unchanged.
                                return invoke(emit, downstream, new Object[]{new Object(), values[1]});
                            }
                            return invoke(emit, downstream, values);
                        });
                return invoke(method, upstream, new Object[]{collector, args[1]});
            });
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to adapt read-count events", error);
        }
    }

    private static Object invoke(Method method, Object target, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException error) {
            throw error.getCause();
        }
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "equals": return proxy == args[0];
            case "hashCode": return System.identityHashCode(proxy);
            case "toString": return "HaiagaruReadCountEvents";
            default: throw new UnsupportedOperationException(method.getName());
        }
    }
}
