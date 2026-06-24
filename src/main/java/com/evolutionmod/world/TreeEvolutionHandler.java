package com.evolutionmod.world;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.genetics.Gene;
import com.evolutionmod.genetics.Genome;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Gère l'évolution des arbres, buissons et plantes.
 *
 * Les arbres évoluent via le génome d'espèce global :
 * - Couleur des feuilles (hue génétique)
 * - Hauteur et forme
 * - Bioluminescence des troncs
 * - Fruits mutants
 * - Dureté du bois
 *
 * Note: La couleur effective des blocs nécessite un renderer client custom,
 * mais on stocke les données dans les tags de chunk pour le rendu.
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID)
public class TreeEvolutionHandler {

    private static final Random RANDOM = new Random();

    /**
     * Quand un arbre pousse, lui appliquer les traits du génome de son espèce.
     */
    @SubscribeEvent
    public static void onSaplingGrow(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockState placed = event.getPlacedBlock();
        String species = detectTreeSpecies(placed.getBlock());
        if (species == null) return;

        Genome treeGenome = EvolutionManager.getInstance().getSpeciesTemplate(species);
        if (treeGenome == null) return;

        // Stocker les données génétiques dans le chunk pour le renderer
        BlockPos pos = event.getPos();
        applyTreeGenetics(level, pos, treeGenome, species);
    }

    /**
     * Quand des feuilles (de chêne, seul arbre vanilla à drop des pommes) sont
     * cassées et qu'une pomme allait tomber, on a une chance de la remplacer
     * par un fruit muté reflétant le génome actuel de l'espèce. Sans ce hook,
     * generateMutantFruit() ne serait jamais appelée et FRUIT_EFFECT / FRUIT_SIZE
     * resteraient des gènes purement décoratifs sans impact en jeu.
     */
    @SubscribeEvent
    public static void onLeavesBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockState broken = event.getState();
        if (!(broken.getBlock() instanceof LeavesBlock)) return;

        String species = detectLeafSpecies(broken.getBlock());
        if (species == null) return;

        Genome treeGenome = EvolutionManager.getInstance().getSpeciesTemplate(species);
        if (treeGenome == null) return;

        // Probabilité de drop de pomme vanilla (~0.5% par feuille cassée).
        // On ne remplace par un fruit muté que si l'espèce a effectivement divergé,
        // pour ne pas spammer des fruits "mutés" identiques au vanilla sur un arbre frais.
        boolean hasDivergedFruit = treeGenome.getGeneValue(Gene.GeneType.FRUIT_EFFECT) > 2.0
                || treeGenome.getTotalMutations() > 5;

