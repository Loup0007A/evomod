package net.minecraft.server.commands;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.execution.tasks.FallthroughTask;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;

public class FunctionCommand {
   private static final DynamicCommandExceptionType ERROR_ARGUMENT_NOT_COMPOUND = new DynamicCommandExceptionType(
      p_308741_ -> Component.translatableEscape("commands.function.error.argument_not_compound", p_308741_)
   );
   static final DynamicCommandExceptionType ERROR_NO_FUNCTIONS = new DynamicCommandExceptionType(
      p_308742_ -> Component.translatableEscape("commands.function.scheduled.no_functions", p_308742_)
   );
   @VisibleForTesting
   public static final Dynamic2CommandExceptionType ERROR_FUNCTION_INSTANTATION_FAILURE = new Dynamic2CommandExceptionType(
      (p_308724_, p_308725_) -> Component.translatableEscape("commands.function.instantiationFailure", p_308724_, p_308725_)
   );
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_FUNCTION = (p_137719_, p_137720_) -> {
      ServerFunctionManager $$2 = ((CommandSourceStack)p_137719_.getSource()).getServer().getFunctions();
      SharedSuggestionProvider.suggestResource($$2.getTagNames(), p_137720_, "#");
      return SharedSuggestionProvider.suggestResource($$2.getFunctionNames(), p_137720_);
   };
   static final FunctionCommand.Callbacks<CommandSourceStack> FULL_CONTEXT_CALLBACKS = new FunctionCommand.Callbacks<CommandSourceStack>() {
      public void signalResult(CommandSourceStack p_311645_, ResourceLocation p_312021_, int p_313021_) {
         p_311645_.sendSuccess(() -> Component.translatable("commands.function.result", Component.translationArg(p_312021_), p_313021_), true);
      }
   };

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> $$1 = Commands.literal("with");

