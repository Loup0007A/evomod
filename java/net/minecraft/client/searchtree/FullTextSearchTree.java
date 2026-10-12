package net.minecraft.client.searchtree;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FullTextSearchTree<T> extends IdSearchTree<T> {
   private final SearchTree<T> plainTextSearchTree;

   public FullTextSearchTree(Function<T, Stream<String>> pFilter, Function<T, Stream<ResourceLocation>> pIdGetter, List<T> pContents) {
      super(pIdGetter, pContents);
      this.plainTextSearchTree = SearchTree.plainText(pContents, pFilter);
   }

   @Override
   protected List<T> searchPlainText(String pQuery) {
      return this.plainTextSearchTree.search(pQuery);
   }

   @Override
   protected List<T> searchResourceLocation(String pNamespace, String pPath) {
      List<T> $$2 = this.resourceLocationSearchTree.searchNamespace(pNamespace);
      List<T> $$3 = this.resourceLocationSearchTree.searchPath(pPath);
      List<T> $$4 = this.plainTextSearchTree.search(pPath);
      Iterator<T> $$5 = new MergingUniqueIterator<T>($$3.iterator(), $$4.iterator(), this.additionOrder);
      return ImmutableList.copyOf(new IntersectionIterator<T>($$2.iterator(), $$5, this.additionOrder));
   }
}
