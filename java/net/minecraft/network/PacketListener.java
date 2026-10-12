package net.minecraft.network;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketUtils;

public interface PacketListener {
   PacketFlow flow();

   ConnectionProtocol protocol();

   void onDisconnect(DisconnectionDetails pDetails);

   default void onPacketError(Packet pPacket, Exception pException) throws ReportedException {
      throw PacketUtils.makeReportedException(pException, pPacket, this);
   }

   default DisconnectionDetails createDisconnectionInfo(Component pReason, Throwable pError) {
      return new DisconnectionDetails(pReason);
   }

   boolean isAcceptingMessages();

   default boolean shouldHandleMessage(Packet<?> pPacket) {
      return this.isAcceptingMessages();
   }

   default void fillCrashReport(CrashReport pCrashReport) {
      CrashReportCategory $$1 = pCrashReport.addCategory("Connection");
      $$1.setDetail("Protocol", () -> this.protocol().id());
      $$1.setDetail("Flow", () -> this.flow().toString());
      this.fillListenerSpecificCrashDetails(pCrashReport, $$1);
   }

   default void fillListenerSpecificCrashDetails(CrashReport pCrashReport, CrashReportCategory pCategory) {
   }
}
