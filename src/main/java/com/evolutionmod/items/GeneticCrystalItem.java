package com.evolutionmod.items;

import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.genetics.Gene;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.util.ItemNbt;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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

/**
 * Cristal Génomique — artefact central du mod.
 *
 * Usages :
 *  [CLIC DROIT sur mob]  → Séquencer : lire le génome du mob dans le cristal
 *  [CLIC DROIT + SHIFT]  → Injecter : transférer les gènes du cristal vers un mob
 *
 * Tiers selon les mutations stockées :
 *   0-10  mut  → Cristal Brut
 *   11-30 mut  → Cristal Évolué
 *   31-60 mut  → Cristal Muté
 *   60+   mut  → Cristal Apex
 *
 * NOTE 1.21.1 : getOrCreateTag()/getTag()/hasTag()/setHoverName()/resetHoverName()
 * ont été retirés d'ItemStack (remplacés par les DataComponents). On passe par
 * l'utilitaire ItemNbt pour le NBT libre, et DataComponents.CUSTOM_NAME pour le nom.
 */
public class GeneticCrystalItem extends Item {

    public GeneticCrystalItem() {
        super(new Item.Properties().stacksTo(1).fireResistant());
    }

    // ──────────────────────────────────────────────────────
    //  Interaction avec une entité vivante
    // ──────────────────────────────────────────────────────

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            return injectGenes(stack, player, target);
        } else {
            return sequenceGenome(stack, player, target);
        }
    }

    /**
     * Lit le génome de l'entité cible et le stocke dans le cristal.
     */
    private InteractionResult sequenceGenome(ItemStack stack, Player player, LivingEntity target) {
        var result = new InteractionResult[]{ InteractionResult.FAIL };

        EvolutionCapability.getEvolutionData(target).ifPresent(evoData -> {
            Genome genome = evoData.getGenome();
            if (genome == null) {
                player.sendSystemMessage(Component.literal("§cCette entité n'a pas de génome détectable."));
                result[0] = InteractionResult.FAIL;
                return;
            }

            ItemNbt.update(stack, tag -> {
                tag.put("StoredGenome", genome.toNBT());
                tag.putString("SourceEntity", target.getType().getDescriptionId());
                tag.putInt("SequencedMutations", genome.getTotalMutations());
            });

            String tier = getTierName(genome.getTotalMutations());
            String[] lineageParts = genome.getSpeciesLineage().split(">");
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(
                tier + " §f[" + lineageParts[lineageParts.length - 1] + "]"
            ));

            player.sendSystemMessage(Component.literal(
                "§6[Séquençage] §eGénome de §f" + target.getType().getDescriptionId() +
                " §ecapturé ! §7(" + genome.getTotalMutations() + " mutations, lignée: " +
                genome.getSpeciesLineage() + ")"
            ));

            spawnSequencingParticles(target);
            result[0] = InteractionResult.SUCCESS;
        });

        return result[0];
    }

    /**
     * Injecte les gènes stockés dans l'entité cible.
     * Crée un transfert horizontal de gènes (HGT) — phénomène réel en biologie !
     */
    private InteractionResult injectGenes(ItemStack stack, Player player, LivingEntity target) {
        if (!ItemNbt.hasKey(stack, "StoredGenome")) {
            player.sendSystemMessage(Component.literal("§cLe cristal est vide. Séquencez d'abord un génome."));
            return InteractionResult.FAIL;
        }

        CompoundTag tag = ItemNbt.get(stack);
        Genome storedGenome = Genome.fromNBT(tag.getCompound("StoredGenome"));

        EvolutionCapability.getEvolutionData(target).ifPresent(evoData -> {
            Genome targetGenome = evoData.getGenome();
            if (targetGenome == null) return;

            Genome hybridGenome = targetGenome.reproduceWith(storedGenome,
                    target.level().getDayTime() / 24000L);

            evoData.setGenome(hybridGenome);

            com.evolutionmod.core.EvolutionManager.getInstance()
                    .applyGenomeMutations(target, hybridGenome);

            player.sendSystemMessage(Component.literal(
                "§5[Injection] §dTransfert génétique horizontal appliqué ! " +
                "§7Nouvelle lignée: §f" + hybridGenome.getSpeciesLineage()
            ));

            target.level().broadcastEntityEvent(target, (byte) 20);
        });

        // Le cristal se vide à 50% de chance (usage consomme)
        if (Math.random() < 0.5) {
            ItemNbt.removeKey(stack, "StoredGenome");
            stack.remove(DataComponents.CUSTOM_NAME);
            player.sendSystemMessage(Component.literal("§7Le cristal s'est vidé lors de l'injection."));
        }

        return InteractionResult.SUCCESS;
    }

    // ──────────────────────────────────────────────────────
    //  Tooltip / Affichage
    // ──────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                 List<Component> tooltip, TooltipFlag flag) {
        if (!ItemNbt.hasKey(stack, "StoredGenome")) {
            tooltip.add(Component.literal("§7Vide — Clic droit sur un mob pour séquencer"));
            tooltip.add(Component.literal("§8Shift + Clic droit → Injecter le génome"));
            return;
        }

        CompoundTag tag = ItemNbt.get(stack);
        Genome genome = Genome.fromNBT(tag.getCompound("StoredGenome"));
        String source = tag.getString("SourceEntity");

        tooltip.add(Component.literal("§7Source: §f" + source));
        tooltip.add(Component.literal("§7Lignée: §f" + genome.getSpeciesLineage()));
        tooltip.add(Component.literal("§7Mutations: §f" + genome.getTotalMutations()));
        tooltip.add(Component.literal("§7Génération: §f" + calcMaxGeneration(genome)));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§eTOP 3 gènes exprimés:"));

        genome.getAllGenes().entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().getGeneration(), a.getValue().getGeneration()))
            .limit(3)
            .forEach(entry -> tooltip.add(Component.literal(
                "  §6" + entry.getKey().name() + " §7= §f" +
                String.format("%.2f", entry.getValue().getValue()) +
                " §8(gen " + entry.getValue().getGeneration() + ")"
            )));

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§8Shift + Clic droit → Injecter dans un mob"));
    }

    // ──────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────

    private String getTierName(int mutations) {
        if (mutations >= 60) return "§5§l✦ Cristal Apex";
        if (mutations >= 31) return "§4§lCristal Muté";
        if (mutations >= 11) return "§d§lCristal Évolué";
        return "§9Cristal Brut";
    }

    private int calcMaxGeneration(Genome genome) {
        return genome.getAllGenes().values().stream()
                .mapToInt(Gene::getGeneration)
                .max().orElse(0);
    }

    private void spawnSequencingParticles(LivingEntity entity) {
        if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.PORTAL,
                entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                20, 0.5, 0.5, 0.5, 0.05
            );
        }
    }
}
