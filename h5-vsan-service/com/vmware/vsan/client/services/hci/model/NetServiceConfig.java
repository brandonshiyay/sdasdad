package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vim.ClusterComputeResource.HostVmkNicInfo;
import com.vmware.vim.binding.vim.host.IpConfig;
import com.vmware.vim.binding.vim.host.IpRouteConfig;
import com.vmware.vim.binding.vim.host.IpConfig.IpV6Address;
import com.vmware.vim.binding.vim.host.IpConfig.IpV6AddressConfiguration;
import com.vmware.vim.binding.vim.host.VirtualNic.IpRouteSpec;
import com.vmware.vim.binding.vim.host.VirtualNic.Specification;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;

@TsModel
public class NetServiceConfig {
   public Service service;
   public boolean useVlan;
   public int vlan;
   public String dvpgName;
   public ManagedObjectReference existingDvpgMor;
   public NetServiceConfig.Protocol protocol;
   public NetServiceConfig.IpType ipv4IpType;
   public NetServiceConfig.HostIpv4Config[] hostIpv4Configs;
   public NetServiceConfig.IpType ipv6IpType;
   public NetServiceConfig.HostIpv6Config[] hostIpv6Configs;

   public HostVmkNicInfo getHostVmkNicInfo(String hostName) {
      HostVmkNicInfo result = new HostVmkNicInfo();
      result.service = this.service.getText();
      result.nicSpec = new Specification();
      result.nicSpec.ip = this.getIpConfig(hostName);
      result.nicSpec.ipRouteSpec = this.getIpRouteSpec(hostName);
      return result;
   }

   private IpConfig getIpConfig(String hostName) {
      IpConfig result = new IpConfig();
      if (this.protocol == NetServiceConfig.Protocol.IPV4 || this.protocol == NetServiceConfig.Protocol.MIXED) {
         result.dhcp = this.ipv4IpType == NetServiceConfig.IpType.DHCP;
         if (!result.dhcp) {
            NetServiceConfig.HostIpv4Config ipv4Config = this.getHostIpv4Config(hostName);
            result.ipAddress = ipv4Config.ipAddress;
            result.subnetMask = ipv4Config.subnetMask;
         }
      }

      if (this.protocol == NetServiceConfig.Protocol.IPV6 || this.protocol == NetServiceConfig.Protocol.MIXED) {
         result.ipV6Config = new IpV6AddressConfiguration();
         switch(this.ipv6IpType) {
         case DHCP:
            result.ipV6Config.dhcpV6Enabled = true;
            break;
         case ROUTER_ADVERTISEMENT:
            result.ipV6Config.autoConfigurationEnabled = true;
            break;
         case STATIC:
            result.ipV6Config.ipV6Address = new IpV6Address[]{new IpV6Address()};
            result.ipV6Config.autoConfigurationEnabled = false;
            IpV6Address ipv6Address = result.ipV6Config.ipV6Address[0];
            NetServiceConfig.HostIpv6Config ipv6Config = this.getHostIpv6Config(hostName);
            ipv6Address.ipAddress = ipv6Config.ipAddress;
            ipv6Address.prefixLength = ipv6Config.prefixLength;
            ipv6Address.operation = "add";
         }
      }

      return result;
   }

   private NetServiceConfig.HostIpv4Config getHostIpv4Config(String hostName) {
      if (this.hostIpv4Configs != null) {
         NetServiceConfig.HostIpv4Config[] var2 = this.hostIpv4Configs;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            NetServiceConfig.HostIpv4Config config = var2[var4];
            if (config.hostname.equals(hostName)) {
               return config;
            }
         }
      }

      return null;
   }

   private NetServiceConfig.HostIpv6Config getHostIpv6Config(String hostName) {
      if (this.hostIpv6Configs != null) {
         NetServiceConfig.HostIpv6Config[] var2 = this.hostIpv6Configs;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            NetServiceConfig.HostIpv6Config config = var2[var4];
            if (config.hostname.equals(hostName)) {
               return config;
            }
         }
      }

      return null;
   }

   private IpRouteSpec getIpRouteSpec(String hostName) {
      IpRouteSpec result = new IpRouteSpec();
      result.ipRouteConfig = new IpRouteConfig();
      if ((this.protocol == NetServiceConfig.Protocol.IPV4 || this.protocol == NetServiceConfig.Protocol.MIXED) && this.ipv4IpType != NetServiceConfig.IpType.DHCP) {
         result.ipRouteConfig.defaultGateway = this.getHostIpv4Config(hostName).defaultGateway;
      }

      if ((this.protocol == NetServiceConfig.Protocol.IPV6 || this.protocol == NetServiceConfig.Protocol.MIXED) && this.ipv6IpType == NetServiceConfig.IpType.STATIC) {
         result.ipRouteConfig.ipV6DefaultGateway = this.getHostIpv6Config(hostName).defaultGateway;
      }

      return result;
   }

   @TsModel
   public static class HostIpv6Config {
      public String hostname;
      public String ipAddress;
      public int prefixLength;
      public String defaultGateway;
   }

   @TsModel
   public static class HostIpv4Config {
      public String hostname;
      public String ipAddress;
      public String subnetMask;
      public String defaultGateway;
   }

   @TsModel
   public static enum IpType {
      STATIC,
      DHCP,
      ROUTER_ADVERTISEMENT;
   }

   @TsModel
   public static enum Protocol {
      IPV4,
      IPV6,
      MIXED;
   }
}
