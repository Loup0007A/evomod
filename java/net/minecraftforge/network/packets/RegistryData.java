package net.minecraftforge.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;

public record RegistryData(int token, ResourceLocation name, ForgeRegistry.Snapshot data) {
   public static final StreamCodec<FriendlyByteBuf, RegistryData> STREAM_CODEC = StreamCodec.ofMember(RegistryData::encode, RegistryData::decode);

   public void encode(FriendlyByteBuf buf) {
      buf.writeVarInt(this.token);
      buf.writeResourceLocation(this.name);
      buf.writeBytes(this.data.getPacketData());
   }

   public static RegistryData decode(FriendlyByteBuf buf) {
      return new RegistryData(buf.readVarInt(), buf.readResourceLocation(), ForgeRegistry.Snapshot.read(buf));
   }
}
