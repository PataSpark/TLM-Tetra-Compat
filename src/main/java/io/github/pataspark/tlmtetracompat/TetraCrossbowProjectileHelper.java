package io.github.pataspark.tlmtetracompat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.items.IItemHandler;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import se.mickelus.tetra.event.ModularProjectileSpawnEvent;
import se.mickelus.tetra.items.modular.impl.crossbow.ModularCrossbowItemImpl;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.properties.AttributeHelper;
import se.mickelus.tetra.properties.TetraAttributes;

public final class TetraCrossbowProjectileHelper {

    private TetraCrossbowProjectileHelper() {
    }

    public static boolean hasLoadedProjectile(ItemStack crossbowStack) {

        if (!(crossbowStack.getItem()
                instanceof ModularCrossbowItemImpl crossbow)) {
            return false;
        }

        CompoundTag tag = crossbowStack.getTag();

        if (tag == null) {
            return false;
        }

        ListTag projectiles = tag.getList(
                "ChargedProjectiles",
                10
        );

        return crossbow.isLoaded(crossbowStack)
                && !projectiles.isEmpty();
    }

    public static boolean loadArrow(
            EntityMaid maid,
            ItemStack crossbowStack) {

        if (!(crossbowStack.getItem()
                instanceof ModularCrossbowItemImpl crossbow)) {
            return false;
        }

        // 已經裝填時，不重複消耗箭矢
        if (crossbow.isLoaded(crossbowStack)) {
            return hasLoadedProjectile(crossbowStack);
        }

        IItemHandler inventory = maid.getAvailableInv(true);

        for (int slot = 0; slot < inventory.getSlots(); slot++) {

            ItemStack stack = inventory.getStackInSlot(slot);

            if (!(stack.getItem() instanceof ArrowItem)) {
                continue;
            }

            // 使用物品處理器取出一支箭
            ItemStack arrow = inventory.extractItem(slot, 1, false);

            if (arrow.isEmpty()) {
                continue;
            }

            CompoundTag crossbowTag =
                    crossbowStack.getOrCreateTag();

            ListTag projectiles = crossbowTag.getList(
                    "ChargedProjectiles",
                    10
            );

            // 建立新的裝填資料，避免殘留舊彈藥
            ListTag loadedProjectiles = new ListTag();

            CompoundTag arrowTag = new CompoundTag();
            arrow.save(arrowTag);

            loadedProjectiles.add(arrowTag);

            crossbowTag.put(
                    "ChargedProjectiles",
                    loadedProjectiles
            );

            crossbow.setLoaded(crossbowStack, true);

            return true;
        }

        return false;
    }

