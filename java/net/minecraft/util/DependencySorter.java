package net.minecraft.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DependencySorter<K, V extends DependencySorter.Entry<K>> {
   private final Map<K, V> contents = new HashMap<>();

   public DependencySorter<K, V> addEntry(K pKey, V pValue) {
      this.contents.put(pKey, pValue);
      return this;
   }

   private void visitDependenciesAndElement(Multimap<K, K> pDependencies, Set<K> pVisited, K pElement, BiConsumer<K, V> pAction) {
      if (pVisited.add(pElement)) {
         pDependencies.get(pElement).forEach(p_285443_ -> this.visitDependenciesAndElement(pDependencies, pVisited, (K)p_285443_, pAction));
         V $$4 = this.contents.get(pElement);
         if ($$4 != null) {
            pAction.accept(pElement, $$4);
         }
      }
   }

   private static <K> boolean isCyclic(Multimap<K, K> pDependencies, K pSource, K pTarget) {
      Collection<K> $$3 = pDependencies.get(pTarget);
      return $$3.contains(pSource) ? true : $$3.stream().anyMatch(p_284974_ -> isCyclic(pDependencies, pSource, (K)p_284974_));
   }

   private static <K> void addDependencyIfNotCyclic(Multimap<K, K> pDependencies, K pSource, K pTarget) {
      if (!isCyclic(pDependencies, pSource, pTarget)) {
         pDependencies.put(pSource, pTarget);
      }
   }

   public void orderByDependencies(BiConsumer<K, V> pAction) {
      Multimap<K, K> $$1 = HashMultimap.create();
      this.contents.forEach((p_285415_, p_285018_) -> p_285018_.visitRequiredDependencies(p_285287_ -> addDependencyIfNotCyclic($$1, (K)p_285415_, p_285287_)));
      this.contents.forEach((p_285462_, p_285526_) -> p_285526_.visitOptionalDependencies(p_285513_ -> addDependencyIfNotCyclic($$1, (K)p_285462_, p_285513_)));
      Set<K> $$2 = new HashSet<>();
      this.contents.keySet().forEach(p_284996_ -> this.visitDependenciesAndElement($$1, $$2, (K)p_284996_, pAction));
   }

   public interface Entry<K> {
      void visitRequiredDependencies(Consumer<K> pVisitor);

      void visitOptionalDependencies(Consumer<K> pVisitor);
   }
}
