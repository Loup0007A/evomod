package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class EnchantingTableBlockEntity extends BlockEntity implements Nameable {
   public int time;
   public float flip;
   public float oFlip;
   public float flipT;
   public float flipA;
   public float open;
   public float oOpen;
   public float rot;
   public float oRot;
   public float tRot;
   private static final RandomSource RANDOM = RandomSource.create();
   @Nullable
   private Component name;

   public EnchantingTableBlockEntity(BlockPos pPos, BlockState pState) {
      super(BlockEntityType.ENCHANTING_TABLE, pPos, pState);
   }

   @Override
   protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.saveAdditional(pTag, pRegistries);
      if (this.hasCustomName()) {
         pTag.putString("CustomName", Component.Serializer.toJson(this.name, pRegistries));
      }
   }

   @Override
   protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.loadAdditional(pTag, pRegistries);
      if (pTag.contains("CustomName", 8)) {
         this.name = parseCustomNameSafe(pTag.getString("CustomName"), pRegistries);
      }
   }

   public static void bookAnimationTick(Level pLevel, BlockPos pPos, BlockState pState, EnchantingTableBlockEntity pEnchantingTable) {
      pEnchantingTable.oOpen = pEnchantingTable.open;
      pEnchantingTable.oRot = pEnchantingTable.rot;
      Player $$4 = pLevel.getNearestPlayer((double)pPos.getX() + 0.5, (double)pPos.getY() + 0.5, (double)pPos.getZ() + 0.5, 3.0, false);
      if ($$4 != null) {
         double $$5 = $$4.getX() - ((double)pPos.getX() + 0.5);
         double $$6 = $$4.getZ() - ((double)pPos.getZ() + 0.5);
         pEnchantingTable.tRot = (float)Mth.atan2($$6, $$5);
         pEnchantingTable.open += 0.1F;
         if (pEnchantingTable.open < 0.5F || RANDOM.nextInt(40) == 0) {
            float $$7 = pEnchantingTable.flipT;

            do {
               pEnchantingTable.flipT = pEnchantingTable.flipT + (float)(RANDOM.nextInt(4) - RANDOM.nextInt(4));
            } while ($$7 == pEnchantingTable.flipT);
         }
      } else {
         pEnchantingTable.tRot += 0.02F;
         pEnchantingTable.open -= 0.1F;
      }

      while (pEnchantingTable.rot >= (float) Math.PI) {
         pEnchantingTable.rot -= (float) (Math.PI * 2);
      }

      while (pEnchantingTable.rot < (float) -Math.PI) {
         pEnchantingTable.rot += (float) (Math.PI * 2);
      }

      while (pEnchantingTable.tRot >= (float) Math.PI) {
         pEnchantingTable.tRot -= (float) (Math.PI * 2);
      }

      while (pEnchantingTable.tRot < (float) -Math.PI) {
         pEnchantingTable.tRot += (float) (Math.PI * 2);
      }

      float $$8 = pEnchantingTable.tRot - pEnchantingTable.rot;

      while ($$8 >= (float) Math.PI) {
         $$8 -= (float) (Math.PI * 2);
      }

      while ($$8 < (float) -Math.PI) {
         $$8 += (float) (Math.PI * 2);
      }

      pEnchantingTable.rot += $$8 * 0.4F;
      pEnchantingTable.open = Mth.clamp(pEnchantingTable.open, 0.0F, 1.0F);
      pEnchantingTable.time++;
      pEnchantingTable.oFlip = pEnchantingTable.flip;
      float $$9 = (pEnchantingTable.flipT - pEnchantingTable.flip) * 0.4F;
      float $$10 = 0.2F;
      $$9 = Mth.clamp($$9, -0.2F, 0.2F);
      pEnchantingTable.flipA = pEnchantingTable.flipA + ($$9 - pEnchantingTable.flipA) * 0.9F;
      pEnchantingTable.flip = pEnchantingTable.flip + pEnchantingTable.flipA;
   }

   @Override
   public Component getName() {
      return (Component)(this.name != null ? this.name : Component.translatable("container.enchant"));
   }

   public void setCustomName(@Nullable Component pCustomName) {
      this.name = pCustomName;
   }

   @Nullable
   @Override
   public Component getCustomName() {
      return this.name;
   }

   @Override
   protected void applyImplicitComponents(BlockEntity.DataComponentInput pComponentInput) {
      super.applyImplicitComponents(pComponentInput);
      this.name = pComponentInput.get(DataComponents.CUSTOM_NAME);
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder pComponents) {
      super.collectImplicitComponents(pComponents);
      pComponents.set(DataComponents.CUSTOM_NAME, this.name);
   }

   @Override
   public void removeComponentsFromTag(CompoundTag pTag) {
      pTag.remove("CustomName");
   }
}
