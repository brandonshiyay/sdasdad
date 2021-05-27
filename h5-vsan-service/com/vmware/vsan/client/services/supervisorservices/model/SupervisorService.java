package com.vmware.vsan.client.services.supervisorservices.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.ProxygenSerializer;
import java.util.List;

@TsModel
public class SupervisorService {
   public static final String KEY_REPO = "registryName";
   public static final String KEY_USERNAME = "registryUsername";
   public static final String KEY_PASSWORD = "registryPasswd";
   public String modelId;
   public String serviceId;
   public String name;
   public String description;
   public String eula;
   public String version;
   public String desiredVersion;
   public List availableVersions;
   public boolean isEnabled;
   public String repo;
   public String username;
   public String password;
   public String licenseKey;
   @ProxygenSerializer.ElementType(AdvancedSetting.class)
   public List advancedSettings;

   public String toString() {
      return "SupervisorService(modelId=" + this.modelId + ", serviceId=" + this.serviceId + ", name=" + this.name + ", description=" + this.description + ", eula=" + this.eula + ", version=" + this.version + ", desiredVersion=" + this.desiredVersion + ", availableVersions=" + this.availableVersions + ", isEnabled=" + this.isEnabled + ", repo=" + this.repo + ", username=" + this.username + ", licenseKey=" + this.licenseKey + ", advancedSettings=" + this.advancedSettings + ")";
   }
}
