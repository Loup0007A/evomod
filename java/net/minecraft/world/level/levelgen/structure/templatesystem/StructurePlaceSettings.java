package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class StructurePlaceSettings {
   private Mirror mirror = Mirror.NONE;
   private Rotation rotation = Rotation.NONE;
   private BlockPos rotationPivot = BlockPos.ZERO;
   private boolean ignoreEntities;
   @Nullable
   private BoundingBox boundingBox;
   private LiquidSettings liquidSettings = LiquidSettings.APPLY_WATERLOGGING;
   @Nullable
   private RandomSource random;
   private int palette;
   private final List<StructureProcessor> processors = Lists.newArrayList();
   private boolean knownShape;
   private boolean finalizeEntities;

   public StructurePlaceSettings copy() {
      StructurePlaceSettings $$0 = new StructurePlaceSettings();
      $$0.mirror = this.mirror;
      $$0.rotation = this.rotation;
      $$0.rotationPivot = this.rotationPivot;
      $$0.ignoreEntities = this.ignoreEntities;
      $$0.boundingBox = this.boundingBox;
      $$0.liquidSettings = this.liquidSettings;
      $$0.random = this.random;
      $$0.palette = this.palette;
      $$0.processors.addAll(this.processors);
      $$0.knownShape = this.knownShape;
      $$0.finalizeEntities = this.finalizeEntities;
      return $$0;
   }

   public StructurePlaceSettings setMirror(Mirror pMirror) {
      this.mirror = pMirror;
      return this;
   }

   public StructurePlaceSettings setRotation(Rotation pRotation) {
      this.rotation = pRotation;
      return this;
   }

   public StructurePlaceSettings setRotationPivot(BlockPos pRotationPivot) {
      this.rotationPivot = pRotationPivot;
      return this;
   }

   public StructurePlaceSettings setIgnoreEntities(boolean pIgnoreEntities) {
      this.ignoreEntities = pIgnoreEntities;
      return this;
   }

   public StructurePlaceSettings setBoundingBox(BoundingBox pBoundingBox) {
      this.boundingBox = pBoundingBox;
      return this;
   }

   public StructurePlaceSettings setRandom(@Nullable RandomSource pRandom) {
      this.random = pRandom;
      return this;
   }

   public StructurePlaceSettings setLiquidSettings(LiquidSettings pLiquidSettings) {
      this.liquidSettings = pLiquidSettings;
      return this;
   }

   public StructurePlaceSettings setKnownShape(boolean pKnownShape) {
      this.knownShape = pKnownShape;
      return this;
   }

   public StructurePlaceSettings clearProcessors() {
      this.processors.clear();
      return this;
   }

   public StructurePlaceSettings addProcessor(StructureProcessor pProcessor) {
      this.processors.add(pProcessor);
      return this;
   }

   public StructurePlaceSettings popProcessor(StructureProcessor pProcessor) {
      this.processors.remove(pProcessor);
      return this;
   }

   public Mirror getMirror() {
      return this.mirror;
   }

   public Rotation getRotation() {
      return this.rotation;
   }

   public BlockPos getRotationPivot() {
      return this.rotationPivot;
   }

   public RandomSource getRandom(@Nullable BlockPos pSeedPos) {
      if (this.random != null) {
         return this.random;
      } else {
         return pSeedPos == null ? RandomSource.create(Util.getMillis()) : RandomSource.create(Mth.getSeed(pSeedPos));
      }
   }

   public boolean isIgnoreEntities() {
      return this.ignoreEntities;
   }

   @Nullable
   public BoundingBox getBoundingBox() {
      return this.boundingBox;
   }

   public boolean getKnownShape() {
      return this.knownShape;
   }

   public List<StructureProcessor> getProcessors() {
      return this.processors;
   }

   public boolean shouldApplyWaterlogging() {
      return this.liquidSettings == LiquidSettings.APPLY_WATERLOGGING;
   }

   public StructureTemplate.Palette getRandomPalette(List<StructureTemplate.Palette> pPalettes, @Nullable BlockPos pPos) {
      int $$2 = pPalettes.size();
      if ($$2 == 0) {
         throw new IllegalStateException("No palettes");
      } else {
         return pPalettes.get(this.getRandom(pPos).nextInt($$2));
      }
   }

   public StructurePlaceSettings setFinalizeEntities(boolean pFinalizeEntities) {
      this.finalizeEntities = pFinalizeEntities;
      return this;
   }

   public boolean shouldFinalizeEntities() {
      return this.finalizeEntities;
   }
}
