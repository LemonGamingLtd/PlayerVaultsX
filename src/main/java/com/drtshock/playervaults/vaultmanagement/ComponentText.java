package com.drtshock.playervaults.vaultmanagement;

import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ComponentText {

    @Nullable private static final Class<?> COMPONENT_CLASS;
    @Nullable private static final Object PLAIN_SERIALIZER;
    @Nullable private static final Method PLAIN_SERIALIZE;

    static {
        Class<?> componentClass = null;
        Object plainSerializer = null;
        Method plainSerialize = null;
        try {
            componentClass = Class.forName("net.kyori.adventure.text.Component");
            Class<?> serializerClass = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer");
            plainSerializer = serializerClass.getMethod("plainText").invoke(null);
            plainSerialize = serializerClass.getMethod("serialize", componentClass);
        } catch (Throwable ignored) {
            componentClass = null;
            plainSerializer = null;
            plainSerialize = null;
        }
        COMPONENT_CLASS = componentClass;
        PLAIN_SERIALIZER = plainSerializer;
        PLAIN_SERIALIZE = plainSerialize;
    }

    private ComponentText() {}

    static boolean isAvailable() {
        return PLAIN_SERIALIZE != null;
    }

    @Nullable
    static String invokeComponentGetter(ItemMeta meta, String methodName) {
        if (!isAvailable()) return null;
        try {
            Method method = meta.getClass().getMethod(methodName);
            Object component = method.invoke(meta);
            return serialize(component);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    @Nullable
    static List<String> invokeLoreGetter(ItemMeta meta) {
        if (!isAvailable()) return null;
        try {
            Method method = meta.getClass().getMethod("lore");
            Object value = method.invoke(meta);
            if (!(value instanceof List<?> list)) return null;
            List<String> out = new ArrayList<>(list.size());
            for (Object element : list) {
                String plain = serialize(element);
                if (plain != null) {
                    out.add(plain);
                }
            }
            return out.isEmpty() ? Collections.emptyList() : out;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }

    @Nullable
    private static String serialize(@Nullable Object component) {
        if (component == null || PLAIN_SERIALIZE == null || COMPONENT_CLASS == null) {
            return null;
        }
        if (!COMPONENT_CLASS.isInstance(component)) {
            return null;
        }
        try {
            return (String) PLAIN_SERIALIZE.invoke(PLAIN_SERIALIZER, component);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
    }
}