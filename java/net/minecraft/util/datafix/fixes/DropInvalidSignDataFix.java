package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.ComponentDataFixUtils;

public class DropInvalidSignDataFix extends NamedEntityFix {
   private static final String[] FIELDS_TO_DROP = new String[]{
      "Text1", "Text2", "Text3", "Text4", "FilteredText1", "FilteredText2", "FilteredText3", "FilteredText4", "Color", "GlowingText"
   };

   public DropInvalidSignDataFix(Schema pOutputSchema, String pName, String pEntityName) {
      super(pOutputSchema, false, pName, References.BLOCK_ENTITY, pEntityName);
   }

   private static <T> Dynamic<T> fix(Dynamic<T> p_297398_) {
      p_297398_ = p_297398_.update("front_text", DropInvalidSignDataFix::fixText);
      p_297398_ = p_297398_.update("back_text", DropInvalidSignDataFix::fixText);

      for (String $$1 : FIELDS_TO_DROP) {
         p_297398_ = p_297398_.remove($$1);
      }

      return p_297398_;
   }

   private static <T> Dynamic<T> fixText(Dynamic<T> p_299128_) {
      boolean $$1 = p_299128_.get("_filtered_correct").asBoolean(false);
      if ($$1) {
         return p_299128_.remove("_filtered_correct");
      } else {
         Optional<Stream<Dynamic<T>>> $$2 = p_299128_.get("filtered_messages").asStreamOpt().result();
         if ($$2.isEmpty()) {
            return p_299128_;
         } else {
            Dynamic<T> $$3 = ComponentDataFixUtils.createEmptyComponent(p_299128_.getOps());
            List<Dynamic<T>> $$4 = p_299128_.get("messages").asStreamOpt().result().orElse(Stream.of()).toList();
            List<Dynamic<T>> $$5 = Streams.mapWithIndex($$2.get(), (p_298117_, p_298041_) -> {
               Dynamic<T> $$4x = p_298041_ < (long)$$4.size() ? $$4.get((int)p_298041_) : $$3;
               return p_298117_.equals($$3) ? $$4x : p_298117_;
            }).toList();
            return $$5.stream().allMatch(p_300495_ -> p_300495_.equals($$3))
               ? p_299128_.remove("filtered_messages")
               : p_299128_.set("filtered_messages", p_299128_.createList($$5.stream()));
         }
      }
   }

   @Override
   protected Typed<?> fix(Typed<?> pTyped) {
      return pTyped.update(DSL.remainderFinder(), DropInvalidSignDataFix::fix);
   }
}
