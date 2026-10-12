package net.minecraft.data.tags;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public abstract class ItemTagsProvider extends IntrinsicHolderTagsProvider<Item> {
   private final CompletableFuture<TagsProvider.TagLookup<Block>> blockTags;
   private final Map<TagKey<Block>, TagKey<Item>> tagsToCopy = new HashMap<>();

   public ItemTagsProvider(
      PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider, CompletableFuture<TagsProvider.TagLookup<Block>> pBlockTags
   ) {
      this(pOutput, pLookupProvider, pBlockTags, "vanilla", null);
   }

   public ItemTagsProvider(
      PackOutput p_275343_,
      CompletableFuture<HolderLookup.Provider> p_275729_,
      CompletableFuture<TagsProvider.TagLookup<Block>> p_275322_,
      String modId,
      @Nullable ExistingFileHelper existingFileHelper
   ) {
      super(p_275343_, Registries.ITEM, p_275729_, p_255790_ -> p_255790_.builtInRegistryHolder().key(), modId, existingFileHelper);
      this.blockTags = p_275322_;
   }

   public ItemTagsProvider(
      PackOutput pOutput,
      CompletableFuture<HolderLookup.Provider> pLookupProvider,
      CompletableFuture<TagsProvider.TagLookup<Item>> pParentProvider,
      CompletableFuture<TagsProvider.TagLookup<Block>> pBlockTags
   ) {
      this(pOutput, pLookupProvider, pParentProvider, pBlockTags, "vanilla", null);
   }

   public ItemTagsProvider(
      PackOutput p_275204_,
      CompletableFuture<HolderLookup.Provider> p_275194_,
      CompletableFuture<TagsProvider.TagLookup<Item>> p_275207_,
      CompletableFuture<TagsProvider.TagLookup<Block>> p_275634_,
      String modId,
      @Nullable ExistingFileHelper existingFileHelper
   ) {
      super(p_275204_, Registries.ITEM, p_275194_, p_275207_, p_274765_ -> p_274765_.builtInRegistryHolder().key(), modId, existingFileHelper);
      this.blockTags = p_275634_;
   }

   protected void copy(TagKey<Block> pBlockTag, TagKey<Item> pItemTag) {
      this.tagsToCopy.put(pBlockTag, pItemTag);
   }

   @Override
   protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
      return super.createContentsProvider().thenCombine(this.blockTags, (p_274766_, p_274767_) -> {
         this.tagsToCopy.forEach((p_274763_, p_274764_) -> {
            TagBuilder tagbuilder = this.getOrCreateRawBuilder((TagKey<Item>)p_274764_);
            Optional<TagBuilder> optional = p_274767_.apply((TagKey<? super TagKey<Block>>)p_274763_);
            optional.orElseThrow(() -> new IllegalStateException("Missing block tag " + p_274764_.location())).build().forEach(tagbuilder::add);
         });
         return (HolderLookup.Provider)p_274766_;
      });
   }
}
