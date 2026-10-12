package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.io.OutputStream;
import org.slf4j.Logger;

public class DebugLoggedPrintStream extends LoggedPrintStream {
   private static final Logger LOGGER = LogUtils.getLogger();

   public DebugLoggedPrintStream(String pName, OutputStream pOut) {
      super(pName, pOut);
   }

   @Override
   protected void logLine(String pString) {
      StackTraceElement[] $$1 = Thread.currentThread().getStackTrace();
      StackTraceElement $$2 = $$1[Math.min(3, $$1.length)];
      LOGGER.info("[{}]@.({}:{}): {}", new Object[]{this.name, $$2.getFileName(), $$2.getLineNumber(), pString});
   }
}
