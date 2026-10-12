package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;

public record StorageValue(ResourceLocation storage, NbtPathArgument.NbtPath path) implements NumberProvider {
   public static final MapCodec<StorageValue> CODEC = RecordCodecBuilder.mapCodec(
      p_330652_ -> p_330652_.group(
               ResourceLocation.CODEC.fieldOf("storage").forGetter(StorageValue::storage),
               NbtPathArgument.NbtPath.CODEC.fieldOf("path").forGetter(StorageValue::path)
            )
            .apply(p_330652_, StorageValue::new)
   );

   @Override
   public LootNumberProviderType getType() {
      return NumberProviders.STORAGE;
   }

   private Optional<NumericTag> getNumericTag(LootContext pContext) {
      CompoundTag $$1 = pContext.getLevel().getServer().getCommandStorage().get(this.storage);

      try {
         List<Tag> $$2 = this.path.get($$1);
         if ($$2.size() == 1 && $$2.get(0) instanceof NumericTag $$3) {
            return Optional.of($$3);
         }
      } catch (CommandSyntaxException var6) {
      }

      return Optional.empty();
   }

   @Override
   public float getFloat(LootContext pLootContext) {
      return this.getNumericTag(pLootContext).map(NumericTag::getAsFloat).orElse(0.0F);
   }

   @Override
   public int getInt(LootContext pLootContext) {
      return this.getNumericTag(pLootContext).map(NumericTag::getAsInt).orElse(0);
   }
}
