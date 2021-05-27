package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.KeyValue;
import com.vmware.vim.vsan.binding.vim.vsan.FileShare;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public class VsanFileServiceShare {
   private static final int RED_THRESHOLD = 80;
   private static final Log logger = LogFactory.getLog(VsanFileServiceShare.class);
   public String uuid;
   public List objectUuids;
   public VsanFileServiceShareConfig config;
   public String host;
   public String serverIpAddress;
   public long usedCapacity;
   public int usageOverQuota;
   public boolean isOverRedThreshold;
   public boolean isOverSoftQuota;
   public VsanFileShareOwnerType managedBy;
   public Map accessPoints = new HashMap();
   public VsanFileShareProtocol protocol;
   public VsanFileShareNfsSecurityType nfsSecurityType;
   public SmbEncryptionOption smbEncryptionOption;
   public String fileServerFqdn;

   public static VsanFileServiceShare fromVmodl(FileShare vmodl) {
      VsanFileServiceShare share = new VsanFileServiceShare();
      share.uuid = vmodl.uuid;
      if (vmodl.runtime != null && ArrayUtils.isNotEmpty(vmodl.runtime.vsanObjectUuids)) {
         share.objectUuids = Arrays.asList(vmodl.runtime.vsanObjectUuids);
      } else {
         logger.warn("No vsanObjectUuids are assigned to share: " + share.uuid);
      }

      if (vmodl.runtime != null && ArrayUtils.isNotEmpty(vmodl.runtime.accessPoints)) {
         KeyValue[] var2 = vmodl.runtime.accessPoints;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            KeyValue pair = var2[var4];
            share.accessPoints.put(pair.key, pair.value);
         }
      }

      share.serverIpAddress = vmodl.runtime.address;
      share.config = VsanFileServiceShareConfig.fromVmodl(vmodl.config);
      if (vmodl.runtime != null) {
         if (vmodl.runtime.usedCapacity != null) {
            share.usedCapacity = vmodl.runtime.usedCapacity;
            share.updateUsageOverQuota();
         }

         share.host = StringUtils.isNotEmpty(vmodl.runtime.hostname) ? vmodl.runtime.hostname : "";
      }

      share.managedBy = VsanFileShareOwnerType.fromVmodl(vmodl.runtime.managedBy);
      share.protocol = VsanFileShareProtocol.parse(vmodl.config.protocols);
      share.nfsSecurityType = VsanFileShareNfsSecurityType.parse(vmodl.config.nfsSecType);
      if (vmodl.config.smbOptions != null) {
         share.smbEncryptionOption = SmbEncryptionOption.parse(vmodl.config.smbOptions.encryption);
      }

      share.fileServerFqdn = vmodl.runtime.fileServerFQDN;
      return share;
   }

   public void updateUsageOverQuota() {
      this.usageOverQuota = 0;
      this.isOverSoftQuota = false;
      this.isOverRedThreshold = false;
      if (this.usedCapacity != 0L && this.config != null) {
         double softQuotaInBytes;
         if (this.config.quota != null && this.config.quota != 0.0D && this.config.quotaSize != null) {
            softQuotaInBytes = this.config.quotaSize.multiplier * this.config.quota;
            double ratio = (double)this.usedCapacity / softQuotaInBytes;
            this.usageOverQuota = (int)Math.round(ratio * 100.0D);
            this.isOverRedThreshold = this.usageOverQuota > 80;
         }

         if (this.config.softQuota != null && this.config.softQuotaSize != null) {
            softQuotaInBytes = this.config.softQuotaSize.multiplier * this.config.softQuota;
            this.isOverSoftQuota = (double)this.usedCapacity > softQuotaInBytes;
         }

      }
   }
}
