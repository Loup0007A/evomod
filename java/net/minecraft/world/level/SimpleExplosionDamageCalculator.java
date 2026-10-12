package net.minecraft.world.level;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class SimpleExplosionDamageCalculator extends ExplosionDamageCalculator {
   private final boolean explodesBlocks;
   private final boolean damagesEntities;
   private final Optional<Float> knockbackMultiplier;
   private final Optional<HolderSet<Block>> immuneBlocks;

   public SimpleExplosionDamageCalculator(
      boolean pExplodesBlocks, boolean pDamagesEntities, Optional<Float> pKnockbackMultiplier, Optional<HolderSet<Block>> pImmuneBlocks
   ) {
      this.explodesBlocks = pExplodesBlocks;
      this.damagesEntities = pDamagesEntities;
      this.knockbackMultiplier = pKnockbackMultiplier;
      this.immuneBlocks = pImmuneBlocks;
   }

   @Override
   public Optional<Float> getBlockExplosionResistance(Explosion pExplosion, BlockGetter pReader, BlockPos pPos, BlockState pState, FluidState pFluid) {
      if (this.immuneBlocks.isPresent()) {
         return pState.is(this.immuneBlocks.get()) ? Optional.of(3600000.0F) : Optional.empty();
      } else {
         return super.getBlockExplosionResistance(pExplosion, pReader, pPos, pState, pFluid);
      }
   }

   @Override
   public boolean shouldBlockExplode(Explosion pExplosion, BlockGetter pReader, BlockPos pPos, BlockState pState, float pPower) {
      return this.explodesBlocks;
   }

   @Override
   public boolean shouldDamageEntity(Explosion pExplosion, Entity pEntity) {
      return this.damagesEntities;
   }

   @Override
   public float getKnockbackMultiplier(Entity pEntity) {
      boolean var10000;
      label17: {
         if (pEntity instanceof Player $$1 && $$1.getAbilities().flying) {
            var10000 = true;
            break label17;
         }

         var10000 = false;
      }

      boolean $$2 = var10000;
      return $$2 ? 0.0F : this.knockbackMultiplier.orElseGet(() -> super.getKnockbackMultiplier(pEntity));
   }
}
