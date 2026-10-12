package com.evolutionmod.network;

import com.evolutionmod.EvolutionMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * Canal réseau du mod. Les gestionnaires « côté client » sont dans des classes
 * séparées ({@code client.ClientPacketHandler}) chargées uniquement sur le
 * client, pour que le serveur dédié ne plante jamais.
 */
public final class EvoNetwork {

    private EvoNetwork() {}

    private static final int PROTOCOL = 3;

    public static final SimpleChannel CHANNEL = ChannelBuilder
        .named(EvolutionMod.id("main"))
        .networkProtocolVersion(PROTOCOL)
        .simpleChannel();

    public static void register() {
        CHANNEL.messageBuilder(SyncGenomeMsg.class, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(SyncGenomeMsg::encode).decoder(SyncGenomeMsg::decode)
            .consumerMainThread(SyncGenomeMsg::handle).add();
        CHANNEL.messageBuilder(JournalDataMsg.class, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(JournalDataMsg::encode).decoder(JournalDataMsg::decode)
            .consumerMainThread(JournalDataMsg::handle).add();
        CHANNEL.messageBuilder(ScanResultMsg.class, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(ScanResultMsg::encode).decoder(ScanResultMsg::decode)
            .consumerMainThread(ScanResultMsg::handle).add();
        CHANNEL.messageBuilder(NotifyMsg.class, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(NotifyMsg::encode).decoder(NotifyMsg::decode)
            .consumerMainThread(NotifyMsg::handle).add();
        CHANNEL.messageBuilder(ForceCycleMsg.class, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ForceCycleMsg::encode).decoder(ForceCycleMsg::decode)
            .consumerMainThread(ForceCycleMsg::handle).add();
        CHANNEL.messageBuilder(RequestJournalMsg.class, NetworkDirection.PLAY_TO_SERVER)
            .encoder(RequestJournalMsg::encode).decoder(RequestJournalMsg::decode)
            .consumerMainThread(RequestJournalMsg::handle).add();
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        CHANNEL.send(msg, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToTrackingAndSelf(Entity entity, Object msg) {
        CHANNEL.send(msg, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(entity));
    }

    public static void sendToAll(MinecraftServer server, Object msg) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sendToPlayer(p, msg);
    }

    public static void sendToServer(Object msg) {
        CHANNEL.send(msg, PacketDistributor.SERVER.noArg());
    }
}
