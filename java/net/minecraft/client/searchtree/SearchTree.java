package net.minecraft.client.searchtree;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface SearchTree<T> {
   static <T> SearchTree<T> empty() {
      return p_344644_ -> List.of();
   }

   static <T> SearchTree<T> plainText(List<T> pContents, Function<T, Stream<String>> pFilter) {
      if (pContents.isEmpty()) {
         return empty();
      } else {
         SuffixArray<T> $$2 = new SuffixArray<>();

         for (T $$3 : pContents) {
            pFilter.apply($$3).forEach(p_342612_ -> $$2.add($$3, p_342612_.toLowerCase(Locale.ROOT)));
         }

         $$2.generate();
         return $$2::search;
      }
   }

   List<T> search(String pQuery);
}
