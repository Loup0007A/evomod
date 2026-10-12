package net.minecraftforge.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.network.simple.BaseProtocol;

public interface PayloadProtocol<BUF extends FriendlyByteBuf, BASE extends CustomPacketPayload>
   extends BaseProtocol<PayloadFlow<BUF, BASE>, PayloadProtocol<BUF, BASE>>,
   PayloadConnection<BASE> {
}
