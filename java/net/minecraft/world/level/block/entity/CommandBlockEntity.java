package net.minecraft.world.level.block.entity;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class CommandBlockEntity extends BlockEntity {
   private boolean powered;
   private boolean auto;
   private boolean conditionMet;
   private final BaseCommandBlock commandBlock = new BaseCommandBlock() {
      @Override
      public void setCommand(String p_59157_) {
         super.setCommand(p_59157_);
         CommandBlockEntity.this.setChanged();
      }

      @Override
      public ServerLevel getLevel() {
         return (ServerLevel)CommandBlockEntity.this.level;
      }

      @Override
      public void onUpdated() {
         BlockState $$0 = CommandBlockEntity.this.level.getBlockState(CommandBlockEntity.this.worldPosition);
         this.getLevel().sendBlockUpdated(CommandBlockEntity.this.worldPosition, $$0, $$0, 3);
      }

      @Override
      public Vec3 getPosition() {
         return Vec3.atCenterOf(CommandBlockEntity.this.worldPosition);
      }

      @Override
      public CommandSourceStack createCommandSourceStack() {
         Direction $$0 = CommandBlockEntity.this.getBlockState().getValue(CommandBlock.FACING);
         return new CommandSourceStack(
            this,
            Vec3.atCenterOf(CommandBlockEntity.this.worldPosition),
            new Vec2(0.0F, $$0.toYRot()),
            this.getLevel(),
            2,
            this.getName().getString(),
            this.getName(),
            this.getLevel().getServer(),
            null
         );
      }

      @Override
      public boolean isValid() {
         return !CommandBlockEntity.this.isRemoved();
      }
   };

   public CommandBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super(BlockEntityType.COMMAND_BLOCK, pPos, pBlockState);
   }

   @Override
   protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.saveAdditional(pTag, pRegistries);
      this.commandBlock.save(pTag, pRegistries);
      pTag.putBoolean("powered", this.isPowered());
      pTag.putBoolean("conditionMet", this.wasConditionMet());
      pTag.putBoolean("auto", this.isAutomatic());
   }

   @Override
   protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.loadAdditional(pTag, pRegistries);
      this.commandBlock.load(pTag, pRegistries);
      this.powered = pTag.getBoolean("powered");
      this.conditionMet = pTag.getBoolean("conditionMet");
      this.setAutomatic(pTag.getBoolean("auto"));
   }

   @Override
   public boolean onlyOpCanSetNbt() {
      return true;
   }

   public BaseCommandBlock getCommandBlock() {
      return this.commandBlock;
   }

   public void setPowered(boolean pPowered) {
      this.powered = pPowered;
   }

   public boolean isPowered() {
      return this.powered;
   }

   public boolean isAutomatic() {
      return this.auto;
   }

   public void setAutomatic(boolean pAuto) {
      boolean $$1 = this.auto;
      this.auto = pAuto;
      if (!$$1 && pAuto && !this.powered && this.level != null && this.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
         this.scheduleTick();
      }
   }

   public void onModeSwitch() {
      CommandBlockEntity.Mode $$0 = this.getMode();
      if ($$0 == CommandBlockEntity.Mode.AUTO && (this.powered || this.auto) && this.level != null) {
         this.scheduleTick();
      }
   }

   private void scheduleTick() {
      Block $$0 = this.getBlockState().getBlock();
      if ($$0 instanceof CommandBlock) {
         this.markConditionMet();
         this.level.scheduleTick(this.worldPosition, $$0, 1);
      }
   }

   public boolean wasConditionMet() {
      return this.conditionMet;
   }

   public boolean markConditionMet() {
      this.conditionMet = true;
      if (this.isConditional()) {
         BlockPos $$0 = this.worldPosition.relative(this.level.getBlockState(this.worldPosition).getValue(CommandBlock.FACING).getOpposite());
         if (this.level.getBlockState($$0).getBlock() instanceof CommandBlock) {
            BlockEntity $$1 = this.level.getBlockEntity($$0);
            this.conditionMet = $$1 instanceof CommandBlockEntity && ((CommandBlockEntity)$$1).getCommandBlock().getSuccessCount() > 0;
         } else {
            this.conditionMet = false;
         }
      }

      return this.conditionMet;
   }

   public CommandBlockEntity.Mode getMode() {
      BlockState $$0 = this.getBlockState();
      if ($$0.is(Blocks.COMMAND_BLOCK)) {
         return CommandBlockEntity.Mode.REDSTONE;
      } else if ($$0.is(Blocks.REPEATING_COMMAND_BLOCK)) {
         return CommandBlockEntity.Mode.AUTO;
      } else {
         return $$0.is(Blocks.CHAIN_COMMAND_BLOCK) ? CommandBlockEntity.Mode.SEQUENCE : CommandBlockEntity.Mode.REDSTONE;
      }
   }

   public boolean isConditional() {
      BlockState $$0 = this.level.getBlockState(this.getBlockPos());
      return $$0.getBlock() instanceof CommandBlock ? $$0.getValue(CommandBlock.CONDITIONAL) : false;
   }

   @Override
   protected void applyImplicitComponents(BlockEntity.DataComponentInput pComponentInput) {
      super.applyImplicitComponents(pComponentInput);
      this.commandBlock.setCustomName(pComponentInput.get(DataComponents.CUSTOM_NAME));
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder pComponents) {
      super.collectImplicitComponents(pComponents);
      pComponents.set(DataComponents.CUSTOM_NAME, this.commandBlock.getCustomName());
   }

   @Override
   public void removeComponentsFromTag(CompoundTag pTag) {
      super.removeComponentsFromTag(pTag);
      pTag.remove("CustomName");
   }

   public static enum Mode {
      SEQUENCE,
      AUTO,
      REDSTONE;
   }
}
