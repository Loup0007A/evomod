package net.minecraft.world.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.ForgeHooks;

public class CreativeModeTab {
   static final ResourceLocation DEFAULT_BACKGROUND = createTextureLocation("items");
   private final Component displayName;
   ResourceLocation backgroundTexture = DEFAULT_BACKGROUND;
   boolean canScroll = true;
   boolean showTitle = true;
   boolean alignedRight = false;
   private final CreativeModeTab.Row row;
   private final int column;
   private final CreativeModeTab.Type type;
   @Nullable
   private ItemStack iconItemStack;
   private Collection<ItemStack> displayItems = ItemStackLinkedSet.createTypeAndComponentsSet();
   private Set<ItemStack> displayItemsSearchTab = ItemStackLinkedSet.createTypeAndComponentsSet();
   private final Supplier<ItemStack> iconGenerator;
   private final CreativeModeTab.DisplayItemsGenerator displayItemsGenerator;
   private final boolean hasSearchBar;
   private final int searchBarWidth;
   private final int labelColor;
   private final int slotColor;
   public final List<ResourceLocation> tabsBefore;
   public final List<ResourceLocation> tabsAfter;

   CreativeModeTab(
      CreativeModeTab.Row p_260217_,
      int p_259557_,
      CreativeModeTab.Type p_260176_,
      Component p_260100_,
      Supplier<ItemStack> p_259543_,
      CreativeModeTab.DisplayItemsGenerator p_259085_,
      CreativeModeTab.Builder builder
   ) {
      this.row = p_260217_;
      this.column = p_259557_;
      this.displayName = p_260100_;
      this.iconGenerator = p_259543_;
      this.displayItemsGenerator = p_259085_;
      this.type = p_260176_;
      this.hasSearchBar = builder.hasSearchBar;
      this.searchBarWidth = builder.searchBarWidth;
      this.labelColor = builder.labelColor;
      this.slotColor = builder.slotColor;
      this.tabsBefore = List.copyOf(builder.tabsBefore);
      this.tabsAfter = List.copyOf(builder.tabsAfter);
   }

   protected CreativeModeTab(CreativeModeTab.Builder builder) {
      this(builder.row, builder.column, builder.type, builder.displayName, builder.iconGenerator, builder.displayItemsGenerator, builder);
   }

