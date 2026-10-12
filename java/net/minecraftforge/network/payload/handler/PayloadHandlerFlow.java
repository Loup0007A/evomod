package net.minecraftforge.network.payload.handler;

import java.util.function.BiConsumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuildable;

public interface PayloadHandlerFlow<BUF extends FriendlyByteBuf, BASE extends CustomPacketPayload>
   extends PayloadHandlerProtocol<BUF, BASE>,
   ChannelBuildable<CustomPacketPayload> {
   <MSG extends BASE> PayloadHandlerFlow<BUF, BASE> add(CustomPacketPayload.Type<MSG> var1, StreamCodec<BUF, MSG> var2);

   <MSG extends BASE> PayloadHandlerFlow<BUF, BASE> addMain(CustomPacketPayload.Type<MSG> var1, StreamCodec<BUF, MSG> var2);

   <MSG extends BASE> PayloadHandlerFlow<BUF, BASE> add(
      CustomPacketPayload.Type<MSG> var1, StreamCodec<BUF, MSG> var2, BiConsumer<MSG, CustomPayloadEvent.Context> var3
   );
}
