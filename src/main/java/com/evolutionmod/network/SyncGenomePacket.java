package com.evolutionmod.network;

import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.genetics.Genome;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Paquet de synchronisation du génome d'une entité vers le client.
 * Envoyé lors du spawn et après chaque évolution.
 *
 * NOTE 1.21.1 (vérifié dans le code source Forge 1.21.1) :
 * NetworkEvent a été entièrement supprimé. Le handler reçoit directement
 * un CustomPayloadEvent.Context (pas un Supplier<Context>), et
 * consumerMainThread() s'occupe déjà lui-même de enqueueWork()/
 * setPacketHandled() en interne avant d'appeler ce callback — donc handle()
 * n'a plus besoin de le faire manuellement.
 */
public class SyncGenomePacket {

    private final int entityId;
    private final CompoundTag genomeNbt;

    public SyncGenomePacket(int entityId, Genome genome) {
        this.entityId = entityId;
        this.genomeNbt = genome.toNBT();
    }

    public SyncGenomePacket(int entityId, CompoundTag nbt) {
        this.entityId = entityId;
        this.genomeNbt = nbt;
    }

    public static void encode(SyncGenomePacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
        buf.writeNbt(packet.genomeNbt);
    }

    public static SyncGenomePacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        CompoundTag nbt = buf.readNbt();
        return new SyncGenomePacket(entityId, nbt);
    }

    public static void handle(SyncGenomePacket packet, CustomPayloadEvent.Context context) {
        // Déjà exécuté sur le thread principal grâce à consumerMainThread().
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(packet.entityId);
            if (entity instanceof LivingEntity livingEntity) {
                EvolutionCapability.getEvolutionData(livingEntity).ifPresent(evoData ->
                    evoData.setGenome(Genome.fromNBT(packet.genomeNbt))
                );
            }
        }
    }
}
