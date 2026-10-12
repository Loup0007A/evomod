package net.minecraft.core;

import com.google.common.collect.AbstractIterator;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.concurrent.Immutable;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

@Immutable
public class BlockPos extends Vec3i {
   public static final Codec<BlockPos> CODEC = Codec.INT_STREAM
      .comapFlatMap(
         p_325638_ -> Util.fixedSize(p_325638_, 3).map(p_175270_ -> new BlockPos(p_175270_[0], p_175270_[1], p_175270_[2])),
         p_121924_ -> IntStream.of(p_121924_.getX(), p_121924_.getY(), p_121924_.getZ())
      )
      .stable();
   public static final StreamCodec<ByteBuf, BlockPos> STREAM_CODEC = new StreamCodec<ByteBuf, BlockPos>() {
      public BlockPos decode(ByteBuf p_335731_) {
         return FriendlyByteBuf.readBlockPos(p_335731_);
      }

      public void encode(ByteBuf p_329093_, BlockPos p_330029_) {
         FriendlyByteBuf.writeBlockPos(p_329093_, p_330029_);
      }
   };
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final BlockPos ZERO = new BlockPos(0, 0, 0);
   private static final int PACKED_X_LENGTH = 1 + Mth.log2(Mth.smallestEncompassingPowerOfTwo(30000000));
   private static final int PACKED_Z_LENGTH = PACKED_X_LENGTH;
   public static final int PACKED_Y_LENGTH = 64 - PACKED_X_LENGTH - PACKED_Z_LENGTH;
   private static final long PACKED_X_MASK = (1L << PACKED_X_LENGTH) - 1L;
   private static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
   private static final long PACKED_Z_MASK = (1L << PACKED_Z_LENGTH) - 1L;
   private static final int Y_OFFSET = 0;
   private static final int Z_OFFSET = PACKED_Y_LENGTH;
   private static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_Z_LENGTH;

   public BlockPos(int pX, int pY, int pZ) {
      super(pX, pY, pZ);
   }

   public BlockPos(Vec3i pVector) {
      this(pVector.getX(), pVector.getY(), pVector.getZ());
   }

   public static long offset(long pPos, Direction pDirection) {
      return offset(pPos, pDirection.getStepX(), pDirection.getStepY(), pDirection.getStepZ());
   }

   public static long offset(long pPos, int pDx, int pDy, int pDz) {
      return asLong(getX(pPos) + pDx, getY(pPos) + pDy, getZ(pPos) + pDz);
   }

   public static int getX(long pPackedPos) {
      return (int)(pPackedPos << 64 - X_OFFSET - PACKED_X_LENGTH >> 64 - PACKED_X_LENGTH);
   }

   public static int getY(long pPackedPos) {
      return (int)(pPackedPos << 64 - PACKED_Y_LENGTH >> 64 - PACKED_Y_LENGTH);
   }

   public static int getZ(long pPackedPos) {
      return (int)(pPackedPos << 64 - Z_OFFSET - PACKED_Z_LENGTH >> 64 - PACKED_Z_LENGTH);
   }

   public static BlockPos of(long pPackedPos) {
      return new BlockPos(getX(pPackedPos), getY(pPackedPos), getZ(pPackedPos));
   }

   public static BlockPos containing(double pX, double pY, double pZ) {
      return new BlockPos(Mth.floor(pX), Mth.floor(pY), Mth.floor(pZ));
   }

   public static BlockPos containing(Position pPosition) {
      return containing(pPosition.x(), pPosition.y(), pPosition.z());
   }

   public static BlockPos min(BlockPos pPos1, BlockPos pPos2) {
      return new BlockPos(Math.min(pPos1.getX(), pPos2.getX()), Math.min(pPos1.getY(), pPos2.getY()), Math.min(pPos1.getZ(), pPos2.getZ()));
   }

   public static BlockPos max(BlockPos pPos1, BlockPos pPos2) {
      return new BlockPos(Math.max(pPos1.getX(), pPos2.getX()), Math.max(pPos1.getY(), pPos2.getY()), Math.max(pPos1.getZ(), pPos2.getZ()));
   }

