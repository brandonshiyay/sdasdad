package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.host.IpConfig.IpV6Address;
import com.vmware.vim.binding.vim.host.IpConfig.IpV6AddressConfiguration;
import com.vmware.vim.vsan.binding.vim.vsan.FileServiceIpConfig;
import com.vmware.vsphere.client.vsan.base.data.AffinitySiteLocation;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

@TsModel
public class VsanFileServiceHostIpSettings {
   public boolean isDefault;
   public String address;
   public String dnsName;
   public AffinitySiteLocation affinityLocation;

   public static VsanFileServiceHostIpSettings fromVmodl(FileServiceIpConfig vmodl) {
      VsanFileServiceHostIpSettings ipSettings = new VsanFileServiceHostIpSettings();
      ipSettings.isDefault = BooleanUtils.isTrue(vmodl.isPrimary);
      ipSettings.dnsName = vmodl.fqdn;
      ipSettings.affinityLocation = AffinitySiteLocation.parse(vmodl.affinityLocation);
      if (vmodl.ipV6Config != null && !ArrayUtils.isEmpty(vmodl.ipV6Config.ipV6Address)) {
         ipSettings.address = vmodl.ipV6Config.ipV6Address[0].ipAddress;
      } else {
         ipSettings.address = vmodl.ipAddress;
      }

      return ipSettings;
   }

   public FileServiceIpConfig toVmodl(VsanFileServiceIpType type, String gateway, String mask) {
      FileServiceIpConfig vmodl = new FileServiceIpConfig();
      vmodl.dhcp = false;
      vmodl.isPrimary = this.isDefault;
      vmodl.gateway = gateway;
      vmodl.subnetMask = mask;
      vmodl.affinityLocation = this.affinityLocation.toVmodl();
      if (StringUtils.isNotBlank(this.dnsName)) {
         vmodl.fqdn = StringUtils.trim(this.dnsName);
      }

      switch(type) {
      case V4:
         vmodl.ipAddress = StringUtils.trim(this.address);
         vmodl.ipV6Config = null;
         break;
      case V6:
         vmodl.ipAddress = null;
         vmodl.ipV6Config = new IpV6AddressConfiguration();
         vmodl.ipV6Config.dhcpV6Enabled = false;
         vmodl.ipV6Config.autoConfigurationEnabled = false;
         IpV6Address address = new IpV6Address();
         address.ipAddress = StringUtils.trim(this.address);
         vmodl.ipV6Config.ipV6Address = new IpV6Address[]{address};
         break;
      default:
         throw new IllegalArgumentException("Unknonw IP type found which cannot handled: " + type);
      }

      return vmodl;
   }
}
