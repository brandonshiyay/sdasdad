package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.KeyValue;
import com.vmware.vim.binding.vim.vm.DefinedProfileSpec;
import com.vmware.vim.binding.vim.vm.ProfileSpec;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareConfig;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareNetPermission;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareSmbOptions;
import com.vmware.vsan.client.services.common.data.LabelData;
import com.vmware.vsphere.client.vsan.base.data.AffinitySiteLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

@TsModel
public class VsanFileServiceShareConfig {
   private static final String SIZE_PATTERN = "^\\d+[\\.\\d]*";
   private static final String METRIC_PATTERN = "[mgtMGT]{1}[bB]{0,1}$";
   private static final String ALL_ACCESS_IP_PATTERN = "*";
   public String name;
   public String domainName;
   public Double quota;
   public VsanFileServiceShareSize quotaSize;
   public Double softQuota;
   public VsanFileServiceShareSize softQuotaSize;
   public LabelData[] labels;
   public String policyId;
   public VsanFileServiceShareNetPermission[] netPermissions;
   public VsanFileServiceShareAccessType accessType;
   public VsanFileShareProtocol protocol;
   public VsanFileShareNfsSecurityType securityType;
   public SmbEncryptionOption smbEncryptionOption;
   public AffinitySiteLocation affinityLocation;
   public boolean isFileAnalyticsEnabled;

   public static VsanFileServiceShareConfig fromVmodl(FileShareConfig vmodl) {
      VsanFileServiceShareConfig share = new VsanFileServiceShareConfig();
      share.name = vmodl.name;
      share.domainName = vmodl.domainName;
      share.protocol = VsanFileShareProtocol.parse(vmodl.protocols);
      share.securityType = VsanFileShareNfsSecurityType.parse(vmodl.nfsSecType);
      if (vmodl.smbOptions != null) {
         share.smbEncryptionOption = SmbEncryptionOption.parse(vmodl.smbOptions.encryption);
      }

      share.quota = parseQuotaValue(vmodl.quota);
      share.quotaSize = parseQuotaMetric(vmodl.quota);
      share.softQuota = parseQuotaValue(vmodl.softQuota);
      share.softQuotaSize = parseQuotaMetric(vmodl.softQuota);
      share.isFileAnalyticsEnabled = BooleanUtils.isTrue(vmodl.fileIndexingEnabled);
      if (ArrayUtils.isNotEmpty(vmodl.labels)) {
         share.labels = (LabelData[])LabelData.fromKeyValue(vmodl.labels).toArray(new LabelData[0]);
      }

      ProfileSpec storagePolicy = vmodl.storagePolicy;
      if (storagePolicy instanceof DefinedProfileSpec) {
         share.policyId = ((DefinedProfileSpec)vmodl.storagePolicy).profileId;
      }

      share.affinityLocation = AffinitySiteLocation.parse(vmodl.affinityLocation);
      share.accessType = getShareAccessType(vmodl.permission);
      if (share.accessType == VsanFileServiceShareAccessType.CUSTOM_ACCESS) {
         share.netPermissions = getNetPermissions(vmodl.permission);
      }

      return share;
   }

   private static VsanFileServiceShareAccessType getShareAccessType(FileShareNetPermission[] permissions) {
      if (ArrayUtils.isEmpty(permissions)) {
         return VsanFileServiceShareAccessType.NO_ACCESS;
      } else {
         if (permissions.length == 1) {
            if (BooleanUtils.isTrue(permissions[0].allowRoot) && "*".equals(permissions[0].ips) && VsanFileServiceShareNetPermissionType.READ_WRITE.value.equals(permissions[0].permissions)) {
               return VsanFileServiceShareAccessType.ALL_ACCESS;
            }

            if ("*".equals(permissions[0].ips) && VsanFileServiceShareNetPermissionType.NO_ACCESS.value.equals(permissions[0].permissions)) {
               return VsanFileServiceShareAccessType.NO_ACCESS;
            }
         }

         return VsanFileServiceShareAccessType.CUSTOM_ACCESS;
      }
   }

