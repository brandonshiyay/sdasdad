package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.host.IpConfig;
import com.vmware.vim.vsan.binding.vim.vsan.FileServiceDomainConfig;
import com.vmware.vim.vsan.binding.vim.vsan.FileServiceIpConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

@TsModel
public class VsanFileServiceDomain {
   public String name;
   public ActiveDirectoryConfig adConfig;
   public String gatewayAddress;
   public String mask;
   public String dnsServers;
   public String dnsSuffixes;
   public VsanFileServiceIpType ipType;
   public VsanFileServiceHostIpSettings[] ipSettings;
   private static final String DNS_SEPARATOR = ",";

   public VsanFileServiceDomain() {
      this.ipType = VsanFileServiceIpType.V4;
   }

   public static VsanFileServiceDomain fromVmodl(FileServiceDomainConfig vmodl) {
      if (vmodl == null) {
         return null;
      } else {
         VsanFileServiceDomain domain = new VsanFileServiceDomain();
         domain.name = vmodl.name;
         if (!ArrayUtils.isEmpty(vmodl.dnsServerAddresses)) {
            domain.dnsServers = String.join(",", Arrays.asList(vmodl.dnsServerAddresses));
         }

         if (!ArrayUtils.isEmpty(vmodl.dnsSuffixes)) {
            domain.dnsSuffixes = String.join(",", Arrays.asList(vmodl.dnsSuffixes));
         }

         if (vmodl.directoryServerConfig != null) {
            domain.adConfig = ActiveDirectoryConfig.fromVmodl(vmodl.directoryServerConfig);
         }

         FileServiceIpConfig[] ipConfigs = vmodl.fileServerIpConfig;
         if (ArrayUtils.isNotEmpty(ipConfigs)) {
            if (ipConfigs[0].ipV6Config != null) {
               domain.ipType = VsanFileServiceIpType.V6;
            } else {
               domain.ipType = VsanFileServiceIpType.V4;
            }

            domain.gatewayAddress = vmodl.fileServerIpConfig[0].gateway;
            domain.mask = vmodl.fileServerIpConfig[0].subnetMask;
            List ipSettings = new ArrayList(ipConfigs.length);
            FileServiceIpConfig[] var4 = ipConfigs;
            int var5 = ipConfigs.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               FileServiceIpConfig ipConfig = var4[var6];
               VsanFileServiceHostIpSettings ipSetting = VsanFileServiceHostIpSettings.fromVmodl(ipConfig);
               ipSettings.add(ipSetting);
            }

            domain.ipSettings = (VsanFileServiceHostIpSettings[])ipSettings.toArray(new VsanFileServiceHostIpSettings[ipSettings.size()]);
         }

         return domain;
      }
   }

   public FileServiceDomainConfig toVmodl() {
      FileServiceDomainConfig vmodl = new FileServiceDomainConfig();
      vmodl.name = this.name;
      List ipConfigs = new ArrayList();
      if (!ArrayUtils.isEmpty(this.ipSettings)) {
         VsanFileServiceHostIpSettings[] var3 = this.ipSettings;
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            VsanFileServiceHostIpSettings ipSetting = var3[var5];
            IpConfig ipConfig = ipSetting.toVmodl(this.ipType, this.gatewayAddress, this.mask);
            ipConfigs.add(ipConfig);
         }
      }

      vmodl.fileServerIpConfig = (FileServiceIpConfig[])ipConfigs.toArray(new FileServiceIpConfig[0]);
      vmodl.dnsServerAddresses = this.toArray(this.dnsServers);
      vmodl.dnsSuffixes = this.toArray(this.dnsSuffixes);
      if (this.adConfig != null) {
         vmodl.directoryServerConfig = this.adConfig.toVmodl();
      }

      return vmodl;
   }

   private String[] toArray(String unformatted) {
      return StringUtils.isBlank(unformatted.trim()) ? null : (String[])Arrays.stream(StringUtils.split(unformatted, ",")).map(StringUtils::trim).filter(StringUtils::isNotBlank).toArray((x$0) -> {
         return new String[x$0];
      });
   }
}
