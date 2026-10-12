package net.minecraft.world.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.world.ForgeChunkManager;

public class ForcedChunksSavedData extends SavedData {
   public static final String FILE_ID = "chunks";
   private static final String TAG_FORCED = "Forced";
   private final LongSet chunks;
   private ForgeChunkManager.TicketTracker<BlockPos> blockForcedChunks = new ForgeChunkManager.TicketTracker<>();
   private ForgeChunkManager.TicketTracker<UUID> entityForcedChunks = new ForgeChunkManager.TicketTracker<>();

   public static SavedData.Factory<ForcedChunksSavedData> factory() {
      return new SavedData.Factory<>(ForcedChunksSavedData::new, ForcedChunksSavedData::load, DataFixTypes.SAVED_DATA_FORCED_CHUNKS);
   }

   private ForcedChunksSavedData(LongSet pChunks) {
      this.chunks = pChunks;
   }

   public ForcedChunksSavedData() {
      this(new LongOpenHashSet());
   }

   public static ForcedChunksSavedData load(CompoundTag p_151484_, HolderLookup.Provider p_329967_) {
      ForcedChunksSavedData ret = new ForcedChunksSavedData(new LongOpenHashSet(p_151484_.getLongArray("Forced")));
      ForgeChunkManager.readForgeForcedChunks(p_151484_, ret.blockForcedChunks, ret.entityForcedChunks);
      return ret;
   }

   @Override
   public CompoundTag save(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      pTag.putLongArray("Forced", this.chunks.toLongArray());
      ForgeChunkManager.writeForgeForcedChunks(pTag, this.blockForcedChunks, this.entityForcedChunks);
      return pTag;
   }

   public LongSet getChunks() {
      return this.chunks;
   }

   public ForgeChunkManager.TicketTracker<BlockPos> getBlockForcedChunks() {
      return this.blockForcedChunks;
   }

   public ForgeChunkManager.TicketTracker<UUID> getEntityForcedChunks() {
      return this.entityForcedChunks;
   }
}
