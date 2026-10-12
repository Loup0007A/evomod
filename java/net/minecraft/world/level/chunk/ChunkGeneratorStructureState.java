package net.minecraft.world.level.chunk;

import com.google.common.base.Stopwatch;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.slf4j.Logger;

public class ChunkGeneratorStructureState {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final RandomState randomState;
   private final BiomeSource biomeSource;
   private final long levelSeed;
   private final long concentricRingsSeed;
   private final Map<Structure, List<StructurePlacement>> placementsForStructure = new Object2ObjectOpenHashMap();
   private final Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>> ringPositions = new Object2ObjectArrayMap();
   private boolean hasGeneratedPositions;
   private final List<Holder<StructureSet>> possibleStructureSets;

   public static ChunkGeneratorStructureState createForFlat(
      RandomState pRandomState, long pLevelSeed, BiomeSource pBiomeSource, Stream<Holder<StructureSet>> pStructureSets
   ) {
      List<Holder<StructureSet>> $$4 = pStructureSets.filter(p_255616_ -> hasBiomesForStructureSet(p_255616_.value(), pBiomeSource)).toList();
      return new ChunkGeneratorStructureState(pRandomState, pBiomeSource, pLevelSeed, 0L, $$4);
   }

   public static ChunkGeneratorStructureState createForNormal(
      RandomState pRandomState, long pSeed, BiomeSource pBiomeSource, HolderLookup<StructureSet> pStructureSetLookup
   ) {
      List<Holder<StructureSet>> $$4 = pStructureSetLookup.listElements()
         .filter(p_256144_ -> hasBiomesForStructureSet(p_256144_.value(), pBiomeSource))
         .collect(Collectors.toUnmodifiableList());
      return new ChunkGeneratorStructureState(pRandomState, pBiomeSource, pSeed, pSeed, $$4);
   }

   private static boolean hasBiomesForStructureSet(StructureSet pStructureSet, BiomeSource pBiomeSource) {
      Stream<Holder<Biome>> $$2 = pStructureSet.structures().stream().flatMap(p_255738_ -> {
         Structure $$1 = p_255738_.structure().value();
         return $$1.biomes().stream();
      });
      return $$2.anyMatch(pBiomeSource.possibleBiomes()::contains);
   }

   private ChunkGeneratorStructureState(
      RandomState pRandomState, BiomeSource pBiomeSource, long pLevelSeed, long pCocentricRingsSeed, List<Holder<StructureSet>> pPossibleStructureSets
   ) {
      this.randomState = pRandomState;
      this.levelSeed = pLevelSeed;
      this.biomeSource = pBiomeSource;
      this.concentricRingsSeed = pCocentricRingsSeed;
      this.possibleStructureSets = pPossibleStructureSets;
   }

   public List<Holder<StructureSet>> possibleStructureSets() {
      return this.possibleStructureSets;
   }

   private void generatePositions() {
      Set<Holder<Biome>> $$0 = this.biomeSource.possibleBiomes();
      this.possibleStructureSets().forEach(p_255638_ -> {
         StructureSet $$2 = p_255638_.value();
         boolean $$3 = false;

         for (StructureSet.StructureSelectionEntry $$4 : $$2.structures()) {
            Structure $$5 = $$4.structure().value();
            if ($$5.biomes().stream().anyMatch($$0::contains)) {
               this.placementsForStructure.computeIfAbsent($$5, p_256235_ -> new ArrayList<>()).add($$2.placement());
               $$3 = true;
            }
         }

         if ($$3 && $$2.placement() instanceof ConcentricRingsStructurePlacement $$7) {
            this.ringPositions.put($$7, this.generateRingPositions((Holder<StructureSet>)p_255638_, $$7));
         }
      });
   }

