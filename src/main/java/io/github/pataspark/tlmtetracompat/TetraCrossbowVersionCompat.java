package io.github.pataspark.tlmtetracompat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.items.modular.impl.crossbow.ModularCrossbowItemImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class TetraCrossbowVersionCompat {

    private static final Method RELOAD_DURATION_METHOD =
            findReloadDurationMethod();

    private TetraCrossbowVersionCompat() {
    }

    private static Method findReloadDurationMethod() {

        // Tetra 6.14.2 使用的方法
        try {
            return ModularCrossbowItemImpl.class.getMethod(
                    "getReloadDuration",
                    ItemStack.class,
                    LivingEntity.class
            );
        } catch (NoSuchMethodException ignored) {
            // 新版方法不存在，嘗試舊版方法
        }

        // Tetra 6.13.0 使用的方法
        try {
            return ModularCrossbowItemImpl.class.getMethod(
                    "getReloadDuration",
                    ItemStack.class
            );
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Unsupported Tetra crossbow reload API",
                    e
            );
        }
    }

    public static int getReloadDuration(
            ModularCrossbowItemImpl crossbow,
            ItemStack stack,
            LivingEntity entity) {

        try {
            Object result;

            if (RELOAD_DURATION_METHOD.getParameterCount() == 2) {
                result = RELOAD_DURATION_METHOD.invoke(
                        crossbow,
                        stack,
                        entity
                );
            } else {
                result = RELOAD_DURATION_METHOD.invoke(
                        crossbow,
                        stack
                );
            }

            return (Integer) result;

        } catch (IllegalAccessException
                 | InvocationTargetException e) {

            throw new IllegalStateException(
                    "Failed to get Tetra crossbow reload duration",
                    e
            );
        }
    }
}