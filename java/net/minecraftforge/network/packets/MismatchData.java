package net.minecraftforge.network.packets;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkContext;

public record MismatchData(Map<ResourceLocation, NetworkContext.NetworkMismatchData.Version> mismatched, Set<ResourceLocation> missing) {
   public static final StreamCodec<FriendlyByteBuf, MismatchData> STREAM_CODEC = StreamCodec.ofMember(MismatchData::encode, MismatchData::decode);
   private static final int MAX_LENGTH = 256;

   public MismatchData(NetworkContext.NetworkMismatchData data) {
      this(data.mismatched(), data.missing());
   }

   public static MismatchData decode(FriendlyByteBuf buf) {
      Map<ResourceLocation, NetworkContext.NetworkMismatchData.Version> mismatched = buf.readMap(
         i -> ResourceLocation.parse(i.readUtf(256)), i -> new NetworkContext.NetworkMismatchData.Version(i.readUtf(256), i.readUtf(256))
      );
      HashSet<ResourceLocation> missing = buf.readCollection(HashSet::new, i -> ResourceLocation.parse(i.readUtf(256)));
      return new MismatchData(mismatched, missing);
   }

   public void encode(FriendlyByteBuf buf) {
      buf.writeMap(this.mismatched, (o, k) -> o.writeUtf(k.toString(), 256), (o, v) -> {
         o.writeUtf(v.received(), 256);
         o.writeUtf(v.had(), 256);
      });
      buf.writeCollection(this.missing, (o, k) -> o.writeUtf(k.toString(), 256));
   }
}
