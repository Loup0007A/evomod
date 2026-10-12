package net.minecraft.world.entity.decoration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Painting extends HangingEntity implements VariantHolder<Holder<PaintingVariant>> {
   private static final EntityDataAccessor<Holder<PaintingVariant>> DATA_PAINTING_VARIANT_ID = SynchedEntityData.defineId(
      Painting.class, EntityDataSerializers.PAINTING_VARIANT
   );
   public static final MapCodec<Holder<PaintingVariant>> VARIANT_MAP_CODEC = PaintingVariant.CODEC.fieldOf("variant");
   public static final Codec<Holder<PaintingVariant>> VARIANT_CODEC = VARIANT_MAP_CODEC.codec();
   public static final float DEPTH = 0.0625F;

   public Painting(EntityType<? extends Painting> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
      pBuilder.define(DATA_PAINTING_VARIANT_ID, this.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT).getAny().orElseThrow());
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
      if (DATA_PAINTING_VARIANT_ID.equals(pKey)) {
         this.recalculateBoundingBox();
      }
   }

   public void setVariant(Holder<PaintingVariant> p_218892_) {
      this.entityData.set(DATA_PAINTING_VARIANT_ID, p_218892_);
   }

   public Holder<PaintingVariant> getVariant() {
      return this.entityData.get(DATA_PAINTING_VARIANT_ID);
   }

   public static Optional<Painting> create(Level pLevel, BlockPos pPos, Direction pDirection) {
      Painting $$3 = new Painting(pLevel, pPos);
      List<Holder<PaintingVariant>> $$4 = new ArrayList<>();
      pLevel.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT).getTagOrEmpty(PaintingVariantTags.PLACEABLE).forEach($$4::add);
      if ($$4.isEmpty()) {
         return Optional.empty();
      } else {
         $$3.setDirection(pDirection);
         $$4.removeIf(p_341439_ -> {
            $$3.setVariant((Holder<PaintingVariant>)p_341439_);
            return !$$3.survives();
         });
         if ($$4.isEmpty()) {
            return Optional.empty();
         } else {
            int $$5 = $$4.stream().mapToInt(Painting::variantArea).max().orElse(0);
            $$4.removeIf(p_218883_ -> variantArea((Holder<PaintingVariant>)p_218883_) < $$5);
            Optional<Holder<PaintingVariant>> $$6 = Util.getRandomSafe($$4, $$3.random);
            if ($$6.isEmpty()) {
               return Optional.empty();
            } else {
               $$3.setVariant($$6.get());
               $$3.setDirection(pDirection);
               return Optional.of($$3);
            }
         }
      }
   }

   private static int variantArea(Holder<PaintingVariant> p_218899_) {
      return p_218899_.value().area();
   }

   private Painting(Level pLevel, BlockPos pPos) {
      super(EntityType.PAINTING, pLevel, pPos);
   }

   public Painting(Level pLevel, BlockPos pPos, Direction pDirection, Holder<PaintingVariant> pVariant) {
      this(pLevel, pPos);
      this.setVariant(pVariant);
      this.setDirection(pDirection);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag pCompound) {
      VARIANT_CODEC.encodeStart(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), this.getVariant())
         .ifSuccess(p_327008_ -> pCompound.merge((CompoundTag)p_327008_));
      pCompound.putByte("facing", (byte)this.direction.get2DDataValue());
      super.addAdditionalSaveData(pCompound);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag pCompound) {
      VARIANT_CODEC.parse(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), pCompound).ifSuccess(this::setVariant);
      this.direction = Direction.from2DDataValue(pCompound.getByte("facing"));
      super.readAdditionalSaveData(pCompound);
      this.setDirection(this.direction);
   }

   @Override
   protected AABB calculateBoundingBox(BlockPos pPos, Direction pDirection) {
      float $$2 = 0.46875F;
      Vec3 $$3 = Vec3.atCenterOf(pPos).relative(pDirection, -0.46875);
      PaintingVariant $$4 = this.getVariant().value();
      double $$5 = this.offsetForPaintingSize($$4.width());
      double $$6 = this.offsetForPaintingSize($$4.height());
      Direction $$7 = pDirection.getCounterClockWise();
      Vec3 $$8 = $$3.relative($$7, $$5).relative(Direction.UP, $$6);
      Direction.Axis $$9 = pDirection.getAxis();
      double $$10 = $$9 == Direction.Axis.X ? 0.0625 : (double)$$4.width();
      double $$11 = (double)$$4.height();
      double $$12 = $$9 == Direction.Axis.Z ? 0.0625 : (double)$$4.width();
      return AABB.ofSize($$8, $$10, $$11, $$12);
   }

   private double offsetForPaintingSize(int pSize) {
      return pSize % 2 == 0 ? 0.5 : 0.0;
   }

   @Override
   public void dropItem(@Nullable Entity pBrokenEntity) {
      if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
         this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
         if (pBrokenEntity instanceof Player $$1 && $$1.hasInfiniteMaterials()) {
            return;
         }

         this.spawnAtLocation(Items.PAINTING);
      }
   }

   @Override
   public void playPlacementSound() {
      this.playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
   }

   @Override
   public void moveTo(double pX, double pY, double pZ, float pYaw, float pPitch) {
      this.setPos(pX, pY, pZ);
   }

   @Override
   public void lerpTo(double pX, double pY, double pZ, float pYRot, float pXRot, int pSteps) {
      this.setPos(pX, pY, pZ);
   }

   @Override
   public Vec3 trackingPosition() {
      return Vec3.atLowerCornerOf(this.pos);
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
      return new ItemStack(Items.PAINTING);
   }
}
