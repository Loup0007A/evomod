package net.minecraftforge.common.capabilities;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface ICapabilityProviderImpl<B extends ICapabilityProviderImpl<B>> extends ICapabilityProvider {
   void invalidateCaps();

   void reviveCaps();
}
