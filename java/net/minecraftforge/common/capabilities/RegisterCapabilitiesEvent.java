package net.minecraftforge.common.capabilities;

import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import org.objectweb.asm.Type;

@Deprecated(
   forRemoval = true,
   since = "1.21"
)
public final class RegisterCapabilitiesEvent extends Event implements IModBusEvent {
   public <T> void register(Class<T> type) {
      Objects.requireNonNull(type, "Attempted to register a capability with invalid type");
      CapabilityManager.get(Type.getInternalName(type), null, true);
   }
}
