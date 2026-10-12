package net.minecraft.commands.synchronization;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;

public class ArgumentUtils {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final byte NUMBER_FLAG_MIN = 1;
   private static final byte NUMBER_FLAG_MAX = 2;

   public static int createNumberFlags(boolean pMin, boolean pMax) {
      int $$2 = 0;
      if (pMin) {
         $$2 |= 1;
      }

      if (pMax) {
         $$2 |= 2;
      }

      return $$2;
   }

   public static boolean numberHasMin(byte pNumber) {
      return (pNumber & 1) != 0;
   }

   public static boolean numberHasMax(byte pNumber) {
      return (pNumber & 2) != 0;
   }

   private static <A extends ArgumentType<?>> void serializeCap(JsonObject pJson, ArgumentTypeInfo.Template<A> pTemplate) {
      serializeCap(pJson, pTemplate.type(), pTemplate);
   }

   private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeCap(
      JsonObject pJson, ArgumentTypeInfo<A, T> pArgumentTypeInfo, ArgumentTypeInfo.Template<A> pTemplate
   ) {
      pArgumentTypeInfo.serializeToJson((T)pTemplate, pJson);
   }

   private static <T extends ArgumentType<?>> void serializeArgumentToJson(JsonObject pJson, T pType) {
      ArgumentTypeInfo.Template<T> $$2 = ArgumentTypeInfos.unpack(pType);
      pJson.addProperty("type", "argument");
      pJson.addProperty("parser", BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey($$2.type()).toString());
      JsonObject $$3 = new JsonObject();
      serializeCap($$3, $$2);
      if ($$3.size() > 0) {
         pJson.add("properties", $$3);
      }
   }

   public static <S> JsonObject serializeNodeToJson(CommandDispatcher<S> pDispatcher, CommandNode<S> pNode) {
      JsonObject $$2 = new JsonObject();
      if (pNode instanceof RootCommandNode) {
         $$2.addProperty("type", "root");
      } else if (pNode instanceof LiteralCommandNode) {
         $$2.addProperty("type", "literal");
      } else if (pNode instanceof ArgumentCommandNode<?, ?> $$3) {
         serializeArgumentToJson($$2, $$3.getType());
      } else {
         LOGGER.error("Could not serialize node {} ({})!", pNode, pNode.getClass());
         $$2.addProperty("type", "unknown");
      }

      JsonObject $$4 = new JsonObject();

      for (CommandNode<S> $$5 : pNode.getChildren()) {
         $$4.add($$5.getName(), serializeNodeToJson(pDispatcher, $$5));
      }

      if ($$4.size() > 0) {
         $$2.add("children", $$4);
      }

      if (pNode.getCommand() != null) {
         $$2.addProperty("executable", true);
      }

      if (pNode.getRedirect() != null) {
         Collection<String> $$6 = pDispatcher.getPath(pNode.getRedirect());
         if (!$$6.isEmpty()) {
            JsonArray $$7 = new JsonArray();

            for (String $$8 : $$6) {
               $$7.add($$8);
            }

            $$2.add("redirect", $$7);
         }
      }

      return $$2;
   }

   public static <T> Set<ArgumentType<?>> findUsedArgumentTypes(CommandNode<T> pNode) {
      Set<CommandNode<T>> $$1 = Sets.newIdentityHashSet();
      Set<ArgumentType<?>> $$2 = Sets.newHashSet();
      findUsedArgumentTypes(pNode, $$2, $$1);
      return $$2;
   }

   private static <T> void findUsedArgumentTypes(CommandNode<T> pNode, Set<ArgumentType<?>> pTypes, Set<CommandNode<T>> pNodes) {
      if (pNodes.add(pNode)) {
         if (pNode instanceof ArgumentCommandNode<?, ?> $$3) {
            pTypes.add($$3.getType());
         }

         pNode.getChildren().forEach(p_235426_ -> findUsedArgumentTypes(p_235426_, pTypes, pNodes));
         CommandNode<T> $$4 = pNode.getRedirect();
         if ($$4 != null) {
            findUsedArgumentTypes($$4, pTypes, pNodes);
         }
      }
   }
}
