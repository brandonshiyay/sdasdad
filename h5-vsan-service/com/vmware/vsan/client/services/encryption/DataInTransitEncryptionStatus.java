package com.vmware.vsan.client.services.encryption;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.DataInTransitEncryptionConfig;
import org.apache.commons.lang3.BooleanUtils;

@TsModel
public class DataInTransitEncryptionStatus {
   public boolean isEnabled;
   public Integer rekeyInterval;

   public static DataInTransitEncryptionStatus fromVmodl(DataInTransitEncryptionConfig encryptionConfig) {
      DataInTransitEncryptionStatus status = new DataInTransitEncryptionStatus();
      status.isEnabled = false;
      if (encryptionConfig == null) {
         return status;
      } else {
         status.isEnabled = BooleanUtils.isTrue(encryptionConfig.enabled);
         status.rekeyInterval = encryptionConfig.rekeyInterval;
         return status;
      }
   }
}
