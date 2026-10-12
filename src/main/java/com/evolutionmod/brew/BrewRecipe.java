package com.evolutionmod.brew;

import com.evolutionmod.genetics.GeneType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Chimie des mixtures du chaudron : chaque ingrédient (plante, viande, poisson)
 * pousse certains gènes du joueur dans un sens... en théorie. En pratique, une
 * part de hasard peut inverser l'effet, et certains ingrédients sont toxiques.
 */
public final class BrewRecipe {

    private BrewRecipe() {}

    /** Effet d'un ingrédient : gènes visés, toxicité, inversion, couleur. */
    public record Ingredient(Map<GeneType, Double> genes, double toxicity, boolean inverter, int color) {}

    /** Résultat d'une mixture. */
    public record Outcome(Map<GeneType, Double> deltas, double toxicity, int color) {}

    private static final Map<String, Ingredient> TABLE = new HashMap<>();

    private static void put(String id, int color, double tox, Object... genes) {
        Map<GeneType, Double> m = new EnumMap<>(GeneType.class);
        for (int i = 0; i + 1 < genes.length; i += 2) m.put((GeneType) genes[i], ((Number) genes[i + 1]).doubleValue());
        TABLE.put(id, new Ingredient(m, tox, false, color));
    }

    static {
        // ── Viandes ──
        put("beef", 0xB5443C, 0, GeneType.VITALITY, 1.0, GeneType.STRENGTH, 0.6);
        put("cooked_beef", 0x8A4B2E, 0, GeneType.VITALITY, 1.2, GeneType.STRENGTH, 0.8);
        put("porkchop", 0xE89A9A, 0, GeneType.VITALITY, 0.8, GeneType.SIZE, 0.6);
        put("cooked_porkchop", 0xC98650, 0, GeneType.VITALITY, 1.0, GeneType.SIZE, 0.7);
        put("mutton", 0xC25B4E, 0, GeneType.ARMOR, 1.0, GeneType.VITALITY, 0.4);
        put("cooked_mutton", 0x9C5A3C, 0, GeneType.ARMOR, 1.2, GeneType.VITALITY, 0.5);
        put("chicken", 0xF2C2B0, 1.0, GeneType.AGILITY, 1.0, GeneType.SIZE, -0.5);
        put("cooked_chicken", 0xD69A5C, 0, GeneType.AGILITY, 1.2, GeneType.SPEED, 0.4);
        put("rabbit", 0xD9A07A, 0.3, GeneType.AGILITY, 1.2, GeneType.SPEED, 0.8);
        put("cooked_rabbit", 0xB07A4A, 0, GeneType.AGILITY, 1.3, GeneType.SPEED, 0.9);
        put("rabbit_foot", 0xE3C8A0, 0, GeneType.AGILITY, 2.0, GeneType.SPEED, 1.0);
        put("rotten_flesh", 0x6E7A3C, 2.0, GeneType.STRENGTH, 1.0, GeneType.TOXIN_RESISTANCE, 1.2);
        put("spider_eye", 0x8C1F3A, 2.0, GeneType.TOXIN_RESISTANCE, 1.5, GeneType.AGILITY, 0.5);
        // ── Poissons ──
        put("cod", 0xC4A882, 0, GeneType.AGILITY, 0.8, GeneType.TOXIN_RESISTANCE, 0.5);
        put("cooked_cod", 0xD8C49C, 0, GeneType.AGILITY, 1.0, GeneType.TOXIN_RESISTANCE, 0.6);
        put("salmon", 0xE0715A, 0, GeneType.AGILITY, 0.8, GeneType.VITALITY, 0.6);
        put("cooked_salmon", 0xE08A5A, 0, GeneType.AGILITY, 1.0, GeneType.VITALITY, 0.7);
        put("tropical_fish", 0xF29F3A, 0.3, GeneType.SPEED, 1.4, GeneType.SIZE, -0.6);
        put("pufferfish", 0xE8D23A, 4.0, GeneType.TOXIN_RESISTANCE, 2.5, GeneType.ARMOR, 1.0);
        // ── Plantes ──
        put("carrot", 0xF08A24, 0, GeneType.SPEED, 0.8);
        put("golden_carrot", 0xF2C94C, 0, GeneType.SPEED, 1.3, GeneType.REGENERATION, 0.8);
        put("potato", 0xD8B66A, 0, GeneType.VITALITY, 0.7);
        put("baked_potato", 0xC9963E, 0, GeneType.VITALITY, 0.9);
        put("poisonous_potato", 0x9DBA3C, 2.5, GeneType.TOXIN_RESISTANCE, 1.5);
        put("beetroot", 0xA4272D, 0, GeneType.REGENERATION, 0.8);
        put("beetroot_seeds", 0x8E3A2A, 0, GeneType.REGENERATION, 0.4);
        put("wheat", 0xD9C26A, 0, GeneType.SIZE, -0.6, GeneType.VITALITY, 0.3);
        put("wheat_seeds", 0x6A9A3C, 0, GeneType.SIZE, -0.8);
        put("apple", 0xD2302D, 0, GeneType.REGENERATION, 0.9);
        put("golden_apple", 0xF5D33B, 0, GeneType.REGENERATION, 2.0, GeneType.ARMOR, 1.0);
        put("enchanted_golden_apple", 0xFFE36B, 0, GeneType.REGENERATION, 3.0, GeneType.ARMOR, 2.0, GeneType.VITALITY, 1.5);
        put("melon_slice", 0xE0483A, 0, GeneType.SPEED, 0.7);
        put("sweet_berries", 0xB01C3A, 0.2, GeneType.REGENERATION, 0.8);
        put("glow_berries", 0xF6B23C, 0, GeneType.REGENERATION, 0.8, GeneType.FIRE_RESISTANCE, 0.5);
        put("kelp", 0x3F6A2A, 0, GeneType.AGILITY, 0.9);
        put("dried_kelp", 0x2E3A22, 0, GeneType.AGILITY, 0.7, GeneType.TOXIN_RESISTANCE, 0.3);
        put("brown_mushroom", 0x9A7356, 0.3, GeneType.TOXIN_RESISTANCE, 1.0);
        put("red_mushroom", 0xC23A2E, 1.2, GeneType.TOXIN_RESISTANCE, 1.2, GeneType.STRENGTH, 0.5);
        put("nether_wart", 0x8A1E24, 0.5, GeneType.FIRE_RESISTANCE, 1.8);
        put("cactus", 0x4C8A2E, 0.3, GeneType.ARMOR, 1.6);
        put("sugar_cane", 0x8CC45A, 0, GeneType.SPEED, 1.0);
        put("pumpkin", 0xE38A1E, 0, GeneType.SIZE, 1.2);
        put("cocoa_beans", 0x6A3A1E, 0, GeneType.SPEED, 0.9, GeneType.STRENGTH, 0.3);
        put("chorus_fruit", 0x8E5A8E, 1.0, GeneType.AGILITY, 1.5, GeneType.SIZE, -1.0);
        put("honey_bottle", 0xF0A02A, 0, GeneType.REGENERATION, 1.2);
        put("bread", 0xC48A3E, 0, GeneType.VITALITY, 0.6);
        put("cookie", 0xB2702E, 0, GeneType.SPEED, 0.5);
        put("pumpkin_pie", 0xD87A2A, 0, GeneType.SIZE, 0.8, GeneType.VITALITY, 0.4);
        put("mushroom_stew", 0xB08060, 0, GeneType.TOXIN_RESISTANCE, 0.8);
        put("suspicious_stew", 0x8AA060, 1.0);
        put("vine", 0x3A7A2A, 0, GeneType.AGILITY, 0.8);
        put("lily_pad", 0x2E7A3A, 0, GeneType.AGILITY, 0.6);
        put("bamboo", 0x7AAA3A, 0, GeneType.SPEED, 0.5, GeneType.SIZE, 0.4);
        put("wither_rose", 0x2A1A2A, 5.0, GeneType.ARMOR, 2.0, GeneType.STRENGTH, 1.5);
        // Le mutagène et l'œil fermenté rendent la mixture plus... imprévisible
        TABLE.put("fermented_spider_eye", new Ingredient(new EnumMap<>(GeneType.class), 1.0, true, 0x7A2A3A));
    }

