package net.minecraft.world.entity;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public interface NeutralMob {
   String TAG_ANGER_TIME = "AngerTime";
   String TAG_ANGRY_AT = "AngryAt";

   int getRemainingPersistentAngerTime();

   void setRemainingPersistentAngerTime(int pRemainingPersistentAngerTime);

   @Nullable
   UUID getPersistentAngerTarget();

   void setPersistentAngerTarget(@Nullable UUID pPersistentAngerTarget);

   void startPersistentAngerTimer();

   default void addPersistentAngerSaveData(CompoundTag pNbt) {
      pNbt.putInt("AngerTime", this.getRemainingPersistentAngerTime());
      if (this.getPersistentAngerTarget() != null) {
         pNbt.putUUID("AngryAt", this.getPersistentAngerTarget());
      }
   }

   default void readPersistentAngerSaveData(Level pLevel, CompoundTag pTag) {
      this.setRemainingPersistentAngerTime(pTag.getInt("AngerTime"));
      if (pLevel instanceof ServerLevel) {
         if (!pTag.hasUUID("AngryAt")) {
            this.setPersistentAngerTarget(null);
         } else {
            UUID $$2 = pTag.getUUID("AngryAt");
            this.setPersistentAngerTarget($$2);
            Entity $$3 = ((ServerLevel)pLevel).getEntity($$2);
            if ($$3 != null) {
               if ($$3 instanceof Mob $$4) {
                  this.setTarget($$4);
                  this.setLastHurtByMob($$4);
               }

               if ($$3 instanceof Player $$5) {
                  this.setTarget($$5);
                  this.setLastHurtByPlayer($$5);
               }
            }
         }
      }
   }

   default void updatePersistentAnger(ServerLevel pServerLevel, boolean pUpdateAnger) {
      LivingEntity $$2 = this.getTarget();
      UUID $$3 = this.getPersistentAngerTarget();
      if (($$2 == null || $$2.isDeadOrDying()) && $$3 != null && pServerLevel.getEntity($$3) instanceof Mob) {
         this.stopBeingAngry();
      } else {
         if ($$2 != null && !Objects.equals($$3, $$2.getUUID())) {
            this.setPersistentAngerTarget($$2.getUUID());
            this.startPersistentAngerTimer();
         }

         if (this.getRemainingPersistentAngerTime() > 0 && ($$2 == null || $$2.getType() != EntityType.PLAYER || !pUpdateAnger)) {
            this.setRemainingPersistentAngerTime(this.getRemainingPersistentAngerTime() - 1);
            if (this.getRemainingPersistentAngerTime() == 0) {
               this.stopBeingAngry();
            }
         }
      }
   }

   default boolean isAngryAt(LivingEntity pTarget) {
      if (!this.canAttack(pTarget)) {
         return false;
      } else {
         return pTarget.getType() == EntityType.PLAYER && this.isAngryAtAllPlayers(pTarget.level())
            ? true
            : pTarget.getUUID().equals(this.getPersistentAngerTarget());
      }
   }

   default boolean isAngryAtAllPlayers(Level pLevel) {
      return pLevel.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER) && this.isAngry() && this.getPersistentAngerTarget() == null;
   }

   default boolean isAngry() {
      return this.getRemainingPersistentAngerTime() > 0;
   }

   default void playerDied(Player pPlayer) {
      if (pPlayer.level().getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
         if (pPlayer.getUUID().equals(this.getPersistentAngerTarget())) {
            this.stopBeingAngry();
         }
      }
   }

   default void forgetCurrentTargetAndRefreshUniversalAnger() {
      this.stopBeingAngry();
      this.startPersistentAngerTimer();
   }

   default void stopBeingAngry() {
      this.setLastHurtByMob(null);
      this.setPersistentAngerTarget(null);
      this.setTarget(null);
      this.setRemainingPersistentAngerTime(0);
   }

   @Nullable
   LivingEntity getLastHurtByMob();

   void setLastHurtByMob(@Nullable LivingEntity pLivingEntity);

   void setLastHurtByPlayer(@Nullable Player pPlayer);

   void setTarget(@Nullable LivingEntity pLivingEntity);

   boolean canAttack(LivingEntity pEntity);

   @Nullable
   LivingEntity getTarget();
}
