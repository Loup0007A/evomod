package com.evolutionmod.core;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.items.EvolutionScannerItem;
import com.evolutionmod.network.EvoNetwork;
import com.evolutionmod.network.JournalDataMsg;
import com.evolutionmod.world.EvolutionSavedData;
import com.evolutionmod.world.HistoryEntry;
import com.evolutionmod.world.SpeciesRecord;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

/**
 * /evolution                 ouvre le Journal d'évolution
 * /evolution inspect [cible] analyse la créature visée (ou la cible)
 * /evolution species         classement des espèces dans le chat
 * /evolution history         derniers événements
 * /evolution cycle [n]       (op) force n cycles d'évolution
 * /evolution reset           (op) efface toute l'histoire évolutive du monde
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID)
public final class EvolutionCommand {

    private EvolutionCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("evolution")
            .executes(EvolutionCommand::journal)
            .then(Commands.literal("journal").executes(EvolutionCommand::journal))
            .then(Commands.literal("status").executes(EvolutionCommand::journal))
            .then(Commands.literal("inspect")
                .executes(ctx -> inspect(ctx, null))
                .then(Commands.argument("target", EntityArgument.entity())
                    .executes(ctx -> inspect(ctx, EntityArgument.getEntity(ctx, "target")))))
            .then(Commands.literal("species").executes(EvolutionCommand::species))
            .then(Commands.literal("history").executes(EvolutionCommand::history))
            .then(Commands.literal("cycle")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> cycle(ctx, 1))
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                    .executes(ctx -> cycle(ctx, IntegerArgumentType.getInteger(ctx, "count")))))
            .then(Commands.literal("reset")
                .requires(s -> s.hasPermission(2))
                .executes(EvolutionCommand::reset)));
    }

    private static int journal(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        EvoNetwork.sendToPlayer(player, new JournalDataMsg(EvolutionManager.buildJournal(player)));
        return 1;
    }

    private static int inspect(CommandContext<CommandSourceStack> ctx, Entity explicit) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Entity target = explicit != null ? explicit : lookedAt(player);
        if (!(target instanceof LivingEntity living)) {
            ctx.getSource().sendFailure(Component.translatable("evolutionmod.command.no_target"));
            return 0;
        }
        return EvolutionScannerItem.scan(player, living) ? 1 : 0;
    }

    private static Entity lookedAt(ServerPlayer player) {
        double reach = 24.0;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(reach));
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(reach)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
            e -> e instanceof LivingEntity && !e.isSpectator(), reach * reach);
        return hit == null ? null : hit.getEntity();
    }

    private static int species(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        EvolutionSavedData data = EvolutionSavedData.get(server);
        List<SpeciesRecord> list = data.species().values().stream()
            .filter(r -> !r.id().equals("minecraft:player"))
            .sorted(Comparator.comparingDouble(SpeciesRecord::divergence).reversed())
            .limit(12).toList();
        ctx.getSource().sendSuccess(() -> Component.translatable("evolutionmod.command.species_header",
            EvolutionManager.currentDay(server), data.cycles()).withStyle(ChatFormatting.GOLD), false);
        for (SpeciesRecord r : list) {
            MutableComponent line = Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
                .append(EvolutionManager.speciesComponent(r.id()).copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(String.format("  %.1f%%", r.divergence() * 100)).withStyle(ChatFormatting.WHITE));
            if (!r.currentLineage().isEmpty()) {
                line.append(Component.literal("  → " + r.currentLineage()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            ctx.getSource().sendSuccess(() -> line, false);
        }
        return list.size();
    }

    private static int history(CommandContext<CommandSourceStack> ctx) {
        EvolutionSavedData data = EvolutionSavedData.get(ctx.getSource().getServer());
        List<HistoryEntry> h = data.history();
        ctx.getSource().sendSuccess(() -> Component.translatable("evolutionmod.command.history_header")
            .withStyle(ChatFormatting.GOLD), false);
        if (h.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("evolutionmod.journal.history.empty")
                .withStyle(ChatFormatting.GRAY), false);
        }
        for (int i = Math.max(0, h.size() - 10); i < h.size(); i++) {
            HistoryEntry e = h.get(i);
            Component line = Component.translatable("evolutionmod.journal.day", e.day())
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(" "))
                .append(historyText(e));
            ctx.getSource().sendSuccess(() -> line, false);
        }
        return h.size();
    }

    public static Component historyText(HistoryEntry e) {
        Component species = EvolutionManager.speciesComponent(e.species());
        return switch (e.type()) {
            case SPECIATION -> Component.translatable("evolutionmod.history.speciation", species,
                Component.literal(e.detail()).withStyle(ChatFormatting.LIGHT_PURPLE));
            case NEW_SPECIES -> Component.translatable("evolutionmod.history.new_species", species);
            default -> Component.translatable("evolutionmod.history.generic", species, e.detail());
        };
    }

    private static int cycle(CommandContext<CommandSourceStack> ctx, int count) {
        MinecraftServer server = ctx.getSource().getServer();
        long day = EvolutionManager.currentDay(server);
        for (int i = 0; i < count; i++) {
            EvolutionManager.runCycle(server, day);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("evolutionmod.command.cycled", count)
            .withStyle(ChatFormatting.GREEN), true);
        return count;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        EvolutionSavedData.get(ctx.getSource().getServer()).reset();
        ctx.getSource().sendSuccess(() -> Component.translatable("evolutionmod.command.reset")
            .withStyle(ChatFormatting.RED), true);
        return 1;
    }
}
