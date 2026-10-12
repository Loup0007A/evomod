package net.minecraftforge.common;

import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.ApiStatus.Internal;

public record ToolAction(String name) {
   public static final Codec<ToolAction> CODEC = Codec.STRING.xmap(ToolAction::get, ToolAction::name).stable();
   private static final Map<String, ToolAction> ACTIONS = new ConcurrentHashMap<>();

   @Internal
   public ToolAction(String name) {
      this.name = name;
   }

   public static Collection<ToolAction> getActions() {
      return Collections.unmodifiableCollection(ACTIONS.values());
   }

   public static ToolAction get(String name) {
      return ACTIONS.computeIfAbsent(name, ToolAction::new);
   }
}
