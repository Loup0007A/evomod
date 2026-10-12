package com.mojang.blaze3d.preprocessor;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class GlslPreprocessor {
   private static final String C_COMMENT = "/\\*(?:[^*]|\\*+[^*/])*\\*+/";
   private static final String LINE_COMMENT = "//[^\\v]*";
   private static final Pattern REGEX_MOJ_IMPORT = Pattern.compile(
      "(#(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*moj_import(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*(?:\"(.*)\"|<(.*)>))"
   );
   private static final Pattern REGEX_VERSION = Pattern.compile("(#(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*version(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*(\\d+))\\b");
   private static final Pattern REGEX_ENDS_WITH_WHITESPACE = Pattern.compile("(?:^|\\v)(?:\\s|/\\*(?:[^*]|\\*+[^*/])*\\*+/|(//[^\\v]*))*\\z");

   public List<String> process(String pShaderData) {
      GlslPreprocessor.Context $$1 = new GlslPreprocessor.Context();
      List<String> $$2 = this.processImports(pShaderData, $$1, "");
      $$2.set(0, this.setVersion($$2.get(0), $$1.glslVersion));
      return $$2;
   }

   private List<String> processImports(String pShaderData, GlslPreprocessor.Context pContext, String pIncludeDirectory) {
      int $$3 = pContext.sourceId;
      int $$4 = 0;
      String $$5 = "";
      List<String> $$6 = Lists.newArrayList();
      Matcher $$7 = REGEX_MOJ_IMPORT.matcher(pShaderData);

      while ($$7.find()) {
         if (!isDirectiveDisabled(pShaderData, $$7, $$4)) {
            String $$8 = $$7.group(2);
            boolean $$9 = $$8 != null;
            if (!$$9) {
               $$8 = $$7.group(3);
            }

            if ($$8 != null) {
               String $$10 = pShaderData.substring($$4, $$7.start(1));
               String $$11 = pIncludeDirectory + $$8;
               String $$12 = this.applyImport($$9, $$11);
               if (!Strings.isNullOrEmpty($$12)) {
                  if (!StringUtil.endsWithNewLine($$12)) {
                     $$12 = $$12 + System.lineSeparator();
                  }

                  pContext.sourceId++;
                  int $$13 = pContext.sourceId;
                  List<String> $$14 = this.processImports($$12, pContext, $$9 ? FileUtil.getFullResourcePath($$11) : "");
                  $$14.set(0, String.format(Locale.ROOT, "#line %d %d\n%s", 0, $$13, this.processVersions($$14.get(0), pContext)));
                  if (!StringUtil.isBlank($$10)) {
                     $$6.add($$10);
                  }

                  $$6.addAll($$14);
               } else {
                  String $$15 = $$9 ? String.format(Locale.ROOT, "/*#moj_import \"%s\"*/", $$8) : String.format(Locale.ROOT, "/*#moj_import <%s>*/", $$8);
                  $$6.add($$5 + $$10 + $$15);
               }

               int $$16 = StringUtil.lineCount(pShaderData.substring(0, $$7.end(1)));
               $$5 = String.format(Locale.ROOT, "#line %d %d", $$16, $$3);
               $$4 = $$7.end(1);
            }
         }
      }

      String $$17 = pShaderData.substring($$4);
      if (!StringUtil.isBlank($$17)) {
         $$6.add($$5 + $$17);
      }

      return $$6;
   }

   private String processVersions(String pVersionData, GlslPreprocessor.Context pContext) {
      Matcher $$2 = REGEX_VERSION.matcher(pVersionData);
      if ($$2.find() && isDirectiveEnabled(pVersionData, $$2)) {
         pContext.glslVersion = Math.max(pContext.glslVersion, Integer.parseInt($$2.group(2)));
         return pVersionData.substring(0, $$2.start(1)) + "/*" + pVersionData.substring($$2.start(1), $$2.end(1)) + "*/" + pVersionData.substring($$2.end(1));
      } else {
         return pVersionData;
      }
   }

   private String setVersion(String pVersionData, int pGlslVersion) {
      Matcher $$2 = REGEX_VERSION.matcher(pVersionData);
      return $$2.find() && isDirectiveEnabled(pVersionData, $$2)
         ? pVersionData.substring(0, $$2.start(2)) + Math.max(pGlslVersion, Integer.parseInt($$2.group(2))) + pVersionData.substring($$2.end(2))
         : pVersionData;
   }

   private static boolean isDirectiveEnabled(String pShaderData, Matcher pMatcher) {
      return !isDirectiveDisabled(pShaderData, pMatcher, 0);
   }

   private static boolean isDirectiveDisabled(String pShaderData, Matcher pMatcher, int pOffset) {
      int $$3 = pMatcher.start() - pOffset;
      if ($$3 == 0) {
         return false;
      } else {
         Matcher $$4 = REGEX_ENDS_WITH_WHITESPACE.matcher(pShaderData.substring(pOffset, pMatcher.start()));
         if (!$$4.find()) {
            return true;
         } else {
            int $$5 = $$4.end(1);
            return $$5 == pMatcher.start();
         }
      }
   }

   @Nullable
   public abstract String applyImport(boolean pUseFullPath, String pDirectory);

   @OnlyIn(Dist.CLIENT)
   static final class Context {
      int glslVersion;
      int sourceId;
   }
}
