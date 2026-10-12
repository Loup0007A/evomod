package net.minecraftforge.network.simple.handler;

import net.minecraftforge.event.network.CustomPayloadEvent;

public interface SimplePacket<C> {
   boolean handle(C var1, CustomPayloadEvent.Context var2);
}
