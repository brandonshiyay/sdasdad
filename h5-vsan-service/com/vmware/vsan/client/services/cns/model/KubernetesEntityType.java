package com.vmware.vsan.client.services.cns.model;

import com.vmware.vsphere.client.vsan.util.EnumUtils;

public enum KubernetesEntityType {
   PERSISTENT_VOLUME_CLAIM,
   POD,
   PERSISTENT_VOLUME,
   UNKNOWN;

   public static KubernetesEntityType fromString(String name) {
      return (KubernetesEntityType)EnumUtils.fromString(KubernetesEntityType.class, name, UNKNOWN);
   }
}
