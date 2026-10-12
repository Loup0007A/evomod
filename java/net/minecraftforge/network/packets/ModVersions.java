package net.minecraftforge.network.packets;

import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

public record ModVersions(Map<String, ModVersions.Info> mods) {
   public static final StreamCodec<FriendlyByteBuf, ModVersions> STREAM_CODEC = StreamCodec.ofMember(ModVersions::encode, ModVersions::decode);
   private static final int MAX_LENGTH = 256;

   public static ModVersions create() {
      return new ModVersions(
         ModList.get()
            .getMods()
            .stream()
            .collect(Collectors.toMap(IModInfo::getModId, mod -> new ModVersions.Info(mod.getDisplayName(), mod.getVersion().toString())))
      );
   }

   public static ModVersions decode(FriendlyByteBuf buf) {
      return new ModVersions(buf.readMap(o -> o.readUtf(256), o -> new ModVersions.Info(o.readUtf(256), o.readUtf(256))));
   }

   public void encode(FriendlyByteBuf output) {
      output.writeMap(this.mods, (o, s) -> o.writeUtf(s, 256), (o, p) -> {
         o.writeUtf(p.name(), 256);
         o.writeUtf(p.version(), 256);
      });
   }

   public static record Info(String name, String version) {
   }
}