   public long asLong() {
      return asLong(this.getX(), this.getY(), this.getZ());
   }

   public static long asLong(int pX, int pY, int pZ) {
      long $$3 = 0L;
      $$3 |= ((long)pX & PACKED_X_MASK) << X_OFFSET;
      $$3 |= ((long)pY & PACKED_Y_MASK) << 0;
      return $$3 | ((long)pZ & PACKED_Z_MASK) << Z_OFFSET;
   }

   public static long getFlatIndex(long pPackedPos) {
      return pPackedPos & -16L;
   }

   public BlockPos offset(int pDx, int pDy, int pDz) {
      return pDx == 0 && pDy == 0 && pDz == 0 ? this : new BlockPos(this.getX() + pDx, this.getY() + pDy, this.getZ() + pDz);
   }

   public Vec3 getCenter() {
      return Vec3.atCenterOf(this);
   }

   public Vec3 getBottomCenter() {
      return Vec3.atBottomCenterOf(this);
   }

   public BlockPos offset(Vec3i pVector) {
      return this.offset(pVector.getX(), pVector.getY(), pVector.getZ());
   }

   public BlockPos subtract(Vec3i pVector) {
      return this.offset(-pVector.getX(), -pVector.getY(), -pVector.getZ());
   }

   public BlockPos multiply(int pScalar) {
      if (pScalar == 1) {
         return this;
      } else {
         return pScalar == 0 ? ZERO : new BlockPos(this.getX() * pScalar, this.getY() * pScalar, this.getZ() * pScalar);
      }
   }

   public BlockPos above() {
      return this.relative(Direction.UP);
   }

   public BlockPos above(int pDistance) {
      return this.relative(Direction.UP, pDistance);
   }

   public BlockPos below() {
      return this.relative(Direction.DOWN);
   }

   public BlockPos below(int pDistance) {
      return this.relative(Direction.DOWN, pDistance);
   }

   public BlockPos north() {
      return this.relative(Direction.NORTH);
   }

   public BlockPos north(int pDistance) {
      return this.relative(Direction.NORTH, pDistance);
   }

   public BlockPos south() {
      return this.relative(Direction.SOUTH);
   }

   public BlockPos south(int pDistance) {
      return this.relative(Direction.SOUTH, pDistance);
   }

   public BlockPos west() {
      return this.relative(Direction.WEST);
   }

   public BlockPos west(int pDistance) {
      return this.relative(Direction.WEST, pDistance);
   }

   public BlockPos east() {
      return this.relative(Direction.EAST);
   }

   public BlockPos east(int pDistance) {
      return this.relative(Direction.EAST, pDistance);
   }

   public BlockPos relative(Direction pDirection) {
      return new BlockPos(this.getX() + pDirection.getStepX(), this.getY() + pDirection.getStepY(), this.getZ() + pDirection.getStepZ());
   }

   public BlockPos relative(Direction pDirection, int pDistance) {
      return pDistance == 0
         ? this
         : new BlockPos(
            this.getX() + pDirection.getStepX() * pDistance, this.getY() + pDirection.getStepY() * pDistance, this.getZ() + pDirection.getStepZ() * pDistance
         );
   }

   public BlockPos relative(Direction.Axis pAxis, int pAmount) {
      if (pAmount == 0) {
         return this;
      } else {
         int $$2 = pAxis == Direction.Axis.X ? pAmount : 0;
         int $$3 = pAxis == Direction.Axis.Y ? pAmount : 0;
         int $$4 = pAxis == Direction.Axis.Z ? pAmount : 0;
         return new BlockPos(this.getX() + $$2, this.getY() + $$3, this.getZ() + $$4);
      }
   }

