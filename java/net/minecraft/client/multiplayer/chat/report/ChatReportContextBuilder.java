package net.minecraft.client.multiplayer.chat.report;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatEvent;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatReportContextBuilder {
   final int leadingCount;
   private final List<ChatReportContextBuilder.Collector> activeCollectors = new ArrayList<>();

   public ChatReportContextBuilder(int pLeadingCount) {
      this.leadingCount = pLeadingCount;
   }

   public void collectAllContext(ChatLog pChatLog, IntCollection pReportedMessages, ChatReportContextBuilder.Handler pHandler) {
      IntSortedSet $$3 = new IntRBTreeSet(pReportedMessages);

      for (int $$4 = $$3.lastInt(); $$4 >= pChatLog.start() && (this.isActive() || !$$3.isEmpty()); $$4--) {
         LoggedChatEvent $$6 = pChatLog.lookup($$4);
         if ($$6 instanceof LoggedChatMessage.Player) {
            LoggedChatMessage.Player $$5 = (LoggedChatMessage.Player)$$6;
            boolean $$6x = this.acceptContext($$5.message());
            if ($$3.remove($$4)) {
               this.trackContext($$5.message());
               pHandler.accept($$4, $$5);
            } else if ($$6x) {
               pHandler.accept($$4, $$5);
            }
         }
      }
   }

   public void trackContext(PlayerChatMessage pLastChainMessage) {
      this.activeCollectors.add(new ChatReportContextBuilder.Collector(pLastChainMessage));
   }

   public boolean acceptContext(PlayerChatMessage pLastChainMessage) {
      boolean $$1 = false;
      Iterator<ChatReportContextBuilder.Collector> $$2 = this.activeCollectors.iterator();

      while ($$2.hasNext()) {
         ChatReportContextBuilder.Collector $$3 = $$2.next();
         if ($$3.accept(pLastChainMessage)) {
            $$1 = true;
            if ($$3.isComplete()) {
               $$2.remove();
            }
         }
      }

      return $$1;
   }

   public boolean isActive() {
      return !this.activeCollectors.isEmpty();
   }

   @OnlyIn(Dist.CLIENT)
   class Collector {
      private final Set<MessageSignature> lastSeenSignatures;
      private PlayerChatMessage lastChainMessage;
      private boolean collectingChain = true;
      private int count;

      Collector(final PlayerChatMessage pLastChainMessage) {
         this.lastSeenSignatures = new ObjectOpenHashSet(pLastChainMessage.signedBody().lastSeen().entries());
         this.lastChainMessage = pLastChainMessage;
      }

      boolean accept(PlayerChatMessage pMessage) {
         if (pMessage.equals(this.lastChainMessage)) {
            return false;
         } else {
            boolean $$1 = this.lastSeenSignatures.remove(pMessage.signature());
            if (this.collectingChain && this.lastChainMessage.sender().equals(pMessage.sender())) {
               if (this.lastChainMessage.link().isDescendantOf(pMessage.link())) {
                  $$1 = true;
                  this.lastChainMessage = pMessage;
               } else {
                  this.collectingChain = false;
               }
            }

            if ($$1) {
               this.count++;
            }

            return $$1;
         }
      }

      boolean isComplete() {
         return this.count >= ChatReportContextBuilder.this.leadingCount || !this.collectingChain && this.lastSeenSignatures.isEmpty();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Handler {
      void accept(int pIndex, LoggedChatMessage.Player pPlayer);
   }
}
