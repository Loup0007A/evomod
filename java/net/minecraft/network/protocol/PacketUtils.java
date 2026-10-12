package net.minecraft.network.protocol;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.network.PacketListener;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import org.slf4j.Logger;

public class PacketUtils {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static <T extends PacketListener> void ensureRunningOnSameThread(Packet<T> pPacket, T pProcessor, ServerLevel pLevel) throws RunningOnDifferentThreadException {
      ensureRunningOnSameThread(pPacket, pProcessor, pLevel.getServer());
   }

   public static <T extends PacketListener> void ensureRunningOnSameThread(Packet<T> pPacket, T pProcessor, BlockableEventLoop<?> pExecutor) throws RunningOnDifferentThreadException {
      if (!pExecutor.isSameThread()) {
         pExecutor.executeIfPossible(() -> {
            if (pProcessor.shouldHandleMessage(pPacket)) {
               try {
                  pPacket.handle(pProcessor);
               } catch (Exception var4) {
                  if (var4 instanceof ReportedException $$3 && $$3.getCause() instanceof OutOfMemoryError) {
                     throw makeReportedException(var4, pPacket, pProcessor);
                  }

                  pProcessor.onPacketError(pPacket, var4);
               }
            } else {
               LOGGER.debug("Ignoring packet due to disconnection: {}", pPacket);
            }
         });
         throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
      }
   }

   public static <T extends PacketListener> ReportedException makeReportedException(Exception pException, Packet<T> pPacket, T pPacketListener) {
      if (pException instanceof ReportedException $$3) {
         fillCrashReport($$3.getReport(), pPacketListener, pPacket);
         return $$3;
      } else {
         CrashReport $$4 = CrashReport.forThrowable(pException, "Main thread packet handler");
         fillCrashReport($$4, pPacketListener, pPacket);
         return new ReportedException($$4);
      }
   }

   public static <T extends PacketListener> void fillCrashReport(CrashReport pCrashReport, T pPacketListener, @Nullable Packet<T> pPacket) {
      if (pPacket != null) {
         CrashReportCategory $$3 = pCrashReport.addCategory("Incoming Packet");
         $$3.setDetail("Type", () -> pPacket.type().toString());
         $$3.setDetail("Is Terminal", () -> Boolean.toString(pPacket.isTerminal()));
         $$3.setDetail("Is Skippable", () -> Boolean.toString(pPacket.isSkippable()));
      }

      pPacketListener.fillCrashReport(pCrashReport);
   }
}
