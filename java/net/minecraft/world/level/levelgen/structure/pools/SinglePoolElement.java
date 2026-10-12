package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Optionull;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class SinglePoolElement extends StructurePoolElement {
   private static final Codec<Either<ResourceLocation, StructureTemplate>> TEMPLATE_CODEC = Codec.of(
      SinglePoolElement::encodeTemplate, ResourceLocation.CODEC.map(Either::left)
   );
   public static final MapCodec<SinglePoolElement> CODEC = RecordCodecBuilder.mapCodec(
      p_341933_ -> p_341933_.group(templateCodec(), processorsCodec(), projectionCodec(), overrideLiquidSettingsCodec())
            .apply(p_341933_, SinglePoolElement::new)
   );
   protected final Either<ResourceLocation, StructureTemplate> template;
   protected final Holder<StructureProcessorList> processors;
   protected final Optional<LiquidSettings> overrideLiquidSettings;

   private static <T> DataResult<T> encodeTemplate(Either<ResourceLocation, StructureTemplate> p_210425_, DynamicOps<T> p_210426_, T p_210427_) {
      Optional<ResourceLocation> $$3 = p_210425_.left();
      return $$3.isEmpty()
         ? DataResult.error(() -> "Can not serialize a runtime pool element")
         : ResourceLocation.CODEC.encode($$3.get(), p_210426_, p_210427_);
   }

   protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Holder<StructureProcessorList>> processorsCodec() {
      return StructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter(p_210464_ -> p_210464_.processors);
   }

   protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Optional<LiquidSettings>> overrideLiquidSettingsCodec() {
      return LiquidSettings.CODEC.optionalFieldOf("override_liquid_settings").forGetter(p_341934_ -> p_341934_.overrideLiquidSettings);
   }

   protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Either<ResourceLocation, StructureTemplate>> templateCodec() {
      return TEMPLATE_CODEC.fieldOf("location").forGetter(p_210431_ -> p_210431_.template);
   }

   protected SinglePoolElement(
      Either<ResourceLocation, StructureTemplate> p_210415_,
      Holder<StructureProcessorList> p_210416_,
      StructureTemplatePool.Projection p_210417_,
      Optional<LiquidSettings> p_344439_
   ) {
      super(p_210417_);
      this.template = p_210415_;
      this.processors = p_210416_;
      this.overrideLiquidSettings = p_344439_;
   }

   @Override
   public Vec3i getSize(StructureTemplateManager pStructureTemplateManager, Rotation pRotation) {
      StructureTemplate $$2 = this.getTemplate(pStructureTemplateManager);
      return $$2.getSize(pRotation);
   }

   private StructureTemplate getTemplate(StructureTemplateManager pStructureTemplateManager) {
      return (StructureTemplate)this.template.map(pStructureTemplateManager::getOrCreate, Function.identity());
   }

   public List<StructureTemplate.StructureBlockInfo> getDataMarkers(
      StructureTemplateManager pStructureTemplateManager, BlockPos pPos, Rotation pRotation, boolean pRelativePosition
   ) {
      StructureTemplate $$4 = this.getTemplate(pStructureTemplateManager);
      List<StructureTemplate.StructureBlockInfo> $$5 = $$4.filterBlocks(
         pPos, new StructurePlaceSettings().setRotation(pRotation), Blocks.STRUCTURE_BLOCK, pRelativePosition
      );
      List<StructureTemplate.StructureBlockInfo> $$6 = Lists.newArrayList();

      for (StructureTemplate.StructureBlockInfo $$7 : $$5) {
         CompoundTag $$8 = $$7.nbt();
         if ($$8 != null) {
            StructureMode $$9 = StructureMode.valueOf($$8.getString("mode"));
            if ($$9 == StructureMode.DATA) {
               $$6.add($$7);
            }
         }
      }

      return $$6;
   }

   @Override
   public List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(
      StructureTemplateManager pStructureTemplateManager, BlockPos pPos, Rotation pRotation, RandomSource pRandom
   ) {
      StructureTemplate $$4 = this.getTemplate(pStructureTemplateManager);
      ObjectArrayList<StructureTemplate.StructureBlockInfo> $$5 = $$4.filterBlocks(
         pPos, new StructurePlaceSettings().setRotation(pRotation), Blocks.JIGSAW, true
      );
      Util.shuffle($$5, pRandom);
      sortBySelectionPriority($$5);
      return $$5;
   }

   @VisibleForTesting
   static void sortBySelectionPriority(List<StructureTemplate.StructureBlockInfo> pStructureBlockInfos) {
      pStructureBlockInfos.sort(
         Comparator.<StructureTemplate.StructureBlockInfo>comparingInt(
               p_309349_ -> Optionull.mapOrDefault(p_309349_.nbt(), p_309348_ -> p_309348_.getInt("selection_priority"), 0)
            )
            .reversed()
      );
   }

   @Override
   public BoundingBox getBoundingBox(StructureTemplateManager pStructureTemplateManager, BlockPos pPos, Rotation pRotation) {
      StructureTemplate $$3 = this.getTemplate(pStructureTemplateManager);
      return $$3.getBoundingBox(new StructurePlaceSettings().setRotation(pRotation), pPos);
   }

   @Override
   public boolean place(
      StructureTemplateManager pStructureTemplateManager,
      WorldGenLevel pLevel,
      StructureManager pStructureManager,
      ChunkGenerator pGenerator,
      BlockPos pOffset,
      BlockPos pPos,
      Rotation pRotation,
      BoundingBox pBox,
      RandomSource pRandom,
      LiquidSettings pLiquidSettings,
      boolean pKeepJigsaws
   ) {
      StructureTemplate $$11 = this.getTemplate(pStructureTemplateManager);
      StructurePlaceSettings $$12 = this.getSettings(pRotation, pBox, pLiquidSettings, pKeepJigsaws);
      if (!$$11.placeInWorld(pLevel, pOffset, pPos, $$12, pRandom, 18)) {
         return false;
      } else {
         for (StructureTemplate.StructureBlockInfo $$14 : StructureTemplate.processBlockInfos(
            pLevel, pOffset, pPos, $$12, this.getDataMarkers(pStructureTemplateManager, pOffset, pRotation, false)
         )) {
            this.handleDataMarker(pLevel, $$14, pOffset, pRotation, pRandom, pBox);
         }

         return true;
      }
   }

   protected StructurePlaceSettings getSettings(Rotation pRotation, BoundingBox pBoundingBox, LiquidSettings pLiquidSettings, boolean pOffset) {
      StructurePlaceSettings $$4 = new StructurePlaceSettings();
      $$4.setBoundingBox(pBoundingBox);
      $$4.setRotation(pRotation);
      $$4.setKnownShape(true);
      $$4.setIgnoreEntities(false);
      $$4.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
      $$4.setFinalizeEntities(true);
      $$4.setLiquidSettings(this.overrideLiquidSettings.orElse(pLiquidSettings));
      if (!pOffset) {
         $$4.addProcessor(JigsawReplacementProcessor.INSTANCE);
      }

      this.processors.value().list().forEach($$4::addProcessor);
      this.getProjection().getProcessors().forEach($$4::addProcessor);
      return $$4;
   }

   @Override
   public StructurePoolElementType<?> getType() {
      return StructurePoolElementType.SINGLE;
   }

   @Override
   public String toString() {
      return "Single[" + this.template + "]";
   }
}
