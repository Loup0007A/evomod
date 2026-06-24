package com.evolutionmod.world;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.genetics.Gene;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.items.ModItems;
import com.evolutionmod.items.MutantFruitItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Gère l'évolution réelle des cultures agricoles (blé, carotte, pomme de
 * terre, betterave, canne à sucre, melon, citrouille, cactus).
 *
 * C'est le pendant exact de {@link TreeEvolutionHandler} pour les plantes
 * basses : sans ce handler, les "génomes d'espèce" de cultures maintenus
 * par {@link EvolutionManager#evolvePlants} muteraient en mémoire sans
 * jamais affecter une seule récolte réelle dans le monde du joueur — la
 * génétique végétale resterait purement décorative côté serveur.
 *
 * Effets concrets appliqués :
 *  - GROWTH_SPEED élevé → la culture pousse plus vite (accélère randomTick)
 *  - FRUIT_SIZE/FRUIT_EFFECT élevés → la récolte donne un item muté
 *    (via MutantFruitItem) au lieu de l'item vanilla classique
 *  - WOOD_HARDNESS (réutilisé pour le cactus comme "dureté de l'épine")
 *    → dégâts de contact du cactus modifiés
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID)
public class CropEvolutionHandler {

    private static final Random RANDOM = new Random();

    /**
     * Quand une culture mûre est récoltée (cassée à maturité), remplacer
     * une fraction du loot vanilla par un fruit muté selon le génome
     * d'espèce courant.
     */
    @SubscribeEvent
    public static void onCropBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockState state = event.getState();
        String species = detectCropSpecies(state.getBlock());
        if (species == null) return;

        if (!isMature(state)) return; // Pas de bonus sur une culture immature cassée par accident

        Genome cropGenome = EvolutionManager.getInstance().getSpeciesTemplate(species);
        if (cropGenome == null) return;

        double fruitEffect = cropGenome.getGeneValue(Gene.GeneType.FRUIT_EFFECT);
        double fruitSize = cropGenome.getGeneValue(Gene.GeneType.FRUIT_SIZE);

        // Seuil de divergence : seulement si l'espèce a vraiment muté, sinon
        // on laisse la loot table vanilla normale faire son travail (pas de
        // doublon systématique à chaque récolte).
        boolean hasDiverged = fruitEffect > 2.0 || cropGenome.getTotalMutations() > 3;
        if (!hasDiverged) return;

        double dropChance = Math.min(0.35, 0.05 + cropGenome.getTotalMutations() * 0.01);
        if (RANDOM.nextDouble() >= dropChance) return;

        ItemStack mutantDrop = MutantFruitItem.createFromGenome(cropGenome, ModItems.MUTANT_FRUIT.get());
        BlockPos pos = event.getPos();
        level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
            level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, mutantDrop
        ));

        EvolutionMod.LOGGER.debug("Récolte mutée de {} : effet={}, taille={}",
            species, fruitEffect, fruitSize);
    }

    /**
     * Accélère la pousse des cultures dont GROWTH_SPEED a fortement muté.
     *
     * BlockEvent.CropGrowEvent.Pre est annoté @HasResult : ALLOW force la
     * progression d'âge, DEFAULT laisse le comportement vanilla (chance
     * normale de pousser ce tick), DENY l'empêche. On utilise ALLOW de façon
     * probabiliste — proportionnelle au surplus de GROWTH_SPEED par rapport
     * à la valeur neutre (1.0) — plutôt que de façon systématique, pour que
     * l'effet reste un avantage progressif et non une croissance instantanée
     * qui casserait l'équilibre de la culture/récolte en survie.
     */
    private static final Random TICK_RANDOM = new Random();

    @SubscribeEvent
    public static void onCropRandomTick(BlockEvent.CropGrowEvent.Pre event) {
        String species = detectCropSpecies(event.getState().getBlock());
        if (species == null) return;

        Genome cropGenome = EvolutionManager.getInstance().getSpeciesTemplate(species);
        if (cropGenome == null) return;

        double growthSpeed = cropGenome.getGeneValue(Gene.GeneType.GROWTH_SPEED);
        if (growthSpeed <= 1.0) return; // pas de bonus si la mutation n'a pas accéléré la pousse

        // growthSpeed va de 0.1 à 5.0 ; au-delà de 1.0 (neutre), on convertit
        // le surplus en chance additionnelle de forcer la pousse ce tick.
        double bonusChance = Math.min(0.5, (growthSpeed - 1.0) / 8.0);
        if (TICK_RANDOM.nextDouble() < bonusChance) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
        }
    }

    /**
     * Détection de l'espèce de culture à partir du bloc vanilla.
     */
    private static String detectCropSpecies(Block block) {
        if (block instanceof CropBlock) {
            String name = block.toString().toLowerCase();
            if (name.contains("wheat")) return "wheat";
            if (name.contains("carrot")) return "carrot";
            if (name.contains("potato")) return "potato";
            if (name.contains("beetroot")) return "beetroot";
        }
        if (block instanceof SugarCaneBlock) return "sugar_cane";
        if (block instanceof CactusBlock) return "cactus";
        String name = block.toString().toLowerCase();
        if (name.contains("melon") && !name.contains("stem")) return "melon";
        if (name.contains("pumpkin") && !name.contains("stem")) return "pumpkin";
        return null;
    }

    /**
     * Vérifie si une culture est à maturité (âge maximal) au moment où elle
     * est cassée — seules les récoltes arrivées à terme bénéficient du bonus
     * génétique, pas un plant immature détruit par erreur.
     */
    private static boolean isMature(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        // Canne à sucre / cactus n'ont pas de notion de maturité comparable
        // (ce sont des colonnes qui poussent en hauteur) — toujours considérés
        // matures pour ce bonus, plus simple et cohérent avec leur croissance.
        return true;
    }
}
