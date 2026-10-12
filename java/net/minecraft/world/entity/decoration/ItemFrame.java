package net.minecraft.world.entity.decoration;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;

public class ItemFrame extends HangingEntity {
   private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ItemFrame.class, EntityDataSerializers.ITEM_STACK);
   private static final EntityDataAccessor<Integer> DATA_ROTATION = SynchedEntityData.defineId(ItemFrame.class, EntityDataSerializers.INT);
   public static final int NUM_ROTATIONS = 8;
   private static final float DEPTH = 0.0625F;
   private static final float WIDTH = 0.75F;
   private static final float HEIGHT = 0.75F;
   private float dropChance = 1.0F;
   private boolean fixed;

   public ItemFrame(EntityType<? extends ItemFrame> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public ItemFrame(Level pLevel, BlockPos pPos, Direction pFacingDirection) {
      this(EntityType.ITEM_FRAME, pLevel, pPos, pFacingDirection);
   }

   public ItemFrame(EntityType<? extends ItemFrame> pEntityType, Level pLevel, BlockPos pPos, Direction pDirection) {
      super(pEntityType, pLevel, pPos);
      this.setDirection(pDirection);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
      pBuilder.define(DATA_ITEM, ItemStack.EMPTY);
      pBuilder.define(DATA_ROTATION, 0);
   }

   @Override
   protected void setDirection(Direction pFacingDirection) {
      Validate.notNull(pFacingDirection);
      this.direction = pFacingDirection;
      if (pFacingDirection.getAxis().isHorizontal()) {
         this.setXRot(0.0F);
         this.setYRot((float)(this.direction.get2DDataValue() * 90));
      } else {
         this.setXRot((float)(-90 * pFacingDirection.getAxisDirection().getStep()));
         this.setYRot(0.0F);
      }

      this.xRotO = this.getXRot();
      this.yRotO = this.getYRot();
      this.recalculateBoundingBox();
   }

   @Override
   protected AABB calculateBoundingBox(BlockPos pPos, Direction pDirection) {
      float $$2 = 0.46875F;
      Vec3 $$3 = Vec3.atCenterOf(pPos).relative(pDirection, -0.46875);
      Direction.Axis $$4 = pDirection.getAxis();
      double $$5 = $$4 == Direction.Axis.X ? 0.0625 : 0.75;
      double $$6 = $$4 == Direction.Axis.Y ? 0.0625 : 0.75;
      double $$7 = $$4 == Direction.Axis.Z ? 0.0625 : 0.75;
      return AABB.ofSize($$3, $$5, $$6, $$7);
   }

   @Override
   public boolean survives() {
      if (this.fixed) {
         return true;
      } else if (!this.level().noCollision(this)) {
         return false;
      } else {
         BlockState $$0 = this.level().getBlockState(this.pos.relative(this.direction.getOpposite()));
         return $$0.isSolid() || this.direction.getAxis().isHorizontal() && DiodeBlock.isDiode($$0)
            ? this.level().getEntities(this, this.getBoundingBox(), HANGING_ENTITY).isEmpty()
            : false;
      }
   }

   @Override
   public void move(MoverType pType, Vec3 pPos) {
      if (!this.fixed) {
         super.move(pType, pPos);
      }
   }

   @Override
   public void push(double pX, double pY, double pZ) {
      if (!this.fixed) {
         super.push(pX, pY, pZ);
      }
   }

   @Override
   public void kill() {
      this.removeFramedMap(this.getItem());
      super.kill();
   }

   @Override
   public boolean hurt(DamageSource pSource, float pAmount) {
      if (this.fixed) {
         return !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !pSource.isCreativePlayer() ? false : super.hurt(pSource, pAmount);
      } else if (this.isInvulnerableTo(pSource)) {
         return false;
      } else if (!pSource.is(DamageTypeTags.IS_EXPLOSION) && !this.getItem().isEmpty()) {
         if (!this.level().isClientSide) {
            this.dropItem(pSource.getEntity(), false);
            this.gameEvent(GameEvent.BLOCK_CHANGE, pSource.getEntity());
            this.playSound(this.getRemoveItemSound(), 1.0F, 1.0F);
         }

         return true;
      } else {
         return super.hurt(pSource, pAmount);
      }
   }

   public SoundEvent getRemoveItemSound() {
      return SoundEvents.ITEM_FRAME_REMOVE_ITEM;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double pDistance) {
      double $$1 = 16.0;
      $$1 *= 64.0 * getViewScale();
      return pDistance < $$1 * $$1;
   }

   @Override
   public void dropItem(@Nullable Entity pBrokenEntity) {
      this.playSound(this.getBreakSound(), 1.0F, 1.0F);
      this.dropItem(pBrokenEntity, true);
      this.gameEvent(GameEvent.BLOCK_CHANGE, pBrokenEntity);
   }

   public SoundEvent getBreakSound() {
      return SoundEvents.ITEM_FRAME_BREAK;
   }

   @Override
   public void playPlacementSound() {
      this.playSound(this.getPlaceSound(), 1.0F, 1.0F);
   }

   public SoundEvent getPlaceSound() {
      return SoundEvents.ITEM_FRAME_PLACE;
   }

   private void dropItem(@Nullable Entity pEntity, boolean pDropSelf) {
      if (!this.fixed) {
         ItemStack $$2 = this.getItem();
         this.setItem(ItemStack.EMPTY);
         if (!this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            if (pEntity == null) {
               this.removeFramedMap($$2);
            }
         } else {
            if (pEntity instanceof Player $$3 && $$3.hasInfiniteMaterials()) {
               this.removeFramedMap($$2);
               return;
            }

            if (pDropSelf) {
               this.spawnAtLocation(this.getFrameItemStack());
            }

            if (!$$2.isEmpty()) {
               $$2 = $$2.copy();
               this.removeFramedMap($$2);
               if (this.random.nextFloat() < this.dropChance) {
                  this.spawnAtLocation($$2);
               }
            }
         }
      }
   }

   private void removeFramedMap(ItemStack pStack) {
      MapId $$1 = this.getFramedMapId(pStack);
      if ($$1 != null) {
         MapItemSavedData $$2 = MapItem.getSavedData($$1, this.level());
         if ($$2 != null) {
            $$2.removedFromFrame(this.pos, this.getId());
            $$2.setDirty(true);
         }
      }

      pStack.setEntityRepresentation(null);
   }

   public ItemStack getItem() {
      return this.getEntityData().get(DATA_ITEM);
   }

   @Nullable
   public MapId getFramedMapId(ItemStack pStack) {
      return pStack.get(DataComponents.MAP_ID);
   }

   public boolean hasFramedMap() {
      return this.getItem().has(DataComponents.MAP_ID);
   }

   public void setItem(ItemStack p_31806_) {
      this.setItem(p_31806_, true);
   }

   public void setItem(ItemStack pStack, boolean pUpdateNeighbours) {
      if (!pStack.isEmpty()) {
         pStack = pStack.copyWithCount(1);
      }

      this.onItemChanged(pStack);
      this.getEntityData().set(DATA_ITEM, pStack);
      if (!pStack.isEmpty()) {
         this.playSound(this.getAddItemSound(), 1.0F, 1.0F);
      }

      if (pUpdateNeighbours && this.pos != null) {
         this.level().updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
      }
   }

   public SoundEvent getAddItemSound() {
      return SoundEvents.ITEM_FRAME_ADD_ITEM;
   }

   @Override
   public SlotAccess getSlot(int pSlot) {
      return pSlot == 0 ? SlotAccess.of(this::getItem, this::setItem) : super.getSlot(pSlot);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
      if (pKey.equals(DATA_ITEM)) {
         this.onItemChanged(this.getItem());
      }
   }

   private void onItemChanged(ItemStack pItem) {
      if (!pItem.isEmpty() && pItem.getFrame() != this) {
         pItem.setEntityRepresentation(this);
      }

      this.recalculateBoundingBox();
   }

   public int getRotation() {
      return this.getEntityData().get(DATA_ROTATION);
   }

   public void setRotation(int pRotation) {
      this.setRotation(pRotation, true);
   }

   private void setRotation(int pRotation, boolean pUpdateNeighbours) {
      this.getEntityData().set(DATA_ROTATION, pRotation % 8);
      if (pUpdateNeighbours && this.pos != null) {
         this.level().updateNeighbourForOutputSignal(this.pos, Blocks.AIR);
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      if (!this.getItem().isEmpty()) {
         pCompound.put("Item", this.getItem().save(this.registryAccess()));
         pCompound.putByte("ItemRotation", (byte)this.getRotation());
         pCompound.putFloat("ItemDropChance", this.dropChance);
      }

      pCompound.putByte("Facing", (byte)this.direction.get3DDataValue());
      pCompound.putBoolean("Invisible", this.isInvisible());
      pCompound.putBoolean("Fixed", this.fixed);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      ItemStack $$2;
      if (pCompound.contains("Item", 10)) {
         CompoundTag $$1 = pCompound.getCompound("Item");
         $$2 = ItemStack.parse(this.registryAccess(), $$1).orElse(ItemStack.EMPTY);
      } else {
         $$2 = ItemStack.EMPTY;
      }

      ItemStack $$4 = this.getItem();
      if (!$$4.isEmpty() && !ItemStack.matches($$2, $$4)) {
         this.removeFramedMap($$4);
      }

      this.setItem($$2, false);
      if (!$$2.isEmpty()) {
         this.setRotation(pCompound.getByte("ItemRotation"), false);
         if (pCompound.contains("ItemDropChance", 99)) {
            this.dropChance = pCompound.getFloat("ItemDropChance");
         }
      }

      this.setDirection(Direction.from3DDataValue(pCompound.getByte("Facing")));
      this.setInvisible(pCompound.getBoolean("Invisible"));
      this.fixed = pCompound.getBoolean("Fixed");
   }

   @Override
   public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
      ItemStack $$2 = pPlayer.getItemInHand(pHand);
      boolean $$3 = !this.getItem().isEmpty();
      boolean $$4 = !$$2.isEmpty();
      if (this.fixed) {
         return InteractionResult.PASS;
      } else if (!this.level().isClientSide) {
         if (!$$3) {
            if ($$4 && !this.isRemoved()) {
               if ($$2.is(Items.FILLED_MAP)) {
                  MapItemSavedData $$5 = MapItem.getSavedData($$2, this.level());
                  if ($$5 != null && $$5.isTrackedCountOverLimit(256)) {
                     return InteractionResult.FAIL;
                  }
               }

               this.setItem($$2);
               this.gameEvent(GameEvent.BLOCK_CHANGE, pPlayer);
               $$2.consume(1, pPlayer);
            }
         } else {
            this.playSound(this.getRotateItemSound(), 1.0F, 1.0F);
            this.setRotation(this.getRotation() + 1);
            this.gameEvent(GameEvent.BLOCK_CHANGE, pPlayer);
         }

         return InteractionResult.CONSUME;
      } else {
         return !$$3 && !$$4 ? InteractionResult.PASS : InteractionResult.SUCCESS;
      }
   }

   public SoundEvent getRotateItemSound() {
      return SoundEvents.ITEM_FRAME_ROTATE_ITEM;
   }

   public int getAnalogOutput() {
      return this.getItem().isEmpty() ? 0 : this.getRotation() % 8 + 1;
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity pEntity) {
      return new ClientboundAddEntityPacket(this, this.direction.get3DDataValue(), this.getPos());
   }

   @Override
   public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
      super.recreateFromPacket(pPacket);
      this.setDirection(Direction.from3DDataValue(pPacket.getData()));
   }

   @Override
   public ItemStack getPickResult() {
      ItemStack $$0 = this.getItem();
      return $$0.isEmpty() ? this.getFrameItemStack() : $$0.copy();
   }

   protected ItemStack getFrameItemStack() {
      return new ItemStack(Items.ITEM_FRAME);
   }

   @Override
   public float getVisualRotationYInDegrees() {
      Direction $$0 = this.getDirection();
      int $$1 = $$0.getAxis().isVertical() ? 90 * $$0.getAxisDirection().getStep() : 0;
      return (float)Mth.wrapDegrees(180 + $$0.get2DDataValue() * 90 + this.getRotation() * 45 + $$1);
   }
}
