package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class FireChargeItem extends Item implements ProjectileItem {
   public FireChargeItem(Item.Properties pProperties) {
      super(pProperties);
   }

   @Override
   public InteractionResult useOn(UseOnContext pContext) {
      Level $$1 = pContext.getLevel();
      BlockPos $$2 = pContext.getClickedPos();
      BlockState $$3 = $$1.getBlockState($$2);
      boolean $$4 = false;
      if (!CampfireBlock.canLight($$3) && !CandleBlock.canLight($$3) && !CandleCakeBlock.canLight($$3)) {
         $$2 = $$2.relative(pContext.getClickedFace());
         if (BaseFireBlock.canBePlacedAt($$1, $$2, pContext.getHorizontalDirection())) {
            this.playSound($$1, $$2);
            $$1.setBlockAndUpdate($$2, BaseFireBlock.getState($$1, $$2));
            $$1.gameEvent(pContext.getPlayer(), GameEvent.BLOCK_PLACE, $$2);
            $$4 = true;
         }
      } else {
         this.playSound($$1, $$2);
         $$1.setBlockAndUpdate($$2, $$3.setValue(BlockStateProperties.LIT, Boolean.valueOf(true)));
         $$1.gameEvent(pContext.getPlayer(), GameEvent.BLOCK_CHANGE, $$2);
         $$4 = true;
      }

      if ($$4) {
         pContext.getItemInHand().shrink(1);
         return InteractionResult.sidedSuccess($$1.isClientSide);
      } else {
         return InteractionResult.FAIL;
      }
   }

   private void playSound(Level pLevel, BlockPos pPos) {
      RandomSource $$2 = pLevel.getRandom();
      pLevel.playSound(null, pPos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, ($$2.nextFloat() - $$2.nextFloat()) * 0.2F + 1.0F);
   }

   @Override
   public Projectile asProjectile(Level pLevel, Position pPos, ItemStack pStack, Direction pDirection) {
      RandomSource $$4 = pLevel.getRandom();
      double $$5 = $$4.triangle((double)pDirection.getStepX(), 0.11485000000000001);
      double $$6 = $$4.triangle((double)pDirection.getStepY(), 0.11485000000000001);
      double $$7 = $$4.triangle((double)pDirection.getStepZ(), 0.11485000000000001);
      Vec3 $$8 = new Vec3($$5, $$6, $$7);
      SmallFireball $$9 = new SmallFireball(pLevel, pPos.x(), pPos.y(), pPos.z(), $$8.normalize());
      $$9.setItem(pStack);
      return $$9;
   }

   @Override
   public void shoot(Projectile pProjectile, double pX, double pY, double pZ, float pVelocity, float pInaccuracy) {
   }

   @Override
   public ProjectileItem.DispenseConfig createDispenseConfig() {
      return ProjectileItem.DispenseConfig.builder()
         .positionFunction((p_334997_, p_333408_) -> DispenserBlock.getDispensePosition(p_334997_, 1.0, Vec3.ZERO))
         .uncertainty(6.6666665F)
         .power(1.0F)
         .overrideDispenseEvent(1018)
         .build();
   }
}
