package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RenameEnchantmentsFix extends DataFix {
   final String name;
   final Map<String, String> renames;

   public RenameEnchantmentsFix(Schema pOutputSchema, String pName, Map<String, String> pRenames) {
      super(pOutputSchema, false);
      this.name = pName;
      this.renames = pRenames;
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> $$1 = $$0.findField("tag");
      return this.fixTypeEverywhereTyped(
         this.name, $$0, p_327788_ -> p_327788_.updateTyped($$1, p_330012_ -> p_330012_.update(DSL.remainderFinder(), this::fixTag))
      );
   }

   private Dynamic<?> fixTag(Dynamic<?> p_331261_) {
      p_331261_ = this.fixEnchantmentList(p_331261_, "Enchantments");
      return this.fixEnchantmentList(p_331261_, "StoredEnchantments");
   }

   private Dynamic<?> fixEnchantmentList(Dynamic<?> pTag, String pKey) {
      return pTag.update(
         pKey,
         p_335338_ -> (Dynamic)p_335338_.asStreamOpt()
               .map(
                  p_329289_ -> p_329289_.map(
                        p_333784_ -> p_333784_.update(
                              "id",
                              p_336146_ -> (Dynamic)p_336146_.asString()
                                    .map(
                                       p_341248_ -> p_333784_.createString(this.renames.getOrDefault(NamespacedSchema.ensureNamespaced(p_341248_), p_341248_))
                                    )
                                    .mapOrElse(Function.identity(), p_332784_ -> p_336146_)
                           )
                     )
               )
               .map(p_335338_::createList)
               .mapOrElse(Function.identity(), p_334640_ -> p_335338_)
      );
   }
}
