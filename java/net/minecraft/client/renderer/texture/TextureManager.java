package net.minecraft.client.renderer.texture;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureManager implements PreparableReloadListener, Tickable, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final ResourceLocation INTENTIONAL_MISSING_TEXTURE = ResourceLocation.withDefaultNamespace("");
   private final Map<ResourceLocation, AbstractTexture> byPath = Maps.newHashMap();
   private final Set<Tickable> tickableTextures = Sets.newHashSet();
   private final Map<String, Integer> prefixRegister = Maps.newHashMap();
   private final ResourceManager resourceManager;

   public TextureManager(ResourceManager pResourceManager) {
      this.resourceManager = pResourceManager;
   }

   public void bindForSetup(ResourceLocation pPath) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> this._bind(pPath));
      } else {
         this._bind(pPath);
      }
   }

   private void _bind(ResourceLocation pPath) {
      AbstractTexture $$1 = this.byPath.get(pPath);
      if ($$1 == null) {
         $$1 = new SimpleTexture(pPath);
         this.register(pPath, $$1);
      }

      $$1.bind();
   }

   public void register(ResourceLocation pPath, AbstractTexture pTexture) {
      pTexture = this.loadTexture(pPath, pTexture);
      AbstractTexture $$2 = this.byPath.put(pPath, pTexture);
      if ($$2 != pTexture) {
         if ($$2 != null && $$2 != MissingTextureAtlasSprite.getTexture()) {
            this.safeClose(pPath, $$2);
         }

         if (pTexture instanceof Tickable) {
            this.tickableTextures.add((Tickable)pTexture);
         }
      }
   }

   private void safeClose(ResourceLocation p_118509_, AbstractTexture p_118510_) {
      if (p_118510_ != MissingTextureAtlasSprite.getTexture()) {
         this.tickableTextures.remove(p_118510_);

         try {
            p_118510_.close();
         } catch (Exception var4) {
            LOGGER.warn("Failed to close texture {}", p_118509_, var4);
         }
      }

      p_118510_.releaseId();
   }

   private AbstractTexture loadTexture(ResourceLocation pPath, AbstractTexture pTexture) {
      try {
         pTexture.load(this.resourceManager);
         return pTexture;
      } catch (IOException var6) {
         if (pPath != INTENTIONAL_MISSING_TEXTURE) {
            LOGGER.warn("Failed to load texture: {}", pPath, var6);
         }

         return MissingTextureAtlasSprite.getTexture();
      } catch (Throwable var7) {
         CrashReport $$4 = CrashReport.forThrowable(var7, "Registering texture");
         CrashReportCategory $$5 = $$4.addCategory("Resource location being registered");
         $$5.setDetail("Resource location", pPath);
         $$5.setDetail("Texture object class", () -> pTexture.getClass().getName());
         throw new ReportedException($$4);
      }
   }

   public AbstractTexture getTexture(ResourceLocation pPath) {
      AbstractTexture $$1 = this.byPath.get(pPath);
      if ($$1 == null) {
         $$1 = new SimpleTexture(pPath);
         this.register(pPath, $$1);
      }

      return $$1;
   }

   public AbstractTexture getTexture(ResourceLocation pPath, AbstractTexture pDefaultTexture) {
      return this.byPath.getOrDefault(pPath, pDefaultTexture);
   }

   public ResourceLocation register(String pName, DynamicTexture pTexture) {
      Integer $$2 = this.prefixRegister.get(pName);
      if ($$2 == null) {
         $$2 = 1;
      } else {
         $$2 = $$2 + 1;
      }

      this.prefixRegister.put(pName, $$2);
      ResourceLocation $$3 = ResourceLocation.withDefaultNamespace(String.format(Locale.ROOT, "dynamic/%s_%d", pName, $$2));
      this.register($$3, pTexture);
      return $$3;
   }

   public CompletableFuture<Void> preload(ResourceLocation pPath, Executor pBackgroundExecutor) {
      if (!this.byPath.containsKey(pPath)) {
         PreloadedTexture $$2 = new PreloadedTexture(this.resourceManager, pPath, pBackgroundExecutor);
         this.byPath.put(pPath, $$2);
         return $$2.getFuture().thenRunAsync(() -> this.register(pPath, $$2), TextureManager::execute);
      } else {
         return CompletableFuture.completedFuture(null);
      }
   }

   private static void execute(Runnable p_118489_) {
      Minecraft.getInstance().execute(() -> RenderSystem.recordRenderCall(p_118489_::run));
   }

   @Override
   public void tick() {
      for (Tickable $$0 : this.tickableTextures) {
         $$0.tick();
      }
   }

   public void release(ResourceLocation pPath) {
      AbstractTexture $$1 = this.byPath.remove(pPath);
      if ($$1 != null) {
         this.safeClose(pPath, $$1);
      }
   }

   @Override
   public void close() {
      this.byPath.forEach(this::safeClose);
      this.byPath.clear();
      this.tickableTextures.clear();
      this.prefixRegister.clear();
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.PreparationBarrier pStage,
      ResourceManager pResourceManager,
      ProfilerFiller pPreparationsProfiler,
      ProfilerFiller pReloadProfiler,
      Executor pBackgroundExecutor,
      Executor pGameExecutor
   ) {
      CompletableFuture<Void> $$6 = new CompletableFuture<>();
      TitleScreen.preloadResources(this, pBackgroundExecutor).thenCompose(pStage::wait).thenAcceptAsync(p_247950_ -> {
         MissingTextureAtlasSprite.getTexture();
         AddRealmPopupScreen.updateCarouselImages(this.resourceManager);
         Iterator<Entry<ResourceLocation, AbstractTexture>> $$4 = this.byPath.entrySet().iterator();

         while ($$4.hasNext()) {
            Entry<ResourceLocation, AbstractTexture> $$5 = $$4.next();
            ResourceLocation $$6x = $$5.getKey();
            AbstractTexture $$7 = $$5.getValue();
            if ($$7 == MissingTextureAtlasSprite.getTexture() && !$$6x.equals(MissingTextureAtlasSprite.getLocation())) {
               $$4.remove();
            } else {
               $$7.reset(this, pResourceManager, $$6x, pGameExecutor);
            }
         }

         Minecraft.getInstance().tell(() -> $$6.complete(null));
      }, p_118505_ -> RenderSystem.recordRenderCall(p_118505_::run));
      return $$6;
   }

   public void dumpAllSheets(Path pPath) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> this._dumpAllSheets(pPath));
      } else {
         this._dumpAllSheets(pPath);
      }
   }

   private void _dumpAllSheets(Path pPath) {
      try {
         Files.createDirectories(pPath);
      } catch (IOException var3) {
         LOGGER.error("Failed to create directory {}", pPath, var3);
         return;
      }

      this.byPath.forEach((p_276101_, p_276102_) -> {
         if (p_276102_ instanceof Dumpable $$3) {
            try {
               $$3.dumpContents(p_276101_, pPath);
            } catch (IOException var5) {
               LOGGER.error("Failed to dump texture {}", p_276101_, var5);
            }
         }
      });
   }
}
