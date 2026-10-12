package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class ParticleArgument implements ArgumentType<ParticleOptions> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "particle{foo:bar}");
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_PARTICLE = new DynamicCommandExceptionType(
      p_308358_ -> Component.translatableEscape("particle.notFound", p_308358_)
   );
   public static final DynamicCommandExceptionType ERROR_INVALID_OPTIONS = new DynamicCommandExceptionType(
      p_325596_ -> Component.translatableEscape("particle.invalidOptions", p_325596_)
   );
   private final HolderLookup.Provider registries;

   public ParticleArgument(CommandBuildContext pBuildContext) {
      this.registries = pBuildContext;
   }

   public static ParticleArgument particle(CommandBuildContext pBuildContext) {
      return new ParticleArgument(pBuildContext);
   }

   public static ParticleOptions getParticle(CommandContext<CommandSourceStack> pContext, String pName) {
      return (ParticleOptions)pContext.getArgument(pName, ParticleOptions.class);
   }

   public ParticleOptions parse(StringReader pReader) throws CommandSyntaxException {
      return readParticle(pReader, this.registries);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static ParticleOptions readParticle(StringReader pReader, HolderLookup.Provider pRegistries) throws CommandSyntaxException {
      ParticleType<?> $$2 = readParticleType(pReader, pRegistries.lookupOrThrow(Registries.PARTICLE_TYPE));
      return readParticle(pReader, (ParticleType<ParticleOptions>)$$2, pRegistries);
   }

   private static ParticleType<?> readParticleType(StringReader pReader, HolderLookup<ParticleType<?>> pParticleTypeLookup) throws CommandSyntaxException {
      ResourceLocation $$2 = ResourceLocation.read(pReader);
      ResourceKey<ParticleType<?>> $$3 = ResourceKey.create(Registries.PARTICLE_TYPE, $$2);
      return pParticleTypeLookup.get($$3).orElseThrow(() -> ERROR_UNKNOWN_PARTICLE.createWithContext(pReader, $$2)).value();
   }

   private static <T extends ParticleOptions> T readParticle(StringReader pReader, ParticleType<T> pParticleType, HolderLookup.Provider pRegistries) throws CommandSyntaxException {
      CompoundTag $$3;
      if (pReader.canRead() && pReader.peek() == '{') {
         $$3 = new TagParser(pReader).readStruct();
      } else {
         $$3 = new CompoundTag();
      }

      return (T)pParticleType.codec().codec().parse(pRegistries.createSerializationContext(NbtOps.INSTANCE), $$3).getOrThrow(ERROR_INVALID_OPTIONS::create);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      HolderLookup.RegistryLookup<ParticleType<?>> $$2 = this.registries.lookupOrThrow(Registries.PARTICLE_TYPE);
      return SharedSuggestionProvider.suggestResource($$2.listElementIds().map(ResourceKey::location), pBuilder);
   }
}
