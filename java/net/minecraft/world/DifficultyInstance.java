package net.minecraft.world;

import javax.annotation.concurrent.Immutable;
import net.minecraft.util.Mth;

@Immutable
public class DifficultyInstance {
   private static final float DIFFICULTY_TIME_GLOBAL_OFFSET = -72000.0F;
   private static final float MAX_DIFFICULTY_TIME_GLOBAL = 1440000.0F;
   private static final float MAX_DIFFICULTY_TIME_LOCAL = 3600000.0F;
   private final Difficulty base;
   private final float effectiveDifficulty;

   public DifficultyInstance(Difficulty pBase, long pLevelTime, long pChunkInhabitedTime, float pMoonPhaseFactor) {
      this.base = pBase;
      this.effectiveDifficulty = this.calculateDifficulty(pBase, pLevelTime, pChunkInhabitedTime, pMoonPhaseFactor);
   }

   public Difficulty getDifficulty() {
      return this.base;
   }

   public float getEffectiveDifficulty() {
      return this.effectiveDifficulty;
   }

   public boolean isHard() {
      return this.effectiveDifficulty >= (float)Difficulty.HARD.ordinal();
   }

   public boolean isHarderThan(float pDifficulty) {
      return this.effectiveDifficulty > pDifficulty;
   }

   public float getSpecialMultiplier() {
      if (this.effectiveDifficulty < 2.0F) {
         return 0.0F;
      } else {
         return this.effectiveDifficulty > 4.0F ? 1.0F : (this.effectiveDifficulty - 2.0F) / 2.0F;
      }
   }

   private float calculateDifficulty(Difficulty pDifficulty, long pLevelTime, long pChunkInhabitedTime, float pMoonPhaseFactor) {
      if (pDifficulty == Difficulty.PEACEFUL) {
         return 0.0F;
      } else {
         boolean $$4 = pDifficulty == Difficulty.HARD;
         float $$5 = 0.75F;
         float $$6 = Mth.clamp(((float)pLevelTime + -72000.0F) / 1440000.0F, 0.0F, 1.0F) * 0.25F;
         $$5 += $$6;
         float $$7 = 0.0F;
         $$7 += Mth.clamp((float)pChunkInhabitedTime / 3600000.0F, 0.0F, 1.0F) * ($$4 ? 1.0F : 0.75F);
         $$7 += Mth.clamp(pMoonPhaseFactor * 0.25F, 0.0F, $$6);
         if (pDifficulty == Difficulty.EASY) {
            $$7 *= 0.5F;
         }

         $$5 += $$7;
         return (float)pDifficulty.getId() * $$5;
      }
   }
}
