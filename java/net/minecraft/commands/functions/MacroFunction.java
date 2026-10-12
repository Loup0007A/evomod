package net.minecraft.commands.functions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class MacroFunction<T extends ExecutionCommandSource<T>> implements CommandFunction<T> {
   private static final DecimalFormat DECIMAL_FORMAT = Util.make(new DecimalFormat("#"), p_312286_ -> {
      p_312286_.setMaximumFractionDigits(15);
      p_312286_.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
   });
   private static final int MAX_CACHE_ENTRIES = 8;
   private final List<String> parameters;
   private final Object2ObjectLinkedOpenHashMap<List<String>, InstantiatedFunction<T>> cache = new Object2ObjectLinkedOpenHashMap(8, 0.25F);
   private final ResourceLocation id;
   private final List<MacroFunction.Entry<T>> entries;

   public MacroFunction(ResourceLocation pId, List<MacroFunction.Entry<T>> pEntries, List<String> pParameters) {
      this.id = pId;
      this.entries = pEntries;
      this.parameters = pParameters;
   }

   @Override
   public ResourceLocation id() {
      return this.id;
   }

   @Override
   public InstantiatedFunction<T> instantiate(@Nullable CompoundTag pArguments, CommandDispatcher<T> pDispatcher) throws FunctionInstantiationException {
      if (pArguments == null) {
         throw new FunctionInstantiationException(Component.translatable("commands.function.error.missing_arguments", Component.translationArg(this.id())));
      } else {
         List<String> $$2 = new ArrayList<>(this.parameters.size());

         for (String $$3 : this.parameters) {
            Tag $$4 = pArguments.get($$3);
            if ($$4 == null) {
               throw new FunctionInstantiationException(
                  Component.translatable("commands.function.error.missing_argument", Component.translationArg(this.id()), $$3)
               );
            }

            $$2.add(stringify($$4));
         }

         InstantiatedFunction<T> $$5 = (InstantiatedFunction<T>)this.cache.getAndMoveToLast($$2);
         if ($$5 != null) {
            return $$5;
         } else {
            if (this.cache.size() >= 8) {
               this.cache.removeFirst();
            }

            InstantiatedFunction<T> $$6 = this.substituteAndParse(this.parameters, $$2, pDispatcher);
            this.cache.put($$2, $$6);
            return $$6;
         }
      }
   }

   private static String stringify(Tag pTag) {
      if (pTag instanceof FloatTag $$1) {
         return DECIMAL_FORMAT.format((double)$$1.getAsFloat());
      } else if (pTag instanceof DoubleTag $$2) {
         return DECIMAL_FORMAT.format($$2.getAsDouble());
      } else if (pTag instanceof ByteTag $$3) {
         return String.valueOf($$3.getAsByte());
      } else if (pTag instanceof ShortTag $$4) {
         return String.valueOf($$4.getAsShort());
      } else {
         return pTag instanceof LongTag $$5 ? String.valueOf($$5.getAsLong()) : pTag.getAsString();
      }
   }

   private static void lookupValues(List<String> pArguments, IntList pParameters, List<String> pOutput) {
      pOutput.clear();
      pParameters.forEach(p_312583_ -> pOutput.add(pArguments.get(p_312583_)));
   }

   private InstantiatedFunction<T> substituteAndParse(List<String> pArgumentNames, List<String> pArgumentValues, CommandDispatcher<T> pDispatcher) throws FunctionInstantiationException {
      List<UnboundEntryAction<T>> $$3 = new ArrayList<>(this.entries.size());
      List<String> $$4 = new ArrayList<>(pArgumentValues.size());

      for (MacroFunction.Entry<T> $$5 : this.entries) {
         lookupValues(pArgumentValues, $$5.parameters(), $$4);
         $$3.add($$5.instantiate($$4, pDispatcher, this.id));
      }

      return new PlainTextFunction<>(this.id().withPath(p_312634_ -> p_312634_ + "/" + pArgumentNames.hashCode()), $$3);
   }

   interface Entry<T> {
      IntList parameters();

      UnboundEntryAction<T> instantiate(List<String> pArguments, CommandDispatcher<T> pDispatcher, ResourceLocation pFunction) throws FunctionInstantiationException;
   }

   static class MacroEntry<T extends ExecutionCommandSource<T>> implements MacroFunction.Entry<T> {
      private final StringTemplate template;
      private final IntList parameters;
      private final T compilationContext;

      public MacroEntry(StringTemplate pTemplate, IntList pParameters, T pCompilationContext) {
         this.template = pTemplate;
         this.parameters = pParameters;
         this.compilationContext = pCompilationContext;
      }

      @Override
      public IntList parameters() {
         return this.parameters;
      }

      @Override
      public UnboundEntryAction<T> instantiate(List<String> pArguments, CommandDispatcher<T> pDispatcher, ResourceLocation pFunction) throws FunctionInstantiationException {
         String $$3 = this.template.substitute(pArguments);

         try {
            return CommandFunction.parseCommand(pDispatcher, this.compilationContext, new StringReader($$3));
         } catch (CommandSyntaxException var6) {
            throw new FunctionInstantiationException(
               Component.translatable("commands.function.error.parse", Component.translationArg(pFunction), $$3, var6.getMessage())
            );
         }
      }
   }

   static class PlainTextEntry<T> implements MacroFunction.Entry<T> {
      private final UnboundEntryAction<T> compiledAction;

      public PlainTextEntry(UnboundEntryAction<T> pCompiledAction) {
         this.compiledAction = pCompiledAction;
      }

      @Override
      public IntList parameters() {
         return IntLists.emptyList();
      }

      @Override
      public UnboundEntryAction<T> instantiate(List<String> pArguments, CommandDispatcher<T> pDispatcher, ResourceLocation pFunction) {
         return this.compiledAction;
      }
   }
}
