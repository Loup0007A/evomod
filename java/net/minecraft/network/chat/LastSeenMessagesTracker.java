package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.BitSet;
import java.util.Objects;
import javax.annotation.Nullable;

public class LastSeenMessagesTracker {
   private final LastSeenTrackedEntry[] trackedMessages;
   private int tail;
   private int offset;
   @Nullable
   private MessageSignature lastTrackedMessage;

   public LastSeenMessagesTracker(int pSize) {
      this.trackedMessages = new LastSeenTrackedEntry[pSize];
   }

   public boolean addPending(MessageSignature pSignature, boolean pAcknowledged) {
      if (Objects.equals(pSignature, this.lastTrackedMessage)) {
         return false;
      } else {
         this.lastTrackedMessage = pSignature;
         this.addEntry(pAcknowledged ? new LastSeenTrackedEntry(pSignature, true) : null);
         return true;
      }
   }

   private void addEntry(@Nullable LastSeenTrackedEntry pEntry) {
      int $$1 = this.tail;
      this.tail = ($$1 + 1) % this.trackedMessages.length;
      this.offset++;
      this.trackedMessages[$$1] = pEntry;
   }

   public void ignorePending(MessageSignature pSignature) {
      for (int $$1 = 0; $$1 < this.trackedMessages.length; $$1++) {
         LastSeenTrackedEntry $$2 = this.trackedMessages[$$1];
         if ($$2 != null && $$2.pending() && pSignature.equals($$2.signature())) {
            this.trackedMessages[$$1] = null;
            break;
         }
      }
   }

   public int getAndClearOffset() {
      int $$0 = this.offset;
      this.offset = 0;
      return $$0;
   }

   public LastSeenMessagesTracker.Update generateAndApplyUpdate() {
      int $$0 = this.getAndClearOffset();
      BitSet $$1 = new BitSet(this.trackedMessages.length);
      ObjectList<MessageSignature> $$2 = new ObjectArrayList(this.trackedMessages.length);

      for (int $$3 = 0; $$3 < this.trackedMessages.length; $$3++) {
         int $$4 = (this.tail + $$3) % this.trackedMessages.length;
         LastSeenTrackedEntry $$5 = this.trackedMessages[$$4];
         if ($$5 != null) {
            $$1.set($$3, true);
            $$2.add($$5.signature());
            this.trackedMessages[$$4] = $$5.acknowledge();
         }
      }

      LastSeenMessages $$6 = new LastSeenMessages($$2);
      LastSeenMessages.Update $$7 = new LastSeenMessages.Update($$0, $$1);
      return new LastSeenMessagesTracker.Update($$6, $$7);
   }

   public int offset() {
      return this.offset;
   }

   public static record Update(LastSeenMessages lastSeen, LastSeenMessages.Update update) {
   }
}