   public BlockPos rotate(Rotation pRotation) {
      switch (pRotation) {
         case NONE:
         default:
            return this;
         case CLOCKWISE_90:
            return new BlockPos(-this.getZ(), this.getY(), this.getX());
         case CLOCKWISE_180:
            return new BlockPos(-this.getX(), this.getY(), -this.getZ());
         case COUNTERCLOCKWISE_90:
            return new BlockPos(this.getZ(), this.getY(), -this.getX());
      }
   }

   public BlockPos cross(Vec3i pVector) {
      return new BlockPos(
         this.getY() * pVector.getZ() - this.getZ() * pVector.getY(),
         this.getZ() * pVector.getX() - this.getX() * pVector.getZ(),
         this.getX() * pVector.getY() - this.getY() * pVector.getX()
      );
   }

   public BlockPos atY(int pY) {
      return new BlockPos(this.getX(), pY, this.getZ());
   }

   public BlockPos immutable() {
      return this;
   }

   public BlockPos.MutableBlockPos mutable() {
      return new BlockPos.MutableBlockPos(this.getX(), this.getY(), this.getZ());
   }

   public Vec3 clampLocationWithin(Vec3 pPos) {
      return new Vec3(
         Mth.clamp(pPos.x, (double)((float)this.getX() + 1.0E-5F), (double)this.getX() + 1.0 - 1.0E-5F),
         Mth.clamp(pPos.y, (double)((float)this.getY() + 1.0E-5F), (double)this.getY() + 1.0 - 1.0E-5F),
         Mth.clamp(pPos.z, (double)((float)this.getZ() + 1.0E-5F), (double)this.getZ() + 1.0 - 1.0E-5F)
      );
   }

   public static Iterable<BlockPos> randomInCube(RandomSource pRandom, int pAmount, BlockPos pCenter, int pRadius) {
      return randomBetweenClosed(
         pRandom,
         pAmount,
         pCenter.getX() - pRadius,
         pCenter.getY() - pRadius,
         pCenter.getZ() - pRadius,
         pCenter.getX() + pRadius,
         pCenter.getY() + pRadius,
         pCenter.getZ() + pRadius
      );
   }

   @Deprecated
   public static Stream<BlockPos> squareOutSouthEast(BlockPos pPos) {
      return Stream.of(pPos, pPos.south(), pPos.east(), pPos.south().east());
   }

   public static Iterable<BlockPos> randomBetweenClosed(RandomSource pRandom, int pAmount, int pMinX, int pMinY, int pMinZ, int pMaxX, int pMaxY, int pMaxZ) {
      int $$8 = pMaxX - pMinX + 1;
      int $$9 = pMaxY - pMinY + 1;
      int $$10 = pMaxZ - pMinZ + 1;
      return () -> new AbstractIterator<BlockPos>() {
            final BlockPos.MutableBlockPos nextPos = new BlockPos.MutableBlockPos();
            int counter = pAmount;

            protected BlockPos computeNext() {
               if (this.counter <= 0) {
                  return (BlockPos)this.endOfData();
               } else {
                  BlockPos $$0 = this.nextPos.set(pMinX + pRandom.nextInt($$8), pMinY + pRandom.nextInt($$9), pMinZ + pRandom.nextInt($$10));
                  this.counter--;
                  return $$0;
               }
            }
         };
   }

