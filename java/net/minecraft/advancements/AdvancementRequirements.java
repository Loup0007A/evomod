package net.minecraft.advancements;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.network.FriendlyByteBuf;

public record AdvancementRequirements(List<List<String>> requirements) {
   public static final Codec<AdvancementRequirements> CODEC = Codec.STRING
      .listOf()
      .listOf()
      .xmap(AdvancementRequirements::new, AdvancementRequirements::requirements);
   public static final AdvancementRequirements EMPTY = new AdvancementRequirements(List.of());

   public AdvancementRequirements(FriendlyByteBuf pBuffer) {
      this(pBuffer.readList(p_325185_ -> p_325185_.readList(FriendlyByteBuf::readUtf)));
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeCollection(this.requirements, (p_325183_, p_325184_) -> p_325183_.writeCollection(p_325184_, FriendlyByteBuf::writeUtf));
   }

   public static AdvancementRequirements allOf(Collection<String> pRequirements) {
      return new AdvancementRequirements(pRequirements.stream().map(List::of).toList());
   }

   public static AdvancementRequirements anyOf(Collection<String> pCriteria) {
      return new AdvancementRequirements(List.of(List.copyOf(pCriteria)));
   }

   public int size() {
      return this.requirements.size();
   }

   public boolean test(Predicate<String> pPredicate) {
      if (this.requirements.isEmpty()) {
         return false;
      } else {
         for (List<String> $$1 : this.requirements) {
            if (!anyMatch($$1, pPredicate)) {
               return false;
            }
         }

         return true;
      }
   }

   public int count(Predicate<String> pFilter) {
      int $$1 = 0;

      for (List<String> $$2 : this.requirements) {
         if (anyMatch($$2, pFilter)) {
            $$1++;
         }
      }

      return $$1;
   }

   private static boolean anyMatch(List<String> pRequirements, Predicate<String> pPredicate) {
      for (String $$2 : pRequirements) {
         if (pPredicate.test($$2)) {
            return true;
         }
      }

      return false;
   }

   public DataResult<AdvancementRequirements> validate(Set<String> pRequirements) {
      Set<String> $$1 = new ObjectOpenHashSet();

      for (List<String> $$2 : this.requirements) {
         if ($$2.isEmpty() && pRequirements.isEmpty()) {
            return DataResult.error(() -> "Requirement entry cannot be empty");
         }

         $$1.addAll($$2);
      }

      if (!pRequirements.equals($$1)) {
         Set<String> $$3 = Sets.difference(pRequirements, $$1);
         Set<String> $$4 = Sets.difference($$1, pRequirements);
         return DataResult.error(() -> "Advancement completion requirements did not exactly match specified criteria. Missing: " + $$3 + ". Unknown: " + $$4);
      } else {
         return DataResult.success(this);
      }
   }

   public boolean isEmpty() {
      return this.requirements.isEmpty();
   }

   @Override
   public String toString() {
      return this.requirements.toString();
   }

   public Set<String> names() {
      Set<String> $$0 = new ObjectOpenHashSet();

      for (List<String> $$1 : this.requirements) {
         $$0.addAll($$1);
      }

      return $$0;
   }

   public interface Strategy {
      AdvancementRequirements.Strategy AND = AdvancementRequirements::allOf;
      AdvancementRequirements.Strategy OR = AdvancementRequirements::anyOf;

      AdvancementRequirements create(Collection<String> pCriteria);
   }
}
