package net.minecraft.client.gui.components.toasts;

import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AdvancementToast implements Toast {
   private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/advancement");
   public static final int DISPLAY_TIME = 5000;
   private final AdvancementHolder advancement;
   private boolean playedSound;

   public AdvancementToast(AdvancementHolder pAdvancement) {
      this.advancement = pAdvancement;
   }

   @Override
   public Toast.Visibility render(GuiGraphics pGuiGraphics, ToastComponent pToastComponent, long pTimeSinceLastVisible) {
      DisplayInfo $$3 = this.advancement.value().display().orElse(null);
      pGuiGraphics.blitSprite(BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
      if ($$3 != null) {
         List<FormattedCharSequence> $$4 = pToastComponent.getMinecraft().font.split($$3.getTitle(), 125);
         int $$5 = $$3.getType() == AdvancementType.CHALLENGE ? 16746751 : 16776960;
         if ($$4.size() == 1) {
            pGuiGraphics.drawString(pToastComponent.getMinecraft().font, $$3.getType().getDisplayName(), 30, 7, $$5 | 0xFF000000, false);
            pGuiGraphics.drawString(pToastComponent.getMinecraft().font, $$4.get(0), 30, 18, -1, false);
         } else {
            int $$6 = 1500;
            float $$7 = 300.0F;
            if (pTimeSinceLastVisible < 1500L) {
               int $$8 = Mth.floor(Mth.clamp((float)(1500L - pTimeSinceLastVisible) / 300.0F, 0.0F, 1.0F) * 255.0F) << 24 | 67108864;
               pGuiGraphics.drawString(pToastComponent.getMinecraft().font, $$3.getType().getDisplayName(), 30, 11, $$5 | $$8, false);
            } else {
               int $$9 = Mth.floor(Mth.clamp((float)(pTimeSinceLastVisible - 1500L) / 300.0F, 0.0F, 1.0F) * 252.0F) << 24 | 67108864;
               int $$10 = this.height() / 2 - $$4.size() * 9 / 2;

               for (FormattedCharSequence $$11 : $$4) {
                  pGuiGraphics.drawString(pToastComponent.getMinecraft().font, $$11, 30, $$10, 16777215 | $$9, false);
                  $$10 += 9;
               }
            }
         }

         if (!this.playedSound && pTimeSinceLastVisible > 0L) {
            this.playedSound = true;
            if ($$3.getType() == AdvancementType.CHALLENGE) {
               pToastComponent.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F));
            }
         }

         pGuiGraphics.renderFakeItem($$3.getIcon(), 8, 8);
         return (double)pTimeSinceLastVisible >= 5000.0 * pToastComponent.getNotificationDisplayTimeMultiplier()
            ? Toast.Visibility.HIDE
            : Toast.Visibility.SHOW;
      } else {
         return Toast.Visibility.HIDE;
      }
   }
}
