package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BoatItem extends Item {
   private static final Predicate<Entity> ENTITY_PREDICATE = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);
   private final Boat.Type type;
   private final boolean hasChest;

   public BoatItem(boolean pHasChest, Boat.Type pType, Item.Properties pProperties) {
      super(pProperties);
      this.hasChest = pHasChest;
      this.type = pType;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      ItemStack $$3 = pPlayer.getItemInHand(pHand);
      HitResult $$4 = getPlayerPOVHitResult(pLevel, pPlayer, ClipContext.Fluid.ANY);
      if ($$4.getType() == HitResult.Type.MISS) {
         return InteractionResultHolder.pass($$3);
      } else {
         Vec3 $$5 = pPlayer.getViewVector(1.0F);
         double $$6 = 5.0;
         List<Entity> $$7 = pLevel.getEntities(pPlayer, pPlayer.getBoundingBox().expandTowards($$5.scale(5.0)).inflate(1.0), ENTITY_PREDICATE);
         if (!$$7.isEmpty()) {
            Vec3 $$8 = pPlayer.getEyePosition();

            for (Entity $$9 : $$7) {
               AABB $$10 = $$9.getBoundingBox().inflate((double)$$9.getPickRadius());
               if ($$10.contains($$8)) {
                  return InteractionResultHolder.pass($$3);
               }
            }
         }

         if ($$4.getType() == HitResult.Type.BLOCK) {
            Boat $$11 = this.getBoat(pLevel, $$4, $$3, pPlayer);
            $$11.setVariant(this.type);
            $$11.setYRot(pPlayer.getYRot());
            if (!pLevel.noCollision($$11, $$11.getBoundingBox())) {
               return InteractionResultHolder.fail($$3);
            } else {
               if (!pLevel.isClientSide) {
                  pLevel.addFreshEntity($$11);
                  pLevel.gameEvent(pPlayer, GameEvent.ENTITY_PLACE, $$4.getLocation());
                  $$3.consume(1, pPlayer);
               }

               pPlayer.awardStat(Stats.ITEM_USED.get(this));
               return InteractionResultHolder.sidedSuccess($$3, pLevel.isClientSide());
            }
         } else {
            return InteractionResultHolder.pass($$3);
         }
      }
   }

   private Boat getBoat(Level pLevel, HitResult pHitResult, ItemStack pStack, Player pPlayer) {
      Vec3 $$4 = pHitResult.getLocation();
      Boat $$5 = (Boat)(this.hasChest ? new ChestBoat(pLevel, $$4.x, $$4.y, $$4.z) : new Boat(pLevel, $$4.x, $$4.y, $$4.z));
      if (pLevel instanceof ServerLevel $$6) {
         EntityType.<Boat>createDefaultStackConfig($$6, pStack, pPlayer).accept($$5);
      }

      return $$5;
   }
}
