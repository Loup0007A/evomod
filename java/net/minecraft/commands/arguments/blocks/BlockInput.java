package net.minecraft.commands.arguments.blocks;

import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockInput implements Predicate<BlockInWorld> {
   private final BlockState state;
   private final Set<Property<?>> properties;
   @Nullable
   private final CompoundTag tag;

   public BlockInput(BlockState pState, Set<Property<?>> pProperties, @Nullable CompoundTag pTag) {
      this.state = pState;
      this.properties = pProperties;
      this.tag = pTag;
   }

   public BlockState getState() {
      return this.state;
   }

   public Set<Property<?>> getDefinedProperties() {
      return this.properties;
   }

   public boolean test(BlockInWorld pBlock) {
      BlockState $$1 = pBlock.getState();
      if (!$$1.is(this.state.getBlock())) {
         return false;
      } else {
         for (Property<?> $$2 : this.properties) {
            if ($$1.getValue($$2) != this.state.getValue($$2)) {
               return false;
            }
         }

         if (this.tag == null) {
            return true;
         } else {
            BlockEntity $$3 = pBlock.getEntity();
            return $$3 != null && NbtUtils.compareNbt(this.tag, $$3.saveWithFullMetadata(pBlock.getLevel().registryAccess()), true);
         }
      }
   }

   public boolean test(ServerLevel pLevel, BlockPos pPos) {
      return this.test(new BlockInWorld(pLevel, pPos, false));
   }

   public boolean place(ServerLevel pLevel, BlockPos pPos, int pFlags) {
      BlockState $$3 = Block.updateFromNeighbourShapes(this.state, pLevel, pPos);
      if ($$3.isAir()) {
         $$3 = this.state;
      }

      if (!pLevel.setBlock(pPos, $$3, pFlags)) {
         return false;
      } else {
         if (this.tag != null) {
            BlockEntity $$4 = pLevel.getBlockEntity(pPos);
            if ($$4 != null) {
               $$4.loadWithComponents(this.tag, pLevel.registryAccess());
            }
         }

         return true;
      }
   }
}
