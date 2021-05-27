package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.common.data.LabelData;

@TsModel
public class VolumeFilterSpec {
   public String id;
   public String name;
   public String datastore;
   public String storagePolicy;
   public String containerCluster;
   public String complianceStatus;
   public String healthStatus;
   public String namespace;
   public String pvName;
   public String pvcName;
   public String podName;
   public LabelData[] labels;
   public long offset;
   public long limit;
}
