package net.minecraft.util;

import com.mojang.authlib.yggdrasil.ServicesKeyInfo;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.logging.LogUtils;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Collection;
import javax.annotation.Nullable;
import org.slf4j.Logger;

public interface SignatureValidator {
   SignatureValidator NO_VALIDATION = (p_216352_, p_216353_) -> true;
   Logger LOGGER = LogUtils.getLogger();

   boolean validate(SignatureUpdater pUpdater, byte[] pSignature);

   default boolean validate(byte[] pDigest, byte[] pSignature) {
      return this.validate(p_216374_ -> p_216374_.update(pDigest), pSignature);
   }

   private static boolean verifySignature(SignatureUpdater pUpdater, byte[] pSignatureBytes, Signature pSignature) throws SignatureException {
      pUpdater.update(pSignature::update);
      return pSignature.verify(pSignatureBytes);
   }

   static SignatureValidator from(PublicKey pPublicKey, String pAlgorithm) {
      return (p_216367_, p_216368_) -> {
         try {
            Signature $$4 = Signature.getInstance(pAlgorithm);
            $$4.initVerify(pPublicKey);
            return verifySignature(p_216367_, p_216368_, $$4);
         } catch (Exception var5) {
            LOGGER.error("Failed to verify signature", var5);
            return false;
         }
      };
   }

   @Nullable
   static SignatureValidator from(ServicesKeySet pServiceKeySet, ServicesKeyType pServiceKeyType) {
      Collection<ServicesKeyInfo> $$2 = pServiceKeySet.keys(pServiceKeyType);
      return $$2.isEmpty() ? null : (p_284690_, p_284691_) -> $$2.stream().anyMatch(p_216361_ -> {
            Signature $$3 = p_216361_.signature();

            try {
               return verifySignature(p_284690_, p_284691_, $$3);
            } catch (SignatureException var5) {
               LOGGER.error("Failed to verify Services signature", var5);
               return false;
            }
         });
   }
}
