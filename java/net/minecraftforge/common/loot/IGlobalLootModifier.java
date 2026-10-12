package net.minecraftforge.common.loot;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public interface IGlobalLootModifier {
   Codec<IGlobalLootModifier> DIRECT_CODEC = Codec.lazyInitialized(
      () -> ForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS.get().getCodec().dispatch(IGlobalLootModifier::codec, Function.identity())
   );
   Codec<LootItemCondition[]> LOOT_CONDITIONS_CODEC = LootItemCondition.DIRECT_CODEC.listOf().xmap(list -> list.toArray(LootItemCondition[]::new), List::of);

   static <U> JsonElement getJson(Dynamic<?> dynamic) {
      return dynamic.getValue() instanceof JsonElement element ? element : (JsonElement)dynamic.getOps().convertTo(JsonOps.INSTANCE, dynamic.getValue());
   }

   @NotNull
   ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> var1, LootContext var2);

   MapCodec<? extends IGlobalLootModifier> codec();
}
