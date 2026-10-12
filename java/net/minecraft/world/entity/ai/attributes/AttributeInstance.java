package net.minecraft.world.entity.ai.attributes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class AttributeInstance {
   private static final String BASE_FIELD = "base";
   private static final String MODIFIERS_FIELD = "modifiers";
   public static final String ID_FIELD = "id";
   private final Holder<Attribute> attribute;
   private final Map<AttributeModifier.Operation, Map<ResourceLocation, AttributeModifier>> modifiersByOperation = Maps.newEnumMap(
      AttributeModifier.Operation.class
   );
   private final Map<ResourceLocation, AttributeModifier> modifierById = new Object2ObjectArrayMap();
   private final Map<ResourceLocation, AttributeModifier> permanentModifiers = new Object2ObjectArrayMap();
   private double baseValue;
   private boolean dirty = true;
   private double cachedValue;
   private final Consumer<AttributeInstance> onDirty;

   public AttributeInstance(Holder<Attribute> pAttribute, Consumer<AttributeInstance> pOnDirty) {
      this.attribute = pAttribute;
      this.onDirty = pOnDirty;
      this.baseValue = pAttribute.value().getDefaultValue();
   }

   public Holder<Attribute> getAttribute() {
      return this.attribute;
   }

   public double getBaseValue() {
      return this.baseValue;
   }

   public void setBaseValue(double pBaseValue) {
      if (pBaseValue != this.baseValue) {
         this.baseValue = pBaseValue;
         this.setDirty();
      }
   }

   @VisibleForTesting
   Map<ResourceLocation, AttributeModifier> getModifiers(AttributeModifier.Operation pOperation) {
      return this.modifiersByOperation.computeIfAbsent(pOperation, p_326790_ -> new Object2ObjectOpenHashMap());
   }

   public Set<AttributeModifier> getModifiers() {
      return ImmutableSet.copyOf(this.modifierById.values());
   }

   @Nullable
   public AttributeModifier getModifier(ResourceLocation pId) {
      return this.modifierById.get(pId);
   }

   public boolean hasModifier(ResourceLocation pId) {
      return this.modifierById.get(pId) != null;
   }

   private void addModifier(AttributeModifier pModifier) {
      AttributeModifier $$1 = this.modifierById.putIfAbsent(pModifier.id(), pModifier);
      if ($$1 != null) {
         throw new IllegalArgumentException("Modifier is already applied on this attribute!");
      } else {
         this.getModifiers(pModifier.operation()).put(pModifier.id(), pModifier);
         this.setDirty();
      }
   }

   public void addOrUpdateTransientModifier(AttributeModifier pModifier) {
      AttributeModifier $$1 = this.modifierById.put(pModifier.id(), pModifier);
      if (pModifier != $$1) {
         this.getModifiers(pModifier.operation()).put(pModifier.id(), pModifier);
         this.setDirty();
      }
   }

   public void addTransientModifier(AttributeModifier pModifier) {
      this.addModifier(pModifier);
   }

   public void addOrReplacePermanentModifier(AttributeModifier pModifier) {
      this.removeModifier(pModifier.id());
      this.addModifier(pModifier);
      this.permanentModifiers.put(pModifier.id(), pModifier);
   }

   public void addPermanentModifier(AttributeModifier pModifier) {
      this.addModifier(pModifier);
      this.permanentModifiers.put(pModifier.id(), pModifier);
   }

   protected void setDirty() {
      this.dirty = true;
      this.onDirty.accept(this);
   }

   public void removeModifier(AttributeModifier pModifier) {
      this.removeModifier(pModifier.id());
   }

   public boolean removeModifier(ResourceLocation pId) {
      AttributeModifier $$1 = this.modifierById.remove(pId);
      if ($$1 == null) {
         return false;
      } else {
         this.getModifiers($$1.operation()).remove(pId);
         this.permanentModifiers.remove(pId);
         this.setDirty();
         return true;
      }
   }

   public void removeModifiers() {
      for (AttributeModifier $$0 : this.getModifiers()) {
         this.removeModifier($$0);
      }
   }

   public double getValue() {
      if (this.dirty) {
         this.cachedValue = this.calculateValue();
         this.dirty = false;
      }

      return this.cachedValue;
   }

   private double calculateValue() {
      double $$0 = this.getBaseValue();

      for (AttributeModifier $$1 : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_VALUE)) {
         $$0 += $$1.amount();
      }

      double $$2 = $$0;

      for (AttributeModifier $$3 : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
         $$2 += $$0 * $$3.amount();
      }

      for (AttributeModifier $$4 : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
         $$2 *= 1.0 + $$4.amount();
      }

      return this.attribute.value().sanitizeValue($$2);
   }

   private Collection<AttributeModifier> getModifiersOrEmpty(AttributeModifier.Operation pOperation) {
      return this.modifiersByOperation.getOrDefault(pOperation, Map.of()).values();
   }

   public void replaceFrom(AttributeInstance pInstance) {
      this.baseValue = pInstance.baseValue;
      this.modifierById.clear();
      this.modifierById.putAll(pInstance.modifierById);
      this.permanentModifiers.clear();
      this.permanentModifiers.putAll(pInstance.permanentModifiers);
      this.modifiersByOperation.clear();
      pInstance.modifiersByOperation
         .forEach((p_326791_, p_326792_) -> this.getModifiers(p_326791_).putAll((Map<? extends ResourceLocation, ? extends AttributeModifier>)p_326792_));
      this.setDirty();
   }

   public CompoundTag save() {
      CompoundTag $$0 = new CompoundTag();
      ResourceKey<Attribute> $$1 = this.attribute.unwrapKey().orElseThrow(() -> new IllegalStateException("Tried to serialize unregistered attribute"));
      $$0.putString("id", $$1.location().toString());
      $$0.putDouble("base", this.baseValue);
      if (!this.permanentModifiers.isEmpty()) {
         ListTag $$2 = new ListTag();

         for (AttributeModifier $$3 : this.permanentModifiers.values()) {
            $$2.add($$3.save());
         }

         $$0.put("modifiers", $$2);
      }

      return $$0;
   }

   public void load(CompoundTag pNbt) {
      this.baseValue = pNbt.getDouble("base");
      if (pNbt.contains("modifiers", 9)) {
         ListTag $$1 = pNbt.getList("modifiers", 10);

         for (int $$2 = 0; $$2 < $$1.size(); $$2++) {
            AttributeModifier $$3 = AttributeModifier.load($$1.getCompound($$2));
            if ($$3 != null) {
               this.modifierById.put($$3.id(), $$3);
               this.getModifiers($$3.operation()).put($$3.id(), $$3);
               this.permanentModifiers.put($$3.id(), $$3);
            }
         }
      }

      this.setDirty();
   }
}
