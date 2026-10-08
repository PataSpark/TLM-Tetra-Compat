package io.github.pataspark.tlmtetracompat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.items.modular.impl.bow.ModularBowItem;

import java.util.Map;

public class TetraShootTargetTask extends Behavior<EntityMaid> {

    private int seeTime = 0;
    private int attackTime = 0;
    private int powerTime = 0;

    public TetraShootTargetTask() {
        super(
                Map.of(
                        MemoryModuleType.ATTACK_TARGET,
                        MemoryStatus.VALUE_PRESENT
                ),
                1200
        );
    }

    @Override
    protected boolean checkExtraStartConditions(
            ServerLevel level,
            EntityMaid maid) {

        return maid.getMainHandItem().getItem()
                instanceof ModularBowItem
                && maid.getBrain()
                .getMemory(MemoryModuleType.ATTACK_TARGET)
                .filter(LivingEntity::isAlive)
                .filter(maid::canSee)
                .isPresent();
    }

    @Override
    protected void start(
            ServerLevel level,
            EntityMaid maid,
            long gameTime) {

        seeTime = 0;
        attackTime = 0;
        powerTime = 0;

        maid.setSwingingArms(true);
    }

    @Override
    protected void stop(
            ServerLevel level,
            EntityMaid maid,
            long gameTime) {

        seeTime = 0;
        attackTime = 0;
        powerTime = 0;

        maid.setSwingingArms(false);
        maid.stopUsingItem();
    }

    @Override
    protected void tick(
            ServerLevel level,
            EntityMaid maid,
            long gameTime) {

        if (attackTime > 0) {
            attackTime--;
        }

        var targetOptional = maid.getBrain()
                .getMemory(MemoryModuleType.ATTACK_TARGET);

        if (targetOptional.isEmpty()) {
            return;
        }

        LivingEntity target = targetOptional.get();

        maid.getLookControl().setLookAt(target, 30.0F, 30.0F);

        boolean canSee = maid.canSee(target);

        if (canSee) {
            seeTime = Math.max(seeTime + 1, 0);
        } else {
            seeTime = Math.min(seeTime - 1, 0);
        }

        if (maid.isUsingItem()) {
            ItemStack bowStack = maid.getMainHandItem();

            if (bowStack.getItem() instanceof ModularBowItem) {
                int usedTicks = maid.getTicksUsingItem();

                int requiredTicks =
                        TetraBowProjectileHelper.getDrawDuration(
                                maid,
                                bowStack
                        );

                if (usedTicks >= requiredTicks) {
                    powerTime = usedTicks;

                    float drawProgress = Math.min(
                            1.0F,
                            (float) powerTime / Math.max(1, requiredTicks)
                    );

                    maid.performRangedAttack(target, drawProgress);

                    maid.stopUsingItem();
                    attackTime = 20;
                }
            }
        }

        if (!maid.isUsingItem()
                && attackTime <= 0
                && seeTime >= -60) {

            maid.startUsingItem(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    protected boolean canStillUse(
            ServerLevel level,
            EntityMaid maid,
            long gameTime) {

        return checkExtraStartConditions(level, maid);
    }
}