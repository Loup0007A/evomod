package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class LevelChunkSection {
   public static final int SECTION_WIDTH = 16;
   public static final int SECTION_HEIGHT = 16;
   public static final int SECTION_SIZE = 4096;
   public static final int BIOME_CONTAINER_BITS = 2;
   private short nonEmptyBlockCount;
   private short tickingBlockCount;
   private short tickingFluidCount;
   private final PalettedContainer<BlockState> states;
   private PalettedContainerRO<Holder<Biome>> biomes;

   public LevelChunkSection(PalettedContainer<BlockState> pStates, PalettedContainerRO<Holder<Biome>> pBiomes) {
      this.states = pStates;
      this.biomes = pBiomes;
      this.recalcBlockCounts();
   }

   public LevelChunkSection(Registry<Biome> pBiomeRegistry) {
      this.states = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
      this.biomes = new PalettedContainer<>(
         pBiomeRegistry.asHolderIdMap(), pBiomeRegistry.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES
      );
   }

   public BlockState getBlockState(int pX, int pY, int pZ) {
      return this.states.get(pX, pY, pZ);
   }

   public FluidState getFluidState(int pX, int pY, int pZ) {
      return this.states.get(pX, pY, pZ).getFluidState();
   }

   public void acquire() {
      this.states.acquire();
   }

   public void release() {
      this.states.release();
   }

   public BlockState setBlockState(int pX, int pY, int pZ, BlockState pState) {
      return this.setBlockState(pX, pY, pZ, pState, true);
   }

   public BlockState setBlockState(int pX, int pY, int pZ, BlockState pState, boolean pUseLocks) {
      BlockState $$5;
      if (pUseLocks) {
         $$5 = this.states.getAndSet(pX, pY, pZ, pState);
      } else {
         $$5 = this.states.getAndSetUnchecked(pX, pY, pZ, pState);
      }

      FluidState $$7 = $$5.getFluidState();
      FluidState $$8 = pState.getFluidState();
      if (!$$5.isAir()) {
         this.nonEmptyBlockCount--;
         if ($$5.isRandomlyTicking()) {
            this.tickingBlockCount--;
         }
      }

      if (!$$7.isEmpty()) {
         this.tickingFluidCount--;
      }

      if (!pState.isAir()) {
         this.nonEmptyBlockCount++;
         if (pState.isRandomlyTicking()) {
            this.tickingBlockCount++;
         }
      }

      if (!$$8.isEmpty()) {
         this.tickingFluidCount++;
      }

      return $$5;
   }

   public boolean hasOnlyAir() {
      return this.nonEmptyBlockCount == 0;
   }

   public boolean isRandomlyTicking() {
      return this.isRandomlyTickingBlocks() || this.isRandomlyTickingFluids();
   }

   public boolean isRandomlyTickingBlocks() {
      return this.tickingBlockCount > 0;
   }

   public boolean isRandomlyTickingFluids() {
      return this.tickingFluidCount > 0;
   }

   public void recalcBlockCounts() {
      class BlockCounter implements PalettedContainer.CountConsumer<BlockState> {
         public int nonEmptyBlockCount;
         public int tickingBlockCount;
         public int tickingFluidCount;

         public void accept(BlockState p_204444_, int p_204445_) {
            FluidState $$2 = p_204444_.getFluidState();
            if (!p_204444_.isAir()) {
               this.nonEmptyBlockCount += p_204445_;
               if (p_204444_.isRandomlyTicking()) {
                  this.tickingBlockCount += p_204445_;
               }
            }

            if (!$$2.isEmpty()) {
               this.nonEmptyBlockCount += p_204445_;
               if ($$2.isRandomlyTicking()) {
                  this.tickingFluidCount += p_204445_;
               }
            }
         }
      }

      BlockCounter $$0 = new BlockCounter();
      this.states.count($$0);
      this.nonEmptyBlockCount = (short)$$0.nonEmptyBlockCount;
      this.tickingBlockCount = (short)$$0.tickingBlockCount;
      this.tickingFluidCount = (short)$$0.tickingFluidCount;
   }

   public PalettedContainer<BlockState> getStates() {
      return this.states;
   }

   public PalettedContainerRO<Holder<Biome>> getBiomes() {
      return this.biomes;
   }

   public void read(FriendlyByteBuf pBuffer) {
      this.nonEmptyBlockCount = pBuffer.readShort();
      this.states.read(pBuffer);
      PalettedContainer<Holder<Biome>> $$1 = this.biomes.recreate();
      $$1.read(pBuffer);
      this.biomes = $$1;
   }

   public void readBiomes(FriendlyByteBuf pBuffer) {
      PalettedContainer<Holder<Biome>> $$1 = this.biomes.recreate();
      $$1.read(pBuffer);
      this.biomes = $$1;
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeShort(this.nonEmptyBlockCount);
      this.states.write(pBuffer);
      this.biomes.write(pBuffer);
   }

   public int getSerializedSize() {
      return 2 + this.states.getSerializedSize() + this.biomes.getSerializedSize();
   }

   public boolean maybeHas(Predicate<BlockState> pPredicate) {
      return this.states.maybeHas(pPredicate);
   }

   public Holder<Biome> getNoiseBiome(int pX, int pY, int pZ) {
      return this.biomes.get(pX, pY, pZ);
   }

   public void fillBiomesFromNoise(BiomeResolver pBiomeResolver, Climate.Sampler pClimateSampler, int pX, int pY, int pZ) {
      PalettedContainer<Holder<Biome>> $$5 = this.biomes.recreate();
      int $$6 = 4;

      for (int $$7 = 0; $$7 < 4; $$7++) {
         for (int $$8 = 0; $$8 < 4; $$8++) {
            for (int $$9 = 0; $$9 < 4; $$9++) {
               $$5.getAndSetUnchecked($$7, $$8, $$9, pBiomeResolver.getNoiseBiome(pX + $$7, pY + $$8, pZ + $$9, pClimateSampler));
            }
         }
      }

      this.biomes = $$5;
   }
}
