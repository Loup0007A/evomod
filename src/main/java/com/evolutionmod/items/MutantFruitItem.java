package com.evolutionmod.items;

import com.evolutionmod.genetics.Gene;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.util.ItemNbt;
import net.minecraft.core.component.DataComponents;
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

import java.util.*;

/**
 * Fruit Muté — poussé sur les arbres dont le génome a divergé.
 *
 * Les effets sont entièrement déterminés par les gènes de l'arbre parent :
 *
 *  FRUIT_EFFECT  → sélectionne la famille d'effets
 *  FRUIT_SIZE    → satiété et durée des effets
 *  FRUIT_COLOR   → teinte visuelle (nom coloré)
 *  PHOTOSYNTHESIS → puissance photosynthétique transférée
 *  VENOMOUS (arbre) → fruit toxique potentiel
 *
 * Table des effets par FRUIT_EFFECT :
 *   0-3   → simple pomme mutée (+ satiété)
 *   3-6   → vitesse et saut améliorés
 *   6-9   → régénération et absorption
 *   9-12  → vision nocturne + résistance
 *   12-15 → force + résistance au feu
 *   15-18 → lévitation + chance
 *   18+   → CHAOS (effets aléatoires multiples)
 *
 * NOTE 1.21.1 : FoodProperties.Builder#saturationMod a été renommé
 * #saturationModifier. ItemStack#getOrCreateTag/getTag/hasTag/setHoverName
 * ont été retirés ; on passe par ItemNbt (custom data component) et
 * DataComponents.CUSTOM_NAME.
 */
public class MutantFruitItem extends Item {

    private static final Random RAND = new Random();

    // Effets groupés par palier de FRUIT_EFFECT
    private static final List<List<int[]>> EFFECT_TIERS = List.of(
        // [0-3] Léger
        List.of(new int[]{/* vitesse */ 1, 400, 0}, new int[]{/* saut */ 8, 200, 0}),
        // [3-6] Modéré
        List.of(new int[]{1, 800, 1}, new int[]{8, 400, 1}, new int[]{/* regen */ 10, 200, 0}),
        // [6-9] Bon
        List.of(new int[]{10, 800, 1}, new int[]{/* absorption */ 22, 600, 1}),
        // [9-12] Très bon
        List.of(new int[]{/* night vision */ 16, 3000, 0}, new int[]{/* resistance */ 11, 600, 0}),
        // [12-15] Puissant
        List.of(new int[]{/* strength */ 5, 1200, 1}, new int[]{/* fire resist */ 12, 600, 0}),
        // [15-18] Exceptionnel
        List.of(new int[]{/* levitation */ 25, 200, 0}, new int[]{/* luck */ 35, 2400, 0},
                new int[]{11, 1200, 1}),
        // [18+] Chaos — géré séparément
        List.of()
    );