   public static Iterable<BlockPos> withinManhattan(BlockPos pPos, int pXSize, int pYSize, int pZSize) {
      int $$4 = pXSize + pYSize + pZSize;
      int $$5 = pPos.getX();
      int $$6 = pPos.getY();
      int $$7 = pPos.getZ();
      return () -> new AbstractIterator<BlockPos>() {
            private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            private int currentDepth;
            private int maxX;
            private int maxY;
            private int x;
            private int y;
            private boolean zMirror;

            protected BlockPos computeNext() {
               if (this.zMirror) {
                  this.zMirror = false;
                  this.cursor.setZ($$7 - (this.cursor.getZ() - $$7));
                  return this.cursor;
               } else {
                  BlockPos $$0;
                  for ($$0 = null; $$0 == null; this.y++) {
                     if (this.y > this.maxY) {
                        this.x++;
                        if (this.x > this.maxX) {
                           this.currentDepth++;
                           if (this.currentDepth > $$4) {
                              return (BlockPos)this.endOfData();
                           }

                           this.maxX = Math.min(pXSize, this.currentDepth);
                           this.x = -this.maxX;
                        }

                        this.maxY = Math.min(pYSize, this.currentDepth - Math.abs(this.x));
                        this.y = -this.maxY;
                     }

                     int $$1 = this.x;
                     int $$2 = this.y;
                     int $$3 = this.currentDepth - Math.abs($$1) - Math.abs($$2);
                     if ($$3 <= pZSize) {
                        this.zMirror = $$3 != 0;
                        $$0 = this.cursor.set($$5 + $$1, $$6 + $$2, $$7 + $$3);
                     }
                  }

                  return $$0;
               }
            }
         };
   }

   public static Optional<BlockPos> findClosestMatch(BlockPos pPos, int pWidth, int pHeight, Predicate<BlockPos> pPosFilter) {
      for (BlockPos $$4 : withinManhattan(pPos, pWidth, pHeight, pWidth)) {
         if (pPosFilter.test($$4)) {
            return Optional.of($$4);
         }
      }

      return Optional.empty();
   }

   public static Stream<BlockPos> withinManhattanStream(BlockPos pPos, int pXSize, int pYSize, int pZSize) {
      return StreamSupport.stream(withinManhattan(pPos, pXSize, pYSize, pZSize).spliterator(), false);
   }

   public static Iterable<BlockPos> betweenClosed(BlockPos pFirstPos, BlockPos pSecondPos) {
      return betweenClosed(
         Math.min(pFirstPos.getX(), pSecondPos.getX()),
         Math.min(pFirstPos.getY(), pSecondPos.getY()),
         Math.min(pFirstPos.getZ(), pSecondPos.getZ()),
         Math.max(pFirstPos.getX(), pSecondPos.getX()),
         Math.max(pFirstPos.getY(), pSecondPos.getY()),
         Math.max(pFirstPos.getZ(), pSecondPos.getZ())
      );
   }

   public static Stream<BlockPos> betweenClosedStream(BlockPos pFirstPos, BlockPos pSecondPos) {
      return StreamSupport.stream(betweenClosed(pFirstPos, pSecondPos).spliterator(), false);
   }

   public static Stream<BlockPos> betweenClosedStream(BoundingBox pBox) {
      return betweenClosedStream(
         Math.min(pBox.minX(), pBox.maxX()),
         Math.min(pBox.minY(), pBox.maxY()),
         Math.min(pBox.minZ(), pBox.maxZ()),
         Math.max(pBox.minX(), pBox.maxX()),
         Math.max(pBox.minY(), pBox.maxY()),
         Math.max(pBox.minZ(), pBox.maxZ())
      );
   }

   public static Stream<BlockPos> betweenClosedStream(AABB pAabb) {
      return betweenClosedStream(
         Mth.floor(pAabb.minX), Mth.floor(pAabb.minY), Mth.floor(pAabb.minZ), Mth.floor(pAabb.maxX), Mth.floor(pAabb.maxY), Mth.floor(pAabb.maxZ)
      );
   }

   public static Stream<BlockPos> betweenClosedStream(int pMinX, int pMinY, int pMinZ, int pMaxX, int pMaxY, int pMaxZ) {
      return StreamSupport.stream(betweenClosed(pMinX, pMinY, pMinZ, pMaxX, pMaxY, pMaxZ).spliterator(), false);
   }

