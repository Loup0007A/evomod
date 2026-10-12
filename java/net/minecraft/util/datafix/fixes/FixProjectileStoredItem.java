package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class FixProjectileStoredItem extends DataFix {
   private static final String EMPTY_POTION = "minecraft:empty";

   public FixProjectileStoredItem(Schema pOutputSchema) {
      super(pOutputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.ENTITY);
      Type<?> $$1 = this.getOutputSchema().getType(References.ENTITY);
      return this.fixTypeEverywhereTyped(
         "Fix AbstractArrow item type",
         $$0,
         $$1,
         ExtraDataFixUtils.chainAllFilters(
            this.fixChoice("minecraft:trident", FixProjectileStoredItem::castUnchecked),
            this.fixChoice("minecraft:arrow", FixProjectileStoredItem::fixArrow),
            this.fixChoice("minecraft:spectral_arrow", FixProjectileStoredItem::fixSpectralArrow)
         )
      );
   }

   private Function<Typed<?>, Typed<?>> fixChoice(String pItemId, FixProjectileStoredItem.SubFixer<?> pFixer) {
      Type<?> $$2 = this.getInputSchema().getChoiceType(References.ENTITY, pItemId);
      Type<?> $$3 = this.getOutputSchema().getChoiceType(References.ENTITY, pItemId);
      return fixChoiceCap(pItemId, pFixer, $$2, $$3);
   }

   private static <T> Function<Typed<?>, Typed<?>> fixChoiceCap(String pItemId, FixProjectileStoredItem.SubFixer<?> pFixer, Type<?> pOldType, Type<T> pNewType) {
      OpticFinder<?> $$4 = DSL.namedChoice(pItemId, pOldType);
      return p_313205_ -> p_313205_.updateTyped($$4, pNewType, p_312567_ -> pFixer.fix(p_312567_, pNewType));
   }

   private static <T> Typed<T> fixArrow(Typed<?> p_312190_, Type<T> p_311775_) {
      return Util.writeAndReadTypedOrThrow(p_312190_, p_311775_, p_312479_ -> p_312479_.set("item", createItemStack(p_312479_, getArrowType(p_312479_))));
   }

   private static String getArrowType(Dynamic<?> pArrowTag) {
      return pArrowTag.get("Potion").asString("minecraft:empty").equals("minecraft:empty") ? "minecraft:arrow" : "minecraft:tipped_arrow";
   }

   private static <T> Typed<T> fixSpectralArrow(Typed<?> p_311496_, Type<T> p_311551_) {
      return Util.writeAndReadTypedOrThrow(p_311496_, p_311551_, p_310800_ -> p_310800_.set("item", createItemStack(p_310800_, "minecraft:spectral_arrow")));
   }

   private static Dynamic<?> createItemStack(Dynamic<?> pDynamic, String pItemId) {
      return pDynamic.createMap(
         ImmutableMap.of(pDynamic.createString("id"), pDynamic.createString(pItemId), pDynamic.createString("Count"), pDynamic.createInt(1))
      );
   }

   private static <T> Typed<T> castUnchecked(Typed<?> p_310006_, Type<T> p_312989_) {
      return new Typed(p_312989_, p_310006_.getOps(), p_310006_.getValue());
   }

   interface SubFixer<F> {
      Typed<F> fix(Typed<?> pTyped, Type<F> pNewType);
   }
}
