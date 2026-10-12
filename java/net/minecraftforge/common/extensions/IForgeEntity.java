package net.minecraftforge.common.extensions;

import java.util.Collection;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.SoundAction;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

public interface IForgeEntity extends ICapabilitySerializable<CompoundTag> {
   private Entity self() {
      return (Entity)this;
   }

   default void deserializeNBT(HolderLookup.Provider registryAccess, CompoundTag nbt) {
      this.self().load(nbt);
   }

   default CompoundTag serializeNBT(HolderLookup.Provider registryAccess) {
      CompoundTag ret = new CompoundTag();
      String id = this.self().getEncodeId();
      if (id != null) {
         ret.putString("id", this.self().getEncodeId());
      }

      return this.self().saveWithoutId(ret);
   }

   boolean canUpdate();

   void canUpdate(boolean var1);

   @Nullable
   Collection<ItemEntity> captureDrops();

   Collection<ItemEntity> captureDrops(@Nullable Collection<ItemEntity> var1);

   CompoundTag getPersistentData();

   default boolean shouldRiderSit() {
      return true;
   }

   default ItemStack getPickedResult(HitResult target) {
      ItemStack result = this.self().getPickResult();
      if (result == null) {
         SpawnEggItem egg = ForgeSpawnEggItem.fromEntityType(this.self().getType());
         result = egg == null ? ItemStack.EMPTY : new ItemStack(egg);
      }

      return result;
   }

   default boolean canRiderInteract() {
      return false;
   }

   default boolean canBeRiddenUnderFluidType(FluidType type, Entity rider) {
      return type.canRideVehicleUnder(this.self(), rider);
   }

   boolean canTrample(BlockState var1, BlockPos var2, float var3);

   default MobCategory getClassification(boolean forSpawnCount) {
      return this.self().getType().getCategory();
   }

   boolean isAddedToWorld();

   void onAddedToWorld();

   void onRemovedFromWorld();

   void revive();

   default boolean isMultipartEntity() {
      return false;
   }

   @Nullable
   default PartEntity<?>[] getParts() {
      return null;
   }

   double getFluidTypeHeight(FluidType var1);

   FluidType getMaxHeightFluidType();

   default boolean isInFluidType(FluidState state) {
      return this.isInFluidType(state.getFluidType());
   }

   default boolean isInFluidType(FluidType type) {
      return this.getFluidTypeHeight(type) > 0.0;
   }

   default boolean isInFluidType(BiPredicate<FluidType, Double> predicate) {
      return this.isInFluidType(predicate, false);
   }

   boolean isInFluidType(BiPredicate<FluidType, Double> var1, boolean var2);

   boolean isInFluidType();

   FluidType getEyeInFluidType();

   default boolean isEyeInFluidType(FluidType type) {
      return type == this.getEyeInFluidType();
   }

   default boolean canStartSwimming() {
      return !this.getEyeInFluidType().isAir() && this.canSwimInFluidType(this.getEyeInFluidType());
   }

   default double getFluidMotionScale(FluidType type) {
      return type.motionScale(this.self());
   }

   default boolean isPushedByFluid(FluidType type) {
      return this.self().isPushedByFluid() && type.canPushEntity(this.self());
   }

   default boolean canSwimInFluidType(FluidType type) {
      return type.canSwim(this.self());
   }

   default boolean canFluidExtinguish(FluidType type) {
      return type.canExtinguish(this.self());
   }

   default float getFluidFallDistanceModifier(FluidType type) {
      return type.getFallDistanceModifier(this.self());
   }

   default boolean canHydrateInFluidType(FluidType type) {
      return type.canHydrate(this.self());
   }

   @Nullable
   default SoundEvent getSoundFromFluidType(FluidType type, SoundAction action) {
      return type.getSound(this.self(), action);
   }

   default boolean hasCustomOutlineRendering(Player player) {
      return false;
   }

   default boolean shouldUpdateFluidWhileBoating(FluidState state, Boat boat) {
      return boat.shouldUpdateFluidWhileRiding(state, this.self());
   }
}
