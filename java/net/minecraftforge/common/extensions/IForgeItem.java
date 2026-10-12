package net.minecraftforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ToolAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IForgeItem {
   private Item self() {
      return (Item)this;
   }

   @Deprecated(
      forRemoval = true,
      since = "1.21"
   )
   default ItemAttributeModifiers getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
      return this.getDefaultAttributeModifiers(stack);
   }

   default ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
      return this.self().getDefaultAttributeModifiers();
   }

   default boolean onDroppedByPlayer(ItemStack item, Player player) {
      return true;
   }

   default Component getHighlightTip(ItemStack item, Component displayName) {
      return displayName;
   }

   default InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
      return InteractionResult.PASS;
   }

   default boolean isPiglinCurrency(ItemStack stack) {
      return stack.getItem() == PiglinAi.BARTERING_ITEM;
   }

   default boolean makesPiglinsNeutral(ItemStack stack, LivingEntity wearer) {
      return stack.getItem() instanceof ArmorItem && ((ArmorItem)stack.getItem()).getMaterial() == ArmorMaterials.GOLD;
   }

   default boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, Player player) {
      return false;
   }

   default void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
   }

   default boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
      return false;
   }

   default ItemStack getCraftingRemainingItem(ItemStack itemStack) {
      return !this.hasCraftingRemainingItem(itemStack) ? ItemStack.EMPTY : new ItemStack(this.self().getCraftingRemainingItem());
   }

   default boolean hasCraftingRemainingItem(ItemStack stack) {
      return this.self().hasCraftingRemainingItem();
   }

   default int getEntityLifespan(ItemStack itemStack, Level level) {
      return 6000;
   }

   default boolean hasCustomEntity(ItemStack stack) {
      return false;
   }

   @Nullable
   default Entity createEntity(Level level, Entity location, ItemStack stack) {
      return null;
   }

   default boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
      return false;
   }

   default boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
      return false;
   }

   default void onInventoryTick(ItemStack stack, Level level, Player player, int slotIndex, int selectedIndex) {
      Inventory inv = player.getInventory();
      int vanillaIndex = slotIndex;
      if (slotIndex >= inv.items.size()) {
         vanillaIndex = slotIndex - inv.items.size();
         if (vanillaIndex >= inv.armor.size()) {
            vanillaIndex -= inv.armor.size();
         }
      }

      stack.inventoryTick(level, player, vanillaIndex, selectedIndex == vanillaIndex);
   }

   default boolean canEquip(ItemStack stack, EquipmentSlot armorType, Entity entity) {
      return entity instanceof LivingEntity living ? living.getEquipmentSlotForItem(stack) == armorType : false;
   }

   @Nullable
   default EquipmentSlot getEquipmentSlot(ItemStack stack) {
      return null;
   }

   default boolean isBookEnchantable(ItemStack stack, ItemStack book) {
      return true;
   }

   @Nullable
   default ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean inner) {
      return null;
   }

   default boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
      return false;
   }

   default boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
      return false;
   }

   default int getEnchantmentValue(ItemStack stack) {
      return this.self().getEnchantmentValue();
   }

   default boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
      return enchantment.isPrimaryItem(stack);
   }

   default boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      return !oldStack.equals(newStack);
   }

   default boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
      return !newStack.is(oldStack.getItem()) ? true : !ItemStack.isSameItemSameComponents(oldStack, newStack);
   }

   default boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
      return oldStack == newStack ? true : !oldStack.isEmpty() && !newStack.isEmpty() && ItemStack.isSameItem(newStack, oldStack);
   }

   @Nullable
   default String getCreatorModId(ItemStack itemStack) {
      return ForgeHooks.getDefaultCreatorModId(itemStack);
   }

   default boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
      return this instanceof AxeItem;
   }

   default int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
      return -1;
   }

   default void onHorseArmorTick(ItemStack stack, Level level, Mob horse) {
   }

   default void onDestroyed(ItemEntity itemEntity, DamageSource damageSource) {
      this.self().onDestroyed(itemEntity);
   }

   default boolean isEnderMask(ItemStack stack, Player player, EnderMan endermanEntity) {
      return stack.getItem() == Blocks.CARVED_PUMPKIN.asItem();
   }

   default boolean canElytraFly(ItemStack stack, LivingEntity entity) {
      return false;
   }

   default boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
      return false;
   }

   default boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer) {
      return stack.is(Items.LEATHER_BOOTS);
   }

   @NotNull
   default AABB getSweepHitBox(@NotNull ItemStack stack, @NotNull Player player, @NotNull Entity target) {
      return target.getBoundingBox().inflate(1.0, 0.25, 1.0);
   }

   default int getDefaultTooltipHideFlags(@NotNull ItemStack stack) {
      return 0;
   }

   default boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int inventorySlot) {
      return stack.isEnchanted();
   }

   default boolean canGrindstoneRepair(ItemStack stack) {
      return false;
   }
}