   private CompletableFuture<List<ChunkPos>> generateRingPositions(Holder<StructureSet> pStructureSet, ConcentricRingsStructurePlacement pPlacement) {
      if (pPlacement.count() == 0) {
         return CompletableFuture.completedFuture(List.of());
      } else {
         Stopwatch $$2 = Stopwatch.createStarted(Util.TICKER);
         int $$3 = pPlacement.distance();
         int $$4 = pPlacement.count();
         List<CompletableFuture<ChunkPos>> $$5 = new ArrayList<>($$4);
         int $$6 = pPlacement.spread();
         HolderSet<Biome> $$7 = pPlacement.preferredBiomes();
         RandomSource $$8 = RandomSource.create();
         $$8.setSeed(this.concentricRingsSeed);
         double $$9 = $$8.nextDouble() * Math.PI * 2.0;
         int $$10 = 0;
         int $$11 = 0;

         for (int $$12 = 0; $$12 < $$4; $$12++) {
            double $$13 = (double)(4 * $$3 + $$3 * $$11 * 6) + ($$8.nextDouble() - 0.5) * (double)$$3 * 2.5;
            int $$14 = (int)Math.round(Math.cos($$9) * $$13);
            int $$15 = (int)Math.round(Math.sin($$9) * $$13);
            RandomSource $$16 = $$8.fork();
            $$5.add(
               CompletableFuture.supplyAsync(
                  () -> {
                     Pair<BlockPos, Holder<Biome>> $$4x = this.biomeSource
                        .findBiomeHorizontal(
                           SectionPos.sectionToBlockCoord($$14, 8),
                           0,
                           SectionPos.sectionToBlockCoord($$15, 8),
                           112,
                           $$7::contains,
                           $$16,
                           this.randomState.sampler()
                        );
                     if ($$4x != null) {
                        BlockPos $$5x = (BlockPos)$$4x.getFirst();
                        return new ChunkPos(SectionPos.blockToSectionCoord($$5x.getX()), SectionPos.blockToSectionCoord($$5x.getZ()));
                     } else {
                        return new ChunkPos($$14, $$15);
                     }
                  },
                  Util.backgroundExecutor()
               )
            );
            $$9 += (Math.PI * 2) / (double)$$6;
            if (++$$10 == $$6) {
               $$11++;
               $$10 = 0;
               $$6 += 2 * $$6 / ($$11 + 1);
               $$6 = Math.min($$6, $$4 - $$12);
               $$9 += $$8.nextDouble() * Math.PI * 2.0;
            }
         }

         return Util.sequence($$5).thenApply(p_256372_ -> {
            double $$3x = (double)$$2.stop().elapsed(TimeUnit.MILLISECONDS) / 1000.0;
            LOGGER.debug("Calculation for {} took {}s", pStructureSet, $$3x);
            return p_256372_;
         });
      }
   }

   public void ensureStructuresGenerated() {
      if (!this.hasGeneratedPositions) {
         this.generatePositions();
         this.hasGeneratedPositions = true;
      }
   }

   @Nullable
   public List<ChunkPos> getRingPositionsFor(ConcentricRingsStructurePlacement pPlacement) {
      this.ensureStructuresGenerated();
      CompletableFuture<List<ChunkPos>> $$1 = this.ringPositions.get(pPlacement);
      return $$1 != null ? $$1.join() : null;
   }

   public List<StructurePlacement> getPlacementsForStructure(Holder<Structure> pStructure) {
      this.ensureStructuresGenerated();
      return this.placementsForStructure.getOrDefault(pStructure.value(), List.of());
   }

   public RandomState randomState() {
      return this.randomState;
   }

   public boolean hasStructureChunkInRange(Holder<StructureSet> pStructureSet, int pX, int pZ, int pRange) {
      StructurePlacement $$4 = pStructureSet.value().placement();

      for (int $$5 = pX - pRange; $$5 <= pX + pRange; $$5++) {
         for (int $$6 = pZ - pRange; $$6 <= pZ + pRange; $$6++) {
            if ($$4.isStructureChunk(this, $$5, $$6)) {
               return true;
            }
         }
      }

      return false;
   }

   public long getLevelSeed() {
      return this.levelSeed;
   }
}
