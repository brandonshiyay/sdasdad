package com.vmware.vsan.client.services.evacuationstatus.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.health.VsanHealthData;
import java.util.Date;
import java.util.List;

@TsModel
public class EvacuationReport {
   public String uuid;
   public String decommissionMode;
   public PrecheckResultStatusType status;
   public boolean hasEvacuationReport;
   public Date reportDate;
   public long dataToMove;
   public String[] messages;
   public String[] nonCompliantObjects;
   public String[] inaccessibleObjects;
   public ClusterEvacuationCapacityData clusterCapacity;
   public ClusterEvacuationCapacityData vsanDirectClusterCapacity;
   public EvacuationTaskData runningTask;
   public VsanHealthData healthSummary;
   public List virtualObjects;
   public Long clusterRepairTime;
   public boolean isDurabilityPossible;
   public boolean isDurabilityGuaranteed;
   public PrecheckPersistenceData persistenceData;
}
