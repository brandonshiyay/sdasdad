package com.vmware.vsan.client.services.network;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VSANWitnessHostInfo;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHclInfo;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterConfigSystem;
import com.vmware.vim.vsan.binding.vim.host.VsanHclNicInfo;
import com.vmware.vim.vsan.binding.vim.host.VsanHostHclInfo;
import com.vmware.vim.vsan.binding.vim.vsan.ConfigInfoEx;
import com.vmware.vim.vsan.binding.vim.vsan.RdmaConfig;
import com.vmware.vim.vsan.binding.vim.vsan.ReconfigSpec;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityProvider;
import com.vmware.vsan.client.services.inventory.InventoryNode;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.Measure;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsan.client.util.retriever.VsanAsyncDataRetriever;
import com.vmware.vsan.client.util.retriever.VsanDataRetrieverFactory;
import com.vmware.vsphere.client.vsan.stretched.VsanStretchedClusterService;
import com.vmware.vsphere.client.vsan.stretched.WitnessHostData;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VsanRdmaService {
   private static String RDMA_CAPABLE_PROTOCOL = "RoCEv2";
   @Autowired
   private VsanCapabilityProvider capabilityProvider;
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private VsanStretchedClusterService vsanStretchedClusterService;
   @Autowired
   private VsanDataRetrieverFactory dataRetrieverFactory;

   @TsService
   public ManagedObjectReference configureVsanRdma(ManagedObjectReference clusterRef, boolean rdmaEnabled) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      ManagedObjectReference var8;
      try {
         VsanVcClusterConfigSystem vsanConfigSystem = conn.getVsanConfigSystem();
         ReconfigSpec spec = new ReconfigSpec();
         spec.rdmaConfig = new RdmaConfig();
         spec.rdmaConfig.rdmaEnabled = rdmaEnabled;
         ManagedObjectReference taskRef = vsanConfigSystem.reconfigureEx(clusterRef, spec);
         var8 = VmodlHelper.assignServerGuid(taskRef, clusterRef.getServerGuid());
      } catch (Throwable var17) {
         var4 = var17;
         throw var17;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var16) {
                  var4.addSuppressed(var16);
               }
            } else {
               conn.close();
            }
         }

      }

      return var8;
   }

   @TsService
   public boolean isRdmaEnabled(ManagedObjectReference clusterRef) throws Exception {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var3 = null;

      boolean var6;
      try {
         VsanVcClusterConfigSystem vsanConfigSystem = conn.getVsanConfigSystem();
         ConfigInfoEx configInfoEx = vsanConfigSystem.getConfigInfoEx(clusterRef);
         if (configInfoEx.rdmaConfig != null) {
            var6 = configInfoEx.rdmaConfig.rdmaEnabled;
            return var6;
         }

         var6 = false;
      } catch (Throwable var16) {
         var3 = var16;
         throw var16;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var15) {
                  var3.addSuppressed(var15);
               }
            } else {
               conn.close();
            }
         }

      }

      return var6;
   }

   @TsService
   public RdmaData getRdmaData(ManagedObjectReference clusterRef) {
      if (!this.capabilityProvider.getVcCapabilityData(clusterRef).isRdmaSupported) {
         return null;
      } else {
         VSANWitnessHostInfo[] vsanWitnessHostInfos = null;
         VsanHostHclInfo[] hostsHclInfo = null;
         ConfigInfoEx configInfoEx = null;

         try {
            Measure measure = new Measure("Retrieving RDMA support information on hosts");
            Throwable var6 = null;

            WitnessHostData[] witnessHostsData;
            try {
               VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadClusterHclInfo().loadWitnessHosts().loadConfigInfoEx();
               VsanClusterHclInfo clusterHclInfo = dataRetriever.getClusterHclInfo();
               hostsHclInfo = clusterHclInfo.getHostResults();
               if (!ArrayUtils.isEmpty(hostsHclInfo)) {
                  vsanWitnessHostInfos = dataRetriever.getWitnessHosts();
                  configInfoEx = dataRetriever.getConfigInfoEx();
                  witnessHostsData = this.vsanStretchedClusterService.getWitnessHostDataByWitnessInfo(vsanWitnessHostInfos, clusterRef);
                  RdmaData rdmaData = new RdmaData();
                  rdmaData.isRdmaEnabled = configInfoEx.rdmaConfig != null && configInfoEx.rdmaConfig.rdmaEnabled;
                  rdmaData.unsupportedHosts = this.getUnsupportedHosts(clusterRef, hostsHclInfo, witnessHostsData);
                  RdmaData var11 = rdmaData;
                  return var11;
               }

               witnessHostsData = null;
            } catch (Throwable var22) {
               var6 = var22;
               throw var22;
            } finally {
               if (measure != null) {
                  if (var6 != null) {
                     try {
                        measure.close();
                     } catch (Throwable var21) {
                        var6.addSuppressed(var21);
                     }
                  } else {
                     measure.close();
                  }
               }

            }

            return witnessHostsData;
         } catch (Exception var24) {
            throw new VsanUiLocalizableException("vsan.services.rdma.fetch.error");
         }
      }
   }

   private List getUnsupportedHosts(ManagedObjectReference clusterRef, VsanHostHclInfo[] hostsHclInfo, WitnessHostData[] witnessHostsData) throws Exception {
      boolean isRdmaHardwareSupported = true;
      Set unsupportedHostsNames = new HashSet();
      VsanHostHclInfo[] var6 = hostsHclInfo;
      int var7 = hostsHclInfo.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         VsanHostHclInfo hostHclInfo = var6[var8];
         if (!this.isRdmaHardwareSupportedOnHost(hostHclInfo, witnessHostsData, (String[])null)) {
            isRdmaHardwareSupported = false;
            unsupportedHostsNames.add(hostHclInfo.hostname);
         }
      }

      if (isRdmaHardwareSupported) {
         return new ArrayList();
      } else {
         DataServiceResponse hostsPropertiesResponse = QueryUtil.getPropertiesForRelatedObjects(clusterRef, "host", HostSystem.class.getSimpleName(), new String[]{"name", "primaryIconId"});
         List unsupportedHosts = new ArrayList();
         Iterator var16 = unsupportedHostsNames.iterator();

         while(true) {
            while(var16.hasNext()) {
               String unsupportedHostName = (String)var16.next();
               Iterator var10 = hostsPropertiesResponse.getResourceObjects().iterator();

               while(var10.hasNext()) {
                  Object hostRef = var10.next();
                  String hostName = (String)hostsPropertiesResponse.getProperty(hostRef, "name");
                  if (unsupportedHostName.equals(hostName)) {
                     InventoryNode unsupportedHost = new InventoryNode((ManagedObjectReference)hostRef, hostName, (String)hostsPropertiesResponse.getProperty(hostRef, "primaryIconId"));
                     unsupportedHosts.add(unsupportedHost);
                     break;
                  }
               }
            }

            return unsupportedHosts;
         }
      }
   }

   @TsService
   public boolean isRdmaHardwareSupported(ManagedObjectReference clusterRef, String[] vSanHostAdapters) {
      if (!this.capabilityProvider.getVcCapabilityData(clusterRef).isRdmaSupported) {
         return false;
      } else {
         WitnessHostData[] witnessHostsData = null;
         VsanHostHclInfo[] hostsHclInfo = null;

         try {
            label146: {
               Measure measure = new Measure("Retrieving RDMA support information on hosts");
               Throwable var6 = null;

               boolean var9;
               try {
                  VsanAsyncDataRetriever dataRetriever = this.dataRetrieverFactory.createVsanAsyncDataRetriever(measure, clusterRef).loadClusterHclInfo().loadWitnessHosts();
                  VsanClusterHclInfo clusterHclInfo = dataRetriever.getClusterHclInfo();
                  hostsHclInfo = clusterHclInfo.getHostResults();
                  if (!ArrayUtils.isEmpty(hostsHclInfo)) {
                     VSANWitnessHostInfo[] vsanWitnessHostInfos = dataRetriever.getWitnessHosts();
                     witnessHostsData = this.vsanStretchedClusterService.getWitnessHostDataByWitnessInfo(vsanWitnessHostInfos, clusterRef);
                     break label146;
                  }

                  var9 = false;
               } catch (Throwable var20) {
                  var6 = var20;
                  throw var20;
               } finally {
                  if (measure != null) {
                     if (var6 != null) {
                        try {
                           measure.close();
                        } catch (Throwable var19) {
                           var6.addSuppressed(var19);
                        }
                     } else {
                        measure.close();
                     }
                  }

               }

               return var9;
            }
         } catch (Exception var22) {
            throw new VsanUiLocalizableException("vsan.services.rdma.fetch.error");
         }

         VsanHostHclInfo[] var23 = hostsHclInfo;
         int var24 = hostsHclInfo.length;

         for(int var25 = 0; var25 < var24; ++var25) {
            VsanHostHclInfo hostHclInfo = var23[var25];
            if (!this.isRdmaHardwareSupportedOnHost(hostHclInfo, witnessHostsData, vSanHostAdapters)) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean isRdmaHardwareSupportedOnHost(VsanHostHclInfo hostHclInfo, WitnessHostData[] witnessHostsData, String[] vSanHostAdapters) {
      if (ArrayUtils.isEmpty(hostHclInfo.pnics)) {
         return false;
      } else {
         int var6;
         if (ArrayUtils.isNotEmpty(witnessHostsData) && StringUtils.isNotEmpty(hostHclInfo.hostname)) {
            WitnessHostData[] var4 = witnessHostsData;
            int var5 = witnessHostsData.length;

            for(var6 = 0; var6 < var5; ++var6) {
               WitnessHostData witnessHostData = var4[var6];
               if (hostHclInfo.hostname.equals(witnessHostData.witnessHostName)) {
                  return true;
               }
            }
         }

         boolean noVsanAdaptersSpecified = ArrayUtils.isEmpty(vSanHostAdapters);
         VsanHclNicInfo[] var10 = hostHclInfo.pnics;
         var6 = var10.length;

         for(int var11 = 0; var11 < var6; ++var11) {
            VsanHclNicInfo nicInfo = var10[var11];
            if (nicInfo.rdmaConfig != null && nicInfo.rdmaConfig.rdmaCapable && RDMA_CAPABLE_PROTOCOL.equals(nicInfo.rdmaConfig.rdmaProtocolCapable) && (noVsanAdaptersSpecified || ArrayUtils.contains(vSanHostAdapters, nicInfo.deviceName))) {
               return true;
            }
         }

         return false;
      }
   }
}
