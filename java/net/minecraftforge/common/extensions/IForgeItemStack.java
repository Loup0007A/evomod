package net.minecraftforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ToolAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IForgeItemStack {
   private ItemStack self() {
      return (ItemStack)this;
   }

   default ItemStack getCraftingRemainingItem() {
      return this.self().getItem().getCraftingRemainingItem(this.self());
   }

   default boolean hasCraftingRemainingItem() {
      return this.self().getItem().hasCraftingRemainingItem(this.self());
   }

   default int getBurnTime(@Nullable RecipeType<?> recipeType) {
      return this.self().getItem().getBurnTime(this.self(), recipeType);
   }

   default InteractionResult onItemUseFirst(UseOnContext context) {
      Player entityplayer = context.getPlayer();
      BlockPos blockpos = context.getClickedPos();
      BlockInWorld blockworldstate = new BlockInWorld(context.getLevel(), blockpos, false);
      boolean adventure = this.self().canBreakBlockInAdventureMode(blockworldstate) || this.self().canPlaceOnBlockInAdventureMode(blockworldstate);
      if (entityplayer != null && !entityplayer.getAbilities().mayBuild && !adventure) {
         return InteractionResult.PASS;
      } else {
         Item item = this.self().getItem();
         InteractionResult enumactionresult = item.onItemUseFirst(this.self(), context);
         if (entityplayer != null && enumactionresult == InteractionResult.SUCCESS) {
            entityplayer.awardStat(Stats.ITEM_USED.get(item));
         }

         return enumactionresult;
      }
   }

   default boolean canPerformAction(ToolAction toolAction) {
      return this.self().getItem().canPerformAction(this.self(), toolAction);
   }

   default boolean onBlockStartBreak(BlockPos pos, Player player) {
      return !this.self().isEmpty() && this.self().getItem().onBlockStartBreak(this.self(), pos, player);
   }

   default boolean shouldCauseBlockBreakReset(ItemStack newStack) {
      return this.self().getItem().shouldCauseBlockBreakReset(this.self(), newStack);
   }

   default boolean canApplyAtEnchantingTable(Enchantment enchantment) {
      return this.self().getItem().canApplyAtEnchantingTable(this.self(), enchantment);
   }

   default int getEnchantmentValue() {
      return this.self().getItem().getEnchantmentValue(this.self());
   }

   @Nullable
   default EquipmentSlot getEquipmentSlot() {
      return this.self().getItem().getEquipmentSlot(this.self());
   }

   default boolean canDisableShield(ItemStack shield, LivingEntity entity, LivingEntity attacker) {
      return this.self().getItem().canDisableShield(this.self(), shield, entity, attacker);
   }

   default boolean onEntitySwing(LivingEntity entity) {
      return this.self().getItem().onEntitySwing(this.self(), entity);
   }

   default void onStopUsing(LivingEntity entity, int count) {
      this.self().getItem().onStopUsing(this.self(), entity, count);
   }

   default int getEntityLifespan(Level level) {
      return this.self().getItem().getEntityLifespan(this.self(), level);
   }

   default boolean onEntityItemUpdate(ItemEntity entity) {
      return this.self().getItem().onEntityItemUpdate(this.self(), entity);
   }

   default void onInventoryTick(Level level, Player player, int slotIndex, int selectedIndex) {
      this.self().getItem().onInventoryTick(this.self(), level, player, slotIndex, selectedIndex);
   }

   default void onHorseArmorTick(Level level, Mob horse) {
      this.self().getItem().onHorseArmorTick(this.self(), level, horse);
   }

   default boolean canEquip(EquipmentSlot armorType, Entity entity) {
      return this.self().getItem().canEquip(this.self(), armorType, entity);
   }

   default boolean isBookEnchantable(ItemStack book) {
      return this.self().getItem().isBookEnchantable(this.self(), book);
   }

   default boolean onDroppedByPlayer(Player player) {
      return this.self().getItem().onDroppedByPlayer(this.self(), player);
   }

   default Component getHighlightTip(Component displayName) {
      return this.self().getItem().getHighlightTip(this.self(), displayName);
   }

   default boolean doesSneakBypassUse(LevelReader level, BlockPos pos, Player player) {
      return this.self().isEmpty() || this.self().getItem().doesSneakBypassUse(this.self(), level, pos, player);
   }

   default boolean isPiglinCurrency() {
      return this.self().getItem().isPiglinCurrency(this.self());
   }

   default boolean makesPiglinsNeutral(LivingEntity wearer) {
      return this.self().getItem().makesPiglinsNeutral(this.self(), wearer);
   }

   default boolean isEnderMask(Player player, EnderMan endermanEntity) {
      return this.self().getItem().isEnderMask(this.self(), player, endermanEntity);
   }

   default boolean canElytraFly(LivingEntity entity) {
      return this.self().getItem().canElytraFly(this.self(), entity);
   }

   default boolean elytraFlightTick(LivingEntity entity, int flightTicks) {
      return this.self().getItem().elytraFlightTick(this.self(), entity, flightTicks);
   }

   default boolean canWalkOnPowderedSnow(LivingEntity wearer) {
      return this.self().getItem().canWalkOnPowderedSnow(this.self(), wearer);
   }

   @NotNull
   default AABB getSweepHitBox(@NotNull Player player, @NotNull Entity target) {
      return this.self().getItem().getSweepHitBox(this.self(), player, target);
   }

   default void onDestroyed(ItemEntity itemEntity, DamageSource damageSource) {
      this.self().getItem().onDestroyed(itemEntity, damageSource);
   }

   default boolean isNotReplaceableByPickAction(Player player, int inventorySlot) {
      return this.self().getItem().isNotReplaceableByPickAction(this.self(), player, inventorySlot);
   }

   default boolean canGrindstoneRepair() {
      return this.self().getItem().canGrindstoneRepair(this.self());
   }

   default ItemAttributeModifiers getDefaultAttributeModifiers() {
      return this.self().getItem().getDefaultAttributeModifiers(this.self());
   }
}
