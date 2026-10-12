package net.minecraft.world.entity.decoration;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LeashFenceKnotEntity extends BlockAttachedEntity {
   public static final double OFFSET_Y = 0.375;

   public LeashFenceKnotEntity(EntityType<? extends LeashFenceKnotEntity> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public LeashFenceKnotEntity(Level pLevel, BlockPos pPos) {
      super(EntityType.LEASH_KNOT, pLevel, pPos);
      this.setPos((double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ());
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
   }

   @Override
   protected void recalculateBoundingBox() {
      this.setPosRaw((double)this.pos.getX() + 0.5, (double)this.pos.getY() + 0.375, (double)this.pos.getZ() + 0.5);
      double $$0 = (double)this.getType().getWidth() / 2.0;
      double $$1 = (double)this.getType().getHeight();
      this.setBoundingBox(new AABB(this.getX() - $$0, this.getY(), this.getZ() - $$0, this.getX() + $$0, this.getY() + $$1, this.getZ() + $$0));
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double pDistance) {
      return pDistance < 1024.0;
   }

   @Override
   public void dropItem(@Nullable Entity pBrokenEntity) {
      this.playSound(SoundEvents.LEASH_KNOT_BREAK, 1.0F, 1.0F);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag pCompound) {
   }

   @Override
   public void readAdditionalSaveData(CompoundTag pCompound) {
   }

   @Override
   public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
      if (this.level().isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         boolean $$2 = false;
         List<Leashable> $$3 = LeadItem.leashableInArea(this.level(), this.getPos(), p_342836_ -> {
            Entity $$2x = p_342836_.getLeashHolder();
            return $$2x == pPlayer || $$2x == this;
         });

         for (Leashable $$4 : $$3) {
            if ($$4.getLeashHolder() == pPlayer) {
               $$4.setLeashedTo(this, true);
               $$2 = true;
            }
         }

         boolean $$5 = false;
         if (!$$2) {
            this.discard();
            if (pPlayer.getAbilities().instabuild) {
               for (Leashable $$6 : $$3) {
                  if ($$6.isLeashed() && $$6.getLeashHolder() == this) {
                     $$6.dropLeash(true, false);
                     $$5 = true;
                  }
               }
            }
         }

         if ($$2 || $$5) {
            this.gameEvent(GameEvent.BLOCK_ATTACH, pPlayer);
         }

         return InteractionResult.CONSUME;
      }
   }

   @Override
   public boolean survives() {
      return this.level().getBlockState(this.pos).is(BlockTags.FENCES);
   }

   public static LeashFenceKnotEntity getOrCreateKnot(Level pLevel, BlockPos pPos) {
      int $$2 = pPos.getX();
      int $$3 = pPos.getY();
      int $$4 = pPos.getZ();

      for (LeashFenceKnotEntity $$6 : pLevel.getEntitiesOfClass(
         LeashFenceKnotEntity.class, new AABB((double)$$2 - 1.0, (double)$$3 - 1.0, (double)$$4 - 1.0, (double)$$2 + 1.0, (double)$$3 + 1.0, (double)$$4 + 1.0)
      )) {
         if ($$6.getPos().equals(pPos)) {
            return $$6;
         }
      }

      LeashFenceKnotEntity $$7 = new LeashFenceKnotEntity(pLevel, pPos);
      pLevel.addFreshEntity($$7);
      return $$7;
   }

   public void playPlacementSound() {
      this.playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0F, 1.0F);
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity pEntity) {
      return new ClientboundAddEntityPacket(this, 0, this.getPos());
   }

   @Override
   public Vec3 getRopeHoldPosition(float pPartialTicks) {
      return this.getPosition(pPartialTicks).add(0.0, 0.2, 0.0);
   }

   @Override
   public ItemStack getPickResult() {
      return new ItemStack(Items.LEAD);
   }
}
