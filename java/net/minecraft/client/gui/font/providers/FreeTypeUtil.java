package net.minecraft.client.gui.font.providers;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class FreeTypeUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Object LIBRARY_LOCK = new Object();
   private static long library = 0L;

   public static long getLibrary() {
      synchronized (LIBRARY_LOCK) {
         if (library == 0L) {
            MemoryStack $$0 = MemoryStack.stackPush();

            try {
               PointerBuffer $$1 = $$0.mallocPointer(1);
               assertError(FreeType.FT_Init_FreeType($$1), "Initializing FreeType library");
               library = $$1.get();
            } catch (Throwable var6) {
               if ($$0 != null) {
                  try {
                     $$0.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if ($$0 != null) {
               $$0.close();
            }
         }

         return library;
      }
   }

   public static void assertError(int pErrorId, String pAction) {
      if (pErrorId != 0) {
         throw new IllegalStateException("FreeType error: " + describeError(pErrorId) + " (" + pAction + ")");
      }
   }

   public static boolean checkError(int pErrorId, String pAction) {
      if (pErrorId != 0) {
         LOGGER.error("FreeType error: {} ({})", describeError(pErrorId), pAction);
         return true;
      } else {
         return false;
      }
   }

   private static String describeError(int pErrorId) {
      String $$1 = FreeType.FT_Error_String(pErrorId);
      return $$1 != null ? $$1 : "Unrecognized error: 0x" + Integer.toHexString(pErrorId);
   }

   public static FT_Vector setVector(FT_Vector pVector, float pX, float pY) {
      long $$3 = (long)Math.round(pX * 64.0F);
      long $$4 = (long)Math.round(pY * 64.0F);
      return pVector.set($$3, $$4);
   }

   public static float x(FT_Vector pVector) {
      return (float)pVector.x() / 64.0F;
   }

   public static void destroy() {
      synchronized (LIBRARY_LOCK) {
         if (library != 0L) {
            FreeType.FT_Done_Library(library);
            library = 0L;
         }
      }
   }
}
