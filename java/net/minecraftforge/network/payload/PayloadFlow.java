package net.minecraftforge.network.payload;

import java.util.function.BiConsumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuildable;

public interface PayloadFlow<BUF extends FriendlyByteBuf, BASE extends CustomPacketPayload>
   extends PayloadProtocol<BUF, BASE>,
   ChannelBuildable<CustomPacketPayload> {
   <MSG extends BASE> PayloadFlow<BUF, BASE> add(
      CustomPacketPayload.Type<MSG> var1, StreamCodec<BUF, MSG> var2, BiConsumer<MSG, CustomPayloadEvent.Context> var3
   );

   default <MSG extends BASE> PayloadFlow<BUF, BASE> addMain(
      CustomPacketPayload.Type<MSG> type, StreamCodec<BUF, MSG> codec, BiConsumer<MSG, CustomPayloadEvent.Context> handler
   ) {
      return this.add(type, codec, (msg, ctx) -> {
         ctx.enqueueWork(() -> handler.accept(msg, ctx));
         ctx.setPacketHandled(true);
      });
   }
}
