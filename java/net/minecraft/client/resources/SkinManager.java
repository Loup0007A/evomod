package net.minecraft.client.resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.properties.Property;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SkinManager {
   static final Logger LOGGER = LogUtils.getLogger();
   private final MinecraftSessionService sessionService;
   private final LoadingCache<SkinManager.CacheKey, CompletableFuture<PlayerSkin>> skinCache;
   private final SkinManager.TextureCache skinTextures;
   private final SkinManager.TextureCache capeTextures;
   private final SkinManager.TextureCache elytraTextures;

   public SkinManager(TextureManager pTextureManager, Path pRoot, final MinecraftSessionService pSessionService, final Executor pExecutor) {
      this.sessionService = pSessionService;
      this.skinTextures = new SkinManager.TextureCache(pTextureManager, pRoot, Type.SKIN);
      this.capeTextures = new SkinManager.TextureCache(pTextureManager, pRoot, Type.CAPE);
      this.elytraTextures = new SkinManager.TextureCache(pTextureManager, pRoot, Type.ELYTRA);
      this.skinCache = CacheBuilder.newBuilder()
         .expireAfterAccess(Duration.ofSeconds(15L))
         .build(new CacheLoader<SkinManager.CacheKey, CompletableFuture<PlayerSkin>>() {
            public CompletableFuture<PlayerSkin> load(SkinManager.CacheKey p_298169_) {
               return CompletableFuture.<MinecraftProfileTextures>supplyAsync(() -> {
                  Property $$2 = p_298169_.packedTextures();
                  if ($$2 == null) {
                     return MinecraftProfileTextures.EMPTY;
                  } else {
                     MinecraftProfileTextures $$3 = pSessionService.unpackTextures($$2);
                     if ($$3.signatureState() == SignatureState.INVALID) {
                        SkinManager.LOGGER.warn("Profile contained invalid signature for textures property (profile id: {})", p_298169_.profileId());
                     }

                     return $$3;
                  }
               }, Util.backgroundExecutor()).thenComposeAsync(p_308313_ -> SkinManager.this.registerTextures(p_298169_.profileId(), p_308313_), pExecutor);
            }
         });
   }

   public Supplier<PlayerSkin> lookupInsecure(GameProfile pProfile) {
      CompletableFuture<PlayerSkin> $$1 = this.getOrLoad(pProfile);
      PlayerSkin $$2 = DefaultPlayerSkin.get(pProfile);
      return () -> $$1.getNow($$2);
   }

   public PlayerSkin getInsecureSkin(GameProfile pProfile) {
      PlayerSkin $$1 = this.getOrLoad(pProfile).getNow(null);
      return $$1 != null ? $$1 : DefaultPlayerSkin.get(pProfile);
   }

   public CompletableFuture<PlayerSkin> getOrLoad(GameProfile pProfile) {
      Property $$1 = this.sessionService.getPackedTextures(pProfile);
      return (CompletableFuture<PlayerSkin>)this.skinCache.getUnchecked(new SkinManager.CacheKey(pProfile.getId(), $$1));
   }

   CompletableFuture<PlayerSkin> registerTextures(UUID pUuid, MinecraftProfileTextures pTextures) {
      MinecraftProfileTexture $$2 = pTextures.skin();
      CompletableFuture<ResourceLocation> $$3;
      PlayerSkin.Model $$4;
      if ($$2 != null) {
         $$3 = this.skinTextures.getOrLoad($$2);
         $$4 = PlayerSkin.Model.byName($$2.getMetadata("model"));
      } else {
         PlayerSkin $$5 = DefaultPlayerSkin.get(pUuid);
         $$3 = CompletableFuture.completedFuture($$5.texture());
         $$4 = $$5.model();
      }

      String $$8 = Optionull.map($$2, MinecraftProfileTexture::getUrl);
      MinecraftProfileTexture $$9 = pTextures.cape();
      CompletableFuture<ResourceLocation> $$10 = $$9 != null ? this.capeTextures.getOrLoad($$9) : CompletableFuture.completedFuture(null);
      MinecraftProfileTexture $$11 = pTextures.elytra();
      CompletableFuture<ResourceLocation> $$12 = $$11 != null ? this.elytraTextures.getOrLoad($$11) : CompletableFuture.completedFuture(null);
      return CompletableFuture.allOf($$3, $$10, $$12)
         .thenApply(p_308309_ -> new PlayerSkin($$3.join(), $$8, $$10.join(), $$12.join(), $$4, pTextures.signatureState() == SignatureState.SIGNED));
   }

   @OnlyIn(Dist.CLIENT)
   static record CacheKey(UUID profileId, @Nullable Property packedTextures) {
   }

   @OnlyIn(Dist.CLIENT)
   static class TextureCache {
      private final TextureManager textureManager;
      private final Path root;
      private final Type type;
      private final Map<String, CompletableFuture<ResourceLocation>> textures = new Object2ObjectOpenHashMap();

      TextureCache(TextureManager pTextureManager, Path pRoot, Type pType) {
         this.textureManager = pTextureManager;
         this.root = pRoot;
         this.type = pType;
      }

      public CompletableFuture<ResourceLocation> getOrLoad(MinecraftProfileTexture pTexture) {
         String $$1 = pTexture.getHash();
         CompletableFuture<ResourceLocation> $$2 = this.textures.get($$1);
         if ($$2 == null) {
            $$2 = this.registerTexture(pTexture);
            this.textures.put($$1, $$2);
         }

         return $$2;
      }

      private CompletableFuture<ResourceLocation> registerTexture(MinecraftProfileTexture pTexture) {
         String $$1 = Hashing.sha1().hashUnencodedChars(pTexture.getHash()).toString();
         ResourceLocation $$2 = this.getTextureLocation($$1);
         Path $$3 = this.root.resolve($$1.length() > 2 ? $$1.substring(0, 2) : "xx").resolve($$1);
         CompletableFuture<ResourceLocation> $$4 = new CompletableFuture<>();
         HttpTexture $$5 = new HttpTexture(
            $$3.toFile(), pTexture.getUrl(), DefaultPlayerSkin.getDefaultTexture(), this.type == Type.SKIN, () -> $$4.complete($$2)
         );
         this.textureManager.register($$2, $$5);
         return $$4;
      }

      private ResourceLocation getTextureLocation(String pName) {
         String $$1 = switch (this.type) {
            case SKIN -> "skins";
            case CAPE -> "capes";
            case ELYTRA -> "elytra";
            default -> throw new MatchException(null, null);
         };
         return ResourceLocation.withDefaultNamespace($$1 + "/" + pName);
      }
   }
}
