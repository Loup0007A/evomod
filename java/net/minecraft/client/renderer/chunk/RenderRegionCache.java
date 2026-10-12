package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderRegionCache {
   private final Long2ObjectMap<RenderRegionCache.ChunkInfo> chunkInfoCache = new Long2ObjectOpenHashMap();

   @Nullable
   public RenderChunkRegion createRegion(Level pLevel, SectionPos pSectionPos) {
      RenderRegionCache.ChunkInfo $$2 = this.getChunkInfo(pLevel, pSectionPos.x(), pSectionPos.z());
      if ($$2.chunk().isSectionEmpty(pSectionPos.y())) {
         return null;
      } else {
         int $$3 = pSectionPos.x() - 1;
         int $$4 = pSectionPos.z() - 1;
         int $$5 = pSectionPos.x() + 1;
         int $$6 = pSectionPos.z() + 1;
         RenderChunk[] $$7 = new RenderChunk[9];

         for (int $$8 = $$4; $$8 <= $$6; $$8++) {
            for (int $$9 = $$3; $$9 <= $$5; $$9++) {
               int $$10 = RenderChunkRegion.index($$3, $$4, $$9, $$8);
               RenderRegionCache.ChunkInfo $$11 = $$9 == pSectionPos.x() && $$8 == pSectionPos.z() ? $$2 : this.getChunkInfo(pLevel, $$9, $$8);
               $$7[$$10] = $$11.renderChunk();
            }
         }

         return new RenderChunkRegion(pLevel, $$3, $$4, $$7);
      }
   }

   private RenderRegionCache.ChunkInfo getChunkInfo(Level pLevel, int pX, int pZ) {
      return (RenderRegionCache.ChunkInfo)this.chunkInfoCache
         .computeIfAbsent(
            ChunkPos.asLong(pX, pZ), p_200464_ -> new RenderRegionCache.ChunkInfo(pLevel.getChunk(ChunkPos.getX(p_200464_), ChunkPos.getZ(p_200464_)))
         );
   }

   @OnlyIn(Dist.CLIENT)
   static final class ChunkInfo {
      private final LevelChunk chunk;
      @Nullable
      private RenderChunk renderChunk;

      ChunkInfo(LevelChunk pChunk) {
         this.chunk = pChunk;
      }

      public LevelChunk chunk() {
         return this.chunk;
      }

      public RenderChunk renderChunk() {
         if (this.renderChunk == null) {
            this.renderChunk = new RenderChunk(this.chunk);
         }

         return this.renderChunk;
      }
   }
}
