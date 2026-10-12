package net.minecraftforge.network.simple;

import java.util.function.BiConsumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;

public interface SimpleFlow<BUF extends FriendlyByteBuf, BASE> extends SimpleProtocol<BUF, BASE>, SimpleBuildable {
   <MSG extends BASE> SimpleFlow<BUF, BASE> add(Class<MSG> var1, StreamCodec<BUF, MSG> var2, BiConsumer<MSG, CustomPayloadEvent.Context> var3);

   default <MSG extends BASE> SimpleFlow<BUF, BASE> addMain(Class<MSG> type, StreamCodec<BUF, MSG> codec, BiConsumer<MSG, CustomPayloadEvent.Context> handler) {
      return this.add(type, codec, (msg, ctx) -> {
         ctx.enqueueWork(() -> handler.accept(msg, ctx));
         ctx.setPacketHandled(true);
      });
   }
}
