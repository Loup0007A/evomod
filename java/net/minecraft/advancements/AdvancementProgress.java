package net.minecraft.advancements;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;

public class AdvancementProgress implements Comparable<AdvancementProgress> {
   private static final DateTimeFormatter OBTAINED_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
   private static final Codec<Instant> OBTAINED_TIME_CODEC = ExtraCodecs.temporalCodec(OBTAINED_TIME_FORMAT)
      .xmap(Instant::from, p_296108_ -> p_296108_.atZone(ZoneId.systemDefault()));
   private static final Codec<Map<String, CriterionProgress>> CRITERIA_CODEC = Codec.unboundedMap(Codec.STRING, OBTAINED_TIME_CODEC)
      .xmap(
         p_296104_ -> p_296104_.entrySet().stream().collect(Collectors.toMap(Entry::getKey, p_296110_ -> new CriterionProgress((Instant)p_296110_.getValue()))),
         p_296101_ -> p_296101_.entrySet()
               .stream()
               .filter(p_296109_ -> ((CriterionProgress)p_296109_.getValue()).isDone())
               .collect(Collectors.toMap(Entry::getKey, p_296103_ -> Objects.requireNonNull(((CriterionProgress)p_296103_.getValue()).getObtained())))
      );
   public static final Codec<AdvancementProgress> CODEC = RecordCodecBuilder.create(
      p_325180_ -> p_325180_.group(
               CRITERIA_CODEC.optionalFieldOf("criteria", Map.of()).forGetter(p_296102_ -> p_296102_.criteria),
               Codec.BOOL.fieldOf("done").orElse(true).forGetter(AdvancementProgress::isDone)
            )
            .apply(p_325180_, (p_296106_, p_296107_) -> new AdvancementProgress(new HashMap<>(p_296106_)))
   );
   private final Map<String, CriterionProgress> criteria;
   private AdvancementRequirements requirements = AdvancementRequirements.EMPTY;

   private AdvancementProgress(Map<String, CriterionProgress> pCriteria) {
      this.criteria = pCriteria;
   }

   public AdvancementProgress() {
      this.criteria = Maps.newHashMap();
   }

   public void update(AdvancementRequirements pRequirements) {
      Set<String> $$1 = pRequirements.names();
      this.criteria.entrySet().removeIf(p_8203_ -> !$$1.contains(p_8203_.getKey()));

      for (String $$2 : $$1) {
         this.criteria.putIfAbsent($$2, new CriterionProgress());
      }

      this.requirements = pRequirements;
   }

   public boolean isDone() {
      return this.requirements.test(this::isCriterionDone);
   }

   public boolean hasProgress() {
      for (CriterionProgress $$0 : this.criteria.values()) {
         if ($$0.isDone()) {
            return true;
         }
      }

      return false;
   }

   public boolean grantProgress(String pCriterionName) {
      CriterionProgress $$1 = this.criteria.get(pCriterionName);
      if ($$1 != null && !$$1.isDone()) {
         $$1.grant();
         return true;
      } else {
         return false;
      }
   }

   public boolean revokeProgress(String pCriterionName) {
      CriterionProgress $$1 = this.criteria.get(pCriterionName);
      if ($$1 != null && $$1.isDone()) {
         $$1.revoke();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public String toString() {
      return "AdvancementProgress{criteria=" + this.criteria + ", requirements=" + this.requirements + "}";
   }

   public void serializeToNetwork(FriendlyByteBuf pBuffer) {
      pBuffer.writeMap(this.criteria, FriendlyByteBuf::writeUtf, (p_144360_, p_144361_) -> p_144361_.serializeToNetwork(p_144360_));
   }

   public static AdvancementProgress fromNetwork(FriendlyByteBuf pBuffer) {
      Map<String, CriterionProgress> $$1 = pBuffer.readMap(FriendlyByteBuf::readUtf, CriterionProgress::fromNetwork);
      return new AdvancementProgress($$1);
   }

   @Nullable
   public CriterionProgress getCriterion(String pCriterionName) {
      return this.criteria.get(pCriterionName);
   }

   private boolean isCriterionDone(String p_301316_) {
      CriterionProgress $$1 = this.getCriterion(p_301316_);
      return $$1 != null && $$1.isDone();
   }

   public float getPercent() {
      if (this.criteria.isEmpty()) {
         return 0.0F;
      } else {
         float $$0 = (float)this.requirements.size();
         float $$1 = (float)this.countCompletedRequirements();
         return $$1 / $$0;
      }
   }

   @Nullable
   public Component getProgressText() {
      if (this.criteria.isEmpty()) {
         return null;
      } else {
         int $$0 = this.requirements.size();
         if ($$0 <= 1) {
            return null;
         } else {
            int $$1 = this.countCompletedRequirements();
            return Component.translatable("advancements.progress", $$1, $$0);
         }
      }
   }

   private int countCompletedRequirements() {
      return this.requirements.count(this::isCriterionDone);
   }

   public Iterable<String> getRemainingCriteria() {
      List<String> $$0 = Lists.newArrayList();

      for (Entry<String, CriterionProgress> $$1 : this.criteria.entrySet()) {
         if (!$$1.getValue().isDone()) {
            $$0.add($$1.getKey());
         }
      }

      return $$0;
   }

   public Iterable<String> getCompletedCriteria() {
      List<String> $$0 = Lists.newArrayList();

      for (Entry<String, CriterionProgress> $$1 : this.criteria.entrySet()) {
         if ($$1.getValue().isDone()) {
            $$0.add($$1.getKey());
         }
      }

      return $$0;
   }

   @Nullable
   public Instant getFirstProgressDate() {
      return this.criteria.values().stream().map(CriterionProgress::getObtained).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
   }

   public int compareTo(AdvancementProgress pOther) {
      Instant $$1 = this.getFirstProgressDate();
      Instant $$2 = pOther.getFirstProgressDate();
      if ($$1 == null && $$2 != null) {
         return 1;
      } else if ($$1 != null && $$2 == null) {
         return -1;
      } else {
         return $$1 == null && $$2 == null ? 0 : $$1.compareTo($$2);
      }
   }
}