   private static VsanFileServiceShareNetPermission[] getNetPermissions(FileShareNetPermission[] permissions) {
      if (ArrayUtils.isEmpty(permissions)) {
         return null;
      } else {
         List netPermissions = new ArrayList();
         FileShareNetPermission[] var2 = permissions;
         int var3 = permissions.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            FileShareNetPermission permission = var2[var4];
            netPermissions.add(VsanFileServiceShareNetPermission.fromVmodl(permission));
         }

         return (VsanFileServiceShareNetPermission[])netPermissions.toArray(new VsanFileServiceShareNetPermission[netPermissions.size()]);
      }
   }

   public FileShareConfig toVmodl() {
      FileShareConfig vmodl = new FileShareConfig();
      vmodl.name = this.name;
      vmodl.domainName = this.domainName;
      vmodl.quota = formatQuota(this.quota, this.quotaSize);
      vmodl.softQuota = formatQuota(this.softQuota, this.softQuotaSize);
      vmodl.protocols = this.protocol == null ? null : VsanFileShareProtocol.toVmodlProtocols(this.protocol);
      vmodl.nfsSecType = this.protocol == null ? null : toVmodlSecurityType(this.protocol, this.securityType);
      if (this.smbEncryptionOption != null && this.protocol == VsanFileShareProtocol.SMB) {
         vmodl.smbOptions = new FileShareSmbOptions();
         vmodl.smbOptions.encryption = this.smbEncryptionOption.value;
      }

      if (ArrayUtils.isNotEmpty(this.labels)) {
         vmodl.labels = LabelData.toKeyValue(this.labels);
      } else {
         vmodl.labels = new KeyValue[0];
      }

      if (this.policyId != null) {
         DefinedProfileSpec profileSpec = new DefinedProfileSpec();
         profileSpec.profileId = this.policyId;
         vmodl.storagePolicy = profileSpec;
      }

      vmodl.affinityLocation = this.affinityLocation.toVmodl();
      vmodl.fileIndexingEnabled = this.isFileAnalyticsEnabled;
      if (this.protocol == VsanFileShareProtocol.SMB) {
         return vmodl;
      } else {
         List netPermissions = new ArrayList();
         VsanFileServiceShareNetPermission netPermission;
         if (this.accessType == VsanFileServiceShareAccessType.NO_ACCESS) {
            netPermission = new VsanFileServiceShareNetPermission();
            netPermission.ipAddress = "*";
            netPermission.isRootSquashed = false;
            netPermission.isNoAccess = true;
            netPermissions.add(netPermission.toVmodl());
         } else if (this.accessType == VsanFileServiceShareAccessType.ALL_ACCESS) {
            netPermission = new VsanFileServiceShareNetPermission();
            netPermission.ipAddress = "*";
            netPermission.isRootSquashed = false;
            netPermission.isWriteAllowed = true;
            netPermissions.add(netPermission.toVmodl());
         } else if (ArrayUtils.isNotEmpty(this.netPermissions)) {
            VsanFileServiceShareNetPermission[] var8 = this.netPermissions;
            int var4 = var8.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               VsanFileServiceShareNetPermission netPermission = var8[var5];
               netPermissions.add(netPermission.toVmodl());
            }
         }

         vmodl.permission = (FileShareNetPermission[])netPermissions.toArray(new FileShareNetPermission[netPermissions.size()]);
         return vmodl;
      }
   }

   private static String toVmodlSecurityType(VsanFileShareProtocol protocol, VsanFileShareNfsSecurityType securityType) {
      switch(protocol) {
      case NFSv4:
         return securityType.value;
      case SMB:
         return null;
      case NFSv3:
      default:
         return VsanFileShareNfsSecurityType.AUTH_SYS.value;
      }
   }

   private static String formatQuota(Double quota, VsanFileServiceShareSize size) {
      if (quota != null && Double.compare(quota, NumberUtils.DOUBLE_ZERO) != 0 && size != null) {
         String formatOption = Math.rint(quota) == quota ? "%.0f" : "%.5f";
         return String.format(formatOption, quota) + (String)size.values.get(0);
      } else {
         return "0";
      }
   }

   private static Double parseQuotaValue(String value) {
      String quota = parse(value, "^\\d+[\\.\\d]*");
      return StringUtils.isEmpty(quota) ? null : Double.valueOf(quota);
   }

   private static VsanFileServiceShareSize parseQuotaMetric(String value) {
      String quota = parse(value, "[mgtMGT]{1}[bB]{0,1}$");
      return StringUtils.isEmpty(quota) ? null : VsanFileServiceShareSize.parse(quota);
   }

   private static String parse(String value, String patternStr) {
      if (StringUtils.isEmpty(value)) {
         return null;
      } else {
         Pattern pattern = Pattern.compile(patternStr);
         Matcher matcher = pattern.matcher(value);
         matcher.find();

         try {
            return matcher.group();
         } catch (Exception var5) {
            return null;
         }
      }
   }
}
