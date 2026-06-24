package com.evolutionmod.network;

import com.evolutionmod.EvolutionMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

/**
 * Canal réseau pour synchroniser les données génétiques client/serveur.
 *
 * NOTE 1.21.1 (vérifié directement dans le code source Forge, branche 1.21.1) :
 * - SimpleChannel vit dans net.minecraftforge.network (PAS .network.simple).
 * - NetworkRegistry#newSimpleChannel n'existe plus du tout.
 * - Le point d'entrée moderne est ChannelBuilder.named(rl)
 *   .networkProtocolVersion(v).simpleChannel().
 * - Chaque message s'enregistre via channel.messageBuilder(Class).encoder(...)
 *   .decoder(...).consumerMainThread(...).add() (API fluide, plus de
 *   registerMessage(id, class, encoder, decoder, handler) à 5 arguments).
 * - Le contexte de handler est CustomPayloadEvent.Context (NetworkEvent a
 *   été entièrement supprimé), avec les mêmes méthodes utiles : isClientSide(),
 *   enqueueWork(Runnable), setPacketHandled(boolean), getSender().
 */
public class EvolutionNetwork {

    private static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel CHANNEL = ChannelBuilder
        .named(ResourceLocation.fromNamespaceAndPath(EvolutionMod.MOD_ID, "evolution_sync"))
        .networkProtocolVersion(PROTOCOL_VERSION)
        .simpleChannel();

    public static void register() {
        CHANNEL.messageBuilder(SyncGenomePacket.class)
            .encoder(SyncGenomePacket::encode)
            .decoder(SyncGenomePacket::decode)
            .consumerMainThread(SyncGenomePacket::handle)
            .add();

        CHANNEL.messageBuilder(EvolutionEventPacket.class)
            .encoder(EvolutionEventPacket::encode)
            .decoder(EvolutionEventPacket::decode)
            .consumerMainThread(EvolutionEventPacket::handle)
            .add();

        CHANNEL.messageBuilder(EvolutionStatusPacket.class)
            .encoder(EvolutionStatusPacket::encode)
            .decoder(EvolutionStatusPacket::decode)
            .consumerMainThread(EvolutionStatusPacket::handle)
            .add();

        EvolutionMod.LOGGER.info("Réseau d'évolution enregistré (channel: {})", EvolutionMod.MOD_ID);
    }
}
