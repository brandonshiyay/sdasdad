package com.vmware.vsan.client.util;

import com.vmware.vim.binding.vim.ClusterComputeResource;
import com.vmware.vim.binding.vim.Folder;
import com.vmware.vim.binding.vim.HostSystem;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.wsdlName;
import com.vmware.vim.vmomi.core.types.VmodlContext;
import com.vmware.vim.vmomi.core.types.VmodlType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VmodlHelper {
   public static final String HOST_FOLDER_PREFIX = "group-h";
   public static final String DATACENTER_FOLDER_PREFIX = "group-d";
   public static final String NETWORK_FOLDER_PREFIX = "group-n";
   public static final String STORAGE_FOLDER_PREFIX = "group-s";
   public static final String VM_FOLDER_PREFIX = "group-v";
   public static final String VC_ROOT_FOLDER = "group-d1";
   public static final String MOREF_UID_PREFIX = "urn:vmomi";
   @Autowired
   private VmodlContext vmodlContext;

   public Class getTypeClass(ManagedObjectReference ref) {
      VmodlType vmodlType = this.vmodlContext.getVmodlTypeMap().getVmodlType(ref.getType());
      return vmodlType.getTypeClass();
   }

   public boolean isOfType(ManagedObjectReference ref, Class typeClass) {
      return typeClass.isAssignableFrom(this.getTypeClass(ref));
   }

   public boolean isCluster(ManagedObjectReference entity) {
      return this.isOfType(entity, ClusterComputeResource.class);
   }

   public boolean isHost(ManagedObjectReference entity) {
      return this.isOfType(entity, HostSystem.class);
   }

   public boolean isHostFolder(ManagedObjectReference entity) {
      return this.isOfType(entity, Folder.class) && entity.getValue().contains("group-h");
   }

   public boolean isDatacenterFolder(ManagedObjectReference entity) {
      return this.isOfType(entity, Folder.class) && entity.getValue().contains("group-d");
   }

   public boolean isNetworkFolder(ManagedObjectReference entity) {
      return this.isOfType(entity, Folder.class) && entity.getValue().contains("group-n");
   }

   public boolean isVmFolder(ManagedObjectReference entity) {
      return this.isOfType(entity, Folder.class) && entity.getValue().contains("group-v");
   }

   public boolean isStorageFolder(ManagedObjectReference entity) {
      return this.isOfType(entity, Folder.class) && entity.getValue().contains("group-s");
   }

   public boolean isVcRootFolder(ManagedObjectReference entity) {
      return this.isOfType(entity, Folder.class) && entity.getValue().equalsIgnoreCase("group-d1");
   }

   public static ManagedObjectReference getRootFolder(String serverGuid) {
      ManagedObjectReference root = new ManagedObjectReference(((wsdlName)Folder.class.getAnnotation(wsdlName.class)).value(), "group-d1", serverGuid);
      return root;
   }

   public static ManagedObjectReference getStorageSystem(ManagedObjectReference hostRef) {
      ManagedObjectReference storageSystem = new ManagedObjectReference("HostStorageSystem", hostRef.getValue().replace("host", "storageSystem"), hostRef.getServerGuid());
      return storageSystem;
   }

   public ManagedObjectReference getVsanInternalSystem(ManagedObjectReference hostRef) {
      ManagedObjectReference vsanInternalSystem = new ManagedObjectReference("HostVsanInternalSystem", hostRef.getValue().replace("host", "ha-vsan-internal-system"), hostRef.getServerGuid());
      return vsanInternalSystem;
   }

   public static String morefToString(ManagedObjectReference moRef) {
      return moRef == null ? "" : "urn:vmomi:" + moRef.getType() + ':' + moRef.getValue() + ":" + moRef.getServerGuid();
   }

   public static ManagedObjectReference assignServerGuid(ManagedObjectReference ref, String serverGuid) {
      if (ref.getServerGuid() == null) {
         ref.setServerGuid(serverGuid);
      }

      return ref;
   }

   public static ManagedObjectReference[] assignServerGuid(ManagedObjectReference[] refs, String serverGuid) {
      ManagedObjectReference[] var2 = refs;
      int var3 = refs.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         ManagedObjectReference ref = var2[var4];
         assignServerGuid(ref, serverGuid);
      }

      return refs;
   }
}
