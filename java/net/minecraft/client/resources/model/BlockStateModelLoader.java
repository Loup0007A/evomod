package net.minecraft.client.resources.model;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class BlockStateModelLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final int SINGLETON_MODEL_GROUP = -1;
   private static final int INVISIBLE_MODEL_GROUP = 0;
   public static final FileToIdConverter BLOCKSTATE_LISTER = FileToIdConverter.json("blockstates");
   private static final Splitter COMMA_SPLITTER = Splitter.on(',');
   private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);
   private static final StateDefinition<Block, BlockState> ITEM_FRAME_FAKE_DEFINITION = new StateDefinition.Builder<Block, BlockState>(Blocks.AIR)
      .add(BooleanProperty.create("map"))
      .create(Block::defaultBlockState, BlockState::new);
   private static final Map<ResourceLocation, StateDefinition<Block, BlockState>> STATIC_DEFINITIONS = Map.of(
      ResourceLocation.withDefaultNamespace("item_frame"),
      ITEM_FRAME_FAKE_DEFINITION,
      ResourceLocation.withDefaultNamespace("glow_item_frame"),
      ITEM_FRAME_FAKE_DEFINITION
   );
   private final Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>> blockStateResources;
   private final ProfilerFiller profiler;
   private final BlockColors blockColors;
   private final BiConsumer<ModelResourceLocation, UnbakedModel> discoveredModelOutput;
   private int nextModelGroup = 1;
   private final Object2IntMap<BlockState> modelGroups = Util.make(new Object2IntOpenHashMap(), p_342055_ -> p_342055_.defaultReturnValue(-1));
   private final BlockStateModelLoader.LoadedModel missingModel;
   private final BlockModelDefinition.Context context = new BlockModelDefinition.Context();

   public BlockStateModelLoader(
      Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>> pBlockStateResources,
      ProfilerFiller pProfiler,
      UnbakedModel pMissingModel,
      BlockColors pBlockColors,
      BiConsumer<ModelResourceLocation, UnbakedModel> pDiscoveredModelOutput
   ) {
      this.blockStateResources = pBlockStateResources;
      this.profiler = pProfiler;
      this.blockColors = pBlockColors;
      this.discoveredModelOutput = pDiscoveredModelOutput;
      BlockStateModelLoader.ModelGroupKey $$5 = new BlockStateModelLoader.ModelGroupKey(List.of(pMissingModel), List.of());
      this.missingModel = new BlockStateModelLoader.LoadedModel(pMissingModel, () -> $$5);
   }

   public void loadAllBlockStates() {
      this.profiler.push("static_definitions");
      STATIC_DEFINITIONS.forEach(this::loadBlockStateDefinitions);
      this.profiler.popPush("blocks");

      for (Block $$0 : BuiltInRegistries.BLOCK) {
         this.loadBlockStateDefinitions($$0.builtInRegistryHolder().key().location(), $$0.getStateDefinition());
      }

      this.profiler.pop();
   }

   private void loadBlockStateDefinitions(ResourceLocation p_343375_, StateDefinition<Block, BlockState> p_342234_) {
      this.context.setDefinition(p_342234_);
      List<Property<?>> $$2 = List.copyOf(this.blockColors.getColoringProperties(p_342234_.getOwner()));
      List<BlockState> $$3 = p_342234_.getPossibleStates();
      Map<ModelResourceLocation, BlockState> $$4 = new HashMap<>();
      $$3.forEach(p_345251_ -> $$4.put(BlockModelShaper.stateToModelLocation(p_343375_, p_345251_), p_345251_));
      Map<BlockState, BlockStateModelLoader.LoadedModel> $$5 = new HashMap<>();
      ResourceLocation $$6 = BLOCKSTATE_LISTER.idToFile(p_343375_);

      try {
         for (BlockStateModelLoader.LoadedJson $$7 : this.blockStateResources.getOrDefault($$6, List.of())) {
            BlockModelDefinition $$8 = $$7.parse(p_343375_, this.context);
            Map<BlockState, BlockStateModelLoader.LoadedModel> $$9 = new IdentityHashMap<>();
            MultiPart $$10;
            if ($$8.isMultiPart()) {
               $$10 = $$8.getMultiPart();
               $$3.forEach(
                  p_345048_ -> $$9.put(
                        p_345048_, new BlockStateModelLoader.LoadedModel($$10, () -> BlockStateModelLoader.ModelGroupKey.create(p_345048_, $$10, $$2))
                     )
               );
            } else {
               $$10 = null;
            }

            $$8.getVariants()
               .forEach(
                  (p_345103_, p_345100_) -> {
                     try {
                        $$3.stream()
                           .filter(predicate(p_342234_, p_345103_))
                           .forEach(
                              p_344771_ -> {
                                 BlockStateModelLoader.LoadedModel $$6x = $$9.put(
                                    p_344771_,
                                    new BlockStateModelLoader.LoadedModel(
                                       p_345100_, () -> BlockStateModelLoader.ModelGroupKey.create(p_344771_, p_345100_, $$2)
                                    )
                                 );
                                 if ($$6x != null && $$6x.model != $$10) {
                                    $$9.put(p_344771_, this.missingModel);
                                    throw new RuntimeException(
                                       "Overlapping definition with: "
                                          + $$8.getVariants()
                                             .entrySet()
                                             .stream()
                                             .filter(p_343452_ -> p_343452_.getValue() == $$6x.model)
                                             .findFirst()
                                             .get()
                                             .getKey()
                                    );
                                 }
                              }
                           );
                     } catch (Exception var12x) {
                        LOGGER.warn(
                           "Exception loading blockstate definition: '{}' in resourcepack: '{}' for variant: '{}': {}",
                           new Object[]{$$6, $$7.source, p_345103_, var12x.getMessage()}
                        );
                     }
                  }
               );
            $$5.putAll($$9);
         }
      } catch (BlockStateModelLoader.BlockStateDefinitionException var18) {
         LOGGER.warn("{}", var18.getMessage());
      } catch (Exception var19) {
         LOGGER.warn("Exception loading blockstate definition: '{}'", $$6, var19);
      } finally {
         Map<BlockStateModelLoader.ModelGroupKey, Set<BlockState>> $$17 = new HashMap<>();
         $$4.forEach((p_342418_, p_345470_) -> {
            BlockStateModelLoader.LoadedModel $$5x = $$5.get(p_345470_);
            if ($$5x == null) {
               LOGGER.warn("Exception loading blockstate definition: '{}' missing model for variant: '{}'", $$6, p_342418_);
               $$5x = this.missingModel;
            }

            this.discoveredModelOutput.accept(p_342418_, $$5x.model);

            try {
               BlockStateModelLoader.ModelGroupKey $$6x = $$5x.key().get();
               $$17.computeIfAbsent($$6x, p_342745_ -> Sets.newIdentityHashSet()).add(p_345470_);
            } catch (Exception var8) {
               LOGGER.warn("Exception evaluating model definition: '{}'", p_342418_, var8);
            }
         });
         $$17.forEach((p_344557_, p_342237_) -> {
            Iterator<BlockState> $$2x = p_342237_.iterator();

            while ($$2x.hasNext()) {
               BlockState $$3x = $$2x.next();
               if ($$3x.getRenderShape() != RenderShape.MODEL) {
                  $$2x.remove();
                  this.modelGroups.put($$3x, 0);
               }
            }

            if (p_342237_.size() > 1) {
               this.registerModelGroup(p_342237_);
            }
         });
      }
   }

   private static Predicate<BlockState> predicate(StateDefinition<Block, BlockState> pStateDefentition, String pProperties) {
      Map<Property<?>, Comparable<?>> $$2 = new HashMap<>();

      for (String $$3 : COMMA_SPLITTER.split(pProperties)) {
         Iterator<String> $$4 = EQUAL_SPLITTER.split($$3).iterator();
         if ($$4.hasNext()) {
            String $$5 = $$4.next();
            Property<?> $$6 = pStateDefentition.getProperty($$5);
            if ($$6 != null && $$4.hasNext()) {
               String $$7 = $$4.next();
               Comparable<?> $$8 = getValueHelper((Property<Comparable<?>>)$$6, $$7);
               if ($$8 == null) {
                  throw new RuntimeException("Unknown value: '" + $$7 + "' for blockstate property: '" + $$5 + "' " + $$6.getPossibleValues());
               }

               $$2.put($$6, $$8);
            } else if (!$$5.isEmpty()) {
               throw new RuntimeException("Unknown blockstate property: '" + $$5 + "'");
            }
         }
      }

      Block $$9 = pStateDefentition.getOwner();
      return p_343589_ -> {
         if (p_343589_ != null && p_343589_.is($$9)) {
            for (Entry<Property<?>, Comparable<?>> $$3x : $$2.entrySet()) {
               if (!Objects.equals(p_343589_.getValue($$3x.getKey()), $$3x.getValue())) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      };
   }

   @Nullable
   static <T extends Comparable<T>> T getValueHelper(Property<T> pProperty, String pPropertyName) {
      return pProperty.getValue(pPropertyName).orElse(null);
   }

   private void registerModelGroup(Iterable<BlockState> pModels) {
      int $$1 = this.nextModelGroup++;
      pModels.forEach(p_345338_ -> this.modelGroups.put(p_345338_, $$1));
   }

   public Object2IntMap<BlockState> getModelGroups() {
      return this.modelGroups;
   }

   @OnlyIn(Dist.CLIENT)
   static class BlockStateDefinitionException extends RuntimeException {
      public BlockStateDefinitionException(String pMessage) {
         super(pMessage);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static record LoadedJson(String source, JsonElement data) {
      BlockModelDefinition parse(ResourceLocation pBlockStateId, BlockModelDefinition.Context pContext) {
         try {
            return BlockModelDefinition.fromJsonElement(pContext, this.data);
         } catch (Exception var4) {
            throw new BlockStateModelLoader.BlockStateDefinitionException(
               String.format(
                  Locale.ROOT, "Exception loading blockstate definition: '%s' in resourcepack: '%s': %s", pBlockStateId, this.source, var4.getMessage()
               )
            );
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static record LoadedModel(UnbakedModel model, Supplier<BlockStateModelLoader.ModelGroupKey> key) {
   }

   @OnlyIn(Dist.CLIENT)
   static record ModelGroupKey(List<UnbakedModel> models, List<Object> coloringValues) {
      public static BlockStateModelLoader.ModelGroupKey create(BlockState pState, MultiPart pModel, Collection<Property<?>> pProperties) {
         StateDefinition<Block, BlockState> $$3 = pState.getBlock().getStateDefinition();
         List<UnbakedModel> $$4 = pModel.getSelectors()
            .stream()
            .filter(p_345001_ -> p_345001_.getPredicate($$3).test(pState))
            .map(Selector::getVariant)
            .collect(Collectors.toUnmodifiableList());
         List<Object> $$5 = getColoringValues(pState, pProperties);
         return new BlockStateModelLoader.ModelGroupKey($$4, $$5);
      }

      public static BlockStateModelLoader.ModelGroupKey create(BlockState pState, UnbakedModel pModel, Collection<Property<?>> pProperties) {
         List<Object> $$3 = getColoringValues(pState, pProperties);
         return new BlockStateModelLoader.ModelGroupKey(List.of(pModel), $$3);
      }

      private static List<Object> getColoringValues(BlockState pState, Collection<Property<?>> pProperties) {
         return pProperties.stream().map(pState::getValue).collect(Collectors.toUnmodifiableList());
      }
   }
}
