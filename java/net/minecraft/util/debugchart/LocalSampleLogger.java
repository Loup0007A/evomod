package net.minecraft.util.debugchart;

public class LocalSampleLogger extends AbstractSampleLogger implements SampleStorage {
   public static final int CAPACITY = 240;
   private final long[][] samples;
   private int start;
   private int size;

   public LocalSampleLogger(int pSize) {
      this(pSize, new long[pSize]);
   }

   public LocalSampleLogger(int pSize, long[] pDefaults) {
      super(pSize, pDefaults);
      this.samples = new long[240][pSize];
   }

   @Override
   protected void useSample() {
      int $$0 = this.wrapIndex(this.start + this.size);
      System.arraycopy(this.sample, 0, this.samples[$$0], 0, this.sample.length);
      if (this.size < 240) {
         this.size++;
      } else {
         this.start = this.wrapIndex(this.start + 1);
      }
   }

   @Override
   public int capacity() {
      return this.samples.length;
   }

   @Override
   public int size() {
      return this.size;
   }

   @Override
   public long get(int pIndex) {
      return this.get(pIndex, 0);
   }

   @Override
   public long get(int pIndex, int pDimension) {
      if (pIndex >= 0 && pIndex < this.size) {
         long[] $$2 = this.samples[this.wrapIndex(this.start + pIndex)];
         if (pDimension >= 0 && pDimension < $$2.length) {
            return $$2[pDimension];
         } else {
            throw new IndexOutOfBoundsException(pDimension + " out of bounds for dimensions " + $$2.length);
         }
      } else {
         throw new IndexOutOfBoundsException(pIndex + " out of bounds for length " + this.size);
      }
   }

   private int wrapIndex(int pIndex) {
      return pIndex % 240;
   }

   @Override
   public void reset() {
      this.start = 0;
      this.size = 0;
   }
}
