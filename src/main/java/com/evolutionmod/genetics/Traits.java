package com.evolutionmod.genetics;

import java.util.ArrayList;
import java.util.List;

/** Traduit un génome en « traits » lisibles (Géant, Venimeux...). */
public final class Traits {

    private Traits() {}

    public record Trait(String key, int color) {
        public String translationKey() {
            return "evolutionmod.trait." + key;
        }
    }

    public static List<Trait> of(Genome g) {
        List<Trait> out = new ArrayList<>();
        check(out, g, GeneType.SIZE, v -> v > 1.3, "giant", 0xFFFF8A5B);
        check(out, g, GeneType.SIZE, v -> v < 0.8, "dwarf", 0xFF6FD3FF);
        check(out, g, GeneType.SPEED, v -> v > 1.2, "fast", 0xFF7BE07B);
        check(out, g, GeneType.SPEED, v -> v < 0.85, "slow", 0xFF8FA6B2);
        check(out, g, GeneType.STRENGTH, v -> v > 1.35, "powerful", 0xFFFF5B7A);
        check(out, g, GeneType.VITALITY, v -> v > 1.4, "robust", 0xFFFF8A5B);
        check(out, g, GeneType.VITALITY, v -> v < 0.8, "frail", 0xFF8FA6B2);
        check(out, g, GeneType.ARMOR, v -> v > 2.5, "armored", 0xFFB0C4D0);
        check(out, g, GeneType.AGILITY, v -> v > 1.2, "agile", 0xFF7BE07B);
        check(out, g, GeneType.TOUGHNESS, v -> v > 0.4, "steadfast", 0xFFB0C4D0);
        check(out, g, GeneType.REGENERATION, v -> v > 0.2, "regenerating", 0xFF7BE07B);
        check(out, g, GeneType.FIRE_RESISTANCE, v -> v > 0.5, "fireproof", 0xFFFFB347);
        check(out, g, GeneType.TOXIN_RESISTANCE, v -> v > 0.5, "immune", 0xFF7BE07B);
        check(out, g, GeneType.SUN_RESISTANCE, v -> v > 0.5, "sunproof", 0xFFFFE066);
        check(out, g, GeneType.AGGRESSION, v -> v > 1.3, "aggressive", 0xFFFF5B7A);
        check(out, g, GeneType.CAMOUFLAGE, v -> v > 0.35, "camouflaged", 0xFFC58BFF);
        check(out, g, GeneType.VENOM, v -> v > 0.15, "venomous", 0xFF9BE05B);
        check(out, g, GeneType.FROST, v -> v > 0.2, "frosty", 0xFF9BE7FF);
        check(out, g, GeneType.EXPLOSIVE, v -> v > 1.3, "explosive", 0xFFFF5B7A);
        check(out, g, GeneType.GLOW, v -> v > 0.4, "glowing", 0xFF57E3C2);
        check(out, g, GeneType.PIGMENT, v -> v > 0.5, "colorful", 0xFFC58BFF);
        check(out, g, GeneType.GROWTH, v -> v > 1.4, "vigorous", 0xFFB8E05B);
        check(out, g, GeneType.YIELD, v -> v > 1.4, "fertile", 0xFFB8E05B);
        check(out, g, GeneType.POTENCY, v -> v > 0.4, "potent", 0xFFC58BFF);
        check(out, g, GeneType.DENSITY, v -> v > 0.5, "dense", 0xFFB0C4D0);
        return out;
    }

    private interface Test {
        boolean ok(double v);
    }

    private static void check(List<Trait> out, Genome g, GeneType t, Test test, String key, int color) {
        if (g.has(t) && test.ok(g.get(t))) out.add(new Trait(key, color));
    }
}
