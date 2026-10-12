package net.minecraft.client.searchtree;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IdSearchTree<T> implements SearchTree<T> {
   protected final Comparator<T> additionOrder;
   protected final ResourceLocationSearchTree<T> resourceLocationSearchTree;

   public IdSearchTree(Function<T, Stream<ResourceLocation>> pIdGetter, List<T> pContents) {
      ToIntFunction<T> $$2 = Util.createIndexLookup(pContents);
      this.additionOrder = Comparator.comparingInt($$2);
      this.resourceLocationSearchTree = ResourceLocationSearchTree.create(pContents, pIdGetter);
   }

   @Override
   public List<T> search(String pQuery) {
      int $$1 = pQuery.indexOf(58);
      return $$1 == -1 ? this.searchPlainText(pQuery) : this.searchResourceLocation(pQuery.substring(0, $$1).trim(), pQuery.substring($$1 + 1).trim());
   }

   protected List<T> searchPlainText(String pQuery) {
      return this.resourceLocationSearchTree.searchPath(pQuery);
   }

   protected List<T> searchResourceLocation(String pNamespace, String pPath) {
      List<T> $$2 = this.resourceLocationSearchTree.searchNamespace(pNamespace);
      List<T> $$3 = this.resourceLocationSearchTree.searchPath(pPath);
      return ImmutableList.copyOf(new IntersectionIterator<T>($$2.iterator(), $$3.iterator(), this.additionOrder));
   }
}
