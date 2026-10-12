package net.minecraft.client.gui.screens;

import com.ibm.icu.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreateBuffetWorldScreen extends Screen {
   private static final Component BIOME_SELECT_INFO = Component.translatable("createWorld.customize.buffet.biome").withColor(-8355712);
   private static final int SPACING = 8;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final Screen parent;
   private final Consumer<Holder<Biome>> applySettings;
   final Registry<Biome> biomes;
   private CreateBuffetWorldScreen.BiomeList list;
   Holder<Biome> biome;
   private Button doneButton;

   public CreateBuffetWorldScreen(Screen pParent, WorldCreationContext pContext, Consumer<Holder<Biome>> pApplySettings) {
      super(Component.translatable("createWorld.customize.buffet.title"));
      this.parent = pParent;
      this.applySettings = pApplySettings;
      this.biomes = pContext.worldgenLoadContext().registryOrThrow(Registries.BIOME);
      Holder<Biome> $$3 = this.biomes.getHolder(Biomes.PLAINS).or(() -> this.biomes.holders().findAny()).orElseThrow();
      this.biome = pContext.selectedDimensions().overworld().getBiomeSource().possibleBiomes().stream().findFirst().orElse($$3);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @Override
   protected void init() {
      LinearLayout $$0 = this.layout.addToHeader(LinearLayout.vertical().spacing(8));
      $$0.defaultCellSetting().alignHorizontallyCenter();
      $$0.addChild(new StringWidget(this.getTitle(), this.font));
      $$0.addChild(new StringWidget(BIOME_SELECT_INFO, this.font));
      this.list = this.layout.addToContents(new CreateBuffetWorldScreen.BiomeList());
      LinearLayout $$1 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      this.doneButton = $$1.addChild(Button.builder(CommonComponents.GUI_DONE, p_325363_ -> {
         this.applySettings.accept(this.biome);
         this.onClose();
      }).build());
      $$1.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_325364_ -> this.onClose()).build());
      this.list.setSelected(this.list.children().stream().filter(p_232738_ -> Objects.equals(p_232738_.biome, this.biome)).findFirst().orElse(null));
      this.layout.visitWidgets(this::addRenderableWidget);
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      this.list.updateSize(this.width, this.layout);
   }

   void updateButtonValidity() {
      this.doneButton.active = this.list.getSelected() != null;
   }

   @OnlyIn(Dist.CLIENT)
   class BiomeList extends ObjectSelectionList<CreateBuffetWorldScreen.BiomeList.Entry> {
      BiomeList() {
         super(CreateBuffetWorldScreen.this.minecraft, CreateBuffetWorldScreen.this.width, CreateBuffetWorldScreen.this.height - 77, 40, 16);
         Collator $$0 = Collator.getInstance(Locale.getDefault());
         CreateBuffetWorldScreen.this.biomes
            .holders()
            .map(p_205389_ -> new CreateBuffetWorldScreen.BiomeList.Entry((Holder.Reference<Biome>)p_205389_))
            .sorted(Comparator.comparing(p_203142_ -> p_203142_.name.getString(), $$0))
            .forEach(p_203138_ -> this.addEntry(p_203138_));
      }

      public void setSelected(@Nullable CreateBuffetWorldScreen.BiomeList.Entry pEntry) {
         super.setSelected(pEntry);
         if (pEntry != null) {
            CreateBuffetWorldScreen.this.biome = pEntry.biome;
         }

         CreateBuffetWorldScreen.this.updateButtonValidity();
      }

      @OnlyIn(Dist.CLIENT)
      class Entry extends ObjectSelectionList.Entry<CreateBuffetWorldScreen.BiomeList.Entry> {
         final Holder.Reference<Biome> biome;
         final Component name;

         public Entry(final Holder.Reference<Biome> pBiome) {
            this.biome = pBiome;
            ResourceLocation $$1 = pBiome.key().location();
            String $$2 = $$1.toLanguageKey("biome");
            if (Language.getInstance().has($$2)) {
               this.name = Component.translatable($$2);
            } else {
               this.name = Component.literal($$1.toString());
            }
         }

         @Override
         public Component getNarration() {
            return Component.translatable("narrator.select", this.name);
         }

         @Override
         public void render(
            GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
         ) {
            pGuiGraphics.drawString(CreateBuffetWorldScreen.this.font, this.name, pLeft + 5, pTop + 2, 16777215);
         }

         @Override
         public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            BiomeList.this.setSelected(this);
            return super.mouseClicked(pMouseX, pMouseY, pButton);
         }
      }
   }
}
