package net.minecraft.world.level.chunk.status;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.Locale;

public final class ChunkDependencies {
   private final ImmutableList<ChunkStatus> dependencyByRadius;
   private final int[] radiusByDependency;

   public ChunkDependencies(ImmutableList<ChunkStatus> pDependencyByRadius) {
      this.dependencyByRadius = pDependencyByRadius;
      int $$1 = pDependencyByRadius.isEmpty() ? 0 : ((ChunkStatus)pDependencyByRadius.getFirst()).getIndex() + 1;
      this.radiusByDependency = new int[$$1];

      for (int $$2 = 0; $$2 < pDependencyByRadius.size(); $$2++) {
         ChunkStatus $$3 = (ChunkStatus)pDependencyByRadius.get($$2);
         int $$4 = $$3.getIndex();

         for (int $$5 = 0; $$5 <= $$4; $$5++) {
            this.radiusByDependency[$$5] = $$2;
         }
      }
   }

   @VisibleForTesting
   public ImmutableList<ChunkStatus> asList() {
      return this.dependencyByRadius;
   }

   public int size() {
      return this.dependencyByRadius.size();
   }

   public int getRadiusOf(ChunkStatus pStatus) {
      int $$1 = pStatus.getIndex();
      if ($$1 >= this.radiusByDependency.length) {
         throw new IllegalArgumentException(
            String.format(Locale.ROOT, "Requesting a ChunkStatus(%s) outside of dependency range(%s)", pStatus, this.dependencyByRadius)
         );
      } else {
         return this.radiusByDependency[$$1];
      }
   }

   public int getRadius() {
      return Math.max(0, this.dependencyByRadius.size() - 1);
   }

   public ChunkStatus get(int pRadius) {
      return (ChunkStatus)this.dependencyByRadius.get(pRadius);
   }

   @Override
   public String toString() {
      return this.dependencyByRadius.toString();
   }
}
