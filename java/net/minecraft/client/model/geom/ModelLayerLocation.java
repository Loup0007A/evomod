package net.minecraft.client.model.geom;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ModelLayerLocation {
   private final ResourceLocation model;
   private final String layer;

   public ModelLayerLocation(ResourceLocation pModel, String pLayer) {
      this.model = pModel;
      this.layer = pLayer;
   }

   public ResourceLocation getModel() {
      return this.model;
   }

   public String getLayer() {
      return this.layer;
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         return !(pOther instanceof ModelLayerLocation $$1) ? false : this.model.equals($$1.model) && this.layer.equals($$1.layer);
      }
   }

   @Override
   public int hashCode() {
      int $$0 = this.model.hashCode();
      return 31 * $$0 + this.layer.hashCode();
   }

   @Override
   public String toString() {
      return this.model + "#" + this.layer;
   }
}
