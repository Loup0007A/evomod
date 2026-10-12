package net.minecraftforge.network.packets;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.Channel;

public class LoginWrapper {
   public static final StreamCodec<FriendlyByteBuf, LoginWrapper> STREAM_CODEC = StreamCodec.ofMember(LoginWrapper::encode, LoginWrapper::new);
   private final ResourceLocation name;
   private FriendlyByteBuf data;
   private final Channel<Object> channel;
   private final Object packet;

   public <MSG> LoginWrapper(Channel<MSG> channel, MSG packet) {
      this(channel.getName(), null, channel, packet);
   }

   private LoginWrapper(FriendlyByteBuf buf) {
      this(buf.readResourceLocation(), buf.wrap(buf.readBytes(buf.readVarInt())), null, null);
   }

   private LoginWrapper(ResourceLocation name, FriendlyByteBuf data, Channel<?> channel, Object packet) {
      this.name = name;
      this.data = data;
      this.channel = (Channel<Object>)channel;
      this.packet = packet;
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeResourceLocation(this.name);
      if (this.data == null) {
         this.data = buf.wrap(Unpooled.buffer());
         this.channel.encode(this.data, this.packet);
      }

      buf.writeVarInt(this.data.readableBytes());
      buf.writeBytes(this.data.slice());
   }

   public ResourceLocation name() {
      return this.name;
   }

   public FriendlyByteBuf data() {
      return this.data;
   }
}
