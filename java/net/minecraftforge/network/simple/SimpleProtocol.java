package net.minecraftforge.network.simple;

import net.minecraft.network.FriendlyByteBuf;

public interface SimpleProtocol<BUF extends FriendlyByteBuf, BASE>
   extends BaseProtocol<SimpleFlow<BUF, BASE>, SimpleProtocol<BUF, BASE>>,
   SimpleConnection<BASE> {
}
