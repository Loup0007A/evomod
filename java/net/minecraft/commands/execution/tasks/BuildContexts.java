package net.minecraft.commands.execution.tasks;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.context.ContextChain.Stage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CommandQueueEntry;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.CustomModifierExecutor;
import net.minecraft.commands.execution.EntryAction;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.network.chat.Component;

public class BuildContexts<T extends ExecutionCommandSource<T>> {
   @VisibleForTesting
   public static final DynamicCommandExceptionType ERROR_FORK_LIMIT_REACHED = new DynamicCommandExceptionType(
      p_311924_ -> Component.translatableEscape("command.forkLimit", p_311924_)
   );
   private final String commandInput;
   private final ContextChain<T> command;

   public BuildContexts(String pCommandInput, ContextChain<T> pCommand) {
      this.commandInput = pCommandInput;
      this.command = pCommand;
   }

   protected void execute(T pOriginalSource, List<T> pSources, ExecutionContext<T> pContext, Frame pFrame, ChainModifiers pChainModifiers) {
      ContextChain<T> $$5 = this.command;
      ChainModifiers $$6 = pChainModifiers;
      List<T> $$7 = pSources;
      if ($$5.getStage() != Stage.EXECUTE) {
         pContext.profiler().push(() -> "prepare " + this.commandInput);

         try {
            for (int $$8 = pContext.forkLimit(); $$5.getStage() != Stage.EXECUTE; $$5 = $$5.nextStage()) {
               CommandContext<T> $$9 = $$5.getTopContext();
               if ($$9.isForked()) {
                  $$6 = $$6.setForked();
               }

               RedirectModifier<T> $$10 = $$9.getRedirectModifier();
               if ($$10 instanceof CustomModifierExecutor<T> $$11) {
                  $$11.apply(pOriginalSource, $$7, $$5, $$6, ExecutionControl.create(pContext, pFrame));
                  return;
               }

               if ($$10 != null) {
                  pContext.incrementCost();
                  boolean $$12 = $$6.isForked();
                  List<T> $$13 = new ObjectArrayList();

                  for (T $$14 : $$7) {
                     try {
                        Collection<T> $$15 = ContextChain.runModifier($$9, $$14, (p_311026_, p_312291_, p_310245_) -> {
                        }, $$12);
                        if ($$13.size() + $$15.size() >= $$8) {
                           pOriginalSource.handleError(ERROR_FORK_LIMIT_REACHED.create($$8), $$12, pContext.tracer());
                           return;
                        }

                        $$13.addAll($$15);
                     } catch (CommandSyntaxException var20) {
                        $$14.handleError(var20, $$12, pContext.tracer());
                        if (!$$12) {
                           return;
                        }
                     }
                  }

                  $$7 = $$13;
               }
            }
         } finally {
            pContext.profiler().pop();
         }
      }

      if ($$7.isEmpty()) {
         if ($$6.isReturn()) {
            pContext.queueNext(new CommandQueueEntry<>(pFrame, FallthroughTask.instance()));
         }
      } else {
         CommandContext<T> $$17 = $$5.getTopContext();
         if ($$17.getCommand() instanceof CustomCommandExecutor<T> $$19) {
            ExecutionControl<T> $$20 = ExecutionControl.create(pContext, pFrame);

            for (T $$21 : $$7) {
               $$19.run($$21, $$5, $$6, $$20);
            }
         } else {
            if ($$6.isReturn()) {
               T $$22 = $$7.get(0);
               $$22 = $$22.withCallback(CommandResultCallback.chain($$22.callback(), pFrame.returnValueConsumer()));
               $$7 = List.of($$22);
            }

            ExecuteCommand<T> $$23 = new ExecuteCommand<>(this.commandInput, $$6, $$17);
            ContinuationTask.schedule(pContext, pFrame, $$7, (p_311832_, p_309437_) -> new CommandQueueEntry<>(p_311832_, $$23.bind(p_309437_)));
         }
      }
   }

   protected void traceCommandStart(ExecutionContext<T> pExecutionContext, Frame pFrame) {
      TraceCallbacks $$2 = pExecutionContext.tracer();
      if ($$2 != null) {
         $$2.onCommand(pFrame.depth(), this.commandInput);
      }
   }

   @Override
   public String toString() {
      return this.commandInput;
   }

   public static class Continuation<T extends ExecutionCommandSource<T>> extends BuildContexts<T> implements EntryAction<T> {
      private final ChainModifiers modifiers;
      private final T originalSource;
      private final List<T> sources;

      public Continuation(String pCommandInput, ContextChain<T> pCommand, ChainModifiers pModifiers, T pOriginalSource, List<T> pSources) {
         super(pCommandInput, pCommand);
         this.originalSource = pOriginalSource;
         this.sources = pSources;
         this.modifiers = pModifiers;
      }

      @Override
      public void execute(ExecutionContext<T> pContext, Frame pFrame) {
         this.execute(this.originalSource, this.sources, pContext, pFrame, this.modifiers);
      }
   }

   public static class TopLevel<T extends ExecutionCommandSource<T>> extends BuildContexts<T> implements EntryAction<T> {
      private final T source;

      public TopLevel(String pCommandInput, ContextChain<T> pCommand, T pSource) {
         super(pCommandInput, pCommand);
         this.source = pSource;
      }

      @Override
      public void execute(ExecutionContext<T> pContext, Frame pFrame) {
         this.traceCommandStart(pContext, pFrame);
         this.execute(this.source, List.of(this.source), pContext, pFrame, ChainModifiers.DEFAULT);
      }
   }

   public static class Unbound<T extends ExecutionCommandSource<T>> extends BuildContexts<T> implements UnboundEntryAction<T> {
      public Unbound(String p_312191_, ContextChain<T> p_309892_) {
         super(p_312191_, p_309892_);
      }

      public void execute(T p_310320_, ExecutionContext<T> p_313071_, Frame p_310123_) {
         this.traceCommandStart(p_313071_, p_310123_);
         this.execute(p_310320_, List.of(p_310320_), p_313071_, p_310123_, ChainModifiers.DEFAULT);
      }
   }
}
