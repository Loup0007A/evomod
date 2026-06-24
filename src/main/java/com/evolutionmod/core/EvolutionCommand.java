package com.evolutionmod.core;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.genetics.Gene;
import com.evolutionmod.genetics.Genome;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Commandes pour inspecter et gérer l'évolution en jeu.
 *
 * /evolution status      - Statistiques globales d'évolution
 * /evolution inspect     - Génome de l'entité visée
 * /evolution accelerate  - Accélérer l'évolution (admin)
 * /evolution history     - Historique évolutif du monde
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID)
public class EvolutionCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("evolution")
                .then(Commands.literal("status")
                    .executes(EvolutionCommand::executeStatus))
                .then(Commands.literal("inspect")
                    .executes(EvolutionCommand::executeInspect))
                .then(Commands.literal("history")
                    .executes(EvolutionCommand::executeHistory))
                .then(Commands.literal("accelerate")
                    .requires(src -> src.hasPermission(2))
                    .executes(EvolutionCommand::executeAccelerate))
                .then(Commands.literal("genome")
                    .executes(EvolutionCommand::executeGenome))
        );
    }

    /**
     * Envoie un EvolutionStatusPacket au joueur pour ouvrir l'écran interactif
     * /evolution status, qui liste toutes les espèces évolvables (plantes,
     * mobs, et joueur) avec le détail complet de leurs gènes, cliquable.
     *
     * NOTE 1.21.1 (vérifié dans le code source Forge réel, Channel.java) :
     * la signature est send(MSG msg, PacketDistributor.PacketTarget target) —
     * le message en premier argument, pas la cible.
     */
    private static int executeStatus(CommandContext<CommandSourceStack> ctx) {
        EvolutionManager mgr = EvolutionManager.getInstance();
        CommandSourceStack source = ctx.getSource();

        try {
            net.minecraft.server.level.ServerPlayer player = source.getPlayerOrException();

            Map<String, net.minecraft.nbt.CompoundTag> speciesGenomes = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, Genome> entry : mgr.getAllEvolvableSpecies().entrySet()) {
                speciesGenomes.put(entry.getKey(), entry.getValue().toNBT());
            }

            com.evolutionmod.network.EvolutionStatusPacket packet = new com.evolutionmod.network.EvolutionStatusPacket(
                speciesGenomes,
                mgr.getCurrentDay(),
                mgr.getTotalMutationsWorldwide(),
                mgr.getSpeciationEvents(),
                mgr.getGlobalEnvironmentalPressure()
            );

            com.evolutionmod.network.EvolutionNetwork.CHANNEL.send(
                packet,
                net.minecraftforge.network.PacketDistributor.PLAYER.with(player)
            );

        } catch (Exception e) {
            // Source non-joueur (ex: console serveur) : on retombe sur le résumé texte,
            // l'écran graphique n'ayant aucun sens hors d'un client.
            source.sendSuccess(() -> Component.literal(
                "\n§6§l═══ ÉTAT DE L'ÉVOLUTION ═══\n" +
                "§eJour actuel: §f" + mgr.getCurrentDay() + "\n" +
                "§eMutations totales: §f" + mgr.getTotalMutationsWorldwide() + "\n" +
                "§eSpéciations: §f" + mgr.getSpeciationEvents() + "\n" +
                "§ePression environnementale: §f" + String.format("%.1f%%", mgr.getGlobalEnvironmentalPressure() * 100) + "\n" +
                "§eEspèces suivies: §f" + mgr.getAllEvolvableSpecies().size()
            ), false);
        }
        return 1;
    }

    private static int executeInspect(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        try {
            Player player = source.getPlayerOrException();

            // Trouver l'entité que le joueur regarde
            Entity target = getEntityPlayerLooksAt(player);

            if (!(target instanceof LivingEntity living)) {
                source.sendFailure(Component.literal("§cRegardez une entité vivante pour l'inspecter."));
                return 0;
            }

            EvolutionCapability.getEvolutionData(living).ifPresent(evoData -> {
                Genome genome = evoData.getGenome();
                if (genome == null) {
                    source.sendSuccess(() -> Component.literal("§7Cette entité n'a pas encore de données génétiques."), false);
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("\n§6§l═══ GÉNOME DE ").append(living.getType().getDescriptionId().toUpperCase()).append(" ═══\n");
                sb.append("§eLineage: §f").append(genome.getSpeciesLineage()).append("\n");
                sb.append("§eMutations accumulées: §f").append(genome.getTotalMutations()).append("\n");
                sb.append("§eFitness score: §f").append(String.format("%.3f", genome.getFitnessScore())).append("\n");
                sb.append("§7─────────── Gènes ───────────\n");

                for (Map.Entry<Gene.GeneType, Gene> entry : genome.getAllGenes().entrySet()) {
                    Gene gene = entry.getValue();
                    String barColor = gene.getGeneration() > 5 ? "§c" : gene.getGeneration() > 2 ? "§e" : "§a";
                    sb.append(barColor).append(gene.getType().name())
                      .append("§7: §f").append(String.format("%.3f", gene.getValue()))
                      .append(" §8(gen ").append(gene.getGeneration()).append(")\n");
                }

                String finalText = sb.toString();
                source.sendSuccess(() -> Component.literal(finalText), false);
            });

        } catch (Exception e) {
            source.sendFailure(Component.literal("§cErreur : " + e.getMessage()));
        }
        return 1;
    }

    private static int executeHistory(CommandContext<CommandSourceStack> ctx) {
        EvolutionManager mgr = EvolutionManager.getInstance();
        CommandSourceStack source = ctx.getSource();

        StringBuilder sb = new StringBuilder("\n§6§l═══ HISTORIQUE ÉVOLUTIF ═══\n");

        if (mgr.getSpeciesTemplates().isEmpty()) {
            sb.append("§7Aucune espèce suivie pour le moment...");
        } else {
            for (Map.Entry<String, Genome> entry : mgr.getSpeciesTemplates().entrySet()) {
                Genome genome = entry.getValue();
                sb.append("§e").append(entry.getKey()).append(":\n");
                sb.append("  §7Lignée: §f").append(genome.getSpeciesLineage()).append("\n");
                sb.append("  §7Mutations: §f").append(genome.getTotalMutations()).append("\n");
            }
        }

        sb.append("\n§6Total speciations: §f").append(mgr.getSpeciationEvents());

        String text = sb.toString();
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int executeAccelerate(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        // Déclencher manuellement 5 cycles d'évolution
        if (source.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            long currentDay = EvolutionManager.getInstance().getCurrentDay();
            for (int i = 1; i <= 5; i++) {
                EvolutionManager.getInstance().onDayChange(currentDay + i, serverLevel);
            }
            source.sendSuccess(() -> Component.literal(
                "§6[ÉVOLUTION] 5 cycles d'évolution accélérés appliqués !"
            ), true);
        }
        return 1;
    }

    private static int executeGenome(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        try {
            Player player = source.getPlayerOrException();
            source.sendSuccess(() -> Component.literal(
                "§7Note: Les joueurs n'évoluent pas encore...\n" +
                "§8Mais qui sait ce que l'avenir réserve... §7:D"
            ), false);
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cCette commande nécessite un joueur."));
        }
        return 1;
    }

    private static Entity getEntityPlayerLooksAt(Player player) {
        // Raytrace simple : entité dans la direction du regard à 5 blocs
        var level = player.level();
        var vec = player.getEyePosition();
        var look = player.getLookAngle();
        var end = vec.add(look.scale(5));

        var clip = level.clip(new net.minecraft.world.level.ClipContext(
            vec, end,
            net.minecraft.world.level.ClipContext.Block.OUTLINE,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            player
        ));

        var entityList = level.getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().expandTowards(look.scale(5)).inflate(1)
        );

        Entity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity e : entityList) {
            if (e == player) continue;
            double dist = e.distanceTo(player);
            if (dist < closestDist) {
                closestDist = dist;
                closest = e;
            }
        }
        return closest;
    }
}
