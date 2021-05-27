package com.vmware.vsan.client.services.resyncing;

import com.fasterxml.jackson.databind.JsonNode;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vim.host.VsanInternalSystem;
import com.vmware.vim.binding.vim.vsan.host.ConfigInfo;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vmomi.core.Future;
import com.vmware.vim.vsan.binding.vim.cluster.VsanObjectSystem;
import com.vmware.vim.vsan.binding.vim.cluster.VsanSyncingObjectFilter;
import com.vmware.vim.vsan.binding.vim.host.VsanSystemEx;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanComponentSyncState;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanObjectSyncState;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanSyncingObjectQueryResult;
import com.vmware.vim.vsan.binding.vim.vsan.host.VsanSyncingObjectRecoveryDetails;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.resyncing.data.ResyncComponent;
import com.vmware.vsan.client.services.resyncing.data.ResyncMonitorData;
import com.vmware.vsan.client.services.resyncing.data.VsanSyncingObjectsQuerySpec;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsphere.client.vsan.base.data.ComponentIntent;
import com.vmware.vsphere.client.vsan.base.data.VsanComponent;
import com.vmware.vsphere.client.vsan.base.data.VsanObject;
import com.vmware.vsphere.client.vsan.base.util.Version;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VsanResyncingComponentsRetriever {
   private static final Version HOST_VERSION_2015 = new Version("6.0.0");
   private static final String[] HOST_PROPERTIES = new String[]{"name", "runtime.connectionState", "config.vsanHostConfig", "config.product.version"};
   private static final Log _logger = LogFactory.getLog(VsanResyncingComponentsRetriever.class);
   private static final VsanProfiler _profiler = new VsanProfiler(VsanResyncingComponentsRetriever.class);
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VcClient vcClient;

   public ResyncMonitorData getVsanResyncObjects(ManagedObjectReference clusterRef, VsanSyncingObjectsQuerySpec spec) throws Exception {
      VsanResyncingComponentsRetriever.HostsData hostsData = this.getHostsData(clusterRef);
      if (VsanCapabilityUtils.getCapabilities(clusterRef).isResyncETAImprovementSupported) {
         ResyncMonitorData result = this.queryResyncingObjects(clusterRef, spec, hostsData.hostNodeUuidToHostNames);
         result.isResyncFilterApiSupported = true;
         return result;
      } else {
         return this.getResyncObjects(hostsData, spec);
      }
   }

   private ResyncMonitorData queryResyncingObjects(ManagedObjectReference clusterRef, VsanSyncingObjectsQuerySpec spec, Map hostNodeUuidToHostNames) throws Exception {
      List filters = new ArrayList();
      String[] var5 = spec.resyncTypes;
      int var6 = var5.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         String type = var5[var7];
         VsanSyncingObjectFilter filter = new VsanSyncingObjectFilter();
         filter.offset = (long)spec.start;
         filter.resyncType = type;
         filter.resyncStatus = spec.status;
         filter.numberOfObjects = (long)spec.limit;
         filters.add(filter);
      }

      try {
         Measure measure = new Measure("Retrieving resyncing objects");
         Throwable var26 = null;

         try {
            List resyncingObjects = new ArrayList();
            Future[] resyncingObjectsFutures = this.getResyncingObjectsFutures(clusterRef, measure, filters);
            Future[] var29 = resyncingObjectsFutures;
            int var10 = resyncingObjectsFutures.length;

            for(int var11 = 0; var11 < var10; ++var11) {
               Future future = var29[var11];
               VsanSyncingObjectQueryResult data = (VsanSyncingObjectQueryResult)future.get();
               resyncingObjects.add(new ResyncMonitorData(data, hostNodeUuidToHostNames));
            }

            ResyncMonitorData var30 = this.combineResyncingObjects(resyncingObjects);
            return var30;
         } catch (Throwable var22) {
            var26 = var22;
            throw var22;
         } finally {
            if (measure != null) {
               if (var26 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var21) {
                     var26.addSuppressed(var21);
                  }
               } else {
                  measure.close();
               }
            }

         }
      } catch (Exception var24) {
         _logger.error("Failed to retrieve resyncing objects", var24);
         return new ResyncMonitorData();
      }
   }

   private Future[] getResyncingObjectsFutures(ManagedObjectReference clusterRef, Measure measure, List filters) throws Exception {
      List futures = new ArrayList(filters.size());
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var6 = null;

      try {
         VsanObjectSystem vsanObjectSystem = conn.getVsanObjectSystem();
         Iterator var8 = filters.iterator();

         while(var8.hasNext()) {
            VsanSyncingObjectFilter filter = (VsanSyncingObjectFilter)var8.next();
            Future future = measure.newFuture("VsanObjectSystem.querySyncingVsanObjectsSummary");
            vsanObjectSystem.querySyncingVsanObjectsSummary(clusterRef, filter, future);
            futures.add(future);
         }
      } catch (Throwable var18) {
         var6 = var18;
         throw var18;
      } finally {
         if (conn != null) {
            if (var6 != null) {
               try {
                  conn.close();
               } catch (Throwable var17) {
                  var6.addSuppressed(var17);
               }
            } else {
               conn.close();
            }
         }

      }

      return (Future[])futures.toArray(new Future[0]);
   }

   private ResyncMonitorData combineResyncingObjects(List resyncingObjects) {
      if (resyncingObjects.size() == 1) {
         return (ResyncMonitorData)resyncingObjects.get(0);
      } else if (resyncingObjects.size() > 1) {
         ResyncMonitorData result = null;
         Iterator var3 = resyncingObjects.iterator();

         while(var3.hasNext()) {
            ResyncMonitorData object = (ResyncMonitorData)var3.next();
            if (object != null) {
               if (result == null) {
                  result = object;
               } else {
                  result.uniteResyncingObjects(object);
               }
            }
         }

         return result;
      } else {
         return new ResyncMonitorData();
      }
   }

   private ResyncMonitorData getResyncObjects(VsanResyncingComponentsRetriever.HostsData hostsData, VsanSyncingObjectsQuerySpec spec) throws Exception {
      List hostRefsEnhancedApi = new ArrayList();
      List hostRefsLegacy = new ArrayList();
      Iterator var5 = hostsData.hostConnectionStates.keySet().iterator();

      while(var5.hasNext()) {
         ManagedObjectReference hostRef = (ManagedObjectReference)var5.next();
         if (ConnectionState.connected.equals(hostsData.hostConnectionStates.get(hostRef))) {
            if (VsanCapabilityUtils.isResyncEnhancedApiSupported(hostRef)) {
               hostRefsEnhancedApi.add(hostRef);
            } else {
               Version esxVersion = (Version)hostsData.hostVersions.get(hostRef);
               if (esxVersion != null && esxVersion.compareTo(HOST_VERSION_2015) >= 0) {
                  hostRefsLegacy.add(hostRef);
               }
            }
         }
      }

      if (!CollectionUtils.isEmpty(hostRefsEnhancedApi)) {
         ResyncMonitorData result = this.getResyncData(hostRefsEnhancedApi, spec, hostsData.hostNodeUuidToHostNames);
         result.isResyncFilterApiSupported = true;
         return result;
      } else if (!CollectionUtils.isEmpty(hostRefsLegacy)) {
         return this.getLegacyVsanResyncObjects(hostRefsLegacy, hostsData.hostNodeUuidToHostNames);
      } else {
         return new ResyncMonitorData();
      }
   }

   private ResyncMonitorData getResyncData(List hostRefs, VsanSyncingObjectsQuerySpec spec, Map hostNodeUuidToHostNames) throws Exception {
      Iterator var4 = hostRefs.iterator();

      while(true) {
         if (var4.hasNext()) {
            ManagedObjectReference hostRef = (ManagedObjectReference)var4.next();
            VsanConnection conn = this.vsanClient.getConnection(hostRef.getServerGuid());
            Throwable var7 = null;

            ResyncMonitorData var12;
            try {
               VsanSystemEx vsanSystemEx = conn.getVsanSystemEx(hostRef);
               if (vsanSystemEx == null) {
                  continue;
               }

               try {
                  VsanProfiler.Point point = _profiler.point("vsanSystemEx.querySyncingVsanObjects(null)");
                  Throwable var10 = null;

                  try {
                     VsanSyncingObjectQueryResult syncingObjects = vsanSystemEx.querySyncingVsanObjects((String[])null, spec.start, spec.limit, spec.includeSummary);
                     if (syncingObjects != null && !ArrayUtils.isEmpty(syncingObjects.objects)) {
                        var12 = new ResyncMonitorData(syncingObjects, hostNodeUuidToHostNames);
                        return var12;
                     }

                     var12 = new ResyncMonitorData();
                  } catch (Throwable var42) {
                     var10 = var42;
                     throw var42;
                  } finally {
                     if (point != null) {
                        if (var10 != null) {
                           try {
                              point.close();
                           } catch (Throwable var41) {
                              var10.addSuppressed(var41);
                           }
                        } else {
                           point.close();
                        }
                     }

                  }
               } catch (Exception var44) {
                  _logger.error("Failed to retrieve syncing objects", var44);
                  continue;
               }
            } catch (Throwable var45) {
               var7 = var45;
               throw var45;
            } finally {
               if (conn != null) {
                  if (var7 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var40) {
                        var7.addSuppressed(var40);
                     }
                  } else {
                     conn.close();
                  }
               }

            }

            return var12;
         }

         return new ResyncMonitorData();
      }
   }

   private ResyncMonitorData getLegacyVsanResyncObjects(List hostRefs, Map hostNodeUuidToHostNames) throws Exception {
      Iterator var3 = hostRefs.iterator();

      while(true) {
         if (var3.hasNext()) {
            ManagedObjectReference hostRef = (ManagedObjectReference)var3.next();
            VcConnection conn = this.vcClient.getConnection(hostRef.getServerGuid());
            Throwable var6 = null;

            ResyncMonitorData var42;
            try {
               VsanInternalSystem vsanInternalSystem = conn.getVsanInternalSystem(hostRef);
               if (vsanInternalSystem == null) {
                  continue;
               }

               String resyncObjectsJsonStr = null;

               try {
                  VsanProfiler.Point point = _profiler.point("vsanInternalSystem.querySyncingVsanObjects(null)");
                  Throwable var10 = null;

                  try {
                     resyncObjectsJsonStr = vsanInternalSystem.querySyncingVsanObjects((String[])null);
                  } catch (Throwable var37) {
                     var10 = var37;
                     throw var37;
                  } finally {
                     if (point != null) {
                        if (var10 != null) {
                           try {
                              point.close();
                           } catch (Throwable var36) {
                              var10.addSuppressed(var36);
                           }
                        } else {
                           point.close();
                        }
                     }

                  }
               } catch (Exception var39) {
                  _logger.error("Failed to retrieve syncing objects", var39);
                  continue;
               }

               Set resyncObjects = VsanResyncingComponentsRetriever.VsanJsonParser.parseVsanResyncObjects(resyncObjectsJsonStr, hostNodeUuidToHostNames);
               var42 = new ResyncMonitorData(this.convertLegacyDataToVsanSyncingObjects(resyncObjects), hostNodeUuidToHostNames);
            } catch (Throwable var40) {
               var6 = var40;
               throw var40;
            } finally {
               if (conn != null) {
                  if (var6 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var35) {
                        var6.addSuppressed(var35);
                     }
                  } else {
                     conn.close();
                  }
               }

            }

            return var42;
         }

         return new ResyncMonitorData();
      }
   }

   private VsanSyncingObjectQueryResult convertLegacyDataToVsanSyncingObjects(Set resyncObjects) {
      VsanSyncingObjectQueryResult syncingObjectQueryResult = new VsanSyncingObjectQueryResult(0L, 0L, -1L, new VsanObjectSyncState[0], (VsanSyncingObjectRecoveryDetails)null);
      if (CollectionUtils.isEmpty(resyncObjects)) {
         return syncingObjectQueryResult;
      } else {
         long componentsCount = 0L;
         long componentsBytesToSync = 0L;
         long recoveryEta = -1L;
         List syncObjects = new ArrayList();
         Iterator var10 = resyncObjects.iterator();

         while(var10.hasNext()) {
            VsanObject resyncObject = (VsanObject)var10.next();
            List components = new ArrayList();
            Iterator var13 = resyncObject.rootConfig.children.iterator();

            while(var13.hasNext()) {
               VsanComponent vsanComponent = (VsanComponent)var13.next();
               if (vsanComponent.byteToSync > 0L) {
                  VsanComponentSyncState vsanResyncComponent = new VsanComponentSyncState(vsanComponent.componentUuid, vsanComponent.capacityDiskUuid, vsanComponent.hostUuid, vsanComponent.byteToSync, vsanComponent.recoveryEta, new String[]{this.intentToResyncReason(vsanComponent.intent)});
                  components.add(vsanResyncComponent);
                  ++componentsCount;
                  componentsBytesToSync += vsanComponent.byteToSync;
                  recoveryEta = Math.max(vsanComponent.recoveryEta, recoveryEta);
               }
            }

            VsanObjectSyncState vsanSyncObject = new VsanObjectSyncState(resyncObject.vsanObjectUuid, (VsanComponentSyncState[])components.toArray(new VsanComponentSyncState[components.size()]));
            syncObjects.add(vsanSyncObject);
         }

         syncingObjectQueryResult.objects = (VsanObjectSyncState[])syncObjects.toArray(new VsanObjectSyncState[syncObjects.size()]);
         syncingObjectQueryResult.setTotalObjectsToSync(componentsCount);
         syncingObjectQueryResult.setTotalBytesToSync(componentsBytesToSync);
         syncingObjectQueryResult.setTotalRecoveryETA(recoveryEta);
         return syncingObjectQueryResult;
      }
   }

   private String intentToResyncReason(ComponentIntent intent) {
      switch(intent) {
      case REPAIR:
      case FIXCOMPLIANCE:
         return ResyncComponent.ResyncReasonCode.repair.toString();
      case DECOM:
         return ResyncComponent.ResyncReasonCode.evacuate.toString();
      case REBALANCE:
         return ResyncComponent.ResyncReasonCode.rebalance.toString();
      case POLICYCHANGE:
         return ResyncComponent.ResyncReasonCode.reconfigure.toString();
      case MOVE:
         return ResyncComponent.ResyncReasonCode.dying_evacuate.toString();
      case STALE:
         return ResyncComponent.ResyncReasonCode.stale.toString();
      case MERGE_CONTACT:
         return ResyncComponent.ResyncReasonCode.merge_concat.toString();
      case FORMAT_CHANGE:
         return ResyncComponent.ResyncReasonCode.object_format_change.toString();
      default:
         _logger.warn("Invalid intent code received from server side:" + intent.toString());
         return ResyncComponent.ResyncReasonCode.VsanSyncReason_Unknown.toString();
      }
   }

   private VsanResyncingComponentsRetriever.HostsData getHostsData(ManagedObjectReference clusterRef) throws Exception {
      VsanResyncingComponentsRetriever.HostsData hostsData = new VsanResyncingComponentsRetriever.HostsData();
      Map hostToVsanHostConfigInfo = new HashMap();
      HashMap hostNames = new HashMap();

      try {
         DataServiceResponse response = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "host", HostSystem.class.getSimpleName(), HOST_PROPERTIES);
         if (response == null) {
            return hostsData;
         } else {
            Iterator var6 = response.getResourceObjects().iterator();

            while(var6.hasNext()) {
               Object resourceObject = var6.next();
               ManagedObjectReference hostRef = (ManagedObjectReference)resourceObject;
               hostNames.put(hostRef, (String)response.getProperty(hostRef, "name"));
               hostsData.hostConnectionStates.put(hostRef, (ConnectionState)response.getProperty(hostRef, "runtime.connectionState"));
               hostToVsanHostConfigInfo.put(hostRef, (ConfigInfo)response.getProperty(hostRef, "config.vsanHostConfig"));
               hostsData.hostVersions.put(hostRef, new Version((String)response.getProperty(hostRef, "config.product.version")));
            }

            var6 = hostNames.keySet().iterator();

            while(true) {
               while(var6.hasNext()) {
                  ManagedObjectReference hostRef = (ManagedObjectReference)var6.next();
                  ConfigInfo vsanConfig = (ConfigInfo)hostToVsanHostConfigInfo.get(hostRef);
                  if (vsanConfig != null && vsanConfig.enabled && vsanConfig.clusterInfo != null && vsanConfig.clusterInfo.nodeUuid != null) {
                     String nodeUuid = vsanConfig.clusterInfo.nodeUuid;
                     String hostName = (String)hostNames.get(hostRef);
                     hostsData.hostNodeUuidToHostNames.put(nodeUuid, hostName);
                  } else {
                     hostsData.hostConnectionStates.remove(hostRef);
                  }
               }

               return hostsData;
            }
         }
      } catch (Exception var11) {
         _logger.error("Failed to retrieve host names: ", var11);
         return hostsData;
      }
   }

   private static class VsanJsonParser {
      private static final String DOM_OBJECTS_KEY = "dom_objects";
      private static final String LSOM_OBJECTS_KEY = "lsom_objects";
      private static final String CONFIG_KEY = "config";
      private static final String CONTENT_KEY = "content";
      private static final String TYPE_KEY = "type";
      private static final String COMPONENT_TYPE = "Component";
      private static final String ATTRIBUTE_KEY = "attributes";
      private static final String OWNER_KEY = "owner";

      public static Set parseVsanResyncObjects(String resyncDataJsonStr, Map hostNodeUuidsToHostNames) throws Exception {
         HashSet result = new HashSet();

         try {
            VsanProfiler.Point point = VsanResyncingComponentsRetriever._profiler.point("parseVsanResyncObjects");
            Throwable var4 = null;

            HashSet var6;
            try {
               JsonNode root = Utils.getJsonRootNode(resyncDataJsonStr);
               if (root != null) {
                  Map componentUuidsToHostNames = getComponentUuidToHostNameMap(root, hostNodeUuidsToHostNames);
                  JsonNode domObjects = root.get("dom_objects");
                  if (domObjects == null) {
                     HashSet var27 = result;
                     return var27;
                  }

                  Iterator fnIterator = domObjects.fieldNames();

                  while(fnIterator.hasNext()) {
                     String vsanObjectUuid = (String)fnIterator.next();
                     JsonNode vsanObjectNode = domObjects.path(vsanObjectUuid).path("config").path("content");
                     if (!vsanObjectNode.isMissingNode()) {
                        List components = getComponentObjects(vsanObjectNode, componentUuidsToHostNames);
                        if (components.size() > 0) {
                           VsanObject vmObjectData = new VsanObject(vsanObjectUuid, components);
                           result.add(vmObjectData);
                        }
                     }
                  }

                  return result;
               }

               var6 = result;
            } catch (Throwable var23) {
               var4 = var23;
               throw var23;
            } finally {
               if (point != null) {
                  if (var4 != null) {
                     try {
                        point.close();
                     } catch (Throwable var22) {
                        var4.addSuppressed(var22);
                     }
                  } else {
                     point.close();
                  }
               }

            }

            return var6;
         } catch (Exception var25) {
            VsanResyncingComponentsRetriever._logger.error("Failed to parse vsan resyncing data JSON string. ", var25);
            throw var25;
         }
      }

      private static Map getComponentUuidToHostNameMap(JsonNode root, Map hostNodeUuidsToHostNames) {
         Map result = new HashMap();
         JsonNode lsomObjects = root.get("lsom_objects");
         if (lsomObjects == null) {
            return result;
         } else {
            Iterator fnIterator = lsomObjects.fieldNames();

            while(fnIterator.hasNext()) {
               String vsanObjectUuid = (String)fnIterator.next();
               JsonNode vsanObjectNode = lsomObjects.get(vsanObjectUuid);
               if (vsanObjectNode != null && !vsanObjectNode.isMissingNode()) {
                  String ownerUuid = vsanObjectNode.path("owner").textValue();
                  if (ownerUuid != null && hostNodeUuidsToHostNames.containsKey(ownerUuid)) {
                     result.put(vsanObjectUuid, hostNodeUuidsToHostNames.get(ownerUuid));
                  }
               }
            }

            return result;
         }
      }

      private static List getComponentObjects(JsonNode node, Map componentUuidsToHostNames) {
         List components = new ArrayList();
         if (node != null && !node.isMissingNode() && node.has("type")) {
            if ("Component".equals(node.get("type").textValue())) {
               JsonNode attributeNode = node.get("attributes");
               if (attributeNode == null) {
                  VsanResyncingComponentsRetriever._logger.warn("Missing attributes field for component node.");
                  return components;
               }

               VsanComponent componentData = new VsanComponent(node, attributeNode, componentUuidsToHostNames);
               components.add(componentData);
            } else {
               Iterator childNodes = node.elements();

               while(childNodes.hasNext()) {
                  List childComponents = getComponentObjects((JsonNode)childNodes.next(), componentUuidsToHostNames);
                  components.addAll(childComponents);
               }
            }

            return components;
         } else {
            return components;
         }
      }
   }

   private static class HostsData {
      Map hostNodeUuidToHostNames;
      Map hostConnectionStates;
      Map hostVersions;

      private HostsData() {
         this.hostNodeUuidToHostNames = new HashMap();
         this.hostConnectionStates = new HashMap();
         this.hostVersions = new HashMap();
      }

      // $FF: synthetic method
      HostsData(Object x0) {
         this();
      }
   }
}
