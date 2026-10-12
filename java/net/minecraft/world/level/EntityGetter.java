package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface EntityGetter {
   List<Entity> getEntities(@Nullable Entity pEntity, AABB pArea, Predicate<? super Entity> pPredicate);

   <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> pEntityTypeTest, AABB pBounds, Predicate<? super T> pPredicate);

   default <T extends Entity> List<T> getEntitiesOfClass(Class<T> pClazz, AABB pArea, Predicate<? super T> pFilter) {
      return this.getEntities(EntityTypeTest.forClass(pClazz), pArea, pFilter);
   }

   List<? extends Player> players();

   default List<Entity> getEntities(@Nullable Entity pEntity, AABB pArea) {
      return this.getEntities(pEntity, pArea, EntitySelector.NO_SPECTATORS);
   }

   default boolean isUnobstructed(@Nullable Entity pEntity, VoxelShape pShape) {
      if (pShape.isEmpty()) {
         return true;
      } else {
         for (Entity $$2 : this.getEntities(pEntity, pShape.bounds())) {
            if (!$$2.isRemoved()
               && $$2.blocksBuilding
               && (pEntity == null || !$$2.isPassengerOfSameVehicle(pEntity))
               && Shapes.joinIsNotEmpty(pShape, Shapes.create($$2.getBoundingBox()), BooleanOp.AND)) {
               return false;
            }
         }

         return true;
      }
   }

   default <T extends Entity> List<T> getEntitiesOfClass(Class<T> pEntityClass, AABB pArea) {
      return this.getEntitiesOfClass(pEntityClass, pArea, EntitySelector.NO_SPECTATORS);
   }

   default List<VoxelShape> getEntityCollisions(@Nullable Entity pEntity, AABB pCollisionBox) {
      if (pCollisionBox.getSize() < 1.0E-7) {
         return List.of();
      } else {
         Predicate<Entity> $$2 = pEntity == null ? EntitySelector.CAN_BE_COLLIDED_WITH : EntitySelector.NO_SPECTATORS.and(pEntity::canCollideWith);
         List<Entity> $$3 = this.getEntities(pEntity, pCollisionBox.inflate(1.0E-7), $$2);
         if ($$3.isEmpty()) {
            return List.of();
         } else {
            Builder<VoxelShape> $$4 = ImmutableList.builderWithExpectedSize($$3.size());

            for (Entity $$5 : $$3) {
               $$4.add(Shapes.create($$5.getBoundingBox()));
            }

            return $$4.build();
         }
      }
   }

   @Nullable
   default Player getNearestPlayer(double pX, double pY, double pZ, double pDistance, @Nullable Predicate<Entity> pPredicate) {
      double $$5 = -1.0;
      Player $$6 = null;

      for (Player $$7 : this.players()) {
         if (pPredicate == null || pPredicate.test($$7)) {
            double $$8 = $$7.distanceToSqr(pX, pY, pZ);
            if ((pDistance < 0.0 || $$8 < pDistance * pDistance) && ($$5 == -1.0 || $$8 < $$5)) {
               $$5 = $$8;
               $$6 = $$7;
            }
         }
      }

      return $$6;
   }

   @Nullable
   default Player getNearestPlayer(Entity pEntity, double pDistance) {
      return this.getNearestPlayer(pEntity.getX(), pEntity.getY(), pEntity.getZ(), pDistance, false);
   }

   @Nullable
   default Player getNearestPlayer(double pX, double pY, double pZ, double pDistance, boolean pCreativePlayers) {
      Predicate<Entity> $$5 = pCreativePlayers ? EntitySelector.NO_CREATIVE_OR_SPECTATOR : EntitySelector.NO_SPECTATORS;
      return this.getNearestPlayer(pX, pY, pZ, pDistance, $$5);
   }

   default boolean hasNearbyAlivePlayer(double pX, double pY, double pZ, double pDistance) {
      for (Player $$4 : this.players()) {
         if (EntitySelector.NO_SPECTATORS.test($$4) && EntitySelector.LIVING_ENTITY_STILL_ALIVE.test($$4)) {
            double $$5 = $$4.distanceToSqr(pX, pY, pZ);
            if (pDistance < 0.0 || $$5 < pDistance * pDistance) {
               return true;
            }
         }
      }

      return false;
   }

   @Nullable
   default Player getNearestPlayer(TargetingConditions pPredicate, LivingEntity pTarget) {
      return this.getNearestEntity(this.players(), pPredicate, pTarget, pTarget.getX(), pTarget.getY(), pTarget.getZ());
   }

   @Nullable
   default Player getNearestPlayer(TargetingConditions pPredicate, LivingEntity pTarget, double pX, double pY, double pZ) {
      return this.getNearestEntity(this.players(), pPredicate, pTarget, pX, pY, pZ);
   }

   @Nullable
   default Player getNearestPlayer(TargetingConditions pPredicate, double pX, double pY, double pZ) {
      return this.getNearestEntity(this.players(), pPredicate, null, pX, pY, pZ);
   }

   @Nullable
   default <T extends LivingEntity> T getNearestEntity(
      Class<? extends T> pEntityClazz, TargetingConditions pConditions, @Nullable LivingEntity pTarget, double pX, double pY, double pZ, AABB pBoundingBox
   ) {
      return this.getNearestEntity(this.getEntitiesOfClass(pEntityClazz, pBoundingBox, p_186454_ -> true), pConditions, pTarget, pX, pY, pZ);
   }

   @Nullable
   default <T extends LivingEntity> T getNearestEntity(
      List<? extends T> pEntities, TargetingConditions pPredicate, @Nullable LivingEntity pTarget, double pX, double pY, double pZ
   ) {
      double $$6 = -1.0;
      T $$7 = null;

      for (T $$8 : pEntities) {
         if (pPredicate.test(pTarget, $$8)) {
            double $$9 = $$8.distanceToSqr(pX, pY, pZ);
            if ($$6 == -1.0 || $$9 < $$6) {
               $$6 = $$9;
               $$7 = $$8;
            }
         }
      }

      return $$7;
   }

   default List<Player> getNearbyPlayers(TargetingConditions pPredicate, LivingEntity pTarget, AABB pArea) {
      List<Player> $$3 = Lists.newArrayList();

      for (Player $$4 : this.players()) {
         if (pArea.contains($$4.getX(), $$4.getY(), $$4.getZ()) && pPredicate.test(pTarget, $$4)) {
            $$3.add($$4);
         }
      }

      return $$3;
   }

   default <T extends LivingEntity> List<T> getNearbyEntities(Class<T> pEntityClazz, TargetingConditions pEntityPredicate, LivingEntity pEntity, AABB pArea) {
      List<T> $$4 = this.getEntitiesOfClass(pEntityClazz, pArea, p_186450_ -> true);
      List<T> $$5 = Lists.newArrayList();

      for (T $$6 : $$4) {
         if (pEntityPredicate.test(pEntity, $$6)) {
            $$5.add($$6);
         }
      }

      return $$5;
   }

   @Nullable
   default Player getPlayerByUUID(UUID pUniqueId) {
      for (int $$1 = 0; $$1 < this.players().size(); $$1++) {
         Player $$2 = this.players().get($$1);
         if (pUniqueId.equals($$2.getUUID())) {
            return $$2;
         }
      }

      return null;
   }
}
