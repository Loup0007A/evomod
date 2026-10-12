package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class AttributeMap {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<Holder<Attribute>, AttributeInstance> attributes = new Object2ObjectOpenHashMap();
   private final Set<AttributeInstance> attributesToSync = new ObjectOpenHashSet();
   private final Set<AttributeInstance> attributesToUpdate = new ObjectOpenHashSet();
   private final AttributeSupplier supplier;

   public AttributeMap(AttributeSupplier pSupplier) {
      this.supplier = pSupplier;
   }

   private void onAttributeModified(AttributeInstance p_22158_) {
      this.attributesToUpdate.add(p_22158_);
      if (p_22158_.getAttribute().value().isClientSyncable()) {
         this.attributesToSync.add(p_22158_);
      }
   }

   public Set<AttributeInstance> getAttributesToSync() {
      return this.attributesToSync;
   }

   public Set<AttributeInstance> getAttributesToUpdate() {
      return this.attributesToUpdate;
   }

   public Collection<AttributeInstance> getSyncableAttributes() {
      return this.attributes.values().stream().filter(p_326797_ -> p_326797_.getAttribute().value().isClientSyncable()).collect(Collectors.toList());
   }

   @Nullable
   public AttributeInstance getInstance(Holder<Attribute> pAttribute) {
      return this.attributes.computeIfAbsent(pAttribute, p_326793_ -> this.supplier.createInstance(this::onAttributeModified, (Holder<Attribute>)p_326793_));
   }

   public boolean hasAttribute(Holder<Attribute> pAttribute) {
      return this.attributes.get(pAttribute) != null || this.supplier.hasAttribute(pAttribute);
   }

   public boolean hasModifier(Holder<Attribute> pAttribute, ResourceLocation pId) {
      AttributeInstance $$2 = this.attributes.get(pAttribute);
      return $$2 != null ? $$2.getModifier(pId) != null : this.supplier.hasModifier(pAttribute, pId);
   }

   public double getValue(Holder<Attribute> pAttribute) {
      AttributeInstance $$1 = this.attributes.get(pAttribute);
      return $$1 != null ? $$1.getValue() : this.supplier.getValue(pAttribute);
   }

   public double getBaseValue(Holder<Attribute> pAttribute) {
      AttributeInstance $$1 = this.attributes.get(pAttribute);
      return $$1 != null ? $$1.getBaseValue() : this.supplier.getBaseValue(pAttribute);
   }

   public double getModifierValue(Holder<Attribute> pAttribute, ResourceLocation pId) {
      AttributeInstance $$2 = this.attributes.get(pAttribute);
      return $$2 != null ? $$2.getModifier(pId).amount() : this.supplier.getModifierValue(pAttribute, pId);
   }

   public void addTransientAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> pModifiers) {
      pModifiers.forEach((p_341286_, p_341287_) -> {
         AttributeInstance $$2 = this.getInstance(p_341286_);
         if ($$2 != null) {
            $$2.removeModifier(p_341287_.id());
            $$2.addTransientModifier(p_341287_);
         }
      });
   }

   public void removeAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> pModifiers) {
      pModifiers.asMap().forEach((p_341283_, p_341284_) -> {
         AttributeInstance $$2 = this.attributes.get(p_341283_);
         if ($$2 != null) {
            p_341284_.forEach(p_341289_ -> $$2.removeModifier(p_341289_.id()));
         }
      });
   }

   public void assignAllValues(AttributeMap pMap) {
      pMap.attributes.values().forEach(p_326796_ -> {
         AttributeInstance $$1 = this.getInstance(p_326796_.getAttribute());
         if ($$1 != null) {
            $$1.replaceFrom(p_326796_);
         }
      });
   }

   public void assignBaseValues(AttributeMap pMap) {
      pMap.attributes.values().forEach(p_341285_ -> {
         AttributeInstance $$1 = this.getInstance(p_341285_.getAttribute());
         if ($$1 != null) {
            $$1.setBaseValue(p_341285_.getBaseValue());
         }
      });
   }

   public ListTag save() {
      ListTag $$0 = new ListTag();

      for (AttributeInstance $$1 : this.attributes.values()) {
         $$0.add($$1.save());
      }

      return $$0;
   }

   public void load(ListTag pNbt) {
      for (int $$1 = 0; $$1 < pNbt.size(); $$1++) {
         CompoundTag $$2 = pNbt.getCompound($$1);
         String $$3 = $$2.getString("id");
         ResourceLocation $$4 = ResourceLocation.tryParse($$3);
         if ($$4 != null) {
            Util.ifElse(BuiltInRegistries.ATTRIBUTE.getHolder($$4), p_326795_ -> {
               AttributeInstance $$2x = this.getInstance(p_326795_);
               if ($$2x != null) {
                  $$2x.load($$2);
               }
            }, () -> LOGGER.warn("Ignoring unknown attribute '{}'", $$4));
         } else {
            LOGGER.warn("Ignoring malformed attribute '{}'", $$3);
         }
      }
   }
}
