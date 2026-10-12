package net.minecraft.client.sounds;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.audio.ListenerTransform;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.MultipliedFloats;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SoundManager extends SimplePreparableReloadListener<SoundManager.Preparations> {
   public static final ResourceLocation EMPTY_SOUND_LOCATION = ResourceLocation.withDefaultNamespace("empty");
   public static final Sound EMPTY_SOUND = new Sound(EMPTY_SOUND_LOCATION, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, false, false, 16);
   public static final ResourceLocation INTENTIONALLY_EMPTY_SOUND_LOCATION = ResourceLocation.withDefaultNamespace("intentionally_empty");
   public static final WeighedSoundEvents INTENTIONALLY_EMPTY_SOUND_EVENT = new WeighedSoundEvents(INTENTIONALLY_EMPTY_SOUND_LOCATION, null);
   public static final Sound INTENTIONALLY_EMPTY_SOUND = new Sound(
      INTENTIONALLY_EMPTY_SOUND_LOCATION, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, false, false, 16
   );
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String SOUNDS_PATH = "sounds.json";
   private static final Gson GSON = new GsonBuilder()
      .registerTypeHierarchyAdapter(Component.class, new Component.SerializerAdapter(RegistryAccess.EMPTY))
      .registerTypeAdapter(SoundEventRegistration.class, new SoundEventRegistrationSerializer())
      .create();
   private static final TypeToken<Map<String, SoundEventRegistration>> SOUND_EVENT_REGISTRATION_TYPE = new TypeToken<Map<String, SoundEventRegistration>>() {
   };
   private final Map<ResourceLocation, WeighedSoundEvents> registry = Maps.newHashMap();
   private final SoundEngine soundEngine;
   private final Map<ResourceLocation, Resource> soundCache = new HashMap<>();

   public SoundManager(Options pOptions) {
      this.soundEngine = new SoundEngine(this, pOptions, ResourceProvider.fromMap(this.soundCache));
   }

   protected SoundManager.Preparations prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      SoundManager.Preparations $$2 = new SoundManager.Preparations();
      pProfiler.startTick();
      pProfiler.push("list");
      $$2.listResources(pResourceManager);
      pProfiler.pop();

      for (String $$3 : pResourceManager.getNamespaces()) {
         pProfiler.push($$3);

         try {
            for (Resource $$5 : pResourceManager.getResourceStack(ResourceLocation.fromNamespaceAndPath($$3, "sounds.json"))) {
               pProfiler.push($$5.sourcePackId());

               try (Reader $$6 = $$5.openAsReader()) {
                  pProfiler.push("parse");
                  Map<String, SoundEventRegistration> $$7 = GsonHelper.fromJson(GSON, $$6, SOUND_EVENT_REGISTRATION_TYPE);
                  pProfiler.popPush("register");

                  for (Entry<String, SoundEventRegistration> $$8 : $$7.entrySet()) {
                     $$2.handleRegistration(ResourceLocation.fromNamespaceAndPath($$3, $$8.getKey()), $$8.getValue());
                  }

                  pProfiler.pop();
               } catch (RuntimeException var15) {
                  LOGGER.warn("Invalid {} in resourcepack: '{}'", new Object[]{"sounds.json", $$5.sourcePackId(), var15});
               }

               pProfiler.pop();
            }
         } catch (IOException var16) {
         }

         pProfiler.pop();
      }

      pProfiler.endTick();
      return $$2;
   }

   protected void apply(SoundManager.Preparations pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      pObject.apply(this.registry, this.soundCache, this.soundEngine);
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         for (ResourceLocation $$3 : this.registry.keySet()) {
            WeighedSoundEvents $$4 = this.registry.get($$3);
            if (!ComponentUtils.isTranslationResolvable($$4.getSubtitle()) && BuiltInRegistries.SOUND_EVENT.containsKey($$3)) {
               LOGGER.error("Missing subtitle {} for sound event: {}", $$4.getSubtitle(), $$3);
            }
         }
      }

      if (LOGGER.isDebugEnabled()) {
         for (ResourceLocation $$5 : this.registry.keySet()) {
            if (!BuiltInRegistries.SOUND_EVENT.containsKey($$5)) {
               LOGGER.debug("Not having sound event for: {}", $$5);
            }
         }
      }

      this.soundEngine.reload();
   }

   public List<String> getAvailableSoundDevices() {
      return this.soundEngine.getAvailableSoundDevices();
   }

   public ListenerTransform getListenerTransform() {
      return this.soundEngine.getListenerTransform();
   }

   static boolean validateSoundResource(Sound pSound, ResourceLocation pLocation, ResourceProvider pResourceProvider) {
      ResourceLocation $$3 = pSound.getPath();
      if (pResourceProvider.getResource($$3).isEmpty()) {
         LOGGER.warn("File {} does not exist, cannot add it to event {}", $$3, pLocation);
         return false;
      } else {
         return true;
      }
   }

   @Nullable
   public WeighedSoundEvents getSoundEvent(ResourceLocation pLocation) {
      return this.registry.get(pLocation);
   }

   public Collection<ResourceLocation> getAvailableSounds() {
      return this.registry.keySet();
   }

   public void queueTickingSound(TickableSoundInstance pTickableSound) {
      this.soundEngine.queueTickingSound(pTickableSound);
   }

   public void play(SoundInstance pSound) {
      this.soundEngine.play(pSound);
   }

   public void playDelayed(SoundInstance pSound, int pDelay) {
      this.soundEngine.playDelayed(pSound, pDelay);
   }

   public void updateSource(Camera pActiveRenderInfo) {
      this.soundEngine.updateSource(pActiveRenderInfo);
   }

   public void pause() {
      this.soundEngine.pause();
   }

   public void stop() {
      this.soundEngine.stopAll();
   }

   public void destroy() {
      this.soundEngine.destroy();
   }

   public void emergencyShutdown() {
      this.soundEngine.emergencyShutdown();
   }

   public void tick(boolean pIsGamePaused) {
      this.soundEngine.tick(pIsGamePaused);
   }

   public void resume() {
      this.soundEngine.resume();
   }

   public void updateSourceVolume(SoundSource pCategory, float pVolume) {
      if (pCategory == SoundSource.MASTER && pVolume <= 0.0F) {
         this.stop();
      }

      this.soundEngine.updateCategoryVolume(pCategory, pVolume);
   }

   public void stop(SoundInstance pSound) {
      this.soundEngine.stop(pSound);
   }

   public boolean isActive(SoundInstance pSound) {
      return this.soundEngine.isActive(pSound);
   }

   public void addListener(SoundEventListener pListener) {
      this.soundEngine.addEventListener(pListener);
   }

   public void removeListener(SoundEventListener pListener) {
      this.soundEngine.removeEventListener(pListener);
   }

   public void stop(@Nullable ResourceLocation pId, @Nullable SoundSource pCategory) {
      this.soundEngine.stop(pId, pCategory);
   }

   public String getDebugString() {
      return this.soundEngine.getDebugString();
   }

   public void reload() {
      this.soundEngine.reload();
   }

   @OnlyIn(Dist.CLIENT)
   protected static class Preparations {
      final Map<ResourceLocation, WeighedSoundEvents> registry = Maps.newHashMap();
      private Map<ResourceLocation, Resource> soundCache = Map.of();

      void listResources(ResourceManager pResourceManager) {
         this.soundCache = Sound.SOUND_LISTER.listMatchingResources(pResourceManager);
      }

      void handleRegistration(ResourceLocation pLocation, SoundEventRegistration pRegistration) {
         WeighedSoundEvents $$2 = this.registry.get(pLocation);
         boolean $$3 = $$2 == null;
         if ($$3 || pRegistration.isReplace()) {
            if (!$$3) {
               SoundManager.LOGGER.debug("Replaced sound event location {}", pLocation);
            }

            $$2 = new WeighedSoundEvents(pLocation, pRegistration.getSubtitle());
            this.registry.put(pLocation, $$2);
         }

         ResourceProvider $$4 = ResourceProvider.fromMap(this.soundCache);

         for (final Sound $$5 : pRegistration.getSounds()) {
            final ResourceLocation $$6 = $$5.getLocation();
            Weighted<Sound> $$8;
            switch ($$5.getType()) {
               case FILE:
                  if (!SoundManager.validateSoundResource($$5, pLocation, $$4)) {
                     continue;
                  }

                  $$8 = $$5;
                  break;
               case SOUND_EVENT:
                  $$8 = new Weighted<Sound>() {
                     @Override
                     public int getWeight() {
                        WeighedSoundEvents $$0 = Preparations.this.registry.get($$6);
                        return $$0 == null ? 0 : $$0.getWeight();
                     }

                     public Sound getSound(RandomSource p_235261_) {
                        WeighedSoundEvents $$1 = Preparations.this.registry.get($$6);
                        if ($$1 == null) {
                           return SoundManager.EMPTY_SOUND;
                        } else {
                           Sound $$2 = $$1.getSound(p_235261_);
                           return new Sound(
                              $$2.getLocation(),
                              new MultipliedFloats($$2.getVolume(), $$5.getVolume()),
                              new MultipliedFloats($$2.getPitch(), $$5.getPitch()),
                              $$5.getWeight(),
                              Sound.Type.FILE,
                              $$2.shouldStream() || $$5.shouldStream(),
                              $$2.shouldPreload(),
                              $$2.getAttenuationDistance()
                           );
                        }
                     }

                     @Override
                     public void preloadIfRequired(SoundEngine p_120438_) {
                        WeighedSoundEvents $$1 = Preparations.this.registry.get($$6);
                        if ($$1 != null) {
                           $$1.preloadIfRequired(p_120438_);
                        }
                     }
                  };
                  break;
               default:
                  throw new IllegalStateException("Unknown SoundEventRegistration type: " + $$5.getType());
            }

            $$2.addSound($$8);
         }
      }

      public void apply(Map<ResourceLocation, WeighedSoundEvents> pSoundRegistry, Map<ResourceLocation, Resource> pCache, SoundEngine pSoundEngine) {
         pSoundRegistry.clear();
         pCache.clear();
         pCache.putAll(this.soundCache);

         for (Entry<ResourceLocation, WeighedSoundEvents> $$3 : this.registry.entrySet()) {
            pSoundRegistry.put($$3.getKey(), $$3.getValue());
            $$3.getValue().preloadIfRequired(pSoundEngine);
         }
      }
   }
}
