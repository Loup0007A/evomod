package net.minecraftforge.client.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public abstract class ClientPauseChangeEvent extends Event {
   private final boolean pause;

   public ClientPauseChangeEvent(boolean pause) {
      this.pause = pause;
   }

   public boolean isPaused() {
      return this.pause;
   }

   public static class Post extends ClientPauseChangeEvent {
      public Post(boolean pause) {
         super(pause);
      }
   }

   @Cancelable
   public static class Pre extends ClientPauseChangeEvent {
      public Pre(boolean pause) {
         super(pause);
      }
   }
}
