package net.minecraftforge.network.simple.handler;

import java.util.function.BiConsumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.simple.SimpleBuildable;

public interface SimpleHandlerFlow<BUF extends FriendlyByteBuf, BASE> extends SimpleHandlerProtocol<BUF, BASE>, SimpleBuildable {
   <MSG extends BASE> SimpleHandlerFlow<BUF, BASE> add(Class<MSG> var1, StreamCodec<BUF, MSG> var2);

   <MSG extends BASE> SimpleHandlerFlow<BUF, BASE> addMain(Class<MSG> var1, StreamCodec<BUF, MSG> var2);

   <MSG extends BASE> SimpleHandlerFlow<BUF, BASE> add(Class<MSG> var1, StreamCodec<BUF, MSG> var2, BiConsumer<MSG, CustomPayloadEvent.Context> var3);
}
