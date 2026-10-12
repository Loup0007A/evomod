package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.Locale;
import java.util.Optional;

public class OptionsLowerCaseLanguageFix extends DataFix {
   public OptionsLowerCaseLanguageFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "OptionsLowerCaseLanguageFix", this.getInputSchema().getType(References.OPTIONS), p_16662_ -> p_16662_.update(DSL.remainderFinder(), p_326637_ -> {
               Optional<String> $$1 = p_326637_.get("lang").asString().result();
               return $$1.isPresent() ? p_326637_.set("lang", p_326637_.createString($$1.get().toLowerCase(Locale.ROOT))) : p_326637_;
            })
      );
   }
}
