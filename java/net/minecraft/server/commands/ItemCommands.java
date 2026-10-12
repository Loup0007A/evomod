package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.commands.arguments.SlotArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class ItemCommands {
   static final Dynamic3CommandExceptionType ERROR_TARGET_NOT_A_CONTAINER = new Dynamic3CommandExceptionType(
      (p_308755_, p_308756_, p_308757_) -> Component.translatableEscape("commands.item.target.not_a_container", p_308755_, p_308756_, p_308757_)
   );
   static final Dynamic3CommandExceptionType ERROR_SOURCE_NOT_A_CONTAINER = new Dynamic3CommandExceptionType(
      (p_308752_, p_308753_, p_308754_) -> Component.translatableEscape("commands.item.source.not_a_container", p_308752_, p_308753_, p_308754_)
   );
   static final DynamicCommandExceptionType ERROR_TARGET_INAPPLICABLE_SLOT = new DynamicCommandExceptionType(
      p_308758_ -> Component.translatableEscape("commands.item.target.no_such_slot", p_308758_)
   );
   private static final DynamicCommandExceptionType ERROR_SOURCE_INAPPLICABLE_SLOT = new DynamicCommandExceptionType(
      p_308749_ -> Component.translatableEscape("commands.item.source.no_such_slot", p_308749_)
   );
   private static final DynamicCommandExceptionType ERROR_TARGET_NO_CHANGES = new DynamicCommandExceptionType(
      p_308748_ -> Component.translatableEscape("commands.item.target.no_changes", p_308748_)
   );
   private static final Dynamic2CommandExceptionType ERROR_TARGET_NO_CHANGES_KNOWN_ITEM = new Dynamic2CommandExceptionType(
      (p_308750_, p_308751_) -> Component.translatableEscape("commands.item.target.no_changed.known_item", p_308750_, p_308751_)
   );
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_MODIFIER = (p_326278_, p_326279_) -> {
      ReloadableServerRegistries.Holder $$2 = ((CommandSourceStack)p_326278_.getSource()).getServer().reloadableRegistries();
      return SharedSuggestionProvider.suggestResource($$2.getKeys(Registries.ITEM_MODIFIER), p_326279_);
   };

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("item").requires(p_180256_ -> p_180256_.hasPermission(2)))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("replace")
                        .then(
                           Commands.literal("block")
                              .then(
                                 Commands.argument("pos", BlockPosArgument.blockPos())
                                    .then(
                                       ((RequiredArgumentBuilder)Commands.argument("slot", SlotArgument.slot())
                                             .then(
                                                Commands.literal("with")
                                                   .then(
                                                      ((RequiredArgumentBuilder)Commands.argument("item", ItemArgument.item(pContext))
                                                            .executes(
                                                               p_180383_ -> setBlockItem(
                                                                     (CommandSourceStack)p_180383_.getSource(),
                                                                     BlockPosArgument.getLoadedBlockPos(p_180383_, "pos"),
                                                                     SlotArgument.getSlot(p_180383_, "slot"),
                                                                     ItemArgument.getItem(p_180383_, "item").createItemStack(1, false)
                                                                  )
                                                            ))
                                                         .then(
                                                            Commands.argument("count", IntegerArgumentType.integer(1, 99))
                                                               .executes(
                                                                  p_180381_ -> setBlockItem(
                                                                        (CommandSourceStack)p_180381_.getSource(),
                                                                        BlockPosArgument.getLoadedBlockPos(p_180381_, "pos"),
                                                                        SlotArgument.getSlot(p_180381_, "slot"),
                                                                        ItemArgument.getItem(p_180381_, "item")
                                                                           .createItemStack(IntegerArgumentType.getInteger(p_180381_, "count"), true)
                                                                     )
                                                               )
                                                         )
                                                   )
                                             ))
                                          .then(
                                             ((LiteralArgumentBuilder)Commands.literal("from")
                                                   .then(
                                                      Commands.literal("block")
                                                         .then(
                                                            Commands.argument("source", BlockPosArgument.blockPos())
                                                               .then(
                                                                  ((RequiredArgumentBuilder)Commands.argument("sourceSlot", SlotArgument.slot())
                                                                        .executes(
                                                                           p_180379_ -> blockToBlock(
                                                                                 (CommandSourceStack)p_180379_.getSource(),
                                                                                 BlockPosArgument.getLoadedBlockPos(p_180379_, "source"),
                                                                                 SlotArgument.getSlot(p_180379_, "sourceSlot"),
                                                                                 BlockPosArgument.getLoadedBlockPos(p_180379_, "pos"),
                                                                                 SlotArgument.getSlot(p_180379_, "slot")
                                                                              )
                                                                        ))
                                                                     .then(
                                                                        Commands.argument("modifier", ResourceOrIdArgument.lootModifier(pContext))
                                                                           .suggests(SUGGEST_MODIFIER)
                                                                           .executes(
                                                                              p_326276_ -> blockToBlock(
                                                                                    (CommandSourceStack)p_326276_.getSource(),
                                                                                    BlockPosArgument.getLoadedBlockPos(p_326276_, "source"),
                                                                                    SlotArgument.getSlot(p_326276_, "sourceSlot"),
                                                                                    BlockPosArgument.getLoadedBlockPos(p_326276_, "pos"),
                                                                                    SlotArgument.getSlot(p_326276_, "slot"),
                                                                                    ResourceOrIdArgument.getLootModifier(p_326276_, "modifier")
                                                                                 )
                                                                           )
                                                                     )
                                                               )
                                                         )
                                                   ))
                                                .then(
                                                   Commands.literal("entity")
                                                      .then(
                                                         Commands.argument("source", EntityArgument.entity())
                                                            .then(
                                                               ((RequiredArgumentBuilder)Commands.argument("sourceSlot", SlotArgument.slot())
                                                                     .executes(
                                                                        p_180375_ -> entityToBlock(
                                                                              (CommandSourceStack)p_180375_.getSource(),
                                                                              EntityArgument.getEntity(p_180375_, "source"),
                                                                              SlotArgument.getSlot(p_180375_, "sourceSlot"),
                                                                              BlockPosArgument.getLoadedBlockPos(p_180375_, "pos"),
                                                                              SlotArgument.getSlot(p_180375_, "slot")
                                                                           )
                                                                     ))
                                                                  .then(
                                                                     Commands.argument("modifier", ResourceOrIdArgument.lootModifier(pContext))
                                                                        .suggests(SUGGEST_MODIFIER)
                                                                        .executes(
                                                                           p_326280_ -> entityToBlock(
                                                                                 (CommandSourceStack)p_326280_.getSource(),
                                                                                 EntityArgument.getEntity(p_326280_, "source"),
                                                                                 SlotArgument.getSlot(p_326280_, "sourceSlot"),
                                                                                 BlockPosArgument.getLoadedBlockPos(p_326280_, "pos"),
                                                                                 SlotArgument.getSlot(p_326280_, "slot"),
                                                                                 ResourceOrIdArgument.getLootModifier(p_326280_, "modifier")
                                                                              )
                                                                        )
                                                                  )
                                                            )
                                                      )
                                                )
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("entity")
                           .then(
                              Commands.argument("targets", EntityArgument.entities())
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("slot", SlotArgument.slot())
                                          .then(
                                             Commands.literal("with")
                                                .then(
                                                   ((RequiredArgumentBuilder)Commands.argument("item", ItemArgument.item(pContext))
                                                         .executes(
                                                            p_180371_ -> setEntityItem(
                                                                  (CommandSourceStack)p_180371_.getSource(),
                                                                  EntityArgument.getEntities(p_180371_, "targets"),
                                                                  SlotArgument.getSlot(p_180371_, "slot"),
                                                                  ItemArgument.getItem(p_180371_, "item").createItemStack(1, false)
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.argument("count", IntegerArgumentType.integer(1, 99))
                                                            .executes(
                                                               p_180369_ -> setEntityItem(
                                                                     (CommandSourceStack)p_180369_.getSource(),
                                                                     EntityArgument.getEntities(p_180369_, "targets"),
                                                                     SlotArgument.getSlot(p_180369_, "slot"),
                                                                     ItemArgument.getItem(p_180369_, "item")
                                                                        .createItemStack(IntegerArgumentType.getInteger(p_180369_, "count"), true)
                                                                  )
                                                            )
                                                      )
                                                )
                                          ))
                                       .then(
                                          ((LiteralArgumentBuilder)Commands.literal("from")
                                                .then(
                                                   Commands.literal("block")
                                                      .then(
                                                         Commands.argument("source", BlockPosArgument.blockPos())
                                                            .then(
                                                               ((RequiredArgumentBuilder)Commands.argument("sourceSlot", SlotArgument.slot())
                                                                     .executes(
                                                                        p_180367_ -> blockToEntities(
                                                                              (CommandSourceStack)p_180367_.getSource(),
                                                                              BlockPosArgument.getLoadedBlockPos(p_180367_, "source"),
                                                                              SlotArgument.getSlot(p_180367_, "sourceSlot"),
                                                                              EntityArgument.getEntities(p_180367_, "targets"),
                                                                              SlotArgument.getSlot(p_180367_, "slot")
                                                                           )
                                                                     ))
                                                                  .then(
                                                                     Commands.argument("modifier", ResourceOrIdArgument.lootModifier(pContext))
                                                                        .suggests(SUGGEST_MODIFIER)
                                                                        .executes(
                                                                           p_326277_ -> blockToEntities(
                                                                                 (CommandSourceStack)p_326277_.getSource(),
                                                                                 BlockPosArgument.getLoadedBlockPos(p_326277_, "source"),
                                                                                 SlotArgument.getSlot(p_326277_, "sourceSlot"),
                                                                                 EntityArgument.getEntities(p_326277_, "targets"),
                                                                                 SlotArgument.getSlot(p_326277_, "slot"),
                                                                                 ResourceOrIdArgument.getLootModifier(p_326277_, "modifier")
                                                                              )
                                                                        )
                                                                  )
                                                            )
                                                      )
                                                ))
                                             .then(
                                                Commands.literal("entity")
                                                   .then(
                                                      Commands.argument("source", EntityArgument.entity())
                                                         .then(
                                                            ((RequiredArgumentBuilder)Commands.argument("sourceSlot", SlotArgument.slot())
                                                                  .executes(
                                                                     p_180363_ -> entityToEntities(
                                                                           (CommandSourceStack)p_180363_.getSource(),
                                                                           EntityArgument.getEntity(p_180363_, "source"),
                                                                           SlotArgument.getSlot(p_180363_, "sourceSlot"),
                                                                           EntityArgument.getEntities(p_180363_, "targets"),
                                                                           SlotArgument.getSlot(p_180363_, "slot")
                                                                        )
                                                                  ))
                                                               .then(
                                                                  Commands.argument("modifier", ResourceOrIdArgument.lootModifier(pContext))
                                                                     .suggests(SUGGEST_MODIFIER)
                                                                     .executes(
                                                                        p_326275_ -> entityToEntities(
                                                                              (CommandSourceStack)p_326275_.getSource(),
                                                                              EntityArgument.getEntity(p_326275_, "source"),
                                                                              SlotArgument.getSlot(p_326275_, "sourceSlot"),
                                                                              EntityArgument.getEntities(p_326275_, "targets"),
                                                                              SlotArgument.getSlot(p_326275_, "slot"),
                                                                              ResourceOrIdArgument.getLootModifier(p_326275_, "modifier")
                                                                           )
                                                                     )
                                                               )
                                                         )
                                                   )
                                             )
                                       )
                                 )
                           )
                     )
               ))
            .then(
               ((LiteralArgumentBuilder)Commands.literal("modify")
                     .then(
                        Commands.literal("block")
                           .then(
                              Commands.argument("pos", BlockPosArgument.blockPos())
                                 .then(
                                    Commands.argument("slot", SlotArgument.slot())
                                       .then(
                                          Commands.argument("modifier", ResourceOrIdArgument.lootModifier(pContext))
                                             .suggests(SUGGEST_MODIFIER)
                                             .executes(
                                                p_326282_ -> modifyBlockItem(
                                                      (CommandSourceStack)p_326282_.getSource(),
                                                      BlockPosArgument.getLoadedBlockPos(p_326282_, "pos"),
                                                      SlotArgument.getSlot(p_326282_, "slot"),
                                                      ResourceOrIdArgument.getLootModifier(p_326282_, "modifier")
                                                   )
                                             )
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("entity")
                        .then(
                           Commands.argument("targets", EntityArgument.entities())
                              .then(
                                 Commands.argument("slot", SlotArgument.slot())
                                    .then(
                                       Commands.argument("modifier", ResourceOrIdArgument.lootModifier(pContext))
                                          .suggests(SUGGEST_MODIFIER)
                                          .executes(
                                             p_326281_ -> modifyEntityItem(
                                                   (CommandSourceStack)p_326281_.getSource(),
                                                   EntityArgument.getEntities(p_326281_, "targets"),
                                                   SlotArgument.getSlot(p_326281_, "slot"),
                                                   ResourceOrIdArgument.getLootModifier(p_326281_, "modifier")
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int modifyBlockItem(CommandSourceStack pSource, BlockPos pPos, int pSlot, Holder<LootItemFunction> pModifier) throws CommandSyntaxException {
      Container $$4 = getContainer(pSource, pPos, ERROR_TARGET_NOT_A_CONTAINER);
      if (pSlot >= 0 && pSlot < $$4.getContainerSize()) {
         ItemStack $$5 = applyModifier(pSource, pModifier, $$4.getItem(pSlot));
         $$4.setItem(pSlot, $$5);
         pSource.sendSuccess(() -> Component.translatable("commands.item.block.set.success", pPos.getX(), pPos.getY(), pPos.getZ(), $$5.getDisplayName()), true);
         return 1;
      } else {
         throw ERROR_TARGET_INAPPLICABLE_SLOT.create(pSlot);
      }
   }

   private static int modifyEntityItem(CommandSourceStack pSource, Collection<? extends Entity> pTargets, int pSourceSlot, Holder<LootItemFunction> pModifer) throws CommandSyntaxException {
      Map<Entity, ItemStack> $$4 = Maps.newHashMapWithExpectedSize(pTargets.size());

      for (Entity $$5 : pTargets) {
         SlotAccess $$6 = $$5.getSlot(pSourceSlot);
         if ($$6 != SlotAccess.NULL) {
            ItemStack $$7 = applyModifier(pSource, pModifer, $$6.get().copy());
            if ($$6.set($$7)) {
               $$4.put($$5, $$7);
               if ($$5 instanceof ServerPlayer) {
                  ((ServerPlayer)$$5).containerMenu.broadcastChanges();
               }
            }
         }
      }

      if ($$4.isEmpty()) {
         throw ERROR_TARGET_NO_CHANGES.create(pSourceSlot);
      } else {
         if ($$4.size() == 1) {
            Entry<Entity, ItemStack> $$8 = $$4.entrySet().iterator().next();
            pSource.sendSuccess(
               () -> Component.translatable("commands.item.entity.set.success.single", $$8.getKey().getDisplayName(), $$8.getValue().getDisplayName()), true
            );
         } else {
            pSource.sendSuccess(() -> Component.translatable("commands.item.entity.set.success.multiple", $$4.size()), true);
         }

         return $$4.size();
      }
   }

   private static int setBlockItem(CommandSourceStack pSource, BlockPos pPos, int pSlot, ItemStack pItem) throws CommandSyntaxException {
      Container $$4 = getContainer(pSource, pPos, ERROR_TARGET_NOT_A_CONTAINER);
      if (pSlot >= 0 && pSlot < $$4.getContainerSize()) {
         $$4.setItem(pSlot, pItem);
         pSource.sendSuccess(
            () -> Component.translatable("commands.item.block.set.success", pPos.getX(), pPos.getY(), pPos.getZ(), pItem.getDisplayName()), true
         );
         return 1;
      } else {
         throw ERROR_TARGET_INAPPLICABLE_SLOT.create(pSlot);
      }
   }

   static Container getContainer(CommandSourceStack pSource, BlockPos pPos, Dynamic3CommandExceptionType pException) throws CommandSyntaxException {
      BlockEntity $$3 = pSource.getLevel().getBlockEntity(pPos);
      if (!($$3 instanceof Container)) {
         throw pException.create(pPos.getX(), pPos.getY(), pPos.getZ());
      } else {
         return (Container)$$3;
      }
   }

   private static int setEntityItem(CommandSourceStack pSource, Collection<? extends Entity> pTargets, int pSlot, ItemStack pItem) throws CommandSyntaxException {
      List<Entity> $$4 = Lists.newArrayListWithCapacity(pTargets.size());

      for (Entity $$5 : pTargets) {
         SlotAccess $$6 = $$5.getSlot(pSlot);
         if ($$6 != SlotAccess.NULL && $$6.set(pItem.copy())) {
            $$4.add($$5);
            if ($$5 instanceof ServerPlayer) {
               ((ServerPlayer)$$5).containerMenu.broadcastChanges();
            }
         }
      }

      if ($$4.isEmpty()) {
         throw ERROR_TARGET_NO_CHANGES_KNOWN_ITEM.create(pItem.getDisplayName(), pSlot);
      } else {
         if ($$4.size() == 1) {
            pSource.sendSuccess(
               () -> Component.translatable("commands.item.entity.set.success.single", $$4.iterator().next().getDisplayName(), pItem.getDisplayName()), true
            );
         } else {
            pSource.sendSuccess(() -> Component.translatable("commands.item.entity.set.success.multiple", $$4.size(), pItem.getDisplayName()), true);
         }

         return $$4.size();
      }
   }

   private static int blockToEntities(CommandSourceStack pSource, BlockPos pPos, int pSourceSlot, Collection<? extends Entity> pTargets, int pSlot) throws CommandSyntaxException {
      return setEntityItem(pSource, pTargets, pSlot, getBlockItem(pSource, pPos, pSourceSlot));
   }

   private static int blockToEntities(
      CommandSourceStack pSource, BlockPos pPos, int pSourceSlot, Collection<? extends Entity> pTargets, int pSlot, Holder<LootItemFunction> pModifier
   ) throws CommandSyntaxException {
      return setEntityItem(pSource, pTargets, pSlot, applyModifier(pSource, pModifier, getBlockItem(pSource, pPos, pSourceSlot)));
   }

   private static int blockToBlock(CommandSourceStack pSource, BlockPos pSourcePos, int pSourceSlot, BlockPos pPos, int pSlot) throws CommandSyntaxException {
      return setBlockItem(pSource, pPos, pSlot, getBlockItem(pSource, pSourcePos, pSourceSlot));
   }

   private static int blockToBlock(
      CommandSourceStack pSource, BlockPos pSourcePos, int pSourceSlot, BlockPos pPos, int pSlot, Holder<LootItemFunction> pModifier
   ) throws CommandSyntaxException {
      return setBlockItem(pSource, pPos, pSlot, applyModifier(pSource, pModifier, getBlockItem(pSource, pSourcePos, pSourceSlot)));
   }

   private static int entityToBlock(CommandSourceStack pSource, Entity pSourceEntity, int pSourceSlot, BlockPos pPos, int pSlot) throws CommandSyntaxException {
      return setBlockItem(pSource, pPos, pSlot, getEntityItem(pSourceEntity, pSourceSlot));
   }

   private static int entityToBlock(
      CommandSourceStack pSource, Entity pSourceEntity, int pSourceSlot, BlockPos pPos, int pSlot, Holder<LootItemFunction> pModifier
   ) throws CommandSyntaxException {
      return setBlockItem(pSource, pPos, pSlot, applyModifier(pSource, pModifier, getEntityItem(pSourceEntity, pSourceSlot)));
   }

   private static int entityToEntities(CommandSourceStack pSource, Entity pSourceEntity, int pSourceSlot, Collection<? extends Entity> pTargets, int pSlot) throws CommandSyntaxException {
      return setEntityItem(pSource, pTargets, pSlot, getEntityItem(pSourceEntity, pSourceSlot));
   }

   private static int entityToEntities(
      CommandSourceStack pSource, Entity pSourceEntity, int pSourceSlot, Collection<? extends Entity> pTargets, int pSlot, Holder<LootItemFunction> pModifier
   ) throws CommandSyntaxException {
      return setEntityItem(pSource, pTargets, pSlot, applyModifier(pSource, pModifier, getEntityItem(pSourceEntity, pSourceSlot)));
   }

   private static ItemStack applyModifier(CommandSourceStack pSource, Holder<LootItemFunction> pModifier, ItemStack pStack) {
      ServerLevel $$3 = pSource.getLevel();
      LootParams $$4 = new LootParams.Builder($$3)
         .withParameter(LootContextParams.ORIGIN, pSource.getPosition())
         .withOptionalParameter(LootContextParams.THIS_ENTITY, pSource.getEntity())
         .create(LootContextParamSets.COMMAND);
      LootContext $$5 = new LootContext.Builder($$4).create(Optional.empty());
      $$5.pushVisitedElement(LootContext.createVisitedEntry(pModifier.value()));
      ItemStack $$6 = pModifier.value().apply(pStack, $$5);
      $$6.limitSize($$6.getMaxStackSize());
      return $$6;
   }

   private static ItemStack getEntityItem(Entity pEntity, int pSlot) throws CommandSyntaxException {
      SlotAccess $$2 = pEntity.getSlot(pSlot);
      if ($$2 == SlotAccess.NULL) {
         throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(pSlot);
      } else {
         return $$2.get().copy();
      }
   }

   private static ItemStack getBlockItem(CommandSourceStack pSource, BlockPos pPos, int pSlot) throws CommandSyntaxException {
      Container $$3 = getContainer(pSource, pPos, ERROR_SOURCE_NOT_A_CONTAINER);
      if (pSlot >= 0 && pSlot < $$3.getContainerSize()) {
         return $$3.getItem(pSlot).copy();
      } else {
         throw ERROR_SOURCE_INAPPLICABLE_SLOT.create(pSlot);
      }
   }
}
