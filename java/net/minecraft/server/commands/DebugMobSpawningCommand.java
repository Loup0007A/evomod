package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;

public class DebugMobSpawningCommand {
   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> $$1 = (LiteralArgumentBuilder<CommandSourceStack>)Commands.literal("debugmobspawning")
         .requires(p_180113_ -> p_180113_.hasPermission(2));

      for (MobCategory $$2 : MobCategory.values()) {
         $$1.then(
            Commands.literal($$2.getName())
               .then(
                  Commands.argument("at", BlockPosArgument.blockPos())
                     .executes(p_180109_ -> spawnMobs((CommandSourceStack)p_180109_.getSource(), $$2, BlockPosArgument.getLoadedBlockPos(p_180109_, "at")))
               )
         );
      }

      pDispatcher.register($$1);
   }

   private static int spawnMobs(CommandSourceStack pSource, MobCategory pMobCategory, BlockPos pPos) {
      NaturalSpawner.spawnCategoryForPosition(pMobCategory, pSource.getLevel(), pPos);
      return 1;
   }
}
