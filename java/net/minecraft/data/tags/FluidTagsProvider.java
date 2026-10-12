package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class FluidTagsProvider extends IntrinsicHolderTagsProvider<Fluid> {
   public FluidTagsProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pProvider) {
      super(pOutput, Registries.FLUID, pProvider, p_256474_ -> p_256474_.builtInRegistryHolder().key());
   }

   public FluidTagsProvider(
      PackOutput p_255941_, CompletableFuture<HolderLookup.Provider> p_256600_, String modId, @Nullable ExistingFileHelper existingFileHelper
   ) {
      super(p_255941_, Registries.FLUID, p_256600_, p_256474_ -> p_256474_.builtInRegistryHolder().key(), modId, existingFileHelper);
   }

   @Override
   protected void addTags(HolderLookup.Provider pProvider) {
      this.tag(FluidTags.WATER).add(Fluids.WATER, Fluids.FLOWING_WATER);
      this.tag(FluidTags.LAVA).add(Fluids.LAVA, Fluids.FLOWING_LAVA);
   }
}