    public MutantFruitItem() {
        super(new Item.Properties()
            .food(new FoodProperties.Builder()
                .nutrition(4)
                .saturationModifier(0.5f)
                .build()
            )
        );
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide()) {
            applyFruitEffects(stack, user);
        }
        return super.finishUsingItem(stack, level, user);
    }

    private void applyFruitEffects(ItemStack stack, LivingEntity user) {
        if (!ItemNbt.has(stack)) return;

        CompoundTag tag = ItemNbt.get(stack);
        double fruitEffect  = tag.getDouble("FruitEffect");
        double fruitSize    = tag.getDouble("FruitSize");
        boolean isVenomous  = tag.getBoolean("IsVenomous");
        double photosynth   = tag.getDouble("Photosynthesis");

        if (fruitSize > 2.0) {
            user.addEffect(new MobEffectInstance(MobEffects.SATURATION,
                    (int)(fruitSize * 40), (int)(fruitSize / 2)));
        }

        // ── Fruit toxique ─────────────────────────────────────────────
        if (isVenomous && RAND.nextDouble() < 0.6) {
            user.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
            if (user instanceof Player p) {
                p.sendSystemMessage(Component.literal("§c⚠ Ce fruit était venimeux !"));
            }
            return;
        }

        // ── Photosynthèse → régénération solaire ─────────────────────
        if (photosynth > 3.0 && user.level().isDay()) {
            user.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                    (int)(photosynth * 100), 1));
        }

        // ── Effets par palier ─────────────────────────────────────────
        int tier = Math.min(6, (int)(fruitEffect / 3.0));

        if (tier >= 6 || fruitEffect >= 18) {
            applyChaosEffects(user, fruitEffect);
        } else {
            List<int[]> effects = EFFECT_TIERS.get(tier);
            int durationMultiplier = (int) Math.max(1, fruitSize);

            for (int[] eff : effects) {
                MobEffectInstance instance = buildEffect(eff[0], eff[1] * durationMultiplier, eff[2]);
                if (instance != null) user.addEffect(instance);
            }
        }
    }

    /**
     * Effets chaos pour les fruits ultra-mutés (FRUIT_EFFECT >= 18).
     * 2-4 effets aléatoires simultanés.
     */
    private void applyChaosEffects(LivingEntity user, double power) {
        if (user instanceof Player p) {
            p.sendSystemMessage(Component.literal("§5✦ Fruit Apex — Effets chaotiques !"));
        }

        MobEffectInstance[] pool = {
            new MobEffectInstance(MobEffects.MOVEMENT_SPEED,   1200, 2),
            new MobEffectInstance(MobEffects.JUMP,              800, 3),
            new MobEffectInstance(MobEffects.REGENERATION,      600, 2),
            new MobEffectInstance(MobEffects.DAMAGE_BOOST,     1200, 2),
            new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 800, 2),
            new MobEffectInstance(MobEffects.NIGHT_VISION,     3000, 0),
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE,  2400, 0),
            new MobEffectInstance(MobEffects.LUCK,             2400, 1),
            new MobEffectInstance(MobEffects.ABSORPTION,        600, 3),
            new MobEffectInstance(MobEffects.LEVITATION,        100, 0),
            new MobEffectInstance(MobEffects.CONFUSION,         400, 0),
            new MobEffectInstance(MobEffects.BLINDNESS,         200, 0),
        };

        int count = 2 + (int)(power / 6.0);
        List<MobEffectInstance> shuffled = new ArrayList<>(Arrays.asList(pool));
        Collections.shuffle(shuffled, RAND);
        shuffled.stream().limit(count).forEach(user::addEffect);
    }

    @Nullable
    private MobEffectInstance buildEffect(int legacyId, int duration, int amplifier) {
        return switch (legacyId) {
            case 1  -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED,    duration, amplifier);
            case 5  -> new MobEffectInstance(MobEffects.DAMAGE_BOOST,      duration, amplifier);
            case 8  -> new MobEffectInstance(MobEffects.JUMP,              duration, amplifier);
            case 10 -> new MobEffectInstance(MobEffects.REGENERATION,      duration, amplifier);
            case 11 -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier);
            case 12 -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE,   duration, amplifier);
            case 16 -> new MobEffectInstance(MobEffects.NIGHT_VISION,      duration, amplifier);
            case 22 -> new MobEffectInstance(MobEffects.ABSORPTION,        duration, amplifier);
            case 25 -> new MobEffectInstance(MobEffects.LEVITATION,        duration, amplifier);
            case 35 -> new MobEffectInstance(MobEffects.LUCK,              duration, amplifier);
            default -> null;
        };
    }

    // ──────────────────────────────────────────────────────
    //  Tooltip
    // ──────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                 List<Component> tooltip, TooltipFlag flag) {
        if (!ItemNbt.has(stack)) return;

        CompoundTag tag = ItemNbt.get(stack);
        double fruitEffect = tag.getDouble("FruitEffect");
        double fruitSize   = tag.getDouble("FruitSize");
        boolean venomous   = tag.getBoolean("IsVenomous");
        String lineage     = tag.getString("TreeLineage");

        tooltip.add(Component.literal("§7Arbre: §f" + lineage));
        tooltip.add(Component.literal("§7Effet génétique: §f" + String.format("%.1f", fruitEffect) +
                " / 20  §8(tier " + Math.min(6, (int)(fruitEffect / 3)) + ")"));
        tooltip.add(Component.literal("§7Taille: §f" + String.format("%.1f", fruitSize) + "x"));

        if (venomous) {
            tooltip.add(Component.literal("§c⚠ Possiblement venimeux"));
        }

        if (fruitEffect >= 18) {
            tooltip.add(Component.literal("§5§l✦ EFFETS CHAOTIQUES — imprévisible"));
        } else if (fruitEffect >= 12) {
            tooltip.add(Component.literal("§6Effets puissants garantis"));
        }
    }

    // ──────────────────────────────────────────────────────
    //  Factory statique appelée par TreeEvolutionHandler
    // ──────────────────────────────────────────────────────

    /**
     * Crée un ItemStack de fruit muté à partir du génome de l'arbre.
     */
    public static ItemStack createFromGenome(Genome treeGenome, Item fruitItem) {
        ItemStack stack = new ItemStack(fruitItem,
                Math.max(1, (int) treeGenome.getGeneValue(Gene.GeneType.FRUIT_SIZE)));

        ItemNbt.update(stack, tag -> {
            tag.putDouble("FruitEffect",   treeGenome.getGeneValue(Gene.GeneType.FRUIT_EFFECT));
            tag.putDouble("FruitSize",     treeGenome.getGeneValue(Gene.GeneType.FRUIT_SIZE));
            tag.putDouble("FruitHue",      treeGenome.getGeneValue(Gene.GeneType.FRUIT_COLOR));
            tag.putDouble("Photosynthesis",treeGenome.getGeneValue(Gene.GeneType.PHOTOSYNTHESIS));
            tag.putBoolean("IsVenomous",   treeGenome.getGeneValue(Gene.GeneType.VENOMOUS) > 3.0);
            tag.putString("TreeLineage",   treeGenome.getSpeciesLineage());
            tag.put("GeneticOrigin",       treeGenome.toNBT());
        });

        double hue = treeGenome.getGeneValue(Gene.GeneType.FRUIT_COLOR);
        String colorCode = hueToMinecraftColor(hue);
        double effect = treeGenome.getGeneValue(Gene.GeneType.FRUIT_EFFECT);
        String tier   = effect >= 18 ? "§5§lApex" : effect >= 12 ? "§6Muté" : effect >= 6 ? "§eÉvolué" : "§aBrut";
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(colorCode + "Fruit " + tier));

        return stack;
    }

    private static String hueToMinecraftColor(double hue) {
        if (hue < 30)  return "§c";
        if (hue < 60)  return "§6";
        if (hue < 90)  return "§e";
        if (hue < 150) return "§a";
        if (hue < 210) return "§b";
        if (hue < 270) return "§9";
        if (hue < 330) return "§5";
        return "§d";
    }
}
