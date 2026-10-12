package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record NbtPredicate(CompoundTag tag) {
   public static final Codec<NbtPredicate> CODEC = TagParser.LENIENT_CODEC.xmap(NbtPredicate::new, NbtPredicate::tag);
   public static final StreamCodec<ByteBuf, NbtPredicate> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(NbtPredicate::new, NbtPredicate::tag);

   public boolean matches(ItemStack pStack) {
      CustomData $$1 = pStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
      return $$1.matchedBy(this.tag);
   }

   public boolean matches(Entity pEntity) {
      return this.matches(getEntityTagToCompare(pEntity));
   }

   public boolean matches(@Nullable Tag pTag) {
      return pTag != null && NbtUtils.compareNbt(this.tag, pTag, true);
   }

   public static CompoundTag getEntityTagToCompare(Entity pEntity) {
      CompoundTag $$1 = pEntity.saveWithoutId(new CompoundTag());
      if (pEntity instanceof Player) {
         ItemStack $$2 = ((Player)pEntity).getInventory().getSelected();
         if (!$$2.isEmpty()) {
            $$1.put("SelectedItem", $$2.save(pEntity.registryAccess()));
         }
      }

      return $$1;
   }
}
