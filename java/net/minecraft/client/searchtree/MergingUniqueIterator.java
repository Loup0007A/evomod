package net.minecraft.client.searchtree;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.util.Comparator;
import java.util.Iterator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MergingUniqueIterator<T> extends AbstractIterator<T> {
   private final PeekingIterator<T> firstIterator;
   private final PeekingIterator<T> secondIterator;
   private final Comparator<T> comparator;

   public MergingUniqueIterator(Iterator<T> pFirstIterator, Iterator<T> pSecondIterator, Comparator<T> pComparator) {
      this.firstIterator = Iterators.peekingIterator(pFirstIterator);
      this.secondIterator = Iterators.peekingIterator(pSecondIterator);
      this.comparator = pComparator;
   }

   protected T computeNext() {
      boolean $$0 = !this.firstIterator.hasNext();
      boolean $$1 = !this.secondIterator.hasNext();
      if ($$0 && $$1) {
         return (T)this.endOfData();
      } else if ($$0) {
         return (T)this.secondIterator.next();
      } else if ($$1) {
         return (T)this.firstIterator.next();
      } else {
         int $$2 = this.comparator.compare((T)this.firstIterator.peek(), (T)this.secondIterator.peek());
         if ($$2 == 0) {
            this.secondIterator.next();
         }

         return (T)($$2 <= 0 ? this.firstIterator.next() : this.secondIterator.next());
      }
   }
}
