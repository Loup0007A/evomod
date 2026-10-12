package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.apache.commons.lang3.mutable.MutableInt;

public class FillBiomeCommand {
   public static final SimpleCommandExceptionType ERROR_NOT_LOADED = new SimpleCommandExceptionType(Component.translatable("argument.pos.unloaded"));
   private static final Dynamic2CommandExceptionType ERROR_VOLUME_TOO_LARGE = new Dynamic2CommandExceptionType(
      (p_308696_, p_308697_) -> Component.translatableEscape("commands.fillbiome.toobig", p_308696_, p_308697_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
      pDispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("fillbiome").requires(p_261890_ -> p_261890_.hasPermission(2)))
            .then(
               Commands.argument("from", BlockPosArgument.blockPos())
                  .then(
                     Commands.argument("to", BlockPosArgument.blockPos())
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("biome", ResourceArgument.resource(pContext, Registries.BIOME))
                                 .executes(
                                    p_262554_ -> fill(
                                          (CommandSourceStack)p_262554_.getSource(),
                                          BlockPosArgument.getLoadedBlockPos(p_262554_, "from"),
                                          BlockPosArgument.getLoadedBlockPos(p_262554_, "to"),
                                          ResourceArgument.getResource(p_262554_, "biome", Registries.BIOME),
                                          p_308700_ -> true
                                       )
                                 ))
                              .then(
                                 Commands.literal("replace")
                                    .then(
                                       Commands.argument("filter", ResourceOrTagArgument.resourceOrTag(pContext, Registries.BIOME))
                                          .executes(
                                             p_262544_ -> fill(
                                                   (CommandSourceStack)p_262544_.getSource(),
                                                   BlockPosArgument.getLoadedBlockPos(p_262544_, "from"),
                                                   BlockPosArgument.getLoadedBlockPos(p_262544_, "to"),
                                                   ResourceArgument.getResource(p_262544_, "biome", Registries.BIOME),
                                                   ResourceOrTagArgument.getResourceOrTag(p_262544_, "filter", Registries.BIOME)::test
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int quantize(int pValue) {
      return QuartPos.toBlock(QuartPos.fromBlock(pValue));
   }

   private static BlockPos quantize(BlockPos pPos) {
      return new BlockPos(quantize(pPos.getX()), quantize(pPos.getY()), quantize(pPos.getZ()));
   }

   private static BiomeResolver makeResolver(
      MutableInt pBiomeEntries, ChunkAccess pChunk, BoundingBox pTargetRegion, Holder<Biome> pReplacementBiome, Predicate<Holder<Biome>> pFilter
   ) {
      return (p_262550_, p_262551_, p_262552_, p_262553_) -> {
         int $$9 = QuartPos.toBlock(p_262550_);
         int $$10 = QuartPos.toBlock(p_262551_);
         int $$11 = QuartPos.toBlock(p_262552_);
         Holder<Biome> $$12 = pChunk.getNoiseBiome(p_262550_, p_262551_, p_262552_);
         if (pTargetRegion.isInside($$9, $$10, $$11) && pFilter.test($$12)) {
            pBiomeEntries.increment();
            return pReplacementBiome;
         } else {
            return $$12;
         }
      };
   }

   public static Either<Integer, CommandSyntaxException> fill(ServerLevel pLevel, BlockPos pFrom, BlockPos pTo, Holder<Biome> pBiome) {
      return fill(pLevel, pFrom, pTo, pBiome, p_262543_ -> true, p_308701_ -> {
      });
   }

   public static Either<Integer, CommandSyntaxException> fill(
      ServerLevel pLevel, BlockPos pFrom, BlockPos pTo, Holder<Biome> pBiome, Predicate<Holder<Biome>> pFilter, Consumer<Supplier<Component>> pMessageOutput
   ) {
      BlockPos $$6 = quantize(pFrom);
      BlockPos $$7 = quantize(pTo);
      BoundingBox $$8 = BoundingBox.fromCorners($$6, $$7);
      int $$9 = $$8.getXSpan() * $$8.getYSpan() * $$8.getZSpan();
      int $$10 = pLevel.getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
      if ($$9 > $$10) {
         return Either.right(ERROR_VOLUME_TOO_LARGE.create($$10, $$9));
      } else {
         List<ChunkAccess> $$11 = new ArrayList<>();

         for (int $$12 = SectionPos.blockToSectionCoord($$8.minZ()); $$12 <= SectionPos.blockToSectionCoord($$8.maxZ()); $$12++) {
            for (int $$13 = SectionPos.blockToSectionCoord($$8.minX()); $$13 <= SectionPos.blockToSectionCoord($$8.maxX()); $$13++) {
               ChunkAccess $$14 = pLevel.getChunk($$13, $$12, ChunkStatus.FULL, false);
               if ($$14 == null) {
                  return Either.right(ERROR_NOT_LOADED.create());
               }

               $$11.add($$14);
            }
         }

         MutableInt $$15 = new MutableInt(0);

         for (ChunkAccess $$16 : $$11) {
            $$16.fillBiomesFromNoise(makeResolver($$15, $$16, $$8, pBiome, pFilter), pLevel.getChunkSource().randomState().sampler());
            $$16.setUnsaved(true);
         }

         pLevel.getChunkSource().chunkMap.resendBiomesForChunks($$11);
         pMessageOutput.accept(
            () -> Component.translatable(
                  "commands.fillbiome.success.count", $$15.getValue(), $$8.minX(), $$8.minY(), $$8.minZ(), $$8.maxX(), $$8.maxY(), $$8.maxZ()
               )
         );
         return Either.left($$15.getValue());
      }
   }

   private static int fill(CommandSourceStack pSource, BlockPos pFrom, BlockPos pTo, Holder.Reference<Biome> pBiome, Predicate<Holder<Biome>> pFilter) throws CommandSyntaxException {
      Either<Integer, CommandSyntaxException> $$5 = fill(pSource.getLevel(), pFrom, pTo, pBiome, pFilter, p_308699_ -> pSource.sendSuccess(p_308699_, true));
      Optional<CommandSyntaxException> $$6 = $$5.right();
      if ($$6.isPresent()) {
         throw (CommandSyntaxException)$$6.get();
      } else {
         return (Integer)$$5.left().get();
      }
   }
}
