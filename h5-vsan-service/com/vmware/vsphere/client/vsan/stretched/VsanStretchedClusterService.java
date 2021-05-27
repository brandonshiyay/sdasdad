package com.vmware.vsphere.client.vsan.stretched;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.HostSystem.ConnectionState;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VSANWitnessHostInfo;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcStretchedClusterSystem;
import com.vmware.vim.vsan.binding.vim.vsan.host.DiskMapInfoEx;
import com.vmware.vise.data.query.ObjectReferenceService;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.stretchedcluster.model.DomainOrHostData;
import com.vmware.vsan.client.services.stretchedcluster.model.VsanHostsResult;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.NoOpMeasure;
import com.vmware.vsan.client.util.dataservice.query.DataServiceHelper;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VsanStretchedClusterService {
   private static final Log logger = LogFactory.getLog(VsanStretchedClusterService.class);
   private static final VsanProfiler _profiler = new VsanProfiler(VsanStretchedClusterService.class);
   private static final String[] DOMAIN_PROPERTIES = new String[]{"name", "isWitnessHost", "config.vsanHostConfig.faultDomainInfo.name", "runtime.inMaintenanceMode", "runtime.connectionState", "preferredFaultDomain"};
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private ObjectReferenceService objectReferenceService;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;
   @Autowired
   private DataServiceHelper dataServiceHelper;

   @TsService
   public List getWitnessHosts(ManagedObjectReference clusterRef) {
      List witnessHosts = new ArrayList();
      VSANWitnessHostInfo[] witnessHostInfos = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      try {
         VsanVcStretchedClusterSystem stretchedClusterSystem = conn.getVcStretchedClusterSystem();
         VsanProfiler.Point point;
         if (stretchedClusterSystem == null) {
            point = null;
            return point;
         }

         try {
            point = _profiler.point("stretchedClusterSystem.getWitnessHosts");
            Throwable var8 = null;

            try {
               witnessHostInfos = stretchedClusterSystem.getWitnessHosts(clusterRef);
            } catch (Throwable var34) {
               var8 = var34;
               throw var34;
            } finally {
               if (point != null) {
                  if (var8 != null) {
                     try {
                        point.close();
                     } catch (Throwable var33) {
                        var8.addSuppressed(var33);
                     }
                  } else {
                     point.close();
                  }
               }

            }
         } catch (Exception var36) {
            logger.error("Could not retrieve witness hosts " + var36.getMessage());
         }
      } catch (Throwable var37) {
         var5 = var37;
         throw var37;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var32) {
                  var5.addSuppressed(var32);
               }
            } else {
               conn.close();
            }
         }

      }

      if (witnessHostInfos != null) {
         VSANWitnessHostInfo[] var39 = witnessHostInfos;
         int var40 = witnessHostInfos.length;

         for(int var41 = 0; var41 < var40; ++var41) {
            VSANWitnessHostInfo witnessHost = var39[var41];
            if (witnessHost.host != null) {
               WitnessHostData witness = new WitnessHostData(witnessHost, clusterRef.getServerGuid());
               witnessHosts.add(witness);
            }
         }
      }

      return witnessHosts;
   }

   @TsService
   public int getAvailableWitnessHostsCount(ManagedObjectReference clusterRef) {
      try {
         WitnessHostData[] witnessHostsData = this.getWitnessHostData(clusterRef);
         return witnessHostsData == null ? 0 : (int)Arrays.stream(witnessHostsData).filter((hostData) -> {
            return !hostData.inMaintenanceMode;
         }).count();
      } catch (Exception var3) {
         throw new VsanUiLocalizableException("vsan.faultDomains.strechedCluster.witnessHostsData.fetch.error", var3);
      }
   }

   @TsService
   public WitnessHostData[] getWitnessHostData(ManagedObjectReference clusterRef) throws Exception {
      VSANWitnessHostInfo[] witnessInfos = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      try {
         VsanVcStretchedClusterSystem stretchedClusterSystem = conn.getVcStretchedClusterSystem();
         witnessInfos = stretchedClusterSystem.getWitnessHosts(clusterRef);
      } catch (Throwable var13) {
         var4 = var13;
         throw var13;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var12) {
                  var4.addSuppressed(var12);
               }
            } else {
               conn.close();
            }
         }

      }

      return this.getWitnessHostDataByWitnessInfo(witnessInfos, clusterRef);
   }

   public WitnessHostData[] getWitnessHostDataByWitnessInfo(VSANWitnessHostInfo[] witnessInfos, ManagedObjectReference clusterRef) throws Exception {
      if (witnessInfos != null && witnessInfos.length != 0) {
         List result = new ArrayList();
         VSANWitnessHostInfo[] var4 = witnessInfos;
         int var5 = witnessInfos.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            VSANWitnessHostInfo witnessInfo = var4[var6];
            ManagedObjectReference witnessRef = new ManagedObjectReference(witnessInfo.host.getType(), witnessInfo.host.getValue(), clusterRef.getServerGuid());
            Map allProperties = QueryUtil.getProperties(witnessRef, new String[]{"name", "primaryIconId", "runtime.inMaintenanceMode"}).getMap();
            Map properties = allProperties.containsKey(witnessRef) ? (Map)allProperties.get(witnessRef) : Collections.emptyMap();
            WitnessHostData witnessHostData = new WitnessHostData();
            witnessHostData.preferredFaultDomainName = witnessInfo.preferredFdName;
            witnessHostData.witnessHost = witnessRef;
            witnessHostData.isMetadataWitnessHost = witnessInfo.metadataMode != null && witnessInfo.metadataMode;
            witnessHostData.isOutOfInventory = !(properties.get("name") instanceof String);
            witnessHostData.witnessHostName = witnessHostData.isOutOfInventory ? Utils.getLocalizedString("vsan.faultDomains.witnessOutOfInventory") : (String)properties.get("name");
            witnessHostData.witnessHostIcon = properties.get("primaryIconId") instanceof String && !witnessHostData.isOutOfInventory ? (String)properties.get("primaryIconId") : "vx-icon-witnesshostdisconnected";
            witnessHostData.inMaintenanceMode = BooleanUtils.isTrue((Boolean)properties.get("runtime.inMaintenanceMode"));
            result.add(witnessHostData);
         }

         return (WitnessHostData[])result.toArray(new WitnessHostData[0]);
      } else {
         return null;
      }
   }

   @TsService
   public boolean hasSharedWitnessHost(ManagedObjectReference clusterRef) {
      if (!VsanCapabilityUtils.isSharedWitnessSupported(clusterRef)) {
         return false;
      } else {
         WitnessHostData[] hostsData = null;

         try {
            hostsData = this.getWitnessHostData(clusterRef);
         } catch (Exception var5) {
            throw new VsanUiLocalizableException(var5);
         }

         if (ArrayUtils.isEmpty(hostsData)) {
            return false;
         } else {
            Optional witnessHostData = Arrays.stream(hostsData).filter((hostData) -> {
               return !hostData.isMetadataWitnessHost;
            }).findFirst();
            if (!witnessHostData.isPresent()) {
               return false;
            } else {
               int clustersCount = this.getWitnessHostClustersCount(((WitnessHostData)witnessHostData.get()).witnessHost);
               return clustersCount > 1;
            }
         }
      }
   }

   private int getWitnessHostClustersCount(ManagedObjectReference hostRef) {
      try {
         Measure measure = new Measure("getWitnessHostClustersInfo");
         Throwable var3 = null;

         int var5;
         try {
            Validate.notNull(hostRef);
            List clusterRuntimeInfos = (List)this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure).loadAssignedSharedWitnessClusters(hostRef).getAssignedSharedWitnessClusters().get(hostRef);
            var5 = CollectionUtils.size(clusterRuntimeInfos);
         } catch (Throwable var15) {
            var3 = var15;
            throw var15;
         } finally {
            if (measure != null) {
               if (var3 != null) {
                  try {
                     measure.close();
                  } catch (Throwable var14) {
                     var3.addSuppressed(var14);
                  }
               } else {
                  measure.close();
               }
            }

         }

         return var5;
      } catch (Exception var17) {
         throw new VsanUiLocalizableException(var17);
      }
   }

   @TsService
   public List getAvailableDomains(ManagedObjectReference clusterRef) throws Exception {
      DataServiceResponse response = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "allVsanHosts", ClusterComputeResource.class.getSimpleName(), DOMAIN_PROPERTIES);
      List result = new ArrayList();
      Set dsResourceObjects = response.getResourceObjects();
      ManagedObjectReference[] hostRefs = (ManagedObjectReference[])dsResourceObjects.toArray(new ManagedObjectReference[0]);
      if (ArrayUtils.isEmpty(hostRefs)) {
         return result;
      } else {
         DataServiceResponse iconProperties = QueryUtil.getProperties(hostRefs, new String[]{"primaryIconId"});
         String preferredFaultDomainName = null;
         Map map = new HashMap();
         Iterator var9 = dsResourceObjects.iterator();

         while(var9.hasNext()) {
            Object hostRef = var9.next();
            String name = (String)response.getProperty(hostRef, "name");
            boolean isWitnessHost = Boolean.valueOf(response.getProperty(hostRef, "isWitnessHost") + "");
            String domainName = (String)response.getProperty(hostRef, "config.vsanHostConfig.faultDomainInfo.name");
            String iconId = (String)iconProperties.getProperty(hostRef, "primaryIconId");
            String hostUid = this.objectReferenceService.getUid(hostRef);
            boolean maintenanceMode = BooleanUtils.isTrue((Boolean)response.getProperty(hostRef, "runtime.inMaintenanceMode"));
            ConnectionState connectionState = (ConnectionState)response.getProperty(hostRef, "runtime.connectionState");
            String hostPreferredFaultDomainName = (String)response.getProperty(hostRef, "preferredFaultDomain");
            if (isWitnessHost) {
               preferredFaultDomainName = hostPreferredFaultDomainName;
            } else {
               if (domainName != null && domainName.length() == 0) {
                  domainName = null;
               }

               if (domainName != null) {
                  DomainOrHostData hostData = DomainOrHostData.createHostData(hostUid, name, iconId, maintenanceMode, com.vmware.vsan.client.services.common.data.ConnectionState.fromHostState(connectionState));
                  List addTo = (List)map.get(domainName);
                  if (addTo == null) {
                     addTo = new ArrayList();
                     map.put(domainName, addTo);
                  }

                  ((List)addTo).add(hostData);
               } else {
                  result.add(DomainOrHostData.createHostData(hostUid, name, iconId, maintenanceMode, com.vmware.vsan.client.services.common.data.ConnectionState.fromHostState(connectionState)));
               }
            }
         }

         var9 = map.keySet().iterator();

         while(var9.hasNext()) {
            String domainName = (String)var9.next();
            result.add(DomainOrHostData.createDomainData(domainName, domainName, domainName.equals(preferredFaultDomainName), (List)map.get(domainName)));
         }

         return result;
      }
   }

   public VsanHostsResult collectVsanHosts(ManagedObjectReference clusterRef, Measure measure) {
      VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadWitnessHosts();

      try {
         Measure hostsMeasure = measure.start("Query data service cluster's host properties.");
         Throwable var5 = null;

         VsanHostsResult var8;
         try {
            PropertyValue[] hostProps = this.dataServiceHelper.getHostsWithConnectionState(clusterRef).getPropertyValues();
            VSANWitnessHostInfo[] witnessHostInfos = dataRetriever.getWitnessHosts();
            var8 = new VsanHostsResult(hostProps, witnessHostInfos);
         } catch (Throwable var18) {
            var5 = var18;
            throw var18;
         } finally {
            if (hostsMeasure != null) {
               if (var5 != null) {
                  try {
                     hostsMeasure.close();
                  } catch (Throwable var17) {
                     var5.addSuppressed(var17);
                  }
               } else {
                  hostsMeasure.close();
               }
            }

         }

         return var8;
      } catch (Exception var20) {
         logger.error("Unable to query cluster's host properties");
         throw new VsanUiLocalizableException(var20);
      }
   }

   @TsService
   public boolean hasDiskGroups(ManagedObjectReference clusterRef, ManagedObjectReference witnessHost) {
      try {
         VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(new NoOpMeasure(), clusterRef).loadDiskMappings(Arrays.asList(witnessHost));
         DiskMapInfoEx[] diskGroups = (DiskMapInfoEx[])dataRetriever.getDiskMappings().get(witnessHost);
         return !ArrayUtils.isEmpty(diskGroups);
      } catch (Exception var5) {
         logger.warn("Failed to check disk groups for host: " + witnessHost, var5);
         return false;
      }
   }

   @TsService
   public boolean getIsVsanStretchedCluster(ManagedObjectReference clusterRef) {
      List witnessHosts = this.getWitnessHosts(clusterRef);
      return CollectionUtils.isNotEmpty(witnessHosts);
   }
}
