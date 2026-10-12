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

public class BlockEntitySignDoubleSidedEditableTextFix extends NamedEntityFix {
   public static final String FILTERED_CORRECT = "_filtered_correct";
   private static final String DEFAULT_COLOR = "black";

   public BlockEntitySignDoubleSidedEditableTextFix(Schema pOutputSchema, String pName, String pEntityName) {
      super(pOutputSchema, false, pName, References.BLOCK_ENTITY, pEntityName);
   }

   private static <T> Dynamic<T> fixTag(Dynamic<T> p_278110_) {
      return p_278110_.set("front_text", fixFrontTextTag(p_278110_))
         .set("back_text", createDefaultText(p_278110_))
         .set("is_waxed", p_278110_.createBoolean(false));
   }

   private static <T> Dynamic<T> fixFrontTextTag(Dynamic<T> pTag) {
      Dynamic<T> $$1 = ComponentDataFixUtils.createEmptyComponent(pTag.getOps());
      List<Dynamic<T>> $$2 = getLines(pTag, "Text").map(p_297945_ -> p_297945_.orElse($$1)).toList();
      Dynamic<T> $$3 = pTag.emptyMap()
         .set("messages", pTag.createList($$2.stream()))
         .set("color", pTag.get("Color").result().orElse(pTag.createString("black")))
         .set("has_glowing_text", pTag.get("GlowingText").result().orElse(pTag.createBoolean(false)))
         .set("_filtered_correct", pTag.createBoolean(true));
      List<Optional<Dynamic<T>>> $$4 = getLines(pTag, "FilteredText").toList();
      if ($$4.stream().anyMatch(Optional::isPresent)) {
         $$3 = $$3.set("filtered_messages", pTag.createList(Streams.mapWithIndex($$4.stream(), (p_299542_, p_300269_) -> {
            Dynamic<T> $$3x = $$2.get((int)p_300269_);
            return p_299542_.orElse($$3x);
         })));
      }

      return $$3;
   }

   private static <T> Stream<Optional<Dynamic<T>>> getLines(Dynamic<T> pDynamic, String pPrefix) {
      return Stream.of(
         pDynamic.get(pPrefix + "1").result(), pDynamic.get(pPrefix + "2").result(), pDynamic.get(pPrefix + "3").result(), pDynamic.get(pPrefix + "4").result()
      );
   }

   private static <T> Dynamic<T> createDefaultText(Dynamic<T> pDynamic) {
      return pDynamic.emptyMap()
         .set("messages", createEmptyLines(pDynamic))
         .set("color", pDynamic.createString("black"))
         .set("has_glowing_text", pDynamic.createBoolean(false));
   }

   private static <T> Dynamic<T> createEmptyLines(Dynamic<T> pDynamic) {
      Dynamic<T> $$1 = ComponentDataFixUtils.createEmptyComponent(pDynamic.getOps());
      return pDynamic.createList(Stream.of($$1, $$1, $$1, $$1));
   }

   @Override
   protected Typed<?> fix(Typed<?> pTyped) {
      return pTyped.update(DSL.remainderFinder(), BlockEntitySignDoubleSidedEditableTextFix::fixTag);
   }
}
