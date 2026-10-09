
package io.github.pataspark.tlmtetracompat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;

import java.util.Map;

public class TetraCrossbowStrafingTask extends Behavior<EntityMaid> {

    private int strafingTime = -1;
    private boolean strafingClockwise = false;
    private boolean strafingBackwards = false;

    public TetraCrossbowStrafingTask() {
        super(Map.of(
                MemoryModuleType.WALK_TARGET,
                MemoryStatus.VALUE_ABSENT,

                MemoryModuleType.LOOK_TARGET,
                MemoryStatus.REGISTERED,

                MemoryModuleType.ATTACK_TARGET,
                MemoryStatus.VALUE_PRESENT,

                MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                MemoryStatus.VALUE_PRESENT
        ), 1200);
    }

    private void stopInPlace(EntityMaid maid) {
        maid.getNavigation().stop();
        maid.setXxa(0.0F);
        maid.setYya(0.0F);
        maid.setSpeed(0.0F);
    }

    @Override
    protected boolean checkExtraStartConditions(
            ServerLevel level,
            EntityMaid maid) {

        return TetraLegacyCrossbowCompat.isTetraCrossbow(
                maid.getMainHandItem()
        )
                && maid.getBrain()
                .getMemory(MemoryModuleType.ATTACK_TARGET)
                .filter(LivingEntity::isAlive)
                .isPresent();
    }

    @Override
    protected boolean canStillUse(
            ServerLevel level,
            EntityMaid maid,
            long gameTime) {

        return checkExtraStartConditions(level, maid);
    }

    @Override
    protected void start(
            ServerLevel level,
            EntityMaid maid,
            long gameTime) {

        maid.setSwingingArms(true);
    }

    @Override
    protected void stop(
            ServerLevel level,
            EntityMaid maid,
            long gameTime) {

        maid.setSwingingArms(false);
        maid.getMoveControl().strafe(0.0F, 0.0F);
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

        double distance = maid.distanceTo(target);

        if (distance < maid.searchRadius()) {
            strafingTime++;
        } else {
            strafingTime = -1;
        }

        if (strafingTime >= 20) {

            if (maid.getRandom().nextFloat() < 0.3F) {
                strafingClockwise = !strafingClockwise;
            }

            if (maid.getRandom().nextFloat() < 0.3F) {
                strafingBackwards = !strafingBackwards;
            }

            strafingTime = 0;
        }

        if (strafingTime > -1) {

            int maxAttackDistance = 15;

            if (distance > maxAttackDistance * 0.5F) {
                strafingBackwards = false;
            } else if (distance < maxAttackDistance * 0.2F) {
                strafingBackwards = true;
            }

            float forward = strafingBackwards ? -0.5F : 0.5F;
            float sideways = strafingClockwise ? 0.5F : -0.5F;

            if (!maid.hasRestriction()
                    && maid.getOwner() instanceof Player player
                    && maid.distanceTo(player) >= maid.getRestrictRadius()) {

                stopInPlace(maid);

            } else {
                maid.getMoveControl().strafe(forward, sideways);
            }

            maid.setYRot(
                    Mth.rotateIfNecessary(
                            maid.getYRot(),
                            maid.yHeadRot,
                            0.0F
                    )
            );

            BehaviorUtils.lookAtEntity(maid, target);

        } else {
            BehaviorUtils.lookAtEntity(maid, target);
        }
    }
}
