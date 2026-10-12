package net.minecraft.network.chat;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.SignedArgument;

public record SignableCommand<S>(List<SignableCommand.Argument<S>> arguments) {
   public static <S> boolean hasSignableArguments(ParseResults<S> pParseResults) {
      return !of(pParseResults).arguments().isEmpty();
   }

   public static <S> SignableCommand<S> of(ParseResults<S> pResults) {
      String $$1 = pResults.getReader().getString();
      CommandContextBuilder<S> $$2 = pResults.getContext();
      CommandContextBuilder<S> $$3 = $$2;
      List<SignableCommand.Argument<S>> $$4 = collectArguments($$1, $$2);

      CommandContextBuilder<S> $$5;
      while (($$5 = $$3.getChild()) != null && $$5.getRootNode() != $$2.getRootNode()) {
         $$4.addAll(collectArguments($$1, $$5));
         $$3 = $$5;
      }

      return new SignableCommand<>($$4);
   }

   private static <S> List<SignableCommand.Argument<S>> collectArguments(String pKey, CommandContextBuilder<S> pContextBuilder) {
      List<SignableCommand.Argument<S>> $$2 = new ArrayList<>();

      for (ParsedCommandNode<S> $$3 : pContextBuilder.getNodes()) {
         CommandNode $$5 = $$3.getNode();
         if ($$5 instanceof ArgumentCommandNode) {
            ArgumentCommandNode<S, ?> $$4 = (ArgumentCommandNode<S, ?>)$$5;
            if ($$4.getType() instanceof SignedArgument) {
               ParsedArgument<S, ?> $$5x = (ParsedArgument<S, ?>)pContextBuilder.getArguments().get($$4.getName());
               if ($$5x != null) {
                  String $$6 = $$5x.getRange().get(pKey);
                  $$2.add(new SignableCommand.Argument<>($$4, $$6));
               }
            }
         }
      }

      return $$2;
   }

   @Nullable
   public SignableCommand.Argument<S> getArgument(String pArgument) {
      for (SignableCommand.Argument<S> $$1 : this.arguments) {
         if (pArgument.equals($$1.name())) {
            return $$1;
         }
      }

      return null;
   }

   public static record Argument<S>(ArgumentCommandNode<S, ?> node, String value) {
      public String name() {
         return this.node.getName();
      }
   }
}
