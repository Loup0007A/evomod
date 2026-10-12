package net.minecraftforge.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record Acknowledge(int token) {
   public static StreamCodec<FriendlyByteBuf, Acknowledge> STREAM_CODEC = StreamCodec.ofMember(Acknowledge::encode, Acknowledge::decode);

   public void encode(FriendlyByteBuf buf) {
      buf.writeVarInt(this.token);
   }

   public static Acknowledge decode(FriendlyByteBuf buf) {
      return new Acknowledge(buf.readVarInt());
   }
}
