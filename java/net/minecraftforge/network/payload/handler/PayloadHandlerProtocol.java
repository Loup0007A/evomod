package net.minecraftforge.network.payload.handler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.network.payload.PayloadConnection;
import net.minecraftforge.network.simple.BaseProtocol;

public interface PayloadHandlerProtocol<BUF extends FriendlyByteBuf, BASE extends CustomPacketPayload>
   extends BaseProtocol<PayloadHandlerFlow<BUF, BASE>, PayloadHandlerProtocol<BUF, BASE>>,
   PayloadConnection<BASE> {
}
