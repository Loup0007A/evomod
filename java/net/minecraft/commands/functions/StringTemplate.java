package net.minecraft.commands.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;

public record StringTemplate(List<String> segments, List<String> variables) {
   public static StringTemplate fromString(String pName, int pLineNumber) {
      Builder<String> $$2 = ImmutableList.builder();
      Builder<String> $$3 = ImmutableList.builder();
      int $$4 = pName.length();
      int $$5 = 0;
      int $$6 = pName.indexOf(36);

      while ($$6 != -1) {
         if ($$6 != $$4 - 1 && pName.charAt($$6 + 1) == '(') {
            $$2.add(pName.substring($$5, $$6));
            int $$7 = pName.indexOf(41, $$6 + 1);
            if ($$7 == -1) {
               throw new IllegalArgumentException("Unterminated macro variable in macro '" + pName + "' on line " + pLineNumber);
            }

            String $$8 = pName.substring($$6 + 2, $$7);
            if (!isValidVariableName($$8)) {
               throw new IllegalArgumentException("Invalid macro variable name '" + $$8 + "' on line " + pLineNumber);
            }

            $$3.add($$8);
            $$5 = $$7 + 1;
            $$6 = pName.indexOf(36, $$5);
         } else {
            $$6 = pName.indexOf(36, $$6 + 1);
         }
      }

      if ($$5 == 0) {
         throw new IllegalArgumentException("Macro without variables on line " + pLineNumber);
      } else {
         if ($$5 != $$4) {
            $$2.add(pName.substring($$5));
         }

         return new StringTemplate($$2.build(), $$3.build());
      }
   }

   private static boolean isValidVariableName(String pVariableName) {
      for (int $$1 = 0; $$1 < pVariableName.length(); $$1++) {
         char $$2 = pVariableName.charAt($$1);
         if (!Character.isLetterOrDigit($$2) && $$2 != '_') {
            return false;
         }
      }

      return true;
   }

   public String substitute(List<String> pArguments) {
      StringBuilder $$1 = new StringBuilder();

      for (int $$2 = 0; $$2 < this.variables.size(); $$2++) {
         $$1.append(this.segments.get($$2)).append(pArguments.get($$2));
         CommandFunction.checkCommandLineLength($$1);
      }

      if (this.segments.size() > this.variables.size()) {
         $$1.append(this.segments.get(this.segments.size() - 1));
      }

      CommandFunction.checkCommandLineLength($$1);
      return $$1.toString();
   }
}
