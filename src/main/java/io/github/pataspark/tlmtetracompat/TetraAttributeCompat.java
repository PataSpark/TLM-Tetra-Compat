package io.github.pataspark.tlmtetracompat;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Collection;

public final class TetraAttributeCompat {

    private TetraAttributeCompat() {
    }

    @SafeVarargs
    public static double calculateValue(
            Attribute attribute,
            Collection<AttributeModifier>... modifierGroups) {

        double addition = attribute.getDefaultValue();
        double multiplyBase = 0.0D;
        double multiplyTotal = 1.0D;

        for (Collection<AttributeModifier> group : modifierGroups) {
            if (group == null) {
                continue;
            }

            for (AttributeModifier modifier : group) {
                switch (modifier.getOperation()) {
                    case ADDITION ->
                            addition += modifier.getAmount();

                    case MULTIPLY_BASE ->
                            multiplyBase += modifier.getAmount();

                    case MULTIPLY_TOTAL ->
                            multiplyTotal *= 1.0D + modifier.getAmount();
                }
            }
        }

        return attribute.sanitizeValue(
                addition * (1.0D + multiplyBase) * multiplyTotal
        );
    }
}