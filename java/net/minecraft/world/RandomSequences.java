package net.minecraft.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class RandomSequences extends SavedData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final long worldSeed;
   private int salt;
   private boolean includeWorldSeed = true;
   private boolean includeSequenceId = true;
   private final Map<ResourceLocation, RandomSequence> sequences = new Object2ObjectOpenHashMap();

   public static SavedData.Factory<RandomSequences> factory(long pSeed) {
      return new SavedData.Factory<>(
         () -> new RandomSequences(pSeed), (p_296656_, p_331296_) -> load(pSeed, p_296656_), DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
      );
   }

   public RandomSequences(long pSeed) {
      this.worldSeed = pSeed;
   }

   public RandomSource get(ResourceLocation pLocation) {
      RandomSource $$1 = this.sequences.computeIfAbsent(pLocation, this::createSequence).random();
      return new RandomSequences.DirtyMarkingRandomSource($$1);
   }

   private RandomSequence createSequence(ResourceLocation p_299723_) {
      return this.createSequence(p_299723_, this.salt, this.includeWorldSeed, this.includeSequenceId);
   }

   private RandomSequence createSequence(ResourceLocation pLocation, int pSalt, boolean pIncludeWorldSeed, boolean pIncludeSequenceId) {
      long $$4 = (pIncludeWorldSeed ? this.worldSeed : 0L) ^ (long)pSalt;
      return new RandomSequence($$4, pIncludeSequenceId ? Optional.of(pLocation) : Optional.empty());
   }

   public void forAllSequences(BiConsumer<ResourceLocation, RandomSequence> pAction) {
      this.sequences.forEach(pAction);
   }

   public void setSeedDefaults(int pSalt, boolean pIncludeWorldSeed, boolean pIncludeSequenceId) {
      this.salt = pSalt;
      this.includeWorldSeed = pIncludeWorldSeed;
      this.includeSequenceId = pIncludeSequenceId;
   }

   @Override
   public CompoundTag save(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      pTag.putInt("salt", this.salt);
      pTag.putBoolean("include_world_seed", this.includeWorldSeed);
      pTag.putBoolean("include_sequence_id", this.includeSequenceId);
      CompoundTag $$2 = new CompoundTag();
      this.sequences
         .forEach(
            (p_326742_, p_326743_) -> $$2.put(p_326742_.toString(), (Tag)RandomSequence.CODEC.encodeStart(NbtOps.INSTANCE, p_326743_).result().orElseThrow())
         );
      pTag.put("sequences", $$2);
      return pTag;
   }

   private static boolean getBooleanWithDefault(CompoundTag pTag, String pKey, boolean pDefaultValue) {
      return pTag.contains(pKey, 1) ? pTag.getBoolean(pKey) : pDefaultValue;
   }

   public static RandomSequences load(long pSeed, CompoundTag pTag) {
      RandomSequences $$2 = new RandomSequences(pSeed);
      $$2.setSeedDefaults(
         pTag.getInt("salt"), getBooleanWithDefault(pTag, "include_world_seed", true), getBooleanWithDefault(pTag, "include_sequence_id", true)
      );
      CompoundTag $$3 = pTag.getCompound("sequences");

      for (String $$5 : $$3.getAllKeys()) {
         try {
            RandomSequence $$6 = (RandomSequence)((Pair)RandomSequence.CODEC.decode(NbtOps.INSTANCE, $$3.get($$5)).result().get()).getFirst();
            $$2.sequences.put(ResourceLocation.parse($$5), $$6);
         } catch (Exception var9) {
            LOGGER.error("Failed to load random sequence {}", $$5, var9);
         }
      }

      return $$2;
   }

   public int clear() {
      int $$0 = this.sequences.size();
      this.sequences.clear();
      return $$0;
   }

   public void reset(ResourceLocation pSequence) {
      this.sequences.put(pSequence, this.createSequence(pSequence));
   }

   public void reset(ResourceLocation pSequence, int pSeed, boolean pIncludeWorldSeed, boolean pIncludeSequenceId) {
      this.sequences.put(pSequence, this.createSequence(pSequence, pSeed, pIncludeWorldSeed, pIncludeSequenceId));
   }

   class DirtyMarkingRandomSource implements RandomSource {
      private final RandomSource random;

      DirtyMarkingRandomSource(final RandomSource pRandom) {
         this.random = pRandom;
      }

      @Override
      public RandomSource fork() {
         RandomSequences.this.setDirty();
         return this.random.fork();
      }

      @Override
      public PositionalRandomFactory forkPositional() {
         RandomSequences.this.setDirty();
         return this.random.forkPositional();
      }

      @Override
      public void setSeed(long pSeed) {
         RandomSequences.this.setDirty();
         this.random.setSeed(pSeed);
      }

      @Override
      public int nextInt() {
         RandomSequences.this.setDirty();
         return this.random.nextInt();
      }

      @Override
      public int nextInt(int pBound) {
         RandomSequences.this.setDirty();
         return this.random.nextInt(pBound);
      }

      @Override
      public long nextLong() {
         RandomSequences.this.setDirty();
         return this.random.nextLong();
      }

      @Override
      public boolean nextBoolean() {
         RandomSequences.this.setDirty();
         return this.random.nextBoolean();
      }

      @Override
      public float nextFloat() {
         RandomSequences.this.setDirty();
         return this.random.nextFloat();
      }

      @Override
      public double nextDouble() {
         RandomSequences.this.setDirty();
         return this.random.nextDouble();
      }

      @Override
      public double nextGaussian() {
         RandomSequences.this.setDirty();
         return this.random.nextGaussian();
      }

      @Override
      public boolean equals(Object pOther) {
         if (this == pOther) {
            return true;
         } else {
            return pOther instanceof RandomSequences.DirtyMarkingRandomSource $$1 ? this.random.equals($$1.random) : false;
         }
      }
   }
}