    public static boolean fireLoadedArrow(
            EntityMaid maid,
            LivingEntity target,
            ItemStack crossbowStack) {

        if (maid.level().isClientSide) {
            return false;
        }

        if (!hasLoadedProjectile(crossbowStack)) {
            return false;
        }

        if (!(crossbowStack.getItem()
                instanceof ModularCrossbowItemImpl crossbow)) {
            return false;
        }

        CompoundTag tag = crossbowStack.getTag();

        if (tag == null) {
            return false;
        }

        ListTag projectiles = tag.getList(
                "ChargedProjectiles",
                10
        );

        if (projectiles.isEmpty()) {
            return false;
        }

        ItemStack ammoStack = ItemStack.of(
                projectiles.getCompound(0)
        );

        if (!(ammoStack.getItem() instanceof ArrowItem arrowItem)) {
            return false;
        }

        // ===== Tetra 拉力計算 =====

        Attribute drawStrengthAttribute =
                TetraAttributes.drawStrength.get();

        AttributeInstance drawStrengthInstance =
                maid.getAttribute(drawStrengthAttribute);

        double strength;

        if (drawStrengthInstance != null) {
            strength = AttributeHelper.calculateValue(
                    drawStrengthAttribute,
                    drawStrengthInstance.getModifiers(),
                    crossbow.getAttributeModifiersCached(crossbowStack)
                            .get(drawStrengthAttribute)
            );
        } else {
            strength = crossbow.getAttributeValue(
                    crossbowStack,
                    drawStrengthAttribute
            );
        }

        // ===== Tetra 箭矢速度 =====

        float velocityBonus =
                crossbow.getEffectLevel(
                        crossbowStack,
                        ItemEffect.velocity
                ) / 100.0F;

        float projectileVelocity =
                ModularCrossbowItemImpl.getProjectileVelocity(
                        strength,
                        velocityBonus
                );

        // ===== 穿透效果 =====

        int piercingLevel =
                crossbow.getEffectLevel(
                        crossbowStack,
                        ItemEffect.piercing
                ) + EnchantmentHelper.getItemEnchantmentLevel(
                        Enchantments.PIERCING,
                        crossbowStack
                );

        // ===== 多重射擊數量 =====

        int multishotEnchantLevel =
                EnchantmentHelper.getItemEnchantmentLevel(
                        Enchantments.MULTISHOT,
                        crossbowStack
                ) * 3;

        int projectileCount = Math.max(
                crossbow.getEffectLevel(
                        crossbowStack,
                        ItemEffect.multishot
                ) + multishotEnchantLevel,
                1
        );

        // ===== 多重射擊散射角度 =====

        double spread = crossbow.getEffectEfficiency(
                crossbowStack,
                ItemEffect.multishot
        );

        if (spread == 0.0 && multishotEnchantLevel > 0) {
            spread = 10.0;
        }

        // ===== 瞄準目標 =====

        double dx = target.getX() - maid.getX();

        double dz = target.getZ() - maid.getZ();

        double horizontalDistance = Math.sqrt(
                dx * dx + dz * dz
        );

        // 先建立第一支箭，取得投射物的實際生成高度
        AbstractArrow firstArrow = arrowItem.createArrow(
                maid.level(),
                ammoStack,
                maid
        );

        // 使用與 Minecraft 原版弩相同的目標高度
        double dy = target.getY(0.75)
                - firstArrow.getY();

        // Tetra 弩的實際箭矢速度
        float actualVelocity = projectileVelocity * 3.15F;

        // 估算箭矢到達目標所需的飛行時間
        double flightTime = horizontalDistance / actualVelocity;

        // 估算重力造成的垂直下墜
        double compensation = 0.5 * 0.05
                * flightTime * flightTime;

        // 計算射擊方向
        Vec3 baseDirection = new Vec3(
                dx,
                dy + compensation,
                dz
        ).normalize();

        int spawnedCount = 0;

        // ===== 依照多重射擊數量生成箭矢 =====

        for (int i = 0; i < projectileCount; i++) {

            AbstractArrow arrow;

            if (i == 0) {
                arrow = firstArrow;
            } else {
                arrow = arrowItem.createArrow(
                        maid.level(),
                        ammoStack,
                        maid
                );
            }

            arrow.setSoundEvent(
                    net.minecraft.sounds.SoundEvents.CROSSBOW_HIT
            );

            arrow.setShotFromCrossbow(true);
            arrow.setCritArrow(true);

            // Tetra 原生傷害公式
            arrow.setBaseDamage(
                    arrow.getBaseDamage() - 2.0F + strength / 3.0F
            );

            if (projectileVelocity > 1.0F) {
                arrow.setBaseDamage(
                        arrow.getBaseDamage() / projectileVelocity
                );
            }

            // 穿透效果
            if (piercingLevel > 0) {
                arrow.setPierceLevel((byte) piercingLevel);
            }

            // Tetra 原生的多重射擊角度公式
            double angleDegrees =
                    -spread * (projectileCount - 1) / 2.0
                            + spread * i;

            double angleRadians = Math.toRadians(angleDegrees);

            double cos = Math.cos(angleRadians);
            double sin = Math.sin(angleRadians);

            // 將中心方向繞 Y 軸旋轉
            double rotatedX =
                    baseDirection.x * cos
                            - baseDirection.z * sin;

            double rotatedZ =
                    baseDirection.x * sin
                            + baseDirection.z * cos;

            arrow.shoot(
                    rotatedX,
                    baseDirection.y,
                    rotatedZ,
                    projectileVelocity * 3.15F,
                    1.0F
            );

            // 觸發 Tetra 原生投射物生成事件
            MinecraftForge.EVENT_BUS.post(
                    new ModularProjectileSpawnEvent(
                            crossbowStack,
                            ammoStack,
                            maid,
                            arrow,
                            maid.level(),
                            1
                    )
            );

            boolean spawned = maid.level().addFreshEntity(arrow);

            if (spawned) {
                spawnedCount++;
            }
        }

        // 沒有任何箭矢成功生成時，保留裝填狀態
        if (spawnedCount == 0) {
            return false;
        }

        // ===== 射擊成功後清除裝填狀態 =====

        tag.remove("ChargedProjectiles");

        crossbow.setLoaded(crossbowStack, false);

        // 每次射擊只消耗一次耐久度
        crossbowStack.hurtAndBreak(
                1,
                maid,
                entity -> entity.broadcastBreakEvent(
                        net.minecraft.world.InteractionHand.MAIN_HAND
                )
        );

        // 觸發 Tetra 原生武器使用效果
        crossbow.applyUsageEffects(
                maid,
                crossbowStack,
                1.0
        );

        maid.level().playSound(
                null,
                maid.getX(),
                maid.getY(),
                maid.getZ(),
                net.minecraft.sounds.SoundEvents.CROSSBOW_SHOOT,
                net.minecraft.sounds.SoundSource.NEUTRAL,
                1.0F,
                1.0F
        );

        return true;
    }
}