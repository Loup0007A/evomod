package net.minecraft.world.entity.animal;

import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public interface Bucketable {
   boolean fromBucket();

   void setFromBucket(boolean pFromBucket);

   void saveToBucketTag(ItemStack pStack);

   void loadFromBucketTag(CompoundTag pTag);

   ItemStack getBucketItemStack();

   SoundEvent getPickupSound();

   @Deprecated
   static void saveDefaultDataToBucketTag(Mob pMob, ItemStack pBucket) {
      pBucket.set(DataComponents.CUSTOM_NAME, pMob.getCustomName());
      CustomData.update(DataComponents.BUCKET_ENTITY_DATA, pBucket, p_335779_ -> {
         if (pMob.isNoAi()) {
            p_335779_.putBoolean("NoAI", pMob.isNoAi());
         }

         if (pMob.isSilent()) {
            p_335779_.putBoolean("Silent", pMob.isSilent());
         }

         if (pMob.isNoGravity()) {
            p_335779_.putBoolean("NoGravity", pMob.isNoGravity());
         }

         if (pMob.hasGlowingTag()) {
            p_335779_.putBoolean("Glowing", pMob.hasGlowingTag());
         }

         if (pMob.isInvulnerable()) {
            p_335779_.putBoolean("Invulnerable", pMob.isInvulnerable());
         }

         p_335779_.putFloat("Health", pMob.getHealth());
      });
   }

   @Deprecated
   static void loadDefaultDataFromBucketTag(Mob pMob, CompoundTag pTag) {
      if (pTag.contains("NoAI")) {
         pMob.setNoAi(pTag.getBoolean("NoAI"));
      }

      if (pTag.contains("Silent")) {
         pMob.setSilent(pTag.getBoolean("Silent"));
      }

      if (pTag.contains("NoGravity")) {
         pMob.setNoGravity(pTag.getBoolean("NoGravity"));
      }

      if (pTag.contains("Glowing")) {
         pMob.setGlowingTag(pTag.getBoolean("Glowing"));
      }

      if (pTag.contains("Invulnerable")) {
         pMob.setInvulnerable(pTag.getBoolean("Invulnerable"));
      }

      if (pTag.contains("Health", 99)) {
         pMob.setHealth(pTag.getFloat("Health"));
      }
   }

   static <T extends LivingEntity & Bucketable> Optional<InteractionResult> bucketMobPickup(Player pPlayer, InteractionHand pHand, T pEntity) {
      ItemStack $$3 = pPlayer.getItemInHand(pHand);
      if ($$3.getItem() == Items.WATER_BUCKET && pEntity.isAlive()) {
         pEntity.playSound(pEntity.getPickupSound(), 1.0F, 1.0F);
         ItemStack $$4 = pEntity.getBucketItemStack();
         pEntity.saveToBucketTag($$4);
         ItemStack $$5 = ItemUtils.createFilledResult($$3, pPlayer, $$4, false);
         pPlayer.setItemInHand(pHand, $$5);
         Level $$6 = pEntity.level();
         if (!$$6.isClientSide) {
            CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)pPlayer, $$4);
         }

         pEntity.discard();
         return Optional.of(InteractionResult.sidedSuccess($$6.isClientSide));
      } else {
         return Optional.empty();
      }
   }
}
