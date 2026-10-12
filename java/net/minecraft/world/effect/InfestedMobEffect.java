package net.minecraft.world.effect;

import java.util.function.ToIntFunction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

class InfestedMobEffect extends MobEffect {
   private final float chanceToSpawn;
   private final ToIntFunction<RandomSource> spawnedCount;

   protected InfestedMobEffect(MobEffectCategory pCategory, int pColor, float pChanceToSpawn, ToIntFunction<RandomSource> pSpawnedCount) {
      super(pCategory, pColor, ParticleTypes.INFESTED);
      this.chanceToSpawn = pChanceToSpawn;
      this.spawnedCount = pSpawnedCount;
   }

   @Override
   public void onMobHurt(LivingEntity pLivingEntity, int pAmplifier, DamageSource pDamageSource, float pAmount) {
      if (pLivingEntity.getRandom().nextFloat() <= this.chanceToSpawn) {
         int $$4 = this.spawnedCount.applyAsInt(pLivingEntity.getRandom());

         for (int $$5 = 0; $$5 < $$4; $$5++) {
            this.spawnSilverfish(
               pLivingEntity.level(),
               pLivingEntity,
               pLivingEntity.getX(),
               pLivingEntity.getY() + (double)pLivingEntity.getBbHeight() / 2.0,
               pLivingEntity.getZ()
            );
         }
      }
   }

   private void spawnSilverfish(Level pLevel, LivingEntity pEntity, double pX, double pY, double pZ) {
      Silverfish $$5 = EntityType.SILVERFISH.create(pLevel);
      if ($$5 != null) {
         RandomSource $$6 = pEntity.getRandom();
         float $$7 = (float) (Math.PI / 2);
         float $$8 = Mth.randomBetween($$6, (float) (-Math.PI / 2), (float) (Math.PI / 2));
         Vector3f $$9 = pEntity.getLookAngle().toVector3f().mul(0.3F).mul(1.0F, 1.5F, 1.0F).rotateY($$8);
         $$5.moveTo(pX, pY, pZ, pLevel.getRandom().nextFloat() * 360.0F, 0.0F);
         $$5.setDeltaMovement(new Vec3($$9));
         pLevel.addFreshEntity($$5);
         $$5.playSound(SoundEvents.SILVERFISH_HURT);
      }
   }
}
