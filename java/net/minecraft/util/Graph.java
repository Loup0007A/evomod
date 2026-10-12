package net.minecraft.util;

import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class Graph {
   private Graph() {
   }

   public static <T> boolean depthFirstSearch(
      Map<T, Set<T>> pGraph, Set<T> pNonCyclicalNodes, Set<T> pPathSet, Consumer<T> pOnNonCyclicalNodeFound, T pCurrentNode
   ) {
      if (pNonCyclicalNodes.contains(pCurrentNode)) {
         return false;
      } else if (pPathSet.contains(pCurrentNode)) {
         return true;
      } else {
         pPathSet.add(pCurrentNode);

         for (T $$5 : pGraph.getOrDefault(pCurrentNode, ImmutableSet.of())) {
            if (depthFirstSearch(pGraph, pNonCyclicalNodes, pPathSet, pOnNonCyclicalNodeFound, $$5)) {
               return true;
            }
         }

         pPathSet.remove(pCurrentNode);
         pNonCyclicalNodes.add(pCurrentNode);
         pOnNonCyclicalNodeFound.accept(pCurrentNode);
         return false;
      }
   }
}