   public static Iterable<BlockPos> betweenClosed(int pX1, int pY1, int pZ1, int pX2, int pY2, int pZ2) {
      int $$6 = pX2 - pX1 + 1;
      int $$7 = pY2 - pY1 + 1;
      int $$8 = pZ2 - pZ1 + 1;
      int $$9 = $$6 * $$7 * $$8;
      return () -> new AbstractIterator<BlockPos>() {
            private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            private int index;

            protected BlockPos computeNext() {
               if (this.index == $$9) {
                  return (BlockPos)this.endOfData();
               } else {
                  int $$0 = this.index % $$6;
                  int $$1 = this.index / $$6;
                  int $$2 = $$1 % $$7;
                  int $$3 = $$1 / $$7;
                  this.index++;
                  return this.cursor.set(pX1 + $$0, pY1 + $$2, pZ1 + $$3);
               }
            }
         };
   }

   public static Iterable<BlockPos.MutableBlockPos> spiralAround(BlockPos pCenter, int pSize, Direction pRotationDirection, Direction pExpansionDirection) {
      Validate.validState(pRotationDirection.getAxis() != pExpansionDirection.getAxis(), "The two directions cannot be on the same axis", new Object[0]);
      return () -> new AbstractIterator<BlockPos.MutableBlockPos>() {
            private final Direction[] directions = new Direction[]{
               pRotationDirection, pExpansionDirection, pRotationDirection.getOpposite(), pExpansionDirection.getOpposite()
            };
            private final BlockPos.MutableBlockPos cursor = pCenter.mutable().move(pExpansionDirection);
            private final int legs = 4 * pSize;
            private int leg = -1;
            private int legSize;
            private int legIndex;
            private int lastX = this.cursor.getX();
            private int lastY = this.cursor.getY();
            private int lastZ = this.cursor.getZ();

            protected BlockPos.MutableBlockPos computeNext() {
               this.cursor.set(this.lastX, this.lastY, this.lastZ).move(this.directions[(this.leg + 4) % 4]);
               this.lastX = this.cursor.getX();
               this.lastY = this.cursor.getY();
               this.lastZ = this.cursor.getZ();
               if (this.legIndex >= this.legSize) {
                  if (this.leg >= this.legs) {
                     return (BlockPos.MutableBlockPos)this.endOfData();
                  }

                  this.leg++;
                  this.legIndex = 0;
                  this.legSize = this.leg / 2 + 1;
               }

               this.legIndex++;
               return this.cursor;
            }
         };
   }

   public static int breadthFirstTraversal(
      BlockPos pStartPos, int pDepth, int pVisitLimit, BiConsumer<BlockPos, Consumer<BlockPos>> pAction, Predicate<BlockPos> pPredicate
   ) {
      Queue<Pair<BlockPos, Integer>> $$5 = new ArrayDeque<>();
      LongSet $$6 = new LongOpenHashSet();
      $$5.add(Pair.of(pStartPos, 0));
      int $$7 = 0;

      while (!$$5.isEmpty()) {
         Pair<BlockPos, Integer> $$8 = $$5.poll();
         BlockPos $$9 = (BlockPos)$$8.getLeft();
         int $$10 = (Integer)$$8.getRight();
         long $$11 = $$9.asLong();
         if ($$6.add($$11) && pPredicate.test($$9)) {
            if (++$$7 >= pVisitLimit) {
               return $$7;
            }

            if ($$10 < pDepth) {
               pAction.accept($$9, p_277234_ -> $$5.add(Pair.of(p_277234_, $$10 + 1)));
            }
         }
      }

      return $$7;
   }

   public static class MutableBlockPos extends BlockPos {
      public MutableBlockPos() {
         this(0, 0, 0);
      }

      public MutableBlockPos(int pX, int pY, int pZ) {
         super(pX, pY, pZ);
      }

      public MutableBlockPos(double pX, double pY, double pZ) {
         this(Mth.floor(pX), Mth.floor(pY), Mth.floor(pZ));
      }

      @Override
      public BlockPos offset(int pDx, int pDy, int pDz) {
         return super.offset(pDx, pDy, pDz).immutable();
      }

      @Override
      public BlockPos multiply(int pScalar) {
         return super.multiply(pScalar).immutable();
      }

      @Override
      public BlockPos relative(Direction pDirection, int pDistance) {
         return super.relative(pDirection, pDistance).immutable();
      }

