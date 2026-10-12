package net.minecraftforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.client.model.data.ModelDataManager;
import org.jetbrains.annotations.Nullable;

public interface IForgeBlockGetter {
   private BlockGetter self() {
      return (BlockGetter)this;
   }

   @Nullable
   default BlockEntity getExistingBlockEntity(BlockPos pos) {
      if (this instanceof Level level) {
         return !level.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))
            ? null
            : level.getChunk(pos).getExistingBlockEntity(pos);
      } else if (this instanceof LevelChunk chunk) {
         return chunk.getBlockEntities().get(pos);
      } else {
         return this instanceof ImposterProtoChunk chunk ? chunk.getWrapped().getExistingBlockEntity(pos) : this.self().getBlockEntity(pos);
      }
   }

   @Nullable
   default ModelDataManager getModelDataManager() {
      return null;
   }
}
