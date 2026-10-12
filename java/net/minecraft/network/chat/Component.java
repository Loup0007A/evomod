package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.Message;
import com.mojang.serialization.JsonOps;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.contents.DataSource;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ChunkPos;

public interface Component extends Message, FormattedText {
   Style getStyle();

   ComponentContents getContents();

   @Override
   default String getString() {
      return FormattedText.super.getString();
   }

   default String getString(int pMaxLength) {
      StringBuilder $$1 = new StringBuilder();
      this.visit(p_130673_ -> {
         int $$3 = pMaxLength - $$1.length();
         if ($$3 <= 0) {
            return STOP_ITERATION;
         } else {
            $$1.append(p_130673_.length() <= $$3 ? p_130673_ : p_130673_.substring(0, $$3));
            return Optional.empty();
         }
      });
      return $$1.toString();
   }

   List<Component> getSiblings();

   @Nullable
   default String tryCollapseToString() {
      if (this.getContents() instanceof PlainTextContents $$0 && this.getSiblings().isEmpty() && this.getStyle().isEmpty()) {
         return $$0.text();
      }

      return null;
   }

   default MutableComponent plainCopy() {
      return MutableComponent.create(this.getContents());
   }

   default MutableComponent copy() {
      return new MutableComponent(this.getContents(), new ArrayList<>(this.getSiblings()), this.getStyle());
   }

   FormattedCharSequence getVisualOrderText();

