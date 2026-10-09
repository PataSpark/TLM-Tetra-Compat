package io.github.pataspark.tlmtetracompat;

import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidRangedWalkToTarget;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskCrossBowAttack;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;


import java.util.List;

public class TetraCrossbowAttack extends TaskCrossBowAttack {

    public static final ResourceLocation UID =
            new ResourceLocation(
                    TlmTetraCompat.MOD_ID,
                    "tetra_crossbow_attack"
            );

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Items.CROSSBOW);
    }

    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        return TetraLegacyCrossbowCompat.isTetraCrossbow(stack);
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>>
    createBrainTasks(EntityMaid maid) {

        BehaviorControl<EntityMaid> findTargetTask =
                StartAttacking.create(
                        this::hasTetraCrossbowAndAmmo,
                        IRangedAttackTask::findFirstValidAttackTarget
                );

        BehaviorControl<EntityMaid> stopAttackingTask =
                StopAttackingIfTargetInvalid.create(
                        target -> !hasTetraCrossbowAndAmmo(maid)
                );

        BehaviorControl<EntityMaid> moveToTargetTask =
                MaidRangedWalkToTarget.create(0.6F);

        BehaviorControl<EntityMaid> strafingTask =
                new TetraCrossbowStrafingTask();

        BehaviorControl<EntityMaid> shootTargetTask =
                new TetraCrossbowShootTask();

        return Lists.newArrayList(
                Pair.of(5, findTargetTask),
                Pair.of(5, stopAttackingTask),
                Pair.of(5, moveToTargetTask),
                Pair.of(5, strafingTask),
                Pair.of(5, shootTargetTask)
        );
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>>
    createRideBrainTasks(EntityMaid maid) {

        BehaviorControl<EntityMaid> findTargetTask =
                StartAttacking.create(
                        this::hasTetraCrossbowAndAmmo,
                        IRangedAttackTask::findFirstValidAttackTarget
                );

        BehaviorControl<EntityMaid> stopAttackingTask =
                StopAttackingIfTargetInvalid.create(
                        target -> !hasTetraCrossbowAndAmmo(maid)
                );

        BehaviorControl<EntityMaid> shootTargetTask =
                new TetraCrossbowShootTask();

        return Lists.newArrayList(
                Pair.of(5, findTargetTask),
                Pair.of(5, stopAttackingTask),
                Pair.of(5, shootTargetTask)
        );
    }

    private boolean hasTetraCrossbowAndAmmo(EntityMaid maid) {

        ItemStack crossbowStack = maid.getMainHandItem();

        if (!TetraLegacyCrossbowCompat.isTetraCrossbow(crossbowStack)) {
            return false;
        }

        // 已裝填時，即使背包沒有箭也允許攻擊
        if (TetraLegacyCrossbowCompat.isLoaded(crossbowStack)) {
            return true;
        }

        // 尚未裝填時，尋找女僕背包中的箭
        for (int i = 0;
             i < maid.getAvailableInv(true).getSlots();
             i++) {

            ItemStack stack = maid.getAvailableInv(true)
                    .getStackInSlot(i);

            if (stack.getItem() instanceof ArrowItem) {
                return true;
            }
        }

        return false;
    }
}