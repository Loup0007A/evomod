package net.minecraft.commands.functions;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.resources.ResourceLocation;

class FunctionBuilder<T extends ExecutionCommandSource<T>> {
   @Nullable
   private List<UnboundEntryAction<T>> plainEntries = new ArrayList<>();
   @Nullable
   private List<MacroFunction.Entry<T>> macroEntries;
   private final List<String> macroArguments = new ArrayList<>();

   public void addCommand(UnboundEntryAction<T> pCommand) {
      if (this.macroEntries != null) {
         this.macroEntries.add(new MacroFunction.PlainTextEntry<>(pCommand));
      } else {
         this.plainEntries.add(pCommand);
      }
   }

   private int getArgumentIndex(String pArgument) {
      int $$1 = this.macroArguments.indexOf(pArgument);
      if ($$1 == -1) {
         $$1 = this.macroArguments.size();
         this.macroArguments.add(pArgument);
      }

      return $$1;
   }

   private IntList convertToIndices(List<String> pArguments) {
      IntArrayList $$1 = new IntArrayList(pArguments.size());

      for (String $$2 : pArguments) {
         $$1.add(this.getArgumentIndex($$2));
      }

      return $$1;
   }

   public void addMacro(String pName, int pLineNumber, T pCompilationContext) {
      StringTemplate $$3 = StringTemplate.fromString(pName, pLineNumber);
      if (this.plainEntries != null) {
         this.macroEntries = new ArrayList<>(this.plainEntries.size() + 1);

         for (UnboundEntryAction<T> $$4 : this.plainEntries) {
            this.macroEntries.add(new MacroFunction.PlainTextEntry<>($$4));
         }

         this.plainEntries = null;
      }

      this.macroEntries.add(new MacroFunction.MacroEntry<>($$3, this.convertToIndices($$3.variables()), pCompilationContext));
   }

   public CommandFunction<T> build(ResourceLocation pId) {
      return (CommandFunction<T>)(this.macroEntries != null
         ? new MacroFunction<>(pId, this.macroEntries, this.macroArguments)
         : new PlainTextFunction<>(pId, this.plainEntries));
   }
}
