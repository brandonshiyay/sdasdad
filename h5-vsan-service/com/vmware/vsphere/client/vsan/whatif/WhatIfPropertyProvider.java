package com.vmware.vsphere.client.vsan.whatif;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.host.VsanSystemEx;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.VsanExtendedConfig;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanWhatIfEvacDetail;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanWhatIfEvacResult;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.config.VsanConfigService;
import com.vmware.vsan.client.services.virtualobjects.VirtualObjectsService;
import com.vmware.vsan.client.services.virtualobjects.data.VirtualObjectModel;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.util.FormatUtil;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class WhatIfPropertyProvider {
   public static final String IS_WHAT_IF_SUPPORTED = "isWhatIfSupported";
   @Autowired
   private VirtualObjectsService _virtualObjectsService;
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VsanConfigService vsanConfigService;

   @TsService
   public boolean getIsWhatIfSupported(ManagedObjectReference host) {
      return VsanCapabilityUtils.isWhatIfSupported(host);
   }

   @TsService
   public WhatIfResult getWhatIfResult(ManagedObjectReference hostRef, WhatIfSpec spec) throws Exception {
      if (StringUtils.isEmpty(spec.entityUuid)) {
         spec.entityUuid = (String)QueryUtil.getProperty(hostRef, "config.vsanHostConfig.clusterInfo.nodeUuid", (Object)null);
      }

      WhatIfResult result = new WhatIfResult();
      if (VsanCapabilityUtils.isWhatIfSupported(hostRef)) {
         Measure measure = new Measure("Collect WhatIfResult");
         Throwable var6 = null;

         VsanWhatIfEvacResult whatIfEvacResult;
         try {
            VsanConnection conn = this.vsanClient.getConnection(hostRef.getServerGuid());
            Throwable var8 = null;

            try {
               VsanSystemEx vsanSystemEx = conn.getVsanSystemEx(hostRef);
               whatIfEvacResult = vsanSystemEx.queryWhatIfEvacuationResult(spec.entityUuid);
            } catch (Throwable var31) {
               var8 = var31;
               throw var31;
            } finally {
               if (conn != null) {
                  if (var8 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var30) {
                        var8.addSuppressed(var30);
                     }
                  } else {
                     conn.close();
                  }
               }

            }
         } catch (Throwable var33) {
            var6 = var33;
            throw var33;
         } finally {
            if (measure != null) {
               if (var6 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var29) {
                     var6.addSuppressed(var29);
                  }
               } else {
                  measure.close();
               }
            }

         }

         List vsanObjects = Collections.emptyList();
         if (spec.detailed && this.hasDataObjects(whatIfEvacResult)) {
            Set vsanObjectUuids = new HashSet();
            vsanObjectUuids.addAll(Utils.arrayToList(whatIfEvacResult.ensureAccess.inaccessibleObjects));
            vsanObjectUuids.addAll(Utils.arrayToList(whatIfEvacResult.ensureAccess.incompliantObjects));
            vsanObjectUuids.addAll(Utils.arrayToList(whatIfEvacResult.evacAllData.inaccessibleObjects));
            vsanObjectUuids.addAll(Utils.arrayToList(whatIfEvacResult.evacAllData.incompliantObjects));
            vsanObjectUuids.addAll(Utils.arrayToList(whatIfEvacResult.noAction.inaccessibleObjects));
            vsanObjectUuids.addAll(Utils.arrayToList(whatIfEvacResult.noAction.incompliantObjects));
            vsanObjects = this._virtualObjectsService.listVirtualObjects(spec.clusterRef);
         }

         long repairTime = 0L;
         if (spec.clusterRef != null && (!ArrayUtils.isEmpty(whatIfEvacResult.ensureAccess.incompliantObjects) || !ArrayUtils.isEmpty(whatIfEvacResult.evacAllData.incompliantObjects) || !ArrayUtils.isEmpty(whatIfEvacResult.noAction.incompliantObjects))) {
            repairTime = this.getClusterRepairTime(spec.clusterRef);
         }

         boolean isDiskResourcePrecheckSupported = VsanCapabilityUtils.isDiskResourcePrecheckSupported(hostRef);
         WhatIfData ensureAccessibilityData = this.getWhatIfData(whatIfEvacResult.ensureAccess, spec.detailed, vsanObjects, false, repairTime, isDiskResourcePrecheckSupported);
         WhatIfData fullDataMigrationData = this.getWhatIfData(whatIfEvacResult.evacAllData, spec.detailed, vsanObjects, false, repairTime, isDiskResourcePrecheckSupported);
         WhatIfData noDataMigrationData = this.getWhatIfData(whatIfEvacResult.noAction, spec.detailed, vsanObjects, true, repairTime, isDiskResourcePrecheckSupported);
         result.ensureAccessibility = ensureAccessibilityData;
         result.fullDataMigration = fullDataMigrationData;
         result.noDataMigration = noDataMigrationData;
         result.isWhatIfSupported = true;
      } else {
         result.isWhatIfSupported = false;
      }

      return result;
   }

   private boolean hasDataObjects(VsanWhatIfEvacResult whatIfEvacResult) {
      return this.hasDataObjects(whatIfEvacResult.evacAllData) || this.hasDataObjects(whatIfEvacResult.ensureAccess) || this.hasDataObjects(whatIfEvacResult.noAction);
   }

   private boolean hasDataObjects(VsanWhatIfEvacDetail whatIfEvacDetail) {
      return whatIfEvacDetail.incompliantObjects != null && whatIfEvacDetail.incompliantObjects.length > 0 || whatIfEvacDetail.inaccessibleObjects != null && whatIfEvacDetail.inaccessibleObjects.length > 0;
   }

   @TsService
   public long getClusterRepairTime(ManagedObjectReference clusterRef) {
      ConfigInfoEx configInfoEx = this.vsanConfigService.getConfigInfoEx(clusterRef);
      VsanExtendedConfig extendedConfig = configInfoEx.getExtendedConfig();
      Long objectRepairTime = extendedConfig != null ? extendedConfig.getObjectRepairTimer() : null;
      return objectRepairTime != null ? objectRepairTime : 0L;
   }

   private WhatIfData getWhatIfData(VsanWhatIfEvacDetail whatIfEvacDetail, Boolean detailed, List virtualObjects, boolean isForNoAction, long repairTime, boolean isDiskResourcePrecheckSupported) {
      WhatIfData result = new WhatIfData();
      result.success = whatIfEvacDetail.success;
      result.successWithoutDataLoss = whatIfEvacDetail.success && ArrayUtils.isEmpty(whatIfEvacDetail.inaccessibleObjects);
      result.bytesToSync = whatIfEvacDetail.bytesToSync == null ? 0L : whatIfEvacDetail.bytesToSync;
      result.extraSpaceNeeded = whatIfEvacDetail.extraSpaceNeeded == null ? 0L : whatIfEvacDetail.extraSpaceNeeded;
      result.failedDueToInaccessibleObjects = whatIfEvacDetail.failedDueToInaccessibleObjects == null ? false : whatIfEvacDetail.failedDueToInaccessibleObjects;
      result.successWithInaccessibleOrNonCompliantObjects = whatIfEvacDetail.success && !ArrayUtils.isEmpty(whatIfEvacDetail.inaccessibleObjects) || !ArrayUtils.isEmpty(whatIfEvacDetail.incompliantObjects);
      if (detailed) {
         result.objects = new ArrayList();
         result.objects.addAll(this.getVsanObjects(whatIfEvacDetail.inaccessibleObjects, virtualObjects, VsanWhatIfComplianceStatus.INACCESSIBLE));
         result.objects.addAll(this.getVsanObjects(whatIfEvacDetail.incompliantObjects, virtualObjects, VsanWhatIfComplianceStatus.NOT_COMPLIANT));
      }

      result.summary = this.getSummary(whatIfEvacDetail, isForNoAction, isDiskResourcePrecheckSupported);
      if (ArrayUtils.isNotEmpty(whatIfEvacDetail.incompliantObjects)) {
         result.repairTime = repairTime;
      }

      return result;
   }

   public List getVsanObjects(String[] objectUUIDs, List virtualObjects, VsanWhatIfComplianceStatus status) {
      List result = new ArrayList();
      if (objectUUIDs == null) {
         return result;
      } else {
         Set uuids = new HashSet(Arrays.asList(objectUUIDs));
         Iterator var6 = virtualObjects.iterator();

         while(true) {
            while(var6.hasNext()) {
               VirtualObjectModel virtualObject = (VirtualObjectModel)var6.next();
               if (ArrayUtils.isEmpty(virtualObject.children)) {
                  if (uuids.contains(virtualObject.uid)) {
                     virtualObject.whatIfComplianceStatus = status;
                     result.add(virtualObject.cloneWithoutChildren());
                  }
               } else {
                  if (virtualObject.vmRef != null) {
                     virtualObject.healthState = null;
                     virtualObject.compositeHealth = null;
                     virtualObject.storagePolicy = null;
                  }

                  List children = new ArrayList();
                  VirtualObjectModel[] var9 = virtualObject.children;
                  int var10 = var9.length;

                  for(int var11 = 0; var11 < var10; ++var11) {
                     VirtualObjectModel child = var9[var11];
                     if (uuids.contains(child.uid)) {
                        child.whatIfComplianceStatus = status;
                        children.add(child.cloneWithoutChildren());
                     }
                  }

                  VirtualObjectModel clone = virtualObject.cloneWithoutChildren();
                  if (uuids.contains(clone.uid)) {
                     clone.whatIfComplianceStatus = status;
                  }

                  if (!children.isEmpty()) {
                     clone.children = (VirtualObjectModel[])children.toArray(new VirtualObjectModel[children.size()]);
                     result.add(clone);
                  } else if (uuids.contains(virtualObject.uid)) {
                     result.add(clone);
                  }
               }
            }

            return result;
         }
      }
   }

   private String getSummary(VsanWhatIfEvacDetail detail, Boolean isForNoAction, boolean isDiskResourcePrecheckSupported) {
      String result = "";
      long bytesToSynch = detail.bytesToSync == null ? 0L : detail.bytesToSync;
      String formattedBytesToSynch = FormatUtil.getStorageFormatted(bytesToSynch, 1L, -1L);
      String incompliantObjectsCount;
      if (detail.success) {
         if (bytesToSynch == 0L) {
            result = Utils.getLocalizedString("vsan.whatIf.summary.common.noDataMoved", " ");
         } else if (!isForNoAction) {
            if (ArrayUtils.isEmpty(detail.incompliantObjects)) {
               result = Utils.getLocalizedString("vsan.whatIf.summary.common.sufficientCapacity", " ");
            }

            result = Utils.getLocalizedString("vsan.whatIf.summary.common.storageMoved", result, formattedBytesToSynch, " ");
         }

         if (!ArrayUtils.isEmpty(detail.inaccessibleObjects)) {
            incompliantObjectsCount = detail.inaccessibleObjects == null ? String.valueOf(0) : String.valueOf(detail.inaccessibleObjects.length);
            result = Utils.getLocalizedString("vsan.whatIf.summary.success.inaccessibleObjects", result, incompliantObjectsCount, " ");
         }

         if (!ArrayUtils.isEmpty(detail.incompliantObjects)) {
            incompliantObjectsCount = detail.incompliantObjects == null ? String.valueOf(0) : String.valueOf(detail.incompliantObjects.length);
            result = Utils.getLocalizedString("vsan.whatIf.summary.success.nonCompliant", result, incompliantObjectsCount, " ");
         }
      } else if (detail.extraSpaceNeeded != null && detail.extraSpaceNeeded > 0L) {
         if (isDiskResourcePrecheckSupported) {
            result = Utils.getLocalizedString("vsan.whatIf.summary.failure.extraStorageNeeded.withoutCapacity");
         } else {
            incompliantObjectsCount = FormatUtil.getStorageFormatted(detail.extraSpaceNeeded, 1L, -1L);
            result = Utils.getLocalizedString("vsan.whatIf.summary.failure.extraStorageNeeded", incompliantObjectsCount);
         }
      } else if (detail.failedDueToInaccessibleObjects != null && detail.failedDueToInaccessibleObjects) {
         result = Utils.getLocalizedString("vsan.whatIf.summary.failure.dueToInaccessibleObjects");
      }

      if (result.equals("")) {
         if (detail.success) {
            result = Utils.getLocalizedString("vsan.whatIf.summary.common.success");
         } else {
            result = Utils.getLocalizedString("vsan.whatIf.summary.common.error");
         }
      }

      return result;
   }
}
