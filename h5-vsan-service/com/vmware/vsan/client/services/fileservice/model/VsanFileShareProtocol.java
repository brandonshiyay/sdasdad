package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public enum VsanFileShareProtocol {
   SMB("SMB"),
   NFSv3("NFSv3"),
   NFSv4("NFSv4"),
   NFSV3_AND_NFSV4("NFSV3_AND_NFSV4"),
   UNKNOWN("FileShareProtocol_Unknown");

   private static final Log logger = LogFactory.getLog(VsanFileShareProtocol.class);
   public final String[] value;

   private VsanFileShareProtocol(String value) {
      this.value = new String[]{value};
   }

   public static VsanFileShareProtocol parse(String[] protocols) {
      if (ArrayUtils.isEmpty(protocols)) {
         return NFSV3_AND_NFSV4;
      } else if (protocols.length > 1) {
         return NFSV3_AND_NFSV4;
      } else {
         Optional found = Arrays.stream(values()).filter((protocol) -> {
            return ArrayUtils.contains(protocol.value, protocols[0]);
         }).findFirst();
         if (found.isPresent()) {
            return (VsanFileShareProtocol)found.get();
         } else {
            logger.warn("Matched protocol not found, return unknown as default. Original protocol is: " + protocols[0]);
            return UNKNOWN;
         }
      }
   }

   public static String[] toVmodlProtocols(VsanFileShareProtocol protocol) {
      switch(protocol) {
      case NFSv3:
      case NFSv4:
      case SMB:
         return protocol.value;
      case NFSV3_AND_NFSV4:
      default:
         return (String[])((String[])ArrayUtils.addAll(NFSv3.value, NFSv4.value));
      }
   }
}
