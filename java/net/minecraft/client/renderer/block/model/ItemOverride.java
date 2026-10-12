package net.minecraft.client.renderer.block.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemOverride {
   private final ResourceLocation model;
   private final List<ItemOverride.Predicate> predicates;

   public ItemOverride(ResourceLocation pModel, List<ItemOverride.Predicate> pPredicates) {
      this.model = pModel;
      this.predicates = ImmutableList.copyOf(pPredicates);
   }

   public ResourceLocation getModel() {
      return this.model;
   }

   public Stream<ItemOverride.Predicate> getPredicates() {
      return this.predicates.stream();
   }

   @OnlyIn(Dist.CLIENT)
   public static class Deserializer implements JsonDeserializer<ItemOverride> {
      public ItemOverride deserialize(JsonElement pJson, Type pType, JsonDeserializationContext pContext) throws JsonParseException {
         JsonObject $$3 = pJson.getAsJsonObject();
         ResourceLocation $$4 = ResourceLocation.parse(GsonHelper.getAsString($$3, "model"));
         List<ItemOverride.Predicate> $$5 = this.getPredicates($$3);
         return new ItemOverride($$4, $$5);
      }

      protected List<ItemOverride.Predicate> getPredicates(JsonObject pJson) {
         Map<ResourceLocation, Float> $$1 = Maps.newLinkedHashMap();
         JsonObject $$2 = GsonHelper.getAsJsonObject(pJson, "predicate");

         for (Entry<String, JsonElement> $$3 : $$2.entrySet()) {
            $$1.put(ResourceLocation.parse($$3.getKey()), GsonHelper.convertToFloat($$3.getValue(), $$3.getKey()));
         }

         return $$1.entrySet()
            .stream()
            .map(p_173453_ -> new ItemOverride.Predicate(p_173453_.getKey(), p_173453_.getValue()))
            .collect(ImmutableList.toImmutableList());
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Predicate {
      private final ResourceLocation property;
      private final float value;

      public Predicate(ResourceLocation pProperty, float pValue) {
         this.property = pProperty;
         this.value = pValue;
      }

      public ResourceLocation getProperty() {
         return this.property;
      }

      public float getValue() {
         return this.value;
      }
   }
}
