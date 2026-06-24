package com.evolutionmod.items;

import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.genetics.Gene;
import com.evolutionmod.genetics.Genome;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Scanner d'Évolution — appareil portable de diagnostic génétique.
 *
 * Contrairement au Cristal Génomique (qui capture et transfère),
 * le Scanner ne fait que LIRE et AFFICHER sans rien modifier :
 *  - Génome complet d'un mob visé
 *  - Comparaison avec le génome "template" de son espèce
 *  - Estimation du fitness score actuel
 *  - Distance évolutive par rapport à l'ancêtre original
 *
 * Outil de diagnostic non-destructif, idéal pour suivre l'évolution sans interférer.
 */
public class EvolutionScannerItem extends Item {

    public EvolutionScannerItem() {
        super(new Item.Properties().stacksTo(1).durability(64));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;

        EvolutionCapability.getEvolutionData(target).ifPresent(evoData -> {
            Genome genome = evoData.getGenome();
            if (genome == null) {
                player.sendSystemMessage(Component.literal("§7[Scanner] Aucune donnée génétique détectée."));
                return;
            }

            displayFullScan(player, target, genome);
            // NOTE 1.21.1 : le compilateur (avec accès au vrai jar Minecraft) confirme
            // que l'overload réellement disponible ici est
            // hurtAndBreak(int, LivingEntity, EquipmentSlot) — pas de Consumer<Item>.
            // La javadoc Forge mentionnant Consumer documentait apparemment un autre
            // overload (visible côté ItemStack côté vanilla mais pas résolu ici, peut-être
            // réservé à un contexte différent) ; on se fie ici au compilateur, qui a
            // une vue directe sur le jar réel utilisé au build, plutôt qu'à la doc.
            stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        });

        return InteractionResult.SUCCESS;
    }

    /**
     * Affiche un scan complet et formaté dans le chat du joueur.
     */
    private void displayFullScan(Player player, LivingEntity target, Genome genome) {
        StringBuilder sb = new StringBuilder();
        String entityName = target.getType().getDescriptionId()
                .replace("entity.minecraft.", "");

        sb.append("\n§b§l╔══ SCAN GÉNÉTIQUE ══╗\n");
        sb.append("§b║ §fEspèce: §e").append(entityName).append("\n");
        sb.append("§b║ §fLignée: §e").append(genome.getSpeciesLineage()).append("\n");
        sb.append("§b║ §fMutations: §e").append(genome.getTotalMutations()).append("\n");

        // Distance évolutive : comparaison avec le template d'espèce
        Genome template = EvolutionManager.getInstance().getSpeciesTemplate(
                genome.getSpeciesLineage().split(">")[0]);

        if (template != null) {
            double divergence = calculateDivergence(genome, template);
            String divergenceBar = buildBar(divergence, 10);
            sb.append("§b║ §fDivergence: §e").append(divergenceBar)
              .append(" §7").append(String.format("%.0f%%", divergence * 100)).append("\n");
        }

        sb.append("§b║ §fFitness: §e").append(String.format("%.2f", genome.getFitnessScore())).append("\n");
        sb.append("§b╠═══ Gènes Dominants ═══╣\n");

        // Top 5 gènes les plus mutés (par génération)
        genome.getAllGenes().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getGeneration(), a.getValue().getGeneration()))
                .limit(5)
                .forEach(entry -> {
                    Gene g = entry.getValue();
                    String tag = g.getGeneration() > 8 ? "§c⚠" : g.getGeneration() > 3 ? "§e●" : "§a○";
                    sb.append("§b║ ").append(tag).append(" §7")
                      .append(padRight(g.getType().name(), 16))
                      .append("§f").append(String.format("%6.2f", g.getValue()))
                      .append(" §8gen.").append(g.getGeneration()).append("\n");
                });

        sb.append("§b╚═══════════════════════╝");

        player.sendSystemMessage(Component.literal(sb.toString()));

        // Avertissement si l'entité est en zone de risque d'extinction
        if (genome.getFitnessScore() < 0.3) {
            player.sendSystemMessage(Component.literal(
                "§4⚠ Cette créature semble mal adaptée à son environnement actuel."
            ));
        }
    }

    /**
     * Calcule un score de divergence (0=identique au template, 1=totalement différent).
     */
    private double calculateDivergence(Genome current, Genome template) {
        double totalDiff = 0;
        int count = 0;

        for (Map.Entry<Gene.GeneType, Gene> entry : current.getAllGenes().entrySet()) {
            Gene templateGene = template.getGene(entry.getKey());
            if (templateGene != null) {
                double[] bounds = Gene.getValueBounds(entry.getKey());
                double range = bounds[1] - bounds[0];
                if (range > 0) {
                    double diff = Math.abs(entry.getValue().getValue() - templateGene.getValue()) / range;
                    totalDiff += diff;
                    count++;
                }
            }
        }

        return count > 0 ? Math.min(1.0, totalDiff / count) : 0.0;
    }

    private String buildBar(double percent, int segments) {
        int filled = (int) Math.round(percent * segments);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < segments; i++) {
            bar.append(i < filled ? "§a█" : "§7░");
        }
        return bar.toString();
    }

    private String padRight(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        return s + " ".repeat(n - s.length());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Outil de diagnostic non-destructif"));
        tooltip.add(Component.literal("§7Clic droit sur un mob pour scanner son génome"));
        tooltip.add(Component.literal("§8Ne modifie rien — pur outil d'observation"));
    }
}
