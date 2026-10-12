package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlackstoneReplaceProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockAgeProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.LavaSubmergedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProtectedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;

public class RuinedPortalPiece extends TemplateStructurePiece {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final float PROBABILITY_OF_GOLD_GONE = 0.3F;
   private static final float PROBABILITY_OF_MAGMA_INSTEAD_OF_NETHERRACK = 0.07F;
   private static final float PROBABILITY_OF_MAGMA_INSTEAD_OF_LAVA = 0.2F;
   private final RuinedPortalPiece.VerticalPlacement verticalPlacement;
   private final RuinedPortalPiece.Properties properties;

   public RuinedPortalPiece(
      StructureTemplateManager pStructureTemplateManager,
      BlockPos pTemplatePosition,
      RuinedPortalPiece.VerticalPlacement pVerticalPlacement,
      RuinedPortalPiece.Properties pProperties,
      ResourceLocation pLocation,
      StructureTemplate pTemplate,
      Rotation pRotation,
      Mirror pMirror,
      BlockPos pPivotPos
   ) {
      super(
         StructurePieceType.RUINED_PORTAL,
         0,
         pStructureTemplateManager,
         pLocation,
         pLocation.toString(),
         makeSettings(pMirror, pRotation, pVerticalPlacement, pPivotPos, pProperties),
         pTemplatePosition
      );
      this.verticalPlacement = pVerticalPlacement;
      this.properties = pProperties;
   }

   public RuinedPortalPiece(StructureTemplateManager pStructureTemplateManager, CompoundTag pTag) {
      super(StructurePieceType.RUINED_PORTAL, pTag, pStructureTemplateManager, p_229188_ -> makeSettings(pStructureTemplateManager, pTag, p_229188_));
      this.verticalPlacement = RuinedPortalPiece.VerticalPlacement.byName(pTag.getString("VerticalPlacement"));
      this.properties = (RuinedPortalPiece.Properties)RuinedPortalPiece.Properties.CODEC
         .parse(new Dynamic(NbtOps.INSTANCE, pTag.get("Properties")))
         .getPartialOrThrow();
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
      super.addAdditionalSaveData(pContext, pTag);
      pTag.putString("Rotation", this.placeSettings.getRotation().name());
      pTag.putString("Mirror", this.placeSettings.getMirror().name());
      pTag.putString("VerticalPlacement", this.verticalPlacement.getName());
      RuinedPortalPiece.Properties.CODEC
         .encodeStart(NbtOps.INSTANCE, this.properties)
         .resultOrPartial(LOGGER::error)
         .ifPresent(p_229177_ -> pTag.put("Properties", p_229177_));
   }

   private static StructurePlaceSettings makeSettings(StructureTemplateManager pStructureTemplateManager, CompoundTag pTag, ResourceLocation pLocation) {
      StructureTemplate $$3 = pStructureTemplateManager.getOrCreate(pLocation);
      BlockPos $$4 = new BlockPos($$3.getSize().getX() / 2, 0, $$3.getSize().getZ() / 2);
      return makeSettings(
         Mirror.valueOf(pTag.getString("Mirror")),
         Rotation.valueOf(pTag.getString("Rotation")),
         RuinedPortalPiece.VerticalPlacement.byName(pTag.getString("VerticalPlacement")),
         $$4,
         (RuinedPortalPiece.Properties)RuinedPortalPiece.Properties.CODEC.parse(new Dynamic(NbtOps.INSTANCE, pTag.get("Properties"))).getPartialOrThrow()
      );
   }

