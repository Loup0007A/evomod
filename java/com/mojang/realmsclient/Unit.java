package com.mojang.realmsclient;

import java.util.Locale;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum Unit {
   B,
   KB,
   MB,
   GB;

   private static final int BASE_UNIT = 1024;

   public static Unit getLargest(long pBytes) {
      if (pBytes < 1024L) {
         return B;
      } else {
         try {
            int $$1 = (int)(Math.log((double)pBytes) / Math.log(1024.0));
            String $$2 = String.valueOf("KMGTPE".charAt($$1 - 1));
            return valueOf($$2 + "B");
         } catch (Exception var4) {
            return GB;
         }
      }
   }

   public static double convertTo(long pBytes, Unit pUnit) {
      return pUnit == B ? (double)pBytes : (double)pBytes / Math.pow(1024.0, (double)pUnit.ordinal());
   }

   public static String humanReadable(long pBytes) {
      int $$1 = 1024;
      if (pBytes < 1024L) {
         return pBytes + " B";
      } else {
         int $$2 = (int)(Math.log((double)pBytes) / Math.log(1024.0));
         String $$3 = "KMGTPE".charAt($$2 - 1) + "";
         return String.format(Locale.ROOT, "%.1f %sB", (double)pBytes / Math.pow(1024.0, (double)$$2), $$3);
      }
   }

   public static String humanReadable(long pBytes, Unit pUnit) {
      return String.format(Locale.ROOT, "%." + (pUnit == GB ? "1" : "0") + "f %s", convertTo(pBytes, pUnit), pUnit.name());
   }
}
