package net.minecraft.world.item.enchantment.effects;

import com.google.common.collect.HashMultimap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record EnchantmentAttributeEffect(ResourceLocation id, Holder<Attribute> attribute, LevelBasedValue amount, AttributeModifier.Operation operation)
   implements EnchantmentLocationBasedEffect {
   public static final MapCodec<EnchantmentAttributeEffect> CODEC = RecordCodecBuilder.mapCodec(
      p_344407_ -> p_344407_.group(
               ResourceLocation.CODEC.fieldOf("id").forGetter(EnchantmentAttributeEffect::id),
               Attribute.CODEC.fieldOf("attribute").forGetter(EnchantmentAttributeEffect::attribute),
               LevelBasedValue.CODEC.fieldOf("amount").forGetter(EnchantmentAttributeEffect::amount),
               AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(EnchantmentAttributeEffect::operation)
            )
            .apply(p_344407_, EnchantmentAttributeEffect::new)
   );

   private ResourceLocation idForSlot(StringRepresentable pSlot) {
      return this.id.withSuffix("/" + pSlot.getSerializedName());
   }

   public AttributeModifier getModifier(int pEnchantmentLevel, StringRepresentable pSlot) {
      return new AttributeModifier(this.idForSlot(pSlot), (double)this.amount().calculate(pEnchantmentLevel), this.operation());
   }

   @Override
   public void onChangedBlock(ServerLevel pLevel, int pEnchantmentLevel, EnchantedItemInUse pItem, Entity pEntity, Vec3 pPos, boolean pApplyTransientEffects) {
      if (pApplyTransientEffects && pEntity instanceof LivingEntity $$6) {
         $$6.getAttributes().addTransientAttributeModifiers(this.makeAttributeMap(pEnchantmentLevel, pItem.inSlot()));
      }
   }

   @Override
   public void onDeactivated(EnchantedItemInUse pItem, Entity pEntity, Vec3 pPos, int pEnchantmentLevel) {
      if (pEntity instanceof LivingEntity $$4) {
         $$4.getAttributes().removeAttributeModifiers(this.makeAttributeMap(pEnchantmentLevel, pItem.inSlot()));
      }
   }

   private HashMultimap<Holder<Attribute>, AttributeModifier> makeAttributeMap(int pEnchantmentLevel, EquipmentSlot pSlot) {
      HashMultimap<Holder<Attribute>, AttributeModifier> $$2 = HashMultimap.create();
      $$2.put(this.attribute, this.getModifier(pEnchantmentLevel, pSlot));
      return $$2;
   }

   @Override
   public MapCodec<EnchantmentAttributeEffect> codec() {
      return CODEC;
   }
}