   public static CreativeModeTab.Builder builder() {
      return new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 0);
   }

   public static ResourceLocation createTextureLocation(String pName) {
      return ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tab_" + pName + ".png");
   }

   /** @deprecated */
   public static CreativeModeTab.Builder builder(CreativeModeTab.Row pRow, int pColumn) {
      return new CreativeModeTab.Builder(pRow, pColumn);
   }

   public Component getDisplayName() {
      return this.displayName;
   }

   public ItemStack getIconItem() {
      if (this.iconItemStack == null) {
         this.iconItemStack = this.iconGenerator.get();
      }

      return this.iconItemStack;
   }

   public ResourceLocation getBackgroundTexture() {
      return this.backgroundTexture;
   }

   public boolean showTitle() {
      return this.showTitle;
   }

   public boolean canScroll() {
      return this.canScroll;
   }

   public int column() {
      return this.column;
   }

   public CreativeModeTab.Row row() {
      return this.row;
   }

   public boolean hasAnyItems() {
      return !this.displayItems.isEmpty();
   }

   public boolean shouldDisplay() {
      return this.type != CreativeModeTab.Type.CATEGORY || this.hasAnyItems();
   }

   public boolean isAlignedRight() {
      return this.alignedRight;
   }

   public CreativeModeTab.Type getType() {
      return this.type;
   }

   public void buildContents(CreativeModeTab.ItemDisplayParameters pParameters) {
      CreativeModeTab.ItemDisplayBuilder creativemodetab$itemdisplaybuilder = new CreativeModeTab.ItemDisplayBuilder(this, pParameters.enabledFeatures);
      ResourceKey<CreativeModeTab> resourcekey = BuiltInRegistries.CREATIVE_MODE_TAB
         .getResourceKey(this)
         .orElseThrow(() -> new IllegalStateException("Unregistered creative tab: " + this));
      ForgeHooks.onCreativeModeTabBuildContents(this, resourcekey, this.displayItemsGenerator, pParameters, creativemodetab$itemdisplaybuilder);
      this.displayItems = creativemodetab$itemdisplaybuilder.tabContents;
      this.displayItemsSearchTab = creativemodetab$itemdisplaybuilder.searchTabContents;
   }

   public Collection<ItemStack> getDisplayItems() {
      return this.displayItems;
   }

   public Collection<ItemStack> getSearchTabDisplayItems() {
      return this.displayItemsSearchTab;
   }

   public boolean contains(ItemStack pStack) {
      return this.displayItemsSearchTab.contains(pStack);
   }

   public boolean hasSearchBar() {
      return this.hasSearchBar;
   }

   public int getSearchBarWidth() {
      return this.searchBarWidth;
   }

   public int getLabelColor() {
      return this.labelColor;
   }

   public int getSlotColor() {
      return this.slotColor;
   }

   public static class Builder {
      private static final CreativeModeTab.DisplayItemsGenerator EMPTY_GENERATOR = (p_270422_, p_259433_) -> {
      };
      private final CreativeModeTab.Row row;
      private final int column;
      private Component displayName = Component.empty();
      private Supplier<ItemStack> iconGenerator = () -> ItemStack.EMPTY;
      private CreativeModeTab.DisplayItemsGenerator displayItemsGenerator = EMPTY_GENERATOR;
      private boolean canScroll = true;
      private boolean showTitle = true;
      private boolean alignedRight = false;
      private CreativeModeTab.Type type = CreativeModeTab.Type.CATEGORY;
      private ResourceLocation backgroundTexture = CreativeModeTab.DEFAULT_BACKGROUND;
      private boolean hasSearchBar = false;
      private int searchBarWidth = 89;
      private int labelColor = 4210752;
      private int slotColor = -2130706433;
      private Function<CreativeModeTab.Builder, CreativeModeTab> tabFactory = builder -> new CreativeModeTab(
            builder.row, builder.column, builder.type, builder.displayName, builder.iconGenerator, builder.displayItemsGenerator, builder
         );
      private final List<ResourceLocation> tabsBefore = new ArrayList<>();
      private final List<ResourceLocation> tabsAfter = new ArrayList<>();

      public Builder(CreativeModeTab.Row pRow, int pColumn) {
         this.row = pRow;
         this.column = pColumn;
      }

      public CreativeModeTab.Builder title(Component pTitle) {
         this.displayName = pTitle;
         return this;
      }

      public CreativeModeTab.Builder icon(Supplier<ItemStack> pIcon) {
         this.iconGenerator = pIcon;
         return this;
      }

      public CreativeModeTab.Builder displayItems(CreativeModeTab.DisplayItemsGenerator pDisplayItemsGenerator) {
         this.displayItemsGenerator = pDisplayItemsGenerator;
         return this;
      }

      public CreativeModeTab.Builder alignedRight() {
         this.alignedRight = true;
         return this;
      }

      public CreativeModeTab.Builder hideTitle() {
         this.showTitle = false;
         return this;
      }

      public CreativeModeTab.Builder noScrollBar() {
         this.canScroll = false;
         return this;
      }

      protected CreativeModeTab.Builder type(CreativeModeTab.Type pType) {
         this.type = pType;
         return pType == CreativeModeTab.Type.SEARCH ? this.withSearchBar() : this;
      }

      public CreativeModeTab.Builder backgroundTexture(ResourceLocation pBackgroundTexture) {
         this.backgroundTexture = pBackgroundTexture;
         return this;
      }

      public CreativeModeTab.Builder withSearchBar() {
         this.hasSearchBar = true;
         return this.backgroundTexture == CreativeModeTab.DEFAULT_BACKGROUND
            ? this.backgroundTexture(CreativeModeTab.createTextureLocation("item_search"))
            : this;
      }

      public CreativeModeTab.Builder withSearchBar(int searchBarWidth) {
         this.searchBarWidth = searchBarWidth;
         return this.withSearchBar();
      }

      public CreativeModeTab.Builder withLabelColor(int labelColor) {
         this.labelColor = labelColor;
         return this;
      }

      public CreativeModeTab.Builder withSlotColor(int slotColor) {
         this.slotColor = slotColor;
         return this;
      }

      public CreativeModeTab.Builder withTabFactory(Function<CreativeModeTab.Builder, CreativeModeTab> tabFactory) {
         this.tabFactory = tabFactory;
         return this;
      }

      public CreativeModeTab.Builder withTabsBefore(ResourceLocation... tabs) {
         this.tabsBefore.addAll(List.of(tabs));
         return this;
      }

      public CreativeModeTab.Builder withTabsAfter(ResourceLocation... tabs) {
         this.tabsAfter.addAll(List.of(tabs));
         return this;
      }

      @SafeVarargs
      public final CreativeModeTab.Builder withTabsBefore(ResourceKey<CreativeModeTab>... tabs) {
         Stream.of(tabs).map(ResourceKey::location).forEach(this.tabsBefore::add);
         return this;
      }

      @SafeVarargs
      public final CreativeModeTab.Builder withTabsAfter(ResourceKey<CreativeModeTab>... tabs) {
         Stream.of(tabs).map(ResourceKey::location).forEach(this.tabsAfter::add);
         return this;
      }

      public CreativeModeTab build() {
         if ((this.type == CreativeModeTab.Type.HOTBAR || this.type == CreativeModeTab.Type.INVENTORY) && this.displayItemsGenerator != EMPTY_GENERATOR) {
            throw new IllegalStateException("Special tabs can't have display items");
         } else {
            CreativeModeTab creativemodetab = this.tabFactory.apply(this);
            creativemodetab.alignedRight = this.alignedRight;
            creativemodetab.showTitle = this.showTitle;
            creativemodetab.canScroll = this.canScroll;
            creativemodetab.backgroundTexture = this.backgroundTexture;
            return creativemodetab;
         }
      }
   }

   @FunctionalInterface
   public interface DisplayItemsGenerator {
      void accept(CreativeModeTab.ItemDisplayParameters pParameters, CreativeModeTab.Output pOutput);
   }

   static class ItemDisplayBuilder implements CreativeModeTab.Output {
      public final Collection<ItemStack> tabContents = ItemStackLinkedSet.createTypeAndComponentsSet();
      public final Set<ItemStack> searchTabContents = ItemStackLinkedSet.createTypeAndComponentsSet();
      private final CreativeModeTab tab;
      private final FeatureFlagSet featureFlagSet;

      public ItemDisplayBuilder(CreativeModeTab pTab, FeatureFlagSet pFeatureFlagSet) {
         this.tab = pTab;
         this.featureFlagSet = pFeatureFlagSet;
      }

      @Override
      public void accept(ItemStack pStack, CreativeModeTab.TabVisibility pTabVisibility) {
         if (pStack.getCount() != 1) {
            throw new IllegalArgumentException("Stack size must be exactly 1");
         } else {
            boolean flag = this.tabContents.contains(pStack) && pTabVisibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY;
            if (flag) {
               throw new IllegalStateException(
                  "Accidentally adding the same item stack twice "
                     + pStack.getDisplayName().getString()
                     + " to a Creative Mode Tab: "
                     + this.tab.getDisplayName().getString()
               );
            } else {
               if (pStack.getItem().isEnabled(this.featureFlagSet)) {
                  switch (pTabVisibility) {
                     case PARENT_AND_SEARCH_TABS:
                        this.tabContents.add(pStack);
                        this.searchTabContents.add(pStack);
                        break;
                     case PARENT_TAB_ONLY:
                        this.tabContents.add(pStack);
                        break;
                     case SEARCH_TAB_ONLY:
                        this.searchTabContents.add(pStack);
                  }
               }
            }
         }
      }
   }

   public static record ItemDisplayParameters(FeatureFlagSet enabledFeatures, boolean hasPermissions, HolderLookup.Provider holders) {
      public boolean needsUpdate(FeatureFlagSet pEnabledFeatures, boolean pHasPermissions, HolderLookup.Provider pHolders) {
         return !this.enabledFeatures.equals(pEnabledFeatures) || this.hasPermissions != pHasPermissions || this.holders != pHolders;
      }
   }

   public interface Output {
      void accept(ItemStack pStack, CreativeModeTab.TabVisibility pTabVisibility);

      default void accept(ItemStack pStack) {
         this.accept(pStack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
      }

      default void accept(ItemLike pItem, CreativeModeTab.TabVisibility pTabVisibility) {
         this.accept(new ItemStack(pItem), pTabVisibility);
      }

      default void accept(ItemLike pItem) {
         this.accept(new ItemStack(pItem), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
      }

      default void acceptAll(Collection<ItemStack> pStacks, CreativeModeTab.TabVisibility pTabVisibility) {
         pStacks.forEach(p_252337_ -> this.accept(p_252337_, pTabVisibility));
      }

      default void acceptAll(Collection<ItemStack> pStacks) {
         this.acceptAll(pStacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
      }
   }

   public static enum Row {
      TOP,
      BOTTOM;
   }

   public static enum TabVisibility {
      PARENT_AND_SEARCH_TABS,
      PARENT_TAB_ONLY,
      SEARCH_TAB_ONLY;
   }

   public static enum Type {
      CATEGORY,
      INVENTORY,
      HOTBAR,
      SEARCH;
   }
}
