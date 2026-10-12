package net.minecraft.data.models.blockstates;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Variant implements Supplier<JsonElement> {
   private final Map<VariantProperty<?>, VariantProperty<?>.Value> values = Maps.newLinkedHashMap();

   public <T> Variant with(VariantProperty<T> pProperty, T pValue) {
      VariantProperty<?>.Value $$2 = this.values.put(pProperty, pProperty.withValue(pValue));
      if ($$2 != null) {
         throw new IllegalStateException("Replacing value of " + $$2 + " with " + pValue);
      } else {
         return this;
      }
   }

   public static Variant variant() {
      return new Variant();
   }

   public static Variant merge(Variant pDefinition1, Variant pDefinition2) {
      Variant $$2 = new Variant();
      $$2.values.putAll(pDefinition1.values);
      $$2.values.putAll(pDefinition2.values);
      return $$2;
   }

   public JsonElement get() {
      JsonObject $$0 = new JsonObject();
      this.values.values().forEach(p_125507_ -> p_125507_.addToVariant($$0));
      return $$0;
   }

   public static JsonElement convertList(List<Variant> pDefinitions) {
      if (pDefinitions.size() == 1) {
         return pDefinitions.get(0).get();
      } else {
         JsonArray $$1 = new JsonArray();
         pDefinitions.forEach(p_125504_ -> $$1.add(p_125504_.get()));
         return $$1;
      }
   }
}
