package net.minecraft.gametest.framework;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public class GameTestInfo {
   private final TestFunction testFunction;
   @Nullable
   private BlockPos structureBlockPos;
   @Nullable
   private BlockPos northWestCorner;
   private final ServerLevel level;
   private final Collection<GameTestListener> listeners = Lists.newArrayList();
   private final int timeoutTicks;
   private final Collection<GameTestSequence> sequences = Lists.newCopyOnWriteArrayList();
   private final Object2LongMap<Runnable> runAtTickTimeMap = new Object2LongOpenHashMap();
   private long startTick;
   private int ticksToWaitForChunkLoading = 20;
   private boolean placedStructure;
   private boolean chunksLoaded;
   private long tickCount;
   private boolean started;
   private final RetryOptions retryOptions;
   private final Stopwatch timer = Stopwatch.createUnstarted();
   private boolean done;
   private final Rotation rotation;
   @Nullable
   private Throwable error;
   @Nullable
   private StructureBlockEntity structureBlockEntity;

   public GameTestInfo(TestFunction pTestFunction, Rotation pRotation, ServerLevel pLevel, RetryOptions pRetryOptions) {
      this.testFunction = pTestFunction;
      this.level = pLevel;
      this.retryOptions = pRetryOptions;
      this.timeoutTicks = pTestFunction.maxTicks();
      this.rotation = pTestFunction.rotation().getRotated(pRotation);
   }

   void setStructureBlockPos(BlockPos pPos) {
      this.structureBlockPos = pPos;
   }

   public GameTestInfo startExecution(int pDelay) {
      this.startTick = this.level.getGameTime() + this.testFunction.setupTicks() + (long)pDelay;
      this.timer.start();
      return this;
   }

   public GameTestInfo placeStructure() {
      if (this.placedStructure) {
         return this;
      } else {
         this.ticksToWaitForChunkLoading = 0;
         this.placedStructure = true;
         StructureBlockEntity $$0 = this.getStructureBlockEntity();
         $$0.placeStructure(this.level);
         BoundingBox $$1 = StructureUtils.getStructureBoundingBox($$0);
         this.level.getBlockTicks().clearArea($$1);
         this.level.clearBlockEvents($$1);
         return this;
      }
   }

   private boolean ensureStructureIsPlaced() {
      if (this.placedStructure) {
         return true;
      } else if (this.ticksToWaitForChunkLoading > 0) {
         this.ticksToWaitForChunkLoading--;
         return false;
      } else {
         this.placeStructure().startExecution(0);
         return true;
      }
   }

   public void tick(GameTestRunner pRunner) {
      if (!this.isDone()) {
         if (this.structureBlockEntity == null) {
            this.fail(new IllegalStateException("Running test without structure block entity"));
         }

         if (this.chunksLoaded
            || StructureUtils.getStructureBoundingBox(this.structureBlockEntity)
               .intersectingChunks()
               .allMatch(p_308533_ -> this.level.isPositionEntityTicking(p_308533_.getWorldPosition()))) {
            this.chunksLoaded = true;
            if (this.ensureStructureIsPlaced()) {
               this.tickInternal();
               if (this.isDone()) {
                  if (this.error != null) {
                     this.listeners.forEach(p_325940_ -> p_325940_.testFailed(this, pRunner));
                  } else {
                     this.listeners.forEach(p_325938_ -> p_325938_.testPassed(this, pRunner));
                  }
               }
            }
         }
      }
   }

   private void tickInternal() {
      this.tickCount = this.level.getGameTime() - this.startTick;
      if (this.tickCount >= 0L) {
         if (!this.started) {
            this.startTest();
         }

         ObjectIterator<Entry<Runnable>> $$0 = this.runAtTickTimeMap.object2LongEntrySet().iterator();

         while ($$0.hasNext()) {
            Entry<Runnable> $$1 = (Entry<Runnable>)$$0.next();
            if ($$1.getLongValue() <= this.tickCount) {
               try {
                  ((Runnable)$$1.getKey()).run();
               } catch (Exception var4) {
                  this.fail(var4);
               }

               $$0.remove();
            }
         }

         if (this.tickCount > (long)this.timeoutTicks) {
            if (this.sequences.isEmpty()) {
               this.fail(new GameTestTimeoutException("Didn't succeed or fail within " + this.testFunction.maxTicks() + " ticks"));
            } else {
               this.sequences.forEach(p_177478_ -> p_177478_.tickAndFailIfNotComplete(this.tickCount));
               if (this.error == null) {
                  this.fail(new GameTestTimeoutException("No sequences finished"));
               }
            }
         } else {
            this.sequences.forEach(p_177476_ -> p_177476_.tickAndContinue(this.tickCount));
         }
      }
   }

   private void startTest() {
      if (!this.started) {
         this.started = true;

         try {
            this.testFunction.run(new GameTestHelper(this));
         } catch (Exception var2) {
            this.fail(var2);
         }
      }
   }

   public void setRunAtTickTime(long pTickTime, Runnable pTask) {
      this.runAtTickTimeMap.put(pTask, pTickTime);
   }

   public String getTestName() {
      return this.testFunction.testName();
   }

   @Nullable
   public BlockPos getStructureBlockPos() {
      return this.structureBlockPos;
   }

   public AABB getStructureBounds() {
      StructureBlockEntity $$0 = this.getStructureBlockEntity();
      return StructureUtils.getStructureBounds($$0);
   }

   public StructureBlockEntity getStructureBlockEntity() {
      if (this.structureBlockEntity == null) {
         if (this.structureBlockPos == null) {
            throw new IllegalStateException("Could not find a structureBlockEntity for this GameTestInfo");
         }

         this.structureBlockEntity = (StructureBlockEntity)this.level.getBlockEntity(this.structureBlockPos);
         if (this.structureBlockEntity == null) {
            throw new IllegalStateException("Could not find a structureBlockEntity at the given coordinate " + this.structureBlockPos);
         }
      }

      return this.structureBlockEntity;
   }

   public ServerLevel getLevel() {
      return this.level;
   }

   public boolean hasSucceeded() {
      return this.done && this.error == null;
   }

   public boolean hasFailed() {
      return this.error != null;
   }

   public boolean hasStarted() {
      return this.started;
   }

   public boolean isDone() {
      return this.done;
   }

   public long getRunTime() {
      return this.timer.elapsed(TimeUnit.MILLISECONDS);
   }

   private void finish() {
      if (!this.done) {
         this.done = true;
         if (this.timer.isRunning()) {
            this.timer.stop();
         }
      }
   }

   public void succeed() {
      if (this.error == null) {
         this.finish();
         AABB $$0 = this.getStructureBounds();
         List<Entity> $$1 = this.getLevel().getEntitiesOfClass(Entity.class, $$0.inflate(1.0), p_308532_ -> !(p_308532_ instanceof Player));
         $$1.forEach(p_308534_ -> p_308534_.remove(Entity.RemovalReason.DISCARDED));
      }
   }

   public void fail(Throwable pError) {
      this.error = pError;
      this.finish();
   }

   @Nullable
   public Throwable getError() {
      return this.error;
   }

   @Override
   public String toString() {
      return this.getTestName();
   }

   public void addListener(GameTestListener pListener) {
      this.listeners.add(pListener);
   }

   public GameTestInfo prepareTestStructure() {
      BlockPos $$0 = this.getOrCalculateNorthwestCorner();
      this.structureBlockEntity = StructureUtils.prepareTestStructure(this, $$0, this.getRotation(), this.level);
      this.structureBlockPos = this.structureBlockEntity.getBlockPos();
      StructureUtils.addCommandBlockAndButtonToStartTest(this.structureBlockPos, new BlockPos(1, 0, -1), this.getRotation(), this.level);
      StructureUtils.encaseStructure(this.getStructureBounds(), this.level, !this.testFunction.skyAccess());
      this.listeners.forEach(p_127630_ -> p_127630_.testStructureLoaded(this));
      return this;
   }

   long getTick() {
      return this.tickCount;
   }

   GameTestSequence createSequence() {
      GameTestSequence $$0 = new GameTestSequence(this);
      this.sequences.add($$0);
      return $$0;
   }

   public boolean isRequired() {
      return this.testFunction.required();
   }

   public boolean isOptional() {
      return !this.testFunction.required();
   }

   public String getStructureName() {
      return this.testFunction.structureName();
   }

   public Rotation getRotation() {
      return this.rotation;
   }

   public TestFunction getTestFunction() {
      return this.testFunction;
   }

   public int getTimeoutTicks() {
      return this.timeoutTicks;
   }

   public boolean isFlaky() {
      return this.testFunction.isFlaky();
   }

   public int maxAttempts() {
      return this.testFunction.maxAttempts();
   }

   public int requiredSuccesses() {
      return this.testFunction.requiredSuccesses();
   }

   public RetryOptions retryOptions() {
      return this.retryOptions;
   }

   public Stream<GameTestListener> getListeners() {
      return this.listeners.stream();
   }

   public GameTestInfo copyReset() {
      GameTestInfo $$0 = new GameTestInfo(this.testFunction, this.rotation, this.level, this.retryOptions());
      if (this.northWestCorner != null) {
         $$0.setNorthWestCorner(this.northWestCorner);
      }

      if (this.structureBlockPos != null) {
         $$0.setStructureBlockPos(this.structureBlockPos);
      }

      return $$0;
   }

   private BlockPos getOrCalculateNorthwestCorner() {
      if (this.northWestCorner == null) {
         BoundingBox $$0 = StructureUtils.getStructureBoundingBox(this.getStructureBlockEntity());
         this.northWestCorner = new BlockPos($$0.minX(), $$0.minY(), $$0.minZ());
      }

      return this.northWestCorner;
   }

   public void setNorthWestCorner(BlockPos pNorthWestCorner) {
      this.northWestCorner = pNorthWestCorner;
   }
}
