package net.minecraftforge.gametest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.forgespi.language.ModFileScanData.AnnotationData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.objectweb.asm.Type;

@Internal
public class ForgeGameTestHooks {
   private static boolean registeredGametests = false;
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Type GAME_TEST_HOLDER = Type.getType(GameTestHolder.class);
   private static final String DEFAULT_BATCH = getDefaultBatch();

   public static boolean isGametestEnabled() {
      return !FMLLoader.isProduction() && (SharedConstants.IS_RUNNING_IN_IDE || isGametestServer() || Boolean.getBoolean("forge.enableGameTest"));
   }

   public static boolean isGametestServer() {
      return !FMLLoader.isProduction() && Boolean.getBoolean("forge.gameTestServer");
   }

   public static void registerGametests() {
      if (!registeredGametests && isGametestEnabled() && ModLoader.isLoadingStateValid()) {
         Set<String> enabledNamespaces = getEnabledNamespaces();
         LOGGER.info("Enabled Gametest Namespaces: {}", enabledNamespaces);
         Set<Method> gameTestMethods = new HashSet<>();
         RegisterGameTestsEvent event = new RegisterGameTestsEvent(gameTestMethods);
         ModLoader.get().postEvent(event);
         ModList.get()
            .getAllScanData()
            .stream()
            .map(ModFileScanData::getAnnotations)
            .flatMap(Collection::stream)
            .filter(a -> GAME_TEST_HOLDER.equals(a.annotationType()))
            .forEach(a -> addGameTestMethods(a, gameTestMethods));

         for (Method gameTestMethod : gameTestMethods) {
            GameTestRegistry.register(gameTestMethod, enabledNamespaces);
         }

         registeredGametests = true;
      }
   }

   private static Set<String> getEnabledNamespaces() {
      String enabledNamespacesStr = System.getProperty("forge.enabledGameTestNamespaces");
      return enabledNamespacesStr == null
         ? Set.of()
         : Arrays.stream(enabledNamespacesStr.split(",")).filter(s -> !s.isBlank()).collect(Collectors.toUnmodifiableSet());
   }

   private static void addGameTestMethods(AnnotationData annotationData, Set<Method> gameTestMethods) {
      try {
         Class<?> clazz = Class.forName(annotationData.clazz().getClassName(), true, ForgeGameTestHooks.class.getClassLoader());
         gameTestMethods.addAll(Arrays.asList(clazz.getDeclaredMethods()));
      } catch (ClassNotFoundException var3) {
         throw new RuntimeException(var3);
      }
   }

   public static String getTestName(Method method, GameTest meta) {
      String name = method.getName().toLowerCase(Locale.ENGLISH);
      return getPrefixed(method, name);
   }

   private static String getPrefixed(Method method, String name) {
      String prefix = getPrefix(method);
      return prefix == null ? name : prefix + "." + name;
   }

   @Nullable
   private static String getPrefix(Method method) {
      Class<?> cls = method.getDeclaringClass();
      boolean shouldPrefix = !method.isAnnotationPresent(GameTestDontPrefix.class) && !cls.isAnnotationPresent(GameTestDontPrefix.class);
      if (!shouldPrefix) {
         return null;
      } else {
         GameTestPrefix prefix = cls.getAnnotation(GameTestPrefix.class);
         if (prefix != null) {
            return prefix.value();
         } else {
            GameTestHolder holder = cls.getAnnotation(GameTestHolder.class);
            if (holder != null && !holder.value().isEmpty()) {
               return holder.value();
            } else {
               Mod mod = method.getDeclaringClass().getAnnotation(Mod.class);
               return mod != null ? mod.value() : cls.getSimpleName().toLowerCase(Locale.ENGLISH);
            }
         }
      }
   }

   public static String getTestTemplate(Method method, GameTest meta, String testName) {
      if (meta.template().indexOf(58) != -1) {
         return meta.template();
      } else {
         String template = null;
         if (meta.template().isEmpty()) {
            template = testName;
         } else {
            template = getPrefixed(method, meta.template());
         }

         Class<?> cls = method.getDeclaringClass();
         GameTestHolder holder = cls.getAnnotation(GameTestHolder.class);
         if (holder != null && !holder.namespace().isEmpty()) {
            return holder.namespace() + ":" + template;
         } else {
            Mod mod = cls.getAnnotation(Mod.class);
            return mod != null ? mod.value() + ":" + template : template;
         }
      }
   }

   private static String getDefaultBatch() {
      try {
         return (String)ObfuscationReflectionHelper.findMethod(GameTest.class, "m_177043_", new Class[0]).getDefaultValue();
      } catch (Exception var1) {
         var1.printStackTrace();
         return "defaultBatch";
      }
   }

   public static String getTestBatch(Method method, GameTest gametest) {
      String batch = gametest.batch();
      if (DEFAULT_BATCH.equals(batch)) {
         String prefix = getPrefix(method);
         return prefix == null ? batch : prefix;
      } else {
         return getPrefixed(method, batch.toLowerCase(Locale.ENGLISH));
      }
   }

   public static void addTest(Collection<TestFunction> functions, Set<String> classes, Set<String> filters, TestFunction func) {
      boolean allowed = filters.isEmpty() || filters.stream().anyMatch(f -> f.equals(func.batchName()) || func.batchName().startsWith(f + "."));
      if (allowed) {
         functions.add(func);
         String batch = func.batchName();
         classes.add(batch);
         int idx = batch.indexOf(46);

         while (idx != -1) {
            batch = batch.substring(0, idx);
            idx = batch.indexOf(46);
            classes.add(batch);
         }
      }
   }
}
