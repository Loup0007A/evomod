package net.minecraft.data.models.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModelLocationUtils {
   @Deprecated
   public static ResourceLocation decorateBlockModelLocation(String pBlockModelLocation) {
      return ResourceLocation.withDefaultNamespace("block/" + pBlockModelLocation);
   }

   public static ResourceLocation decorateItemModelLocation(String pItemModelLocation) {
      return ResourceLocation.withDefaultNamespace("item/" + pItemModelLocation);
   }

   public static ResourceLocation getModelLocation(Block pBlock, String pModelLocationSuffix) {
      ResourceLocation $$2 = BuiltInRegistries.BLOCK.getKey(pBlock);
      return $$2.withPath(p_251253_ -> "block/" + p_251253_ + pModelLocationSuffix);
   }

   public static ResourceLocation getModelLocation(Block pBlock) {
      ResourceLocation $$1 = BuiltInRegistries.BLOCK.getKey(pBlock);
      return $$1.withPrefix("block/");
   }

   public static ResourceLocation getModelLocation(Item pItem) {
      ResourceLocation $$1 = BuiltInRegistries.ITEM.getKey(pItem);
      return $$1.withPrefix("item/");
   }

   public static ResourceLocation getModelLocation(Item pItem, String pModelLocationSuffix) {
      ResourceLocation $$2 = BuiltInRegistries.ITEM.getKey(pItem);
      return $$2.withPath(p_251542_ -> "item/" + p_251542_ + pModelLocationSuffix);
   }
}
