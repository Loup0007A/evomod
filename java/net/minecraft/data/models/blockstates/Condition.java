package net.minecraft.data.models.blockstates;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public interface Condition extends Supplier<JsonElement> {
   void validate(StateDefinition<?, ?> pStateDefinition);

   static Condition.TerminalCondition condition() {
      return new Condition.TerminalCondition();
   }

   static Condition and(Condition... pConditions) {
      return new Condition.CompositeCondition(Condition.Operation.AND, Arrays.asList(pConditions));
   }

   static Condition or(Condition... pConditions) {
      return new Condition.CompositeCondition(Condition.Operation.OR, Arrays.asList(pConditions));
   }

   public static class CompositeCondition implements Condition {
      private final Condition.Operation operation;
      private final List<Condition> subconditions;

      CompositeCondition(Condition.Operation pOperation, List<Condition> pSubconditions) {
         this.operation = pOperation;
         this.subconditions = pSubconditions;
      }

      @Override
      public void validate(StateDefinition<?, ?> pStateDefinition) {
         this.subconditions.forEach(p_125152_ -> p_125152_.validate(pStateDefinition));
      }

      public JsonElement get() {
         JsonArray $$0 = new JsonArray();
         this.subconditions.stream().map(Supplier::get).forEach($$0::add);
         JsonObject $$1 = new JsonObject();
         $$1.add(this.operation.id, $$0);
         return $$1;
      }
   }

   public static enum Operation {
      AND("AND"),
      OR("OR");

      final String id;

      private Operation(final String pId) {
         this.id = pId;
      }
   }

   public static class TerminalCondition implements Condition {
      private final Map<Property<?>, String> terms = Maps.newHashMap();

      private static <T extends Comparable<T>> String joinValues(Property<T> pProperty, Stream<T> pValueStream) {
         return pValueStream.<CharSequence>map(pProperty::getName).collect(Collectors.joining("|"));
      }

      private static <T extends Comparable<T>> String getTerm(Property<T> pProperty, T pFirstValue, T[] pAdditionalValues) {
         return joinValues(pProperty, Stream.concat(Stream.of(pFirstValue), Stream.of(pAdditionalValues)));
      }

      private <T extends Comparable<T>> void putValue(Property<T> pProperty, String pValue) {
         String $$2 = this.terms.put(pProperty, pValue);
         if ($$2 != null) {
            throw new IllegalStateException("Tried to replace " + pProperty + " value from " + $$2 + " to " + pValue);
         }
      }

      public final <T extends Comparable<T>> Condition.TerminalCondition term(Property<T> pProperty, T pValue) {
         this.putValue(pProperty, pProperty.getName(pValue));
         return this;
      }

      @SafeVarargs
      public final <T extends Comparable<T>> Condition.TerminalCondition term(Property<T> pProperty, T pFirstValue, T... pAdditionalValues) {
         this.putValue(pProperty, getTerm(pProperty, pFirstValue, pAdditionalValues));
         return this;
      }

      public final <T extends Comparable<T>> Condition.TerminalCondition negatedTerm(Property<T> pProperty, T pValue) {
         this.putValue(pProperty, "!" + pProperty.getName(pValue));
         return this;
      }

      @SafeVarargs
      public final <T extends Comparable<T>> Condition.TerminalCondition negatedTerm(Property<T> pProperty, T pFirstValue, T... pAdditionalValues) {
         this.putValue(pProperty, "!" + getTerm(pProperty, pFirstValue, pAdditionalValues));
         return this;
      }

      public JsonElement get() {
         JsonObject $$0 = new JsonObject();
         this.terms.forEach((p_125191_, p_125192_) -> $$0.addProperty(p_125191_.getName(), p_125192_));
         return $$0;
      }

      @Override
      public void validate(StateDefinition<?, ?> pStateDefinition) {
         List<Property<?>> $$1 = this.terms
            .keySet()
            .stream()
            .filter(p_125175_ -> pStateDefinition.getProperty(p_125175_.getName()) != p_125175_)
            .collect(Collectors.toList());
         if (!$$1.isEmpty()) {
            throw new IllegalStateException("Properties " + $$1 + " are missing from " + pStateDefinition);
         }
      }
   }
}
