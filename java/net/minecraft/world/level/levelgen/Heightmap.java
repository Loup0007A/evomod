package net.minecraft.world.level.levelgen;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;

public class Heightmap {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final Predicate<BlockState> NOT_AIR = p_284913_ -> !p_284913_.isAir();
   static final Predicate<BlockState> MATERIAL_MOTION_BLOCKING = BlockBehaviour.BlockStateBase::blocksMotion;
   private final BitStorage data;
   private final Predicate<BlockState> isOpaque;
   private final ChunkAccess chunk;

   public Heightmap(ChunkAccess pChunk, Heightmap.Types pType) {
      this.isOpaque = pType.isOpaque();
      this.chunk = pChunk;
      int $$2 = Mth.ceillog2(pChunk.getHeight() + 1);
      this.data = new SimpleBitStorage($$2, 256);
   }

   public static void primeHeightmaps(ChunkAccess pChunk, Set<Heightmap.Types> pTypes) {
      int $$2 = pTypes.size();
      ObjectList<Heightmap> $$3 = new ObjectArrayList($$2);
      ObjectListIterator<Heightmap> $$4 = $$3.iterator();
      int $$5 = pChunk.getHighestSectionPosition() + 16;
      BlockPos.MutableBlockPos $$6 = new BlockPos.MutableBlockPos();

      for (int $$7 = 0; $$7 < 16; $$7++) {
         for (int $$8 = 0; $$8 < 16; $$8++) {
            for (Heightmap.Types $$9 : pTypes) {
               $$3.add(pChunk.getOrCreateHeightmapUnprimed($$9));
            }

            for (int $$10 = $$5 - 1; $$10 >= pChunk.getMinBuildHeight(); $$10--) {
               $$6.set($$7, $$10, $$8);
               BlockState $$11 = pChunk.getBlockState($$6);
               if (!$$11.is(Blocks.AIR)) {
                  while ($$4.hasNext()) {
                     Heightmap $$12 = (Heightmap)$$4.next();
                     if ($$12.isOpaque.test($$11)) {
                        $$12.setHeight($$7, $$8, $$10 + 1);
                        $$4.remove();
                     }
                  }

                  if ($$3.isEmpty()) {
                     break;
                  }

                  $$4.back($$2);
               }
            }
         }
      }
   }

   public boolean update(int pX, int pY, int pZ, BlockState pState) {
      int $$4 = this.getFirstAvailable(pX, pZ);
      if (pY <= $$4 - 2) {
         return false;
      } else {
         if (this.isOpaque.test(pState)) {
            if (pY >= $$4) {
               this.setHeight(pX, pZ, pY + 1);
               return true;
            }
         } else if ($$4 - 1 == pY) {
            BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos();

            for (int $$6 = pY - 1; $$6 >= this.chunk.getMinBuildHeight(); $$6--) {
               $$5.set(pX, $$6, pZ);
               if (this.isOpaque.test(this.chunk.getBlockState($$5))) {
                  this.setHeight(pX, pZ, $$6 + 1);
                  return true;
               }
            }

            this.setHeight(pX, pZ, this.chunk.getMinBuildHeight());
            return true;
         }

         return false;
      }
   }

   public int getFirstAvailable(int pX, int pZ) {
      return this.getFirstAvailable(getIndex(pX, pZ));
   }

   public int getHighestTaken(int pX, int pZ) {
      return this.getFirstAvailable(getIndex(pX, pZ)) - 1;
   }

   private int getFirstAvailable(int pIndex) {
      return this.data.get(pIndex) + this.chunk.getMinBuildHeight();
   }

   private void setHeight(int pX, int pZ, int pValue) {
      this.data.set(getIndex(pX, pZ), pValue - this.chunk.getMinBuildHeight());
   }

   public void setRawData(ChunkAccess pChunk, Heightmap.Types pType, long[] pData) {
      long[] $$3 = this.data.getRaw();
      if ($$3.length == pData.length) {
         System.arraycopy(pData, 0, $$3, 0, pData.length);
      } else {
         LOGGER.warn("Ignoring heightmap data for chunk " + pChunk.getPos() + ", size does not match; expected: " + $$3.length + ", got: " + pData.length);
         primeHeightmaps(pChunk, EnumSet.of(pType));
      }
   }

   public long[] getRawData() {
      return this.data.getRaw();
   }

   private static int getIndex(int pX, int pZ) {
      return pX + pZ * 16;
   }

   public static enum Types implements StringRepresentable {
      WORLD_SURFACE_WG("WORLD_SURFACE_WG", Heightmap.Usage.WORLDGEN, Heightmap.NOT_AIR),
      WORLD_SURFACE("WORLD_SURFACE", Heightmap.Usage.CLIENT, Heightmap.NOT_AIR),
      OCEAN_FLOOR_WG("OCEAN_FLOOR_WG", Heightmap.Usage.WORLDGEN, Heightmap.MATERIAL_MOTION_BLOCKING),
      OCEAN_FLOOR("OCEAN_FLOOR", Heightmap.Usage.LIVE_WORLD, Heightmap.MATERIAL_MOTION_BLOCKING),
      MOTION_BLOCKING("MOTION_BLOCKING", Heightmap.Usage.CLIENT, p_284915_ -> p_284915_.blocksMotion() || !p_284915_.getFluidState().isEmpty()),
      MOTION_BLOCKING_NO_LEAVES(
         "MOTION_BLOCKING_NO_LEAVES",
         Heightmap.Usage.LIVE_WORLD,
         p_284914_ -> (p_284914_.blocksMotion() || !p_284914_.getFluidState().isEmpty()) && !(p_284914_.getBlock() instanceof LeavesBlock)
      );

      public static final Codec<Heightmap.Types> CODEC = StringRepresentable.fromEnum(Heightmap.Types::values);
      private final String serializationKey;
      private final Heightmap.Usage usage;
      private final Predicate<BlockState> isOpaque;

      private Types(final String pSerializationKey, final Heightmap.Usage pUsage, final Predicate<BlockState> pIsOpaque) {
         this.serializationKey = pSerializationKey;
         this.usage = pUsage;
         this.isOpaque = pIsOpaque;
      }

      public String getSerializationKey() {
         return this.serializationKey;
      }

      public boolean sendToClient() {
         return this.usage == Heightmap.Usage.CLIENT;
      }

      public boolean keepAfterWorldgen() {
         return this.usage != Heightmap.Usage.WORLDGEN;
      }

      public Predicate<BlockState> isOpaque() {
         return this.isOpaque;
      }

      @Override
      public String getSerializedName() {
         return this.serializationKey;
      }
   }

   public static enum Usage {
      WORLDGEN,
      LIVE_WORLD,
      CLIENT;
   }
}
