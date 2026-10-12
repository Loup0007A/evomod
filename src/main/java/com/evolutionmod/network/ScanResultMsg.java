package com.evolutionmod.network;

import com.evolutionmod.client.ClientPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Serveur → client : résultat d'un scan (ouvre l'écran d'analyse). */
public record ScanResultMsg(int entityId, CompoundTag genome, CompoundTag species) {

    public static void encode(ScanResultMsg msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeNbt(msg.genome);
        buf.writeNbt(msg.species);
    }

    public static ScanResultMsg decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        CompoundTag g = buf.readNbt();
        CompoundTag s = buf.readNbt();
        return new ScanResultMsg(id, g == null ? new CompoundTag() : g, s == null ? new CompoundTag() : s);
    }

    public static void handle(ScanResultMsg msg, CustomPayloadEvent.Context ctx) {
        if (FMLEnvironment.dist.isClient()) ClientPacketHandler.onScan(msg);
        ctx.setPacketHandled(true);
    }
}
