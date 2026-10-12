package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class ProjectileStoredWeaponFix extends DataFix {
   public ProjectileStoredWeaponFix(Schema pOutputSchema) {
      super(pOutputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.ENTITY);
      Type<?> $$1 = this.getOutputSchema().getType(References.ENTITY);
      return this.fixTypeEverywhereTyped(
         "Fix Arrow stored weapon", $$0, $$1, ExtraDataFixUtils.chainAllFilters(this.fixChoice("minecraft:arrow"), this.fixChoice("minecraft:spectral_arrow"))
      );
   }

   private Function<Typed<?>, Typed<?>> fixChoice(String pEntityId) {
      Type<?> $$1 = this.getInputSchema().getChoiceType(References.ENTITY, pEntityId);
      Type<?> $$2 = this.getOutputSchema().getChoiceType(References.ENTITY, pEntityId);
      return fixChoiceCap(pEntityId, $$1, $$2);
   }

   private static <T> Function<Typed<?>, Typed<?>> fixChoiceCap(String pEntityId, Type<?> pInputType, Type<T> pOutputType) {
      OpticFinder<?> $$3 = DSL.namedChoice(pEntityId, pInputType);
      return p_344956_ -> p_344956_.updateTyped($$3, pOutputType, p_344764_ -> Util.writeAndReadTypedOrThrow(p_344764_, pOutputType, UnaryOperator.identity()));
   }
}
