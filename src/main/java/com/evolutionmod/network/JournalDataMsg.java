package com.evolutionmod.network;

import com.evolutionmod.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Serveur → client : instantané du monde pour le Journal d'évolution. */
public record JournalDataMsg(CompoundTag data) {

    public static void encode(JournalDataMsg msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.data);
    }

    public static JournalDataMsg decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new JournalDataMsg(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(JournalDataMsg msg, CustomPayloadEvent.Context ctx) {
        if (FMLEnvironment.dist.isClient()) ClientPacketHandler.onJournal(msg);
        ctx.setPacketHandled(true);
    }
}
