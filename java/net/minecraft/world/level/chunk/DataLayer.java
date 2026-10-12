package net.minecraft.world.level.chunk;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.VisibleForDebug;

public class DataLayer {
   public static final int LAYER_COUNT = 16;
   public static final int LAYER_SIZE = 128;
   public static final int SIZE = 2048;
   private static final int NIBBLE_SIZE = 4;
   @Nullable
   protected byte[] data;
   private int defaultValue;

   public DataLayer() {
      this(0);
   }

   public DataLayer(int pSize) {
      this.defaultValue = pSize;
   }

   public DataLayer(byte[] pData) {
      this.data = pData;
      this.defaultValue = 0;
      if (pData.length != 2048) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("DataLayer should be 2048 bytes not: " + pData.length));
      }
   }

   public int get(int pX, int pY, int pZ) {
      return this.get(getIndex(pX, pY, pZ));
   }

   public void set(int pX, int pY, int pZ, int pValue) {
      this.set(getIndex(pX, pY, pZ), pValue);
   }

   private static int getIndex(int pX, int pY, int pZ) {
      return pY << 8 | pZ << 4 | pX;
   }

   private int get(int pIndex) {
      if (this.data == null) {
         return this.defaultValue;
      } else {
         int $$1 = getByteIndex(pIndex);
         int $$2 = getNibbleIndex(pIndex);
         return this.data[$$1] >> 4 * $$2 & 15;
      }
   }

   private void set(int pIndex, int pValue) {
      byte[] $$2 = this.getData();
      int $$3 = getByteIndex(pIndex);
      int $$4 = getNibbleIndex(pIndex);
      int $$5 = ~(15 << 4 * $$4);
      int $$6 = (pValue & 15) << 4 * $$4;
      $$2[$$3] = (byte)($$2[$$3] & $$5 | $$6);
   }

   private static int getNibbleIndex(int pIndex) {
      return pIndex & 1;
   }

   private static int getByteIndex(int pIndex) {
      return pIndex >> 1;
   }

   public void fill(int pDefaultValue) {
      this.defaultValue = pDefaultValue;
      this.data = null;
   }

   private static byte packFilled(int pValue) {
      byte $$1 = (byte)pValue;

      for (int $$2 = 4; $$2 < 8; $$2 += 4) {
         $$1 = (byte)($$1 | pValue << $$2);
      }

      return $$1;
   }

   public byte[] getData() {
      if (this.data == null) {
         this.data = new byte[2048];
         if (this.defaultValue != 0) {
            Arrays.fill(this.data, packFilled(this.defaultValue));
         }
      }

      return this.data;
   }

   public DataLayer copy() {
      return this.data == null ? new DataLayer(this.defaultValue) : new DataLayer((byte[])this.data.clone());
   }

   @Override
   public String toString() {
      StringBuilder $$0 = new StringBuilder();

      for (int $$1 = 0; $$1 < 4096; $$1++) {
         $$0.append(Integer.toHexString(this.get($$1)));
         if (($$1 & 15) == 15) {
            $$0.append("\n");
         }

         if (($$1 & 0xFF) == 255) {
            $$0.append("\n");
         }
      }

      return $$0.toString();
   }

   @VisibleForDebug
   public String layerToString(int pUnused) {
      StringBuilder $$1 = new StringBuilder();

      for (int $$2 = 0; $$2 < 256; $$2++) {
         $$1.append(Integer.toHexString(this.get($$2)));
         if (($$2 & 15) == 15) {
            $$1.append("\n");
         }
      }

      return $$1.toString();
   }

   public boolean isDefinitelyHomogenous() {
      return this.data == null;
   }

   public boolean isDefinitelyFilledWith(int pValue) {
      return this.data == null && this.defaultValue == pValue;
   }

   public boolean isEmpty() {
      return this.data == null && this.defaultValue == 0;
   }
}
