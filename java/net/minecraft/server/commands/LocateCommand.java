package net.minecraft.server.commands;

import com.google.common.base.Stopwatch;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;

public class LocateCommand {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final DynamicCommandExceptionType ERROR_STRUCTURE_NOT_FOUND = new DynamicCommandExceptionType(
      p_308765_ -> Component.translatableEscape("commands.locate.structure.not_found", p_308765_)
   );
   private static final DynamicCommandExceptionType ERROR_STRUCTURE_INVALID = new DynamicCommandExceptionType(
      p_308764_ -> Component.translatableEscape("commands.locate.structure.invalid", p_308764_)
   );
   private static final DynamicCommandExceptionType ERROR_BIOME_NOT_FOUND = new DynamicCommandExceptionType(
      p_308763_ -> Component.translatableEscape("commands.locate.biome.not_found", p_308763_)
   );
   private static final DynamicCommandExceptionType ERROR_POI_NOT_FOUND = new DynamicCommandExceptionType(
      p_308766_ -> Component.translatableEscape("commands.locate.poi.not_found", p_308766_)
   );
   private static final int MAX_STRUCTURE_SEARCH_RADIUS = 100;
   private static final int MAX_BIOME_SEARCH_RADIUS = 6400;
   private static final int BIOME_SAMPLE_RESOLUTION_HORIZONTAL = 32;
   private static final int BIOME_SAMPLE_RESOLUTION_VERTICAL = 64;
   private static final int POI_SEARCH_RADIUS = 256;

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("locate")
                     .requires(p_214470_ -> p_214470_.hasPermission(2)))
                  .then(
                     Commands.literal("structure")
                        .then(
                           Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
                              .executes(
                                 p_258233_ -> locateStructure(
                                       (CommandSourceStack)p_258233_.getSource(),
                                       ResourceOrTagKeyArgument.getResourceOrTagKey(p_258233_, "structure", Registries.STRUCTURE, ERROR_STRUCTURE_INVALID)
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("biome")
                     .then(
                        Commands.argument("biome", ResourceOrTagArgument.resourceOrTag(pContext, Registries.BIOME))
                           .executes(
                              p_258232_ -> locateBiome(
                                    (CommandSourceStack)p_258232_.getSource(), ResourceOrTagArgument.getResourceOrTag(p_258232_, "biome", Registries.BIOME)
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("poi")
                  .then(
                     Commands.argument("poi", ResourceOrTagArgument.resourceOrTag(pContext, Registries.POINT_OF_INTEREST_TYPE))
                        .executes(
                           p_258234_ -> locatePoi(
                                 (CommandSourceStack)p_258234_.getSource(),
                                 ResourceOrTagArgument.getResourceOrTag(p_258234_, "poi", Registries.POINT_OF_INTEREST_TYPE)
                              )
                        )
                  )
            )
      );
   }

   private static Optional<? extends HolderSet.ListBacked<Structure>> getHolders(
      ResourceOrTagKeyArgument.Result<Structure> pStructure, Registry<Structure> pStructureRegistry
   ) {
      return (Optional<? extends HolderSet.ListBacked<Structure>>)pStructure.unwrap()
         .map(p_258231_ -> pStructureRegistry.getHolder(p_258231_).map(p_214491_ -> HolderSet.direct(p_214491_)), pStructureRegistry::getTag);
   }

   private static int locateStructure(CommandSourceStack pSource, ResourceOrTagKeyArgument.Result<Structure> pStructure) throws CommandSyntaxException {
      Registry<Structure> $$2 = pSource.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
      HolderSet<Structure> $$3 = (HolderSet<Structure>)getHolders(pStructure, $$2).orElseThrow(() -> ERROR_STRUCTURE_INVALID.create(pStructure.asPrintable()));
      BlockPos $$4 = BlockPos.containing(pSource.getPosition());
      ServerLevel $$5 = pSource.getLevel();
      Stopwatch $$6 = Stopwatch.createStarted(Util.TICKER);
      Pair<BlockPos, Holder<Structure>> $$7 = $$5.getChunkSource().getGenerator().findNearestMapStructure($$5, $$3, $$4, 100, false);
      $$6.stop();
      if ($$7 == null) {
         throw ERROR_STRUCTURE_NOT_FOUND.create(pStructure.asPrintable());
      } else {
         return showLocateResult(pSource, pStructure, $$4, $$7, "commands.locate.structure.success", false, $$6.elapsed());
      }
   }

   private static int locateBiome(CommandSourceStack pSource, ResourceOrTagArgument.Result<Biome> pBiome) throws CommandSyntaxException {
      BlockPos $$2 = BlockPos.containing(pSource.getPosition());
      Stopwatch $$3 = Stopwatch.createStarted(Util.TICKER);
      Pair<BlockPos, Holder<Biome>> $$4 = pSource.getLevel().findClosestBiome3d(pBiome, $$2, 6400, 32, 64);
      $$3.stop();
      if ($$4 == null) {
         throw ERROR_BIOME_NOT_FOUND.create(pBiome.asPrintable());
      } else {
         return showLocateResult(pSource, pBiome, $$2, $$4, "commands.locate.biome.success", true, $$3.elapsed());
      }
   }

   private static int locatePoi(CommandSourceStack pSource, ResourceOrTagArgument.Result<PoiType> pPoiType) throws CommandSyntaxException {
      BlockPos $$2 = BlockPos.containing(pSource.getPosition());
      ServerLevel $$3 = pSource.getLevel();
      Stopwatch $$4 = Stopwatch.createStarted(Util.TICKER);
      Optional<Pair<Holder<PoiType>, BlockPos>> $$5 = $$3.getPoiManager().findClosestWithType(pPoiType, $$2, 256, PoiManager.Occupancy.ANY);
      $$4.stop();
      if ($$5.isEmpty()) {
         throw ERROR_POI_NOT_FOUND.create(pPoiType.asPrintable());
      } else {
         return showLocateResult(pSource, pPoiType, $$2, $$5.get().swap(), "commands.locate.poi.success", false, $$4.elapsed());
      }
   }

   public static int showLocateResult(
      CommandSourceStack pSource,
      ResourceOrTagArgument.Result<?> pResult,
      BlockPos pSourcePosition,
      Pair<BlockPos, ? extends Holder<?>> pResultWithPosition,
      String pTranslationKey,
      boolean pAbsoluteY,
      Duration pDuration
   ) {
      String $$7 = (String)pResult.unwrap()
         .map(
            p_248147_ -> pResult.asPrintable(), p_326290_ -> pResult.asPrintable() + " (" + ((Holder)pResultWithPosition.getSecond()).getRegisteredName() + ")"
         );
      return showLocateResult(pSource, pSourcePosition, pResultWithPosition, pTranslationKey, pAbsoluteY, $$7, pDuration);
   }

   public static int showLocateResult(
      CommandSourceStack pSource,
      ResourceOrTagKeyArgument.Result<?> pResult,
      BlockPos pSourcePosition,
      Pair<BlockPos, ? extends Holder<?>> pResultWithPosition,
      String pTranslationKey,
      boolean pAbsoluteY,
      Duration pDuration
   ) {
      String $$7 = (String)pResult.unwrap()
         .map(
            p_214498_ -> p_214498_.location().toString(),
            p_326287_ -> "#" + p_326287_.location() + " (" + ((Holder)pResultWithPosition.getSecond()).getRegisteredName() + ")"
         );
      return showLocateResult(pSource, pSourcePosition, pResultWithPosition, pTranslationKey, pAbsoluteY, $$7, pDuration);
   }

   private static int showLocateResult(
      CommandSourceStack pSource,
      BlockPos pSourcePosition,
      Pair<BlockPos, ? extends Holder<?>> pResultWithoutPosition,
      String pTranslationKey,
      boolean pAbsoluteY,
      String pElementName,
      Duration pDuration
   ) {
      BlockPos $$7 = (BlockPos)pResultWithoutPosition.getFirst();
      int $$8 = pAbsoluteY
         ? Mth.floor(Mth.sqrt((float)pSourcePosition.distSqr($$7)))
         : Mth.floor(dist(pSourcePosition.getX(), pSourcePosition.getZ(), $$7.getX(), $$7.getZ()));
      String $$9 = pAbsoluteY ? String.valueOf($$7.getY()) : "~";
      Component $$10 = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", $$7.getX(), $$9, $$7.getZ()))
         .withStyle(
            p_214489_ -> p_214489_.withColor(ChatFormatting.GREEN)
                  .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + $$7.getX() + " " + $$9 + " " + $$7.getZ()))
                  .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip")))
         );
      pSource.sendSuccess(() -> Component.translatable(pTranslationKey, pElementName, $$10, $$8), false);
      LOGGER.info("Locating element " + pElementName + " took " + pDuration.toMillis() + " ms");
      return $$8;
   }

   private static float dist(int pX1, int pZ1, int pX2, int pZ2) {
      int $$4 = pX2 - pX1;
      int $$5 = pZ2 - pZ1;
      return Mth.sqrt((float)($$4 * $$4 + $$5 * $$5));
   }
}
