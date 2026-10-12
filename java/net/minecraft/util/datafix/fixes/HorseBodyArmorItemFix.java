package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class HorseBodyArmorItemFix extends NamedEntityWriteReadFix {
   private final String previousBodyArmorTag;
   private final boolean clearArmorItems;

   public HorseBodyArmorItemFix(Schema pOutputSchema, String pEntityName, String pPreviousBodyArmorTag, boolean pClearArmorItems) {
      super(pOutputSchema, true, "Horse armor fix for " + pEntityName, References.ENTITY, pEntityName);
      this.previousBodyArmorTag = pPreviousBodyArmorTag;
      this.clearArmorItems = pClearArmorItems;
   }

   @Override
   protected <T> Dynamic<T> fix(Dynamic<T> pTag) {
      Optional<? extends Dynamic<?>> $$1 = pTag.get(this.previousBodyArmorTag).result();
      if ($$1.isPresent()) {
         Dynamic<?> $$2 = (Dynamic<?>)$$1.get();
         Dynamic<T> $$3 = pTag.remove(this.previousBodyArmorTag);
         if (this.clearArmorItems) {
            $$3 = $$3.update(
               "ArmorItems",
               p_333243_ -> p_333243_.createList(
                     Streams.mapWithIndex(p_333243_.asStream(), (p_328879_, p_335895_) -> p_335895_ == 2L ? p_328879_.emptyMap() : p_328879_)
                  )
            );
            $$3 = $$3.update(
               "ArmorDropChances",
               p_335133_ -> p_335133_.createList(
                     Streams.mapWithIndex(p_335133_.asStream(), (p_333050_, p_334688_) -> p_334688_ == 2L ? p_333050_.createFloat(0.085F) : p_333050_)
                  )
            );
         }

         $$3 = $$3.set("body_armor_item", $$2);
         return $$3.set("body_armor_drop_chance", pTag.createFloat(2.0F));
      } else {
         return pTag;
      }
   }
}
