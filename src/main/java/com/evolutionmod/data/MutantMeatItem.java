package com.evolutionmod.data;

import com.evolutionmod.genetics.Gene;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.util.ItemNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

/**
 * Viande mutante : drop spécial des mobs hautement évolués.
 * Ses effets dépendent du génome du mob tué.
 *
 * - Venin élevé → poison quand mangé
 * - Bioluminescence → vision nocturne
 * - Régénération élevée → régénération
 * - Force élevée → force temporaire
 * - Taille géante → satiété extrême
 *
 * NOTE 1.21.1 : FoodProperties.Builder#saturationMod → #saturationModifier.
 * ItemStack#getTag/hasTag retirés → utilitaire ItemNbt.
 * FoodProperties.Builder#meat() a été retiré (vérifié absent de la liste de
 * méthodes disponibles en 1.21.x) — le marquage "viande" servait surtout au
 * système de reproduction des loups/chiens et a migré vers les data
 * components (Consumable). Sans certitude sur le remplaçant exact, ce trait
 * cosmétique est simplement omis plutôt que deviné.
 */
public class MutantMeatItem extends Item {

    private static final Random RANDOM = new Random();

    public MutantMeatItem() {
        super(new Item.Properties()
            .food(new FoodProperties.Builder()
                .nutrition(6)
                .saturationModifier(0.6f)
                .build()
            )
        );
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof Player player) {
            applyGeneticFoodEffects(stack, player);
        }
        return super.finishUsingItem(stack, level, user);
    }

    private void applyGeneticFoodEffects(ItemStack stack, Player player) {
        if (!ItemNbt.hasKey(stack, "GeneticData")) return;

        Genome genome = Genome.fromNBT(ItemNbt.get(stack).getCompound("GeneticData"));

        // VENIN → empoisonnement
        double venom = genome.getGeneValue(Gene.GeneType.VENOMOUS);
        if (venom > 3.0) {
            int amplifier = (int) Math.min(3, venom / 3.0);
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200 * amplifier, amplifier - 1));
        }

        // BIOLUMINESCENCE → vision nocturne
        double biolum = genome.getGeneValue(Gene.GeneType.BIOLUMINESCENCE);
        if (biolum > 4.0) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3000, 0));
        }

        // RÉGÉNÉRATION → régénération rapide
        double regen = genome.getGeneValue(Gene.GeneType.REGENERATION);
        if (regen > 0.8) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, (int) regen));
        }

        // FORCE élevée → force temporaire
        double strength = genome.getGeneValue(Gene.GeneType.STRENGTH);
        if (strength > 6.0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, (int) (strength / 3)));
        }

        // VITESSE élevée → vitesse temporaire
        double speed = genome.getGeneValue(Gene.GeneType.SPEED);
        if (speed > 3.0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 800, (int) speed - 1));
        }

        // RÉSISTANCE AU FEU → immunité au feu brève
        double fireRes = genome.getGeneValue(Gene.GeneType.FIRE_RESISTANCE);
        if (fireRes > 0.8) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
        }

        // MUTATION RADICALE → effet aléatoire (chaos génétique)
        if (genome.getTotalMutations() > 50 && RANDOM.nextDouble() < 0.4) {
            applyRandomEffect(player);
        }
    }

    private void applyRandomEffect(Player player) {
        MobEffectInstance[] effects = {
            new MobEffectInstance(MobEffects.CONFUSION, 400, 1),
            new MobEffectInstance(MobEffects.LEVITATION, 100, 0),
            new MobEffectInstance(MobEffects.LUCK, 2400, 0),
            new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1),
            new MobEffectInstance(MobEffects.JUMP, 400, 2),
            new MobEffectInstance(MobEffects.ABSORPTION, 600, 2),
        };
        player.addEffect(effects[RANDOM.nextInt(effects.length)]);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (ItemNbt.hasKey(stack, "GeneticData")) {
            Genome genome = Genome.fromNBT(ItemNbt.get(stack).getCompound("GeneticData"));
            tooltip.add(Component.literal("§7Origine: §f" + genome.getSpeciesLineage()));
            tooltip.add(Component.literal("§7Mutations: §f" + genome.getTotalMutations()));

            double venom = genome.getGeneValue(Gene.GeneType.VENOMOUS);
            if (venom > 3) tooltip.add(Component.literal("§c⚠ Contient du venin !"));

            double biolum = genome.getGeneValue(Gene.GeneType.BIOLUMINESCENCE);
            if (biolum > 4) tooltip.add(Component.literal("§b✦ Bioluminescent"));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
