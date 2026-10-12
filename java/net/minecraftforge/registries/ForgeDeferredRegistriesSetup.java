package net.minecraftforge.registries;

import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class ForgeDeferredRegistriesSetup {
   private static boolean setup = false;

   public static void setup(IEventBus modEventBus) {
      synchronized (ForgeDeferredRegistriesSetup.class) {
         if (setup) {
            throw new IllegalStateException("Setup has already been called!");
         }

         setup = true;
      }

      for (DeferredRegister<?> reg : ForgeRegistries.registries) {
         reg.register(modEventBus);
      }
   }
}
