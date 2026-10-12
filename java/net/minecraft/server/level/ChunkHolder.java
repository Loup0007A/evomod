package net.minecraft.server.level;

import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class ChunkHolder extends GenerationChunkHolder {
   public static final ChunkResult<LevelChunk> UNLOADED_LEVEL_CHUNK = ChunkResult.error("Unloaded level chunk");
   private static final CompletableFuture<ChunkResult<LevelChunk>> UNLOADED_LEVEL_CHUNK_FUTURE = CompletableFuture.completedFuture(UNLOADED_LEVEL_CHUNK);
   private final LevelHeightAccessor levelHeightAccessor;
   private volatile CompletableFuture<ChunkResult<LevelChunk>> fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
   private volatile CompletableFuture<ChunkResult<LevelChunk>> tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
   private volatile CompletableFuture<ChunkResult<LevelChunk>> entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
   private int oldTicketLevel;
   private int ticketLevel;
   private int queueLevel;
   private boolean hasChangedSections;
   private final ShortSet[] changedBlocksPerSection;
   private final BitSet blockChangedLightSectionFilter = new BitSet();
   private final BitSet skyChangedLightSectionFilter = new BitSet();
   private final LevelLightEngine lightEngine;
   private final ChunkHolder.LevelChangeListener onLevelChange;
   private final ChunkHolder.PlayerProvider playerProvider;
   private boolean wasAccessibleSinceLastSave;
   private CompletableFuture<?> pendingFullStateConfirmation = CompletableFuture.completedFuture(null);
   private CompletableFuture<?> sendSync = CompletableFuture.completedFuture(null);
   private CompletableFuture<?> saveSync = CompletableFuture.completedFuture(null);

   public ChunkHolder(
      ChunkPos pPos,
      int pTicketLevel,
      LevelHeightAccessor pLevelHeightAccessor,
      LevelLightEngine pLightEngine,
      ChunkHolder.LevelChangeListener pOnLevelChange,
      ChunkHolder.PlayerProvider pPlayerProvider
   ) {
      super(pPos);
      this.levelHeightAccessor = pLevelHeightAccessor;
      this.lightEngine = pLightEngine;
      this.onLevelChange = pOnLevelChange;
      this.playerProvider = pPlayerProvider;
      this.oldTicketLevel = ChunkLevel.MAX_LEVEL + 1;
      this.ticketLevel = this.oldTicketLevel;
      this.queueLevel = this.oldTicketLevel;
      this.setTicketLevel(pTicketLevel);
      this.changedBlocksPerSection = new ShortSet[pLevelHeightAccessor.getSectionsCount()];
   }

   public CompletableFuture<ChunkResult<LevelChunk>> getTickingChunkFuture() {
      return this.tickingChunkFuture;
   }

   public CompletableFuture<ChunkResult<LevelChunk>> getEntityTickingChunkFuture() {
      return this.entityTickingChunkFuture;
   }

   public CompletableFuture<ChunkResult<LevelChunk>> getFullChunkFuture() {
      return this.fullChunkFuture;
   }

   @Nullable
   public LevelChunk getTickingChunk() {
      return this.getTickingChunkFuture().getNow(UNLOADED_LEVEL_CHUNK).orElse(null);
   }

   @Nullable
   public LevelChunk getChunkToSend() {
      return !this.sendSync.isDone() ? null : this.getTickingChunk();
   }

   public CompletableFuture<?> getSendSyncFuture() {
      return this.sendSync;
   }

   public void addSendDependency(CompletableFuture<?> pDependency) {
      if (this.sendSync.isDone()) {
         this.sendSync = pDependency;
      } else {
         this.sendSync = this.sendSync.thenCombine((CompletionStage<? extends Object>)pDependency, (p_341205_, p_341206_) -> null);
      }
   }

   public CompletableFuture<?> getSaveSyncFuture() {
      return this.saveSync;
   }

   public boolean isReadyForSaving() {
      return this.getGenerationRefCount() == 0 && this.saveSync.isDone();
   }

   private void addSaveDependency(CompletableFuture<?> pDependency) {
      if (this.saveSync.isDone()) {
         this.saveSync = pDependency;
      } else {
         this.saveSync = this.saveSync.thenCombine((CompletionStage<? extends Object>)pDependency, (p_296561_, p_296562_) -> null);
      }
   }

   public void blockChanged(BlockPos pPos) {
      LevelChunk $$1 = this.getTickingChunk();
      if ($$1 != null) {
         int $$2 = this.levelHeightAccessor.getSectionIndex(pPos.getY());
         if (this.changedBlocksPerSection[$$2] == null) {
            this.hasChangedSections = true;
            this.changedBlocksPerSection[$$2] = new ShortOpenHashSet();
         }

         this.changedBlocksPerSection[$$2].add(SectionPos.sectionRelativePos(pPos));
      }
   }

   public void sectionLightChanged(LightLayer pType, int pSectionY) {
      ChunkAccess $$2 = this.getChunkIfPresent(ChunkStatus.INITIALIZE_LIGHT);
      if ($$2 != null) {
         $$2.setUnsaved(true);
         LevelChunk $$3 = this.getTickingChunk();
         if ($$3 != null) {
            int $$4 = this.lightEngine.getMinLightSection();
            int $$5 = this.lightEngine.getMaxLightSection();
            if (pSectionY >= $$4 && pSectionY <= $$5) {
               int $$6 = pSectionY - $$4;
               if (pType == LightLayer.SKY) {
                  this.skyChangedLightSectionFilter.set($$6);
               } else {
                  this.blockChangedLightSectionFilter.set($$6);
               }
            }
         }
      }
   }

   public void broadcastChanges(LevelChunk pChunk) {
      if (this.hasChangedSections || !this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
         Level $$1 = pChunk.getLevel();
         if (!this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
            List<ServerPlayer> $$2 = this.playerProvider.getPlayers(this.pos, true);
            if (!$$2.isEmpty()) {
               ClientboundLightUpdatePacket $$3 = new ClientboundLightUpdatePacket(
                  pChunk.getPos(), this.lightEngine, this.skyChangedLightSectionFilter, this.blockChangedLightSectionFilter
               );
               this.broadcast($$2, $$3);
            }

            this.skyChangedLightSectionFilter.clear();
            this.blockChangedLightSectionFilter.clear();
         }

         if (this.hasChangedSections) {
            List<ServerPlayer> $$4 = this.playerProvider.getPlayers(this.pos, false);

            for (int $$5 = 0; $$5 < this.changedBlocksPerSection.length; $$5++) {
               ShortSet $$6 = this.changedBlocksPerSection[$$5];
               if ($$6 != null) {
                  this.changedBlocksPerSection[$$5] = null;
                  if (!$$4.isEmpty()) {
                     int $$7 = this.levelHeightAccessor.getSectionYFromSectionIndex($$5);
                     SectionPos $$8 = SectionPos.of(pChunk.getPos(), $$7);
                     if ($$6.size() == 1) {
                        BlockPos $$9 = $$8.relativeToBlockPos($$6.iterator().nextShort());
                        BlockState $$10 = $$1.getBlockState($$9);
                        this.broadcast($$4, new ClientboundBlockUpdatePacket($$9, $$10));
                        this.broadcastBlockEntityIfNeeded($$4, $$1, $$9, $$10);
                     } else {
                        LevelChunkSection $$11 = pChunk.getSection($$5);
                        ClientboundSectionBlocksUpdatePacket $$12 = new ClientboundSectionBlocksUpdatePacket($$8, $$6, $$11);
                        this.broadcast($$4, $$12);
                        $$12.runUpdates((p_288761_, p_288762_) -> this.broadcastBlockEntityIfNeeded($$4, $$1, p_288761_, p_288762_));
                     }
                  }
               }
            }

            this.hasChangedSections = false;
         }
      }
   }

   private void broadcastBlockEntityIfNeeded(List<ServerPlayer> pPlayers, Level pLevel, BlockPos pPos, BlockState pState) {
      if (pState.hasBlockEntity()) {
         this.broadcastBlockEntity(pPlayers, pLevel, pPos);
      }
   }

   private void broadcastBlockEntity(List<ServerPlayer> pPlayers, Level pLevel, BlockPos pPos) {
      BlockEntity $$3 = pLevel.getBlockEntity(pPos);
      if ($$3 != null) {
         Packet<?> $$4 = $$3.getUpdatePacket();
         if ($$4 != null) {
            this.broadcast(pPlayers, $$4);
         }
      }
   }

   private void broadcast(List<ServerPlayer> pPlayers, Packet<?> pPacket) {
      pPlayers.forEach(p_296560_ -> p_296560_.connection.send(pPacket));
   }

   @Override
   public int getTicketLevel() {
      return this.ticketLevel;
   }

   @Override
   public int getQueueLevel() {
      return this.queueLevel;
   }

   private void setQueueLevel(int p_140087_) {
      this.queueLevel = p_140087_;
   }

   public void setTicketLevel(int pLevel) {
      this.ticketLevel = pLevel;
   }

   private void scheduleFullChunkPromotion(
      ChunkMap pChunkMap, CompletableFuture<ChunkResult<LevelChunk>> pFuture, Executor pExecutor, FullChunkStatus pFullChunkStatus
   ) {
      this.pendingFullStateConfirmation.cancel(false);
      CompletableFuture<Void> $$4 = new CompletableFuture<>();
      $$4.thenRunAsync(() -> pChunkMap.onFullChunkStatusChange(this.pos, pFullChunkStatus), pExecutor);
      this.pendingFullStateConfirmation = $$4;
      pFuture.thenAccept(p_326372_ -> p_326372_.ifSuccess(p_200424_ -> $$4.complete(null)));
   }

   private void demoteFullChunk(ChunkMap pChunkMap, FullChunkStatus pFullChunkStatus) {
      this.pendingFullStateConfirmation.cancel(false);
      pChunkMap.onFullChunkStatusChange(this.pos, pFullChunkStatus);
   }

   protected void updateFutures(ChunkMap pChunkMap, Executor pExecutor) {
      FullChunkStatus $$2 = ChunkLevel.fullStatus(this.oldTicketLevel);
      FullChunkStatus $$3 = ChunkLevel.fullStatus(this.ticketLevel);
      boolean $$4 = $$2.isOrAfter(FullChunkStatus.FULL);
      boolean $$5 = $$3.isOrAfter(FullChunkStatus.FULL);
      this.wasAccessibleSinceLastSave |= $$5;
      if (!$$4 && $$5) {
         this.fullChunkFuture = pChunkMap.prepareAccessibleChunk(this);
         this.scheduleFullChunkPromotion(pChunkMap, this.fullChunkFuture, pExecutor, FullChunkStatus.FULL);
         this.addSaveDependency(this.fullChunkFuture);
      }

      if ($$4 && !$$5) {
         this.fullChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
         this.fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
      }

      boolean $$6 = $$2.isOrAfter(FullChunkStatus.BLOCK_TICKING);
      boolean $$7 = $$3.isOrAfter(FullChunkStatus.BLOCK_TICKING);
      if (!$$6 && $$7) {
         this.tickingChunkFuture = pChunkMap.prepareTickingChunk(this);
         this.scheduleFullChunkPromotion(pChunkMap, this.tickingChunkFuture, pExecutor, FullChunkStatus.BLOCK_TICKING);
         this.addSaveDependency(this.tickingChunkFuture);
      }

      if ($$6 && !$$7) {
         this.tickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
         this.tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
      }

      boolean $$8 = $$2.isOrAfter(FullChunkStatus.ENTITY_TICKING);
      boolean $$9 = $$3.isOrAfter(FullChunkStatus.ENTITY_TICKING);
      if (!$$8 && $$9) {
         if (this.entityTickingChunkFuture != UNLOADED_LEVEL_CHUNK_FUTURE) {
            throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
         }

         this.entityTickingChunkFuture = pChunkMap.prepareEntityTickingChunk(this);
         this.scheduleFullChunkPromotion(pChunkMap, this.entityTickingChunkFuture, pExecutor, FullChunkStatus.ENTITY_TICKING);
         this.addSaveDependency(this.entityTickingChunkFuture);
      }

      if ($$8 && !$$9) {
         this.entityTickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
         this.entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
      }

      if (!$$3.isOrAfter($$2)) {
         this.demoteFullChunk(pChunkMap, $$3);
      }

      this.onLevelChange.onLevelChange(this.pos, this::getQueueLevel, this.ticketLevel, this::setQueueLevel);
      this.oldTicketLevel = this.ticketLevel;
   }

   public boolean wasAccessibleSinceLastSave() {
      return this.wasAccessibleSinceLastSave;
   }

   public void refreshAccessibility() {
      this.wasAccessibleSinceLastSave = ChunkLevel.fullStatus(this.ticketLevel).isOrAfter(FullChunkStatus.FULL);
   }

   @FunctionalInterface
   public interface LevelChangeListener {
      void onLevelChange(ChunkPos pChunkPos, IntSupplier pQueueLevelGetter, int pTicketLevel, IntConsumer pQueueLevelSetter);
   }

   public interface PlayerProvider {
      List<ServerPlayer> getPlayers(ChunkPos pPos, boolean pBoundaryOnly);
   }
}
