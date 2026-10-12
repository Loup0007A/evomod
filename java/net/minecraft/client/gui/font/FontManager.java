package net.minecraft.client.gui.font;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.DependencySorter;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class FontManager implements PreparableReloadListener, AutoCloseable {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String FONTS_PATH = "fonts.json";
   public static final ResourceLocation MISSING_FONT = ResourceLocation.withDefaultNamespace("missing");
   private static final FileToIdConverter FONT_DEFINITIONS = FileToIdConverter.json("font");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private final FontSet missingFontSet;
   private final List<GlyphProvider> providersToClose = new ArrayList<>();
   private final Map<ResourceLocation, FontSet> fontSets = new HashMap<>();
   private final TextureManager textureManager;
   @Nullable
   private volatile FontSet lastFontSetCache;

   public FontManager(TextureManager pTextureManager) {
      this.textureManager = pTextureManager;
      this.missingFontSet = Util.make(new FontSet(pTextureManager, MISSING_FONT), p_325351_ -> p_325351_.reload(List.of(createFallbackProvider()), Set.of()));
   }

   private static GlyphProvider.Conditional createFallbackProvider() {
      return new GlyphProvider.Conditional(new AllMissingGlyphProvider(), FontOption.Filter.ALWAYS_PASS);
   }

   @Override
   public CompletableFuture<Void> reload(
      PreparableReloadListener.PreparationBarrier pPreparationBarrier,
      ResourceManager pResourceManager,
      ProfilerFiller pPreparationsProfiler,
      ProfilerFiller pReloadProfiler,
      Executor pBackgroundExecutor,
      Executor pGameExecutor
   ) {
      pPreparationsProfiler.startTick();
      pPreparationsProfiler.endTick();
      return this.prepare(pResourceManager, pBackgroundExecutor)
         .thenCompose(pPreparationBarrier::wait)
         .thenAcceptAsync(p_284609_ -> this.apply(p_284609_, pReloadProfiler), pGameExecutor);
   }

   private CompletableFuture<FontManager.Preparation> prepare(ResourceManager pResourceManager, Executor pExecutor) {
      List<CompletableFuture<FontManager.UnresolvedBuilderBundle>> $$2 = new ArrayList<>();

      for (Entry<ResourceLocation, List<Resource>> $$3 : FONT_DEFINITIONS.listMatchingResourceStacks(pResourceManager).entrySet()) {
         ResourceLocation $$4 = FONT_DEFINITIONS.fileToId($$3.getKey());
         $$2.add(CompletableFuture.supplyAsync(() -> {
            List<Pair<FontManager.BuilderId, GlyphProviderDefinition.Conditional>> $$4x = loadResourceStack($$3.getValue(), $$4);
            FontManager.UnresolvedBuilderBundle $$5 = new FontManager.UnresolvedBuilderBundle($$4);

            for (Pair<FontManager.BuilderId, GlyphProviderDefinition.Conditional> $$6 : $$4x) {
               FontManager.BuilderId $$7 = (FontManager.BuilderId)$$6.getFirst();
               FontOption.Filter $$8 = ((GlyphProviderDefinition.Conditional)$$6.getSecond()).filter();
               ((GlyphProviderDefinition.Conditional)$$6.getSecond()).definition().unpack().ifLeft(p_325337_ -> {
                  CompletableFuture<Optional<GlyphProvider>> $$6x = this.safeLoad($$7, p_325337_, pResourceManager, pExecutor);
                  $$5.add($$7, $$8, $$6x);
               }).ifRight(p_325345_ -> $$5.add($$7, $$8, p_325345_));
            }

            return $$5;
         }, pExecutor));
      }

      return Util.sequence($$2)
         .thenCompose(
            p_325341_ -> {
               List<CompletableFuture<Optional<GlyphProvider>>> $$2x = p_325341_.stream()
                  .flatMap(FontManager.UnresolvedBuilderBundle::listBuilders)
                  .collect(Util.toMutableList());
               GlyphProvider.Conditional $$3x = createFallbackProvider();
               $$2x.add(CompletableFuture.completedFuture(Optional.of($$3x.provider())));
               return Util.sequence($$2x)
                  .thenCompose(
                     p_284618_ -> {
                        Map<ResourceLocation, List<GlyphProvider.Conditional>> $$4x = this.resolveProviders(p_325341_);
                        CompletableFuture<?>[] $$5 = $$4x.values()
                           .stream()
                           .map(p_284585_ -> CompletableFuture.runAsync(() -> this.finalizeProviderLoading(p_284585_, $$3x), pExecutor))
                           .toArray(CompletableFuture[]::new);
                        return CompletableFuture.allOf($$5).thenApply(p_284595_ -> {
                           List<GlyphProvider> $$3xx = p_284618_.stream().flatMap(Optional::stream).toList();
                           return new FontManager.Preparation($$4x, $$3xx);
                        });
                     }
                  );
            }
         );
   }

   private CompletableFuture<Optional<GlyphProvider>> safeLoad(
      FontManager.BuilderId pBuilderId, GlyphProviderDefinition.Loader pLoader, ResourceManager pResourceManager, Executor pExecutor
   ) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            return Optional.of(pLoader.load(pResourceManager));
         } catch (Exception var4x) {
            LOGGER.warn("Failed to load builder {}, rejecting", pBuilderId, var4x);
            return Optional.empty();
         }
      }, pExecutor);
   }

   private Map<ResourceLocation, List<GlyphProvider.Conditional>> resolveProviders(List<FontManager.UnresolvedBuilderBundle> pUnresolvedBuilderBundles) {
      Map<ResourceLocation, List<GlyphProvider.Conditional>> $$1 = new HashMap<>();
      DependencySorter<ResourceLocation, FontManager.UnresolvedBuilderBundle> $$2 = new DependencySorter<>();
      pUnresolvedBuilderBundles.forEach(p_284626_ -> $$2.addEntry(p_284626_.fontId, p_284626_));
      $$2.orderByDependencies(
         (p_284620_, p_284621_) -> p_284621_.resolve($$1::get).ifPresent(p_284590_ -> $$1.put(p_284620_, (List<GlyphProvider.Conditional>)p_284590_))
      );
      return $$1;
   }

   private void finalizeProviderLoading(List<GlyphProvider.Conditional> pProviders, GlyphProvider.Conditional pFallbackProvider) {
      pProviders.add(0, pFallbackProvider);
      IntSet $$2 = new IntOpenHashSet();

      for (GlyphProvider.Conditional $$3 : pProviders) {
         $$2.addAll($$3.provider().getSupportedGlyphs());
      }

      $$2.forEach(p_325339_ -> {
         if (p_325339_ != 32) {
            for (GlyphProvider.Conditional $$2x : Lists.reverse(pProviders)) {
               if ($$2x.provider().getGlyph(p_325339_) != null) {
                  break;
               }
            }
         }
      });
   }

   private static Set<FontOption> getFontOptions(Options pOptions) {
      Set<FontOption> $$1 = EnumSet.noneOf(FontOption.class);
      if (pOptions.forceUnicodeFont().get()) {
         $$1.add(FontOption.UNIFORM);
      }

      if (pOptions.japaneseGlyphVariants().get()) {
         $$1.add(FontOption.JAPANESE_VARIANTS);
      }

      return $$1;
   }

   private void apply(FontManager.Preparation pPreperation, ProfilerFiller pProfiler) {
      pProfiler.startTick();
      pProfiler.push("closing");
      this.lastFontSetCache = null;
      this.fontSets.values().forEach(FontSet::close);
      this.fontSets.clear();
      this.providersToClose.forEach(GlyphProvider::close);
      this.providersToClose.clear();
      Set<FontOption> $$2 = getFontOptions(Minecraft.getInstance().options);
      pProfiler.popPush("reloading");
      pPreperation.fontSets().forEach((p_325349_, p_325350_) -> {
         FontSet $$3 = new FontSet(this.textureManager, p_325349_);
         $$3.reload(Lists.reverse(p_325350_), $$2);
         this.fontSets.put(p_325349_, $$3);
      });
      this.providersToClose.addAll(pPreperation.allProviders);
      pProfiler.pop();
      pProfiler.endTick();
      if (!this.fontSets.containsKey(Minecraft.DEFAULT_FONT)) {
         throw new IllegalStateException("Default font failed to load");
      }
   }

   public void updateOptions(Options pOptions) {
      Set<FontOption> $$1 = getFontOptions(pOptions);

      for (FontSet $$2 : this.fontSets.values()) {
         $$2.reload($$1);
      }
   }

   private static List<Pair<FontManager.BuilderId, GlyphProviderDefinition.Conditional>> loadResourceStack(List<Resource> pResources, ResourceLocation pFontId) {
      List<Pair<FontManager.BuilderId, GlyphProviderDefinition.Conditional>> $$2 = new ArrayList<>();

      for (Resource $$3 : pResources) {
         try (Reader $$4 = $$3.openAsReader()) {
            JsonElement $$5 = (JsonElement)GSON.fromJson($$4, JsonElement.class);
            FontManager.FontDefinitionFile $$6 = (FontManager.FontDefinitionFile)FontManager.FontDefinitionFile.CODEC
               .parse(JsonOps.INSTANCE, $$5)
               .getOrThrow(JsonParseException::new);
            List<GlyphProviderDefinition.Conditional> $$7 = $$6.providers;

            for (int $$8 = $$7.size() - 1; $$8 >= 0; $$8--) {
               FontManager.BuilderId $$9 = new FontManager.BuilderId(pFontId, $$3.sourcePackId(), $$8);
               $$2.add(Pair.of($$9, $$7.get($$8)));
            }
         } catch (Exception var13) {
            LOGGER.warn("Unable to load font '{}' in {} in resourcepack: '{}'", new Object[]{pFontId, "fonts.json", $$3.sourcePackId(), var13});
         }
      }

      return $$2;
   }

   public Font createFont() {
      return new Font(this::getFontSetCached, false);
   }

   public Font createFontFilterFishy() {
      return new Font(this::getFontSetCached, true);
   }

   private FontSet getFontSetRaw(ResourceLocation pFontSet) {
      return this.fontSets.getOrDefault(pFontSet, this.missingFontSet);
   }

   private FontSet getFontSetCached(ResourceLocation p_332431_) {
      FontSet $$1 = this.lastFontSetCache;
      if ($$1 != null && p_332431_.equals($$1.name())) {
         return $$1;
      } else {
         FontSet $$2 = this.getFontSetRaw(p_332431_);
         this.lastFontSetCache = $$2;
         return $$2;
      }
   }

   @Override
   public void close() {
      this.fontSets.values().forEach(FontSet::close);
      this.providersToClose.forEach(GlyphProvider::close);
      this.missingFontSet.close();
   }

   @OnlyIn(Dist.CLIENT)
   static record BuilderId(ResourceLocation fontId, String pack, int index) {
      @Override
      public String toString() {
         return "(" + this.fontId + ": builder #" + this.index + " from pack " + this.pack + ")";
      }
   }

   @OnlyIn(Dist.CLIENT)
   static record BuilderResult(FontManager.BuilderId id, FontOption.Filter filter, Either<CompletableFuture<Optional<GlyphProvider>>, ResourceLocation> result) {
      public Optional<List<GlyphProvider.Conditional>> resolve(Function<ResourceLocation, List<GlyphProvider.Conditional>> pProviderResolver) {
         return (Optional<List<GlyphProvider.Conditional>>)this.result
            .map(
               p_325356_ -> ((Optional)p_325356_.join()).map(p_325357_ -> List.of(new GlyphProvider.Conditional(p_325357_, this.filter))),
               p_325359_ -> {
                  List<GlyphProvider.Conditional> $$2 = pProviderResolver.apply(p_325359_);
                  if ($$2 == null) {
                     FontManager.LOGGER
                        .warn(
                           "Can't find font {} referenced by builder {}, either because it's missing, failed to load or is part of loading cycle",
                           p_325359_,
                           this.id
                        );
                     return Optional.empty();
                  } else {
                     return Optional.of($$2.stream().map(this::mergeFilters).toList());
                  }
               }
            );
      }

      private GlyphProvider.Conditional mergeFilters(GlyphProvider.Conditional p_330532_) {
         return new GlyphProvider.Conditional(p_330532_.provider(), this.filter.merge(p_330532_.filter()));
      }
   }

   @OnlyIn(Dist.CLIENT)
   static record FontDefinitionFile(List<GlyphProviderDefinition.Conditional> providers) {
      public static final Codec<FontManager.FontDefinitionFile> CODEC = RecordCodecBuilder.create(
         p_325360_ -> p_325360_.group(
                  GlyphProviderDefinition.Conditional.CODEC.listOf().fieldOf("providers").forGetter(FontManager.FontDefinitionFile::providers)
               )
               .apply(p_325360_, FontManager.FontDefinitionFile::new)
      );
   }

   @OnlyIn(Dist.CLIENT)
   static record Preparation(Map<ResourceLocation, List<GlyphProvider.Conditional>> fontSets, List<GlyphProvider> allProviders) {
   }

   @OnlyIn(Dist.CLIENT)
   static record UnresolvedBuilderBundle(ResourceLocation fontId, List<FontManager.BuilderResult> builders, Set<ResourceLocation> dependencies)
      implements DependencySorter.Entry<ResourceLocation> {
      public UnresolvedBuilderBundle(ResourceLocation pFontId) {
         this(pFontId, new ArrayList<>(), new HashSet<>());
      }

      public void add(FontManager.BuilderId pBuilderId, FontOption.Filter pFilter, GlyphProviderDefinition.Reference pGlyphProvider) {
         this.builders.add(new FontManager.BuilderResult(pBuilderId, pFilter, Either.right(pGlyphProvider.id())));
         this.dependencies.add(pGlyphProvider.id());
      }

      public void add(FontManager.BuilderId pBuilderId, FontOption.Filter pFilter, CompletableFuture<Optional<GlyphProvider>> pGlyphProvider) {
         this.builders.add(new FontManager.BuilderResult(pBuilderId, pFilter, Either.left(pGlyphProvider)));
      }

      private Stream<CompletableFuture<Optional<GlyphProvider>>> listBuilders() {
         return this.builders.stream().flatMap(p_285041_ -> p_285041_.result.left().stream());
      }

      public Optional<List<GlyphProvider.Conditional>> resolve(Function<ResourceLocation, List<GlyphProvider.Conditional>> pProviderResolver) {
         List<GlyphProvider.Conditional> $$1 = new ArrayList<>();

         for (FontManager.BuilderResult $$2 : this.builders) {
            Optional<List<GlyphProvider.Conditional>> $$3 = $$2.resolve(pProviderResolver);
            if (!$$3.isPresent()) {
               return Optional.empty();
            }

            $$1.addAll($$3.get());
         }

         return Optional.of($$1);
      }

      @Override
      public void visitRequiredDependencies(Consumer<ResourceLocation> pVisitor) {
         this.dependencies.forEach(pVisitor);
      }

      @Override
      public void visitOptionalDependencies(Consumer<ResourceLocation> pVisitor) {
      }
   }
}
