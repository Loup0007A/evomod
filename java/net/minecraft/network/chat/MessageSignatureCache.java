package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public class MessageSignatureCache {
   public static final int NOT_FOUND = -1;
   private static final int DEFAULT_CAPACITY = 128;
   private final MessageSignature[] entries;

   public MessageSignatureCache(int pSize) {
      this.entries = new MessageSignature[pSize];
   }

   public static MessageSignatureCache createDefault() {
      return new MessageSignatureCache(128);
   }

   public int pack(MessageSignature pSignature) {
      for (int $$1 = 0; $$1 < this.entries.length; $$1++) {
         if (pSignature.equals(this.entries[$$1])) {
            return $$1;
         }
      }

      return -1;
   }

   @Nullable
   public MessageSignature unpack(int pIndex) {
      return this.entries[pIndex];
   }

   public void push(SignedMessageBody pSignedMessageBody, @Nullable MessageSignature pSignature) {
      List<MessageSignature> $$2 = pSignedMessageBody.lastSeen().entries();
      ArrayDeque<MessageSignature> $$3 = new ArrayDeque<>($$2.size() + 1);
      $$3.addAll($$2);
      if (pSignature != null) {
         $$3.add(pSignature);
      }

      this.push($$3);
   }

   @VisibleForTesting
   void push(List<MessageSignature> pChatMessages) {
      this.push(new ArrayDeque<>(pChatMessages));
   }

   private void push(ArrayDeque<MessageSignature> pDeque) {
      Set<MessageSignature> $$1 = new ObjectOpenHashSet(pDeque);

      for (int $$2 = 0; !pDeque.isEmpty() && $$2 < this.entries.length; $$2++) {
         MessageSignature $$3 = this.entries[$$2];
         this.entries[$$2] = pDeque.removeLast();
         if ($$3 != null && !$$1.contains($$3)) {
            pDeque.addFirst($$3);
         }
      }
   }
}
