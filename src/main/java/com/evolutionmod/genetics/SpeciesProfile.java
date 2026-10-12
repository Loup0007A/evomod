package com.evolutionmod.genetics;

import com.evolutionmod.config.EvoConfig;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Décrit quels gènes possède une espèce et avec quelles valeurs de départ.
 *
 * @param id       identifiant d'espèce ("minecraft:zombie", "minecraft:player", "plant:wheat")
 * @param kind     famille de l'espèce
 * @param baseName nom racine utilisé pour générer les noms de lignées
 */
public record SpeciesProfile(String id, Kind kind, String baseName, Set<GeneType> genes,
                             Map<GeneType, Double> base) {

    public enum Kind {
        HOSTILE, PASSIVE, PLAYER, TREE, CROP;

        public boolean isPlant() {
            return this == TREE || this == CROP;
        }
    }

    public static final String PLAYER_ID = "minecraft:player";

    public double baseValue(GeneType type) {
        Double v = base.get(type);
        return v != null ? v : type.neutral;
    }

    // ───────────────────────── Profils d'entités ─────────────────────────

    private static final Set<GeneType> HOSTILE_GENES = EnumSet.of(
        GeneType.SIZE, GeneType.HUE, GeneType.PIGMENT, GeneType.GLOW,
        GeneType.SPEED, GeneType.STRENGTH, GeneType.VITALITY, GeneType.ARMOR,
        GeneType.AGILITY, GeneType.TOUGHNESS, GeneType.REGENERATION,
        GeneType.FIRE_RESISTANCE, GeneType.TOXIN_RESISTANCE,
        GeneType.AGGRESSION, GeneType.CAMOUFLAGE, GeneType.VENOM, GeneType.FROST);

    private static final Set<GeneType> PASSIVE_GENES = EnumSet.of(
        GeneType.SIZE, GeneType.HUE, GeneType.PIGMENT, GeneType.GLOW,
        GeneType.SPEED, GeneType.VITALITY, GeneType.ARMOR, GeneType.AGILITY,
        GeneType.TOUGHNESS, GeneType.REGENERATION, GeneType.FIRE_RESISTANCE,
        GeneType.TOXIN_RESISTANCE, GeneType.CAMOUFLAGE);

    private static final Set<GeneType> PLAYER_GENES = EnumSet.of(
        GeneType.SIZE, GeneType.SPEED, GeneType.STRENGTH, GeneType.VITALITY,
        GeneType.ARMOR, GeneType.AGILITY, GeneType.REGENERATION,
        GeneType.FIRE_RESISTANCE, GeneType.TOXIN_RESISTANCE);

    /** Vrai si cette entité participe à l'évolution. */
    public static boolean isEvolvable(LivingEntity entity) {
        if (entity instanceof Player) return EvoConfig.playerEvolution();
        if (!(entity instanceof Mob)) return false;               // porte-armures, etc.
        if (entity instanceof EnderDragon || entity instanceof WitherBoss || entity instanceof Warden) return false;
        if (entity instanceof AbstractVillager) return false;     // on ne touche pas aux échanges
        String id = entityId(entity.getType());
        if (EvoConfig.isBlacklisted(id)) return false;
        return entity instanceof Enemy || entity instanceof NeutralMob || isAnimalLike(entity);
    }

    private static boolean isAnimalLike(LivingEntity entity) {
        return entity instanceof net.minecraft.world.entity.animal.Animal
            || entity instanceof net.minecraft.world.entity.animal.WaterAnimal
            || entity instanceof net.minecraft.world.entity.ambient.AmbientCreature;
    }

    public static String entityId(EntityType<?> type) {
        return EntityType.getKey(type).toString();
    }

    public static SpeciesProfile forEntity(LivingEntity entity) {
        String id = entityId(entity.getType());
        String path = EntityType.getKey(entity.getType()).getPath();

        if (entity instanceof Player) {
            return new SpeciesProfile(PLAYER_ID, Kind.PLAYER, "homo", EnumSet.copyOf(PLAYER_GENES),
                new EnumMap<>(GeneType.class));
        }

        boolean hostile = entity instanceof Enemy;
        Set<GeneType> genes = EnumSet.copyOf(hostile ? HOSTILE_GENES : PASSIVE_GENES);
        Map<GeneType, Double> base = new EnumMap<>(GeneType.class);

        if (!hostile && entity instanceof NeutralMob) {
            genes.add(GeneType.STRENGTH);
            genes.add(GeneType.AGGRESSION);
        }
        if (entity.getType().is(EntityTypeTags.UNDEAD)) {
            genes.add(GeneType.SUN_RESISTANCE);
        }
        if (entity instanceof Creeper) {
            genes.add(GeneType.EXPLOSIVE);
        }

        // Quelques prédispositions naturelles pour donner du caractère aux espèces
        switch (path) {
            case "cave_spider" -> base.put(GeneType.VENOM, 0.25);
            case "spider" -> base.put(GeneType.AGILITY, 1.15);
            case "husk" -> base.put(GeneType.SUN_RESISTANCE, 0.4);
            case "stray" -> base.put(GeneType.FROST, 0.2);
            case "blaze", "magma_cube", "strider", "ghast" -> base.put(GeneType.FIRE_RESISTANCE, 0.6);
            case "glow_squid" -> base.put(GeneType.GLOW, 0.6);
            case "drowned" -> base.put(GeneType.AGILITY, 1.1);
            case "witch" -> base.put(GeneType.TOXIN_RESISTANCE, 0.5);
            default -> { }
        }

        return new SpeciesProfile(id, hostile ? Kind.HOSTILE : Kind.PASSIVE, rootName(path), genes, base);
    }

    // ───────────────────────── Profils végétaux ─────────────────────────

    public static final String[] TREE_SPECIES = {
        "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"};
    public static final String[] CROP_SPECIES = {
        "wheat", "carrot", "potato", "beetroot", "sugar_cane", "melon", "pumpkin", "cactus",
        "nether_wart", "sweet_berry"};

    private static final Set<GeneType> TREE_GENES = EnumSet.of(
        GeneType.HUE, GeneType.PIGMENT, GeneType.GLOW, GeneType.GROWTH,
        GeneType.YIELD, GeneType.POTENCY, GeneType.DENSITY);

    private static final Set<GeneType> CROP_GENES = EnumSet.of(
        GeneType.HUE, GeneType.PIGMENT, GeneType.GROWTH, GeneType.YIELD, GeneType.POTENCY);

    public static String plantId(String plant) {
        return "plant:" + plant;
    }

    public static SpeciesProfile forPlant(String plant) {
        boolean tree = java.util.Arrays.asList(TREE_SPECIES).contains(plant);
        return new SpeciesProfile(plantId(plant), tree ? Kind.TREE : Kind.CROP, rootName(plant),
            EnumSet.copyOf(tree ? TREE_GENES : CROP_GENES), new EnumMap<>(GeneType.class));
    }

    /** Retrouve le profil d'une espèce à partir de son identifiant seul (ex : sauvegarde). */
    public static Kind kindOf(String speciesId) {
        if (speciesId.equals(PLAYER_ID)) return Kind.PLAYER;
        if (speciesId.startsWith("plant:")) {
            String p = speciesId.substring(6);
            return java.util.Arrays.asList(TREE_SPECIES).contains(p) ? Kind.TREE : Kind.CROP;
        }
        return Kind.HOSTILE;
    }

    private static String rootName(String path) {
        String clean = path.replace("_", "").toLowerCase(Locale.ROOT);
        if (clean.length() > 5) clean = clean.substring(0, 5);
        return clean;
    }

    // ───────────────────────── Noms de lignées ─────────────────────────

    private static final String[] PREFIXES = {
        "Neo", "Proto", "Xeno", "Hyper", "Crypto", "Meta", "Para", "Omni", "Umbra", "Lumo", "Ferro", "Pyro"};
    private static final String[] SUFFIXES = {
        "morphus", "rex", "oid", "vex", "ax", "ion", "ura", "is", "on", "ix", "ora", "us"};

    /** Nom latinisé déterministe pour la n-ième spéciation d'une espèce. */
    public static String lineageName(String speciesId, String root, int level, long seed) {
        long h = speciesId.hashCode() * 31L + level * 7919L + seed;
        java.util.Random r = new java.util.Random(h);
        String prefix = PREFIXES[r.nextInt(PREFIXES.length)];
        String suffix = SUFFIXES[r.nextInt(SUFFIXES.length)];
        String body = root.isEmpty() ? "" : root.substring(0, Math.min(root.length(), 3 + r.nextInt(3)));
        return prefix + body + suffix;
    }
}
