package com.evolutionmod.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * Configuration commune (fichier config/evolutionmod-common.toml).
 */
public final class EvoConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.DoubleValue MUTATION_RATE;
    private static final ForgeConfigSpec.DoubleValue SELECTION_STRENGTH;
    private static final ForgeConfigSpec.BooleanValue PLAYER_EVOLUTION;
    private static final ForgeConfigSpec.DoubleValue PLAYER_EXPRESSION;
    private static final ForgeConfigSpec.IntValue MAX_CATCH_UP_CYCLES;
    private static final ForgeConfigSpec.BooleanValue ANNOUNCE_SPECIATION;
    private static final ForgeConfigSpec.BooleanValue DAILY_REPORT;
    private static final ForgeConfigSpec.BooleanValue GENETIC_DROPS;
    private static final ForgeConfigSpec.BooleanValue PLANT_EVOLUTION;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Rythme de l'évolution").push("evolution");
        MUTATION_RATE = b.comment("Multiplicateur global du taux de mutation (1.0 = normal, 0 = aucune mutation)")
            .defineInRange("mutationRate", 1.0, 0.0, 10.0);
        SELECTION_STRENGTH = b.comment("Force de la sélection naturelle (attirance du gabarit d'espèce vers les individus qui réussissent)")
            .defineInRange("selectionStrength", 1.0, 0.0, 10.0);
        MAX_CATCH_UP_CYCLES = b.comment("Nombre maximal de cycles rattrapés si plusieurs jours passent d'un coup (/time set, sommeil...)")
            .defineInRange("maxCatchUpCycles", 3, 1, 30);
        PLANT_EVOLUTION = b.comment("Les arbres et cultures évoluent-ils ?")
            .define("plantEvolution", true);
        BLACKLIST = b.comment("Entités exclues de l'évolution (ex: \"minecraft:bee\")")
            .defineListAllowEmpty("blacklist", List.of(), o -> o instanceof String);
        b.pop();

        b.comment("Évolution du joueur").push("player");
        PLAYER_EVOLUTION = b.comment("Le joueur évolue-t-il lui aussi ?")
            .define("playerEvolution", true);
        PLAYER_EXPRESSION = b.comment("Intensité d'expression des gènes du joueur (0..1). Faible = changements subtils.")
            .defineInRange("playerExpression", 0.35, 0.0, 1.0);
        b.pop();

        b.comment("Notifications et récompenses").push("feedback");
        ANNOUNCE_SPECIATION = b.comment("Annoncer les spéciations à tous les joueurs")
            .define("announceSpeciation", true);
        DAILY_REPORT = b.comment("Afficher un petit rapport HUD à chaque cycle d'évolution")
            .define("dailyReport", true);
        GENETIC_DROPS = b.comment("Les créatures mutées lâchent des cristaux génomiques et de la viande mutante")
            .define("geneticDrops", true);
        b.pop();

        SPEC = b.build();
    }

    private EvoConfig() {}

    private static <T> T safe(ForgeConfigSpec.ConfigValue<T> value, T fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback; // config pas encore chargée
        }
    }

    public static double mutationRate() { return safe(MUTATION_RATE, 1.0); }
    public static double selectionStrength() { return safe(SELECTION_STRENGTH, 1.0); }
    public static boolean playerEvolution() { return safe(PLAYER_EVOLUTION, true); }
    public static double playerExpression() { return safe(PLAYER_EXPRESSION, 0.35); }
    public static int maxCatchUpCycles() { return safe(MAX_CATCH_UP_CYCLES, 3); }
    public static boolean announceSpeciation() { return safe(ANNOUNCE_SPECIATION, true); }
    public static boolean dailyReport() { return safe(DAILY_REPORT, true); }
    public static boolean geneticDrops() { return safe(GENETIC_DROPS, true); }
    public static boolean plantEvolution() { return safe(PLANT_EVOLUTION, true); }

    public static boolean isBlacklisted(String id) {
        List<? extends String> list = safe(BLACKLIST, List.of());
        return list.contains(id);
    }
}
