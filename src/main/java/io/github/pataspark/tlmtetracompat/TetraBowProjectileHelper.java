package io.github.pataspark.tlmtetracompat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
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

    public static void shootProjectile(
            EntityMaid maid,
            LivingEntity target,
            AbstractArrow projectile,
            ItemStack bowStack,
            int drawProgress) {

        float velocity = getArrowVelocity(
                bowStack,
                drawProgress
        );

        Vec3 direction = new Vec3(
                target.getX() - maid.getX(),
                target.getEyeY() - maid.getEyeY(),
                target.getZ() - maid.getZ()
        );

        projectile.shoot(
                direction.x,
                direction.y,
                direction.z,
                velocity * 3.0F,
                0.0F
        );
    }

    public static boolean fireSingleArrow(
            EntityMaid maid,
            LivingEntity target,
            ItemStack bowStack,
            ItemStack ammoStack,
            int drawProgress) {

        if (maid.level().isClientSide) {
            return false;
        }

        AbstractArrow projectile = createProjectile(
                maid,
                ammoStack
        );

        if (projectile == null) {
            return false;
        }

        float velocity = getArrowVelocity(
                bowStack,
                drawProgress
        );

        if (velocity <= 0.1F) {
            return false;
        }

        shootProjectile(
                maid,
                target,
                projectile,
                bowStack,
                drawProgress
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

        boolean spawned = maid.level().addFreshEntity(projectile);

        if (spawned) {
            ModularBowItem bow = (ModularBowItem) bowStack.getItem();
            bow.applyDamage(1, bowStack, maid);
        }

        return spawned;
    }
}