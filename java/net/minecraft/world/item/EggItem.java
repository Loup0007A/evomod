package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.level.Level;

public class EggItem extends Item implements ProjectileItem {
   public EggItem(Item.Properties pProperties) {
      super(pProperties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      ItemStack $$3 = pPlayer.getItemInHand(pHand);
      pLevel.playSound(
         null,
         pPlayer.getX(),
         pPlayer.getY(),
         pPlayer.getZ(),
         SoundEvents.EGG_THROW,
         SoundSource.PLAYERS,
         0.5F,
         0.4F / (pLevel.getRandom().nextFloat() * 0.4F + 0.8F)
      );
      if (!pLevel.isClientSide) {
         ThrownEgg $$4 = new ThrownEgg(pLevel, pPlayer);
         $$4.setItem($$3);
         $$4.shootFromRotation(pPlayer, pPlayer.getXRot(), pPlayer.getYRot(), 0.0F, 1.5F, 1.0F);
         pLevel.addFreshEntity($$4);
      }

      pPlayer.awardStat(Stats.ITEM_USED.get(this));
      $$3.consume(1, pPlayer);
      return InteractionResultHolder.sidedSuccess($$3, pLevel.isClientSide());
   }

   @Override
   public Projectile asProjectile(Level pLevel, Position pPos, ItemStack pStack, Direction pDirection) {
      ThrownEgg $$4 = new ThrownEgg(pLevel, pPos.x(), pPos.y(), pPos.z());
      $$4.setItem(pStack);
      return $$4;
   }
}
