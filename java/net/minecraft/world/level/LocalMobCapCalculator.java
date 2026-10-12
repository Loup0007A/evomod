package net.minecraft.world.level;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;

public class LocalMobCapCalculator {
   private final Long2ObjectMap<List<ServerPlayer>> playersNearChunk = new Long2ObjectOpenHashMap();
   private final Map<ServerPlayer, LocalMobCapCalculator.MobCounts> playerMobCounts = Maps.newHashMap();
   private final ChunkMap chunkMap;

   public LocalMobCapCalculator(ChunkMap pChunkMap) {
      this.chunkMap = pChunkMap;
   }

   private List<ServerPlayer> getPlayersNear(ChunkPos pPos) {
      return (List<ServerPlayer>)this.playersNearChunk.computeIfAbsent(pPos.toLong(), p_186511_ -> this.chunkMap.getPlayersCloseForSpawning(pPos));
   }

   public void addMob(ChunkPos pPos, MobCategory pCategory) {
      for (ServerPlayer $$2 : this.getPlayersNear(pPos)) {
         this.playerMobCounts.computeIfAbsent($$2, p_186503_ -> new LocalMobCapCalculator.MobCounts()).add(pCategory);
      }
   }

   public boolean canSpawn(MobCategory pCategory, ChunkPos pPos) {
      for (ServerPlayer $$2 : this.getPlayersNear(pPos)) {
         LocalMobCapCalculator.MobCounts $$3 = this.playerMobCounts.get($$2);
         if ($$3 == null || $$3.canSpawn(pCategory)) {
            return true;
         }
      }

      return false;
   }

   static class MobCounts {
      private final Object2IntMap<MobCategory> counts = new Object2IntOpenHashMap(MobCategory.values().length);

      public void add(MobCategory pCategory) {
         this.counts.computeInt(pCategory, (p_186520_, p_186521_) -> p_186521_ == null ? 1 : p_186521_ + 1);
      }

      public boolean canSpawn(MobCategory pCategory) {
         return this.counts.getOrDefault(pCategory, 0) < pCategory.getMaxInstancesPerChunk();
      }
   }
}
