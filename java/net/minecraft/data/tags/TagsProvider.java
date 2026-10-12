package net.minecraft.data.tags;

import com.google.common.collect.Maps;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.extensions.IForgeTagAppender;
import net.minecraftforge.registries.RegistryManager;
import org.jetbrains.annotations.Nullable;

public abstract class TagsProvider<T> implements DataProvider {
   protected final PackOutput.PathProvider pathProvider;
   private final CompletableFuture<HolderLookup.Provider> lookupProvider;
   private final CompletableFuture<Void> contentsDone = new CompletableFuture<>();
   private final CompletableFuture<TagsProvider.TagLookup<T>> parentProvider;
   protected final ResourceKey<? extends Registry<T>> registryKey;
   protected final Map<ResourceLocation, TagBuilder> builders = Maps.newLinkedHashMap();
   protected final String modId;
   @Nullable
   protected final ExistingFileHelper existingFileHelper;
   private final ExistingFileHelper.IResourceType resourceType;
   private final ExistingFileHelper.IResourceType elementResourceType;

   /** @deprecated */
   protected TagsProvider(PackOutput pOutput, ResourceKey<? extends Registry<T>> pRegistryKey, CompletableFuture<HolderLookup.Provider> pLookupProvider) {
      this(pOutput, pRegistryKey, pLookupProvider, "vanilla", null);
   }

