package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BrushItem extends Item {
   public static final int ANIMATION_DURATION = 10;
   private static final int USE_DURATION = 200;

   public BrushItem(Item.Properties pProperties) {
      super(pProperties);
   }

   @Override
   public InteractionResult useOn(UseOnContext pContext) {
      Player $$1 = pContext.getPlayer();
      if ($$1 != null && this.calculateHitResult($$1).getType() == HitResult.Type.BLOCK) {
         $$1.startUsingItem(pContext.getHand());
      }

      return InteractionResult.CONSUME;
   }

   @Override
   public UseAnim getUseAnimation(ItemStack pStack) {
      return UseAnim.BRUSH;
   }

   @Override
   public int getUseDuration(ItemStack pStack, LivingEntity pEntity) {
      return 200;
   }

   @Override
   public void onUseTick(Level pLevel, LivingEntity pLivingEntity, ItemStack pStack, int pRemainingUseDuration) {
      if (pRemainingUseDuration >= 0 && pLivingEntity instanceof Player $$4) {
         HitResult $$6 = this.calculateHitResult($$4);
         if ($$6 instanceof BlockHitResult $$7 && $$6.getType() == HitResult.Type.BLOCK) {
            int $$9 = this.getUseDuration(pStack, pLivingEntity) - pRemainingUseDuration + 1;
            boolean $$10 = $$9 % 10 == 5;
            if ($$10) {
               BlockPos $$11 = $$7.getBlockPos();
               BlockState $$12 = pLevel.getBlockState($$11);
               HumanoidArm $$13 = pLivingEntity.getUsedItemHand() == InteractionHand.MAIN_HAND ? $$4.getMainArm() : $$4.getMainArm().getOpposite();
               if ($$12.shouldSpawnTerrainParticles() && $$12.getRenderShape() != RenderShape.INVISIBLE) {
                  this.spawnDustParticles(pLevel, $$7, $$12, pLivingEntity.getViewVector(0.0F), $$13);
               }

               SoundEvent $$15;
               if ($$12.getBlock() instanceof BrushableBlock $$14) {
                  $$15 = $$14.getBrushSound();
               } else {
                  $$15 = SoundEvents.BRUSH_GENERIC;
               }

               pLevel.playSound($$4, $$11, $$15, SoundSource.BLOCKS);
               if (!pLevel.isClientSide() && pLevel.getBlockEntity($$11) instanceof BrushableBlockEntity $$17) {
                  boolean $$18 = $$17.brush(pLevel.getGameTime(), $$4, $$7.getDirection());
                  if ($$18) {
                     EquipmentSlot $$19 = pStack.equals($$4.getItemBySlot(EquipmentSlot.OFFHAND)) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                     pStack.hurtAndBreak(1, pLivingEntity, $$19);
                  }
               }
            }

            return;
         }

         pLivingEntity.releaseUsingItem();
      } else {
         pLivingEntity.releaseUsingItem();
      }
   }

   private HitResult calculateHitResult(Player pPlayer) {
      return ProjectileUtil.getHitResultOnViewVector(pPlayer, p_281111_ -> !p_281111_.isSpectator() && p_281111_.isPickable(), pPlayer.blockInteractionRange());
   }

   private void spawnDustParticles(Level pLevel, BlockHitResult pHitResult, BlockState pState, Vec3 pPos, HumanoidArm pArm) {
      double $$5 = 3.0;
      int $$6 = pArm == HumanoidArm.RIGHT ? 1 : -1;
      int $$7 = pLevel.getRandom().nextInt(7, 12);
      BlockParticleOption $$8 = new BlockParticleOption(ParticleTypes.BLOCK, pState);
      Direction $$9 = pHitResult.getDirection();
      BrushItem.DustParticlesDelta $$10 = BrushItem.DustParticlesDelta.fromDirection(pPos, $$9);
      Vec3 $$11 = pHitResult.getLocation();

      for (int $$12 = 0; $$12 < $$7; $$12++) {
         pLevel.addParticle(
            $$8,
            $$11.x - (double)($$9 == Direction.WEST ? 1.0E-6F : 0.0F),
            $$11.y,
            $$11.z - (double)($$9 == Direction.NORTH ? 1.0E-6F : 0.0F),
            $$10.xd() * (double)$$6 * 3.0 * pLevel.getRandom().nextDouble(),
            0.0,
            $$10.zd() * (double)$$6 * 3.0 * pLevel.getRandom().nextDouble()
         );
      }
   }

   static record DustParticlesDelta(double xd, double yd, double zd) {
      private static final double ALONG_SIDE_DELTA = 1.0;
      private static final double OUT_FROM_SIDE_DELTA = 0.1;

      public static BrushItem.DustParticlesDelta fromDirection(Vec3 pPos, Direction pDirection) {
         double $$2 = 0.0;

         return switch (pDirection) {
            case DOWN, UP -> new BrushItem.DustParticlesDelta(pPos.z(), 0.0, -pPos.x());
            case NORTH -> new BrushItem.DustParticlesDelta(1.0, 0.0, -0.1);
            case SOUTH -> new BrushItem.DustParticlesDelta(-1.0, 0.0, 0.1);
            case WEST -> new BrushItem.DustParticlesDelta(-0.1, 0.0, -1.0);
            case EAST -> new BrushItem.DustParticlesDelta(0.1, 0.0, 1.0);
         };
      }
   }
}
