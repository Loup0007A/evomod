package net.minecraftforge.common.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.crafting.conditions.ICondition;

public class ConditionalAdvancement {
   public static ConditionalAdvancement.Builder builder() {
      return new ConditionalAdvancement.Builder();
   }

   private static record Adv(ICondition condition, Advancement adv, JsonObject json) {
   }

   public static class Builder {
      private static final ResourceLocation DOESNT_MATTER = ResourceLocation.fromNamespaceAndPath("doesnt", "matter");
      private List<ConditionalAdvancement.Adv> advancements = new ArrayList<>();
      private ICondition condition;

      public ConditionalAdvancement.Builder condition(ICondition value) {
         this.condition = value;
         return this;
      }

      public ConditionalAdvancement.Builder advancement(Consumer<Consumer<Advancement.Builder>> callable) {
         callable.accept(this::advancement);
         return this;
      }

      public ConditionalAdvancement.Builder advancement(Advancement.Builder builder) {
         return this.advancement(builder.build(DOESNT_MATTER).value());
      }

      public ConditionalAdvancement.Builder advancement(AdvancementHolder holder) {
         return this.advancement(holder.value());
      }

      public ConditionalAdvancement.Builder advancement(JsonObject value) {
         return this.advancement(null, value);
      }

      private ConditionalAdvancement.Builder advancement(Advancement value) {
         return this.advancement(value, null);
      }

      private ConditionalAdvancement.Builder advancement(Advancement value, JsonObject json) {
         if (this.condition == null) {
            throw new IllegalStateException("Can not add a advancement with no conditions.");
         } else if (value == null) {
            throw new IllegalStateException("Can not add a null advancement");
         } else {
            this.advancements.add(new ConditionalAdvancement.Adv(this.condition, value, json));
            this.condition = null;
            return this;
         }
      }

      public JsonObject build(HolderLookup.Provider lookup) {
         JsonObject json = new JsonObject();
         JsonArray array = new JsonArray();
         json.add("advancements", array);
         RegistryOps<JsonElement> ops = lookup.createSerializationContext(JsonOps.INSTANCE);

         for (ConditionalAdvancement.Adv pair : this.advancements) {
            JsonObject holder = null;
            if (pair.json != null) {
               holder = pair.json;
            } else {
               holder = (JsonObject)Advancement.CODEC.encodeStart(ops, pair.adv()).getOrThrow(IllegalStateException::new);
            }

            if (holder.has("forge:condition")) {
               throw new IllegalStateException("Recipe already serialized conditions!");
            }

            ForgeHooks.writeCondition(pair.condition(), holder);
            array.add(holder);
         }

         return json;
      }
   }
}
