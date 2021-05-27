package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareNetPermission;
import org.apache.commons.lang3.BooleanUtils;

@TsModel
public class VsanFileServiceShareNetPermission {
   public String ipAddress;
   public boolean isReadonly;
   public boolean isWriteAllowed;
   public boolean isRootSquashed;
   public boolean isNoAccess;

   public static VsanFileServiceShareNetPermission fromVmodl(FileShareNetPermission vmodl) {
      if (vmodl == null) {
         return null;
      } else {
         VsanFileServiceShareNetPermission sharePermission = new VsanFileServiceShareNetPermission();
         sharePermission.ipAddress = vmodl.ips;
         if (VsanFileServiceShareNetPermissionType.READ_WRITE.value.equals(vmodl.permissions)) {
            sharePermission.isWriteAllowed = true;
         } else if (VsanFileServiceShareNetPermissionType.READ_ONLY.value.equals(vmodl.permissions)) {
            sharePermission.isReadonly = true;
         }

         sharePermission.isRootSquashed = BooleanUtils.isNotTrue(vmodl.allowRoot);
         return sharePermission;
      }
   }

   public FileShareNetPermission toVmodl() {
      FileShareNetPermission vmodl = new FileShareNetPermission();
      vmodl.ips = this.ipAddress;
      vmodl.allowRoot = !this.isRootSquashed;
      if (this.isNoAccess) {
         vmodl.permissions = VsanFileServiceShareNetPermissionType.NO_ACCESS.value;
      } else if (this.isWriteAllowed) {
         vmodl.permissions = VsanFileServiceShareNetPermissionType.READ_WRITE.value;
      } else if (this.isReadonly) {
         vmodl.permissions = VsanFileServiceShareNetPermissionType.READ_ONLY.value;
      }

      return vmodl;
   }
}
