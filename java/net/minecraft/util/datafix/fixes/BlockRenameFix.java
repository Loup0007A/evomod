package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class BlockRenameFix extends DataFix {
   private final String name;

   public BlockRenameFix(Schema pOutputSchema, String pName) {
      super(pOutputSchema, false);
      this.name = pName;
   }

   public TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.BLOCK_NAME);
      Type<Pair<String, String>> $$1 = DSL.named(References.BLOCK_NAME.typeName(), NamespacedSchema.namespacedString());
      if (!Objects.equals($$0, $$1)) {
         throw new IllegalStateException("block type is not what was expected.");
      } else {
         TypeRewriteRule $$2 = this.fixTypeEverywhere(this.name + " for block", $$1, p_14923_ -> p_145145_ -> p_145145_.mapSecond(this::renameBlock));
         TypeRewriteRule $$3 = this.fixTypeEverywhereTyped(
            this.name + " for block_state",
            this.getInputSchema().getType(References.BLOCK_STATE),
            p_14913_ -> p_14913_.update(DSL.remainderFinder(), this::fixBlockState)
         );
         TypeRewriteRule $$4 = this.fixTypeEverywhereTyped(
            this.name + " for flat_block_state",
            this.getInputSchema().getType(References.FLAT_BLOCK_STATE),
            p_326554_ -> p_326554_.update(
                  DSL.remainderFinder(),
                  p_326555_ -> (Dynamic)DataFixUtils.orElse(p_326555_.asString().result().map(this::fixFlatBlockState).map(p_326555_::createString), p_326555_)
               )
         );
         return TypeRewriteRule.seq($$2, new TypeRewriteRule[]{$$3, $$4});
      }
   }

   private Dynamic<?> fixBlockState(Dynamic<?> p_330740_) {
      Optional<String> $$1 = p_330740_.get("Name").asString().result();
      return $$1.isPresent() ? p_330740_.set("Name", p_330740_.createString(this.renameBlock($$1.get()))) : p_330740_;
   }

   private String fixFlatBlockState(String p_332510_) {
      int $$1 = p_332510_.indexOf(91);
      int $$2 = p_332510_.indexOf(123);
      int $$3 = p_332510_.length();
      if ($$1 > 0) {
         $$3 = $$1;
      }

      if ($$2 > 0) {
         $$3 = Math.min($$3, $$2);
      }

      String $$4 = p_332510_.substring(0, $$3);
      String $$5 = this.renameBlock($$4);
      return $$5 + p_332510_.substring($$3);
   }

   protected abstract String renameBlock(String p_14924_);

   public static DataFix create(Schema pOutputSchema, String pName, final Function<String, String> pRenamer) {
      return new BlockRenameFix(pOutputSchema, pName) {
         @Override
         protected String renameBlock(String p_333306_) {
            return pRenamer.apply(p_333306_);
         }
      };
   }
}
