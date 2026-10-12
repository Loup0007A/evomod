package net.minecraft.client.renderer.texture.atlas;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SpriteSourceList {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter ATLAS_INFO_CONVERTER = new FileToIdConverter("atlases", ".json");
   private final List<SpriteSource> sources;

   private SpriteSourceList(List<SpriteSource> pSources) {
      this.sources = pSources;
   }

   public List<Function<SpriteResourceLoader, SpriteContents>> list(ResourceManager pResourceManager) {
      final Map<ResourceLocation, SpriteSource.SpriteSupplier> $$1 = new HashMap<>();
      SpriteSource.Output $$2 = new SpriteSource.Output() {
         @Override
         public void add(ResourceLocation p_297663_, SpriteSource.SpriteSupplier p_299416_) {
            SpriteSource.SpriteSupplier $$2 = $$1.put(p_297663_, p_299416_);
            if ($$2 != null) {
               $$2.discard();
            }
         }

         @Override
         public void removeAll(Predicate<ResourceLocation> p_299726_) {
            Iterator<Entry<ResourceLocation, SpriteSource.SpriteSupplier>> $$1 = $$1.entrySet().iterator();

            while ($$1.hasNext()) {
               Entry<ResourceLocation, SpriteSource.SpriteSupplier> $$2 = $$1.next();
               if (p_299726_.test($$2.getKey())) {
                  $$2.getValue().discard();
                  $$1.remove();
               }
            }
         }
      };
      this.sources.forEach(p_299872_ -> p_299872_.run(pResourceManager, $$2));
      Builder<Function<SpriteResourceLoader, SpriteContents>> $$3 = ImmutableList.builder();
      $$3.add((Function<SpriteResourceLoader, SpriteContents>)p_299121_ -> MissingTextureAtlasSprite.create());
      $$3.addAll($$1.values());
      return $$3.build();
   }

   public static SpriteSourceList load(ResourceManager pResourceManager, ResourceLocation pSprite) {
      ResourceLocation $$2 = ATLAS_INFO_CONVERTER.idToFile(pSprite);
      List<SpriteSource> $$3 = new ArrayList<>();

      for (Resource $$4 : pResourceManager.getResourceStack($$2)) {
         try (BufferedReader $$5 = $$4.openAsReader()) {
            Dynamic<JsonElement> $$6 = new Dynamic(JsonOps.INSTANCE, JsonParser.parseReader($$5));
            $$3.addAll((Collection<? extends SpriteSource>)SpriteSources.FILE_CODEC.parse($$6).getOrThrow());
         } catch (Exception var11) {
            LOGGER.error("Failed to parse atlas definition {} in pack {}", new Object[]{$$2, $$4.sourcePackId(), var11});
         }
      }

      return new SpriteSourceList($$3);
   }
}