      for (DataCommands.DataProvider $$2 : DataCommands.SOURCE_PROVIDERS) {
         $$2.wrap($$1, p_308740_ -> p_308740_.executes(new FunctionCommand.FunctionCustomExecutor() {
               @Override
               protected CompoundTag arguments(CommandContext<CommandSourceStack> p_309658_) throws CommandSyntaxException {
                  return $$2.access(p_309658_).getData();
               }
            }).then(Commands.argument("path", NbtPathArgument.nbtPath()).executes(new FunctionCommand.FunctionCustomExecutor() {
               @Override
               protected CompoundTag arguments(CommandContext<CommandSourceStack> p_310697_) throws CommandSyntaxException {
                  return FunctionCommand.getArgumentTag(NbtPathArgument.getPath(p_310697_, "path"), $$2.access(p_310697_));
               }
            })));
      }

      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("function").requires(p_137722_ -> p_137722_.hasPermission(2)))
            .then(
               ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("name", FunctionArgument.functions())
                        .suggests(SUGGEST_FUNCTION)
                        .executes(new FunctionCommand.FunctionCustomExecutor() {
                           @Nullable
                           @Override
                           protected CompoundTag arguments(CommandContext<CommandSourceStack> p_310275_) {
                              return null;
                           }
                        }))
                     .then(Commands.argument("arguments", CompoundTagArgument.compoundTag()).executes(new FunctionCommand.FunctionCustomExecutor() {
                        @Override
                        protected CompoundTag arguments(CommandContext<CommandSourceStack> p_310980_) {
                           return CompoundTagArgument.getCompoundTag(p_310980_, "arguments");
                        }
                     })))
                  .then($$1)
            )
      );
   }

   static CompoundTag getArgumentTag(NbtPathArgument.NbtPath pNbtPath, DataAccessor pDataAccessor) throws CommandSyntaxException {
      Tag $$2 = DataCommands.getSingleTag(pNbtPath, pDataAccessor);
      if ($$2 instanceof CompoundTag) {
         return (CompoundTag)$$2;
      } else {
         throw ERROR_ARGUMENT_NOT_COMPOUND.create($$2.getType().getName());
      }
   }

   public static CommandSourceStack modifySenderForExecution(CommandSourceStack pSource) {
      return pSource.withSuppressedOutput().withMaximumPermission(2);
   }

   public static <T extends ExecutionCommandSource<T>> void queueFunctions(
      Collection<CommandFunction<T>> pFunctions,
      @Nullable CompoundTag pArguments,
      T pOriginalSource,
      T pSource,
      ExecutionControl<T> pExecutionControl,
      FunctionCommand.Callbacks<T> pCallbacks,
      ChainModifiers pChainModifiers
   ) throws CommandSyntaxException {
      if (pChainModifiers.isReturn()) {
         queueFunctionsAsReturn(pFunctions, pArguments, pOriginalSource, pSource, pExecutionControl, pCallbacks);
      } else {
         queueFunctionsNoReturn(pFunctions, pArguments, pOriginalSource, pSource, pExecutionControl, pCallbacks);
      }
   }

   private static <T extends ExecutionCommandSource<T>> void instantiateAndQueueFunctions(
      @Nullable CompoundTag pArguments,
      ExecutionControl<T> pExecutionControl,
      CommandDispatcher<T> pDispatcher,
      T pSource,
      CommandFunction<T> pFunction,
      ResourceLocation pFunctionId,
      CommandResultCallback pResultCallback,
      boolean pReturnParentFrame
   ) throws CommandSyntaxException {
      try {
         InstantiatedFunction<T> $$8 = pFunction.instantiate(pArguments, pDispatcher);
         pExecutionControl.queueNext(new CallFunction<>($$8, pResultCallback, pReturnParentFrame).bind(pSource));
      } catch (FunctionInstantiationException var9) {
         throw ERROR_FUNCTION_INSTANTATION_FAILURE.create(pFunctionId, var9.messageComponent());
      }
   }

   private static <T extends ExecutionCommandSource<T>> CommandResultCallback decorateOutputIfNeeded(
      T pSource, FunctionCommand.Callbacks<T> pCallbacks, ResourceLocation pFunction, CommandResultCallback pResultCallback
   ) {
      return pSource.isSilent() ? pResultCallback : (p_326268_, p_326269_) -> {
         pCallbacks.signalResult(pSource, pFunction, p_326269_);
         pResultCallback.onResult(p_326268_, p_326269_);
      };
   }

   private static <T extends ExecutionCommandSource<T>> void queueFunctionsAsReturn(
      Collection<CommandFunction<T>> pFunctions,
      @Nullable CompoundTag pArguments,
      T pOriginalSource,
      T pSource,
      ExecutionControl<T> pExectutionControl,
      FunctionCommand.Callbacks<T> pCallbacks
   ) throws CommandSyntaxException {
      CommandDispatcher<T> $$6 = pOriginalSource.dispatcher();
      T $$7 = pSource.clearCallbacks();
      CommandResultCallback $$8 = CommandResultCallback.chain(pOriginalSource.callback(), pExectutionControl.currentFrame().returnValueConsumer());

      for (CommandFunction<T> $$9 : pFunctions) {
         ResourceLocation $$10 = $$9.id();
         CommandResultCallback $$11 = decorateOutputIfNeeded(pOriginalSource, pCallbacks, $$10, $$8);
         instantiateAndQueueFunctions(pArguments, pExectutionControl, $$6, $$7, $$9, $$10, $$11, true);
      }

      pExectutionControl.queueNext(FallthroughTask.instance());
   }

   private static <T extends ExecutionCommandSource<T>> void queueFunctionsNoReturn(
      Collection<CommandFunction<T>> pFunctions,
      @Nullable CompoundTag pArguments,
      T pOriginalSource,
      T pSource,
      ExecutionControl<T> pExecutionControl,
      FunctionCommand.Callbacks<T> pCallbacks
   ) throws CommandSyntaxException {
      CommandDispatcher<T> $$6 = pOriginalSource.dispatcher();
      T $$7 = pSource.clearCallbacks();
      CommandResultCallback $$8 = pOriginalSource.callback();
      if (!pFunctions.isEmpty()) {
         if (pFunctions.size() == 1) {
            CommandFunction<T> $$9 = pFunctions.iterator().next();
            ResourceLocation $$10 = $$9.id();
            CommandResultCallback $$11 = decorateOutputIfNeeded(pOriginalSource, pCallbacks, $$10, $$8);
            instantiateAndQueueFunctions(pArguments, pExecutionControl, $$6, $$7, $$9, $$10, $$11, false);
         } else if ($$8 == CommandResultCallback.EMPTY) {
            for (CommandFunction<T> $$12 : pFunctions) {
               ResourceLocation $$13 = $$12.id();
               CommandResultCallback $$14 = decorateOutputIfNeeded(pOriginalSource, pCallbacks, $$13, $$8);
               instantiateAndQueueFunctions(pArguments, pExecutionControl, $$6, $$7, $$12, $$13, $$14, false);
            }
         } else {
            class Accumulator {
               boolean anyResult;
               int sum;

               public void add(int p_310205_) {
                  this.anyResult = true;
                  this.sum += p_310205_;
               }
            }

            Accumulator $$15 = new Accumulator();
            CommandResultCallback $$16 = (p_308727_, p_308728_) -> $$15.add(p_308728_);

            for (CommandFunction<T> $$17 : pFunctions) {
               ResourceLocation $$18 = $$17.id();
               CommandResultCallback $$19 = decorateOutputIfNeeded(pOriginalSource, pCallbacks, $$18, $$16);
               instantiateAndQueueFunctions(pArguments, pExecutionControl, $$6, $$7, $$17, $$18, $$19, false);
            }

            pExecutionControl.queueNext((p_308731_, p_308732_) -> {
               if ($$15.anyResult) {
                  $$8.onSuccess($$15.sum);
               }
            });
         }
      }
   }

   public interface Callbacks<T> {
      void signalResult(T pSource, ResourceLocation pFunction, int pCommands);
   }

   abstract static class FunctionCustomExecutor
      extends CustomCommandExecutor.WithErrorHandling<CommandSourceStack>
      implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
      @Nullable
      protected abstract CompoundTag arguments(CommandContext<CommandSourceStack> pContext) throws CommandSyntaxException;

      public void runGuarded(
         CommandSourceStack pSource,
         ContextChain<CommandSourceStack> pContextChain,
         ChainModifiers pChainModifiers,
         ExecutionControl<CommandSourceStack> pExecutionControl
      ) throws CommandSyntaxException {
         CommandContext<CommandSourceStack> $$4 = pContextChain.getTopContext().copyFor(pSource);
         Pair<ResourceLocation, Collection<CommandFunction<CommandSourceStack>>> $$5 = FunctionArgument.getFunctionCollection($$4, "name");
         Collection<CommandFunction<CommandSourceStack>> $$6 = (Collection<CommandFunction<CommandSourceStack>>)$$5.getSecond();
         if ($$6.isEmpty()) {
            throw FunctionCommand.ERROR_NO_FUNCTIONS.create(Component.translationArg((ResourceLocation)$$5.getFirst()));
         } else {
            CompoundTag $$7 = this.arguments($$4);
            CommandSourceStack $$8 = FunctionCommand.modifySenderForExecution(pSource);
            if ($$6.size() == 1) {
               pSource.sendSuccess(
                  () -> Component.translatable("commands.function.scheduled.single", Component.translationArg($$6.iterator().next().id())), true
               );
            } else {
               pSource.sendSuccess(
                  () -> Component.translatable(
                        "commands.function.scheduled.multiple",
                        ComponentUtils.formatList($$6.stream().map(CommandFunction::id).toList(), Component::translationArg)
                     ),
                  true
               );
            }

            FunctionCommand.queueFunctions($$6, $$7, pSource, $$8, pExecutionControl, FunctionCommand.FULL_CONTEXT_CALLBACKS, pChainModifiers);
         }
      }
   }
}
