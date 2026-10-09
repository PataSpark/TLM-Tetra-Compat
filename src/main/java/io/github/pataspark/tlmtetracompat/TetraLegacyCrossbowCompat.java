package io.github.pataspark.tlmtetracompat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import se.mickelus.tetra.items.modular.ModularItem;

public final class TetraLegacyCrossbowCompat {

    private static final ResourceLocation CROSSBOW_ID =
            new ResourceLocation("tetra", "modular_crossbow");

    private TetraLegacyCrossbowCompat() {
    }

    public static boolean isTetraCrossbow(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (!(stack.getItem() instanceof ModularItem)) {
            return false;
        }

        return CROSSBOW_ID.equals(
                ForgeRegistries.ITEMS.getKey(stack.getItem())
        );
    }

    public static boolean isLoaded(ItemStack stack) {
        if (!isTetraCrossbow(stack)) {
            return false;
        }

        try {
            java.lang.reflect.Method method =
                    stack.getItem().getClass().getMethod(
                            "isLoaded", ItemStack.class
                    );

            return (Boolean) method.invoke(stack.getItem(), stack);

        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to check Tetra crossbow loaded state", e
            );
        }
    }

    public static void setLoaded(ItemStack stack, boolean loaded) {
        if (!isTetraCrossbow(stack)) {
            return;
        }

        try {
            java.lang.reflect.Method method =
                    stack.getItem().getClass().getMethod(
                            "setLoaded",
                            ItemStack.class,
                            boolean.class
                    );

            method.invoke(stack.getItem(), stack, loaded);

        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to set Tetra crossbow loaded state", e
            );
        }
    }

    public static int getReloadDuration(
            ItemStack stack,
            net.minecraft.world.entity.LivingEntity entity) {

        if (!isTetraCrossbow(stack)) {
            return 0;
        }

        Object crossbow = stack.getItem();

        try {
            java.lang.reflect.Method method;

            try {
                // 較新版本的 Tetra API
                method = crossbow.getClass().getMethod(
                        "getReloadDuration",
                        ItemStack.class,
                        net.minecraft.world.entity.LivingEntity.class
                );

                return (Integer) method.invoke(crossbow, stack, entity);

            } catch (NoSuchMethodException ignored) {
                // Tetra 6.9.0、6.13.0 使用的舊版 API
                method = crossbow.getClass().getMethod(
                        "getReloadDuration",
                        ItemStack.class
                );

                return (Integer) method.invoke(crossbow, stack);
            }

        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to get Tetra crossbow reload duration", e
            );
        }
    }

    public static float getProjectileVelocity(
            ItemStack stack,
            double strength,
            float velocityBonus) {

        if (!isTetraCrossbow(stack)) {
            return 0.0F;
        }

        try {
            java.lang.reflect.Method method =
                    stack.getItem().getClass().getMethod(
                            "getProjectileVelocity",
                            double.class,
                            float.class
                    );

            return (Float) method.invoke(
                    null,
                    strength,
                    velocityBonus
            );

        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to calculate Tetra crossbow projectile velocity", e
            );
        }
    }
}