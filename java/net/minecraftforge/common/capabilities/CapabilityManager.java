package net.minecraftforge.common.capabilities;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.Logging;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.objectweb.asm.Type;

public final class CapabilityManager {
   @Deprecated(
      forRemoval = true,
      since = "1.21"
   )
   public static final CapabilityManager INSTANCE = new CapabilityManager();
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Type AUTO_REGISTER = Type.getType(AutoRegisterCapability.class);
   private static final Map<CapabilityManager.Key, Capability<?>> providers = new HashMap<>();

   public static <T> Capability<T> get(CapabilityToken<T> type) {
      return get(type.getType(), null, false);
   }

   public static <T> Capability<T> get(CapabilityToken<T> type, String modid, String name) {
      return get(type, ResourceLocation.fromNamespaceAndPath(modid, name));
   }

   public static <T> Capability<T> get(CapabilityToken<T> type, ResourceLocation name) {
      return get(type.getType(), name, false);
   }

   static <T> Capability<T> get(String type, ResourceLocation name, boolean registering) {
      Capability<T> cap;
      synchronized (providers) {
         Capability<T> parent = (Capability<T>)providers.computeIfAbsent(new CapabilityManager.Key(type, null), k -> new Capability(type.intern()));
         if (name == null) {
            cap = parent;
         } else {
            cap = (Capability<T>)providers.computeIfAbsent(new CapabilityManager.Key(type, name), k -> {
               Capability<Object> ret = new Capability<>((parent.getName() + "#" + name.toString()).intern());
               parent.addListener(p -> ret.onRegister());
               return ret;
            });
         }
      }

      if (registering) {
         if (name != null) {
            error("Cannot register capability type " + type + " + with a name: " + name);
         }

         synchronized (cap) {
            if (cap.isRegistered()) {
               error("Cannot register capability type " + type + " multiple times");
            }

            cap.onRegister();
         }
      }

      return cap;
   }

   @Internal
   public static void injectCapabilities(ModList modlist) {
      for (Type auto : modlist.getAllScanData()
         .stream()
         .flatMap(e -> e.getAnnotations().stream())
         .filter(a -> AUTO_REGISTER.equals(a.annotationType()))
         .map(a -> a.clazz())
         .distinct()
         .sorted(Comparator.comparing(Type::toString))
         .toList()) {
         LOGGER.debug(Logging.CAPABILITIES, "Attempting to automatically register: " + auto);
         get(auto.getInternalName(), null, true);
      }

      RegisterCapabilitiesEvent event = new RegisterCapabilitiesEvent();
      ModLoader.get().postEvent(event);
   }

   private static void error(String message) {
      LOGGER.error(Logging.CAPABILITIES, message);
      throw new IllegalStateException(message);
   }

   private static record Key(String type, ResourceLocation name) {
   }
}
