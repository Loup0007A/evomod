package com.evolutionmod.network;

import com.evolutionmod.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Serveur → client : notification affichée dans le HUD.
 * Les arguments commençant par "#species:" ou "#gene:" sont traduits côté client.
 */
public record NotifyMsg(Kind kind, String key, List<String> args) {

    public enum Kind {
        DAILY(0xFF6FD3FF), SPECIATION(0xFFFFB347), PLAYER(0xFFFFE066), LAB(0xFF7BE07B), INFO(0xFFC58BFF),
        ADAPTATION(0xFF9BE05B), BREW(0xFFB98CFF), DANGER(0xFFFF5B5B);

        public final int color;

        Kind(int color) {
            this.color = color;
        }
    }

    public static void encode(NotifyMsg msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.kind);
        buf.writeUtf(msg.key);
        buf.writeVarInt(msg.args.size());
        for (String a : msg.args) buf.writeUtf(a);
    }

    public static NotifyMsg decode(FriendlyByteBuf buf) {
        Kind kind = buf.readEnum(Kind.class);
        String key = buf.readUtf();
        int n = buf.readVarInt();
        List<String> args = new ArrayList<>(n);
        for (int i = 0; i < n; i++) args.add(buf.readUtf());
        return new NotifyMsg(kind, key, args);
    }

    public static void handle(NotifyMsg msg, CustomPayloadEvent.Context ctx) {
        if (FMLEnvironment.dist.isClient()) ClientPacketHandler.onNotify(msg);
        ctx.setPacketHandled(true);
    }
}
