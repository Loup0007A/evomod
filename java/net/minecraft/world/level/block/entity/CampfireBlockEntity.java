package net.minecraft.world.level.block.entity;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class CampfireBlockEntity extends BlockEntity implements Clearable {
   private static final int BURN_COOL_SPEED = 2;
   private static final int NUM_SLOTS = 4;
   private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
   private final int[] cookingProgress = new int[4];
   private final int[] cookingTime = new int[4];
   private final RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> quickCheck = RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);

   public CampfireBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super(BlockEntityType.CAMPFIRE, pPos, pBlockState);
   }

   public static void cookTick(Level pLevel, BlockPos pPos, BlockState pState, CampfireBlockEntity pBlockEntity) {
      boolean $$4 = false;

      for (int $$5 = 0; $$5 < pBlockEntity.items.size(); $$5++) {
         ItemStack $$6 = pBlockEntity.items.get($$5);
         if (!$$6.isEmpty()) {
            $$4 = true;
            pBlockEntity.cookingProgress[$$5]++;
            if (pBlockEntity.cookingProgress[$$5] >= pBlockEntity.cookingTime[$$5]) {
               SingleRecipeInput $$7 = new SingleRecipeInput($$6);
               ItemStack $$8 = pBlockEntity.quickCheck
                  .getRecipeFor($$7, pLevel)
                  .map(p_341839_ -> p_341839_.value().assemble($$7, pLevel.registryAccess()))
                  .orElse($$6);
               if ($$8.isItemEnabled(pLevel.enabledFeatures())) {
                  Containers.dropItemStack(pLevel, (double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ(), $$8);
                  pBlockEntity.items.set($$5, ItemStack.EMPTY);
                  pLevel.sendBlockUpdated(pPos, pState, pState, 3);
                  pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pState));
               }
            }
         }
      }

      if ($$4) {
         setChanged(pLevel, pPos, pState);
      }
   }

   public static void cooldownTick(Level pLevel, BlockPos pPos, BlockState pState, CampfireBlockEntity pBlockEntity) {
      boolean $$4 = false;

      for (int $$5 = 0; $$5 < pBlockEntity.items.size(); $$5++) {
         if (pBlockEntity.cookingProgress[$$5] > 0) {
            $$4 = true;
            pBlockEntity.cookingProgress[$$5] = Mth.clamp(pBlockEntity.cookingProgress[$$5] - 2, 0, pBlockEntity.cookingTime[$$5]);
         }
      }

      if ($$4) {
         setChanged(pLevel, pPos, pState);
      }
   }

   public static void particleTick(Level pLevel, BlockPos pPos, BlockState pState, CampfireBlockEntity pBlockEntity) {
      RandomSource $$4 = pLevel.random;
      if ($$4.nextFloat() < 0.11F) {
         for (int $$5 = 0; $$5 < $$4.nextInt(2) + 2; $$5++) {
            CampfireBlock.makeParticles(pLevel, pPos, pState.getValue(CampfireBlock.SIGNAL_FIRE), false);
         }
      }

      int $$6 = pState.getValue(CampfireBlock.FACING).get2DDataValue();

      for (int $$7 = 0; $$7 < pBlockEntity.items.size(); $$7++) {
         if (!pBlockEntity.items.get($$7).isEmpty() && $$4.nextFloat() < 0.2F) {
            Direction $$8 = Direction.from2DDataValue(Math.floorMod($$7 + $$6, 4));
            float $$9 = 0.3125F;
            double $$10 = (double)pPos.getX() + 0.5 - (double)((float)$$8.getStepX() * 0.3125F) + (double)((float)$$8.getClockWise().getStepX() * 0.3125F);
            double $$11 = (double)pPos.getY() + 0.5;
            double $$12 = (double)pPos.getZ() + 0.5 - (double)((float)$$8.getStepZ() * 0.3125F) + (double)((float)$$8.getClockWise().getStepZ() * 0.3125F);

            for (int $$13 = 0; $$13 < 4; $$13++) {
               pLevel.addParticle(ParticleTypes.SMOKE, $$10, $$11, $$12, 0.0, 5.0E-4, 0.0);
            }
         }
      }
   }

   public NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.loadAdditional(pTag, pRegistries);
      this.items.clear();
      ContainerHelper.loadAllItems(pTag, this.items, pRegistries);
      if (pTag.contains("CookingTimes", 11)) {
         int[] $$2 = pTag.getIntArray("CookingTimes");
         System.arraycopy($$2, 0, this.cookingProgress, 0, Math.min(this.cookingTime.length, $$2.length));
      }

      if (pTag.contains("CookingTotalTimes", 11)) {
         int[] $$3 = pTag.getIntArray("CookingTotalTimes");
         System.arraycopy($$3, 0, this.cookingTime, 0, Math.min(this.cookingTime.length, $$3.length));
      }
   }

   @Override
   protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.saveAdditional(pTag, pRegistries);
      ContainerHelper.saveAllItems(pTag, this.items, true, pRegistries);
      pTag.putIntArray("CookingTimes", this.cookingProgress);
      pTag.putIntArray("CookingTotalTimes", this.cookingTime);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
      CompoundTag $$1 = new CompoundTag();
      ContainerHelper.saveAllItems($$1, this.items, true, pRegistries);
      return $$1;
   }

   public Optional<RecipeHolder<CampfireCookingRecipe>> getCookableRecipe(ItemStack pStack) {
      return this.items.stream().noneMatch(ItemStack::isEmpty) ? Optional.empty() : this.quickCheck.getRecipeFor(new SingleRecipeInput(pStack), this.level);
   }

   public boolean placeFood(@Nullable LivingEntity pEntity, ItemStack pFood, int pCookTime) {
      for (int $$3 = 0; $$3 < this.items.size(); $$3++) {
         ItemStack $$4 = this.items.get($$3);
         if ($$4.isEmpty()) {
            this.cookingTime[$$3] = pCookTime;
            this.cookingProgress[$$3] = 0;
            this.items.set($$3, pFood.consumeAndReturn(1, pEntity));
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(pEntity, this.getBlockState()));
            this.markUpdated();
            return true;
         }
      }

      return false;
   }

   private void markUpdated() {
      this.setChanged();
      this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
   }

   @Override
   public void clearContent() {
      this.items.clear();
   }

   public void dowse() {
      if (this.level != null) {
         this.markUpdated();
      }
   }

   @Override
   protected void applyImplicitComponents(BlockEntity.DataComponentInput pComponentInput) {
      super.applyImplicitComponents(pComponentInput);
      pComponentInput.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.getItems());
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder pComponents) {
      super.collectImplicitComponents(pComponents);
      pComponents.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
   }

   @Override
   public void removeComponentsFromTag(CompoundTag pTag) {
      pTag.remove("Items");
   }
}
