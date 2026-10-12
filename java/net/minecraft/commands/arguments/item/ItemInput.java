package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemInput {
   private static final Dynamic2CommandExceptionType ERROR_STACK_TOO_BIG = new Dynamic2CommandExceptionType(
      (p_308404_, p_308405_) -> Component.translatableEscape("arguments.item.overstacked", p_308404_, p_308405_)
   );
   private final Holder<Item> item;
   private final DataComponentPatch components;

   public ItemInput(Holder<Item> pItem, DataComponentPatch pComponents) {
      this.item = pItem;
      this.components = pComponents;
   }

   public Item getItem() {
      return this.item.value();
   }

   public ItemStack createItemStack(int pCount, boolean pAllowOversizedStacks) throws CommandSyntaxException {
      ItemStack $$2 = new ItemStack(this.item, pCount);
      $$2.applyComponents(this.components);
      if (pAllowOversizedStacks && pCount > $$2.getMaxStackSize()) {
         throw ERROR_STACK_TOO_BIG.create(this.getItemName(), $$2.getMaxStackSize());
      } else {
         return $$2;
      }
   }

   public String serialize(HolderLookup.Provider pLevelRegistry) {
      StringBuilder $$1 = new StringBuilder(this.getItemName());
      String $$2 = this.serializeComponents(pLevelRegistry);
      if (!$$2.isEmpty()) {
         $$1.append('[');
         $$1.append($$2);
         $$1.append(']');
      }

      return $$1.toString();
   }

   private String serializeComponents(HolderLookup.Provider pLevelRegistries) {
      DynamicOps<Tag> $$1 = pLevelRegistries.createSerializationContext(NbtOps.INSTANCE);
      return this.components.entrySet().stream().flatMap(p_340970_ -> {
         DataComponentType<?> $$2 = p_340970_.getKey();
         ResourceLocation $$3 = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey($$2);
         if ($$3 == null) {
            return Stream.empty();
         } else {
            Optional<?> $$4 = p_340970_.getValue();
            if ($$4.isPresent()) {
               TypedDataComponent<?> $$5 = TypedDataComponent.createUnchecked($$2, $$4.get());
               return $$5.encodeValue($$1).result().stream().map(p_340968_ -> $$3.toString() + "=" + p_340968_);
            } else {
               return Stream.of("!" + $$3.toString());
            }
         }
      }).collect(Collectors.joining(String.valueOf(',')));
   }

   private String getItemName() {
      return this.item.unwrapKey().map(ResourceKey::location).orElseGet(() -> "unknown[" + this.item + "]").toString();
   }
}
