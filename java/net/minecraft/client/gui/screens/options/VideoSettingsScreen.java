package net.minecraft.client.gui.screens.options;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VideoSettingsScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("options.videoTitle");
   private static final Component FABULOUS = Component.translatable("options.graphics.fabulous").withStyle(ChatFormatting.ITALIC);
   private static final Component WARNING_MESSAGE = Component.translatable("options.graphics.warning.message", FABULOUS, FABULOUS);
   private static final Component WARNING_TITLE = Component.translatable("options.graphics.warning.title").withStyle(ChatFormatting.RED);
   private static final Component BUTTON_ACCEPT = Component.translatable("options.graphics.warning.accept");
   private static final Component BUTTON_CANCEL = Component.translatable("options.graphics.warning.cancel");
   private final GpuWarnlistManager gpuWarnlistManager;
   private final int oldMipmaps;

   private static OptionInstance<?>[] options(Options pOptions) {
      return new OptionInstance[]{
         pOptions.graphicsMode(),
         pOptions.renderDistance(),
         pOptions.prioritizeChunkUpdates(),
         pOptions.simulationDistance(),
         pOptions.ambientOcclusion(),
         pOptions.framerateLimit(),
         pOptions.enableVsync(),
         pOptions.bobView(),
         pOptions.guiScale(),
         pOptions.attackIndicator(),
         pOptions.gamma(),
         pOptions.cloudStatus(),
         pOptions.fullscreen(),
         pOptions.particles(),
         pOptions.mipmapLevels(),
         pOptions.entityShadows(),
         pOptions.screenEffectScale(),
         pOptions.entityDistanceScaling(),
         pOptions.fovEffectScale(),
         pOptions.showAutosaveIndicator(),
         pOptions.glintSpeed(),
         pOptions.glintStrength(),
         pOptions.menuBackgroundBlurriness()
      };
   }

   public VideoSettingsScreen(Screen pLastScreen, Minecraft pMinecraft, Options pOptions) {
      super(pLastScreen, pOptions, TITLE);
      this.gpuWarnlistManager = pMinecraft.getGpuWarnlistManager();
      this.gpuWarnlistManager.resetWarnings();
      if (pOptions.graphicsMode().get() == GraphicsStatus.FABULOUS) {
         this.gpuWarnlistManager.dismissWarning();
      }

      this.oldMipmaps = pOptions.mipmapLevels().get();
   }

   @Override
   protected void addOptions() {
      int $$0 = -1;
      Window $$1 = this.minecraft.getWindow();
      Monitor $$2 = $$1.findBestMonitor();
      int $$3;
      if ($$2 == null) {
         $$3 = -1;
      } else {
         Optional<VideoMode> $$4 = $$1.getPreferredFullscreenVideoMode();
         $$3 = $$4.map($$2::getVideoModeIndex).orElse(-1);
      }

      OptionInstance<Integer> $$6 = new OptionInstance<>(
         "options.fullscreen.resolution",
         OptionInstance.noTooltip(),
         (p_344242_, p_344033_) -> {
            if ($$2 == null) {
               return Component.translatable("options.fullscreen.unavailable");
            } else if (p_344033_ == -1) {
               return Options.genericValueLabel(p_344242_, Component.translatable("options.fullscreen.current"));
            } else {
               VideoMode $$3x = $$2.getMode(p_344033_);
               return Options.genericValueLabel(
                  p_344242_,
                  Component.translatable(
                     "options.fullscreen.entry",
                     $$3x.getWidth(),
                     $$3x.getHeight(),
                     $$3x.getRefreshRate(),
                     $$3x.getRedBits() + $$3x.getGreenBits() + $$3x.getBlueBits()
                  )
               );
            }
         },
         new OptionInstance.IntRange(-1, $$2 != null ? $$2.getModeCount() - 1 : -1),
         $$3,
         p_345267_ -> {
            if ($$2 != null) {
               $$1.setPreferredFullscreenVideoMode(p_345267_ == -1 ? Optional.empty() : Optional.of($$2.getMode(p_345267_)));
            }
         }
      );
      this.list.addBig($$6);
      this.list.addBig(this.options.biomeBlendRadius());
      this.list.addSmall(options(this.options));
   }

   @Override
   public void onClose() {
      this.minecraft.getWindow().changeFullscreenVideoMode();
      super.onClose();
   }

   @Override
   public void removed() {
      if (this.options.mipmapLevels().get() != this.oldMipmaps) {
         this.minecraft.updateMaxMipLevel(this.options.mipmapLevels().get());
         this.minecraft.delayTextureReload();
      }

      super.removed();
   }

   @Override
   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (super.mouseClicked(pMouseX, pMouseY, pButton)) {
         if (this.gpuWarnlistManager.isShowingWarning()) {
            List<Component> $$3 = Lists.newArrayList(new Component[]{WARNING_MESSAGE, CommonComponents.NEW_LINE});
            String $$4 = this.gpuWarnlistManager.getRendererWarnings();
            if ($$4 != null) {
               $$3.add(CommonComponents.NEW_LINE);
               $$3.add(Component.translatable("options.graphics.warning.renderer", $$4).withStyle(ChatFormatting.GRAY));
            }

            String $$5 = this.gpuWarnlistManager.getVendorWarnings();
            if ($$5 != null) {
               $$3.add(CommonComponents.NEW_LINE);
               $$3.add(Component.translatable("options.graphics.warning.vendor", $$5).withStyle(ChatFormatting.GRAY));
            }

            String $$6 = this.gpuWarnlistManager.getVersionWarnings();
            if ($$6 != null) {
               $$3.add(CommonComponents.NEW_LINE);
               $$3.add(Component.translatable("options.graphics.warning.version", $$6).withStyle(ChatFormatting.GRAY));
            }

            this.minecraft
               .setScreen(
                  new UnsupportedGraphicsWarningScreen(
                     WARNING_TITLE, $$3, ImmutableList.of(new UnsupportedGraphicsWarningScreen.ButtonOption(BUTTON_ACCEPT, p_343553_ -> {
                        this.options.graphicsMode().set(GraphicsStatus.FABULOUS);
                        Minecraft.getInstance().levelRenderer.allChanged();
                        this.gpuWarnlistManager.dismissWarning();
                        this.minecraft.setScreen(this);
                     }), new UnsupportedGraphicsWarningScreen.ButtonOption(BUTTON_CANCEL, p_342805_ -> {
                        this.gpuWarnlistManager.dismissWarningAndSkipFabulous();
                        this.minecraft.setScreen(this);
                     }))
                  )
               );
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
      if (Screen.hasControlDown()) {
         OptionInstance<Integer> $$4 = this.options.guiScale();
         if ($$4.values() instanceof OptionInstance.ClampingLazyMaxIntRange $$5) {
            int $$6 = $$4.get();
            int $$7 = $$6 == 0 ? $$5.maxInclusive() + 1 : $$6;
            int $$8 = $$7 + (int)Math.signum(pScrollY);
            if ($$8 != 0 && $$8 <= $$5.maxInclusive() && $$8 >= $$5.minInclusive()) {
               CycleButton<Integer> $$9 = (CycleButton<Integer>)this.list.findOption($$4);
               if ($$9 != null) {
                  $$4.set($$8);
                  $$9.setValue($$8);
                  this.list.setScrollAmount(0.0);
                  return true;
               }
            }
         }

         return false;
      } else {
         return super.mouseScrolled(pMouseX, pMouseY, pScrollX, pScrollY);
      }
   }
}
