package io.github.pataspark.tlmtetracompat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.items.IItemHandler;
import se.mickelus.tetra.items.modular.impl.crossbow.ModularCrossbowItemImpl;

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

        net.minecraft.world.entity.projectile.AbstractArrow arrow =
                arrowItem.createArrow(
                        maid.level(),
                        ammoStack,
                        maid
                );

        arrow.setSoundEvent(
                net.minecraft.sounds.SoundEvents.CROSSBOW_HIT
        );

        arrow.setShotFromCrossbow(true);
        arrow.setCritArrow(true);

        double dx = target.getX() - maid.getX();

        double dy = target.getY()
                + target.getBbHeight() * 0.5
                - arrow.getY();

        double dz = target.getZ() - maid.getZ();

        double horizontalDistance = Math.sqrt(
                dx * dx + dz * dz
        );

        arrow.shoot(
                dx,
                dy + horizontalDistance * 0.1,
                dz,
                3.15F,
                1.0F
        );

        boolean spawned = maid.level().addFreshEntity(arrow);

        if (!spawned) {
            return false;
        }

        // 確認投射物成功生成後，才清除裝填資料
        tag.remove("ChargedProjectiles");

        if (crossbowStack.getItem()
                instanceof ModularCrossbowItemImpl crossbow) {
            crossbow.setLoaded(crossbowStack, false);
        }

        crossbowStack.hurtAndBreak(
                1,
                maid,
                entity -> entity.broadcastBreakEvent(
                        net.minecraft.world.InteractionHand.MAIN_HAND
                )
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