   private static StructurePlaceSettings makeSettings(
      Mirror pMirror, Rotation pRotation, RuinedPortalPiece.VerticalPlacement pVerticalPlacement, BlockPos pPos, RuinedPortalPiece.Properties pProperties
   ) {
      BlockIgnoreProcessor $$5 = pProperties.airPocket ? BlockIgnoreProcessor.STRUCTURE_BLOCK : BlockIgnoreProcessor.STRUCTURE_AND_AIR;
      List<ProcessorRule> $$6 = Lists.newArrayList();
      $$6.add(getBlockReplaceRule(Blocks.GOLD_BLOCK, 0.3F, Blocks.AIR));
      $$6.add(getLavaProcessorRule(pVerticalPlacement, pProperties));
      if (!pProperties.cold) {
         $$6.add(getBlockReplaceRule(Blocks.NETHERRACK, 0.07F, Blocks.MAGMA_BLOCK));
      }

      StructurePlaceSettings $$7 = new StructurePlaceSettings()
         .setRotation(pRotation)
         .setMirror(pMirror)
         .setRotationPivot(pPos)
         .addProcessor($$5)
         .addProcessor(new RuleProcessor($$6))
         .addProcessor(new BlockAgeProcessor(pProperties.mossiness))
         .addProcessor(new ProtectedBlockProcessor(BlockTags.FEATURES_CANNOT_REPLACE))
         .addProcessor(new LavaSubmergedBlockProcessor());
      if (pProperties.replaceWithBlackstone) {
         $$7.addProcessor(BlackstoneReplaceProcessor.INSTANCE);
      }

      return $$7;
   }

   private static ProcessorRule getLavaProcessorRule(RuinedPortalPiece.VerticalPlacement pVerticalPlacement, RuinedPortalPiece.Properties pProperties) {
      if (pVerticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR) {
         return getBlockReplaceRule(Blocks.LAVA, Blocks.MAGMA_BLOCK);
      } else {
         return pProperties.cold ? getBlockReplaceRule(Blocks.LAVA, Blocks.NETHERRACK) : getBlockReplaceRule(Blocks.LAVA, 0.2F, Blocks.MAGMA_BLOCK);
      }
   }

   @Override
   public void postProcess(
      WorldGenLevel pLevel,
      StructureManager pStructureManager,
      ChunkGenerator pGenerator,
      RandomSource pRandom,
      BoundingBox pBox,
      ChunkPos pChunkPos,
      BlockPos pPos
   ) {
      BoundingBox $$7 = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
      if (pBox.isInside($$7.getCenter())) {
         pBox.encapsulate($$7);
         super.postProcess(pLevel, pStructureManager, pGenerator, pRandom, pBox, pChunkPos, pPos);
         this.spreadNetherrack(pRandom, pLevel);
         this.addNetherrackDripColumnsBelowPortal(pRandom, pLevel);
         if (this.properties.vines || this.properties.overgrown) {
            BlockPos.betweenClosedStream(this.getBoundingBox()).forEach(p_229127_ -> {
               if (this.properties.vines) {
                  this.maybeAddVines(pRandom, pLevel, p_229127_);
               }

               if (this.properties.overgrown) {
                  this.maybeAddLeavesAbove(pRandom, pLevel, p_229127_);
               }
            });
         }
      }
   }

   @Override
   protected void handleDataMarker(String pName, BlockPos pPos, ServerLevelAccessor pLevel, RandomSource pRandom, BoundingBox pBox) {
   }

   private void maybeAddVines(RandomSource pRandom, LevelAccessor pLevel, BlockPos pPos) {
      BlockState $$3 = pLevel.getBlockState(pPos);
      if (!$$3.isAir() && !$$3.is(Blocks.VINE)) {
         Direction $$4 = getRandomHorizontalDirection(pRandom);
         BlockPos $$5 = pPos.relative($$4);
         BlockState $$6 = pLevel.getBlockState($$5);
         if ($$6.isAir()) {
            if (Block.isFaceFull($$3.getCollisionShape(pLevel, pPos), $$4)) {
               BooleanProperty $$7 = VineBlock.getPropertyForFace($$4.getOpposite());
               pLevel.setBlock($$5, Blocks.VINE.defaultBlockState().setValue($$7, Boolean.valueOf(true)), 3);
            }
         }
      }
   }

