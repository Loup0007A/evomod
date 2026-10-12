package net.minecraft.world.entity;

import com.mojang.datafixers.util.Either;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.Items;

public interface Leashable {
   String LEASH_TAG = "leash";
   double LEASH_TOO_FAR_DIST = 10.0;
   double LEASH_ELASTIC_DIST = 6.0;

   @Nullable
   Leashable.LeashData getLeashData();

   void setLeashData(@Nullable Leashable.LeashData pLeashData);

   default boolean isLeashed() {
      return this.getLeashData() != null && this.getLeashData().leashHolder != null;
   }

   default boolean mayBeLeashed() {
      return this.getLeashData() != null;
   }

   default boolean canHaveALeashAttachedToIt() {
      return this.canBeLeashed() && !this.isLeashed();
   }

   default boolean canBeLeashed() {
      return true;
   }

   default void setDelayedLeashHolderId(int pDelayedLeashHolderId) {
      this.setLeashData(new Leashable.LeashData(pDelayedLeashHolderId));
      dropLeash((Entity)this, false, false);
   }

   @Nullable
   default Leashable.LeashData readLeashData(CompoundTag pTag) {
      if (pTag.contains("leash", 10)) {
         return new Leashable.LeashData(Either.left(pTag.getCompound("leash").getUUID("UUID")));
      } else {
         if (pTag.contains("leash", 11)) {
            Either<UUID, BlockPos> $$1 = NbtUtils.readBlockPos(pTag, "leash").<Either<UUID, BlockPos>>map(Either::right).orElse(null);
            if ($$1 != null) {
               return new Leashable.LeashData($$1);
            }
         }

         return null;
      }
   }

   default void writeLeashData(CompoundTag pTag, @Nullable Leashable.LeashData pLeashData) {
      if (pLeashData != null) {
         Either<UUID, BlockPos> $$2 = pLeashData.delayedLeashInfo;
         if (pLeashData.leashHolder instanceof LeashFenceKnotEntity $$3) {
            $$2 = Either.right($$3.getPos());
         } else if (pLeashData.leashHolder != null) {
            $$2 = Either.left(pLeashData.leashHolder.getUUID());
         }

         if ($$2 != null) {
            pTag.put("leash", (Tag)$$2.map(p_345095_ -> {
               CompoundTag $$1 = new CompoundTag();
               $$1.putUUID("UUID", p_345095_);
               return $$1;
            }, NbtUtils::writeBlockPos));
         }
      }
   }

   private static <E extends Entity & Leashable> void restoreLeashFromSave(E pEntity, Leashable.LeashData pLeashData) {
      if (pLeashData.delayedLeashInfo != null && pEntity.level() instanceof ServerLevel $$2) {
         Optional<UUID> $$3 = pLeashData.delayedLeashInfo.left();
         Optional<BlockPos> $$4 = pLeashData.delayedLeashInfo.right();
         if ($$3.isPresent()) {
            Entity $$5 = $$2.getEntity($$3.get());
            if ($$5 != null) {
               setLeashedTo(pEntity, $$5, true);
               return;
            }
         } else if ($$4.isPresent()) {
            setLeashedTo(pEntity, LeashFenceKnotEntity.getOrCreateKnot($$2, $$4.get()), true);
            return;
         }

         if (pEntity.tickCount > 100) {
            pEntity.spawnAtLocation(Items.LEAD);
            pEntity.setLeashData(null);
         }
      }
   }

   default void dropLeash(boolean pBroadcastPacket, boolean pDropItem) {
      dropLeash((Entity)this, pBroadcastPacket, pDropItem);
   }

   private static <E extends Entity & Leashable> void dropLeash(E pEntity, boolean pBroadcastPacket, boolean pDropItem) {
      Leashable.LeashData $$3 = pEntity.getLeashData();
      if ($$3 != null && $$3.leashHolder != null) {
         pEntity.setLeashData(null);
         if (!pEntity.level().isClientSide && pDropItem) {
            pEntity.spawnAtLocation(Items.LEAD);
         }

         if (pBroadcastPacket && pEntity.level() instanceof ServerLevel $$4) {
            $$4.getChunkSource().broadcast(pEntity, new ClientboundSetEntityLinkPacket(pEntity, null));
         }
      }
   }

