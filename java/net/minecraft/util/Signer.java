package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.security.PrivateKey;
import java.security.Signature;
import org.slf4j.Logger;

public interface Signer {
   Logger LOGGER = LogUtils.getLogger();

   byte[] sign(SignatureUpdater pUpdater);

   default byte[] sign(byte[] pSignature) {
      return this.sign(p_216394_ -> p_216394_.update(pSignature));
   }

   static Signer from(PrivateKey pPrivateKey, String pAlgorithm) {
      return p_216386_ -> {
         try {
            Signature $$3 = Signature.getInstance(pAlgorithm);
            $$3.initSign(pPrivateKey);
            p_216386_.update($$3::update);
            return $$3.sign();
         } catch (Exception var4) {
            throw new IllegalStateException("Failed to sign message", var4);
         }
      };
   }
}
