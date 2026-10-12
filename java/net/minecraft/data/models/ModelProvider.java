package net.minecraft.data.models;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.blockstates.BlockStateGenerator;
import net.minecraft.data.models.model.DelegatedModel;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModelProvider implements DataProvider {
   private final PackOutput.PathProvider blockStatePathProvider;
   private final PackOutput.PathProvider modelPathProvider;

   public ModelProvider(PackOutput pOutput) {
      this.blockStatePathProvider = pOutput.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
      this.modelPathProvider = pOutput.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
   }

   @Override
   public CompletableFuture<?> run(CachedOutput pOutput) {
      Map<Block, BlockStateGenerator> $$1 = Maps.newHashMap();
      Consumer<BlockStateGenerator> $$2 = p_325906_ -> {
         Block $$2x = p_325906_.getBlock();
         BlockStateGenerator $$3x = $$1.put($$2x, p_325906_);
         if ($$3x != null) {
            throw new IllegalStateException("Duplicate blockstate definition for " + $$2x);
         }
      };
      Map<ResourceLocation, Supplier<JsonElement>> $$3 = Maps.newHashMap();
      Set<Item> $$4 = Sets.newHashSet();
      BiConsumer<ResourceLocation, Supplier<JsonElement>> $$5 = (p_325908_, p_325909_) -> {
         Supplier<JsonElement> $$3x = $$3.put(p_325908_, p_325909_);
         if ($$3x != null) {
            throw new IllegalStateException("Duplicate model definition for " + p_325908_);
         }
      };
      Consumer<Item> $$6 = $$4::add;
      new BlockModelGenerators($$2, $$5, $$6).run();
      new ItemModelGenerators($$5).run();
      List<Block> $$7 = BuiltInRegistries.BLOCK
         .entrySet()
         .stream()
         .filter(p_296355_ -> true)
         .map(Entry::getValue)
         .filter(p_125117_ -> !$$1.containsKey(p_125117_))
         .toList();
      if (!$$7.isEmpty()) {
         throw new IllegalStateException("Missing blockstate definitions for: " + $$7);
      } else {
         BuiltInRegistries.BLOCK.forEach(p_125128_ -> {
            Item $$3x = Item.BY_BLOCK.get(p_125128_);
            if ($$3x != null) {
               if ($$4.contains($$3x)) {
                  return;
               }

               ResourceLocation $$4x = ModelLocationUtils.getModelLocation($$3x);
               if (!$$3.containsKey($$4x)) {
                  $$3.put($$4x, new DelegatedModel(ModelLocationUtils.getModelLocation(p_125128_)));
               }
            }
         });
         return CompletableFuture.allOf(
            this.saveCollection(pOutput, $$1, p_248016_ -> this.blockStatePathProvider.json(p_248016_.builtInRegistryHolder().key().location())),
            this.saveCollection(pOutput, $$3, this.modelPathProvider::json)
         );
      }
   }

   private <T> CompletableFuture<?> saveCollection(
      CachedOutput pOutput, Map<T, ? extends Supplier<JsonElement>> pObjectToJsonMap, Function<T, Path> pResolveObjectPath
   ) {
      return CompletableFuture.allOf(pObjectToJsonMap.entrySet().stream().map(p_253408_ -> {
         Path $$3 = pResolveObjectPath.apply(p_253408_.getKey());
         JsonElement $$4 = p_253408_.getValue().get();
         return DataProvider.saveStable(pOutput, $$4, $$3);
      }).toArray(CompletableFuture[]::new));
   }

   @Override
   public final String getName() {
      return "Model Definitions";
   }
}
