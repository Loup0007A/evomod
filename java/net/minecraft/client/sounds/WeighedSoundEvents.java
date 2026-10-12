package net.minecraft.client.sounds;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WeighedSoundEvents implements Weighted<Sound> {
   private final List<Weighted<Sound>> list = Lists.newArrayList();
   @Nullable
   private final Component subtitle;

   public WeighedSoundEvents(ResourceLocation pLocation, @Nullable String pSubtitleKey) {
      this.subtitle = pSubtitleKey == null ? null : Component.translatable(pSubtitleKey);
   }

   @Override
   public int getWeight() {
      int $$0 = 0;

      for (Weighted<Sound> $$1 : this.list) {
         $$0 += $$1.getWeight();
      }

      return $$0;
   }

   public Sound getSound(RandomSource pRandomSource) {
      int $$1 = this.getWeight();
      if (!this.list.isEmpty() && $$1 != 0) {
         int $$2 = pRandomSource.nextInt($$1);

         for (Weighted<Sound> $$3 : this.list) {
            $$2 -= $$3.getWeight();
            if ($$2 < 0) {
               return $$3.getSound(pRandomSource);
            }
         }

         return SoundManager.EMPTY_SOUND;
      } else {
         return SoundManager.EMPTY_SOUND;
      }
   }

   public void addSound(Weighted<Sound> pAccessor) {
      this.list.add(pAccessor);
   }

   @Nullable
   public Component getSubtitle() {
      return this.subtitle;
   }

   @Override
   public void preloadIfRequired(SoundEngine pEngine) {
      for (Weighted<Sound> $$1 : this.list) {
         $$1.preloadIfRequired(pEngine);
      }
   }
}
