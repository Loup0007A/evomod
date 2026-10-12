package net.minecraft.client.renderer;

import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SectionBufferBuilderPool {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Queue<SectionBufferBuilderPack> freeBuffers;
   private volatile int freeBufferCount;

   private SectionBufferBuilderPool(List<SectionBufferBuilderPack> pFreeBuffers) {
      this.freeBuffers = Queues.newArrayDeque(pFreeBuffers);
      this.freeBufferCount = this.freeBuffers.size();
   }

   public static SectionBufferBuilderPool allocate(int pBufferCount) {
      int $$1 = Math.max(1, (int)((double)Runtime.getRuntime().maxMemory() * 0.3) / SectionBufferBuilderPack.TOTAL_BUFFERS_SIZE);
      int $$2 = Math.max(1, Math.min(pBufferCount, $$1));
      List<SectionBufferBuilderPack> $$3 = new ArrayList<>($$2);

      try {
         for (int $$4 = 0; $$4 < $$2; $$4++) {
            $$3.add(new SectionBufferBuilderPack());
         }
      } catch (OutOfMemoryError var7) {
         LOGGER.warn("Allocated only {}/{} buffers", $$3.size(), $$2);
         int $$6 = Math.min($$3.size() * 2 / 3, $$3.size() - 1);

         for (int $$7 = 0; $$7 < $$6; $$7++) {
            $$3.remove($$3.size() - 1).close();
         }
      }

      return new SectionBufferBuilderPool($$3);
   }

   @Nullable
   public SectionBufferBuilderPack acquire() {
      SectionBufferBuilderPack $$0 = this.freeBuffers.poll();
      if ($$0 != null) {
         this.freeBufferCount = this.freeBuffers.size();
         return $$0;
      } else {
         return null;
      }
   }

   public void release(SectionBufferBuilderPack pBuffer) {
      this.freeBuffers.add(pBuffer);
      this.freeBufferCount = this.freeBuffers.size();
   }

   public boolean isEmpty() {
      return this.freeBuffers.isEmpty();
   }

   public int getFreeBufferCount() {
      return this.freeBufferCount;
   }
}
