package net.minecraft.client.searchtree;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.util.Comparator;
import java.util.Iterator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IntersectionIterator<T> extends AbstractIterator<T> {
   private final PeekingIterator<T> firstIterator;
   private final PeekingIterator<T> secondIterator;
   private final Comparator<T> comparator;

   public IntersectionIterator(Iterator<T> pFirstIterator, Iterator<T> pSecondIterator, Comparator<T> pComparator) {
      this.firstIterator = Iterators.peekingIterator(pFirstIterator);
      this.secondIterator = Iterators.peekingIterator(pSecondIterator);
      this.comparator = pComparator;
   }

   protected T computeNext() {
      while (this.firstIterator.hasNext() && this.secondIterator.hasNext()) {
         int $$0 = this.comparator.compare((T)this.firstIterator.peek(), (T)this.secondIterator.peek());
         if ($$0 == 0) {
            this.secondIterator.next();
            return (T)this.firstIterator.next();
         }

         if ($$0 < 0) {
            this.firstIterator.next();
         } else {
            this.secondIterator.next();
         }
      }

      return (T)this.endOfData();
   }
}
