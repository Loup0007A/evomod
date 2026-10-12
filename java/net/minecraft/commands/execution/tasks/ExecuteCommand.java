package net.minecraft.commands.execution.tasks;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.UnboundEntryAction;

public class ExecuteCommand<T extends ExecutionCommandSource<T>> implements UnboundEntryAction<T> {
   private final String commandInput;
   private final ChainModifiers modifiers;
   private final CommandContext<T> executionContext;

   public ExecuteCommand(String pCommandInput, ChainModifiers pModifiers, CommandContext<T> pExecutionContext) {
      this.commandInput = pCommandInput;
      this.modifiers = pModifiers;
      this.executionContext = pExecutionContext;
   }

   public void execute(T pSource, ExecutionContext<T> pExecutionContext, Frame pFrame) {
      pExecutionContext.profiler().push(() -> "execute " + this.commandInput);

      try {
         pExecutionContext.incrementCost();
         int $$3 = ContextChain.runExecutable(this.executionContext, pSource, ExecutionCommandSource.resultConsumer(), this.modifiers.isForked());
         TraceCallbacks $$4 = pExecutionContext.tracer();
         if ($$4 != null) {
            $$4.onReturn(pFrame.depth(), this.commandInput, $$3);
         }
      } catch (CommandSyntaxException var9) {
         pSource.handleError(var9, this.modifiers.isForked(), pExecutionContext.tracer());
      } finally {
         pExecutionContext.profiler().pop();
      }
   }
}