      @Override
      public BlockPos relative(Direction.Axis pAxis, int pAmount) {
         return super.relative(pAxis, pAmount).immutable();
      }

      @Override
      public BlockPos rotate(Rotation pRotation) {
         return super.rotate(pRotation).immutable();
      }

      public BlockPos.MutableBlockPos set(int pX, int pY, int pZ) {
         this.setX(pX);
         this.setY(pY);
         this.setZ(pZ);
         return this;
      }

      public BlockPos.MutableBlockPos set(double pX, double pY, double pZ) {
         return this.set(Mth.floor(pX), Mth.floor(pY), Mth.floor(pZ));
      }

      public BlockPos.MutableBlockPos set(Vec3i pVector) {
         return this.set(pVector.getX(), pVector.getY(), pVector.getZ());
      }

      public BlockPos.MutableBlockPos set(long pPackedPos) {
         return this.set(getX(pPackedPos), getY(pPackedPos), getZ(pPackedPos));
      }

      public BlockPos.MutableBlockPos set(AxisCycle pCycle, int pX, int pY, int pZ) {
         return this.set(pCycle.cycle(pX, pY, pZ, Direction.Axis.X), pCycle.cycle(pX, pY, pZ, Direction.Axis.Y), pCycle.cycle(pX, pY, pZ, Direction.Axis.Z));
      }

      public BlockPos.MutableBlockPos setWithOffset(Vec3i pPos, Direction pDirection) {
         return this.set(pPos.getX() + pDirection.getStepX(), pPos.getY() + pDirection.getStepY(), pPos.getZ() + pDirection.getStepZ());
      }

      public BlockPos.MutableBlockPos setWithOffset(Vec3i pVector, int pOffsetX, int pOffsetY, int pOffsetZ) {
         return this.set(pVector.getX() + pOffsetX, pVector.getY() + pOffsetY, pVector.getZ() + pOffsetZ);
      }

      public BlockPos.MutableBlockPos setWithOffset(Vec3i pPos, Vec3i pOffset) {
         return this.set(pPos.getX() + pOffset.getX(), pPos.getY() + pOffset.getY(), pPos.getZ() + pOffset.getZ());
      }

      public BlockPos.MutableBlockPos move(Direction pDirection) {
         return this.move(pDirection, 1);
      }

      public BlockPos.MutableBlockPos move(Direction pDirection, int pN) {
         return this.set(this.getX() + pDirection.getStepX() * pN, this.getY() + pDirection.getStepY() * pN, this.getZ() + pDirection.getStepZ() * pN);
      }

      public BlockPos.MutableBlockPos move(int pX, int pY, int pZ) {
         return this.set(this.getX() + pX, this.getY() + pY, this.getZ() + pZ);
      }

      public BlockPos.MutableBlockPos move(Vec3i pOffset) {
         return this.set(this.getX() + pOffset.getX(), this.getY() + pOffset.getY(), this.getZ() + pOffset.getZ());
      }

      public BlockPos.MutableBlockPos clamp(Direction.Axis pAxis, int pMin, int pMax) {
         switch (pAxis) {
            case X:
               return this.set(Mth.clamp(this.getX(), pMin, pMax), this.getY(), this.getZ());
            case Y:
               return this.set(this.getX(), Mth.clamp(this.getY(), pMin, pMax), this.getZ());
            case Z:
               return this.set(this.getX(), this.getY(), Mth.clamp(this.getZ(), pMin, pMax));
            default:
               throw new IllegalStateException("Unable to clamp axis " + pAxis);
         }
      }

      public BlockPos.MutableBlockPos setX(int pX) {
         super.setX(pX);
         return this;
      }

      public BlockPos.MutableBlockPos setY(int pY) {
         super.setY(pY);
         return this;
      }

      public BlockPos.MutableBlockPos setZ(int pZ) {
         super.setZ(pZ);
         return this;
      }

      @Override
      public BlockPos immutable() {
         return new BlockPos(this);
      }
   }
}
