package net.minecraftforge.server.command;

import java.util.Locale;
import net.minecraft.commands.CommandSource;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ConnectionType;
import net.minecraftforge.network.NetworkContext;

public class TextComponentHelper {
   private TextComponentHelper() {
   }

   public static MutableComponent createComponentTranslation(CommandSource source, String translation, Object... args) {
      return isVanillaClient(source)
         ? Component.literal(String.format(Locale.ENGLISH, Language.getInstance().getOrDefault(translation), args))
         : Component.translatable(translation, args);
   }

   private static boolean isVanillaClient(CommandSource sender) {
      return sender instanceof ServerPlayer playerMP ? NetworkContext.get(playerMP.connection.getConnection()).getType() == ConnectionType.VANILLA : false;
   }
}
