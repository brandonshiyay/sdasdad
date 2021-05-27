package com.vmware.vsphere.client.vsan.dataprovider.vum;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.host.MaintenanceSpec;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHclInfo;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthQuerySpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthSummary;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vim.vsan.binding.vim.host.VsanHclFirmwareUpdateSpec;
import com.vmware.vim.vsan.binding.vim.host.VsanUpdateManager;
import com.vmware.vim.vsan.binding.vim.vsan.VsanDownloadItem;
import com.vmware.vim.vsan.binding.vim.vsan.VsanHealthPerspective;
import com.vmware.vim.vsan.binding.vim.vsan.VsanUpdateItem;
import com.vmware.vim.vsan.binding.vim.vsan.VsanVibScanResult;
import com.vmware.vim.vsan.binding.vim.vsan.VsanVibSpec;
import com.vmware.vise.data.ParameterSpec;
import com.vmware.vise.data.PropertySpec;
import com.vmware.vise.data.query.DataServiceExtensionRegistry;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vise.data.query.TypeInfo;
import com.vmware.vsan.client.services.ProxygenSerializer;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.common.VsanBasePropertyProviderAdapter;
import com.vmware.vsan.client.services.vum.VumBaselineRecommendationService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.data.VumBaselineRecommendationType;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VumPropertyProviderAdapter extends VsanBasePropertyProviderAdapter {
   private static final Logger logger = LoggerFactory.getLogger(VumPropertyProviderAdapter.class);
   private static final VsanProfiler _profiler = new VsanProfiler(VumPropertyProviderAdapter.class);
   private static final String UPDATES = "updates";
   private static final String AVAILABLE = "vumVsanIntegrationAvailable";
   private static final String IMPORT = "vumVsanImportFirmware";
   private static final String VENDOR_INSTALL = "vumVsanInstallVendorTool";
   private static final String VENDOR_INSTALL_FROM_CHECKSUM = "vumVsanInstallVendorToolFromChecksum";
   private static final String VENDOR_DOWNLOAD_AND_INSTALL = "vumVsanDownloadInstallVendorTool";
   private static final String BASELINE_RECOMMENDATION = "baselineRecommendation";
   private static final String BASELINE_RECOMMENDATION_AVAILABLE = "vsanEnabledAndBaselineRecommendationAvailable";
   private static final String TOOL = "tool";
   @Autowired
   private VumBaselineRecommendationService baselineRecommendationService;
   @Autowired
   private VsanClient vsanClient;

   public VumPropertyProviderAdapter(DataServiceExtensionRegistry registry) {
      TypeInfo clusterType = new TypeInfo();
      clusterType.type = ClusterComputeResource.class.getSimpleName();
      clusterType.properties = new String[]{"updates", "vumVsanImportFirmware", "vumVsanIntegrationAvailable", "vumVsanInstallVendorTool", "vumVsanInstallVendorToolFromChecksum", "vumVsanDownloadInstallVendorTool", "baselineRecommendation", "vsanEnabledAndBaselineRecommendationAvailable"};
      registry.registerDataAdapter(this, new TypeInfo[]{clusterType});
   }

   protected ResultSet getResult(PropertyRequestSpec propertyRequest) {
      ResultSet result = new ResultSet();
      ManagedObjectReference[] targetObjects = (ManagedObjectReference[])Arrays.copyOf(propertyRequest.objects, propertyRequest.objects.length, ManagedObjectReference[].class);
      PropertySpec[] var4 = propertyRequest.properties;
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         PropertySpec propertySpec = var4[var6];
         String[] var8 = propertySpec.propertyNames;
         int var9 = var8.length;

         for(int var10 = 0; var10 < var9; ++var10) {
            String propertyName = var8[var10];

            try {
               byte var13 = -1;
               switch(propertyName.hashCode()) {
               case -1526316595:
                  if (propertyName.equals("vumVsanIntegrationAvailable")) {
                     var13 = 0;
                  }
                  break;
               case -1206112381:
                  if (propertyName.equals("vumVsanInstallVendorTool")) {
                     var13 = 3;
                  }
                  break;
               case -234430262:
                  if (propertyName.equals("updates")) {
                     var13 = 1;
                  }
                  break;
               case 79019995:
                  if (propertyName.equals("vumVsanDownloadInstallVendorTool")) {
                     var13 = 5;
                  }
                  break;
               case 115089611:
                  if (propertyName.equals("vsanEnabledAndBaselineRecommendationAvailable")) {
                     var13 = 6;
                  }
                  break;
               case 1413649656:
                  if (propertyName.equals("vumVsanImportFirmware")) {
                     var13 = 2;
                  }
                  break;
               case 1580590384:
                  if (propertyName.equals("vumVsanInstallVendorToolFromChecksum")) {
                     var13 = 4;
                  }
                  break;
               case 1635718494:
                  if (propertyName.equals("baselineRecommendation")) {
                     var13 = 7;
                  }
               }

               switch(var13) {
               case 0:
                  ArrayList items = new ArrayList();
                  ManagedObjectReference[] var15 = targetObjects;
                  int var16 = targetObjects.length;

                  for(int var17 = 0; var17 < var16; ++var17) {
                     ManagedObjectReference clusterRef = var15[var17];
                     boolean available = VsanCapabilityUtils.isVsanVumIntegrationSupported(clusterRef);
                     items.add(QueryUtil.createResultItem("vumVsanIntegrationAvailable", available, clusterRef));
                  }

                  result.items = (ResultItem[])items.toArray(new ResultItem[targetObjects.length]);
                  break;
               case 1:
                  result.items = this.getUpdates(targetObjects);
                  break;
               case 2:
                  result.items = this.importFirmware(targetObjects, propertySpec.parameters);
                  break;
               case 3:
                  result.items = this.installVib(targetObjects, propertySpec.parameters);
                  break;
               case 4:
                  result.items = this.installVibFromChecksum(targetObjects, propertySpec.parameters);
                  break;
               case 5:
                  result.items = this.downloadAndInstallTools(targetObjects);
                  break;
               case 6:
                  result.items = (ResultItem[])this.getBaselineRecommendationAvailable(targetObjects).toArray(new ResultItem[targetObjects.length]);
                  break;
               case 7:
                  result.items = (ResultItem[])this.getVumBaselineRecommendation(targetObjects).toArray(new ResultItem[targetObjects.length]);
                  break;
               default:
                  throw new UnsupportedOperationException();
               }
            } catch (Exception var20) {
               result.error = var20;
            }
         }
      }

      return result;
   }

   private ArrayList getVumBaselineRecommendation(ManagedObjectReference[] targetObjects) {
      ArrayList items = new ArrayList();
      ManagedObjectReference[] var3 = targetObjects;
      int var4 = targetObjects.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         ManagedObjectReference clusterRef = var3[var5];
         VumBaselineRecommendationType recommendation = this.baselineRecommendationService.getClusterVumBaselineRecommendation(clusterRef);
         items.add(QueryUtil.createResultItem("baselineRecommendation", this.getBaselineRecommendationTxt(recommendation), clusterRef));
      }

      return items;
   }

   private String getBaselineRecommendationTxt(VumBaselineRecommendationType recommendation) {
      switch(recommendation) {
      case latestRelease:
         return Utils.getLocalizedString("vsan.vum.baseline.recommendation.latestRelease");
      case latestPatch:
         return Utils.getLocalizedString("vsan.vum.baseline.recommendation.latestPatch");
      case noRecommendation:
         return Utils.getLocalizedString("vsan.vum.baseline.recommendation.noRecommendation");
      default:
         logger.error("Not supported vum baseline recommendation found: " + recommendation);
         return recommendation.toString();
      }
   }

   private ArrayList getBaselineRecommendationAvailable(ManagedObjectReference[] targetObjects) throws Exception {
      ArrayList items = new ArrayList();
      DataServiceResponse response = QueryUtil.getProperties(targetObjects, new String[]{"configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.enabled"});
      ManagedObjectReference[] var4 = targetObjects;
      int var5 = targetObjects.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ManagedObjectReference clusterRef = var4[var6];
         Object propertyValue = ((Map)response.getMap().get(clusterRef)).get("configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.enabled");
         boolean isVsanEnabled = propertyValue == null ? false : (Boolean)propertyValue;
         boolean available = isVsanEnabled && VsanCapabilityUtils.isVumBaselineRecommendationSupportedOnVc(clusterRef);
         items.add(QueryUtil.createResultItem("vsanEnabledAndBaselineRecommendationAvailable", available, clusterRef));
      }

      return items;
   }

   private ResultItem[] importFirmware(ManagedObjectReference[] refs, ParameterSpec[] parameterSpecs) throws Exception {
      if (parameterSpecs != null && parameterSpecs.length != 0) {
         List checksums = null;
         ParameterSpec[] var4 = parameterSpecs;
         int var5 = parameterSpecs.length;

         int var6;
         for(var6 = 0; var6 < var5; ++var6) {
            ParameterSpec spec = var4[var6];
            if (spec.propertyName.equals("vumVsanImportFirmware")) {
               checksums = (List)spec.parameter;
               break;
            }
         }

         if (checksums == null) {
            logger.warn("Unable to find supplied checksums for the update importFirmware.");
            return new ResultItem[0];
         } else {
            ArrayList result = new ArrayList();
            ManagedObjectReference[] var42 = refs;
            var6 = refs.length;

            for(int var43 = 0; var43 < var6; ++var43) {
               ManagedObjectReference clusterRef = var42[var43];
               VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
               Throwable var10 = null;

               try {
                  VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
                  VsanProfiler.Point p = _profiler.point("healthSystem.downloadHclFile");
                  Throwable var14 = null;

                  ManagedObjectReference taskRef;
                  try {
                     taskRef = healthSystem.downloadHclFile((String[])checksums.toArray(new String[checksums.size()]));
                  } catch (Throwable var37) {
                     var14 = var37;
                     throw var37;
                  } finally {
                     if (p != null) {
                        if (var14 != null) {
                           try {
                              p.close();
                           } catch (Throwable var36) {
                              var14.addSuppressed(var36);
                           }
                        } else {
                           p.close();
                        }
                     }

                  }

                  if (taskRef != null) {
                     taskRef.setServerGuid(clusterRef.getServerGuid());
                  }

                  result.add(QueryUtil.createResultItem("vumVsanImportFirmware", taskRef, clusterRef));
               } catch (Throwable var39) {
                  var10 = var39;
                  throw var39;
               } finally {
                  if (conn != null) {
                     if (var10 != null) {
                        try {
                           conn.close();
                        } catch (Throwable var35) {
                           var10.addSuppressed(var35);
                        }
                     } else {
                        conn.close();
                     }
                  }

               }
            }

            return (ResultItem[])result.toArray(new ResultItem[refs.length]);
         }
      } else {
         logger.warn("Missing importFirmware parameter spec.");
         return new ResultItem[0];
      }
   }

   private ResultItem[] installVibFromChecksum(ManagedObjectReference[] refs, ParameterSpec[] parameterSpecs) throws Exception {
      if (parameterSpecs != null && parameterSpecs.length != 0) {
         List vibSpecs = new ArrayList();
         ParameterSpec[] var4 = parameterSpecs;
         int var5 = parameterSpecs.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            ParameterSpec spec = var4[var6];
            if (spec.propertyName.equals("vumVsanInstallVendorToolFromChecksum")) {
               String checksum = (String)spec.parameter;
               vibSpecs = this.getVibSpecs(refs, checksum);
               break;
            }
         }

         if (((List)vibSpecs).size() == 0) {
            return new ResultItem[0];
         } else {
            ArrayList result = new ArrayList();
            VsanProfiler.Point p = _profiler.point("updateManager.vsanVibInstall");
            Throwable var42 = null;

            try {
               ManagedObjectReference[] var43 = refs;
               int var44 = refs.length;

               for(int var9 = 0; var9 < var44; ++var9) {
                  ManagedObjectReference clusterRef = var43[var9];
                  VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
                  Throwable var12 = null;

                  try {
                     VsanUpdateManager updateManager = conn.getUpdateManager();
                     ManagedObjectReference updateTask = updateManager.vsanVibInstall(clusterRef, (VsanVibSpec[])((List)vibSpecs).toArray(new VsanVibSpec[0]), (VsanVibScanResult[])null, (VsanHclFirmwareUpdateSpec[])null, (MaintenanceSpec)null, false, false);
                     updateTask.setServerGuid(clusterRef.getServerGuid());
                     result.add(QueryUtil.createResultItem("vumVsanInstallVendorToolFromChecksum", updateTask, clusterRef));
                  } catch (Throwable var36) {
                     var12 = var36;
                     throw var36;
                  } finally {
                     if (conn != null) {
                        if (var12 != null) {
                           try {
                              conn.close();
                           } catch (Throwable var35) {
                              var12.addSuppressed(var35);
                           }
                        } else {
                           conn.close();
                        }
                     }

                  }
               }
            } catch (Throwable var38) {
               var42 = var38;
               throw var38;
            } finally {
               if (p != null) {
                  if (var42 != null) {
                     try {
                        p.close();
                     } catch (Throwable var34) {
                        var42.addSuppressed(var34);
                     }
                  } else {
                     p.close();
                  }
               }

            }

            return (ResultItem[])result.toArray(new ResultItem[refs.length]);
         }
      } else {
         logger.warn("Missing checksum parameter spec.");
         return new ResultItem[0];
      }
   }

   private ResultItem[] installVib(ManagedObjectReference[] refs, ParameterSpec[] parameterSpecs) throws Exception {
      if (parameterSpecs != null && parameterSpecs.length != 0) {
         Map vendorVibSpec = null;
         ParameterSpec[] var4 = parameterSpecs;
         int var5 = parameterSpecs.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            ParameterSpec spec = var4[var6];
            if (spec.propertyName.equals("vumVsanInstallVendorTool")) {
               vendorVibSpec = (Map)spec.parameter;
               break;
            }
         }

         if (vendorVibSpec == null) {
            logger.warn("Unable to find supplied vendor vib specs.");
            return new ResultItem[0];
         } else {
            ArrayList result = new ArrayList();
            VsanProfiler.Point p = _profiler.point("updateManager.vsanVibInstall");
            Throwable var42 = null;

            try {
               ManagedObjectReference[] var43 = refs;
               int var8 = refs.length;

               for(int var9 = 0; var9 < var8; ++var9) {
                  ManagedObjectReference clusterRef = var43[var9];
                  VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
                  Throwable var12 = null;

                  try {
                     VsanUpdateManager updateManager = conn.getUpdateManager();
                     ManagedObjectReference updateTask = updateManager.vsanVibInstall(clusterRef, new VsanVibSpec[]{this.toVibSpec(vendorVibSpec)}, (VsanVibScanResult[])null, (VsanHclFirmwareUpdateSpec[])null, (MaintenanceSpec)null, false, false);
                     updateTask.setServerGuid(clusterRef.getServerGuid());
                     result.add(QueryUtil.createResultItem("vumVsanInstallVendorTool", updateTask, clusterRef));
                  } catch (Throwable var36) {
                     var12 = var36;
                     throw var36;
                  } finally {
                     if (conn != null) {
                        if (var12 != null) {
                           try {
                              conn.close();
                           } catch (Throwable var35) {
                              var12.addSuppressed(var35);
                           }
                        } else {
                           conn.close();
                        }
                     }

                  }
               }
            } catch (Throwable var38) {
               var42 = var38;
               throw var38;
            } finally {
               if (p != null) {
                  if (var42 != null) {
                     try {
                        p.close();
                     } catch (Throwable var34) {
                        var42.addSuppressed(var34);
                     }
                  } else {
                     p.close();
                  }
               }

            }

            return (ResultItem[])result.toArray(new ResultItem[refs.length]);
         }
      } else {
         logger.warn("Missing vib spec parameter spec.");
         return new ResultItem[0];
      }
   }

   private List getVibSpecs(ManagedObjectReference[] refs, String checksum) throws Exception {
      List result = new ArrayList();
      ManagedObjectReference[] var4 = refs;
      int var5 = refs.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ManagedObjectReference clusterRef = var4[var6];
         VsanClusterHclInfo hclInfo = this.getHclInfo(clusterRef);
         if (hclInfo != null && hclInfo.updateItems != null) {
            VsanUpdateItem[] var9 = hclInfo.updateItems;
            int var10 = var9.length;

            label55:
            for(int var11 = 0; var11 < var10; ++var11) {
               VsanUpdateItem updateItem = var9[var11];
               if (updateItem.vibType != null && updateItem.vibType.equals("tool") && updateItem.downloadInfo != null) {
                  VsanDownloadItem[] var13 = updateItem.downloadInfo;
                  int var14 = var13.length;

                  for(int var15 = 0; var15 < var14; ++var15) {
                     VsanDownloadItem downloadItem = var13[var15];
                     if (downloadItem.sha1sum.equals(checksum) && updateItem.vibSpec != null) {
                        VsanVibSpec[] var17 = updateItem.vibSpec;
                        int var18 = var17.length;
                        int var19 = 0;

                        while(true) {
                           if (var19 >= var18) {
                              continue label55;
                           }

                           VsanVibSpec vibSpec = var17[var19];
                           result.add(vibSpec);
                           ++var19;
                        }
                     }
                  }
               }
            }
         }
      }

      return result;
   }

   private ResultItem[] downloadAndInstallTools(ManagedObjectReference[] refs) throws Exception {
      ArrayList result = new ArrayList();
      VsanProfiler.Point p = _profiler.point("updateManager.downloadAndInstall");
      Throwable var4 = null;

      try {
         ManagedObjectReference[] var5 = refs;
         int var6 = refs.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            ManagedObjectReference clusterRef = var5[var7];
            VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
            Throwable var10 = null;

            try {
               VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
               ManagedObjectReference updateTask = healthSystem.downloadAndInstallVendorTool(clusterRef);
               updateTask.setServerGuid(clusterRef.getServerGuid());
               result.add(QueryUtil.createResultItem("vumVsanDownloadInstallVendorTool", updateTask, clusterRef));
            } catch (Throwable var34) {
               var10 = var34;
               throw var34;
            } finally {
               if (conn != null) {
                  if (var10 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var33) {
                        var10.addSuppressed(var33);
                     }
                  } else {
                     conn.close();
                  }
               }

            }
         }
      } catch (Throwable var36) {
         var4 = var36;
         throw var36;
      } finally {
         if (p != null) {
            if (var4 != null) {
               try {
                  p.close();
               } catch (Throwable var32) {
                  var4.addSuppressed(var32);
               }
            } else {
               p.close();
            }
         }

      }

      return (ResultItem[])result.toArray(new ResultItem[refs.length]);
   }

   private ResultItem[] getUpdates(ManagedObjectReference[] refs) throws Exception {
      ProxygenSerializer serializer = new ProxygenSerializer();
      ArrayList result = new ArrayList();
      ManagedObjectReference[] var4 = refs;
      int var5 = refs.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ManagedObjectReference clusterRef = var4[var6];
         VsanClusterHclInfo hclInfo = this.getHclInfo(clusterRef);
         Map data = (Map)serializer.serialize(hclInfo);
         result.add(QueryUtil.createResultItem("updates", data, clusterRef));
      }

      return (ResultItem[])result.toArray(new ResultItem[refs.length]);
   }

   private VsanClusterHclInfo getHclInfo(ManagedObjectReference clusterRef) throws Exception {
      String perspective = VsanHealthPerspective.vsanUpgradePreCheck.toString();
      String[] properties = new String[]{"hclInfo"};
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      VsanClusterHclInfo var10;
      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();

         try {
            VsanProfiler.Point p = _profiler.point("healthSystem.queryClusterHealthSummary");
            Throwable var8 = null;

            try {
               VsanClusterHealthSummary healthSummary = healthSystem.queryClusterHealthSummary(clusterRef, (Integer)null, (String[])null, false, properties, false, perspective, (ManagedObjectReference[])null, (VsanClusterHealthQuerySpec)null, (Boolean)null);
               var10 = healthSummary.hclInfo;
            } catch (Throwable var35) {
               var8 = var35;
               throw var35;
            } finally {
               if (p != null) {
                  if (var8 != null) {
                     try {
                        p.close();
                     } catch (Throwable var34) {
                        var8.addSuppressed(var34);
                     }
                  } else {
                     p.close();
                  }
               }

            }
         } catch (Exception var37) {
            logger.error("Could not retrieve update items for the cluster", var37);
            throw var37;
         }
      } catch (Throwable var38) {
         var5 = var38;
         throw var38;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var33) {
                  var5.addSuppressed(var33);
               }
            } else {
               conn.close();
            }
         }

      }

      return var10;
   }

   private VsanVibSpec toVibSpec(Map specMap) {
      VsanVibSpec spec = new VsanVibSpec();
      Map morefMap = (Map)specMap.get("host");
      spec.host = new ManagedObjectReference((String)morefMap.get("type"), (String)morefMap.get("value"), (String)morefMap.get("serverGuid"));
      spec.metaUrl = (String)specMap.get("metaUrl");
      spec.metaSha1Sum = (String)specMap.get("metaSha1Sum");
      spec.vibUrl = (String)specMap.get("vibUrl");
      spec.vibSha1Sum = (String)specMap.get("vibSha1Sum");
      return spec;
   }
}