   protected TagsProvider(
      PackOutput p_256596_,
      ResourceKey<? extends Registry<T>> p_255886_,
      CompletableFuture<HolderLookup.Provider> p_256513_,
      String modId,
      @Nullable ExistingFileHelper existingFileHelper
   ) {
      this(p_256596_, p_255886_, p_256513_, CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()), modId, existingFileHelper);
   }

   /** @deprecated */
   protected TagsProvider(
      PackOutput pOutput,
      ResourceKey<? extends Registry<T>> pRegistryKey,
      CompletableFuture<HolderLookup.Provider> pLookupProvider,
      CompletableFuture<TagsProvider.TagLookup<T>> pParentProvider
   ) {
      this(pOutput, pRegistryKey, pLookupProvider, pParentProvider, "vanilla", null);
   }

   protected TagsProvider(
      PackOutput p_275432_,
      ResourceKey<? extends Registry<T>> p_275476_,
      CompletableFuture<HolderLookup.Provider> p_275222_,
      CompletableFuture<TagsProvider.TagLookup<T>> p_275565_,
      String modId,
      @Nullable ExistingFileHelper existingFileHelper
   ) {
      this.pathProvider = p_275432_.createRegistryTagsPathProvider(p_275476_);
      this.registryKey = p_275476_;
      this.parentProvider = p_275565_;
      this.lookupProvider = p_275222_;
      this.modId = modId;
      this.existingFileHelper = existingFileHelper;
      this.resourceType = new ExistingFileHelper.ResourceType(PackType.SERVER_DATA, ".json", Registries.tagsDirPath(p_275476_));
      this.elementResourceType = new ExistingFileHelper.ResourceType(PackType.SERVER_DATA, ".json", Registries.elementsDirPath(p_275476_));
   }

   @Nullable
   protected Path getPath(ResourceLocation id) {
      return this.pathProvider.json(id);
   }

   @Override
   public String getName() {
      return "Tags for " + this.registryKey.location() + " mod id " + this.modId;
   }

   protected abstract void addTags(HolderLookup.Provider pProvider);

   @Override
   public CompletableFuture<?> run(CachedOutput pOutput) {
      record CombinedData<T>(HolderLookup.Provider contents, TagsProvider.TagLookup<T> parent) {
      }

      return this.createContentsProvider()
         .thenApply(p_275895_ -> {
            this.contentsDone.complete(null);
            return (HolderLookup.Provider)p_275895_;
         })
         .thenCombineAsync(
            this.parentProvider, (p_274778_, p_274779_) -> new CombinedData<>(p_274778_, (TagsProvider.TagLookup<T>)p_274779_), Util.backgroundExecutor()
         )
         .thenCompose(
            p_325926_ -> {
               HolderLookup.RegistryLookup<T> registrylookup = p_325926_.contents
                  .lookup(this.registryKey)
                  .orElseThrow(
                     () -> RegistryManager.ACTIVE.getRegistry(this.registryKey) != null
                           ? new IllegalStateException("Forge registry " + this.registryKey.location() + " does not have support for tags")
                           : new IllegalStateException("Registry " + this.registryKey.location() + " not found")
                  );
               Predicate<ResourceLocation> predicate = p_255496_ -> registrylookup.get(ResourceKey.create(this.registryKey, p_255496_)).isPresent();
               Predicate<ResourceLocation> predicate1 = p_274776_ -> this.builders.containsKey(p_274776_)
                     || p_325926_.parent.contains(TagKey.create(this.registryKey, p_274776_));
               return CompletableFuture.allOf(
                  this.builders
                     .entrySet()
                     .stream()
                     .map(
                        p_325931_ -> {
                           ResourceLocation resourcelocation = p_325931_.getKey();
                           TagBuilder tagbuilder = p_325931_.getValue();
                           List<TagEntry> list = tagbuilder.build();
                           List<TagEntry> list1 = Stream.concat(list.stream(), tagbuilder.getRemoveEntries())
                              .filter(p_274771_ -> !p_274771_.verifyIfPresent(predicate, predicate1))
                              .filter(this::missing)
                              .toList();
                           if (!list1.isEmpty()) {
                              throw new IllegalArgumentException(
                                 String.format(
                                    Locale.ROOT,
                                    "Couldn't define tag %s as it is missing following references: %s",
                                    resourcelocation,
                                    list1.stream().map(Objects::toString).collect(Collectors.joining(","))
                                 )
                              );
                           } else {
                              Path path = this.getPath(resourcelocation);
                              return path == null
                                 ? CompletableFuture.completedFuture(null)
                                 : DataProvider.saveStable(
                                    pOutput,
                                    p_325926_.contents,
                                    TagFile.CODEC,
                                    new TagFile(list, tagbuilder.isReplace(), tagbuilder.getRemoveEntries().toList()),
                                    path
                                 );
                           }
                        }
                     )
                     .toArray(CompletableFuture[]::new)
               );
            }
         );
   }

   protected TagsProvider.TagAppender<T> tag(TagKey<T> pTag) {
      TagBuilder tagbuilder = this.getOrCreateRawBuilder(pTag);
      return new TagsProvider.TagAppender<>(tagbuilder, this.modId);
   }

   protected TagBuilder getOrCreateRawBuilder(TagKey<T> pTag) {
      return this.builders.computeIfAbsent(pTag.location(), p_236442_ -> {
         if (this.existingFileHelper != null) {
            this.existingFileHelper.trackGenerated(p_236442_, this.resourceType);
         }

         return TagBuilder.create();
      });
   }

   public CompletableFuture<TagsProvider.TagLookup<T>> contentsGetter() {
      return this.contentsDone.thenApply(p_276016_ -> p_274772_ -> Optional.ofNullable(this.builders.get(p_274772_.location())));
   }

   protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
      return this.lookupProvider.thenApply(p_274768_ -> {
         this.builders.clear();
         this.addTags(p_274768_);
         return (HolderLookup.Provider)p_274768_;
      });
   }

   private boolean missing(TagEntry reference) {
      return !reference.isRequired()
         ? false
         : this.existingFileHelper == null
            || !this.existingFileHelper.exists(reference.getId(), reference.isTag() ? this.resourceType : this.elementResourceType);
   }

   public static class TagAppender<T> implements IForgeTagAppender<T> {
      private final TagBuilder builder;
      private final String modid;

      protected TagAppender(TagBuilder pBuilder) {
         this(pBuilder, "vanilla");
      }

      protected TagAppender(TagBuilder p_256426_, String modid) {
         this.builder = p_256426_;
         this.modid = modid;
      }

      public final TagsProvider.TagAppender<T> add(ResourceKey<T> pKey) {
         this.builder.addElement(pKey.location());
         return this;
      }

      @SafeVarargs
      public final TagsProvider.TagAppender<T> add(ResourceKey<T>... pKeys) {
         for (ResourceKey<T> resourcekey : pKeys) {
            this.builder.addElement(resourcekey.location());
         }

         return this;
      }

      public final TagsProvider.TagAppender<T> addAll(List<ResourceKey<T>> pKeys) {
         for (ResourceKey<T> resourcekey : pKeys) {
            this.builder.addElement(resourcekey.location());
         }

         return this;
      }

      public TagsProvider.TagAppender<T> addOptional(ResourceLocation pLocation) {
         this.builder.addOptionalElement(pLocation);
         return this;
      }

      public TagsProvider.TagAppender<T> addTag(TagKey<T> pTag) {
         this.builder.addTag(pTag.location());
         return this;
      }

      public TagsProvider.TagAppender<T> addOptionalTag(ResourceLocation pLocation) {
         this.builder.addOptionalTag(pLocation);
         return this;
      }

      public TagsProvider.TagAppender<T> add(TagEntry tag) {
         this.builder.add(tag);
         return this;
      }

      public TagBuilder getInternalBuilder() {
         return this.builder;
      }

      public String getModID() {
         return this.modid;
      }
   }

   @FunctionalInterface
   public interface TagLookup<T> extends Function<TagKey<T>, Optional<TagBuilder>> {
      static <T> TagsProvider.TagLookup<T> empty() {
         return p_275247_ -> Optional.empty();
      }

      default boolean contains(TagKey<T> pKey) {
         return this.apply(pKey).isPresent();
      }
   }
}
