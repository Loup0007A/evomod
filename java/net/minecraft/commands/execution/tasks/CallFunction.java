package net.minecraft.commands.execution.tasks;

import java.util.List;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.CommandQueueEntry;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.TraceCallbacks;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.commands.functions.InstantiatedFunction;

public class CallFunction<T extends ExecutionCommandSource<T>> implements UnboundEntryAction<T> {
   private final InstantiatedFunction<T> function;
   private final CommandResultCallback resultCallback;
   private final boolean returnParentFrame;

   public CallFunction(InstantiatedFunction<T> pFunction, CommandResultCallback pResultCallback, boolean pReturnParentFrame) {
      this.function = pFunction;
      this.resultCallback = pResultCallback;
      this.returnParentFrame = pReturnParentFrame;
   }

   public void execute(T pSource, ExecutionContext<T> pExecutionContext, Frame pFrame) {
      pExecutionContext.incrementCost();
      List<UnboundEntryAction<T>> $$3 = this.function.entries();
      TraceCallbacks $$4 = pExecutionContext.tracer();
      if ($$4 != null) {
         $$4.onCall(pFrame.depth(), this.function.id(), this.function.entries().size());
      }

      int $$5 = pFrame.depth() + 1;
      Frame.FrameControl $$6 = this.returnParentFrame ? pFrame.frameControl() : pExecutionContext.frameControlForDepth($$5);
      Frame $$7 = new Frame($$5, this.resultCallback, $$6);
      ContinuationTask.schedule(pExecutionContext, $$7, $$3, (p_310328_, p_313182_) -> new CommandQueueEntry<>(p_310328_, p_313182_.bind(pSource)));
   }
}
