package net.minecraft.world.level;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public interface Spawner {
   void setEntityId(EntityType<?> pEntityType, RandomSource pRandom);

   static void appendHoverText(ItemStack pStack, List<Component> pTooltipLines, String pSpawnDataKey) {
      Component $$3 = getSpawnEntityDisplayName(pStack, pSpawnDataKey);
      if ($$3 != null) {
         pTooltipLines.add($$3);
      } else {
         pTooltipLines.add(CommonComponents.EMPTY);
         pTooltipLines.add(Component.translatable("block.minecraft.spawner.desc1").withStyle(ChatFormatting.GRAY));
         pTooltipLines.add(CommonComponents.space().append(Component.translatable("block.minecraft.spawner.desc2").withStyle(ChatFormatting.BLUE)));
      }
   }

   @Nullable
   static Component getSpawnEntityDisplayName(ItemStack pStack, String pSpawnDataKey) {
      CompoundTag $$2 = pStack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).getUnsafe();
      ResourceLocation $$3 = getEntityKey($$2, pSpawnDataKey);
      return $$3 != null
         ? BuiltInRegistries.ENTITY_TYPE
            .getOptional($$3)
            .map(p_311493_ -> Component.translatable(p_311493_.getDescriptionId()).withStyle(ChatFormatting.GRAY))
            .orElse(null)
         : null;
   }

   @Nullable
   private static ResourceLocation getEntityKey(CompoundTag pTag, String pSpawnDataKey) {
      if (pTag.contains(pSpawnDataKey, 10)) {
         String $$2 = pTag.getCompound(pSpawnDataKey).getCompound("entity").getString("id");
         return ResourceLocation.tryParse($$2);
      } else {
         return null;
      }
   }
}
