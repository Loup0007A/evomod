package net.minecraft.world.level;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface CollisionGetter extends BlockGetter {
   WorldBorder getWorldBorder();

   @Nullable
   BlockGetter getChunkForCollisions(int pChunkX, int pChunkZ);

   default boolean isUnobstructed(@Nullable Entity pEntity, VoxelShape pShape) {
      return true;
   }

   default boolean isUnobstructed(BlockState pState, BlockPos pPos, CollisionContext pContext) {
      VoxelShape $$3 = pState.getCollisionShape(this, pPos, pContext);
      return $$3.isEmpty() || this.isUnobstructed(null, $$3.move((double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ()));
   }

   default boolean isUnobstructed(Entity pEntity) {
      return this.isUnobstructed(pEntity, Shapes.create(pEntity.getBoundingBox()));
   }

   default boolean noCollision(AABB pCollisionBox) {
      return this.noCollision(null, pCollisionBox);
   }

   default boolean noCollision(Entity pEntity) {
      return this.noCollision(pEntity, pEntity.getBoundingBox());
   }

   default boolean noCollision(@Nullable Entity pEntity, AABB pCollisionBox) {
      for (VoxelShape $$2 : this.getBlockCollisions(pEntity, pCollisionBox)) {
         if (!$$2.isEmpty()) {
            return false;
         }
      }

      if (!this.getEntityCollisions(pEntity, pCollisionBox).isEmpty()) {
         return false;
      } else if (pEntity == null) {
         return true;
      } else {
         VoxelShape $$3 = this.borderCollision(pEntity, pCollisionBox);
         return $$3 == null || !Shapes.joinIsNotEmpty($$3, Shapes.create(pCollisionBox), BooleanOp.AND);
      }
   }

   default boolean noBlockCollision(@Nullable Entity pEntity, AABB pBoundingBox) {
      for (VoxelShape $$2 : this.getBlockCollisions(pEntity, pBoundingBox)) {
         if (!$$2.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   List<VoxelShape> getEntityCollisions(@Nullable Entity pEntity, AABB pCollisionBox);

   default Iterable<VoxelShape> getCollisions(@Nullable Entity pEntity, AABB pCollisionBox) {
      List<VoxelShape> $$2 = this.getEntityCollisions(pEntity, pCollisionBox);
      Iterable<VoxelShape> $$3 = this.getBlockCollisions(pEntity, pCollisionBox);
      return $$2.isEmpty() ? $$3 : Iterables.concat($$2, $$3);
   }

   default Iterable<VoxelShape> getBlockCollisions(@Nullable Entity pEntity, AABB pCollisionBox) {
      return () -> new BlockCollisions<VoxelShape>(this, pEntity, pCollisionBox, false, (p_286215_, p_286216_) -> p_286216_);
   }

   @Nullable
   private VoxelShape borderCollision(Entity pEntity, AABB pBox) {
      WorldBorder $$2 = this.getWorldBorder();
      return $$2.isInsideCloseToBorder(pEntity, pBox) ? $$2.getCollisionShape() : null;
   }

   default boolean collidesWithSuffocatingBlock(@Nullable Entity pEntity, AABB pBox) {
      BlockCollisions<VoxelShape> $$2 = new BlockCollisions<>(this, pEntity, pBox, true, (p_286211_, p_286212_) -> p_286212_);

      while ($$2.hasNext()) {
         if (!((VoxelShape)$$2.next()).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   default Optional<BlockPos> findSupportingBlock(Entity pEntity, AABB pBox) {
      BlockPos $$2 = null;
      double $$3 = Double.MAX_VALUE;
      BlockCollisions<BlockPos> $$4 = new BlockCollisions<>(this, pEntity, pBox, false, (p_286213_, p_286214_) -> p_286213_);

      while ($$4.hasNext()) {
         BlockPos $$5 = (BlockPos)$$4.next();
         double $$6 = $$5.distToCenterSqr(pEntity.position());
         if ($$6 < $$3 || $$6 == $$3 && ($$2 == null || $$2.compareTo($$5) < 0)) {
            $$2 = $$5.immutable();
            $$3 = $$6;
         }
      }

      return Optional.ofNullable($$2);
   }

   default Optional<Vec3> findFreePosition(@Nullable Entity pEntity, VoxelShape pShape, Vec3 pPos, double pX, double pY, double pZ) {
      if (pShape.isEmpty()) {
         return Optional.empty();
      } else {
         AABB $$6 = pShape.bounds().inflate(pX, pY, pZ);
         VoxelShape $$7 = StreamSupport.stream(this.getBlockCollisions(pEntity, $$6).spliterator(), false)
            .filter(p_186430_ -> this.getWorldBorder() == null || this.getWorldBorder().isWithinBounds(p_186430_.bounds()))
            .flatMap(p_186426_ -> p_186426_.toAabbs().stream())
            .map(p_186424_ -> p_186424_.inflate(pX / 2.0, pY / 2.0, pZ / 2.0))
            .map(Shapes::create)
            .reduce(Shapes.empty(), Shapes::or);
         VoxelShape $$8 = Shapes.join(pShape, $$7, BooleanOp.ONLY_FIRST);
         return $$8.closestPointTo(pPos);
      }
   }
}
