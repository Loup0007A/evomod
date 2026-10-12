package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public class GameTestSequence {
   final GameTestInfo parent;
   private final List<GameTestEvent> events = Lists.newArrayList();
   private long lastTick;

   GameTestSequence(GameTestInfo pTestInfo) {
      this.parent = pTestInfo;
      this.lastTick = pTestInfo.getTick();
   }

   public GameTestSequence thenWaitUntil(Runnable pTask) {
      this.events.add(GameTestEvent.create(pTask));
      return this;
   }

   public GameTestSequence thenWaitUntil(long pExpectedDelay, Runnable pTask) {
      this.events.add(GameTestEvent.create(pExpectedDelay, pTask));
      return this;
   }

   public GameTestSequence thenIdle(int pTick) {
      return this.thenExecuteAfter(pTick, () -> {
      });
   }

   public GameTestSequence thenExecute(Runnable pTask) {
      this.events.add(GameTestEvent.create(() -> this.executeWithoutFail(pTask)));
      return this;
   }

   public GameTestSequence thenExecuteAfter(int pTick, Runnable pTask) {
      this.events.add(GameTestEvent.create(() -> {
         if (this.parent.getTick() < this.lastTick + (long)pTick) {
            throw new GameTestAssertException("Test timed out before sequence completed");
         } else {
            this.executeWithoutFail(pTask);
         }
      }));
      return this;
   }

   public GameTestSequence thenExecuteFor(int pTick, Runnable pTask) {
      this.events.add(GameTestEvent.create(() -> {
         if (this.parent.getTick() < this.lastTick + (long)pTick) {
            this.executeWithoutFail(pTask);
            throw new GameTestAssertException("Test timed out before sequence completed");
         }
      }));
      return this;
   }

   public void thenSucceed() {
      this.events.add(GameTestEvent.create(this.parent::succeed));
   }

   public void thenFail(Supplier<Exception> pException) {
      this.events.add(GameTestEvent.create(() -> this.parent.fail(pException.get())));
   }

   public GameTestSequence.Condition thenTrigger() {
      GameTestSequence.Condition $$0 = new GameTestSequence.Condition();
      this.events.add(GameTestEvent.create(() -> $$0.trigger(this.parent.getTick())));
      return $$0;
   }

   public void tickAndContinue(long pTick) {
      try {
         this.tick(pTick);
      } catch (GameTestAssertException var4) {
      }
   }

   public void tickAndFailIfNotComplete(long pTicks) {
      try {
         this.tick(pTicks);
      } catch (GameTestAssertException var4) {
         this.parent.fail(var4);
      }
   }

   private void executeWithoutFail(Runnable pTask) {
      try {
         pTask.run();
      } catch (GameTestAssertException var3) {
         this.parent.fail(var3);
      }
   }

   private void tick(long pTick) {
      Iterator<GameTestEvent> $$1 = this.events.iterator();

      while ($$1.hasNext()) {
         GameTestEvent $$2 = $$1.next();
         $$2.assertion.run();
         $$1.remove();
         long $$3 = pTick - this.lastTick;
         long $$4 = this.lastTick;
         this.lastTick = pTick;
         if ($$2.expectedDelay != null && $$2.expectedDelay != $$3) {
            this.parent
               .fail(new GameTestAssertException("Succeeded in invalid tick: expected " + ($$4 + $$2.expectedDelay) + ", but current tick is " + pTick));
            break;
         }
      }
   }

   public class Condition {
      private static final long NOT_TRIGGERED = -1L;
      private long triggerTime = -1L;

      void trigger(long pTriggerTime) {
         if (this.triggerTime != -1L) {
            throw new IllegalStateException("Condition already triggered at " + this.triggerTime);
         } else {
            this.triggerTime = pTriggerTime;
         }
      }

      public void assertTriggeredThisTick() {
         long $$0 = GameTestSequence.this.parent.getTick();
         if (this.triggerTime != $$0) {
            if (this.triggerTime == -1L) {
               throw new GameTestAssertException("Condition not triggered (t=" + $$0 + ")");
            } else {
               throw new GameTestAssertException("Condition triggered at " + this.triggerTime + ", (t=" + $$0 + ")");
            }
         }
      }
   }
}
