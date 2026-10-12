package net.minecraftforge.common;

import java.util.function.Supplier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public final class ForgeTier implements Tier {
   private final int uses;
   private final float speed;
   private final float attackDamageBonus;
   private final int enchantmentValue;
   @NotNull
   private final TagKey<Block> tag;
   @NotNull
   private final Supplier<Ingredient> repairIngredient;
   @NotNull
   public final TagKey<Block> incorrectBlocks;

   public ForgeTier(
      int uses,
      float speed,
      float attackDamageBonus,
      int enchantmentValue,
      @NotNull TagKey<Block> tag,
      @NotNull Supplier<Ingredient> repairIngredient,
      @NotNull TagKey<Block> incorrectBlocks
   ) {
      this.uses = uses;
      this.speed = speed;
      this.attackDamageBonus = attackDamageBonus;
      this.enchantmentValue = enchantmentValue;
      this.tag = tag;
      this.repairIngredient = repairIngredient;
      this.incorrectBlocks = incorrectBlocks;
   }

   @Override
   public int getUses() {
      return this.uses;
   }

   @Override
   public float getSpeed() {
      return this.speed;
   }

   @Override
   public float getAttackDamageBonus() {
      return this.attackDamageBonus;
   }

   @Override
   public int getEnchantmentValue() {
      return this.enchantmentValue;
   }

   @NotNull
   public TagKey<Block> getTag() {
      return this.tag;
   }

   @Override
   public TagKey<Block> getIncorrectBlocksForDrops() {
      return this.incorrectBlocks;
   }

   @NotNull
   @Override
   public Ingredient getRepairIngredient() {
      return this.repairIngredient.get();
   }

   @Override
   public String toString() {
      return "ForgeTier[uses="
         + this.uses
         + ", speed="
         + this.speed
         + ", attackDamageBonus="
         + this.attackDamageBonus
         + ", enchantmentValue="
         + this.enchantmentValue
         + ", tag="
         + this.tag
         + ", incorrect="
         + this.incorrectBlocks
         + ", repairIngredient="
         + this.repairIngredient
         + "]";
   }
}
