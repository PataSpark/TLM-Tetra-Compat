package io.github.pataspark.tlmtetracompat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import se.mickelus.tetra.items.modular.impl.bow.ModularBowItem;
import se.mickelus.tetra.properties.TetraAttributes;
import se.mickelus.tetra.effect.ItemEffect;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

public class TetraBowProjectileHelper {

    public static double getDrawStrength(ItemStack bowStack) {
        ModularBowItem bow = (ModularBowItem) bowStack.getItem();

        return bow.getAttributeValue(
                bowStack,
                TetraAttributes.drawStrength.get()
        );
    }

    public static int getDrawDuration(
            EntityMaid maid,
            ItemStack bowStack) {

        ModularBowItem bow = (ModularBowItem) bowStack.getItem();

        return bow.getDrawDuration(maid, bowStack);
    }

    public static float getArrowVelocity(
            ItemStack bowStack,
            int drawProgress) {

        ModularBowItem bow = (ModularBowItem) bowStack.getItem();

        double strength = getDrawStrength(bowStack);

        float velocityBonus =
                (float) bow.getEffectLevel(
                        bowStack,
                        ItemEffect.velocity
                ) / 100.0F;

        boolean hasSuspend =
                bow.getEffectLevel(
                        bowStack,
                        ItemEffect.suspend
                ) > 0;

        return ModularBowItem.getArrowVelocity(
                drawProgress,
                strength,
                velocityBonus,
                hasSuspend
        );
    }

    public static int getPiercingLevel(ItemStack bowStack) {
        ModularBowItem bow = (ModularBowItem) bowStack.getItem();

        return bow.getEffectLevel(
                bowStack,
                ItemEffect.piercing
        ) + EnchantmentHelper.getTagEnchantmentLevel(
                Enchantments.PIERCING,
                bowStack
        );
    }

    public static int getMultishotCount(ItemStack bowStack) {
        ModularBowItem bow = (ModularBowItem) bowStack.getItem();

        return Math.max(
                1,
                bow.getEffectLevel(
                        bowStack,
                        ItemEffect.multishot
                )
        );
    }

    public static int getActualArrowCount(
            ItemStack bowStack,
            ItemStack ammoStack) {

        if (ammoStack.isEmpty()) {
            return 0;
        }

        int multishotCount = getMultishotCount(bowStack);

        return Math.min(
                multishotCount,
                ammoStack.getCount()
        );
    }

    public static float getMultishotAngle(
            int arrowIndex,
            int arrowCount,
            float angleStep) {

        if (arrowCount <= 1) {
            return 0.0F;
        }

        float center = (arrowCount - 1) / 2.0F;

        return (arrowIndex - center) * angleStep;
    }

    public static float getMultishotSpread(ItemStack bowStack) {
        ModularBowItem bow = (ModularBowItem) bowStack.getItem();

        return bow.getEffectEfficiency(
                bowStack,
                ItemEffect.multishot
        );
    }

    public static float getArrowInaccuracy(ItemStack bowStack) {
        ModularBowItem bow = (ModularBowItem) bowStack.getItem();

        return Math.max(
                0.0F,
                100.0F - bow.getEffectEfficiency(
                        bowStack,
                        ItemEffect.spread
                )
        );
    }

    public static AbstractArrow createProjectile(
            EntityMaid maid,
            ItemStack ammoStack) {

        if (!(ammoStack.getItem() instanceof ArrowItem ammoItem)) {
            return null;
        }

        Level level = maid.level();

        return ammoItem.createArrow(
                level,
                ammoStack,
                maid
        );
    }

    public static void applyTetraDamage(
            AbstractArrow projectile,
            ItemStack bowStack,
            float projectileVelocity) {

        double strength = getDrawStrength(bowStack);

        projectile.setBaseDamage(
                projectile.getBaseDamage() - 2.0D + strength / 3.0D
        );

        int powerLevel = EnchantmentHelper.getTagEnchantmentLevel(
                Enchantments.POWER_ARROWS,
                bowStack
        );

        if (powerLevel > 0) {
            projectile.setBaseDamage(
                    projectile.getBaseDamage()
                            + powerLevel * 0.5D + 0.5D
            );
        }

        if (projectileVelocity > 1.0F) {
            projectile.setBaseDamage(
                    projectile.getBaseDamage() / projectileVelocity
            );
        }
    }

    public static void applyTetraEffects(
            AbstractArrow projectile,
            ItemStack bowStack,
            int drawProgress) {

        int punchLevel = EnchantmentHelper.getTagEnchantmentLevel(
                Enchantments.PUNCH_ARROWS,
                bowStack
        );

        if (punchLevel > 0) {
            projectile.setKnockback(punchLevel);
        }

        int flameLevel = EnchantmentHelper.getTagEnchantmentLevel(
                Enchantments.FLAMING_ARROWS,
                bowStack
        );

        if (flameLevel > 0) {
            projectile.setSecondsOnFire(100);
        }

        int piercingLevel = getPiercingLevel(bowStack);

        if (piercingLevel > 0) {
            projectile.setPierceLevel((byte) piercingLevel);
        }

        ModularBowItem bow = (ModularBowItem) bowStack.getItem();

        boolean hasSuspend = bow.getEffectLevel(
                bowStack,
                ItemEffect.suspend
        ) > 0;

        if (hasSuspend && drawProgress >= 20) {
            projectile.setNoGravity(true);
        }

        if (drawProgress >= 20) {
            projectile.setCritArrow(true);
        }
    }