    public static String idOf(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null ? "minecraft:air" : key.toString();
    }

    /** Vrai si l'objet peut aller dans une mixture (plante, viande, poisson ou nourriture). */
    public static boolean isIngredient(ItemStack stack) {
        return !stack.isEmpty() && of(idOf(stack.getItem())) != null;
    }

    /** Effet d'un ingrédient à partir de son identifiant, ou null s'il n'en est pas un. */
    public static Ingredient of(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        if (rl.getNamespace().equals("minecraft")) {
            Ingredient known = TABLE.get(rl.getPath());
            if (known != null) return known;
        }
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null) return null;
        ItemStack stack = new ItemStack(item);
        boolean plant = stack.is(ItemTags.SAPLINGS) || stack.is(ItemTags.FLOWERS) || stack.is(ItemTags.LEAVES)
            || (item instanceof BlockItem bi && (bi.getBlock() instanceof BushBlock
                || bi.getBlock() instanceof CactusBlock || bi.getBlock() instanceof SugarCaneBlock));
        boolean food = stack.has(DataComponents.FOOD);
        boolean mutant = rl.getNamespace().equals("evolutionmod") && rl.getPath().startsWith("mutant_");
        if (!plant && !food && !mutant) return null;

        // Ingrédient inconnu : un gène pseudo-aléatoire, propre à cet objet
        Random r = new Random(id.hashCode());
        GeneType[] pool = {GeneType.SIZE, GeneType.SPEED, GeneType.STRENGTH, GeneType.VITALITY, GeneType.ARMOR,
            GeneType.AGILITY, GeneType.REGENERATION, GeneType.FIRE_RESISTANCE, GeneType.TOXIN_RESISTANCE};
        Map<GeneType, Double> m = new EnumMap<>(GeneType.class);
        m.put(pool[r.nextInt(pool.length)], (mutant ? 1.8 : 0.7) * (r.nextBoolean() ? 1 : 0.6));
        if (mutant) m.put(pool[r.nextInt(pool.length)], 1.2);
        int color = java.awt.Color.HSBtoRGB(r.nextFloat(), 0.6f, 0.85f) & 0xFFFFFF;
        return new Ingredient(m, mutant ? 0.8 : (plant && !food ? 0.2 : 0), false, color);
    }

    /** Calcule l'effet d'une mixture. Déterministe pour une liste et une graine données. */
    public static Outcome compute(List<String> ids, long seed) {
        Random r = new Random(seed);
        Map<GeneType, Double> deltas = new EnumMap<>(GeneType.class);
        Map<String, Integer> counts = new HashMap<>();
        double tox = 0;
        int inverters = 0;
        float hueX = 0, hueY = 0;
        for (String id : ids) {
            Ingredient ing = of(id);
            if (ing == null) continue;
            int n = counts.merge(id, 1, Integer::sum);
            if (n >= 3) tox += 0.7; // surdosage
            tox += ing.toxicity();
            if (ing.inverter()) inverters++;
            for (Map.Entry<GeneType, Double> e : ing.genes().entrySet()) {
                double v = e.getValue() * (0.6 + r.nextDouble() * 0.8);
                if (r.nextDouble() < 0.25) v = -v; // instabilité : l'effet s'inverse
                deltas.merge(e.getKey(), v, Double::sum);
            }
            float[] hsb = java.awt.Color.RGBtoHSB((ing.color() >> 16) & 0xFF, (ing.color() >> 8) & 0xFF,
                ing.color() & 0xFF, null);
            hueX += (float) Math.cos(hsb[0] * Math.PI * 2);
            hueY += (float) Math.sin(hsb[0] * Math.PI * 2);
        }
        if (inverters % 2 == 1) deltas.replaceAll((k, v) -> -v * 1.3);
        if (inverters > 0) tox += 0.5 * inverters;
        if (r.nextDouble() < 0.07) tox += 3; // réaction catastrophique
        float hue = (float) (Math.atan2(hueY, hueX) / (Math.PI * 2));
        if (hue < 0) hue += 1;
        int color = java.awt.Color.HSBtoRGB(hue, Math.min(1f, 0.55f + (float) tox * 0.08f), 0.9f) & 0xFFFFFF;
        return new Outcome(deltas, tox, color);
    }
}
