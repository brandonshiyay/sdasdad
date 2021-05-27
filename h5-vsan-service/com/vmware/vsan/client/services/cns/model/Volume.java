package com.vmware.vsan.client.services.cns.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsan.client.services.common.data.StorageCompliance;
import java.util.ArrayList;
import java.util.List;

@TsModel
public class Volume {
   public String id;
   public String backingObjectId;
   public String path;
   public String name;
   public String iconId;
   public String fileshareName;
   public CnsVolumeType type;
   public List labels = new ArrayList();
   public String storagePolicyId;
   public List containerClusters = new ArrayList();
   public List vmData;
   public List datastoreData;
   public StorageCompliance compliance;
   public CnsHealthStatus healthStatus;
   public String datastoreType;
   public long capacity;
   public List podNames = new ArrayList();
}
