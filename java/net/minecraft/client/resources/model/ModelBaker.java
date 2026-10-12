package net.minecraft.client.resources.model;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.IForgeModelBaker;

@OnlyIn(Dist.CLIENT)
public interface ModelBaker extends IForgeModelBaker {
   UnbakedModel getModel(ResourceLocation pLocation);

   @Deprecated
   @Nullable
   BakedModel bake(ResourceLocation pLocation, ModelState pTransform);
}