   private void maybeAddLeavesAbove(RandomSource pRandom, LevelAccessor pLevel, BlockPos pPos) {
      if (pRandom.nextFloat() < 0.5F && pLevel.getBlockState(pPos).is(Blocks.NETHERRACK) && pLevel.getBlockState(pPos.above()).isAir()) {
         pLevel.setBlock(pPos.above(), Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, Boolean.valueOf(true)), 3);
      }
   }

   private void addNetherrackDripColumnsBelowPortal(RandomSource pRandom, LevelAccessor pLevel) {
      for (int $$2 = this.boundingBox.minX() + 1; $$2 < this.boundingBox.maxX(); $$2++) {
         for (int $$3 = this.boundingBox.minZ() + 1; $$3 < this.boundingBox.maxZ(); $$3++) {
            BlockPos $$4 = new BlockPos($$2, this.boundingBox.minY(), $$3);
            if (pLevel.getBlockState($$4).is(Blocks.NETHERRACK)) {
               this.addNetherrackDripColumn(pRandom, pLevel, $$4.below());
            }
         }
      }
   }

   private void addNetherrackDripColumn(RandomSource pRandom, LevelAccessor pLevel, BlockPos pPos) {
      BlockPos.MutableBlockPos $$3 = pPos.mutable();
      this.placeNetherrackOrMagma(pRandom, pLevel, $$3);
      int $$4 = 8;

      while ($$4 > 0 && pRandom.nextFloat() < 0.5F) {
         $$3.move(Direction.DOWN);
         $$4--;
         this.placeNetherrackOrMagma(pRandom, pLevel, $$3);
      }
   }

   private void spreadNetherrack(RandomSource pRandom, LevelAccessor pLevel) {
      boolean $$2 = this.verticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_LAND_SURFACE
         || this.verticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR;
      BlockPos $$3 = this.boundingBox.getCenter();
      int $$4 = $$3.getX();
      int $$5 = $$3.getZ();
      float[] $$6 = new float[]{1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.9F, 0.9F, 0.8F, 0.7F, 0.6F, 0.4F, 0.2F};
      int $$7 = $$6.length;
      int $$8 = (this.boundingBox.getXSpan() + this.boundingBox.getZSpan()) / 2;
      int $$9 = pRandom.nextInt(Math.max(1, 8 - $$8 / 2));
      int $$10 = 3;
      BlockPos.MutableBlockPos $$11 = BlockPos.ZERO.mutable();

      for (int $$12 = $$4 - $$7; $$12 <= $$4 + $$7; $$12++) {
         for (int $$13 = $$5 - $$7; $$13 <= $$5 + $$7; $$13++) {
            int $$14 = Math.abs($$12 - $$4) + Math.abs($$13 - $$5);
            int $$15 = Math.max(0, $$14 + $$9);
            if ($$15 < $$7) {
               float $$16 = $$6[$$15];
               if (pRandom.nextDouble() < (double)$$16) {
                  int $$17 = getSurfaceY(pLevel, $$12, $$13, this.verticalPlacement);
                  int $$18 = $$2 ? $$17 : Math.min(this.boundingBox.minY(), $$17);
                  $$11.set($$12, $$18, $$13);
                  if (Math.abs($$18 - this.boundingBox.minY()) <= 3 && this.canBlockBeReplacedByNetherrackOrMagma(pLevel, $$11)) {
                     this.placeNetherrackOrMagma(pRandom, pLevel, $$11);
                     if (this.properties.overgrown) {
                        this.maybeAddLeavesAbove(pRandom, pLevel, $$11);
                     }

                     this.addNetherrackDripColumn(pRandom, pLevel, $$11.below());
                  }
               }
            }
         }
      }
   }

   private boolean canBlockBeReplacedByNetherrackOrMagma(LevelAccessor pLevel, BlockPos pPos) {
      BlockState $$2 = pLevel.getBlockState(pPos);
      return !$$2.is(Blocks.AIR)
         && !$$2.is(Blocks.OBSIDIAN)
         && !$$2.is(BlockTags.FEATURES_CANNOT_REPLACE)
         && (this.verticalPlacement == RuinedPortalPiece.VerticalPlacement.IN_NETHER || !$$2.is(Blocks.LAVA));
   }

   private void placeNetherrackOrMagma(RandomSource pRandom, LevelAccessor pLevel, BlockPos pPos) {
      if (!this.properties.cold && pRandom.nextFloat() < 0.07F) {
         pLevel.setBlock(pPos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
      } else {
         pLevel.setBlock(pPos, Blocks.NETHERRACK.defaultBlockState(), 3);
      }
   }

   private static int getSurfaceY(LevelAccessor pLevel, int pX, int pZ, RuinedPortalPiece.VerticalPlacement pVerticalPlacement) {
      return pLevel.getHeight(getHeightMapType(pVerticalPlacement), pX, pZ) - 1;
   }

   public static Heightmap.Types getHeightMapType(RuinedPortalPiece.VerticalPlacement pVerticalPlacement) {
      return pVerticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.WORLD_SURFACE_WG;
   }

   private static ProcessorRule getBlockReplaceRule(Block pBlock, float pProbability, Block pReplaceBlock) {
      return new ProcessorRule(new RandomBlockMatchTest(pBlock, pProbability), AlwaysTrueTest.INSTANCE, pReplaceBlock.defaultBlockState());
   }

   private static ProcessorRule getBlockReplaceRule(Block pBlock, Block pReplaceBlock) {
      return new ProcessorRule(new BlockMatchTest(pBlock), AlwaysTrueTest.INSTANCE, pReplaceBlock.defaultBlockState());
   }

   public static class Properties {
      public static final Codec<RuinedPortalPiece.Properties> CODEC = RecordCodecBuilder.create(
         p_229214_ -> p_229214_.group(
                  Codec.BOOL.fieldOf("cold").forGetter(p_229226_ -> p_229226_.cold),
                  Codec.FLOAT.fieldOf("mossiness").forGetter(p_229224_ -> p_229224_.mossiness),
                  Codec.BOOL.fieldOf("air_pocket").forGetter(p_229222_ -> p_229222_.airPocket),
                  Codec.BOOL.fieldOf("overgrown").forGetter(p_229220_ -> p_229220_.overgrown),
                  Codec.BOOL.fieldOf("vines").forGetter(p_229218_ -> p_229218_.vines),
                  Codec.BOOL.fieldOf("replace_with_blackstone").forGetter(p_229216_ -> p_229216_.replaceWithBlackstone)
               )
               .apply(p_229214_, RuinedPortalPiece.Properties::new)
      );
      public boolean cold;
      public float mossiness;
      public boolean airPocket;
      public boolean overgrown;
      public boolean vines;
      public boolean replaceWithBlackstone;

      public Properties() {
      }

      public Properties(boolean p_229207_, float p_229208_, boolean p_229209_, boolean p_229210_, boolean p_229211_, boolean p_229212_) {
         this.cold = p_229207_;
         this.mossiness = p_229208_;
         this.airPocket = p_229209_;
         this.overgrown = p_229210_;
         this.vines = p_229211_;
         this.replaceWithBlackstone = p_229212_;
      }
   }

   public static enum VerticalPlacement implements StringRepresentable {
      ON_LAND_SURFACE("on_land_surface"),
      PARTLY_BURIED("partly_buried"),
      ON_OCEAN_FLOOR("on_ocean_floor"),
      IN_MOUNTAIN("in_mountain"),
      UNDERGROUND("underground"),
      IN_NETHER("in_nether");

      public static final StringRepresentable.EnumCodec<RuinedPortalPiece.VerticalPlacement> CODEC = StringRepresentable.fromEnum(
         RuinedPortalPiece.VerticalPlacement::values
      );
      private final String name;

      private VerticalPlacement(final String pName) {
         this.name = pName;
      }

      public String getName() {
         return this.name;
      }

      public static RuinedPortalPiece.VerticalPlacement byName(String pName) {
         return CODEC.byName(pName);
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
