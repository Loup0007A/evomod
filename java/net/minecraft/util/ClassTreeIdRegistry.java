package net.minecraft.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.Util;

public class ClassTreeIdRegistry {
   public static final int NO_ID_VALUE = -1;
   private final Object2IntMap<Class<?>> classToLastIdCache = Util.make(new Object2IntOpenHashMap(), p_327761_ -> p_327761_.defaultReturnValue(-1));

   public int getLastIdFor(Class<?> pClazz) {
      int $$1 = this.classToLastIdCache.getInt(pClazz);
      if ($$1 != -1) {
         return $$1;
      } else {
         Class<?> $$2 = pClazz;

         while (($$2 = $$2.getSuperclass()) != Object.class) {
            int $$3 = this.classToLastIdCache.getInt($$2);
            if ($$3 != -1) {
               return $$3;
            }
         }

         return -1;
      }
   }

   public int getCount(Class<?> pClazz) {
      return this.getLastIdFor(pClazz) + 1;
   }

   public int define(Class<?> pClazz) {
      int $$1 = this.getLastIdFor(pClazz);
      int $$2 = $$1 == -1 ? 0 : $$1 + 1;
      this.classToLastIdCache.put(pClazz, $$2);
      return $$2;
   }
}
