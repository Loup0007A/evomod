package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.slf4j.Logger;

public class BeehiveBlockEntity extends BlockEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String TAG_FLOWER_POS = "flower_pos";
   private static final String BEES = "bees";
   static final List<String> IGNORED_BEE_TAGS = Arrays.asList(
      "Air",
      "ArmorDropChances",
      "ArmorItems",
      "Brain",
      "CanPickUpLoot",
      "DeathTime",
      "FallDistance",
      "FallFlying",
      "Fire",
      "HandDropChances",
      "HandItems",
      "HurtByTimestamp",
      "HurtTime",
      "LeftHanded",
      "Motion",
      "NoGravity",
      "OnGround",
      "PortalCooldown",
      "Pos",
      "Rotation",
      "SleepingX",
      "SleepingY",
      "SleepingZ",
      "CannotEnterHiveTicks",
      "TicksSincePollination",
      "CropsGrownSincePollination",
      "hive_pos",
      "Passengers",
      "leash",
      "UUID"
   );
   public static final int MAX_OCCUPANTS = 3;
   private static final int MIN_TICKS_BEFORE_REENTERING_HIVE = 400;
   private static final int MIN_OCCUPATION_TICKS_NECTAR = 2400;
   public static final int MIN_OCCUPATION_TICKS_NECTARLESS = 600;
   private final List<BeehiveBlockEntity.BeeData> stored = Lists.newArrayList();
   @Nullable
   private BlockPos savedFlowerPos;

   public BeehiveBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super(BlockEntityType.BEEHIVE, pPos, pBlockState);
   }

   @Override
   public void setChanged() {
      if (this.isFireNearby()) {
         this.emptyAllLivingFromHive(null, this.level.getBlockState(this.getBlockPos()), BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
      }

      super.setChanged();
   }

   public boolean isFireNearby() {
      if (this.level == null) {
         return false;
      } else {
         for (BlockPos $$0 : BlockPos.betweenClosed(this.worldPosition.offset(-1, -1, -1), this.worldPosition.offset(1, 1, 1))) {
            if (this.level.getBlockState($$0).getBlock() instanceof FireBlock) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isEmpty() {
      return this.stored.isEmpty();
   }

   public boolean isFull() {
      return this.stored.size() == 3;
   }

   public void emptyAllLivingFromHive(@Nullable Player pPlayer, BlockState pState, BeehiveBlockEntity.BeeReleaseStatus pReleaseStatus) {
      List<Entity> $$3 = this.releaseAllOccupants(pState, pReleaseStatus);
      if (pPlayer != null) {
         for (Entity $$4 : $$3) {
            if ($$4 instanceof Bee) {
               Bee $$5 = (Bee)$$4;
               if (pPlayer.position().distanceToSqr($$4.position()) <= 16.0) {
                  if (!this.isSedated()) {
                     $$5.setTarget(pPlayer);
                  } else {
                     $$5.setStayOutOfHiveCountdown(400);
                  }
               }
            }
         }
      }
   }

   private List<Entity> releaseAllOccupants(BlockState pState, BeehiveBlockEntity.BeeReleaseStatus pReleaseStatus) {
      List<Entity> $$2 = Lists.newArrayList();
      this.stored
         .removeIf(p_327282_ -> releaseOccupant(this.level, this.worldPosition, pState, p_327282_.toOccupant(), $$2, pReleaseStatus, this.savedFlowerPos));
      if (!$$2.isEmpty()) {
         super.setChanged();
      }

      return $$2;
   }

   @VisibleForDebug
   public int getOccupantCount() {
      return this.stored.size();
   }

   public static int getHoneyLevel(BlockState pState) {
      return pState.getValue(BeehiveBlock.HONEY_LEVEL);
   }

   @VisibleForDebug
   public boolean isSedated() {
      return CampfireBlock.isSmokeyPos(this.level, this.getBlockPos());
   }

   public void addOccupant(Entity pOccupant) {
      if (this.stored.size() < 3) {
         pOccupant.stopRiding();
         pOccupant.ejectPassengers();
         this.storeBee(BeehiveBlockEntity.Occupant.of(pOccupant));
         if (this.level != null) {
            if (pOccupant instanceof Bee $$1 && $$1.hasSavedFlowerPos() && (!this.hasSavedFlowerPos() || this.level.random.nextBoolean())) {
               this.savedFlowerPos = $$1.getSavedFlowerPos();
            }

            BlockPos $$2 = this.getBlockPos();
            this.level.playSound(null, (double)$$2.getX(), (double)$$2.getY(), (double)$$2.getZ(), SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0F, 1.0F);
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, $$2, GameEvent.Context.of(pOccupant, this.getBlockState()));
         }

         pOccupant.discard();
         super.setChanged();
      }
   }

   public void storeBee(BeehiveBlockEntity.Occupant p_329282_) {
      this.stored.add(new BeehiveBlockEntity.BeeData(p_329282_));
   }

   private static boolean releaseOccupant(
      Level pLevel,
      BlockPos pPos,
      BlockState pState,
      BeehiveBlockEntity.Occupant pOccupant,
      @Nullable List<Entity> pStoredInHives,
      BeehiveBlockEntity.BeeReleaseStatus pReleaseStatus,
      @Nullable BlockPos pStoredFlowerPos
   ) {
      if ((pLevel.isNight() || pLevel.isRaining()) && pReleaseStatus != BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY) {
         return false;
      } else {
         Direction $$7 = pState.getValue(BeehiveBlock.FACING);
         BlockPos $$8 = pPos.relative($$7);
         boolean $$9 = !pLevel.getBlockState($$8).getCollisionShape(pLevel, $$8).isEmpty();
         if ($$9 && pReleaseStatus != BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY) {
            return false;
         } else {
            Entity $$10 = pOccupant.createEntity(pLevel, pPos);
            if ($$10 != null) {
               if ($$10 instanceof Bee $$11) {
                  if (pStoredFlowerPos != null && !$$11.hasSavedFlowerPos() && pLevel.random.nextFloat() < 0.9F) {
                     $$11.setSavedFlowerPos(pStoredFlowerPos);
                  }

                  if (pReleaseStatus == BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED) {
                     $$11.dropOffNectar();
                     if (pState.is(BlockTags.BEEHIVES, p_202037_ -> p_202037_.hasProperty(BeehiveBlock.HONEY_LEVEL))) {
                        int $$12 = getHoneyLevel(pState);
                        if ($$12 < 5) {
                           int $$13 = pLevel.random.nextInt(100) == 0 ? 2 : 1;
                           if ($$12 + $$13 > 5) {
                              $$13--;
                           }

                           pLevel.setBlockAndUpdate(pPos, pState.setValue(BeehiveBlock.HONEY_LEVEL, Integer.valueOf($$12 + $$13)));
                        }
                     }
                  }

                  if (pStoredInHives != null) {
                     pStoredInHives.add($$11);
                  }

                  float $$14 = $$10.getBbWidth();
                  double $$15 = $$9 ? 0.0 : 0.55 + (double)($$14 / 2.0F);
                  double $$16 = (double)pPos.getX() + 0.5 + $$15 * (double)$$7.getStepX();
                  double $$17 = (double)pPos.getY() + 0.5 - (double)($$10.getBbHeight() / 2.0F);
                  double $$18 = (double)pPos.getZ() + 0.5 + $$15 * (double)$$7.getStepZ();
                  $$10.moveTo($$16, $$17, $$18, $$10.getYRot(), $$10.getXRot());
               }

               pLevel.playSound(null, pPos, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.0F);
               pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of($$10, pLevel.getBlockState(pPos)));
               return pLevel.addFreshEntity($$10);
            } else {
               return false;
            }
         }
      }
   }

   private boolean hasSavedFlowerPos() {
      return this.savedFlowerPos != null;
   }

   private static void tickOccupants(Level pLevel, BlockPos pPos, BlockState pState, List<BeehiveBlockEntity.BeeData> pData, @Nullable BlockPos pSavedFlowerPos) {
      boolean $$5 = false;
      Iterator<BeehiveBlockEntity.BeeData> $$6 = pData.iterator();

      while ($$6.hasNext()) {
         BeehiveBlockEntity.BeeData $$7 = $$6.next();
         if ($$7.tick()) {
            BeehiveBlockEntity.BeeReleaseStatus $$8 = $$7.hasNectar()
               ? BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED
               : BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED;
            if (releaseOccupant(pLevel, pPos, pState, $$7.toOccupant(), null, $$8, pSavedFlowerPos)) {
               $$5 = true;
               $$6.remove();
            }
         }
      }

      if ($$5) {
         setChanged(pLevel, pPos, pState);
      }
   }

   public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, BeehiveBlockEntity pBeehive) {
      tickOccupants(pLevel, pPos, pState, pBeehive.stored, pBeehive.savedFlowerPos);
      if (!pBeehive.stored.isEmpty() && pLevel.getRandom().nextDouble() < 0.005) {
         double $$4 = (double)pPos.getX() + 0.5;
         double $$5 = (double)pPos.getY();
         double $$6 = (double)pPos.getZ() + 0.5;
         pLevel.playSound(null, $$4, $$5, $$6, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 1.0F, 1.0F);
      }

      DebugPackets.sendHiveInfo(pLevel, pPos, pState, pBeehive);
   }

   @Override
   protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.loadAdditional(pTag, pRegistries);
      this.stored.clear();
      if (pTag.contains("bees")) {
         BeehiveBlockEntity.Occupant.LIST_CODEC
            .parse(NbtOps.INSTANCE, pTag.get("bees"))
            .resultOrPartial(p_327283_ -> LOGGER.error("Failed to parse bees: '{}'", p_327283_))
            .ifPresent(p_327284_ -> p_327284_.forEach(this::storeBee));
      }

      this.savedFlowerPos = NbtUtils.readBlockPos(pTag, "flower_pos").orElse(null);
   }

   @Override
   protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.saveAdditional(pTag, pRegistries);
      pTag.put("bees", (Tag)BeehiveBlockEntity.Occupant.LIST_CODEC.encodeStart(NbtOps.INSTANCE, this.getBees()).getOrThrow());
      if (this.hasSavedFlowerPos()) {
         pTag.put("flower_pos", NbtUtils.writeBlockPos(this.savedFlowerPos));
      }
   }

   @Override
   protected void applyImplicitComponents(BlockEntity.DataComponentInput pComponentInput) {
      super.applyImplicitComponents(pComponentInput);
      this.stored.clear();
      List<BeehiveBlockEntity.Occupant> $$1 = pComponentInput.getOrDefault(DataComponents.BEES, List.of());
      $$1.forEach(this::storeBee);
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder pComponents) {
      super.collectImplicitComponents(pComponents);
      pComponents.set(DataComponents.BEES, this.getBees());
   }

   @Override
   public void removeComponentsFromTag(CompoundTag pTag) {
      super.removeComponentsFromTag(pTag);
      pTag.remove("bees");
   }

   private List<BeehiveBlockEntity.Occupant> getBees() {
      return this.stored.stream().map(BeehiveBlockEntity.BeeData::toOccupant).toList();
   }

   static class BeeData {
      private final BeehiveBlockEntity.Occupant occupant;
      private int ticksInHive;

      BeeData(BeehiveBlockEntity.Occupant pOccupant) {
         this.occupant = pOccupant;
         this.ticksInHive = pOccupant.ticksInHive();
      }

      public boolean tick() {
         return this.ticksInHive++ > this.occupant.minTicksInHive;
      }

      public BeehiveBlockEntity.Occupant toOccupant() {
         return new BeehiveBlockEntity.Occupant(this.occupant.entityData, this.ticksInHive, this.occupant.minTicksInHive);
      }

      public boolean hasNectar() {
         return this.occupant.entityData.getUnsafe().getBoolean("HasNectar");
      }
   }

   public static enum BeeReleaseStatus {
      HONEY_DELIVERED,
      BEE_RELEASED,
      EMERGENCY;
   }

   public static record Occupant(CustomData entityData, int ticksInHive, int minTicksInHive) {
      public static final Codec<BeehiveBlockEntity.Occupant> CODEC = RecordCodecBuilder.create(
         p_330401_ -> p_330401_.group(
                  CustomData.CODEC.optionalFieldOf("entity_data", CustomData.EMPTY).forGetter(BeehiveBlockEntity.Occupant::entityData),
                  Codec.INT.fieldOf("ticks_in_hive").forGetter(BeehiveBlockEntity.Occupant::ticksInHive),
                  Codec.INT.fieldOf("min_ticks_in_hive").forGetter(BeehiveBlockEntity.Occupant::minTicksInHive)
               )
               .apply(p_330401_, BeehiveBlockEntity.Occupant::new)
      );
      public static final Codec<List<BeehiveBlockEntity.Occupant>> LIST_CODEC = CODEC.listOf();
      public static final StreamCodec<ByteBuf, BeehiveBlockEntity.Occupant> STREAM_CODEC = StreamCodec.composite(
         CustomData.STREAM_CODEC,
         BeehiveBlockEntity.Occupant::entityData,
         ByteBufCodecs.VAR_INT,
         BeehiveBlockEntity.Occupant::ticksInHive,
         ByteBufCodecs.VAR_INT,
         BeehiveBlockEntity.Occupant::minTicksInHive,
         BeehiveBlockEntity.Occupant::new
      );

      public static BeehiveBlockEntity.Occupant of(Entity pEntity) {
         CompoundTag $$1 = new CompoundTag();
         pEntity.save($$1);
         BeehiveBlockEntity.IGNORED_BEE_TAGS.forEach($$1::remove);
         boolean $$2 = $$1.getBoolean("HasNectar");
         return new BeehiveBlockEntity.Occupant(CustomData.of($$1), 0, $$2 ? 2400 : 600);
      }

      public static BeehiveBlockEntity.Occupant create(int pTicksInHive) {
         CompoundTag $$1 = new CompoundTag();
         $$1.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.BEE).toString());
         return new BeehiveBlockEntity.Occupant(CustomData.of($$1), pTicksInHive, 600);
      }

      @Nullable
      public Entity createEntity(Level pLevel, BlockPos pPos) {
         CompoundTag $$2 = this.entityData.copyTag();
         BeehiveBlockEntity.IGNORED_BEE_TAGS.forEach($$2::remove);
         Entity $$3 = EntityType.loadEntityRecursive($$2, pLevel, p_334152_ -> p_334152_);
         if ($$3 != null && $$3.getType().is(EntityTypeTags.BEEHIVE_INHABITORS)) {
            $$3.setNoGravity(true);
            if ($$3 instanceof Bee $$4) {
               $$4.setHivePos(pPos);
               setBeeReleaseData(this.ticksInHive, $$4);
            }

            return $$3;
         } else {
            return null;
         }
      }

      private static void setBeeReleaseData(int pTicksInHive, Bee pBee) {
         int $$2 = pBee.getAge();
         if ($$2 < 0) {
            pBee.setAge(Math.min(0, $$2 + pTicksInHive));
         } else if ($$2 > 0) {
            pBee.setAge(Math.max(0, $$2 - pTicksInHive));
         }

         pBee.setInLoveTime(Math.max(0, pBee.getInLoveTime() - pTicksInHive));
      }
   }
}
