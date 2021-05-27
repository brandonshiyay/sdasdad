package com.vmware.vsan.client.services.diskmanagement;

import java.util.List;

public interface IStorageData {
   StorageCapacity getCapacity();

   List getObjectUuids();
}