   @Override
   default <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> pAcceptor, Style pStyle) {
      Style $$2 = this.getStyle().applyTo(pStyle);
      Optional<T> $$3 = this.getContents().visit(pAcceptor, $$2);
      if ($$3.isPresent()) {
         return $$3;
      } else {
         for (Component $$4 : this.getSiblings()) {
            Optional<T> $$5 = $$4.visit(pAcceptor, $$2);
            if ($$5.isPresent()) {
               return $$5;
            }
         }

         return Optional.empty();
      }
   }

   @Override
   default <T> Optional<T> visit(FormattedText.ContentConsumer<T> pAcceptor) {
      Optional<T> $$1 = this.getContents().visit(pAcceptor);
      if ($$1.isPresent()) {
         return $$1;
      } else {
         for (Component $$2 : this.getSiblings()) {
            Optional<T> $$3 = $$2.visit(pAcceptor);
            if ($$3.isPresent()) {
               return $$3;
            }
         }

         return Optional.empty();
      }
   }

   default List<Component> toFlatList() {
      return this.toFlatList(Style.EMPTY);
   }

   default List<Component> toFlatList(Style pStyle) {
      List<Component> $$1 = Lists.newArrayList();
      this.visit((p_178403_, p_178404_) -> {
         if (!p_178404_.isEmpty()) {
            $$1.add(literal(p_178404_).withStyle(p_178403_));
         }

         return Optional.empty();
      }, pStyle);
      return $$1;
   }

   default boolean contains(Component pOther) {
      if (this.equals(pOther)) {
         return true;
      } else {
         List<Component> $$1 = this.toFlatList();
         List<Component> $$2 = pOther.toFlatList(this.getStyle());
         return Collections.indexOfSubList($$1, $$2) != -1;
      }
   }

   static Component nullToEmpty(@Nullable String pText) {
      return (Component)(pText != null ? literal(pText) : CommonComponents.EMPTY);
   }

   static MutableComponent literal(String pText) {
      return MutableComponent.create(PlainTextContents.create(pText));
   }

   static MutableComponent translatable(String pKey) {
      return MutableComponent.create(new TranslatableContents(pKey, null, TranslatableContents.NO_ARGS));
   }

   static MutableComponent translatable(String pKey, Object... pArgs) {
      return MutableComponent.create(new TranslatableContents(pKey, null, pArgs));
   }

   static MutableComponent translatableEscape(String pKey, Object... pArgs) {
      for (int $$2 = 0; $$2 < pArgs.length; $$2++) {
         Object $$3 = pArgs[$$2];
         if (!TranslatableContents.isAllowedPrimitiveArgument($$3) && !($$3 instanceof Component)) {
            pArgs[$$2] = String.valueOf($$3);
         }
      }

      return translatable(pKey, pArgs);
   }

   static MutableComponent translatableWithFallback(String pKey, @Nullable String pFallback) {
      return MutableComponent.create(new TranslatableContents(pKey, pFallback, TranslatableContents.NO_ARGS));
   }

   static MutableComponent translatableWithFallback(String pKey, @Nullable String pFallback, Object... pArgs) {
      return MutableComponent.create(new TranslatableContents(pKey, pFallback, pArgs));
   }

   static MutableComponent empty() {
      return MutableComponent.create(PlainTextContents.EMPTY);
   }

   static MutableComponent keybind(String pName) {
      return MutableComponent.create(new KeybindContents(pName));
   }

   static MutableComponent nbt(String pNbtPathPattern, boolean pInterpreting, Optional<Component> pSeparator, DataSource pDataSource) {
      return MutableComponent.create(new NbtContents(pNbtPathPattern, pInterpreting, pSeparator, pDataSource));
   }

   static MutableComponent score(String pName, String pObjective) {
      return MutableComponent.create(new ScoreContents(pName, pObjective));
   }

   static MutableComponent selector(String pPattern, Optional<Component> pSeparator) {
      return MutableComponent.create(new SelectorContents(pPattern, pSeparator));
   }

   static Component translationArg(Date pDate) {
      return literal(pDate.toString());
   }

   static Component translationArg(Message pMessage) {
      return (Component)(pMessage instanceof Component $$1 ? $$1 : literal(pMessage.getString()));
   }

   static Component translationArg(UUID pUuid) {
      return literal(pUuid.toString());
   }

   static Component translationArg(ResourceLocation pLocation) {
      return literal(pLocation.toString());
   }

   static Component translationArg(ChunkPos pChunkPos) {
      return literal(pChunkPos.toString());
   }

   static Component translationArg(URI pUri) {
      return literal(pUri.toString());
   }

   public static class Serializer {
      private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

      private Serializer() {
      }

      static MutableComponent deserialize(JsonElement pJson, HolderLookup.Provider pProvider) {
         return (MutableComponent)ComponentSerialization.CODEC
            .parse(pProvider.createSerializationContext(JsonOps.INSTANCE), pJson)
            .getOrThrow(JsonParseException::new);
      }

      static JsonElement serialize(Component pComponent, HolderLookup.Provider pProvider) {
         return (JsonElement)ComponentSerialization.CODEC
            .encodeStart(pProvider.createSerializationContext(JsonOps.INSTANCE), pComponent)
            .getOrThrow(JsonParseException::new);
      }

      public static String toJson(Component pComponent, HolderLookup.Provider pRegistries) {
         return GSON.toJson(serialize(pComponent, pRegistries));
      }

      @Nullable
      public static MutableComponent fromJson(String pJson, HolderLookup.Provider pRegistries) {
         JsonElement $$2 = JsonParser.parseString(pJson);
         return $$2 == null ? null : deserialize($$2, pRegistries);
      }

      @Nullable
      public static MutableComponent fromJson(@Nullable JsonElement pJson, HolderLookup.Provider pRegistries) {
         return pJson == null ? null : deserialize(pJson, pRegistries);
      }

      @Nullable
      public static MutableComponent fromJsonLenient(String pJson, HolderLookup.Provider pRegistries) {
         JsonReader $$2 = new JsonReader(new StringReader(pJson));
         $$2.setLenient(true);
         JsonElement $$3 = JsonParser.parseReader($$2);
         return $$3 == null ? null : deserialize($$3, pRegistries);
      }
   }

   public static class SerializerAdapter implements JsonDeserializer<MutableComponent>, JsonSerializer<Component> {
      private final HolderLookup.Provider registries;

      public SerializerAdapter(HolderLookup.Provider pRegistries) {
         this.registries = pRegistries;
      }

      public MutableComponent deserialize(JsonElement pJson, Type pTypeOfT, JsonDeserializationContext pContext) throws JsonParseException {
         return Component.Serializer.deserialize(pJson, this.registries);
      }

      public JsonElement serialize(Component pSrc, Type pTypeOfSrc, JsonSerializationContext pContext) {
         return Component.Serializer.serialize(pSrc, this.registries);
      }
   }
}
