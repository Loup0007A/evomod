package net.minecraftforge.event;

import java.util.function.BooleanSupplier;
import net.minecraft.client.DeltaTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;

public class TickEvent extends Event {
   public final TickEvent.Type type;
   public final LogicalSide side;
   public final TickEvent.Phase phase;

   public TickEvent(TickEvent.Type type, LogicalSide side, TickEvent.Phase phase) {
      this.type = type;
      this.side = side;
      this.phase = phase;
   }

   public static class ClientTickEvent extends TickEvent {
      protected ClientTickEvent(TickEvent.Phase phase) {
         super(TickEvent.Type.CLIENT, LogicalSide.CLIENT, phase);
      }

      public static class Post extends TickEvent.ClientTickEvent {
         public Post() {
            super(TickEvent.Phase.END);
         }
      }

      public static class Pre extends TickEvent.ClientTickEvent {
         public Pre() {
            super(TickEvent.Phase.START);
         }
      }
   }

   public static class LevelTickEvent extends TickEvent {
      public final Level level;
      private final BooleanSupplier haveTime;

      protected LevelTickEvent(LogicalSide side, Level level, BooleanSupplier haveTime, TickEvent.Phase phase) {
         super(TickEvent.Type.LEVEL, side, phase);
         this.level = level;
         this.haveTime = haveTime;
      }

      public boolean haveTime() {
         return this.haveTime.getAsBoolean();
      }

      public static class Post extends TickEvent.LevelTickEvent {
         public Post(LogicalSide side, Level level, BooleanSupplier haveTime) {
            super(side, level, haveTime, TickEvent.Phase.END);
         }
      }

      public static class Pre extends TickEvent.LevelTickEvent {
         public Pre(LogicalSide side, Level level, BooleanSupplier haveTime) {
            super(side, level, haveTime, TickEvent.Phase.START);
         }
      }
   }

   public static enum Phase {
      START,
      END;
   }

   public static class PlayerTickEvent extends TickEvent {
      public final Player player;

      protected PlayerTickEvent(Player player, TickEvent.Phase phase) {
         super(TickEvent.Type.PLAYER, player instanceof ServerPlayer ? LogicalSide.SERVER : LogicalSide.CLIENT, phase);
         this.player = player;
      }

      public static class Post extends TickEvent.PlayerTickEvent {
         public Post(Player player) {
            super(player, TickEvent.Phase.END);
         }
      }

      public static class Pre extends TickEvent.PlayerTickEvent {
         public Pre(Player player) {
            super(player, TickEvent.Phase.START);
         }
      }
   }

   public static class RenderTickEvent extends TickEvent {
      private final DeltaTracker timer;

      private RenderTickEvent(TickEvent.Phase phase, DeltaTracker timer) {
         super(TickEvent.Type.RENDER, LogicalSide.CLIENT, phase);
         this.timer = timer;
      }

      public DeltaTracker getTimer() {
         return this.timer;
      }

      public static class Post extends TickEvent.RenderTickEvent {
         public Post(DeltaTracker timer) {
            super(TickEvent.Phase.END, timer);
         }
      }

      public static class Pre extends TickEvent.RenderTickEvent {
         public Pre(DeltaTracker timer) {
            super(TickEvent.Phase.START, timer);
         }
      }
   }

   public static class ServerTickEvent extends TickEvent {
      private final BooleanSupplier haveTime;
      private final MinecraftServer server;

      protected ServerTickEvent(BooleanSupplier haveTime, MinecraftServer server, TickEvent.Phase phase) {
         super(TickEvent.Type.SERVER, LogicalSide.SERVER, phase);
         this.haveTime = haveTime;
         this.server = server;
      }

      public boolean haveTime() {
         return this.haveTime.getAsBoolean();
      }

      public MinecraftServer getServer() {
         return this.server;
      }

      public static class Post extends TickEvent.ServerTickEvent {
         public Post(BooleanSupplier haveTime, MinecraftServer server) {
            super(haveTime, server, TickEvent.Phase.END);
         }
      }

      public static class Pre extends TickEvent.ServerTickEvent {
         public Pre(BooleanSupplier haveTime, MinecraftServer server) {
            super(haveTime, server, TickEvent.Phase.START);
         }
      }
   }

   public static enum Type {
      LEVEL,
      PLAYER,
      CLIENT,
      SERVER,
      RENDER;
   }
}
