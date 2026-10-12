package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MaceItem extends Item {
   private static final int DEFAULT_ATTACK_DAMAGE = 5;
   private static final float DEFAULT_ATTACK_SPEED = -3.4F;
   public static final float SMASH_ATTACK_FALL_THRESHOLD = 1.5F;
   private static final float SMASH_ATTACK_HEAVY_THRESHOLD = 5.0F;
   public static final float SMASH_ATTACK_KNOCKBACK_RADIUS = 3.5F;
   private static final float SMASH_ATTACK_KNOCKBACK_POWER = 0.7F;

   public MaceItem(Item.Properties pProperties) {
      super(pProperties);
   }

   public static ItemAttributeModifiers createAttributes() {
      return ItemAttributeModifiers.builder()
         .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.4F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
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
   public int getEnchantmentValue() {
      return 15;
   }

   @Override
   public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
      if (pAttacker instanceof ServerPlayer $$3 && canSmashAttack($$3)) {
         ServerLevel $$4 = (ServerLevel)pAttacker.level();
         if ($$3.isIgnoringFallDamageFromCurrentImpulse() && $$3.currentImpulseImpactPos != null) {
            if ($$3.currentImpulseImpactPos.y > $$3.position().y) {
               $$3.currentImpulseImpactPos = $$3.position();
            }
         } else {
            $$3.currentImpulseImpactPos = $$3.position();
         }

         $$3.setIgnoreFallDamageFromCurrentImpulse(true);
         $$3.setDeltaMovement($$3.getDeltaMovement().with(Direction.Axis.Y, 0.01F));
         $$3.connection.send(new ClientboundSetEntityMotionPacket($$3));
         if (pTarget.onGround()) {
            $$3.setSpawnExtraParticlesOnFall(true);
            SoundEvent $$5 = $$3.fallDistance > 5.0F ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND;
            $$4.playSound(null, $$3.getX(), $$3.getY(), $$3.getZ(), $$5, $$3.getSoundSource(), 1.0F, 1.0F);
         } else {
            $$4.playSound(null, $$3.getX(), $$3.getY(), $$3.getZ(), SoundEvents.MACE_SMASH_AIR, $$3.getSoundSource(), 1.0F, 1.0F);
         }

         knockback($$4, $$3, pTarget);
      }

      return true;
   }

   @Override
   public void postHurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
      pStack.hurtAndBreak(1, pAttacker, EquipmentSlot.MAINHAND);
      if (canSmashAttack(pAttacker)) {
         pAttacker.resetFallDistance();
      }
   }

   @Override
   public boolean isValidRepairItem(ItemStack pStack, ItemStack pRepairCandidate) {
      return pRepairCandidate.is(Items.BREEZE_ROD);
   }

   @Override
   public float getAttackDamageBonus(Entity pTarget, float pDamage, DamageSource pDamageSource) {
      if (pDamageSource.getDirectEntity() instanceof LivingEntity $$3) {
         if (!canSmashAttack($$3)) {
            return 0.0F;
         } else {
            float $$5 = 3.0F;
            float $$6 = 8.0F;
            float $$7 = $$3.fallDistance;
            float $$8;
            if ($$7 <= 3.0F) {
               $$8 = 4.0F * $$7;
            } else if ($$7 <= 8.0F) {
               $$8 = 12.0F + 2.0F * ($$7 - 3.0F);
            } else {
               $$8 = 22.0F + $$7 - 8.0F;
            }

            return $$3.level() instanceof ServerLevel $$11
               ? $$8 + EnchantmentHelper.modifyFallBasedDamage($$11, $$3.getWeaponItem(), pTarget, pDamageSource, 0.0F) * $$7
               : $$8;
         }
      } else {
         return 0.0F;
      }
   }

   private static void knockback(Level pLevel, Player pPlayer, Entity pEntity) {
      pLevel.levelEvent(2013, pEntity.getOnPos(), 750);
      pLevel.getEntitiesOfClass(LivingEntity.class, pEntity.getBoundingBox().inflate(3.5), knockbackPredicate(pPlayer, pEntity)).forEach(p_341573_ -> {
         Vec3 $$3 = p_341573_.position().subtract(pEntity.position());
         double $$4 = getKnockbackPower(pPlayer, p_341573_, $$3);
         Vec3 $$5 = $$3.normalize().scale($$4);
         if ($$4 > 0.0) {
            p_341573_.push($$5.x, 0.7F, $$5.z);
            if (p_341573_ instanceof ServerPlayer $$6) {
               $$6.connection.send(new ClientboundSetEntityMotionPacket($$6));
            }
         }
      });
   }

   private static Predicate<LivingEntity> knockbackPredicate(Player pPlayer, Entity pEntity) {
      return p_341576_ -> {
         boolean $$3;
         boolean $$4;
         boolean $$5;
         boolean var10000;
         label62: {
            $$3 = !p_341576_.isSpectator();
            $$4 = p_341576_ != pPlayer && p_341576_ != pEntity;
            $$5 = !pPlayer.isAlliedTo(p_341576_);
            if (p_341576_ instanceof TamableAnimal $$6 && $$6.isTame() && pPlayer.getUUID().equals($$6.getOwnerUUID())) {
               var10000 = true;
               break label62;
            }

            var10000 = false;
         }

         boolean $$7;
         label55: {
            $$7 = !var10000;
            if (p_341576_ instanceof ArmorStand $$8 && $$8.isMarker()) {
               var10000 = false;
               break label55;
            }

            var10000 = true;
         }

         boolean $$10 = var10000;
         boolean $$11 = pEntity.distanceToSqr(p_341576_) <= Math.pow(3.5, 2.0);
         return $$3 && $$4 && $$5 && $$7 && $$10 && $$11;
      };
   }

   private static double getKnockbackPower(Player pPlayer, LivingEntity pEntity, Vec3 pEntityPos) {
      return (3.5 - pEntityPos.length())
         * 0.7F
         * (double)(pPlayer.fallDistance > 5.0F ? 2 : 1)
         * (1.0 - pEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
   }

   public static boolean canSmashAttack(LivingEntity pEntity) {
      return pEntity.fallDistance > 1.5F && !pEntity.isFallFlying();
   }
}
