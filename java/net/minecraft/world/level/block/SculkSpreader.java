package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class SculkSpreader {
   public static final int MAX_GROWTH_RATE_RADIUS = 24;
   public static final int MAX_CHARGE = 1000;
   public static final float MAX_DECAY_FACTOR = 0.5F;
   private static final int MAX_CURSORS = 32;
   public static final int SHRIEKER_PLACEMENT_RATE = 11;
   final boolean isWorldGeneration;
   private final TagKey<Block> replaceableBlocks;
   private final int growthSpawnCost;
   private final int noGrowthRadius;
   private final int chargeDecayRate;
   private final int additionalDecayRate;
   private List<SculkSpreader.ChargeCursor> cursors = new ArrayList<>();
   private static final Logger LOGGER = LogUtils.getLogger();

   public SculkSpreader(
      boolean pIsWorldGeneration, TagKey<Block> pReplaceableBlocks, int pGrowthSpawnCoat, int pNoGrowthRadius, int pChargeDecayRate, int pAdditionalDecayRate
   ) {
      this.isWorldGeneration = pIsWorldGeneration;
      this.replaceableBlocks = pReplaceableBlocks;
      this.growthSpawnCost = pGrowthSpawnCoat;
      this.noGrowthRadius = pNoGrowthRadius;
      this.chargeDecayRate = pChargeDecayRate;
      this.additionalDecayRate = pAdditionalDecayRate;
   }

   public static SculkSpreader createLevelSpreader() {
      return new SculkSpreader(false, BlockTags.SCULK_REPLACEABLE, 10, 4, 10, 5);
   }

   public static SculkSpreader createWorldGenSpreader() {
      return new SculkSpreader(true, BlockTags.SCULK_REPLACEABLE_WORLD_GEN, 50, 1, 5, 10);
   }

   public TagKey<Block> replaceableBlocks() {
      return this.replaceableBlocks;
   }

   public int growthSpawnCost() {
      return this.growthSpawnCost;
   }

   public int noGrowthRadius() {
      return this.noGrowthRadius;
   }

   public int chargeDecayRate() {
      return this.chargeDecayRate;
   }

   public int additionalDecayRate() {
      return this.additionalDecayRate;
   }

   public boolean isWorldGeneration() {
      return this.isWorldGeneration;
   }

   @VisibleForTesting
   public List<SculkSpreader.ChargeCursor> getCursors() {
      return this.cursors;
   }

   public void clear() {
      this.cursors.clear();
   }

   public void load(CompoundTag pTag) {
      if (pTag.contains("cursors", 9)) {
         this.cursors.clear();
         List<SculkSpreader.ChargeCursor> $$1 = (List<SculkSpreader.ChargeCursor>)SculkSpreader.ChargeCursor.CODEC
            .listOf()
            .parse(new Dynamic(NbtOps.INSTANCE, pTag.getList("cursors", 10)))
            .resultOrPartial(LOGGER::error)
            .orElseGet(ArrayList::new);
         int $$2 = Math.min($$1.size(), 32);

         for (int $$3 = 0; $$3 < $$2; $$3++) {
            this.addCursor($$1.get($$3));
         }
      }
   }

   public void save(CompoundTag pTag) {
      SculkSpreader.ChargeCursor.CODEC
         .listOf()
         .encodeStart(NbtOps.INSTANCE, this.cursors)
         .resultOrPartial(LOGGER::error)
         .ifPresent(p_222273_ -> pTag.put("cursors", p_222273_));
   }

   public void addCursors(BlockPos pPos, int pCharge) {
      while (pCharge > 0) {
         int $$2 = Math.min(pCharge, 1000);
         this.addCursor(new SculkSpreader.ChargeCursor(pPos, $$2));
         pCharge -= $$2;
      }
   }

   private void addCursor(SculkSpreader.ChargeCursor pCursor) {
      if (this.cursors.size() < 32) {
         this.cursors.add(pCursor);
      }
   }

   public void updateCursors(LevelAccessor pLevel, BlockPos pPos, RandomSource pRandom, boolean pShouldConvertBlocks) {
      if (!this.cursors.isEmpty()) {
         List<SculkSpreader.ChargeCursor> $$4 = new ArrayList<>();
         Map<BlockPos, SculkSpreader.ChargeCursor> $$5 = new HashMap<>();
         Object2IntMap<BlockPos> $$6 = new Object2IntOpenHashMap();

         for (SculkSpreader.ChargeCursor $$7 : this.cursors) {
            $$7.update(pLevel, pPos, pRandom, this, pShouldConvertBlocks);
            if ($$7.charge <= 0) {
               pLevel.levelEvent(3006, $$7.getPos(), 0);
            } else {
               BlockPos $$8 = $$7.getPos();
               $$6.computeInt($$8, (p_222264_, p_222265_) -> (p_222265_ == null ? 0 : p_222265_) + $$7.charge);
               SculkSpreader.ChargeCursor $$9 = $$5.get($$8);
               if ($$9 == null) {
                  $$5.put($$8, $$7);
                  $$4.add($$7);
               } else if (!this.isWorldGeneration() && $$7.charge + $$9.charge <= 1000) {
                  $$9.mergeWith($$7);
               } else {
                  $$4.add($$7);
                  if ($$7.charge < $$9.charge) {
                     $$5.put($$8, $$7);
                  }
               }
            }
         }

         ObjectIterator var16 = $$6.object2IntEntrySet().iterator();

         while (var16.hasNext()) {
            Entry<BlockPos> $$10 = (Entry<BlockPos>)var16.next();
            BlockPos $$11 = (BlockPos)$$10.getKey();
            int $$12 = $$10.getIntValue();
            SculkSpreader.ChargeCursor $$13 = $$5.get($$11);
            Collection<Direction> $$14 = $$13 == null ? null : $$13.getFacingData();
            if ($$12 > 0 && $$14 != null) {
               int $$15 = (int)(Math.log1p((double)$$12) / 2.3F) + 1;
               int $$16 = ($$15 << 6) + MultifaceBlock.pack($$14);
               pLevel.levelEvent(3006, $$11, $$16);
            }
         }

         this.cursors = $$4;
      }
   }

   public static class ChargeCursor {
      private static final ObjectArrayList<Vec3i> NON_CORNER_NEIGHBOURS = Util.make(
         new ObjectArrayList(18),
         p_222338_ -> BlockPos.betweenClosedStream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1))
               .filter(p_222336_ -> (p_222336_.getX() == 0 || p_222336_.getY() == 0 || p_222336_.getZ() == 0) && !p_222336_.equals(BlockPos.ZERO))
               .map(BlockPos::immutable)
               .forEach(p_222338_::add)
      );
      public static final int MAX_CURSOR_DECAY_DELAY = 1;
      private BlockPos pos;
      int charge;
      private int updateDelay;
      private int decayDelay;
      @Nullable
      private Set<Direction> facings;
      private static final Codec<Set<Direction>> DIRECTION_SET = Direction.CODEC
         .listOf()
         .xmap(p_222340_ -> Sets.newEnumSet(p_222340_, Direction.class), Lists::newArrayList);
      public static final Codec<SculkSpreader.ChargeCursor> CODEC = RecordCodecBuilder.create(
         p_222330_ -> p_222330_.group(
                  BlockPos.CODEC.fieldOf("pos").forGetter(SculkSpreader.ChargeCursor::getPos),
                  Codec.intRange(0, 1000).fieldOf("charge").orElse(0).forGetter(SculkSpreader.ChargeCursor::getCharge),
                  Codec.intRange(0, 1).fieldOf("decay_delay").orElse(1).forGetter(SculkSpreader.ChargeCursor::getDecayDelay),
                  Codec.intRange(0, Integer.MAX_VALUE).fieldOf("update_delay").orElse(0).forGetter(p_222346_ -> p_222346_.updateDelay),
                  DIRECTION_SET.lenientOptionalFieldOf("facings").forGetter(p_222343_ -> Optional.ofNullable(p_222343_.getFacingData()))
               )
               .apply(p_222330_, SculkSpreader.ChargeCursor::new)
      );

      private ChargeCursor(BlockPos p_222299_, int p_222300_, int p_222301_, int p_222302_, Optional<Set<Direction>> p_222303_) {
         this.pos = p_222299_;
         this.charge = p_222300_;
         this.decayDelay = p_222301_;
         this.updateDelay = p_222302_;
         this.facings = p_222303_.orElse(null);
      }

      public ChargeCursor(BlockPos pPos, int pCharge) {
         this(pPos, pCharge, 1, 0, Optional.empty());
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public int getCharge() {
         return this.charge;
      }

      public int getDecayDelay() {
         return this.decayDelay;
      }

      @Nullable
      public Set<Direction> getFacingData() {
         return this.facings;
      }

      private boolean shouldUpdate(LevelAccessor pLevel, BlockPos pPos, boolean pIsWorldGeneration) {
         if (this.charge <= 0) {
            return false;
         } else if (pIsWorldGeneration) {
            return true;
         } else {
            return pLevel instanceof ServerLevel $$3 ? $$3.shouldTickBlocksAt(pPos) : false;
         }
      }

      public void update(LevelAccessor pLevel, BlockPos pPos, RandomSource pRandom, SculkSpreader pSpreader, boolean pShouldConvertBlocks) {
         if (this.shouldUpdate(pLevel, pPos, pSpreader.isWorldGeneration)) {
            if (this.updateDelay > 0) {
               this.updateDelay--;
            } else {
               BlockState $$5 = pLevel.getBlockState(this.pos);
               SculkBehaviour $$6 = getBlockBehaviour($$5);
               if (pShouldConvertBlocks && $$6.attemptSpreadVein(pLevel, this.pos, $$5, this.facings, pSpreader.isWorldGeneration())) {
                  if ($$6.canChangeBlockStateOnSpread()) {
                     $$5 = pLevel.getBlockState(this.pos);
                     $$6 = getBlockBehaviour($$5);
                  }

                  pLevel.playSound(null, this.pos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 1.0F);
               }

               this.charge = $$6.attemptUseCharge(this, pLevel, pPos, pRandom, pSpreader, pShouldConvertBlocks);
               if (this.charge <= 0) {
                  $$6.onDischarged(pLevel, $$5, this.pos, pRandom);
               } else {
                  BlockPos $$7 = getValidMovementPos(pLevel, this.pos, pRandom);
                  if ($$7 != null) {
                     $$6.onDischarged(pLevel, $$5, this.pos, pRandom);
                     this.pos = $$7.immutable();
                     if (pSpreader.isWorldGeneration() && !this.pos.closerThan(new Vec3i(pPos.getX(), this.pos.getY(), pPos.getZ()), 15.0)) {
                        this.charge = 0;
                        return;
                     }

                     $$5 = pLevel.getBlockState($$7);
                  }

                  if ($$5.getBlock() instanceof SculkBehaviour) {
                     this.facings = MultifaceBlock.availableFaces($$5);
                  }

                  this.decayDelay = $$6.updateDecayDelay(this.decayDelay);
                  this.updateDelay = $$6.getSculkSpreadDelay();
               }
            }
         }
      }

      void mergeWith(SculkSpreader.ChargeCursor pCursor) {
         this.charge = this.charge + pCursor.charge;
         pCursor.charge = 0;
         this.updateDelay = Math.min(this.updateDelay, pCursor.updateDelay);
      }

      private static SculkBehaviour getBlockBehaviour(BlockState pState) {
         return pState.getBlock() instanceof SculkBehaviour $$1 ? $$1 : SculkBehaviour.DEFAULT;
      }

      private static List<Vec3i> getRandomizedNonCornerNeighbourOffsets(RandomSource pRandom) {
         return Util.shuffledCopy(NON_CORNER_NEIGHBOURS, pRandom);
      }

      @Nullable
      private static BlockPos getValidMovementPos(LevelAccessor pLevel, BlockPos pPos, RandomSource pRandom) {
         BlockPos.MutableBlockPos $$3 = pPos.mutable();
         BlockPos.MutableBlockPos $$4 = pPos.mutable();

         for (Vec3i $$5 : getRandomizedNonCornerNeighbourOffsets(pRandom)) {
            $$4.setWithOffset(pPos, $$5);
            BlockState $$6 = pLevel.getBlockState($$4);
            if ($$6.getBlock() instanceof SculkBehaviour && isMovementUnobstructed(pLevel, pPos, $$4)) {
               $$3.set($$4);
               if (SculkVeinBlock.hasSubstrateAccess(pLevel, $$6, $$4)) {
                  break;
               }
            }
         }

         return $$3.equals(pPos) ? null : $$3;
      }

      private static boolean isMovementUnobstructed(LevelAccessor pLevel, BlockPos pFromPos, BlockPos pToPos) {
         if (pFromPos.distManhattan(pToPos) == 1) {
            return true;
         } else {
            BlockPos $$3 = pToPos.subtract(pFromPos);
            Direction $$4 = Direction.fromAxisAndDirection(
               Direction.Axis.X, $$3.getX() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE
            );
            Direction $$5 = Direction.fromAxisAndDirection(
               Direction.Axis.Y, $$3.getY() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE
            );
            Direction $$6 = Direction.fromAxisAndDirection(
               Direction.Axis.Z, $$3.getZ() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE
            );
            if ($$3.getX() == 0) {
               return isUnobstructed(pLevel, pFromPos, $$5) || isUnobstructed(pLevel, pFromPos, $$6);
            } else {
               return $$3.getY() == 0
                  ? isUnobstructed(pLevel, pFromPos, $$4) || isUnobstructed(pLevel, pFromPos, $$6)
                  : isUnobstructed(pLevel, pFromPos, $$4) || isUnobstructed(pLevel, pFromPos, $$5);
            }
         }
      }

      private static boolean isUnobstructed(LevelAccessor pLevel, BlockPos pPos, Direction pDirection) {
         BlockPos $$3 = pPos.relative(pDirection);
         return !pLevel.getBlockState($$3).isFaceSturdy(pLevel, $$3, pDirection.getOpposite());
      }
   }
}
