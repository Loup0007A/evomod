package com.evolutionmod.network;

import com.evolutionmod.core.EvolutionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client → serveur : demande l'ouverture (ou le rafraîchissement) du Journal. */
public record RequestJournalMsg() {

    public static void encode(RequestJournalMsg msg, FriendlyByteBuf buf) {}

    public static RequestJournalMsg decode(FriendlyByteBuf buf) {
        return new RequestJournalMsg();
    }

    public static void handle(RequestJournalMsg msg, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player != null) {
            EvoNetwork.sendToPlayer(player, new JournalDataMsg(EvolutionManager.buildJournal(player)));
        }
        ctx.setPacketHandled(true);
    }
}
