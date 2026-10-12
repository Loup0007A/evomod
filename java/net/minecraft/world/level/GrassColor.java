package net.minecraft.world.level;

public class GrassColor {
   private static int[] pixels = new int[65536];

   public static void init(int[] pGrassBuffer) {
      pixels = pGrassBuffer;
   }

   public static int get(double pTemperature, double pHumidity) {
      pHumidity *= pTemperature;
      int $$2 = (int)((1.0 - pTemperature) * 255.0);
      int $$3 = (int)((1.0 - pHumidity) * 255.0);
      int $$4 = $$3 << 8 | $$2;
      return $$4 >= pixels.length ? -65281 : pixels[$$4];
   }

   public static int getDefaultColor() {
      return get(0.5, 1.0);
   }
}
