package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RemoveEmptyItemInBrushableBlockFix extends NamedEntityWriteReadFix {
   public RemoveEmptyItemInBrushableBlockFix(Schema pOutputSchema) {
      super(pOutputSchema, false, "RemoveEmptyItemInSuspiciousBlockFix", References.BLOCK_ENTITY, "minecraft:brushable_block");
   }

   @Override
   protected <T> Dynamic<T> fix(Dynamic<T> pTag) {
      Optional<Dynamic<T>> $$1 = pTag.get("item").result();
      return $$1.isPresent() && isEmptyStack($$1.get()) ? pTag.remove("item") : pTag;
   }

   private static boolean isEmptyStack(Dynamic<?> pTag) {
      String $$1 = NamespacedSchema.ensureNamespaced(pTag.get("id").asString("minecraft:air"));
      int $$2 = pTag.get("count").asInt(0);
      return $$1.equals("minecraft:air") || $$2 == 0;
   }
}
