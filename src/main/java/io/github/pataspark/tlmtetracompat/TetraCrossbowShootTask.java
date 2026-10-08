
package io.github.pataspark.tlmtetracompat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.items.modular.impl.crossbow.ModularCrossbowItemImpl;

import java.util.Map;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class TetraCrossbowShootTask extends Behavior<EntityMaid> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private enum CrossbowState {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK
    }

    private CrossbowState state = CrossbowState.UNCHARGED;
    private int chargeTime = 0;
    private int attackDelay = 0;

    public TetraCrossbowShootTask() {
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
                instanceof ModularCrossbowItemImpl
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

        ItemStack stack = maid.getMainHandItem();

        state = TetraCrossbowProjectileHelper
                .hasLoadedProjectile(stack)
                ? CrossbowState.CHARGED
                : CrossbowState.UNCHARGED;

        chargeTime = 0;
        attackDelay = 0;

        maid.setSwingingArms(true);
    }

    @Override
    protected void stop(
            ServerLevel level,
            EntityMaid maid,
            long gameTime) {

        LOGGER.info(
                "[TLM Tetra Compat] ShootTask STOP, "
                        + "state=" + state
                        + ", swingingArms=" + maid.isSwingingArms()
        );

        state = CrossbowState.UNCHARGED;
        chargeTime = 0;
        attackDelay = 0;

        maid.setSwingingArms(false);
        maid.stopUsingItem();
        maid.setChargingCrossbow(false);
    }

    @Override
    protected boolean canStillUse(
            ServerLevel level,
            EntityMaid maid,
            long gameTime) {

        return checkExtraStartConditions(level, maid);
    }

    @Override
    protected void tick(
            ServerLevel level,
            EntityMaid maid,
            long gameTime) {

        var targetOptional = maid.getBrain()
                .getMemory(MemoryModuleType.ATTACK_TARGET);

        if (targetOptional.isEmpty()) {
            return;
        }

        LivingEntity target = targetOptional.get();

        maid.getLookControl().setLookAt(
                target,
                30.0F,
                30.0F
        );

        ItemStack stack = maid.getMainHandItem();

        if (!(stack.getItem()
                instanceof ModularCrossbowItemImpl crossbow)) {
            return;
        }

        if (state == CrossbowState.READY_TO_ATTACK
                && attackDelay % 5 == 0) {

            LOGGER.info(
                    "[TLM Tetra Compat] "
                            + "state=" + state
                            + ", attackDelay=" + attackDelay
                            + ", swingingArms=" + maid.isSwingingArms()
                            + ", isUsingItem=" + maid.isUsingItem()
            );
        }

        switch (state) {

            case UNCHARGED -> {

                if (TetraCrossbowProjectileHelper
                        .hasLoadedProjectile(stack)) {

                    state = CrossbowState.CHARGED;
                    break;
                }

                chargeTime = 0;

                maid.startUsingItem(InteractionHand.MAIN_HAND);
                maid.setChargingCrossbow(true);

                state = CrossbowState.CHARGING;
            }

            case CHARGING -> {

                chargeTime++;

                int requiredTicks = crossbow.getReloadDuration(
                        stack,
                        maid
                );

                if (chargeTime % 10 == 0) {
                    LOGGER.info(
                            "[TLM Tetra Compat] "
                                    + "chargeTime=" + chargeTime
                                    + ", requiredTicks=" + requiredTicks
                                    + ", isUsingItem=" + maid.isUsingItem()
                                    + ", useItemRemainingTicks="
                                    + maid.getUseItemRemainingTicks()
//                                    + ", isChargingCrossbow="
//                                    + maid.isChargingCrossbow()
                    );
                }

                if (chargeTime >= requiredTicks) {

                    maid.stopUsingItem();
                    maid.setChargingCrossbow(false);

                    boolean loaded =
                            TetraCrossbowProjectileHelper.loadArrow(
                                    maid,
                                    stack
                            );

                    if (loaded) {
                        state = CrossbowState.CHARGED;
                    } else {
                        state = CrossbowState.UNCHARGED;
                    }
                }
            }

            case CHARGED -> {

                attackDelay = 20;

                state = CrossbowState.READY_TO_ATTACK;
            }

            case READY_TO_ATTACK -> {

                if (attackDelay > 0) {
                    attackDelay--;
                    break;
                }

                if (!maid.canSee(target)) {
                    break;
                }


                boolean fired =
                        TetraCrossbowProjectileHelper.fireLoadedArrow(
                                maid,
                                target,
                                stack
                        );

                if (fired) {
                    state = CrossbowState.UNCHARGED;
                    attackDelay = 20;
                } else {
                    // 射擊失敗時，不直接刪除已裝填彈藥
                    attackDelay = 20;
                }
            }
        }
    }
}
