package net.minecraftforge.network.simple.handler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.simple.BaseProtocol;
import net.minecraftforge.network.simple.SimpleConnection;

public interface SimpleHandlerProtocol<BUF extends FriendlyByteBuf, BASE>
   extends BaseProtocol<SimpleHandlerFlow<BUF, BASE>, SimpleHandlerProtocol<BUF, BASE>>,
   SimpleConnection<BASE> {
}
