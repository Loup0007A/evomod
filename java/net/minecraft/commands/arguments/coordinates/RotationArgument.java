package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class RotationArgument implements ArgumentType<Coordinates> {
   private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ ~", "~-5 ~5");
   public static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(Component.translatable("argument.rotation.incomplete"));

   public static RotationArgument rotation() {
      return new RotationArgument();
   }

   public static Coordinates getRotation(CommandContext<CommandSourceStack> pContext, String pName) {
      return (Coordinates)pContext.getArgument(pName, Coordinates.class);
   }

   public Coordinates parse(StringReader pReader) throws CommandSyntaxException {
      int $$1 = pReader.getCursor();
      if (!pReader.canRead()) {
         throw ERROR_NOT_COMPLETE.createWithContext(pReader);
      } else {
         WorldCoordinate $$2 = WorldCoordinate.parseDouble(pReader, false);
         if (pReader.canRead() && pReader.peek() == ' ') {
            pReader.skip();
            WorldCoordinate $$3 = WorldCoordinate.parseDouble(pReader, false);
            return new WorldCoordinates($$3, $$2, new WorldCoordinate(true, 0.0));
         } else {
            pReader.setCursor($$1);
            throw ERROR_NOT_COMPLETE.createWithContext(pReader);
         }
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