    public static boolean isInfiniteAmmo(
            ItemStack bowStack,
            ItemStack ammoStack
    ) {
        boolean hasInfinity =
                EnchantmentHelper.getItemEnchantmentLevel(
                        Enchantments.INFINITY_ARROWS,
                        bowStack
                ) > 0;

        return hasInfinity && ammoStack.is(Items.ARROW);
    }

    public static void spawnSuspendParticles(
            EntityMaid maid,
            AbstractArrow projectile,
            ItemStack bowStack,
            int drawProgress) {

        ModularBowItem bow =
                (ModularBowItem) bowStack.getItem();

        boolean hasSuspend = bow.getEffectLevel(
                bowStack,
                ItemEffect.suspend
        ) > 0;

        if (!hasSuspend || drawProgress < 20) {
            return;
        }

        if (!(maid.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 direction =
                projectile.getDeltaMovement().normalize();

        Vec3 position = projectile.position();

        for (int i = 0; i < 4; i++) {

            Vec3 particlePos = position.add(
                    direction.scale(2 + i * 2)
            );

            serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.01D
            );
        }
    }

    public static void shootProjectile(
            EntityMaid maid,
            LivingEntity target,
            AbstractArrow projectile,
            ItemStack bowStack,
            int drawProgress,
            float yawOffset) {

        float velocity = getArrowVelocity(
                bowStack,
                drawProgress
        );

        Vec3 direction = new Vec3(
                target.getX() - maid.getX(),
                target.getEyeY() - maid.getEyeY(),
                target.getZ() - maid.getZ()
        ).normalize();

        double radians = Math.toRadians(yawOffset);

        double rotatedX =
                direction.x * Math.cos(radians)
                        - direction.z * Math.sin(radians);

        double rotatedZ =
                direction.x * Math.sin(radians)
                        + direction.z * Math.cos(radians);

        projectile.shoot(
                rotatedX,
                direction.y,
                rotatedZ,
                velocity * 3.0F,
                getArrowInaccuracy(bowStack)
        );
    }

    public static int fireMultipleArrows(
            EntityMaid maid,
            LivingEntity target,
            ItemStack bowStack,
            ItemStack ammoStack,
            int drawProgress) {

        if (maid.level().isClientSide) {
            return 0;
        }

        int arrowCount;

        if (isInfiniteAmmo(bowStack, ammoStack)) {
            arrowCount = getMultishotCount(bowStack);
        } else {
            arrowCount = getActualArrowCount(
                    bowStack,
                    ammoStack
            );
        }

        float velocity = getArrowVelocity(
                bowStack,
                drawProgress
        );

        if (arrowCount <= 0 || velocity <= 0.1F) {
            return 0;
        }

        float angleStep = getMultishotSpread(bowStack);
        int firedCount = 0;

        for (int i = 0; i < arrowCount; i++) {

            AbstractArrow projectile = createProjectile(
                    maid,
                    ammoStack
            );

            if (projectile == null) {
                break;
            }

            float yawOffset = getMultishotAngle(
                    i,
                    arrowCount,
                    angleStep
            );

            shootProjectile(
                    maid,
                    target,
                    projectile,
                    bowStack,
                    drawProgress,
                    yawOffset
            );

            applyTetraDamage(
                    projectile,
                    bowStack,
                    velocity
            );

            applyTetraEffects(
                    projectile,
                    bowStack,
                    drawProgress
            );

            if (isInfiniteAmmo(bowStack, ammoStack)) {
                projectile.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }

            if (maid.level().addFreshEntity(projectile)) {
                spawnSuspendParticles(
                        maid,
                        projectile,
                        bowStack,
                        drawProgress
                );

                firedCount++;
            }
        }

        if (firedCount > 0) {
            playBowShootSound(maid, bowStack, drawProgress);

            ModularBowItem bow = (ModularBowItem) bowStack.getItem();

            bow.applyDamage(1, bowStack, maid);
            bow.applyNegativeUsageEffects(maid, bowStack, 1.0D);

            if (drawProgress > 15) {
                bow.applyPositiveUsageEffects(maid, bowStack, 1.0D);
            }
        }

        return firedCount;
    }

    private static void playBowShootSound(
            EntityMaid maid,
            ItemStack bowStack,
            int drawProgress
    ) {
        ModularBowItem bow = (ModularBowItem) bowStack.getItem();

        float projectileVelocity =
                getArrowVelocity(bowStack, drawProgress);

        float velocityBonus =
                (float) bow.getEffectLevel(
                        bowStack,
                        ItemEffect.velocity
                ) / 100.0F;

        boolean hasSuspend =
                bow.getEffectLevel(
                        bowStack,
                        ItemEffect.suspend
                ) > 0;

        float pitchBase = projectileVelocity;

        if (velocityBonus > 0.0F) {
            pitchBase = projectileVelocity
                    - projectileVelocity * velocityBonus;
        } else if (hasSuspend) {
            pitchBase = projectileVelocity / 2.0F;
        }

        float volume = 0.8F + projectileVelocity * 0.2F;

        float pitch = 1.9F
                + maid.level().random.nextFloat() * 0.2F
                - pitchBase * 0.8F;

        maid.level().playSound(
                null,
                maid.getX(),
                maid.getY(),
                maid.getZ(),
                SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS,
                volume,
                pitch
        );
    }
}