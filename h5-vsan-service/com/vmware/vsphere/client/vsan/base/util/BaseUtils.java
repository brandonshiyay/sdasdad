package com.vmware.vsphere.client.vsan.base.util;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Datastore;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vim.VirtualMachine;
import com.vmware.vim.binding.vim.Datastore.HostMount;
import com.vmware.vim.binding.vim.host.DiskDimensions.Lba;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.capability.VsanCapabilityUtils;
import com.vmware.vsan.client.services.csd.CsdUtils;
import com.vmware.vsan.client.util.VmodlHelper;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BaseUtils {
   private static final int BLOCK_SIZE_DEFAULT = 512;
   private static final String DATASTORE_HOST_PROPERTY = "host";
   private static final Log _logger = LogFactory.getLog(BaseUtils.class);

   public static void setUTCTimeZone(Calendar calendar) {
      if (calendar != null) {
         calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
      }

   }

   public static Calendar getCalendarFromLong(Long time) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(time);
      setUTCTimeZone(calendar);
      return calendar;
   }

   public static long lbaToBytes(Lba lba) {
      int blockSize = lba.blockSize;
      if (blockSize == 0) {
         blockSize = 512;
      }

      return lba.block * (long)blockSize;
   }

   public static String getIndexedString(List existingStrings, String baseString, String indexSeparator) {
      if (baseString != null && baseString.length() != 0) {
         if (existingStrings != null && existingStrings.size() != 0) {
            if (indexSeparator == null) {
               indexSeparator = "";
            }

            String newName = baseString;
            Boolean isUnique = false;
            int index = 1;

            while(true) {
               while(!isUnique) {
                  isUnique = true;
                  Iterator var6 = existingStrings.iterator();

                  while(var6.hasNext()) {
                     String str = (String)var6.next();
                     if (str.equalsIgnoreCase(newName)) {
                        newName = baseString + indexSeparator + index;
                        ++index;
                        isUnique = false;
                        break;
                     }
                  }
               }

               return newName;
            }
         } else {
            return baseString;
         }
      } else {
         throw new IllegalArgumentException("Default name cannot be null or empty.");
      }
   }

   public static ManagedObjectReference getCluster(ManagedObjectReference moRef) {
      Validate.notNull(moRef);
      String moRefType = moRef.getType();
      if (ClusterComputeResource.class.getSimpleName().equalsIgnoreCase(moRefType)) {
         return moRef;
      } else if (Datastore.class.getSimpleName().equalsIgnoreCase(moRefType)) {
         return getDatastoreCluster(moRef);
      } else if (VirtualMachine.class.getSimpleName().equalsIgnoreCase(moRefType)) {
         return getVmCluster(moRef);
      } else if (HostSystem.class.getSimpleName().equalsIgnoreCase(moRefType)) {
         return getHostCluster(moRef);
      } else {
         throw new IllegalArgumentException("Not supported MoRef type.");
      }
   }

   private static ManagedObjectReference getDatastoreCluster(ManagedObjectReference dsRef) {
      try {
         HostMount[] hostMounts = (HostMount[])QueryUtil.getProperty(dsRef, "host");
         Map hostToCluster = getHostToCluster(hostMounts);
         if (MapUtils.isEmpty(hostToCluster)) {
            return null;
         } else {
            List clusterRefs = new ArrayList(hostToCluster.values());
            List mountedHostRefs = new ArrayList(hostToCluster.keySet());
            return areMoRefsEqual(clusterRefs) && !allHostsSupportCsd(mountedHostRefs) ? (ManagedObjectReference)clusterRefs.get(0) : CsdUtils.getDatastoreServerCluster(dsRef);
         }
      } catch (Exception var5) {
         _logger.error("Could not retrieve cluster for datastore: " + dsRef, var5);
         return null;
      }
   }

   private static Map getHostToCluster(HostMount[] hostMounts) throws Exception {
      if (ArrayUtils.isEmpty(hostMounts)) {
         return new HashMap();
      } else {
         ManagedObjectReference[] hostRefs = (ManagedObjectReference[])Arrays.stream(hostMounts).filter((hostMount) -> {
            return hostMount.key != null;
         }).map((hostMount) -> {
            return hostMount.key;
         }).toArray((x$0) -> {
            return new ManagedObjectReference[x$0];
         });
         if (ArrayUtils.isEmpty(hostRefs)) {
            return new HashMap();
         } else {
            DataServiceResponse hostsProperties = QueryUtil.getProperties(hostRefs, new String[]{"parent"});
            return (Map)Arrays.stream(hostRefs).filter((hostRef) -> {
               return hostsProperties.hasProperty(hostRef, "parent");
            }).filter((hostRef) -> {
               ManagedObjectReference moRef = (ManagedObjectReference)hostsProperties.getProperty(hostRef, "parent");
               return ClusterComputeResource.class.getSimpleName().equalsIgnoreCase(moRef.getType());
            }).collect(Collectors.toMap((hostRef) -> {
               return hostRef;
            }, (hostRef) -> {
               return (ManagedObjectReference)hostsProperties.getProperty(hostRef, "parent");
            }));
         }
      }
   }

   private static boolean areMoRefsEqual(List moRefs) {
      boolean var1;
      if (CollectionUtils.isNotEmpty(moRefs)) {
         Stream var10000 = moRefs.stream();
         ManagedObjectReference var10001 = (ManagedObjectReference)moRefs.get(0);
         var10001.getClass();
         if (var10000.allMatch(var10001::equals)) {
            var1 = true;
            return var1;
         }
      }

      var1 = false;
      return var1;
   }

   private static boolean allHostsSupportCsd(List hostRefs) {
      return CollectionUtils.isNotEmpty(hostRefs) && hostRefs.stream().allMatch(VsanCapabilityUtils::isCsdSupported);
   }

   private static ManagedObjectReference getVmCluster(ManagedObjectReference vmRef) {
      try {
         ManagedObjectReference clusterRef = (ManagedObjectReference)QueryUtil.getProperty(vmRef, "cluster");
         return VmodlHelper.assignServerGuid(clusterRef, vmRef.getServerGuid());
      } catch (Exception var2) {
         return null;
      }
   }

   private static ManagedObjectReference getHostCluster(ManagedObjectReference hostRef) {
      try {
         ManagedObjectReference clusterRef = (ManagedObjectReference)QueryUtil.getProperty(hostRef, "cluster");
         return VmodlHelper.assignServerGuid(clusterRef, hostRef.getServerGuid());
      } catch (Exception var2) {
         _logger.error("The host is not in cluster.", var2);
         return null;
      }
   }

   public static ManagedObjectReference generateMor(String rowValue, String serverGuid) {
      String[] params = rowValue.split(":");
      return params != null && params.length >= 3 ? new ManagedObjectReference(params[params.length - 2], params[params.length - 1], serverGuid) : null;
   }

   public static Object getMapNextKey(Map mapObj) {
      return MapUtils.isEmpty(mapObj) ? null : mapObj.keySet().iterator().next();
   }

   public static double toPercentage(double value, double total) {
      return total == 0.0D ? 0.0D : value / total * 100.0D;
   }

   public static double round(double value, int digits) {
      return (double)((int)Math.round(value * Math.pow(10.0D, (double)digits))) / Math.pow(10.0D, (double)digits);
   }
}
