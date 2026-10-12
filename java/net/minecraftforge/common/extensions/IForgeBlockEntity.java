package net.minecraftforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelDataManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;

public interface IForgeBlockEntity extends ICapabilityProvider {
   AABB INFINITE_EXTENT_AABB = new AABB(
      Double.NEGATIVE_INFINITY,
      Double.NEGATIVE_INFINITY,
      Double.NEGATIVE_INFINITY,
      Double.POSITIVE_INFINITY,
      Double.POSITIVE_INFINITY,
      Double.POSITIVE_INFINITY
   );

   private BlockEntity self() {
      return (BlockEntity)this;
   }

   default void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookup) {
      CompoundTag compoundtag = pkt.getTag();
      if (compoundtag != null) {
         this.self().loadWithComponents(compoundtag, lookup);
      }
   }

   default void handleUpdateTag(CompoundTag tag, HolderLookup.Provider holders) {
      this.self().loadWithComponents(tag, holders);
   }

   default void onChunkUnloaded() {
   }

   default void onLoad() {
      this.requestModelDataUpdate();
   }

   default AABB getRenderBoundingBox() {
      AABB bb = INFINITE_EXTENT_AABB;
      BlockState state = this.self().getBlockState();
      Block block = state.getBlock();
      BlockPos pos = this.self().getBlockPos();
      if (block == Blocks.ENCHANTING_TABLE) {
         bb = new AABB(pos);
      } else if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
         bb = AABB.encapsulatingFullBlocks(pos.offset(-1, 0, -1), pos.offset(1, 0, 1));
      } else if (block == Blocks.STRUCTURE_BLOCK) {
         bb = INFINITE_EXTENT_AABB;
      } else if (block != null && block != Blocks.BEACON) {
         AABB cbb = null;

         try {
            VoxelShape collisionShape = state.getCollisionShape(this.self().getLevel(), pos);
            if (!collisionShape.isEmpty()) {
               cbb = collisionShape.bounds().move(pos);
            }
         } catch (Exception var7) {
            cbb = AABB.encapsulatingFullBlocks(pos.offset(-1, 0, -1), pos.offset(1, 1, 1));
         }

         if (cbb != null) {
            bb = cbb;
         }
      }

      return bb;
   }

   default void requestModelDataUpdate() {
      BlockEntity te = this.self();
      Level level = te.getLevel();
      if (level != null && level.isClientSide) {
         ModelDataManager modelDataManager = level.getModelDataManager();
         if (modelDataManager != null) {
            modelDataManager.requestRefresh(te);
         }
      }
   }

   @NotNull
   default ModelData getModelData() {
      return ModelData.EMPTY;
   }

   default boolean hasCustomOutlineRendering(Player player) {
      return false;
   }
}
