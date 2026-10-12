package net.minecraft.gametest.framework;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class StructureUtils {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int DEFAULT_Y_SEARCH_RADIUS = 10;
   public static final String DEFAULT_TEST_STRUCTURES_DIR = "gameteststructures";
   public static String testStructuresDir = "gameteststructures";

   public static Rotation getRotationForRotationSteps(int pRotationSteps) {
      switch (pRotationSteps) {
         case 0:
            return Rotation.NONE;
         case 1:
            return Rotation.CLOCKWISE_90;
         case 2:
            return Rotation.CLOCKWISE_180;
         case 3:
            return Rotation.COUNTERCLOCKWISE_90;
         default:
            throw new IllegalArgumentException("rotationSteps must be a value from 0-3. Got value " + pRotationSteps);
      }
   }

   public static int getRotationStepsForRotation(Rotation pRotation) {
      switch (pRotation) {
         case NONE:
            return 0;
         case CLOCKWISE_90:
            return 1;
         case CLOCKWISE_180:
            return 2;
         case COUNTERCLOCKWISE_90:
            return 3;
         default:
            throw new IllegalArgumentException("Unknown rotation value, don't know how many steps it represents: " + pRotation);
      }
   }

   public static AABB getStructureBounds(StructureBlockEntity pStructureBlockEntity) {
      return AABB.of(getStructureBoundingBox(pStructureBlockEntity));
   }

   public static BoundingBox getStructureBoundingBox(StructureBlockEntity pStructureBlockEntity) {
      BlockPos $$1 = getStructureOrigin(pStructureBlockEntity);
      BlockPos $$2 = getTransformedFarCorner($$1, pStructureBlockEntity.getStructureSize(), pStructureBlockEntity.getRotation());
      return BoundingBox.fromCorners($$1, $$2);
   }

   public static BlockPos getStructureOrigin(StructureBlockEntity pStructureBlockEntity) {
      return pStructureBlockEntity.getBlockPos().offset(pStructureBlockEntity.getStructurePos());
   }

   public static void addCommandBlockAndButtonToStartTest(BlockPos pStructureBlockPos, BlockPos pOffset, Rotation pRotation, ServerLevel pServerLevel) {
      BlockPos $$4 = StructureTemplate.transform(pStructureBlockPos.offset(pOffset), Mirror.NONE, pRotation, pStructureBlockPos);
      pServerLevel.setBlockAndUpdate($$4, Blocks.COMMAND_BLOCK.defaultBlockState());
      CommandBlockEntity $$5 = (CommandBlockEntity)pServerLevel.getBlockEntity($$4);
      $$5.getCommandBlock().setCommand("test runclosest");
      BlockPos $$6 = StructureTemplate.transform($$4.offset(0, 0, -1), Mirror.NONE, pRotation, $$4);
      pServerLevel.setBlockAndUpdate($$6, Blocks.STONE_BUTTON.defaultBlockState().rotate(pRotation));
   }

   public static void createNewEmptyStructureBlock(String pStructureName, BlockPos pPos, Vec3i pSize, Rotation pRotation, ServerLevel pServerLevel) {
      BoundingBox $$5 = getStructureBoundingBox(pPos.above(), pSize, pRotation);
      clearSpaceForStructure($$5, pServerLevel);
      pServerLevel.setBlockAndUpdate(pPos, Blocks.STRUCTURE_BLOCK.defaultBlockState());
      StructureBlockEntity $$6 = (StructureBlockEntity)pServerLevel.getBlockEntity(pPos);
      $$6.setIgnoreEntities(false);
      $$6.setStructureName(ResourceLocation.parse(pStructureName));
      $$6.setStructureSize(pSize);
      $$6.setMode(StructureMode.SAVE);
      $$6.setShowBoundingBox(true);
   }

   public static StructureBlockEntity prepareTestStructure(GameTestInfo pGameTestInfo, BlockPos pPos, Rotation pRotation, ServerLevel pLevel) {
      Vec3i $$4 = pLevel.getStructureManager()
         .get(ResourceLocation.parse(pGameTestInfo.getStructureName()))
         .orElseThrow(() -> new IllegalStateException("Missing test structure: " + pGameTestInfo.getStructureName()))
         .getSize();
      BoundingBox $$5 = getStructureBoundingBox(pPos, $$4, pRotation);
      BlockPos $$6;
      if (pRotation == Rotation.NONE) {
         $$6 = pPos;
      } else if (pRotation == Rotation.CLOCKWISE_90) {
         $$6 = pPos.offset($$4.getZ() - 1, 0, 0);
      } else if (pRotation == Rotation.CLOCKWISE_180) {
         $$6 = pPos.offset($$4.getX() - 1, 0, $$4.getZ() - 1);
      } else {
         if (pRotation != Rotation.COUNTERCLOCKWISE_90) {
            throw new IllegalArgumentException("Invalid rotation: " + pRotation);
         }

         $$6 = pPos.offset(0, 0, $$4.getX() - 1);
      }

      forceLoadChunks($$5, pLevel);
      clearSpaceForStructure($$5, pLevel);
      return createStructureBlock(pGameTestInfo, $$6.below(), pRotation, pLevel);
   }

   public static void encaseStructure(AABB pBounds, ServerLevel pLevel, boolean pPlaceBarriers) {
      BlockPos $$3 = BlockPos.containing(pBounds.minX, pBounds.minY, pBounds.minZ).offset(-1, 0, -1);
      BlockPos $$4 = BlockPos.containing(pBounds.maxX, pBounds.maxY, pBounds.maxZ);
      BlockPos.betweenClosedStream($$3, $$4).forEach(p_325964_ -> {
         boolean $$5 = p_325964_.getX() == $$3.getX() || p_325964_.getX() == $$4.getX() || p_325964_.getZ() == $$3.getZ() || p_325964_.getZ() == $$4.getZ();
         boolean $$6 = p_325964_.getY() == $$4.getY();
         if ($$5 || $$6 && pPlaceBarriers) {
            pLevel.setBlockAndUpdate(p_325964_, Blocks.BARRIER.defaultBlockState());
         }
      });
   }

   public static void removeBarriers(AABB pBounds, ServerLevel pLevel) {
      BlockPos $$2 = BlockPos.containing(pBounds.minX, pBounds.minY, pBounds.minZ).offset(-1, 0, -1);
      BlockPos $$3 = BlockPos.containing(pBounds.maxX, pBounds.maxY, pBounds.maxZ);
      BlockPos.betweenClosedStream($$2, $$3).forEach(p_325970_ -> {
         boolean $$4 = p_325970_.getX() == $$2.getX() || p_325970_.getX() == $$3.getX() || p_325970_.getZ() == $$2.getZ() || p_325970_.getZ() == $$3.getZ();
         boolean $$5 = p_325970_.getY() == $$3.getY();
         if (pLevel.getBlockState(p_325970_).is(Blocks.BARRIER) && ($$4 || $$5)) {
            pLevel.setBlockAndUpdate(p_325970_, Blocks.AIR.defaultBlockState());
         }
      });
   }

   private static void forceLoadChunks(BoundingBox pBoundingBox, ServerLevel pLevel) {
      pBoundingBox.intersectingChunks().forEach(p_308541_ -> pLevel.setChunkForced(p_308541_.x, p_308541_.z, true));
   }

   public static void clearSpaceForStructure(BoundingBox pBoundingBox, ServerLevel pLevel) {
      int $$2 = pBoundingBox.minY() - 1;
      BoundingBox $$3 = new BoundingBox(
         pBoundingBox.minX() - 2, pBoundingBox.minY() - 3, pBoundingBox.minZ() - 3, pBoundingBox.maxX() + 3, pBoundingBox.maxY() + 20, pBoundingBox.maxZ() + 3
      );
      BlockPos.betweenClosedStream($$3).forEach(p_177748_ -> clearBlock($$2, p_177748_, pLevel));
      pLevel.getBlockTicks().clearArea($$3);
      pLevel.clearBlockEvents($$3);
      AABB $$4 = AABB.of($$3);
      List<Entity> $$5 = pLevel.getEntitiesOfClass(Entity.class, $$4, p_177750_ -> !(p_177750_ instanceof Player));
      $$5.forEach(Entity::discard);
   }

   public static BlockPos getTransformedFarCorner(BlockPos pPos, Vec3i pOffset, Rotation pRotation) {
      BlockPos $$3 = pPos.offset(pOffset).offset(-1, -1, -1);
      return StructureTemplate.transform($$3, Mirror.NONE, pRotation, pPos);
   }

   public static BoundingBox getStructureBoundingBox(BlockPos pPos, Vec3i pOffset, Rotation pRotation) {
      BlockPos $$3 = getTransformedFarCorner(pPos, pOffset, pRotation);
      BoundingBox $$4 = BoundingBox.fromCorners(pPos, $$3);
      int $$5 = Math.min($$4.minX(), $$4.maxX());
      int $$6 = Math.min($$4.minZ(), $$4.maxZ());
      return $$4.move(pPos.getX() - $$5, 0, pPos.getZ() - $$6);
   }

   public static Optional<BlockPos> findStructureBlockContainingPos(BlockPos pPos, int pRadius, ServerLevel pServerLevel) {
      return findStructureBlocks(pPos, pRadius, pServerLevel).filter(p_177756_ -> doesStructureContain(p_177756_, pPos, pServerLevel)).findFirst();
   }

   public static Optional<BlockPos> findNearestStructureBlock(BlockPos pPos, int pRadius, ServerLevel pLevel) {
      Comparator<BlockPos> $$3 = Comparator.comparingInt(p_177759_ -> p_177759_.distManhattan(pPos));
      return findStructureBlocks(pPos, pRadius, pLevel).min($$3);
   }

   public static Stream<BlockPos> findStructureByTestFunction(BlockPos pPos, int pRadius, ServerLevel pLevel, String pTestName) {
      return findStructureBlocks(pPos, pRadius, pLevel)
         .map(p_325972_ -> (StructureBlockEntity)pLevel.getBlockEntity(p_325972_))
         .filter(Objects::nonNull)
         .filter(p_325954_ -> Objects.equals(p_325954_.getStructureName(), pTestName))
         .map(BlockEntity::getBlockPos)
         .map(BlockPos::immutable);
   }

   public static Stream<BlockPos> findStructureBlocks(BlockPos pPos, int pRadius, ServerLevel pLevel) {
      BoundingBox $$3 = getBoundingBoxAtGround(pPos, pRadius, pLevel);
      return BlockPos.betweenClosedStream($$3).filter(p_325959_ -> pLevel.getBlockState(p_325959_).is(Blocks.STRUCTURE_BLOCK)).map(BlockPos::immutable);
   }

   private static StructureBlockEntity createStructureBlock(GameTestInfo pGameTestInfo, BlockPos pPos, Rotation pRotation, ServerLevel pLevel) {
      pLevel.setBlockAndUpdate(pPos, Blocks.STRUCTURE_BLOCK.defaultBlockState());
      StructureBlockEntity $$4 = (StructureBlockEntity)pLevel.getBlockEntity(pPos);
      $$4.setMode(StructureMode.LOAD);
      $$4.setRotation(pRotation);
      $$4.setIgnoreEntities(false);
      $$4.setStructureName(ResourceLocation.parse(pGameTestInfo.getStructureName()));
      $$4.setMetaData(pGameTestInfo.getTestName());
      if (!$$4.loadStructureInfo(pLevel)) {
         throw new RuntimeException(
            "Failed to load structure info for test: " + pGameTestInfo.getTestName() + ". Structure name: " + pGameTestInfo.getStructureName()
         );
      } else {
         return $$4;
      }
   }

   private static BoundingBox getBoundingBoxAtGround(BlockPos pPos, int pRadius, ServerLevel pLevel) {
      BlockPos $$3 = BlockPos.containing((double)pPos.getX(), (double)pLevel.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pPos).getY(), (double)pPos.getZ());
      return new BoundingBox($$3).inflatedBy(pRadius, 10, pRadius);
   }

   public static Stream<BlockPos> lookedAtStructureBlockPos(BlockPos pPos, Entity pEntity, ServerLevel pLevel) {
      int $$3 = 200;
      Vec3 $$4 = pEntity.getEyePosition();
      Vec3 $$5 = $$4.add(pEntity.getLookAngle().scale(200.0));
      return findStructureBlocks(pPos, 200, pLevel)
         .map(p_325966_ -> pLevel.getBlockEntity(p_325966_, BlockEntityType.STRUCTURE_BLOCK))
         .flatMap(Optional::stream)
         .filter(p_325957_ -> getStructureBounds(p_325957_).clip($$4, $$5).isPresent())
         .map(BlockEntity::getBlockPos)
         .sorted(Comparator.comparing(pPos::distSqr))
         .limit(1L);
   }

   private static void clearBlock(int pStructureBlockY, BlockPos pPos, ServerLevel pServerLevel) {
      BlockState $$3;
      if (pPos.getY() < pStructureBlockY) {
         $$3 = Blocks.STONE.defaultBlockState();
      } else {
         $$3 = Blocks.AIR.defaultBlockState();
      }

      BlockInput $$5 = new BlockInput($$3, Collections.emptySet(), null);
      $$5.place(pServerLevel, pPos, 2);
      pServerLevel.blockUpdated(pPos, $$3.getBlock());
   }

   private static boolean doesStructureContain(BlockPos pStructureBlockPos, BlockPos pPosToTest, ServerLevel pServerLevel) {
      StructureBlockEntity $$3 = (StructureBlockEntity)pServerLevel.getBlockEntity(pStructureBlockPos);
      return getStructureBoundingBox($$3).isInside(pPosToTest);
   }
}
