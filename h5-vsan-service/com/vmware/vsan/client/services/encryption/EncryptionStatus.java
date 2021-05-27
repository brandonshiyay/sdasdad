package com.vmware.vsan.client.services.encryption;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.DataEncryptionConfig;
import com.vmware.vsphere.client.vsan.data.EncryptionState;
import org.apache.commons.lang3.BooleanUtils;

@TsModel
public class EncryptionStatus {
   public EncryptionState state;
   public String kmipClusterId;
   public Boolean eraseDisksBeforeUse;

   public static EncryptionStatus fromVmodl(DataEncryptionConfig dataEncryptionConfig) {
      EncryptionStatus dataAtRestEncryption = new EncryptionStatus();
      dataAtRestEncryption.state = EncryptionState.Disabled;
      if (dataEncryptionConfig == null) {
         return dataAtRestEncryption;
      } else {
         dataAtRestEncryption.state = BooleanUtils.isTrue(dataEncryptionConfig.encryptionEnabled) ? EncryptionState.Enabled : EncryptionState.Disabled;
         dataAtRestEncryption.kmipClusterId = dataEncryptionConfig.kmsProviderId == null ? "" : dataEncryptionConfig.kmsProviderId.id;
         dataAtRestEncryption.eraseDisksBeforeUse = dataEncryptionConfig.eraseDisksBeforeUse;
         if (dataAtRestEncryption.state == EncryptionState.Enabled && "".equals(dataAtRestEncryption.kmipClusterId)) {
            dataAtRestEncryption.state = EncryptionState.EnabledNoKmip;
         }

         return dataAtRestEncryption;
      }
   }
}
