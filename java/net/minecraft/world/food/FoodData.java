package net.minecraft.world.food;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;

public class FoodData {
   private int foodLevel = 20;
   private float saturationLevel;
   private float exhaustionLevel;
   private int tickTimer;
   private int lastFoodLevel = 20;

   public FoodData() {
      this.saturationLevel = 5.0F;
   }

   private void add(int pFoodLevel, float pSaturationLevel) {
      this.foodLevel = Mth.clamp(pFoodLevel + this.foodLevel, 0, 20);
      this.saturationLevel = Mth.clamp(pSaturationLevel + this.saturationLevel, 0.0F, (float)this.foodLevel);
   }

   public void eat(int pFoodLevelModifier, float pSaturationLevelModifier) {
      this.add(pFoodLevelModifier, FoodConstants.saturationByModifier(pFoodLevelModifier, pSaturationLevelModifier));
   }

   public void eat(FoodProperties pFoodProperties) {
      this.add(pFoodProperties.nutrition(), pFoodProperties.saturation());
   }

   public void tick(Player pPlayer) {
      Difficulty $$1 = pPlayer.level().getDifficulty();
      this.lastFoodLevel = this.foodLevel;
      if (this.exhaustionLevel > 4.0F) {
         this.exhaustionLevel -= 4.0F;
         if (this.saturationLevel > 0.0F) {
            this.saturationLevel = Math.max(this.saturationLevel - 1.0F, 0.0F);
         } else if ($$1 != Difficulty.PEACEFUL) {
            this.foodLevel = Math.max(this.foodLevel - 1, 0);
         }
      }

      boolean $$2 = pPlayer.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
      if ($$2 && this.saturationLevel > 0.0F && pPlayer.isHurt() && this.foodLevel >= 20) {
         this.tickTimer++;
         if (this.tickTimer >= 10) {
            float $$3 = Math.min(this.saturationLevel, 6.0F);
            pPlayer.heal($$3 / 6.0F);
            this.addExhaustion($$3);
            this.tickTimer = 0;
         }
      } else if ($$2 && this.foodLevel >= 18 && pPlayer.isHurt()) {
         this.tickTimer++;
         if (this.tickTimer >= 80) {
            pPlayer.heal(1.0F);
            this.addExhaustion(6.0F);
            this.tickTimer = 0;
         }
      } else if (this.foodLevel <= 0) {
         this.tickTimer++;
         if (this.tickTimer >= 80) {
            if (pPlayer.getHealth() > 10.0F || $$1 == Difficulty.HARD || pPlayer.getHealth() > 1.0F && $$1 == Difficulty.NORMAL) {
               pPlayer.hurt(pPlayer.damageSources().starve(), 1.0F);
            }

            this.tickTimer = 0;
         }
      } else {
         this.tickTimer = 0;
      }
   }

   public void readAdditionalSaveData(CompoundTag pCompoundTag) {
      if (pCompoundTag.contains("foodLevel", 99)) {
         this.foodLevel = pCompoundTag.getInt("foodLevel");
         this.tickTimer = pCompoundTag.getInt("foodTickTimer");
         this.saturationLevel = pCompoundTag.getFloat("foodSaturationLevel");
         this.exhaustionLevel = pCompoundTag.getFloat("foodExhaustionLevel");
      }
   }

   public void addAdditionalSaveData(CompoundTag pCompoundTag) {
      pCompoundTag.putInt("foodLevel", this.foodLevel);
      pCompoundTag.putInt("foodTickTimer", this.tickTimer);
      pCompoundTag.putFloat("foodSaturationLevel", this.saturationLevel);
      pCompoundTag.putFloat("foodExhaustionLevel", this.exhaustionLevel);
   }

   public int getFoodLevel() {
      return this.foodLevel;
   }

   public int getLastFoodLevel() {
      return this.lastFoodLevel;
   }

   public boolean needsFood() {
      return this.foodLevel < 20;
   }

   public void addExhaustion(float pExhaustion) {
      this.exhaustionLevel = Math.min(this.exhaustionLevel + pExhaustion, 40.0F);
   }

   public float getExhaustionLevel() {
      return this.exhaustionLevel;
   }

   public float getSaturationLevel() {
      return this.saturationLevel;
   }

   public void setFoodLevel(int pFoodLevel) {
      this.foodLevel = pFoodLevel;
   }

   public void setSaturation(float pSaturationLevel) {
      this.saturationLevel = pSaturationLevel;
   }

   public void setExhaustion(float pExhaustionLevel) {
      this.exhaustionLevel = pExhaustionLevel;
   }
}
