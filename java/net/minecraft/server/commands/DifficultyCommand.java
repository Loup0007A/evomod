package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;

public class DifficultyCommand {
   private static final DynamicCommandExceptionType ERROR_ALREADY_DIFFICULT = new DynamicCommandExceptionType(
      p_308648_ -> Component.translatableEscape("commands.difficulty.failure", p_308648_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> $$1 = Commands.literal("difficulty");

      for (Difficulty $$2 : Difficulty.values()) {
         $$1.then(Commands.literal($$2.getKey()).executes(p_136937_ -> setDifficulty((CommandSourceStack)p_136937_.getSource(), $$2)));
      }

      pDispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)$$1.requires(p_136943_ -> p_136943_.hasPermission(2))).executes(p_326237_ -> {
         Difficulty $$1x = ((CommandSourceStack)p_326237_.getSource()).getLevel().getDifficulty();
         ((CommandSourceStack)p_326237_.getSource()).sendSuccess(() -> Component.translatable("commands.difficulty.query", $$1x.getDisplayName()), false);
         return $$1x.getId();
      }));
   }

   public static int setDifficulty(CommandSourceStack pSource, Difficulty pDifficulty) throws CommandSyntaxException {
      MinecraftServer $$2 = pSource.getServer();
      if ($$2.getWorldData().getDifficulty() == pDifficulty) {
         throw ERROR_ALREADY_DIFFICULT.create(pDifficulty.getKey());
      } else {
         $$2.setDifficulty(pDifficulty, true);
         pSource.sendSuccess(() -> Component.translatable("commands.difficulty.success", pDifficulty.getDisplayName()), true);
         return 0;
      }
   }
}
