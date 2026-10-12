package net.minecraft.util;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ParticleUtils {
   public static void spawnParticlesOnBlockFaces(Level pLevel, BlockPos pPos, ParticleOptions pParticle, IntProvider pCount) {
      for (Direction $$4 : Direction.values()) {
         spawnParticlesOnBlockFace(pLevel, pPos, pParticle, pCount, $$4, () -> getRandomSpeedRanges(pLevel.random), 0.55);
      }
   }

   public static void spawnParticlesOnBlockFace(
      Level pLevel, BlockPos pPos, ParticleOptions pParticle, IntProvider pCount, Direction pDirection, Supplier<Vec3> pSpeedSupplier, double pSpread
   ) {
      int $$7 = pCount.sample(pLevel.random);

      for (int $$8 = 0; $$8 < $$7; $$8++) {
         spawnParticleOnFace(pLevel, pPos, pDirection, pParticle, pSpeedSupplier.get(), pSpread);
      }
   }

   private static Vec3 getRandomSpeedRanges(RandomSource pRandom) {
      return new Vec3(Mth.nextDouble(pRandom, -0.5, 0.5), Mth.nextDouble(pRandom, -0.5, 0.5), Mth.nextDouble(pRandom, -0.5, 0.5));
   }

   public static void spawnParticlesAlongAxis(Direction.Axis pAxis, Level pLevel, BlockPos pPos, double pSpread, ParticleOptions pParticle, UniformInt pCount) {
      Vec3 $$6 = Vec3.atCenterOf(pPos);
      boolean $$7 = pAxis == Direction.Axis.X;
      boolean $$8 = pAxis == Direction.Axis.Y;
      boolean $$9 = pAxis == Direction.Axis.Z;
      int $$10 = pCount.sample(pLevel.random);

      for (int $$11 = 0; $$11 < $$10; $$11++) {
         double $$12 = $$6.x + Mth.nextDouble(pLevel.random, -1.0, 1.0) * ($$7 ? 0.5 : pSpread);
         double $$13 = $$6.y + Mth.nextDouble(pLevel.random, -1.0, 1.0) * ($$8 ? 0.5 : pSpread);
         double $$14 = $$6.z + Mth.nextDouble(pLevel.random, -1.0, 1.0) * ($$9 ? 0.5 : pSpread);
         double $$15 = $$7 ? Mth.nextDouble(pLevel.random, -1.0, 1.0) : 0.0;
         double $$16 = $$8 ? Mth.nextDouble(pLevel.random, -1.0, 1.0) : 0.0;
         double $$17 = $$9 ? Mth.nextDouble(pLevel.random, -1.0, 1.0) : 0.0;
         pLevel.addParticle(pParticle, $$12, $$13, $$14, $$15, $$16, $$17);
      }
   }

   public static void spawnParticleOnFace(Level pLevel, BlockPos pPos, Direction pDirection, ParticleOptions pParticle, Vec3 pSpeed, double pSpread) {
      Vec3 $$6 = Vec3.atCenterOf(pPos);
      int $$7 = pDirection.getStepX();
      int $$8 = pDirection.getStepY();
      int $$9 = pDirection.getStepZ();
      double $$10 = $$6.x + ($$7 == 0 ? Mth.nextDouble(pLevel.random, -0.5, 0.5) : (double)$$7 * pSpread);
      double $$11 = $$6.y + ($$8 == 0 ? Mth.nextDouble(pLevel.random, -0.5, 0.5) : (double)$$8 * pSpread);
      double $$12 = $$6.z + ($$9 == 0 ? Mth.nextDouble(pLevel.random, -0.5, 0.5) : (double)$$9 * pSpread);
      double $$13 = $$7 == 0 ? pSpeed.x() : 0.0;
      double $$14 = $$8 == 0 ? pSpeed.y() : 0.0;
      double $$15 = $$9 == 0 ? pSpeed.z() : 0.0;
      pLevel.addParticle(pParticle, $$10, $$11, $$12, $$13, $$14, $$15);
   }

   public static void spawnParticleBelow(Level pLevel, BlockPos pPos, RandomSource pRandom, ParticleOptions pParticle) {
      double $$4 = (double)pPos.getX() + pRandom.nextDouble();
      double $$5 = (double)pPos.getY() - 0.05;
      double $$6 = (double)pPos.getZ() + pRandom.nextDouble();
      pLevel.addParticle(pParticle, $$4, $$5, $$6, 0.0, 0.0, 0.0);
   }

   public static void spawnParticleInBlock(LevelAccessor pLevel, BlockPos pPos, int pCount, ParticleOptions pParticle) {
      double $$4 = 0.5;
      BlockState $$5 = pLevel.getBlockState(pPos);
      double $$6 = $$5.isAir() ? 1.0 : $$5.getShape(pLevel, pPos).max(Direction.Axis.Y);
      spawnParticles(pLevel, pPos, pCount, 0.5, $$6, true, pParticle);
   }

   public static void spawnParticles(
      LevelAccessor pLevel, BlockPos pPos, int pCount, double pXzSpread, double pYSpread, boolean pAllowInAir, ParticleOptions pParticle
   ) {
      RandomSource $$7 = pLevel.getRandom();

      for (int $$8 = 0; $$8 < pCount; $$8++) {
         double $$9 = $$7.nextGaussian() * 0.02;
         double $$10 = $$7.nextGaussian() * 0.02;
         double $$11 = $$7.nextGaussian() * 0.02;
         double $$12 = 0.5 - pXzSpread;
         double $$13 = (double)pPos.getX() + $$12 + $$7.nextDouble() * pXzSpread * 2.0;
         double $$14 = (double)pPos.getY() + $$7.nextDouble() * pYSpread;
         double $$15 = (double)pPos.getZ() + $$12 + $$7.nextDouble() * pXzSpread * 2.0;
         if (pAllowInAir || !pLevel.getBlockState(BlockPos.containing($$13, $$14, $$15).below()).isAir()) {
            pLevel.addParticle(pParticle, $$13, $$14, $$15, $$9, $$10, $$11);
         }
      }
   }

   public static void spawnSmashAttackParticles(LevelAccessor pLevel, BlockPos pPos, int pPower) {
      Vec3 $$3 = pPos.getCenter().add(0.0, 0.5, 0.0);
      BlockParticleOption $$4 = new BlockParticleOption(ParticleTypes.DUST_PILLAR, pLevel.getBlockState(pPos));

      for (int $$5 = 0; (float)$$5 < (float)pPower / 3.0F; $$5++) {
         double $$6 = $$3.x + pLevel.getRandom().nextGaussian() / 2.0;
         double $$7 = $$3.y;
         double $$8 = $$3.z + pLevel.getRandom().nextGaussian() / 2.0;
         double $$9 = pLevel.getRandom().nextGaussian() * 0.2F;
         double $$10 = pLevel.getRandom().nextGaussian() * 0.2F;
         double $$11 = pLevel.getRandom().nextGaussian() * 0.2F;
         pLevel.addParticle($$4, $$6, $$7, $$8, $$9, $$10, $$11);
      }

      for (int $$12 = 0; (float)$$12 < (float)pPower / 1.5F; $$12++) {
         double $$13 = $$3.x + 3.5 * Math.cos((double)$$12) + pLevel.getRandom().nextGaussian() / 2.0;
         double $$14 = $$3.y;
         double $$15 = $$3.z + 3.5 * Math.sin((double)$$12) + pLevel.getRandom().nextGaussian() / 2.0;
         double $$16 = pLevel.getRandom().nextGaussian() * 0.05F;
         double $$17 = pLevel.getRandom().nextGaussian() * 0.05F;
         double $$18 = pLevel.getRandom().nextGaussian() * 0.05F;
         pLevel.addParticle($$4, $$13, $$14, $$15, $$16, $$17, $$18);
      }
   }
}
