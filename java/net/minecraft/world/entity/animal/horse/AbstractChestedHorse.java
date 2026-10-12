package net.minecraft.world.entity.animal.horse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public abstract class AbstractChestedHorse extends AbstractHorse {
   private static final EntityDataAccessor<Boolean> DATA_ID_CHEST = SynchedEntityData.defineId(AbstractChestedHorse.class, EntityDataSerializers.BOOLEAN);
   private final EntityDimensions babyDimensions;

   protected AbstractChestedHorse(EntityType<? extends AbstractChestedHorse> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.canGallop = false;
      this.babyDimensions = pEntityType.getDimensions()
         .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, pEntityType.getHeight() - 0.15625F, 0.0F))
         .scale(0.5F);
   }

   @Override
   protected void randomizeAttributes(RandomSource pRandom) {
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue((double)generateMaxHealth(pRandom::nextInt));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
      super.defineSynchedData(pBuilder);
      pBuilder.define(DATA_ID_CHEST, false);
   }

   public static AttributeSupplier.Builder createBaseChestedHorseAttributes() {
      return createBaseHorseAttributes().add(Attributes.MOVEMENT_SPEED, 0.175F).add(Attributes.JUMP_STRENGTH, 0.5);
   }

   public boolean hasChest() {
      return this.entityData.get(DATA_ID_CHEST);
   }

   public void setChest(boolean pChested) {
      this.entityData.set(DATA_ID_CHEST, pChested);
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose pPose) {
      return this.isBaby() ? this.babyDimensions : super.getDefaultDimensions(pPose);
   }

   @Override
   protected void dropEquipment() {
      super.dropEquipment();
      if (this.hasChest()) {
         if (!this.level().isClientSide) {
            this.spawnAtLocation(Blocks.CHEST);
         }

         this.setChest(false);
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putBoolean("ChestedHorse", this.hasChest());
      if (this.hasChest()) {
         ListTag $$1 = new ListTag();

         for (int $$2 = 1; $$2 < this.inventory.getContainerSize(); $$2++) {
            ItemStack $$3 = this.inventory.getItem($$2);
            if (!$$3.isEmpty()) {
               CompoundTag $$4 = new CompoundTag();
               $$4.putByte("Slot", (byte)($$2 - 1));
               $$1.add($$3.save(this.registryAccess(), $$4));
            }
         }

         pCompound.put("Items", $$1);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.setChest(pCompound.getBoolean("ChestedHorse"));
      this.createInventory();
      if (this.hasChest()) {
         ListTag $$1 = pCompound.getList("Items", 10);

         for (int $$2 = 0; $$2 < $$1.size(); $$2++) {
            CompoundTag $$3 = $$1.getCompound($$2);
            int $$4 = $$3.getByte("Slot") & 255;
            if ($$4 < this.inventory.getContainerSize() - 1) {
               this.inventory.setItem($$4 + 1, ItemStack.parse(this.registryAccess(), $$3).orElse(ItemStack.EMPTY));
            }
         }
      }

      this.syncSaddleToClients();
   }

   @Override
   public SlotAccess getSlot(int pSlot) {
      return pSlot == 499 ? new SlotAccess() {
         @Override
         public ItemStack get() {
            return AbstractChestedHorse.this.hasChest() ? new ItemStack(Items.CHEST) : ItemStack.EMPTY;
         }

         @Override
         public boolean set(ItemStack p_149485_) {
            if (p_149485_.isEmpty()) {
               if (AbstractChestedHorse.this.hasChest()) {
                  AbstractChestedHorse.this.setChest(false);
                  AbstractChestedHorse.this.createInventory();
               }

               return true;
            } else if (p_149485_.is(Items.CHEST)) {
               if (!AbstractChestedHorse.this.hasChest()) {
                  AbstractChestedHorse.this.setChest(true);
                  AbstractChestedHorse.this.createInventory();
               }

               return true;
            } else {
               return false;
            }
         }
      } : super.getSlot(pSlot);
   }

   @Override
   public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
      boolean $$2 = !this.isBaby() && this.isTamed() && pPlayer.isSecondaryUseActive();
      if (!this.isVehicle() && !$$2) {
         ItemStack $$3 = pPlayer.getItemInHand(pHand);
         if (!$$3.isEmpty()) {
            if (this.isFood($$3)) {
               return this.fedFood(pPlayer, $$3);
            }

            if (!this.isTamed()) {
               this.makeMad();
               return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (!this.hasChest() && $$3.is(Items.CHEST)) {
               this.equipChest(pPlayer, $$3);
               return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
         }

         return super.mobInteract(pPlayer, pHand);
      } else {
         return super.mobInteract(pPlayer, pHand);
      }
   }

   private void equipChest(Player pPlayer, ItemStack pChestStack) {
      this.setChest(true);
      this.playChestEquipsSound();
      pChestStack.consume(1, pPlayer);
      this.createInventory();
   }

   protected void playChestEquipsSound() {
      this.playSound(SoundEvents.DONKEY_CHEST, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
   }

   @Override
   public int getInventoryColumns() {
      return this.hasChest() ? 5 : 0;
   }
}
