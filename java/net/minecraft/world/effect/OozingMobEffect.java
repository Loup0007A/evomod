package net.minecraft.world.effect;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

class OozingMobEffect extends MobEffect {
   private static final int RADIUS_TO_CHECK_SLIMES = 2;
   public static final int SLIME_SIZE = 2;
   private final ToIntFunction<RandomSource> spawnedCount;

   protected OozingMobEffect(MobEffectCategory pCategory, int pColor, ToIntFunction<RandomSource> pSpawnedCount) {
      super(pCategory, pColor, ParticleTypes.ITEM_SLIME);
      this.spawnedCount = pSpawnedCount;
   }

   @VisibleForTesting
   protected static int numberOfSlimesToSpawn(int pMaxEntityCramming, OozingMobEffect.NearbySlimes pNearbySlimes, int pSpawnCount) {
      return pMaxEntityCramming < 1 ? pSpawnCount : Mth.clamp(0, pMaxEntityCramming - pNearbySlimes.count(pMaxEntityCramming), pSpawnCount);
   }

   @Override
   public void onMobRemoved(LivingEntity pLivingEntity, int pAmplifier, Entity.RemovalReason pReason) {
      if (pReason == Entity.RemovalReason.KILLED) {
         int $$3 = this.spawnedCount.applyAsInt(pLivingEntity.getRandom());
         Level $$4 = pLivingEntity.level();
         int $$5 = $$4.getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
         int $$6 = numberOfSlimesToSpawn($$5, OozingMobEffect.NearbySlimes.closeTo(pLivingEntity), $$3);

         for (int $$7 = 0; $$7 < $$6; $$7++) {
            this.spawnSlimeOffspring(pLivingEntity.level(), pLivingEntity.getX(), pLivingEntity.getY() + 0.5, pLivingEntity.getZ());
         }
      }
   }

   private void spawnSlimeOffspring(Level pLevel, double pX, double pY, double pZ) {
      Slime $$4 = EntityType.SLIME.create(pLevel);
      if ($$4 != null) {
         $$4.setSize(2, true);
         $$4.moveTo(pX, pY, pZ, pLevel.getRandom().nextFloat() * 360.0F, 0.0F);
         pLevel.addFreshEntity($$4);
      }
   }

   @FunctionalInterface
   protected interface NearbySlimes {
      int count(int pMaxEntityCramming);

      static OozingMobEffect.NearbySlimes closeTo(LivingEntity pEntity) {
         return p_343171_ -> {
            List<Slime> $$2 = new ArrayList<>();
            pEntity.level().getEntities(EntityType.SLIME, pEntity.getBoundingBox().inflate(2.0), p_344894_ -> p_344894_ != pEntity, $$2, p_343171_);
            return $$2.size();
         };
      }
   }
}
