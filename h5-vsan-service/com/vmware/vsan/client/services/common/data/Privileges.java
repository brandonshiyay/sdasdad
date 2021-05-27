package com.vmware.vsan.client.services.common.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class Privileges {
   public static final String EDIT_CLUSTER = "Host.Inventory.EditCluster";
   public static final String RENAME_CLUSTER = "Host.Inventory.RenameCluster";
   public static final String CONFIG_STORAGE = "Host.Config.Storage";
   public static final String HOST_MAINTENANCE = "Host.Config.Maintenance";
   public static final String NAMESPACES_MANAGE = "Namespaces.Manage";
   public static final String NAMESPACES_CONFIGURE = "Namespaces.Configure";
   public static final String DATASTORE_FILE_MANAGEMENT = "Datastore.FileManagement";
   public static final String ADD_STANDALONE_HOST = "Host.Inventory.AddStandaloneHost";
   public static final String MOVE_HOST = "Host.Inventory.MoveHost";
   public static final String MANAGE_KEYS = "Cryptographer.ManageKeys";
   public static final String SHALLOW_REKEY = "Vsan.Cluster.ShallowRekey";
   public static final String MANAGE_KEY_SERVERS = "Cryptographer.ManageKeyServers";
   public static final String MANAGE_ENCRYPTION_POLICIES = "Cryptographer.ManageEncryptionPolicy";
   public static final String GLOBAL_DIAGNOSTICS = "Global.Diagnostics";
   public static final String GLOBAL_SETTINGS = "Global.Settings";
   public static final String GLOBAL_VCSERVER = "Global.VCServer";
   public static final String SYSTEM_READ = "System.Read";
   public static final String READ_POLICIES = "StorageProfile.View";
   public static final String UPDATE_POLICIES = "StorageProfile.Update";
   public static final String DVS_CREATE = "DVSwitch.Create";
   public static final String DVS_MODIFY = "DVSwitch.Modify";
   public static final String DVS_HOST_OPERATION = "DVSwitch.HostOp";
   public static final String PORT_GROUP_CREATE = "DVPortgroup.Create";
   public static final String HOST_NETWORK_CONFIG = "Host.Config.Network";
   public static final String ASSIGN_NETWORK = "Network.Assign";
}
