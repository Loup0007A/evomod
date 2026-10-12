package net.minecraft.world.damagesource;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class CombatRules {
   public static final float MAX_ARMOR = 20.0F;
   public static final float ARMOR_PROTECTION_DIVIDER = 25.0F;
   public static final float BASE_ARMOR_TOUGHNESS = 2.0F;
   public static final float MIN_ARMOR_RATIO = 0.2F;
   private static final int NUM_ARMOR_ITEMS = 4;

   public static float getDamageAfterAbsorb(LivingEntity pEntity, float pDamage, DamageSource pDamageSource, float pArmorValue, float pArmorToughness) {
      float $$5 = 2.0F + pArmorToughness / 4.0F;
      float $$6 = Mth.clamp(pArmorValue - pDamage / $$5, pArmorValue * 0.2F, 20.0F);
      float $$7 = $$6 / 25.0F;
      ItemStack $$8 = pDamageSource.getWeaponItem();
      float $$10;
      if ($$8 != null && pEntity.level() instanceof ServerLevel $$9) {
         $$10 = Mth.clamp(EnchantmentHelper.modifyArmorEffectiveness($$9, $$8, pEntity, pDamageSource, $$7), 0.0F, 1.0F);
      } else {
         $$10 = $$7;
      }

      float $$12 = 1.0F - $$10;
      return pDamage * $$12;
   }

   public static float getDamageAfterMagicAbsorb(float pDamage, float pEnchantModifiers) {
      float $$2 = Mth.clamp(pEnchantModifiers, 0.0F, 20.0F);
      return pDamage * (1.0F - $$2 / 25.0F);
   }
}
