package com.evolutionmod.network;

import com.evolutionmod.genetics.GeneticEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Paquet pour diffuser les événements évolutifs importants à tous les clients.
 * Utilisé pour les speciations et mutations radicales.
 *
 * NOTE 1.21.1 : voir SyncGenomePacket pour le détail du changement d'API
 * réseau (NetworkEvent → CustomPayloadEvent.Context, plus de Supplier
 * autour du contexte, enqueueWork/setPacketHandled gérés par consumerMainThread).
 */
public class EvolutionEventPacket {

    private final GeneticEvent.EventType eventType;
    private final String entityName;
    private final String lineage;
    private final long day;

    public EvolutionEventPacket(GeneticEvent.EventType type, String entityName, String lineage, long day) {
        this.eventType = type;
        this.entityName = entityName;
        this.lineage = lineage;
        this.day = day;
    }

    public static void encode(EvolutionEventPacket packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.eventType);
        buf.writeUtf(packet.entityName);
        buf.writeUtf(packet.lineage);
        buf.writeLong(packet.day);
    }

    public static EvolutionEventPacket decode(FriendlyByteBuf buf) {
        return new EvolutionEventPacket(
            buf.readEnum(GeneticEvent.EventType.class),
            buf.readUtf(),
            buf.readUtf(),
            buf.readLong()
        );
    }

    public static void handle(EvolutionEventPacket packet, CustomPayloadEvent.Context context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && packet.eventType == GeneticEvent.EventType.SPECIATION) {
            String msg = String.format(
                "§6§l[ÉVOLUTION]§r §eNouvelle espèce '%s' issue d'un %s ! §7(Jour %d)",
                packet.lineage, packet.entityName, packet.day
            );
            mc.player.displayClientMessage(Component.literal(msg), false);
        }
    }
}