   static <E extends Entity & Leashable> void tickLeash(E pEntity) {
      Leashable.LeashData $$1 = pEntity.getLeashData();
      if ($$1 != null && $$1.delayedLeashInfo != null) {
         restoreLeashFromSave(pEntity, $$1);
      }

      if ($$1 != null && $$1.leashHolder != null) {
         if (!pEntity.isAlive() || !$$1.leashHolder.isAlive()) {
            dropLeash(pEntity, true, true);
         }

         Entity $$2 = pEntity.getLeashHolder();
         if ($$2 != null && $$2.level() == pEntity.level()) {
            float $$3 = pEntity.distanceTo($$2);
            if (!pEntity.handleLeashAtDistance($$2, $$3)) {
               return;
            }

            if ((double)$$3 > 10.0) {
               pEntity.leashTooFarBehaviour();
            } else if ((double)$$3 > 6.0) {
               pEntity.elasticRangeLeashBehaviour($$2, $$3);
               pEntity.checkSlowFallDistance();
            } else {
               pEntity.closeRangeLeashBehaviour($$2);
            }
         }
      }
   }

   default boolean handleLeashAtDistance(Entity pLeashHolder, float pDistance) {
      return true;
   }

   default void leashTooFarBehaviour() {
      this.dropLeash(true, true);
   }

   default void closeRangeLeashBehaviour(Entity pEntity) {
   }

   default void elasticRangeLeashBehaviour(Entity pLeashHolder, float pDistance) {
      legacyElasticRangeLeashBehaviour((Entity)this, pLeashHolder, pDistance);
   }

   private static <E extends Entity & Leashable> void legacyElasticRangeLeashBehaviour(E pEntity, Entity pLeashHolder, float pDistance) {
      double $$3 = (pLeashHolder.getX() - pEntity.getX()) / (double)pDistance;
      double $$4 = (pLeashHolder.getY() - pEntity.getY()) / (double)pDistance;
      double $$5 = (pLeashHolder.getZ() - pEntity.getZ()) / (double)pDistance;
      pEntity.setDeltaMovement(
         pEntity.getDeltaMovement().add(Math.copySign($$3 * $$3 * 0.4, $$3), Math.copySign($$4 * $$4 * 0.4, $$4), Math.copySign($$5 * $$5 * 0.4, $$5))
      );
   }

   default void setLeashedTo(Entity pLeashHolder, boolean pBroadcastPacket) {
      setLeashedTo((Entity)this, pLeashHolder, pBroadcastPacket);
   }

   private static <E extends Entity & Leashable> void setLeashedTo(E pEntity, Entity pLeashHolder, boolean pBroadcastPacket) {
      Leashable.LeashData $$3 = pEntity.getLeashData();
      if ($$3 == null) {
         $$3 = new Leashable.LeashData(pLeashHolder);
         pEntity.setLeashData($$3);
      } else {
         $$3.setLeashHolder(pLeashHolder);
      }

      if (pBroadcastPacket && pEntity.level() instanceof ServerLevel $$4) {
         $$4.getChunkSource().broadcast(pEntity, new ClientboundSetEntityLinkPacket(pEntity, pLeashHolder));
      }

      if (pEntity.isPassenger()) {
         pEntity.stopRiding();
      }
   }

   @Nullable
   default Entity getLeashHolder() {
      return getLeashHolder((Entity)this);
   }

   @Nullable
   private static <E extends Entity & Leashable> Entity getLeashHolder(E pEntity) {
      Leashable.LeashData $$1 = pEntity.getLeashData();
      if ($$1 == null) {
         return null;
      } else {
         if ($$1.delayedLeashHolderId != 0 && pEntity.level().isClientSide) {
            Entity var3 = pEntity.level().getEntity($$1.delayedLeashHolderId);
            if (var3 instanceof Entity) {
               $$1.setLeashHolder(var3);
            }
         }

         return $$1.leashHolder;
      }
   }

   public static final class LeashData {
      int delayedLeashHolderId;
      @Nullable
      public Entity leashHolder;
      @Nullable
      public Either<UUID, BlockPos> delayedLeashInfo;

      LeashData(Either<UUID, BlockPos> pDelayedLeashInfo) {
         this.delayedLeashInfo = pDelayedLeashInfo;
      }

      LeashData(Entity pLeashHolder) {
         this.leashHolder = pLeashHolder;
      }

      LeashData(int pDelayedLeashInfoId) {
         this.delayedLeashHolderId = pDelayedLeashInfoId;
      }

      public void setLeashHolder(Entity pLeashHolder) {
         this.leashHolder = pLeashHolder;
         this.delayedLeashInfo = null;
         this.delayedLeashHolderId = 0;
      }
   }
}
