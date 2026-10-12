package net.minecraftforge.common.data;

import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public abstract class SpriteSourceProvider extends JsonCodecProvider<List<SpriteSource>> {
   protected static final ResourceLocation BLOCKS_ATLAS = ResourceLocation.withDefaultNamespace("blocks");
   protected static final ResourceLocation BANNER_PATTERNS_ATLAS = ResourceLocation.withDefaultNamespace("banner_patterns");
   protected static final ResourceLocation BEDS_ATLAS = ResourceLocation.withDefaultNamespace("beds");
   protected static final ResourceLocation CHESTS_ATLAS = ResourceLocation.withDefaultNamespace("chests");
   protected static final ResourceLocation SHIELD_PATTERNS_ATLAS = ResourceLocation.withDefaultNamespace("shield_patterns");
   protected static final ResourceLocation SHULKER_BOXES_ATLAS = ResourceLocation.withDefaultNamespace("shulker_boxes");
   protected static final ResourceLocation SIGNS_ATLAS = ResourceLocation.withDefaultNamespace("signs");
   protected static final ResourceLocation MOB_EFFECTS_ATLAS = ResourceLocation.withDefaultNamespace("mob_effects");
   protected static final ResourceLocation PAINTINGS_ATLAS = ResourceLocation.withDefaultNamespace("paintings");
   protected static final ResourceLocation PARTICLES_ATLAS = ResourceLocation.withDefaultNamespace("particles");
   private final Map<ResourceLocation, SpriteSourceProvider.SourceList> atlases = new HashMap<>();

   public SpriteSourceProvider(PackOutput output, ExistingFileHelper fileHelper, String modid) {
      super(output, fileHelper, modid, JsonOps.INSTANCE, PackType.CLIENT_RESOURCES, "atlases", SpriteSources.FILE_CODEC, Map.of());
   }

   @Override
   protected final void gather(BiConsumer<ResourceLocation, List<SpriteSource>> consumer) {
      this.addSources();

      for (Entry<ResourceLocation, SpriteSourceProvider.SourceList> entry : this.atlases.entrySet()) {
         ResourceLocation atlas = entry.getKey();
         SpriteSourceProvider.SourceList srcList = entry.getValue();
         consumer.accept(atlas, srcList.sources);
      }
   }

   protected abstract void addSources();

   protected final SpriteSourceProvider.SourceList atlas(ResourceLocation atlas) {
      return this.atlases.computeIfAbsent(atlas, $ -> new SpriteSourceProvider.SourceList());
   }

   protected static final class SourceList {
      private final List<SpriteSource> sources = new ArrayList<>();

      public SpriteSourceProvider.SourceList addSource(SpriteSource source) {
         this.sources.add(source);
         return this;
      }
   }
}
