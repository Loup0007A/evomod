package com.evolutionmod.network;

import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.world.EvolutionSavedData;
import com.evolutionmod.world.HistoryEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client → serveur : bouton « Forcer un cycle » du journal (opérateurs uniquement). */
public record ForceCycleMsg(int count) {

    public static void encode(ForceCycleMsg msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.count);
    }

    public static ForceCycleMsg decode(FriendlyByteBuf buf) {
        return new ForceCycleMsg(buf.readVarInt());
    }

    public static void handle(ForceCycleMsg msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null && EvolutionManager.canForce(player)) {
            int n = Math.max(1, Math.min(10, msg.count));
            var server = player.getServer();
            for (int i = 0; i < n; i++) EvolutionManager.runCycle(server, EvolutionManager.currentDay(server));
            EvolutionSavedData.get(server).addHistory(new HistoryEntry(EvolutionManager.currentDay(server),
                HistoryEntry.Type.FORCED, "minecraft:player", player.getGameProfile().getName(), n));
            EvoNetwork.sendToPlayer(player, new JournalDataMsg(EvolutionManager.buildJournal(player)));
        }
        ctx.setPacketHandled(true);
    }
}
