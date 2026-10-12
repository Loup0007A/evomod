package net.minecraft.world.item;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

public class ArmorItem extends Item implements Equipable {
   public static final DispenseItemBehavior DISPENSE_ITEM_BEHAVIOR = new DefaultDispenseItemBehavior() {
      @Override
      protected ItemStack execute(BlockSource p_40408_, ItemStack p_40409_) {
         return ArmorItem.dispenseArmor(p_40408_, p_40409_) ? p_40409_ : super.execute(p_40408_, p_40409_);
      }
   };
   protected final ArmorItem.Type type;
   protected final Holder<ArmorMaterial> material;
   private final Supplier<ItemAttributeModifiers> defaultModifiers;

   public static boolean dispenseArmor(BlockSource pBlockSource, ItemStack pArmorItem) {
      BlockPos $$2 = pBlockSource.pos().relative(pBlockSource.state().getValue(DispenserBlock.FACING));
      List<LivingEntity> $$3 = pBlockSource.level()
         .getEntitiesOfClass(LivingEntity.class, new AABB($$2), EntitySelector.NO_SPECTATORS.and(new EntitySelector.MobCanWearArmorEntitySelector(pArmorItem)));
      if ($$3.isEmpty()) {
         return false;
      } else {
         LivingEntity $$4 = $$3.get(0);
         EquipmentSlot $$5 = $$4.getEquipmentSlotForItem(pArmorItem);
         ItemStack $$6 = pArmorItem.split(1);
         $$4.setItemSlot($$5, $$6);
         if ($$4 instanceof Mob) {
            ((Mob)$$4).setDropChance($$5, 2.0F);
            ((Mob)$$4).setPersistenceRequired();
         }

         return true;
      }
   }

   public ArmorItem(Holder<ArmorMaterial> pMaterial, ArmorItem.Type pType, Item.Properties pProperties) {
      super(pProperties);
      this.material = pMaterial;
      this.type = pType;
      DispenserBlock.registerBehavior(this, DISPENSE_ITEM_BEHAVIOR);
      this.defaultModifiers = Suppliers.memoize(() -> {
         int $$2 = pMaterial.value().getDefense(pType);
         float $$3 = pMaterial.value().toughness();
         ItemAttributeModifiers.Builder $$4 = ItemAttributeModifiers.builder();
         EquipmentSlotGroup $$5 = EquipmentSlotGroup.bySlot(pType.getSlot());
         ResourceLocation $$6 = ResourceLocation.withDefaultNamespace("armor." + pType.getName());
         $$4.add(Attributes.ARMOR, new AttributeModifier($$6, (double)$$2, AttributeModifier.Operation.ADD_VALUE), $$5);
         $$4.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier($$6, (double)$$3, AttributeModifier.Operation.ADD_VALUE), $$5);
         float $$7 = pMaterial.value().knockbackResistance();
         if ($$7 > 0.0F) {
            $$4.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier($$6, (double)$$7, AttributeModifier.Operation.ADD_VALUE), $$5);
         }

         return $$4.build();
      });
   }

   public ArmorItem.Type getType() {
      return this.type;
   }

   @Override
   public int getEnchantmentValue() {
      return this.material.value().enchantmentValue();
   }

   public Holder<ArmorMaterial> getMaterial() {
      return this.material;
   }

   @Override
   public boolean isValidRepairItem(ItemStack pToRepair, ItemStack pRepair) {
      return this.material.value().repairIngredient().get().test(pRepair) || super.isValidRepairItem(pToRepair, pRepair);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      return this.swapWithEquipmentSlot(this, pLevel, pPlayer, pHand);
   }

   @Override
   public ItemAttributeModifiers getDefaultAttributeModifiers() {
      return this.defaultModifiers.get();
   }

   public int getDefense() {
      return this.material.value().getDefense(this.type);
   }

   public float getToughness() {
      return this.material.value().toughness();
   }

   @Override
   public EquipmentSlot getEquipmentSlot() {
      return this.type.getSlot();
   }

   @Override
   public Holder<SoundEvent> getEquipSound() {
      return this.getMaterial().value().equipSound();
   }

   public static enum Type implements StringRepresentable {
      HELMET(EquipmentSlot.HEAD, 11, "helmet"),
      CHESTPLATE(EquipmentSlot.CHEST, 16, "chestplate"),
      LEGGINGS(EquipmentSlot.LEGS, 15, "leggings"),
      BOOTS(EquipmentSlot.FEET, 13, "boots"),
      BODY(EquipmentSlot.BODY, 16, "body");

      public static final Codec<ArmorItem.Type> CODEC = StringRepresentable.fromValues(ArmorItem.Type::values);
      private final EquipmentSlot slot;
      private final String name;
      private final int durability;

      private Type(final EquipmentSlot pSlot, final int pDurability, final String pName) {
         this.slot = pSlot;
         this.name = pName;
         this.durability = pDurability;
      }

      public int getDurability(int pDurabilityFactor) {
         return this.durability * pDurabilityFactor;
      }

      public EquipmentSlot getSlot() {
         return this.slot;
      }

      public String getName() {
         return this.name;
      }

      public boolean hasTrims() {
         return this == HELMET || this == CHESTPLATE || this == LEGGINGS || this == BOOTS;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
