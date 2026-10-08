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
import se.mickelus.tetra.items.modular.impl.bow.ModularBowItem;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import se.mickelus.tetra.properties.TetraAttributes;

public class TetraBowAttack extends TaskBowAttack {

    public static final ResourceLocation UID =
            new ResourceLocation(TlmTetraCompat.MOD_ID, "tetra_bow_attack");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(ModularBowItem.instance);
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

        boolean fired = TetraBowProjectileHelper.fireSingleArrow(
                shooter,
                target,
                bowStack,
                arrowStack,
                drawProgress
        );

        if (fired) {
            arrowStack.shrink(1);
        }
    }

    private double getTetraDrawStrength(ItemStack bowStack) {
        ModularBowItem bow = (ModularBowItem) bowStack.getItem();

        return bow.getAttributeValue(
                bowStack,
                TetraAttributes.drawStrength.get()
        );
    }
}
