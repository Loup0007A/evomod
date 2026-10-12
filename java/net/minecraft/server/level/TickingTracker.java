package net.minecraft.server.level;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.ChunkPos;

public class TickingTracker extends ChunkTracker {
   public static final int MAX_LEVEL = 33;
   private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
   protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
   private final Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets = new Long2ObjectOpenHashMap();

   public TickingTracker() {
      super(34, 16, 256);
      this.chunks.defaultReturnValue((byte)33);
   }

   private SortedArraySet<Ticket<?>> getTickets(long pChunkPos) {
      return (SortedArraySet<Ticket<?>>)this.tickets.computeIfAbsent(pChunkPos, p_184180_ -> SortedArraySet.create(4));
   }

   private int getTicketLevelAt(SortedArraySet<Ticket<?>> pTickets) {
      return pTickets.isEmpty() ? 34 : pTickets.first().getTicketLevel();
   }

   public void addTicket(long pChunkPos, Ticket<?> pTicket) {
      SortedArraySet<Ticket<?>> $$2 = this.getTickets(pChunkPos);
      int $$3 = this.getTicketLevelAt($$2);
      $$2.add(pTicket);
      if (pTicket.getTicketLevel() < $$3) {
         this.update(pChunkPos, pTicket.getTicketLevel(), true);
      }
   }

   public void removeTicket(long pChunkPos, Ticket<?> pTicket) {
      SortedArraySet<Ticket<?>> $$2 = this.getTickets(pChunkPos);
      $$2.remove(pTicket);
      if ($$2.isEmpty()) {
         this.tickets.remove(pChunkPos);
      }

      this.update(pChunkPos, this.getTicketLevelAt($$2), false);
   }

   public <T> void addTicket(TicketType<T> pType, ChunkPos pChunkPos, int pTicketLevel, T pKey) {
      this.addTicket(pChunkPos.toLong(), new Ticket<>(pType, pTicketLevel, pKey));
   }

   public <T> void removeTicket(TicketType<T> pType, ChunkPos pChunkPos, int pTicketLevel, T pKey) {
      Ticket<T> $$4 = new Ticket<>(pType, pTicketLevel, pKey);
      this.removeTicket(pChunkPos.toLong(), $$4);
   }

   public void replacePlayerTicketsLevel(int pTicketLevel) {
      List<Pair<Ticket<ChunkPos>, Long>> $$1 = new ArrayList<>();
      ObjectIterator var3 = this.tickets.long2ObjectEntrySet().iterator();

      while (var3.hasNext()) {
         Entry<SortedArraySet<Ticket<?>>> $$2 = (Entry<SortedArraySet<Ticket<?>>>)var3.next();

         for (Ticket<?> $$3 : (SortedArraySet)$$2.getValue()) {
            if ($$3.getType() == TicketType.PLAYER) {
               $$1.add(Pair.of($$3, $$2.getLongKey()));
            }
         }
      }

      for (Pair<Ticket<ChunkPos>, Long> $$4 : $$1) {
         Long $$5 = (Long)$$4.getSecond();
         Ticket<ChunkPos> $$6 = (Ticket<ChunkPos>)$$4.getFirst();
         this.removeTicket($$5, $$6);
         ChunkPos $$7 = new ChunkPos($$5);
         TicketType<ChunkPos> $$8 = $$6.getType();
         this.addTicket($$8, $$7, pTicketLevel, $$7);
      }
   }

   @Override
   protected int getLevelFromSource(long pPos) {
      SortedArraySet<Ticket<?>> $$1 = (SortedArraySet<Ticket<?>>)this.tickets.get(pPos);
      return $$1 != null && !$$1.isEmpty() ? $$1.first().getTicketLevel() : Integer.MAX_VALUE;
   }

   public int getLevel(ChunkPos pChunkPos) {
      return this.getLevel(pChunkPos.toLong());
   }

   @Override
   protected int getLevel(long pChunkPos) {
      return this.chunks.get(pChunkPos);
   }

   @Override
   protected void setLevel(long pChunkPos, int pLevel) {
      if (pLevel >= 33) {
         this.chunks.remove(pChunkPos);
      } else {
         this.chunks.put(pChunkPos, (byte)pLevel);
      }
   }

   public void runAllUpdates() {
      this.runUpdates(Integer.MAX_VALUE);
   }

   public String getTicketDebugString(long pChunkPos) {
      SortedArraySet<Ticket<?>> $$1 = (SortedArraySet<Ticket<?>>)this.tickets.get(pChunkPos);
      return $$1 != null && !$$1.isEmpty() ? $$1.first().toString() : "no_ticket";
   }
}
