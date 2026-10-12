package com.evolutionmod.network;

import com.evolutionmod.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Serveur → client : génome d'une entité (tag vide = aucun génome). */
public record SyncGenomeMsg(int entityId, CompoundTag genome) {

    public static void encode(SyncGenomeMsg msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeNbt(msg.genome);
    }

    public static SyncGenomeMsg decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        CompoundTag tag = buf.readNbt();
        return new SyncGenomeMsg(id, tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncGenomeMsg msg, CustomPayloadEvent.Context ctx) {
        if (FMLEnvironment.dist.isClient()) ClientPacketHandler.onSyncGenome(msg);
        ctx.setPacketHandled(true);
    }
}
