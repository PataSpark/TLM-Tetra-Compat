package io.github.pataspark.tlmtetracompat;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidRangedWalkToTarget;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskBowAttack;
import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import se.mickelus.tetra.items.modular.impl.bow.ModularBowItem;
import java.util.List;

public class TetraBowAttack extends TaskBowAttack {

    public static final ResourceLocation UID =
            new ResourceLocation(TlmTetraCompat.MOD_ID, "tetra_bow_attack");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Items.BOW);
    }

    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        return stack.getItem() instanceof ModularBowItem;
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {

        BehaviorControl<EntityMaid> findTargetTask =
                StartAttacking.create(
                        this::hasTetraBowAndArrow,
                        IRangedAttackTask::findFirstValidAttackTarget
                );

        BehaviorControl<EntityMaid> stopAttackingTask =
                StopAttackingIfTargetInvalid.create(
                        target -> !hasTetraBowAndArrow(maid)
                );

        BehaviorControl<EntityMaid> moveToTargetTask =
                MaidRangedWalkToTarget.create(0.6f);

        BehaviorControl<EntityMaid> maidAttackStrafingTask =
                new TetraAttackStrafingTask();

        BehaviorControl<EntityMaid> shootTargetTask =
                new TetraShootTargetTask();

        return Lists.newArrayList(
                Pair.of(5, findTargetTask),
                Pair.of(5, stopAttackingTask),
                Pair.of(5, moveToTargetTask),
                Pair.of(5, maidAttackStrafingTask),
                Pair.of(5, shootTargetTask)
        );
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createRideBrainTasks(
            EntityMaid maid) {

        BehaviorControl<EntityMaid> findTargetTask =
                StartAttacking.create(
                        this::hasTetraBowAndArrow,
                        IRangedAttackTask::findFirstValidAttackTarget
                );

        BehaviorControl<EntityMaid> stopAttackingTask =
                StopAttackingIfTargetInvalid.create(
                        target -> !hasTetraBowAndArrow(maid)
                );

        BehaviorControl<EntityMaid> shootTargetTask =
                new TetraShootTargetTask();

        return Lists.newArrayList(
                Pair.of(5, findTargetTask),
                Pair.of(5, stopAttackingTask),
                Pair.of(5, shootTargetTask)
        );
    }

    private boolean hasTetraBowAndArrow(EntityMaid maid) {
        if (!(maid.getMainHandItem().getItem() instanceof ModularBowItem)) {
            return false;
        }

        for (int i = 0; i < maid.getAvailableInv(true).getSlots(); i++) {
            ItemStack stack = maid.getAvailableInv(true).getStackInSlot(i);

            if (stack.getItem() instanceof ArrowItem) {
                return true;
            }
        }

        return false;
    }

    private int findArrow(EntityMaid maid) {
        for (int i = 0; i < maid.getAvailableInv(true).getSlots(); i++) {
            ItemStack stack = maid.getAvailableInv(true).getStackInSlot(i);

            if (stack.getItem() instanceof ArrowItem) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void performRangedAttack(
            EntityMaid shooter,
            LivingEntity target,
            float distanceFactor) {

        int arrowSlot = findArrow(shooter);

        if (arrowSlot < 0) {
            return;
        }

        ItemStack bowStack = shooter.getMainHandItem();

        if (!(bowStack.getItem() instanceof ModularBowItem bow)) {
            return;
        }

        ItemStack arrowStack = shooter.getAvailableInv(true)
                .getStackInSlot(arrowSlot);

        int drawProgress = Math.round(
                Math.min(
                        1.0F,
                        distanceFactor
                ) * 20.0F
        );

        int firedCount = TetraBowProjectileHelper.fireMultipleArrows(
                shooter,
                target,
                bowStack,
                arrowStack,
                drawProgress
        );

        if (firedCount > 0
                && !TetraBowProjectileHelper.isInfiniteAmmo(
                bowStack,
                arrowStack
        )) {
            arrowStack.shrink(firedCount);
        }
    }
}
