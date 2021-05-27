package com.vmware.vsan.client.services.proactivetests;

import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterCreateVmHealthTestResult;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthResultBase;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthResultRow;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterHealthResultTable;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterNetworkLoadTestResult;
import com.vmware.vim.vsan.binding.vim.cluster.VsanClusterNetworkPerfTaskSpec;
import com.vmware.vim.vsan.binding.vim.cluster.VsanVcClusterHealthSystem;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.csd.CsdService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.health.ProactiveTestData;
import com.vmware.vsphere.client.vsan.health.VsanTestData;
import com.vmware.vsphere.client.vsan.health.util.VsanHealthUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProactiveTestsService {
   private static final Log _logger = LogFactory.getLog(ProactiveTestsService.class);
   private static final VsanProfiler _profiler = new VsanProfiler(ProactiveTestsService.class);
   private static final String UNICASTPERFTEST_HELPID = "com.vmware.vsan.health.test.unicastperftest";
   private static final String MOR_PATTERN = "^(mor:).*$";
   @Autowired
   private VsanClient vsanClient;
   @Autowired
   private CsdService csdService;

   @TsService
   public List getProactiveTestResults(ManagedObjectReference clusterRef, ManagedObjectReference datastoreRef, ManagedObjectReference serverClusterRef) {
      List results = new ArrayList();
      ProactiveTestData vmCreationTestResult = this.getLastVmCreationTestResult(clusterRef, datastoreRef);
      if (vmCreationTestResult != null) {
         results.add(vmCreationTestResult);
      }

      ProactiveTestData networkTestResult = null;
      if (VsanCapabilityUtils.isNetworkPerfTestSupportedOnCluster(clusterRef)) {
         networkTestResult = this.getLastNetworkTestResult(clusterRef, serverClusterRef);
      }

      if (networkTestResult != null) {
         results.add(networkTestResult);
      }

      return results;
   }

   @TsService
   public ProactiveTestData getLastVmCreationTestResult(ManagedObjectReference clusterRef, ManagedObjectReference datastoreRef) {
      VsanClusterCreateVmHealthTestResult[] vmCreationTestResults = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();

         try {
            VsanProfiler.Point point = _profiler.point("healthSystem.queryClusterCreateVmHealthHistoryTest");
            Throwable var8 = null;

            try {
               vmCreationTestResults = healthSystem.queryClusterCreateVmHealthHistoryTest(clusterRef, 1, datastoreRef);
            } catch (Throwable var33) {
               var8 = var33;
               throw var33;
            } finally {
               if (point != null) {
                  if (var8 != null) {
                     try {
                        point.close();
                     } catch (Throwable var32) {
                        var8.addSuppressed(var32);
                     }
                  } else {
                     point.close();
                  }
               }

            }
         } catch (Exception var35) {
            _logger.error("Unable to get VM creation test history results.", var35);
            throw new VsanUiLocalizableException("vsan.proactive.tests.vmcreation.history.error");
         }
      } catch (Throwable var36) {
         var5 = var36;
         throw var36;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var31) {
                  var5.addSuppressed(var31);
               }
            } else {
               conn.close();
            }
         }

      }

      ProactiveTestData result;
      if (ArrayUtils.isEmpty(vmCreationTestResults)) {
         result = this.createEmptyVMCreationTestResult();
      } else {
         this.filterMissingHostHealthTests(vmCreationTestResults[0]);
         result = this.createVMCreationTestResult(clusterRef, vmCreationTestResults[0]);
      }

      return result;
   }

   @TsService
   public ProactiveTestData getLastNetworkTestResult(ManagedObjectReference clusterRef, ManagedObjectReference serverClusterRef) {
      VsanClusterNetworkPerfTaskSpec networkPerfTaskSpec = null;
      if (this.csdService.isCsdSupported(clusterRef)) {
         networkPerfTaskSpec = new VsanClusterNetworkPerfTaskSpec();
         networkPerfTaskSpec.setCluster(serverClusterRef);
      }

      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var6 = null;

      VsanClusterNetworkLoadTestResult[] networkLoadResults;
      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();

         try {
            VsanProfiler.Point point = _profiler.point("healthSystem.queryClusterNetworkPerfHistoryTest");
            Throwable var9 = null;

            try {
               networkLoadResults = healthSystem.queryClusterNetworkPerfHistoryTest(clusterRef, 1, networkPerfTaskSpec);
            } catch (Throwable var34) {
               var9 = var34;
               throw var34;
            } finally {
               if (point != null) {
                  if (var9 != null) {
                     try {
                        point.close();
                     } catch (Throwable var33) {
                        var9.addSuppressed(var33);
                     }
                  } else {
                     point.close();
                  }
               }

            }
         } catch (Exception var36) {
            _logger.error("Unable to get network test history results.", var36);
            throw new VsanUiLocalizableException("vsan.proactive.tests.network.history.error");
         }
      } catch (Throwable var37) {
         var6 = var37;
         throw var37;
      } finally {
         if (conn != null) {
            if (var6 != null) {
               try {
                  conn.close();
               } catch (Throwable var32) {
                  var6.addSuppressed(var32);
               }
            } else {
               conn.close();
            }
         }

      }

      ProactiveTestData result;
      if (!ArrayUtils.isEmpty(networkLoadResults) && "com.vmware.vsan.health.test.unicastperftest".equals(networkLoadResults[0].clusterResult.healthTest.testId)) {
         result = this.createNetworkLoadTestResult(clusterRef, networkLoadResults[0]);
      } else {
         result = this.createEmptyNetworkLoadTestResult();
      }

      return result;
   }

   @TsService
   public ProactiveTestData getVMCreationTestResult(ManagedObjectReference clusterRef, int timeout, ManagedObjectReference datastoreRef) {
      ProactiveTestData data = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var6 = null;

      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();

         try {
            VsanProfiler.Point point = _profiler.point("healthSystem.queryClusterCreateVmHealthTest");
            Throwable var9 = null;

            try {
               VsanClusterCreateVmHealthTestResult vmHealthTestResult = healthSystem.queryClusterCreateVmHealthTest(clusterRef, timeout, datastoreRef);
               if (vmHealthTestResult != null && vmHealthTestResult.clusterResult != null) {
                  data = this.createVMCreationTestResult(clusterRef, vmHealthTestResult);
               }
            } catch (Throwable var34) {
               var9 = var34;
               throw var34;
            } finally {
               if (point != null) {
                  if (var9 != null) {
                     try {
                        point.close();
                     } catch (Throwable var33) {
                        var9.addSuppressed(var33);
                     }
                  } else {
                     point.close();
                  }
               }

            }
         } catch (Exception var36) {
            _logger.error("Unable to get the VM creation test result.", var36);
            throw new VsanUiLocalizableException("vsan.proactive.tests.vmcreation.test.result.error");
         }
      } catch (Throwable var37) {
         var6 = var37;
         throw var37;
      } finally {
         if (conn != null) {
            if (var6 != null) {
               try {
                  conn.close();
               } catch (Throwable var32) {
                  var6.addSuppressed(var32);
               }
            } else {
               conn.close();
            }
         }

      }

      return data;
   }

   @TsService
   public ProactiveTestData getNetworkPerfTestResult(ManagedObjectReference clusterRef, boolean isMulticast) {
      ProactiveTestData data = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var5 = null;

      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();

         try {
            VsanProfiler.Point point = _profiler.point("healthSystem.queryClusterNetworkPerfTest");
            Throwable var8 = null;

            try {
               VsanClusterNetworkLoadTestResult networkLoadResults = healthSystem.queryClusterNetworkPerfTest(clusterRef, isMulticast, (Integer)null);
               if (networkLoadResults != null && networkLoadResults.clusterResult != null) {
                  data = this.createNetworkLoadTestResult(clusterRef, networkLoadResults);
               }
            } catch (Throwable var33) {
               var8 = var33;
               throw var33;
            } finally {
               if (point != null) {
                  if (var8 != null) {
                     try {
                        point.close();
                     } catch (Throwable var32) {
                        var8.addSuppressed(var32);
                     }
                  } else {
                     point.close();
                  }
               }

            }
         } catch (Exception var35) {
            _logger.error("Unable to get the network load test result.", var35);
            throw new VsanUiLocalizableException("vsan.proactive.tests.network.test.result.error");
         }
      } catch (Throwable var36) {
         var5 = var36;
         throw var36;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var31) {
                  var5.addSuppressed(var31);
               }
            } else {
               conn.close();
            }
         }

      }

      return data;
   }

   @TsService
   public ManagedObjectReference startNetworkPerfTestTask(ManagedObjectReference clusterRef, ManagedObjectReference serverClusterRef) {
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var4 = null;

      Object var9;
      try {
         VsanVcClusterHealthSystem healthSystem = conn.getVsanVcClusterHealthSystem();
         VsanClusterNetworkPerfTaskSpec networkPerfTaskSpec = new VsanClusterNetworkPerfTaskSpec();
         networkPerfTaskSpec.setCluster(serverClusterRef);

         try {
            VsanProfiler.Point point = _profiler.point("healthSystem.queryClusterNetworkPerfTask");
            Throwable var8 = null;

            try {
               var9 = VmodlHelper.assignServerGuid(healthSystem.queryClusterNetworkPerfTask(clusterRef, networkPerfTaskSpec), clusterRef.getServerGuid());
            } catch (Throwable var34) {
               var9 = var34;
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
            _logger.error("Unable to start network test task.", var36);
            throw new VsanUiLocalizableException("vsan.proactive.tests.network.test.result.error");
         }
      } catch (Throwable var37) {
         var4 = var37;
         throw var37;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var32) {
                  var4.addSuppressed(var32);
               }
            } else {
               conn.close();
            }
         }

      }

      return (ManagedObjectReference)var9;
   }

   private ProactiveTestData createVMCreationTestResult(ManagedObjectReference clusterRef, VsanClusterCreateVmHealthTestResult vmHealthTestResult) {
      Set moRefs = new HashSet();
      VsanHealthUtil.addToTestMoRefsFromBaseResults(vmHealthTestResult.clusterResult.healthTest.testDetails, moRefs, clusterRef.getServerGuid());
      ProactiveTestData data = new ProactiveTestData();
      data.generalData = new VsanTestData(vmHealthTestResult.clusterResult.healthTest, vmHealthTestResult.clusterResult.timestamp, VsanHealthUtil.getNamesForMoRefs(moRefs), true, false);
      data.timestamp = vmHealthTestResult.clusterResult.timestamp.getTimeInMillis();
      data.perfTestType = ProactiveTestData.PerfTestType.vmCreation;
      return data;
   }

   private ProactiveTestData createEmptyVMCreationTestResult() {
      ProactiveTestData result = new ProactiveTestData();
      result.generalData = new VsanTestData();
      result.perfTestType = ProactiveTestData.PerfTestType.vmCreation;
      return result;
   }

   private ProactiveTestData createEmptyNetworkLoadTestResult() {
      ProactiveTestData result = new ProactiveTestData();
      result.generalData = new VsanTestData();
      result.perfTestType = ProactiveTestData.PerfTestType.unicast;
      return result;
   }

   private ProactiveTestData createNetworkLoadTestResult(ManagedObjectReference clusterRef, VsanClusterNetworkLoadTestResult networkLoadTestResult) {
      Set moRefs = new HashSet();
      VsanHealthUtil.addToTestMoRefsFromBaseResults(networkLoadTestResult.clusterResult.healthTest.testDetails, moRefs, clusterRef.getServerGuid());
      ProactiveTestData data = new ProactiveTestData();
      data.generalData = new VsanTestData(networkLoadTestResult.clusterResult.healthTest, networkLoadTestResult.clusterResult.timestamp, VsanHealthUtil.getNamesForMoRefs(moRefs), true, false);
      data.timestamp = networkLoadTestResult.clusterResult.timestamp.getTimeInMillis();
      data.perfTestType = ProactiveTestData.PerfTestType.unicast;
      return data;
   }

   private void filterMissingHostHealthTests(VsanClusterCreateVmHealthTestResult testResult) {
      VsanClusterHealthResultBase[] testDetails = testResult.clusterResult.healthTest.testDetails;
      if (!ArrayUtils.isEmpty(testDetails)) {
         VsanClusterHealthResultBase[] var3 = testDetails;
         int var4 = testDetails.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            VsanClusterHealthResultBase testDetail = var3[var5];
            if (testDetail instanceof VsanClusterHealthResultTable) {
               VsanClusterHealthResultTable table = (VsanClusterHealthResultTable)testDetail;
               if (!ArrayUtils.isEmpty(table.rows)) {
                  List availableRows = new ArrayList();
                  VsanClusterHealthResultRow[] var9 = table.rows;
                  int var10 = var9.length;

                  for(int var11 = 0; var11 < var10; ++var11) {
                     VsanClusterHealthResultRow row = var9[var11];
                     if (ArrayUtils.isNotEmpty(row.values) && StringUtils.isNotBlank(row.values[0])) {
                        boolean isHostInCluster = row.values[0].matches("^(mor:).*$");
                        if (isHostInCluster) {
                           availableRows.add(row);
                        }
                     }
                  }

                  table.rows = (VsanClusterHealthResultRow[])availableRows.toArray(new VsanClusterHealthResultRow[0]);
               }
            }
         }

      }
   }
}
