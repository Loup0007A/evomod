package net.minecraft.client.model.geom;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityModelSet implements ResourceManagerReloadListener {
   private Map<ModelLayerLocation, LayerDefinition> roots = ImmutableMap.of();

   public ModelPart bakeLayer(ModelLayerLocation pModelLayerLocation) {
      LayerDefinition $$1 = this.roots.get(pModelLayerLocation);
      if ($$1 == null) {
         throw new IllegalArgumentException("No model for layer " + pModelLayerLocation);
      } else {
         return $$1.bakeRoot();
      }
   }

   @Override
   public void onResourceManagerReload(ResourceManager pResourceManager) {
      this.roots = ImmutableMap.copyOf(LayerDefinitions.createRoots());
   }
}
