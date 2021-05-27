package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.ActiveDirectoryServerConfig;
import com.vmware.vim.vsan.binding.vim.vsan.DirectoryServerConfig;
import org.apache.commons.lang3.StringUtils;

@TsModel
public class ActiveDirectoryConfig {
   public String domain;
   public String username;
   public String password;
   public String organizationalUnit;

   public static ActiveDirectoryConfig fromVmodl(DirectoryServerConfig config) {
      ActiveDirectoryConfig adConfig = new ActiveDirectoryConfig();
      if (config instanceof ActiveDirectoryServerConfig) {
         ActiveDirectoryServerConfig adServerConfig = (ActiveDirectoryServerConfig)config;
         adConfig.domain = adServerConfig.activeDirectoryDomainName;
         adConfig.username = adServerConfig.username;
         adConfig.organizationalUnit = adServerConfig.organizationalUnit;
      }

      return adConfig;
   }

   public ActiveDirectoryServerConfig toVmodl() {
      if (StringUtils.isBlank(this.password)) {
         return null;
      } else {
         ActiveDirectoryServerConfig adServerConfig = new ActiveDirectoryServerConfig();
         adServerConfig.activeDirectoryDomainName = StringUtils.trim(this.domain);
         adServerConfig.username = StringUtils.trim(this.username);
         adServerConfig.password = StringUtils.trim(this.password);
         adServerConfig.organizationalUnit = StringUtils.trim(this.organizationalUnit);
         return adServerConfig;
      }
   }
}
