package net.minecraft.client.multiplayer.prediction;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockStatePredictionHandler implements AutoCloseable {
   private final Long2ObjectOpenHashMap<BlockStatePredictionHandler.ServerVerifiedState> serverVerifiedStates = new Long2ObjectOpenHashMap();
   private int currentSequenceNr;
   private boolean isPredicting;

   public void retainKnownServerState(BlockPos pPos, BlockState pState, LocalPlayer pPlayer) {
      this.serverVerifiedStates
         .compute(
            pPos.asLong(),
            (p_340892_, p_340893_) -> p_340893_ != null
                  ? p_340893_.setSequence(this.currentSequenceNr)
                  : new BlockStatePredictionHandler.ServerVerifiedState(this.currentSequenceNr, pState, pPlayer.position())
         );
   }

   public boolean updateKnownServerState(BlockPos pPos, BlockState pState) {
      BlockStatePredictionHandler.ServerVerifiedState $$2 = (BlockStatePredictionHandler.ServerVerifiedState)this.serverVerifiedStates.get(pPos.asLong());
      if ($$2 == null) {
         return false;
      } else {
         $$2.setBlockState(pState);
         return true;
      }
   }

   public void endPredictionsUpTo(int pSequence, ClientLevel pLevel) {
      ObjectIterator<Entry<BlockStatePredictionHandler.ServerVerifiedState>> $$2 = this.serverVerifiedStates.long2ObjectEntrySet().iterator();

      while ($$2.hasNext()) {
         Entry<BlockStatePredictionHandler.ServerVerifiedState> $$3 = (Entry<BlockStatePredictionHandler.ServerVerifiedState>)$$2.next();
         BlockStatePredictionHandler.ServerVerifiedState $$4 = (BlockStatePredictionHandler.ServerVerifiedState)$$3.getValue();
         if ($$4.sequence <= pSequence) {
            BlockPos $$5 = BlockPos.of($$3.getLongKey());
            $$2.remove();
            pLevel.syncBlockState($$5, $$4.blockState, $$4.playerPos);
         }
      }
   }

   public BlockStatePredictionHandler startPredicting() {
      this.currentSequenceNr++;
      this.isPredicting = true;
      return this;
   }

   @Override
   public void close() {
      this.isPredicting = false;
   }

   public int currentSequence() {
      return this.currentSequenceNr;
   }

   public boolean isPredicting() {
      return this.isPredicting;
   }

   @OnlyIn(Dist.CLIENT)
   static class ServerVerifiedState {
      final Vec3 playerPos;
      int sequence;
      BlockState blockState;

      ServerVerifiedState(int pSequence, BlockState pBlockState, Vec3 pPlayerPos) {
         this.sequence = pSequence;
         this.blockState = pBlockState;
         this.playerPos = pPlayerPos;
      }

      BlockStatePredictionHandler.ServerVerifiedState setSequence(int pSequence) {
         this.sequence = pSequence;
         return this;
      }

      void setBlockState(BlockState pBlockState) {
         this.blockState = pBlockState;
      }
   }
}