        if (hasDivergedFruit && RANDOM.nextDouble() < 0.005) {
            ItemStack fruit = generateMutantFruit(treeGenome);
            level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                level,
                event.getPos().getX() + 0.5,
                event.getPos().getY() + 0.5,
                event.getPos().getZ() + 0.5,
                fruit
            ));
        }
    }


    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockState broken = event.getState();
        if (!isWoodBlock(broken.getBlock())) return;

        String species = detectLogSpecies(broken.getBlock());
        if (species == null) return;

        Genome treeGenome = EvolutionManager.getInstance().getSpeciesTemplate(species);
        if (treeGenome == null) return;

        // Wood hardness génétique -> durabilité des outils
        double hardness = treeGenome.getGeneValue(Gene.GeneType.WOOD_HARDNESS);

        // Si le bois est extrêmement dur (muté), générer un loot spécial
        if (hardness > 7.0 && RANDOM.nextDouble() < 0.3) {
            ItemStack specialWood = createMutantWoodDrop(treeGenome, species);
            // Le drop est géré via les loot tables; on stocke dans le NBT
            level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                level,
                event.getPos().getX() + 0.5,
                event.getPos().getY() + 0.5,
                event.getPos().getZ() + 0.5,
                specialWood
            ));
        }
    }

    /**
     * Applique les traits génétiques à un arbre lors de sa pousse.
     */
    private static void applyTreeGenetics(ServerLevel level, BlockPos pos, Genome genome, String species) {
        try {
            double biolum = genome.getGeneValue(Gene.GeneType.BIOLUMINESCENCE);
            double heightMax = genome.getGeneValue(Gene.GeneType.HEIGHT_MAX);
            double leafColor = genome.getGeneValue(Gene.GeneType.COLOR_HUE);

            // Bioluminescence extrême -> remplacer une partie des feuilles par des
            // feuilles bioluminescentes portant la teinte génétique réelle de l'arbre.
            if (biolum > 8.0) {
                makeBioluminescentLeaves(level, pos, biolum, leafColor);
            }

            // Bûches très endurcies -> remplacer le tronc par des bûches mutantes
            double woodHardness = genome.getGeneValue(Gene.GeneType.WOOD_HARDNESS);
            if (woodHardness > 7.0) {
                makeMutantTrunk(level, pos, woodHardness, leafColor);
            }

            EvolutionMod.LOGGER.debug("Arbre {} poussé avec gènes: hauteur={:.1f}, biolum={:.1f}, hue={:.0f}",
                species, heightMax, biolum, leafColor);

        } catch (Exception e) {
            EvolutionMod.LOGGER.warn("Erreur applyTreeGenetics: {}", e.getMessage());
        }
    }

    /**
     * Remplace une fraction des feuilles normales par le vrai bloc
     * {@link com.evolutionmod.blocks.BioluminescentLeavesBlock}, en propageant
     * le niveau de lumière (gène BIOLUMINESCENCE) et la teinte (gène COLOR_HUE)
     * directement dans le blockstate posé.
     */
    private static void makeBioluminescentLeaves(ServerLevel level, BlockPos centerPos,
                                                  double biolumValue, double hueValue) {
        int radius = 4;
        int lightLevel = Math.max(1, Math.min(15, (int) biolumValue));
        int hueSegment = com.evolutionmod.blocks.BioluminescentLeavesBlock.hueToSegment(hueValue);

        BlockState bioluminescentState = com.evolutionmod.blocks.ModBlocks.BIOLUMINESCENT_LEAVES.get()
                .defaultBlockState()
                .setValue(com.evolutionmod.blocks.BioluminescentLeavesBlock.GENETIC_LIGHT, lightLevel)
                .setValue(com.evolutionmod.blocks.BioluminescentLeavesBlock.GENETIC_HUE_SEGMENT, hueSegment)
                .setValue(LeavesBlock.PERSISTENT, true); // évite la décomposition immédiate

        for (BlockPos pos : BlockPos.betweenClosed(
            centerPos.offset(-radius, 3, -radius),
            centerPos.offset(radius, 8, radius))) {

            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof LeavesBlock && RANDOM.nextDouble() < 0.15) {
                level.setBlock(pos, bioluminescentState, 3);
            }
        }
    }

    /**
     * Remplace une fraction des bûches du tronc par le vrai bloc
     * {@link com.evolutionmod.blocks.MutantLogBlock}, en conservant l'axe
     * d'origine et en propageant la dureté (WOOD_HARDNESS) et la teinte
     * (COLOR_HUE) dans le blockstate.
     */
    private static void makeMutantTrunk(ServerLevel level, BlockPos centerPos,
                                        double hardnessValue, double hueValue) {
        int radius = 1; // le tronc est étroit, contrairement à la canopée
        int hardnessLevel = com.evolutionmod.blocks.MutantLogBlock.hardnessToLevel(hardnessValue);
        int hueSegment = com.evolutionmod.blocks.MutantLogBlock.hueToSegment(hueValue);

        for (BlockPos pos : BlockPos.betweenClosed(
            centerPos.offset(-radius, 0, -radius),
            centerPos.offset(radius, 6, radius))) {

            BlockState state = level.getBlockState(pos);
            if (isWoodBlock(state.getBlock()) && state.hasProperty(RotatedPillarBlock.AXIS)) {
                BlockState mutantState = com.evolutionmod.blocks.ModBlocks.MUTANT_LOG.get()
                        .defaultBlockState()
                        .setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS))
                        .setValue(com.evolutionmod.blocks.MutantLogBlock.GENETIC_HARDNESS, hardnessLevel)
                        .setValue(com.evolutionmod.blocks.MutantLogBlock.GENETIC_HUE_SEGMENT, hueSegment);

                level.setBlock(pos, mutantState, 3);
            }
        }
    }

    /**
     * Crée un drop de bois muté avec des propriétés spéciales encodées en NBT.
     */
    private static ItemStack createMutantWoodDrop(Genome genome, String species) {
        // Sélectionner le type de bois de base
        ItemStack wood = new ItemStack(getMutantWoodItem(genome, species));

        com.evolutionmod.util.ItemNbt.update(wood, nbt -> {
            nbt.put("GeneticData", genome.toNBT());
            nbt.putDouble("WoodHardness", genome.getGeneValue(Gene.GeneType.WOOD_HARDNESS));
            nbt.putDouble("ColorHue", genome.getGeneValue(Gene.GeneType.COLOR_HUE));
            nbt.putInt("TotalMutations", genome.getTotalMutations());
        });

        // Nom coloré selon la dureté
        double hardness = genome.getGeneValue(Gene.GeneType.WOOD_HARDNESS);
        String prefix = hardness > 8 ? "§4§lADAMANTIN" : hardness > 5 ? "§6Durci" : "§aMuté";
        wood.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
            net.minecraft.network.chat.Component.literal(
                prefix + " §r" + species + " §7[gen " + genome.getTotalMutations() + "]"
            ));

        return wood;
    }

    /**
     * Choisit le type d'item de bois selon le génome (plus muté = bois plus rare).
     */
    private static net.minecraft.world.item.Item getMutantWoodItem(Genome genome, String species) {
        int mutations = genome.getTotalMutations();
        if (mutations > 100) return Items.NETHER_BRICK; // Bois tellement muté = quasi pierre
        if (mutations > 50) return Items.BLACKSTONE;     // Bois ultra-dur
        return switch (species) {
            case "oak" -> Items.OAK_LOG;
            case "spruce" -> Items.SPRUCE_LOG;
            case "birch" -> Items.BIRCH_LOG;
            case "jungle" -> Items.JUNGLE_LOG;
            case "acacia" -> Items.ACACIA_LOG;
            case "dark_oak" -> Items.DARK_OAK_LOG;
            case "mangrove" -> Items.MANGROVE_LOG;
            case "cherry" -> Items.CHERRY_LOG;
            default -> Items.OAK_LOG;
        };
    }

    /**
     * Génère un fruit muté selon les gènes de l'arbre.
     *
     * Délègue à {@link com.evolutionmod.items.MutantFruitItem#createFromGenome},
     * qui est la seule source de vérité pour les effets dynamiques du fruit
     * (appliqués réellement à la consommation, pas seulement en lore statique).
     * L'item de base choisi ici sert uniquement de "skin" visuel/sonore vanilla
     * tant qu'aucun modèle 3D custom n'est fourni pour MUTANT_FRUIT.
     */
    public static ItemStack generateMutantFruit(Genome treeGenome) {
        double fruitEffect = treeGenome.getGeneValue(Gene.GeneType.FRUIT_EFFECT);

        // On utilise l'item MUTANT_FRUIT du mod comme support, mais on conserve
        // selectFruitType() pour les cas où un visuel vanilla connu est préférable
        // (ex: Chorus Fruit pour le comportement de téléportation visuel).
        if (fruitEffect > 15.0) {
            // Cas spécial : le comportement de téléportation du Chorus Fruit
            // n'est pas reproductible par notre item custom, donc on garde le vrai bloc vanilla
            // et on lui attache quand même les données génétiques pour la cohérence du loot.
            ItemStack chorus = new ItemStack(Items.CHORUS_FRUIT, 1);
            com.evolutionmod.util.ItemNbt.update(chorus, nbt ->
                nbt.put("GeneticOrigin", treeGenome.toNBT())
            );
            chorus.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal(
                    hueToColorCode(treeGenome.getGeneValue(Gene.GeneType.FRUIT_COLOR)) +
                    "Chorus Muté §7[effet " + String.format("%.1f", fruitEffect) + "]"
                ));
            return chorus;
        }

        return com.evolutionmod.items.MutantFruitItem.createFromGenome(
            treeGenome, com.evolutionmod.items.ModItems.MUTANT_FRUIT.get()
        );
    }

    private static String hueToColorCode(double hue) {
        if (hue < 30) return "§c";      // Rouge
        if (hue < 60) return "§6";      // Orange
        if (hue < 90) return "§e";      // Jaune
        if (hue < 150) return "§a";     // Vert
        if (hue < 210) return "§b";     // Cyan
        if (hue < 270) return "§9";     // Bleu
        if (hue < 330) return "§5";     // Violet
        return "§d";                     // Rose
    }

    // =================== DETECTION HELPERS ===================

    private static String detectTreeSpecies(Block block) {
        String name = block.toString().toLowerCase();
        if (name.contains("oak_sapling")) return "oak";
        if (name.contains("spruce_sapling")) return "spruce";
        if (name.contains("birch_sapling")) return "birch";
        if (name.contains("jungle_sapling")) return "jungle";
        if (name.contains("acacia_sapling")) return "acacia";
        if (name.contains("dark_oak_sapling")) return "dark_oak";
        if (name.contains("mangrove_propagule")) return "mangrove";
        if (name.contains("cherry_sapling")) return "cherry";
        return null;
    }

    /**
     * Détecte l'espèce à partir d'un bloc de feuilles. Seul le chêne drop des
     * pommes en vanilla ; on se limite à lui pour rester cohérent avec la
     * loot table d'origine que l'on enrichit (et non que l'on remplace entièrement).
     */
    private static String detectLeafSpecies(Block block) {
        String name = block.toString().toLowerCase();
        if (name.contains("oak_leaves") && !name.contains("dark_oak")) return "oak";
        return null;
    }

    private static String detectLogSpecies(Block block) {
        String name = block.toString().toLowerCase();
        if (name.contains("oak_log") || name.contains("oak_wood")) return "oak";
        if (name.contains("spruce_log")) return "spruce";
        if (name.contains("birch_log")) return "birch";
        if (name.contains("jungle_log")) return "jungle";
        if (name.contains("acacia_log")) return "acacia";
        if (name.contains("dark_oak_log")) return "dark_oak";
        if (name.contains("mangrove_log")) return "mangrove";
        if (name.contains("cherry_log")) return "cherry";
        return null;
    }

    private static boolean isWoodBlock(Block block) {
        String name = block.toString().toLowerCase();
        return name.contains("_log") || name.contains("_wood") || name.contains("_stem");
    }
}
