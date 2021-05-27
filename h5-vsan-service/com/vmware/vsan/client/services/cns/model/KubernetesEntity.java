package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;
import java.util.ArrayList;
import java.util.List;

@TsModel
public class KubernetesEntity {
   public String name;
   public KubernetesEntityType type;
   public String namespace;
   public List labels = new ArrayList();
   public KubernetesEntity persistentVolumeClaim;
   public VolumeContainerCluster guestCluster;
   public List podNames = new ArrayList();
}
