package net.minecraft.client.gui.components;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CommonButtons {
   public static SpriteIconButton language(int pWidth, Button.OnPress pOnPress, boolean pIconOnly) {
      return SpriteIconButton.builder(Component.translatable("options.language"), pOnPress, pIconOnly)
         .width(pWidth)
         .sprite(ResourceLocation.withDefaultNamespace("icon/language"), 15, 15)
         .build();
   }

   public static SpriteIconButton accessibility(int pWidth, Button.OnPress pOnPress, boolean pIconOnly) {
      Component $$3 = pIconOnly ? Component.translatable("options.accessibility") : Component.translatable("accessibility.onboarding.accessibility.button");
      return SpriteIconButton.builder($$3, pOnPress, pIconOnly)
         .width(pWidth)
         .sprite(ResourceLocation.withDefaultNamespace("icon/accessibility"), 15, 15)
         .build();
   }
}
