package com.evolutionmod.core;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.config.EvoConfig;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Traduit un génome en caractéristiques concrètes de l'entité.
 *
 * Tout passe par des {@link AttributeModifier} identifiés : on ne touche jamais
 * aux valeurs de base vanilla, l'effet est donc réversible, cumulable avec les
 * autres mods, et un gène neutre ne change strictement rien.
 */
public final class Phenotype {

    private Phenotype() {}

    private record Mapping(GeneType gene, Holder<Attribute> attribute, ResourceLocation id,
                           AttributeModifier.Operation op, double factor, boolean multiplier) {}

    private static final List<Mapping> MAPPINGS = List.of(
        new Mapping(GeneType.SIZE, Attributes.SCALE, EvolutionMod.id("gene_size"),
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 1.0, true),
        new Mapping(GeneType.SPEED, Attributes.MOVEMENT_SPEED, EvolutionMod.id("gene_speed"),
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 1.0, true),
        new Mapping(GeneType.STRENGTH, Attributes.ATTACK_DAMAGE, EvolutionMod.id("gene_strength"),
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 1.0, true),
        new Mapping(GeneType.VITALITY, Attributes.MAX_HEALTH, EvolutionMod.id("gene_vitality"),
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 1.0, true),
        new Mapping(GeneType.ARMOR, Attributes.ARMOR, EvolutionMod.id("gene_armor"),
            AttributeModifier.Operation.ADD_VALUE, 1.0, false),
        new Mapping(GeneType.AGILITY, Attributes.JUMP_STRENGTH, EvolutionMod.id("gene_agility"),
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 0.6, true),
        new Mapping(GeneType.AGILITY, Attributes.SAFE_FALL_DISTANCE, EvolutionMod.id("gene_agility_fall"),
            AttributeModifier.Operation.ADD_VALUE, 4.0, true),
        new Mapping(GeneType.TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE, EvolutionMod.id("gene_toughness"),
            AttributeModifier.Operation.ADD_VALUE, 0.8, false),
        new Mapping(GeneType.AGGRESSION, Attributes.FOLLOW_RANGE, EvolutionMod.id("gene_aggression"),
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, 1.0, true)
    );

    /**
     * Applique le génome à l'entité.
     *
     * @param fresh vrai pour une créature qui vient de naître : elle reçoit alors
     *              tous ses points de vie (sinon un géant naîtrait blessé).
     */
    public static void apply(LivingEntity entity, Genome genome, boolean fresh) {
        boolean player = entity instanceof Player;
        double expression = player ? EvoConfig.playerExpression() : 1.0;

        for (Mapping m : MAPPINGS) {
            AttributeInstance inst = entity.getAttribute(m.attribute());
            if (inst == null) continue;
            double amount = 0.0;
            if (genome.has(m.gene())) {
                double v = genome.get(m.gene());
                amount = (m.multiplier() ? (v - 1.0) : v) * m.factor() * expression;
            }
            inst.removeModifier(m.id());
            if (Math.abs(amount) > 1.0e-4) {
                inst.addPermanentModifier(new AttributeModifier(m.id(), amount, m.op()));
            }
        }

        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        } else if (fresh && !player) {
            entity.setHealth(entity.getMaxHealth());
        }

        if (entity instanceof Creeper creeper && genome.has(GeneType.EXPLOSIVE)) {
            applyExplosionRadius(creeper, genome.get(GeneType.EXPLOSIVE));
        }
    }

    /** Retire toute trace génétique d'une entité. */
    public static void clear(LivingEntity entity) {
        for (Mapping m : MAPPINGS) {
            AttributeInstance inst = entity.getAttribute(m.attribute());
            if (inst != null) inst.removeModifier(m.id());
        }
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
    }

    private static void applyExplosionRadius(Creeper creeper, double explosive) {
        int radius = (int) Math.max(1, Math.min(8, Math.round(3.0 * explosive)));
        CompoundTag tag = new CompoundTag();
        creeper.addAdditionalSaveData(tag);
        if (tag.getByte("ExplosionRadius") != radius) {
            tag.putByte("ExplosionRadius", (byte) radius);
            creeper.readAdditionalSaveData(tag);
        }
    }

    /**
     * La version 1.0 du mod écrasait directement les valeurs de base des
     * attributs (zombies à 250 PV, etc.). On remet les valeurs vanilla.
     */
    @SuppressWarnings("unchecked")
    public static void repairLegacyAttributes(LivingEntity entity) {
        try {
            AttributeSupplier defaults = DefaultAttributes.getSupplier(
                (EntityType<? extends LivingEntity>) entity.getType());
            for (Mapping m : MAPPINGS) {
                AttributeInstance inst = entity.getAttribute(m.attribute());
                if (inst != null && defaults.hasAttribute(m.attribute())) {
                    inst.setBaseValue(defaults.getBaseValue(m.attribute()));
                }
            }
            for (Holder<Attribute> h : List.of(Attributes.GRAVITY)) {
                AttributeInstance inst = entity.getAttribute(h);
                if (inst != null && defaults.hasAttribute(h)) inst.setBaseValue(defaults.getBaseValue(h));
            }
            entity.setGlowingTag(false);
            entity.getPersistentData().remove("EvolutionGenome");
        } catch (Exception e) {
            EvolutionMod.LOGGER.debug("Réparation d'attributs impossible pour {} : {}", entity, e.toString());
        }
    }
}
