package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TridentItem extends Item implements ProjectileItem {
   public static final int THROW_THRESHOLD_TIME = 10;
   public static final float BASE_DAMAGE = 8.0F;
   public static final float SHOOT_POWER = 2.5F;

   public TridentItem(Item.Properties pProperties) {
      super(pProperties);
   }

   public static ItemAttributeModifiers createAttributes() {
      return ItemAttributeModifiers.builder()
         .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 8.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.9F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .build();
   }

   public static Tool createToolProperties() {
      return new Tool(List.of(), 1.0F, 2);
   }

   @Override
   public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
      return !pPlayer.isCreative();
   }

   @Override
   public UseAnim getUseAnimation(ItemStack pStack) {
      return UseAnim.SPEAR;
   }

   @Override
   public int getUseDuration(ItemStack pStack, LivingEntity pEntity) {
      return 72000;
   }

   @Override
   public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving, int pTimeLeft) {
      if (pEntityLiving instanceof Player $$4) {
         int $$6 = this.getUseDuration(pStack, pEntityLiving) - pTimeLeft;
         if ($$6 >= 10) {
            float $$7 = EnchantmentHelper.getTridentSpinAttackStrength(pStack, $$4);
            if (!($$7 > 0.0F) || $$4.isInWaterOrRain()) {
               if (!isTooDamagedToUse(pStack)) {
                  Holder<SoundEvent> $$8 = EnchantmentHelper.pickHighestLevel(pStack, EnchantmentEffectComponents.TRIDENT_SOUND)
                     .orElse(SoundEvents.TRIDENT_THROW);
                  if (!pLevel.isClientSide) {
                     pStack.hurtAndBreak(1, $$4, LivingEntity.getSlotForHand(pEntityLiving.getUsedItemHand()));
                     if ($$7 == 0.0F) {
                        ThrownTrident $$9 = new ThrownTrident(pLevel, $$4, pStack);
                        $$9.shootFromRotation($$4, $$4.getXRot(), $$4.getYRot(), 0.0F, 2.5F, 1.0F);
                        if ($$4.hasInfiniteMaterials()) {
                           $$9.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                        }

                        pLevel.addFreshEntity($$9);
                        pLevel.playSound(null, $$9, $$8.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                        if (!$$4.hasInfiniteMaterials()) {
                           $$4.getInventory().removeItem(pStack);
                        }
                     }
                  }

                  $$4.awardStat(Stats.ITEM_USED.get(this));
                  if ($$7 > 0.0F) {
                     float $$10 = $$4.getYRot();
                     float $$11 = $$4.getXRot();
                     float $$12 = -Mth.sin($$10 * (float) (Math.PI / 180.0)) * Mth.cos($$11 * (float) (Math.PI / 180.0));
                     float $$13 = -Mth.sin($$11 * (float) (Math.PI / 180.0));
                     float $$14 = Mth.cos($$10 * (float) (Math.PI / 180.0)) * Mth.cos($$11 * (float) (Math.PI / 180.0));
                     float $$15 = Mth.sqrt($$12 * $$12 + $$13 * $$13 + $$14 * $$14);
                     $$12 *= $$7 / $$15;
                     $$13 *= $$7 / $$15;
                     $$14 *= $$7 / $$15;
                     $$4.push((double)$$12, (double)$$13, (double)$$14);
                     $$4.startAutoSpinAttack(20, 8.0F, pStack);
                     if ($$4.onGround()) {
                        float $$16 = 1.1999999F;
                        $$4.move(MoverType.SELF, new Vec3(0.0, 1.1999999F, 0.0));
                     }

                     pLevel.playSound(null, $$4, $$8.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                  }
               }
            }
         }
      }
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      ItemStack $$3 = pPlayer.getItemInHand(pHand);
      if (isTooDamagedToUse($$3)) {
         return InteractionResultHolder.fail($$3);
      } else if (EnchantmentHelper.getTridentSpinAttackStrength($$3, pPlayer) > 0.0F && !pPlayer.isInWaterOrRain()) {
         return InteractionResultHolder.fail($$3);
      } else {
         pPlayer.startUsingItem(pHand);
         return InteractionResultHolder.consume($$3);
      }
   }

   private static boolean isTooDamagedToUse(ItemStack pStack) {
      return pStack.getDamageValue() >= pStack.getMaxDamage() - 1;
   }

   @Override
   public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
      return true;
   }

   @Override
   public void postHurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
      pStack.hurtAndBreak(1, pAttacker, EquipmentSlot.MAINHAND);
   }

   @Override
   public int getEnchantmentValue() {
      return 1;
   }

   @Override
   public Projectile asProjectile(Level pLevel, Position pPos, ItemStack pStack, Direction pDirection) {
      ThrownTrident $$4 = new ThrownTrident(pLevel, pPos.x(), pPos.y(), pPos.z(), pStack.copyWithCount(1));
      $$4.pickup = AbstractArrow.Pickup.ALLOWED;
      return $$4;
   }
}
