package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExperimentsScreen extends Screen {
   private static final Component TITLE = Component.translatable("selectWorld.experiments");
   private static final Component INFO = Component.translatable("selectWorld.experiments.info").withStyle(ChatFormatting.RED);
   private static final int MAIN_CONTENT_WIDTH = 310;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final Screen parent;
   private final PackRepository packRepository;
   private final Consumer<PackRepository> output;
   private final Object2BooleanMap<Pack> packs = new Object2BooleanLinkedOpenHashMap();

   public ExperimentsScreen(Screen pParent, PackRepository pPackRepository, Consumer<PackRepository> pOutput) {
      super(TITLE);
      this.parent = pParent;
      this.packRepository = pPackRepository;
      this.output = pOutput;

      for (Pack $$3 : pPackRepository.getAvailablePacks()) {
         if ($$3.getPackSource() == PackSource.FEATURE) {
            this.packs.put($$3, pPackRepository.getSelectedPacks().contains($$3));
         }
      }
   }

   @Override
   protected void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      LinearLayout $$0 = this.layout.addToContents(LinearLayout.vertical());
      $$0.addChild(new MultiLineTextWidget(INFO, this.font).setMaxWidth(310), p_296222_ -> p_296222_.paddingBottom(15));
      SwitchGrid.Builder $$1 = SwitchGrid.builder(310).withInfoUnderneath(2, true).withRowSpacing(4);
      this.packs
         .forEach(
            (p_270880_, p_270874_) -> $$1.addSwitch(
                     getHumanReadableTitle(p_270880_), () -> this.packs.getBoolean(p_270880_), p_270491_ -> this.packs.put(p_270880_, p_270491_)
                  )
                  .withInfo(p_270880_.getDescription())
         );
      $$1.build($$0::addChild);
      LinearLayout $$2 = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      $$2.addChild(Button.builder(CommonComponents.GUI_DONE, p_270336_ -> this.onDone()).build());
      $$2.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_274702_ -> this.onClose()).build());
      this.layout.visitWidgets(p_325439_ -> {
         AbstractWidget var10000 = this.addRenderableWidget(p_325439_);
      });
      this.repositionElements();
   }

   private static Component getHumanReadableTitle(Pack pPack) {
      String $$1 = "dataPack." + pPack.getId() + ".name";
      return (Component)(I18n.exists($$1) ? Component.translatable($$1) : pPack.getTitle());
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), INFO);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   private void onDone() {
      List<Pack> $$0 = new ArrayList<>(this.packRepository.getSelectedPacks());
      List<Pack> $$1 = new ArrayList<>();
      this.packs.forEach((p_270540_, p_270780_) -> {
         $$0.remove(p_270540_);
         if (p_270780_) {
            $$1.add(p_270540_);
         }
      });
      $$0.addAll(Lists.reverse($$1));
      this.packRepository.setSelected($$0.stream().map(Pack::getId).toList());
      this.output.accept(this.packRepository);
   }
}
