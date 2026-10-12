package net.minecraft.network.chat;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class HoverEvent {
   public static final Codec<HoverEvent> CODEC = Codec.withAlternative(
         HoverEvent.TypedHoverEvent.CODEC.codec(), HoverEvent.TypedHoverEvent.LEGACY_CODEC.codec()
      )
      .xmap(HoverEvent::new, p_326065_ -> p_326065_.event);
   private final HoverEvent.TypedHoverEvent<?> event;

   public <T> HoverEvent(HoverEvent.Action<T> pAction, T pValue) {
      this(new HoverEvent.TypedHoverEvent<>(pAction, pValue));
   }

   private HoverEvent(HoverEvent.TypedHoverEvent<?> p_313245_) {
      this.event = p_313245_;
   }

   public HoverEvent.Action<?> getAction() {
      return this.event.action;
   }

   @Nullable
   public <T> T getValue(HoverEvent.Action<T> pActionType) {
      return this.event.action == pActionType ? pActionType.cast(this.event.value) : null;
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         return pOther != null && this.getClass() == pOther.getClass() ? ((HoverEvent)pOther).event.equals(this.event) : false;
      }
   }

   @Override
   public String toString() {
      return this.event.toString();
   }

   @Override
   public int hashCode() {
      return this.event.hashCode();
   }

   public static class Action<T> implements StringRepresentable {
      public static final HoverEvent.Action<Component> SHOW_TEXT = new HoverEvent.Action<>(
         "show_text", true, ComponentSerialization.CODEC, (p_326066_, p_326067_) -> DataResult.success(p_326066_)
      );
      public static final HoverEvent.Action<HoverEvent.ItemStackInfo> SHOW_ITEM = new HoverEvent.Action<>(
         "show_item", true, HoverEvent.ItemStackInfo.CODEC, HoverEvent.ItemStackInfo::legacyCreate
      );
      public static final HoverEvent.Action<HoverEvent.EntityTooltipInfo> SHOW_ENTITY = new HoverEvent.Action<>(
         "show_entity", true, HoverEvent.EntityTooltipInfo.CODEC, HoverEvent.EntityTooltipInfo::legacyCreate
      );
      public static final Codec<HoverEvent.Action<?>> UNSAFE_CODEC = StringRepresentable.fromValues(
         () -> new HoverEvent.Action[]{SHOW_TEXT, SHOW_ITEM, SHOW_ENTITY}
      );
      public static final Codec<HoverEvent.Action<?>> CODEC = UNSAFE_CODEC.validate(HoverEvent.Action::filterForSerialization);
      private final String name;
      private final boolean allowFromServer;
      final MapCodec<HoverEvent.TypedHoverEvent<T>> codec;
      final MapCodec<HoverEvent.TypedHoverEvent<T>> legacyCodec;

      public Action(String pName, boolean pAllowFromServer, Codec<T> pCodec, final HoverEvent.LegacyConverter<T> pLegacyConverter) {
         this.name = pName;
         this.allowFromServer = pAllowFromServer;
         this.codec = pCodec.xmap(p_308563_ -> new HoverEvent.TypedHoverEvent<>(this, (T)p_308563_), p_308564_ -> p_308564_.value).fieldOf("contents");
         this.legacyCodec = (new Codec<HoverEvent.TypedHoverEvent<T>>() {
            public <D> DataResult<Pair<HoverEvent.TypedHoverEvent<T>, D>> decode(DynamicOps<D> p_333314_, D p_328005_) {
               return ComponentSerialization.CODEC.decode(p_333314_, p_328005_).flatMap(p_332506_ -> {
                  DataResult<T> $$4;
                  if (p_333314_ instanceof RegistryOps<D> $$3) {
                     $$4 = pLegacyConverter.parse((Component)p_332506_.getFirst(), $$3);
                  } else {
                     $$4 = pLegacyConverter.parse((Component)p_332506_.getFirst(), null);
                  }

                  return $$4.map(p_335904_ -> Pair.of(new HoverEvent.TypedHoverEvent<>(Action.this, p_335904_), p_332506_.getSecond()));
               });
            }

            public <D> DataResult<D> encode(HoverEvent.TypedHoverEvent<T> p_328207_, DynamicOps<D> p_331514_, D p_330476_) {
               return DataResult.error(() -> "Can't encode in legacy format");
            }
         }).fieldOf("value");
      }

      public boolean isAllowedFromServer() {
         return this.allowFromServer;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      T cast(Object pParameter) {
         return (T)pParameter;
      }

      @Override
      public String toString() {
         return "<action " + this.name + ">";
      }

      private static DataResult<HoverEvent.Action<?>> filterForSerialization(@Nullable HoverEvent.Action<?> p_311888_) {
         if (p_311888_ == null) {
            return DataResult.error(() -> "Unknown action");
         } else {
            return !p_311888_.isAllowedFromServer()
               ? DataResult.error(() -> "Action not allowed: " + p_311888_)
               : DataResult.success(p_311888_, Lifecycle.stable());
         }
      }
   }

   public static class EntityTooltipInfo {
      public static final Codec<HoverEvent.EntityTooltipInfo> CODEC = RecordCodecBuilder.create(
         p_341102_ -> p_341102_.group(
                  BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(p_309982_ -> p_309982_.type),
                  UUIDUtil.LENIENT_CODEC.fieldOf("id").forGetter(p_312795_ -> p_312795_.id),
                  ComponentSerialization.CODEC.lenientOptionalFieldOf("name").forGetter(p_310270_ -> p_310270_.name)
               )
               .apply(p_341102_, HoverEvent.EntityTooltipInfo::new)
      );
      public final EntityType<?> type;
      public final UUID id;
      public final Optional<Component> name;
      @Nullable
      private List<Component> linesCache;

      public EntityTooltipInfo(EntityType<?> pType, UUID pId, @Nullable Component pName) {
         this(pType, pId, Optional.ofNullable(pName));
      }

      public EntityTooltipInfo(EntityType<?> p_312321_, UUID p_312750_, Optional<Component> p_312078_) {
         this.type = p_312321_;
         this.id = p_312750_;
         this.name = p_312078_;
      }

      public static DataResult<HoverEvent.EntityTooltipInfo> legacyCreate(Component pName, @Nullable RegistryOps<?> pOps) {
         try {
            CompoundTag $$2 = TagParser.parseTag(pName.getString());
            DynamicOps<JsonElement> $$3 = (DynamicOps<JsonElement>)(pOps != null ? pOps.withParent(JsonOps.INSTANCE) : JsonOps.INSTANCE);
            DataResult<Component> $$4 = ComponentSerialization.CODEC.parse($$3, JsonParser.parseString($$2.getString("name")));
            EntityType<?> $$5 = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse($$2.getString("type")));
            UUID $$6 = UUID.fromString($$2.getString("id"));
            return $$4.map(p_326072_ -> new HoverEvent.EntityTooltipInfo($$5, $$6, p_326072_));
         } catch (Exception var7) {
            return DataResult.error(() -> "Failed to parse tooltip: " + var7.getMessage());
         }
      }

      public List<Component> getTooltipLines() {
         if (this.linesCache == null) {
            this.linesCache = new ArrayList<>();
            this.name.ifPresent(this.linesCache::add);
            this.linesCache.add(Component.translatable("gui.entity_tooltip.type", this.type.getDescription()));
            this.linesCache.add(Component.literal(this.id.toString()));
         }

         return this.linesCache;
      }

      @Override
      public boolean equals(Object pOther) {
         if (this == pOther) {
            return true;
         } else if (pOther != null && this.getClass() == pOther.getClass()) {
            HoverEvent.EntityTooltipInfo $$1 = (HoverEvent.EntityTooltipInfo)pOther;
            return this.type.equals($$1.type) && this.id.equals($$1.id) && this.name.equals($$1.name);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int $$0 = this.type.hashCode();
         $$0 = 31 * $$0 + this.id.hashCode();
         return 31 * $$0 + this.name.hashCode();
      }
   }

   public static class ItemStackInfo {
      public static final Codec<HoverEvent.ItemStackInfo> FULL_CODEC = ItemStack.CODEC
         .xmap(HoverEvent.ItemStackInfo::new, HoverEvent.ItemStackInfo::getItemStack);
      private static final Codec<HoverEvent.ItemStackInfo> SIMPLE_CODEC = ItemStack.SIMPLE_ITEM_CODEC
         .xmap(HoverEvent.ItemStackInfo::new, HoverEvent.ItemStackInfo::getItemStack);
      public static final Codec<HoverEvent.ItemStackInfo> CODEC = Codec.withAlternative(FULL_CODEC, SIMPLE_CODEC);
      private final Holder<Item> item;
      private final int count;
      private final DataComponentPatch components;
      @Nullable
      private ItemStack itemStack;

      ItemStackInfo(Holder<Item> pItem, int pCount, DataComponentPatch pComponents) {
         this.item = pItem;
         this.count = pCount;
         this.components = pComponents;
      }

      public ItemStackInfo(ItemStack p_130897_) {
         this(p_130897_.getItemHolder(), p_130897_.getCount(), p_130897_.getComponentsPatch());
      }

      @Override
      public boolean equals(Object pOther) {
         if (this == pOther) {
            return true;
         } else if (pOther != null && this.getClass() == pOther.getClass()) {
            HoverEvent.ItemStackInfo $$1 = (HoverEvent.ItemStackInfo)pOther;
            return this.count == $$1.count && this.item.equals($$1.item) && this.components.equals($$1.components);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int $$0 = this.item.hashCode();
         $$0 = 31 * $$0 + this.count;
         return 31 * $$0 + this.components.hashCode();
      }

      public ItemStack getItemStack() {
         if (this.itemStack == null) {
            this.itemStack = new ItemStack(this.item, this.count, this.components);
         }

         return this.itemStack;
      }

      private static DataResult<HoverEvent.ItemStackInfo> legacyCreate(Component pName, @Nullable RegistryOps<?> pOps) {
         try {
            CompoundTag $$2 = TagParser.parseTag(pName.getString());
            DynamicOps<Tag> $$3 = (DynamicOps<Tag>)(pOps != null ? pOps.withParent(NbtOps.INSTANCE) : NbtOps.INSTANCE);
            return ItemStack.CODEC.parse($$3, $$2).map(HoverEvent.ItemStackInfo::new);
         } catch (CommandSyntaxException var4) {
            return DataResult.error(() -> "Failed to parse item tag: " + var4.getMessage());
         }
      }
   }

   public interface LegacyConverter<T> {
      DataResult<T> parse(Component pName, @Nullable RegistryOps<?> pOps);
   }

   static record TypedHoverEvent<T>(HoverEvent.Action<T> action, T value) {
      public static final MapCodec<HoverEvent.TypedHoverEvent<?>> CODEC = HoverEvent.Action.CODEC
         .dispatchMap("action", HoverEvent.TypedHoverEvent::action, p_326074_ -> p_326074_.codec);
      public static final MapCodec<HoverEvent.TypedHoverEvent<?>> LEGACY_CODEC = HoverEvent.Action.CODEC
         .dispatchMap("action", HoverEvent.TypedHoverEvent::action, p_326073_ -> p_326073_.legacyCodec);
   }
}
