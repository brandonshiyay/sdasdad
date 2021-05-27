package com.vmware.vsan.client.services.common.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum ConnectionState {
   connected,
   notResponding,
   disconnected;

   public static ConnectionState fromHostState(com.vmware.vim.binding.vim.HostSystem.ConnectionState state) {
      switch(state) {
      case connected:
         return connected;
      case notResponding:
         return notResponding;
      default:
         return disconnected;
      }
   }
}
