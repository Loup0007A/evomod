package net.minecraft.network.protocol.game;

import com.google.common.collect.Queues;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.List;
import java.util.Queue;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceLocation;

public class ClientboundCommandsPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundCommandsPacket> STREAM_CODEC = Packet.codec(
      ClientboundCommandsPacket::write, ClientboundCommandsPacket::new
   );
   private static final byte MASK_TYPE = 3;
   private static final byte FLAG_EXECUTABLE = 4;
   private static final byte FLAG_REDIRECT = 8;
   private static final byte FLAG_CUSTOM_SUGGESTIONS = 16;
   private static final byte TYPE_ROOT = 0;
   private static final byte TYPE_LITERAL = 1;
   private static final byte TYPE_ARGUMENT = 2;
   private final int rootIndex;
   private final List<ClientboundCommandsPacket.Entry> entries;

   public ClientboundCommandsPacket(RootCommandNode<SharedSuggestionProvider> pRoot) {
      Object2IntMap<CommandNode<SharedSuggestionProvider>> $$1 = enumerateNodes(pRoot);
      this.entries = createEntries($$1);
      this.rootIndex = $$1.getInt(pRoot);
   }

   private ClientboundCommandsPacket(FriendlyByteBuf p_178805_) {
      this.entries = p_178805_.readList(ClientboundCommandsPacket::readNode);
      this.rootIndex = p_178805_.readVarInt();
      validateEntries(this.entries);
   }

   private void write(FriendlyByteBuf p_131886_) {
      p_131886_.writeCollection(this.entries, (p_237642_, p_237643_) -> p_237643_.write(p_237642_));
      p_131886_.writeVarInt(this.rootIndex);
   }

   private static void validateEntries(List<ClientboundCommandsPacket.Entry> pEntries, BiPredicate<ClientboundCommandsPacket.Entry, IntSet> pValidator) {
      IntSet $$2 = new IntOpenHashSet(IntSets.fromTo(0, pEntries.size()));

      while (!$$2.isEmpty()) {
         boolean $$3 = $$2.removeIf(p_237637_ -> pValidator.test(pEntries.get(p_237637_), $$2));
         if (!$$3) {
            throw new IllegalStateException("Server sent an impossible command tree");
         }
      }
   }

   private static void validateEntries(List<ClientboundCommandsPacket.Entry> pEntries) {
      validateEntries(pEntries, ClientboundCommandsPacket.Entry::canBuild);
      validateEntries(pEntries, ClientboundCommandsPacket.Entry::canResolve);
   }

   private static Object2IntMap<CommandNode<SharedSuggestionProvider>> enumerateNodes(RootCommandNode<SharedSuggestionProvider> pRootNode) {
      Object2IntMap<CommandNode<SharedSuggestionProvider>> $$1 = new Object2IntOpenHashMap();
      Queue<CommandNode<SharedSuggestionProvider>> $$2 = Queues.newArrayDeque();
      $$2.add(pRootNode);

      CommandNode<SharedSuggestionProvider> $$3;
      while (($$3 = $$2.poll()) != null) {
         if (!$$1.containsKey($$3)) {
            int $$4 = $$1.size();
            $$1.put($$3, $$4);
            $$2.addAll($$3.getChildren());
            if ($$3.getRedirect() != null) {
               $$2.add($$3.getRedirect());
            }
         }
      }

      return $$1;
   }

   private static List<ClientboundCommandsPacket.Entry> createEntries(Object2IntMap<CommandNode<SharedSuggestionProvider>> pNodes) {
      ObjectArrayList<ClientboundCommandsPacket.Entry> $$1 = new ObjectArrayList(pNodes.size());
      $$1.size(pNodes.size());
      ObjectIterator var2 = Object2IntMaps.fastIterable(pNodes).iterator();

      while (var2.hasNext()) {
         it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CommandNode<SharedSuggestionProvider>> $$2 = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CommandNode<SharedSuggestionProvider>>)var2.next();
         $$1.set($$2.getIntValue(), createEntry((CommandNode<SharedSuggestionProvider>)$$2.getKey(), pNodes));
      }

      return $$1;
   }

   private static ClientboundCommandsPacket.Entry readNode(FriendlyByteBuf p_131888_) {
      byte $$1 = p_131888_.readByte();
      int[] $$2 = p_131888_.readVarIntArray();
      int $$3 = ($$1 & 8) != 0 ? p_131888_.readVarInt() : 0;
      ClientboundCommandsPacket.NodeStub $$4 = read(p_131888_, $$1);
      return new ClientboundCommandsPacket.Entry($$4, $$1, $$3, $$2);
   }

   @Nullable
   private static ClientboundCommandsPacket.NodeStub read(FriendlyByteBuf pBuffer, byte pFlags) {
      int $$2 = pFlags & 3;
      if ($$2 == 2) {
         String $$3 = pBuffer.readUtf();
         int $$4 = pBuffer.readVarInt();
         ArgumentTypeInfo<?, ?> $$5 = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.byId($$4);
         if ($$5 == null) {
            return null;
         } else {
            ArgumentTypeInfo.Template<?> $$6 = $$5.deserializeFromNetwork(pBuffer);
            ResourceLocation $$7 = (pFlags & 16) != 0 ? pBuffer.readResourceLocation() : null;
            return new ClientboundCommandsPacket.ArgumentNodeStub($$3, $$6, $$7);
         }
      } else if ($$2 == 1) {
         String $$8 = pBuffer.readUtf();
         return new ClientboundCommandsPacket.LiteralNodeStub($$8);
      } else {
         return null;
      }
   }

   private static ClientboundCommandsPacket.Entry createEntry(
      CommandNode<SharedSuggestionProvider> pNode, Object2IntMap<CommandNode<SharedSuggestionProvider>> pNodes
   ) {
      int $$2 = 0;
      int $$3;
      if (pNode.getRedirect() != null) {
         $$2 |= 8;
         $$3 = pNodes.getInt(pNode.getRedirect());
      } else {
         $$3 = 0;
      }

      if (pNode.getCommand() != null) {
         $$2 |= 4;
      }

      ClientboundCommandsPacket.NodeStub $$5;
      if (pNode instanceof RootCommandNode) {
         $$2 |= 0;
         $$5 = null;
      } else if (pNode instanceof ArgumentCommandNode<SharedSuggestionProvider, ?> $$6) {
         $$5 = new ClientboundCommandsPacket.ArgumentNodeStub($$6);
         $$2 |= 2;
         if ($$6.getCustomSuggestions() != null) {
            $$2 |= 16;
         }
      } else {
         if (!(pNode instanceof LiteralCommandNode $$8)) {
            throw new UnsupportedOperationException("Unknown node type " + pNode);
         }

         $$5 = new ClientboundCommandsPacket.LiteralNodeStub($$8.getLiteral());
         $$2 |= 1;
      }

      int[] $$11 = pNode.getChildren().stream().mapToInt(pNodes::getInt).toArray();
      return new ClientboundCommandsPacket.Entry($$5, $$2, $$3, $$11);
   }

   @Override
   public PacketType<ClientboundCommandsPacket> type() {
      return GamePacketTypes.CLIENTBOUND_COMMANDS;
   }

   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleCommands(this);
   }

   public RootCommandNode<SharedSuggestionProvider> getRoot(CommandBuildContext pContext) {
      return (RootCommandNode<SharedSuggestionProvider>)new ClientboundCommandsPacket.NodeResolver(pContext, this.entries).resolve(this.rootIndex);
   }

   static class ArgumentNodeStub implements ClientboundCommandsPacket.NodeStub {
      private final String id;
      private final ArgumentTypeInfo.Template<?> argumentType;
      @Nullable
      private final ResourceLocation suggestionId;

      @Nullable
      private static ResourceLocation getSuggestionId(@Nullable SuggestionProvider<SharedSuggestionProvider> pProvider) {
         return pProvider != null ? SuggestionProviders.getName(pProvider) : null;
      }

      ArgumentNodeStub(String pId, ArgumentTypeInfo.Template<?> pArgumentType, @Nullable ResourceLocation pSuggestionId) {
         this.id = pId;
         this.argumentType = pArgumentType;
         this.suggestionId = pSuggestionId;
      }

      public ArgumentNodeStub(ArgumentCommandNode<SharedSuggestionProvider, ?> pArgumentNode) {
         this(pArgumentNode.getName(), ArgumentTypeInfos.unpack(pArgumentNode.getType()), getSuggestionId(pArgumentNode.getCustomSuggestions()));
      }

      @Override
      public ArgumentBuilder<SharedSuggestionProvider, ?> build(CommandBuildContext pContext) {
         ArgumentType<?> $$1 = this.argumentType.instantiate(pContext);
         RequiredArgumentBuilder<SharedSuggestionProvider, ?> $$2 = RequiredArgumentBuilder.argument(this.id, $$1);
         if (this.suggestionId != null) {
            $$2.suggests(SuggestionProviders.getProvider(this.suggestionId));
         }

         return $$2;
      }

      @Override
      public void write(FriendlyByteBuf pBuffer) {
         pBuffer.writeUtf(this.id);
         serializeCap(pBuffer, this.argumentType);
         if (this.suggestionId != null) {
            pBuffer.writeResourceLocation(this.suggestionId);
         }
      }

      private static <A extends ArgumentType<?>> void serializeCap(FriendlyByteBuf pBuffer, ArgumentTypeInfo.Template<A> pArgumentInfoTemplate) {
         serializeCap(pBuffer, pArgumentInfoTemplate.type(), pArgumentInfoTemplate);
      }

      private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeCap(
         FriendlyByteBuf pBuffer, ArgumentTypeInfo<A, T> pArgumentInfo, ArgumentTypeInfo.Template<A> pArgumentInfoTemplate
      ) {
         pBuffer.writeVarInt(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(pArgumentInfo));
         pArgumentInfo.serializeToNetwork((T)pArgumentInfoTemplate, pBuffer);
      }
   }

   static class Entry {
      @Nullable
      final ClientboundCommandsPacket.NodeStub stub;
      final int flags;
      final int redirect;
      final int[] children;

      Entry(@Nullable ClientboundCommandsPacket.NodeStub pStub, int pFlags, int pRedirect, int[] pChildren) {
         this.stub = pStub;
         this.flags = pFlags;
         this.redirect = pRedirect;
         this.children = pChildren;
      }

      public void write(FriendlyByteBuf pBuffer) {
         pBuffer.writeByte(this.flags);
         pBuffer.writeVarIntArray(this.children);
         if ((this.flags & 8) != 0) {
            pBuffer.writeVarInt(this.redirect);
         }

         if (this.stub != null) {
            this.stub.write(pBuffer);
         }
      }

      public boolean canBuild(IntSet pChildren) {
         return (this.flags & 8) != 0 ? !pChildren.contains(this.redirect) : true;
      }

      public boolean canResolve(IntSet pChildren) {
         for (int $$1 : this.children) {
            if (pChildren.contains($$1)) {
               return false;
            }
         }

         return true;
      }
   }

   static class LiteralNodeStub implements ClientboundCommandsPacket.NodeStub {
      private final String id;

      LiteralNodeStub(String pId) {
         this.id = pId;
      }

      @Override
      public ArgumentBuilder<SharedSuggestionProvider, ?> build(CommandBuildContext pContext) {
         return LiteralArgumentBuilder.literal(this.id);
      }

      @Override
      public void write(FriendlyByteBuf pBuffer) {
         pBuffer.writeUtf(this.id);
      }
   }

   static class NodeResolver {
      private final CommandBuildContext context;
      private final List<ClientboundCommandsPacket.Entry> entries;
      private final List<CommandNode<SharedSuggestionProvider>> nodes;

      NodeResolver(CommandBuildContext pContext, List<ClientboundCommandsPacket.Entry> pEntries) {
         this.context = pContext;
         this.entries = pEntries;
         ObjectArrayList<CommandNode<SharedSuggestionProvider>> $$2 = new ObjectArrayList();
         $$2.size(pEntries.size());
         this.nodes = $$2;
      }

      public CommandNode<SharedSuggestionProvider> resolve(int pIndex) {
         CommandNode<SharedSuggestionProvider> $$1 = this.nodes.get(pIndex);
         if ($$1 != null) {
            return $$1;
         } else {
            ClientboundCommandsPacket.Entry $$2 = this.entries.get(pIndex);
            CommandNode<SharedSuggestionProvider> $$3;
            if ($$2.stub == null) {
               $$3 = new RootCommandNode();
            } else {
               ArgumentBuilder<SharedSuggestionProvider, ?> $$4 = $$2.stub.build(this.context);
               if (($$2.flags & 8) != 0) {
                  $$4.redirect(this.resolve($$2.redirect));
               }

               if (($$2.flags & 4) != 0) {
                  $$4.executes(p_237694_ -> 0);
               }

               $$3 = $$4.build();
            }

            this.nodes.set(pIndex, $$3);

            for (int $$6 : $$2.children) {
               CommandNode<SharedSuggestionProvider> $$7 = this.resolve($$6);
               if (!($$7 instanceof RootCommandNode)) {
                  $$3.addChild($$7);
               }
            }

            return $$3;
         }
      }
   }

   interface NodeStub {
      ArgumentBuilder<SharedSuggestionProvider, ?> build(CommandBuildContext pContext);

      void write(FriendlyByteBuf pBuffer);
   }
}
