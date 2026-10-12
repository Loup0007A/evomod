package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class LootCommand {
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_LOOT_TABLE = (p_326304_, p_326305_) -> {
      ReloadableServerRegistries.Holder $$2 = ((CommandSourceStack)p_326304_.getSource()).getServer().reloadableRegistries();
      return SharedSuggestionProvider.suggestResource($$2.getKeys(Registries.LOOT_TABLE), p_326305_);
   };
   private static final DynamicCommandExceptionType ERROR_NO_HELD_ITEMS = new DynamicCommandExceptionType(
      p_308774_ -> Component.translatableEscape("commands.drop.no_held_items", p_308774_)
   );
   private static final DynamicCommandExceptionType ERROR_NO_LOOT_TABLE = new DynamicCommandExceptionType(
      p_308775_ -> Component.translatableEscape("commands.drop.no_loot_table", p_308775_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
      pDispatcher.register(
         addTargets(
            (LiteralArgumentBuilder)Commands.literal("loot").requires(p_137937_ -> p_137937_.hasPermission(2)),
            (p_214520_, p_214521_) -> p_214520_.then(
                     Commands.literal("fish")
                        .then(
                           Commands.argument("loot_table", ResourceOrIdArgument.lootTable(pContext))
                              .suggests(SUGGEST_LOOT_TABLE)
                              .then(
                                 ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                                "pos", BlockPosArgument.blockPos()
                                             )
                                             .executes(
                                                p_326309_ -> dropFishingLoot(
                                                      p_326309_,
                                                      ResourceOrIdArgument.getLootTable(p_326309_, "loot_table"),
                                                      BlockPosArgument.getLoadedBlockPos(p_326309_, "pos"),
                                                      ItemStack.EMPTY,
                                                      p_214521_
                                                   )
                                             ))
                                          .then(
                                             Commands.argument("tool", ItemArgument.item(pContext))
                                                .executes(
                                                   p_326303_ -> dropFishingLoot(
                                                         p_326303_,
                                                         ResourceOrIdArgument.getLootTable(p_326303_, "loot_table"),
                                                         BlockPosArgument.getLoadedBlockPos(p_326303_, "pos"),
                                                         ItemArgument.getItem(p_326303_, "tool").createItemStack(1, false),
                                                         p_214521_
                                                      )
                                                )
                                          ))
                                       .then(
                                          Commands.literal("mainhand")
                                             .executes(
                                                p_326295_ -> dropFishingLoot(
                                                      p_326295_,
                                                      ResourceOrIdArgument.getLootTable(p_326295_, "loot_table"),
                                                      BlockPosArgument.getLoadedBlockPos(p_326295_, "pos"),
                                                      getSourceHandItem((CommandSourceStack)p_326295_.getSource(), EquipmentSlot.MAINHAND),
                                                      p_214521_
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("offhand")
                                          .executes(
                                             p_326299_ -> dropFishingLoot(
                                                   p_326299_,
                                                   ResourceOrIdArgument.getLootTable(p_326299_, "loot_table"),
                                                   BlockPosArgument.getLoadedBlockPos(p_326299_, "pos"),
                                                   getSourceHandItem((CommandSourceStack)p_326299_.getSource(), EquipmentSlot.OFFHAND),
                                                   p_214521_
                                                )
                                          )
                                    )
                              )
                        )
                  )
                  .then(
                     Commands.literal("loot")
                        .then(
                           Commands.argument("loot_table", ResourceOrIdArgument.lootTable(pContext))
                              .suggests(SUGGEST_LOOT_TABLE)
                              .executes(p_326301_ -> dropChestLoot(p_326301_, ResourceOrIdArgument.getLootTable(p_326301_, "loot_table"), p_214521_))
                        )
                  )
                  .then(
                     Commands.literal("kill")
                        .then(
                           Commands.argument("target", EntityArgument.entity())
                              .executes(p_180406_ -> dropKillLoot(p_180406_, EntityArgument.getEntity(p_180406_, "target"), p_214521_))
                        )
                  )
                  .then(
                     Commands.literal("mine")
                        .then(
                           ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("pos", BlockPosArgument.blockPos())
                                       .executes(
                                          p_180403_ -> dropBlockLoot(
                                                p_180403_, BlockPosArgument.getLoadedBlockPos(p_180403_, "pos"), ItemStack.EMPTY, p_214521_
                                             )
                                       ))
                                    .then(
                                       Commands.argument("tool", ItemArgument.item(pContext))
                                          .executes(
                                             p_180400_ -> dropBlockLoot(
                                                   p_180400_,
                                                   BlockPosArgument.getLoadedBlockPos(p_180400_, "pos"),
                                                   ItemArgument.getItem(p_180400_, "tool").createItemStack(1, false),
                                                   p_214521_
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("mainhand")
                                       .executes(
                                          p_180397_ -> dropBlockLoot(
                                                p_180397_,
                                                BlockPosArgument.getLoadedBlockPos(p_180397_, "pos"),
                                                getSourceHandItem((CommandSourceStack)p_180397_.getSource(), EquipmentSlot.MAINHAND),
                                                p_214521_
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("offhand")
                                    .executes(
                                       p_180394_ -> dropBlockLoot(
                                             p_180394_,
                                             BlockPosArgument.getLoadedBlockPos(p_180394_, "pos"),
                                             getSourceHandItem((CommandSourceStack)p_180394_.getSource(), EquipmentSlot.OFFHAND),
                                             p_214521_
                                          )
                                    )
                              )
                        )
                  )
         )
      );
   }

   private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addTargets(T pBuilder, LootCommand.TailProvider pTailProvider) {
      return (T)pBuilder.then(
            ((LiteralArgumentBuilder)Commands.literal("replace")
                  .then(
                     Commands.literal("entity")
                        .then(
                           Commands.argument("entities", EntityArgument.entities())
                              .then(
                                 pTailProvider.construct(
                                       Commands.argument("slot", SlotArgument.slot()),
                                       (p_138032_, p_138033_, p_138034_) -> entityReplace(
                                             EntityArgument.getEntities(p_138032_, "entities"),
                                             SlotArgument.getSlot(p_138032_, "slot"),
                                             p_138033_.size(),
                                             p_138033_,
                                             p_138034_
                                          )
                                    )
                                    .then(
                                       pTailProvider.construct(
                                          Commands.argument("count", IntegerArgumentType.integer(0)),
                                          (p_138025_, p_138026_, p_138027_) -> entityReplace(
                                                EntityArgument.getEntities(p_138025_, "entities"),
                                                SlotArgument.getSlot(p_138025_, "slot"),
                                                IntegerArgumentType.getInteger(p_138025_, "count"),
                                                p_138026_,
                                                p_138027_
                                             )
                                       )
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("block")
                     .then(
                        Commands.argument("targetPos", BlockPosArgument.blockPos())
                           .then(
                              pTailProvider.construct(
                                    Commands.argument("slot", SlotArgument.slot()),
                                    (p_138018_, p_138019_, p_138020_) -> blockReplace(
                                          (CommandSourceStack)p_138018_.getSource(),
                                          BlockPosArgument.getLoadedBlockPos(p_138018_, "targetPos"),
                                          SlotArgument.getSlot(p_138018_, "slot"),
                                          p_138019_.size(),
                                          p_138019_,
                                          p_138020_
                                       )
                                 )
                                 .then(
                                    pTailProvider.construct(
                                       Commands.argument("count", IntegerArgumentType.integer(0)),
                                       (p_138011_, p_138012_, p_138013_) -> blockReplace(
                                             (CommandSourceStack)p_138011_.getSource(),
                                             BlockPosArgument.getLoadedBlockPos(p_138011_, "targetPos"),
                                             IntegerArgumentType.getInteger(p_138011_, "slot"),
                                             IntegerArgumentType.getInteger(p_138011_, "count"),
                                             p_138012_,
                                             p_138013_
                                          )
                                    )
                                 )
                           )
                     )
               )
         )
         .then(
            Commands.literal("insert")
               .then(
                  pTailProvider.construct(
                     Commands.argument("targetPos", BlockPosArgument.blockPos()),
                     (p_138004_, p_138005_, p_138006_) -> blockDistribute(
                           (CommandSourceStack)p_138004_.getSource(), BlockPosArgument.getLoadedBlockPos(p_138004_, "targetPos"), p_138005_, p_138006_
                        )
                  )
               )
         )
         .then(
            Commands.literal("give")
               .then(
                  pTailProvider.construct(
                     Commands.argument("players", EntityArgument.players()),
                     (p_137992_, p_137993_, p_137994_) -> playerGive(EntityArgument.getPlayers(p_137992_, "players"), p_137993_, p_137994_)
                  )
               )
         )
         .then(
            Commands.literal("spawn")
               .then(
                  pTailProvider.construct(
                     Commands.argument("targetPos", Vec3Argument.vec3()),
                     (p_137918_, p_137919_, p_137920_) -> dropInWorld(
                           (CommandSourceStack)p_137918_.getSource(), Vec3Argument.getVec3(p_137918_, "targetPos"), p_137919_, p_137920_
                        )
                  )
               )
         );
   }

   private static Container getContainer(CommandSourceStack pSource, BlockPos pPos) throws CommandSyntaxException {
      BlockEntity $$2 = pSource.getLevel().getBlockEntity(pPos);
      if (!($$2 instanceof Container)) {
         throw ItemCommands.ERROR_TARGET_NOT_A_CONTAINER.create(pPos.getX(), pPos.getY(), pPos.getZ());
      } else {
         return (Container)$$2;
      }
   }

   private static int blockDistribute(CommandSourceStack pSource, BlockPos pPos, List<ItemStack> pItems, LootCommand.Callback pCallback) throws CommandSyntaxException {
      Container $$4 = getContainer(pSource, pPos);
      List<ItemStack> $$5 = Lists.newArrayListWithCapacity(pItems.size());

      for (ItemStack $$6 : pItems) {
         if (distributeToContainer($$4, $$6.copy())) {
            $$4.setChanged();
            $$5.add($$6);
         }
      }

      pCallback.accept($$5);
      return $$5.size();
   }

   private static boolean distributeToContainer(Container pContainer, ItemStack pItem) {
      boolean $$2 = false;

      for (int $$3 = 0; $$3 < pContainer.getContainerSize() && !pItem.isEmpty(); $$3++) {
         ItemStack $$4 = pContainer.getItem($$3);
         if (pContainer.canPlaceItem($$3, pItem)) {
            if ($$4.isEmpty()) {
               pContainer.setItem($$3, pItem);
               $$2 = true;
               break;
            }

            if (canMergeItems($$4, pItem)) {
               int $$5 = pItem.getMaxStackSize() - $$4.getCount();
               int $$6 = Math.min(pItem.getCount(), $$5);
               pItem.shrink($$6);
               $$4.grow($$6);
               $$2 = true;
            }
         }
      }

      return $$2;
   }

   private static int blockReplace(CommandSourceStack pSource, BlockPos pPos, int pSlot, int pNumSlots, List<ItemStack> pItems, LootCommand.Callback pCallback) throws CommandSyntaxException {
      Container $$6 = getContainer(pSource, pPos);
      int $$7 = $$6.getContainerSize();
      if (pSlot >= 0 && pSlot < $$7) {
         List<ItemStack> $$8 = Lists.newArrayListWithCapacity(pItems.size());

         for (int $$9 = 0; $$9 < pNumSlots; $$9++) {
            int $$10 = pSlot + $$9;
            ItemStack $$11 = $$9 < pItems.size() ? pItems.get($$9) : ItemStack.EMPTY;
            if ($$6.canPlaceItem($$10, $$11)) {
               $$6.setItem($$10, $$11);
               $$8.add($$11);
            }
         }

         pCallback.accept($$8);
         return $$8.size();
      } else {
         throw ItemCommands.ERROR_TARGET_INAPPLICABLE_SLOT.create(pSlot);
      }
   }

   private static boolean canMergeItems(ItemStack pFirst, ItemStack pSecond) {
      return pFirst.getCount() <= pFirst.getMaxStackSize() && ItemStack.isSameItemSameComponents(pFirst, pSecond);
   }

   private static int playerGive(Collection<ServerPlayer> pTargets, List<ItemStack> pItems, LootCommand.Callback pCallback) throws CommandSyntaxException {
      List<ItemStack> $$3 = Lists.newArrayListWithCapacity(pItems.size());

      for (ItemStack $$4 : pItems) {
         for (ServerPlayer $$5 : pTargets) {
            if ($$5.getInventory().add($$4.copy())) {
               $$3.add($$4);
            }
         }
      }

      pCallback.accept($$3);
      return $$3.size();
   }

   private static void setSlots(Entity pTarget, List<ItemStack> pItems, int pStartSlot, int pNumSlots, List<ItemStack> pSetItems) {
      for (int $$5 = 0; $$5 < pNumSlots; $$5++) {
         ItemStack $$6 = $$5 < pItems.size() ? pItems.get($$5) : ItemStack.EMPTY;
         SlotAccess $$7 = pTarget.getSlot(pStartSlot + $$5);
         if ($$7 != SlotAccess.NULL && $$7.set($$6.copy())) {
            pSetItems.add($$6);
         }
      }
   }

   private static int entityReplace(
      Collection<? extends Entity> pTargets, int pStartSlot, int pNumSlots, List<ItemStack> pItems, LootCommand.Callback pCallback
   ) throws CommandSyntaxException {
      List<ItemStack> $$5 = Lists.newArrayListWithCapacity(pItems.size());

      for (Entity $$6 : pTargets) {
         if ($$6 instanceof ServerPlayer $$7) {
            setSlots($$6, pItems, pStartSlot, pNumSlots, $$5);
            $$7.containerMenu.broadcastChanges();
         } else {
            setSlots($$6, pItems, pStartSlot, pNumSlots, $$5);
         }
      }

      pCallback.accept($$5);
      return $$5.size();
   }

   private static int dropInWorld(CommandSourceStack pSource, Vec3 pPos, List<ItemStack> pItems, LootCommand.Callback pCallback) throws CommandSyntaxException {
      ServerLevel $$4 = pSource.getLevel();
      pItems.forEach(p_137884_ -> {
         ItemEntity $$3 = new ItemEntity($$4, pPos.x, pPos.y, pPos.z, p_137884_.copy());
         $$3.setDefaultPickUpDelay();
         $$4.addFreshEntity($$3);
      });
      pCallback.accept(pItems);
      return pItems.size();
   }

   private static void callback(CommandSourceStack pSource, List<ItemStack> pItems) {
      if (pItems.size() == 1) {
         ItemStack $$2 = pItems.get(0);
         pSource.sendSuccess(() -> Component.translatable("commands.drop.success.single", $$2.getCount(), $$2.getDisplayName()), false);
      } else {
         pSource.sendSuccess(() -> Component.translatable("commands.drop.success.multiple", pItems.size()), false);
      }
   }

   private static void callback(CommandSourceStack pSource, List<ItemStack> pItems, ResourceKey<LootTable> pLootTable) {
      if (pItems.size() == 1) {
         ItemStack $$3 = pItems.get(0);
         pSource.sendSuccess(
            () -> Component.translatable(
                  "commands.drop.success.single_with_table", $$3.getCount(), $$3.getDisplayName(), Component.translationArg(pLootTable.location())
               ),
            false
         );
      } else {
         pSource.sendSuccess(
            () -> Component.translatable("commands.drop.success.multiple_with_table", pItems.size(), Component.translationArg(pLootTable.location())), false
         );
      }
   }

   private static ItemStack getSourceHandItem(CommandSourceStack pSource, EquipmentSlot pSlot) throws CommandSyntaxException {
      Entity $$2 = pSource.getEntityOrException();
      if ($$2 instanceof LivingEntity) {
         return ((LivingEntity)$$2).getItemBySlot(pSlot);
      } else {
         throw ERROR_NO_HELD_ITEMS.create($$2.getDisplayName());
      }
   }

   private static int dropBlockLoot(CommandContext<CommandSourceStack> pContext, BlockPos pPos, ItemStack pTool, LootCommand.DropConsumer pDropConsumer) throws CommandSyntaxException {
      CommandSourceStack $$4 = (CommandSourceStack)pContext.getSource();
      ServerLevel $$5 = $$4.getLevel();
      BlockState $$6 = $$5.getBlockState(pPos);
      BlockEntity $$7 = $$5.getBlockEntity(pPos);
      LootParams.Builder $$8 = new LootParams.Builder($$5)
         .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pPos))
         .withParameter(LootContextParams.BLOCK_STATE, $$6)
         .withOptionalParameter(LootContextParams.BLOCK_ENTITY, $$7)
         .withOptionalParameter(LootContextParams.THIS_ENTITY, $$4.getEntity())
         .withParameter(LootContextParams.TOOL, pTool);
      List<ItemStack> $$9 = $$6.getDrops($$8);
      return pDropConsumer.accept(pContext, $$9, p_326293_ -> callback($$4, p_326293_, $$6.getBlock().getLootTable()));
   }

   private static int dropKillLoot(CommandContext<CommandSourceStack> pContext, Entity pEntity, LootCommand.DropConsumer pDropConsumer) throws CommandSyntaxException {
      if (!(pEntity instanceof LivingEntity)) {
         throw ERROR_NO_LOOT_TABLE.create(pEntity.getDisplayName());
      } else {
         ResourceKey<LootTable> $$3 = ((LivingEntity)pEntity).getLootTable();
         CommandSourceStack $$4 = (CommandSourceStack)pContext.getSource();
         LootParams.Builder $$5 = new LootParams.Builder($$4.getLevel());
         Entity $$6 = $$4.getEntity();
         if ($$6 instanceof Player $$7) {
            $$5.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, $$7);
         }

         $$5.withParameter(LootContextParams.DAMAGE_SOURCE, pEntity.damageSources().magic());
         $$5.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, $$6);
         $$5.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, $$6);
         $$5.withParameter(LootContextParams.THIS_ENTITY, pEntity);
         $$5.withParameter(LootContextParams.ORIGIN, $$4.getPosition());
         LootParams $$8 = $$5.create(LootContextParamSets.ENTITY);
         LootTable $$9 = $$4.getServer().reloadableRegistries().getLootTable($$3);
         List<ItemStack> $$10 = $$9.getRandomItems($$8);
         return pDropConsumer.accept(pContext, $$10, p_326312_ -> callback($$4, p_326312_, $$3));
      }
   }

   private static int dropChestLoot(CommandContext<CommandSourceStack> pContext, Holder<LootTable> pLootTable, LootCommand.DropConsumer pDropCOnsimer) throws CommandSyntaxException {
      CommandSourceStack $$3 = (CommandSourceStack)pContext.getSource();
      LootParams $$4 = new LootParams.Builder($$3.getLevel())
         .withOptionalParameter(LootContextParams.THIS_ENTITY, $$3.getEntity())
         .withParameter(LootContextParams.ORIGIN, $$3.getPosition())
         .create(LootContextParamSets.CHEST);
      return drop(pContext, pLootTable, $$4, pDropCOnsimer);
   }

   private static int dropFishingLoot(
      CommandContext<CommandSourceStack> pContext, Holder<LootTable> pLootTable, BlockPos pPos, ItemStack pTool, LootCommand.DropConsumer pDropConsumet
   ) throws CommandSyntaxException {
      CommandSourceStack $$5 = (CommandSourceStack)pContext.getSource();
      LootParams $$6 = new LootParams.Builder($$5.getLevel())
         .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pPos))
         .withParameter(LootContextParams.TOOL, pTool)
         .withOptionalParameter(LootContextParams.THIS_ENTITY, $$5.getEntity())
         .create(LootContextParamSets.FISHING);
      return drop(pContext, pLootTable, $$6, pDropConsumet);
   }

   private static int drop(
      CommandContext<CommandSourceStack> pContext, Holder<LootTable> pLootTable, LootParams pParams, LootCommand.DropConsumer pDropConsumer
   ) throws CommandSyntaxException {
      CommandSourceStack $$4 = (CommandSourceStack)pContext.getSource();
      List<ItemStack> $$5 = pLootTable.value().getRandomItems(pParams);
      return pDropConsumer.accept(pContext, $$5, p_137997_ -> callback($$4, p_137997_));
   }

   @FunctionalInterface
   interface Callback {
      void accept(List<ItemStack> pItems) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface DropConsumer {
      int accept(CommandContext<CommandSourceStack> pContext, List<ItemStack> pItems, LootCommand.Callback pCallback) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface TailProvider {
      ArgumentBuilder<CommandSourceStack, ?> construct(ArgumentBuilder<CommandSourceStack, ?> pBuilder, LootCommand.DropConsumer pDropConsumer);
   }
}
