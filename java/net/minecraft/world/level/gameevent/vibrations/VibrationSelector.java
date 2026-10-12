package net.minecraft.world.level.gameevent.vibrations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;

public class VibrationSelector {
   public static final Codec<VibrationSelector> CODEC = RecordCodecBuilder.create(
      p_327443_ -> p_327443_.group(
               VibrationInfo.CODEC.lenientOptionalFieldOf("event").forGetter(p_251862_ -> p_251862_.currentVibrationData.map(Pair::getLeft)),
               Codec.LONG.fieldOf("tick").forGetter(p_251458_ -> p_251458_.currentVibrationData.<Long>map(Pair::getRight).orElse(-1L))
            )
            .apply(p_327443_, VibrationSelector::new)
   );
   private Optional<Pair<VibrationInfo, Long>> currentVibrationData;

   public VibrationSelector(Optional<VibrationInfo> p_251736_, long p_251649_) {
      this.currentVibrationData = p_251736_.map(p_251571_ -> Pair.of(p_251571_, p_251649_));
   }

   public VibrationSelector() {
      this.currentVibrationData = Optional.empty();
   }

   public void addCandidate(VibrationInfo pVibrationInfo, long pTick) {
      if (this.shouldReplaceVibration(pVibrationInfo, pTick)) {
         this.currentVibrationData = Optional.of(Pair.of(pVibrationInfo, pTick));
      }
   }

   private boolean shouldReplaceVibration(VibrationInfo pVibrationInfo, long pTick) {
      if (this.currentVibrationData.isEmpty()) {
         return true;
      } else {
         Pair<VibrationInfo, Long> $$2 = this.currentVibrationData.get();
         long $$3 = (Long)$$2.getRight();
         if (pTick != $$3) {
            return false;
         } else {
            VibrationInfo $$4 = (VibrationInfo)$$2.getLeft();
            if (pVibrationInfo.distance() < $$4.distance()) {
               return true;
            } else {
               return pVibrationInfo.distance() > $$4.distance()
                  ? false
                  : VibrationSystem.getGameEventFrequency(pVibrationInfo.gameEvent()) > VibrationSystem.getGameEventFrequency($$4.gameEvent());
            }
         }
      }
   }

   public Optional<VibrationInfo> chosenCandidate(long pTick) {
      if (this.currentVibrationData.isEmpty()) {
         return Optional.empty();
      } else {
         return this.currentVibrationData.get().getRight() < pTick ? Optional.of((VibrationInfo)this.currentVibrationData.get().getLeft()) : Optional.empty();
      }
   }

   public void startOver() {
      this.currentVibrationData = Optional.empty();
   }
}
