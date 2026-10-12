package net.minecraft.world.level.levelgen.structure.templatesystem;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;

public abstract class StructureProcessor {
   @Deprecated
   @Nullable
   public StructureTemplate.StructureBlockInfo processBlock(
      LevelReader pLevel,
      BlockPos pOffset,
      BlockPos pPos,
      StructureTemplate.StructureBlockInfo pBlockInfo,
      StructureTemplate.StructureBlockInfo pRelativeBlockInfo,
      StructurePlaceSettings pSettings
   ) {
      return pRelativeBlockInfo;
   }

   protected abstract StructureProcessorType<?> getType();

   public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
      ServerLevelAccessor pServerLevel,
      BlockPos pOffset,
      BlockPos pPos,
      List<StructureTemplate.StructureBlockInfo> pOriginalBlockInfos,
      List<StructureTemplate.StructureBlockInfo> pProcessedBlockInfos,
      StructurePlaceSettings pSettings
   ) {
      return pProcessedBlockInfos;
   }

   public StructureTemplate.StructureEntityInfo processEntity(
      LevelReader world,
      BlockPos seedPos,
      StructureTemplate.StructureEntityInfo rawEntityInfo,
      StructureTemplate.StructureEntityInfo entityInfo,
      StructurePlaceSettings placementSettings,
      StructureTemplate template
   ) {
      return entityInfo;
   }

   @Nullable
   public StructureTemplate.StructureBlockInfo process(
      LevelReader level,
      BlockPos seed,
      BlockPos offset,
      StructureTemplate.StructureBlockInfo rawBlock,
      StructureTemplate.StructureBlockInfo block,
      StructurePlaceSettings settings,
      @Nullable StructureTemplate template
   ) {
      return this.processBlock(level, seed, offset, rawBlock, block, settings);
   }
}
