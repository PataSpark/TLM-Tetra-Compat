
package io.github.pataspark.tlmtetracompat.mixin;

import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.AnimationManager;
import io.github.pataspark.tlmtetracompat.TetraCrossbowProjectileHelper;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.AnimationBuilder;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.predicate.AnimationEvent;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.PlayState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.ILoopType.EDefaultLoopTypes;

import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;

@Mixin(value = AnimationManager.class, remap = false)
public abstract class AnimationManagerMixin {

    @Inject(
            method = "predicateMainhandHold",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void tlmTetraCompat$chargedCrossbowHold(
            AnimationEvent<GeckoMaidEntity<?>> event,
            CallbackInfoReturnable<PlayState> cir
    ) {
        IMaid maid = event.getAnimatableEntity().getMaid();

        if (maid == null) {
            return;
        }

        Mob entity = maid.asEntity();

        if (entity.swinging || entity.isUsingItem()) {
            return;
        }

        ItemStack stack = entity.getItemInHand(InteractionHand.MAIN_HAND);

        if (!TetraCrossbowProjectileHelper.hasLoadedProjectile(stack)) {
            return;
        }

        event.getController().setAnimation(
                new AnimationBuilder().addAnimation(
                        "hold_mainhand:charged_crossbow",
                        EDefaultLoopTypes.LOOP
                )
        );

        cir.setReturnValue(PlayState.CONTINUE);
    }
}
