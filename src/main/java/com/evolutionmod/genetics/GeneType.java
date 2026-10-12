package com.evolutionmod.genetics;

import java.util.Locale;

/**
 * Tous les gènes connus du mod.
 *
 * Chaque gène a une plage [min, max] et une valeur « neutre » qui correspond
 * exactement au comportement vanilla : une créature dont tous les gènes sont
 * neutres est strictement identique à la créature d'origine. Les gènes
 * physiques sont exprimés en multiplicateurs (1.0 = vanilla), les traits
 * spéciaux en intensité 0..1 (0 = absent).
 */
public enum GeneType {
    // ── Morphologie ──────────────────────────────────────────────
    SIZE(Category.MORPHOLOGY, 0.5, 2.2, 1.0),
    HUE(Category.MORPHOLOGY, 0.0, 360.0, 0.0),
    PIGMENT(Category.MORPHOLOGY, 0.0, 1.0, 0.0),
    GLOW(Category.MORPHOLOGY, 0.0, 1.0, 0.0),

    // ── Capacités physiques ──────────────────────────────────────
    SPEED(Category.PHYSICAL, 0.6, 1.8, 1.0),
    STRENGTH(Category.PHYSICAL, 0.5, 3.0, 1.0),
    VITALITY(Category.PHYSICAL, 0.5, 3.5, 1.0),
    ARMOR(Category.PHYSICAL, 0.0, 10.0, 0.0),
    AGILITY(Category.PHYSICAL, 0.7, 1.8, 1.0),
    TOUGHNESS(Category.PHYSICAL, 0.0, 1.0, 0.0),

    // ── Métabolisme ──────────────────────────────────────────────
    REGENERATION(Category.METABOLIC, 0.0, 1.0, 0.0),
    FIRE_RESISTANCE(Category.METABOLIC, 0.0, 1.0, 0.0),
    TOXIN_RESISTANCE(Category.METABOLIC, 0.0, 1.0, 0.0),
    SUN_RESISTANCE(Category.METABOLIC, 0.0, 1.0, 0.0),

    // ── Comportement ─────────────────────────────────────────────
    AGGRESSION(Category.BEHAVIOR, 0.5, 2.0, 1.0),
    CAMOUFLAGE(Category.BEHAVIOR, 0.0, 1.0, 0.0),

    // ── Armes biologiques ────────────────────────────────────────
    VENOM(Category.OFFENSIVE, 0.0, 1.0, 0.0),
    FROST(Category.OFFENSIVE, 0.0, 1.0, 0.0),
    EXPLOSIVE(Category.OFFENSIVE, 0.5, 2.5, 1.0),

    // ── Végétaux ─────────────────────────────────────────────────
    GROWTH(Category.PLANT, 0.5, 3.0, 1.0),
    YIELD(Category.PLANT, 0.5, 3.0, 1.0),
    POTENCY(Category.PLANT, 0.0, 1.0, 0.0),
    DENSITY(Category.PLANT, 0.0, 1.0, 0.0);

    public enum Category {
        MORPHOLOGY(0xFF6FD3FF),
        PHYSICAL(0xFFFF8A5B),
        METABOLIC(0xFF7BE07B),
        BEHAVIOR(0xFFC58BFF),
        OFFENSIVE(0xFFFF5B7A),
        PLANT(0xFFB8E05B);

        public final int color;

        Category(int color) {
            this.color = color;
        }

        public String translationKey() {
            return "evolutionmod.category." + name().toLowerCase(Locale.ROOT);
        }
    }

    public final Category category;
    public final double min;
    public final double max;
    public final double neutral;

    GeneType(Category category, double min, double max, double neutral) {
        this.category = category;
        this.min = min;
        this.max = max;
        this.neutral = neutral;
    }

    public double range() {
        return max - min;
    }

    public double clamp(double value) {
        if (this == HUE) {
            double v = value % 360.0;
            return v < 0 ? v + 360.0 : v;
        }
        return Math.max(min, Math.min(max, value));
    }

    /** Position normalisée 0..1 d'une valeur dans la plage du gène. */
    public double normalize(double value) {
        return range() <= 0 ? 0 : (value - min) / range();
    }

    /** Distance normalisée 0..1 entre deux valeurs de ce gène (circulaire pour la teinte). */
    public double distance(double a, double b) {
        if (this == HUE) {
            double d = Math.abs(a - b) % 360.0;
            if (d > 180.0) d = 360.0 - d;
            return d / 180.0;
        }
        return range() <= 0 ? 0 : Math.abs(a - b) / range();
    }

    public boolean isMultiplier() {
        return neutral == 1.0;
    }

    public String translationKey() {
        return "evolutionmod.gene." + name().toLowerCase(Locale.ROOT);
    }

    public String descriptionKey() {
        return translationKey() + ".desc";
    }

    /** Affichage court d'une valeur : "×1.35", "42%", "210°", "+3.5". */
    public String format(double value) {
        if (this == HUE) return String.format(Locale.ROOT, "%.0f°", value);
        if (this == ARMOR) return String.format(Locale.ROOT, "+%.1f", value);
        if (isMultiplier()) return String.format(Locale.ROOT, "×%.2f", value);
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0);
    }

    public static GeneType byName(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
