package com.vmware.vsphere.client.vsan.base.data;

public class VsanCapabilityType {
   public static final String CAPABILITY = "capability";
   public static final String ALLFLASH = "allflash";
   public static final String STRETCHEDCLUSTER = "stretchedcluster";
   public static final String DATAEFFICIENCY = "dataefficiency";
   public static final String CLUSTERCONFIG = "clusterconfig";
   public static final String UPGRADE = "upgrade";
   public static final String OBJECTIDENTITIES = "objectidentities";
   public static final String ISCSITARGETS = "iscsitargets";
   public static final String WITNESSMANAGEMENT = "witnessmanagement";
   public static final String PERFSVCVERBOSEMODE = "perfsvcverbosemode";
   public static final String PERFSVCAUTOCONFIG = "perfsvcautoconfig";
   public static final String UPDATES_MGMT = "firmwareupdate";
   public static final String WHATIFCOMPLIANCE = "complianceprecheck";
   public static final String CONFIG_ASSIST = "configassist";
   public static final String RESYNC_THROTTLING = "throttleresync";
   public static final String ENCRYPTION = "encryption";
   public static final String PERFORMANCE_ANALYSIS = "perfanalysis";
   public static final String WHAT_IF = "decomwhatif";
   public static final String CLOUD_HEALTH = "cloudhealth";
   public static final String RESYNC_ENHANCED = "enhancedresyncapi";
   public static final String NETWORK_PERF_TEST = "netperftest";
   public static final String VSAN_VUM_INTEGRATION_ENABLED = "fullStackFw";
   public static final String WHAT_IF_CAPACITY = "whatifcapacity";
   public static final String HISTORICAL_CAPACITY = "historicalcapacity";
   public static final String NESTED_FDS_CAPABILITY = "genericnestedfd";
   public static final String REPAIR_TIMER_IN_RESYNC_STATS_CAPABILITY = "repairtimerinresyncstats";
   public static final String ADVANCED_OPTIONS_CAPABILITY = "clusteradvancedoptions";
   public static final String PURGE_INACCESSIBLE_VM_SWAP_OBJECTS_CAPABILITY = "purgeinaccessiblevmswapobjects";
   public static final String RECREATE_DISK_GROUP_CAPABILITY = "recreatediskgroup";
   public static final String UPDATE_VUM_RELEASE_CATALOG_OFFLINE_CAPABILITY = "updatevumreleasecatalogoffline";
   public static final String PERF_DIAGNOSTIC_MODE = "diagnosticmode";
   public static final String PERF_DIAGNOSTICS_FEEDBACK = "diagnosticsfeedback";
   public static final String ADVANCED_PERFORMANCE = "performanceforsupport";
   public static final String GET_HCL_LAST_UPDATE_ON_VC = "gethcllastupdateonvc";
   public static final String AUTOMATIC_REBALANCE = "automaticrebalance";
   public static final String FILE_SERVICE = "fileservices";
   public static final String CNS_VOLUMES = "cnsvolumes";
   public static final String VSAN_RDMA = "vsanrdma";
   public static final String RESYNC_ETA_IMPROVEMENT = "resyncetaimprovement";
   public static final String GUEST_TRIM_UNMAP = "umap";
   public static final String ISCSI_ONLINE_RESIZE = "vitonlineresize";
   public static final String ISCSI_STRETCHED_CLUSTER = "vitstretchedcluster";
   public static final String VUM_BASELINE_RECOMMENDATION = "vumbaselinerecommendation";
   public static final String SUPPORT_INSIGHT = "supportinsight";
   public static final String HOST_RESOURCE_PRECHECK = "resourceprecheck";
   public static final String DISK_RESOURCE_PRECHECK = "diskresourceprecheck";
   public static final String PERF_VERBOSE_MODE_IN_CLUSTER_CONFIGURATION = "verbosemodeconfiguration";
   public static final String VM_LEVEL_CAPACITY_MONITORING = "vmlevelcapacity";
   public static final String SLACK_SPACE_CAPACITY_REPORTING = "slackspacecapacity";
   public static final String HOST_RESERVED_CAPACITY = "hostreservedcapacity";
   public static final String PERFSVC_VSAN_CPU_METRICS = "perfsvcvsancpumetrics";
   public static final String FILE_SERVICE_KERBEROS = "fileservicekerberos";
   public static final String SHARED_WITNESS = "sharedwitness";
   public static final String PMAN_INTEGRATION = "pmanintegration";
   public static final String PERSISTENCE_SERVICE = "wcpappplatform";
   public static final String FILE_VOLUMES = "filevolumes";
   public static final String NATIVE_LARGE_CLUSTER = "nativelargeclustersupport";
   public static final String IO_INSIGHT = "ioinsight";
   public static final String DEV_VMODL_VERSION = "apidevversionenabled";
   public static final String HISTORICAL_HEALTH = "historicalhealth";
   public static final String DATA_IN_TRANSIT_ENCRYPTION = "dataintransitencryption";
   public static final String FILE_SERVICE_SMB = "fileservicesmb";
   public static final String FILE_SERVICE_NFSV3 = "fileservicenfsv3";
   public static final String REMOTE_DATASTORE = "remotedatastore";
   public static final String MULTI_VM_PERF = "perfsvctwoyaxisgraph";
   public static final String HARDWARE_MANAGEMENT = "hardwaremgmt";
   public static final String COMPRESSION_ONLY = "compressiononly";
   public static final String VSAN_DEFAULT_GATEWAY = "vsandefaultgatewaysupported";
   public static final String VSAN_MANAGED_VMFS_SUPPORTED = "vsanmanagedvmfs";
   public static final String SLACK_SPACE_RESERVATION = "capacityreservation";
   public static final String PERSISTENCE_SERVICE_AIR_GAP = "pspairgap";
   public static final String POLICY_SATISFIABILITY = "capacityevaluationonvc";
   public static final String VSAN_MANAGED_PMEM_SUPPORTED = "vsanmanagedpmem";
   public static final String VSAN_OBJECT_HEALTH_V2 = "vsanobjhealthv2";
   public static final String CAPACITY_OVERSUBSCRIPTION = "capacityoversubscription";
   public static final String VM_IO_DIAGNOSTICS = "vmIoDiagnostics";
   public static final String VSAN_NETWORK_DIAGNOSTICS = "vsandiagnostics";
   public static final String ENSURE_DURABILITY = "ensuredurability";
   public static final String DISK_MGMT_REDESIGN = "diskmgmtredesign";
   public static final String COMPUTE_ONLY = "vsanclient";
   public static final String TOP_CONTRIBUTORS = "topcontributors";
   public static final String CAPACITY_CUSTOMIZABLE_THRESHOLDS = "capacitycustomizablethresholds";
   public static final String FILE_SERVICE_STRETCHED_CLUSTER = "fileservicesc";
   public static final String FILE_SERVICE_SNAPSHOT = "fileservicesnapshot";
   public static final String FILE_SERVICE_OWE = "fileserviceowe";
   public static final String DIT_SHARED_WITNESS_INTEROPERABILITY = "dit4sw";
   public static final String VSAN_DIRECT_DISK_DECOMMISSION = "vsandirectdiskdecom";
   public static final String FILE_ANALYTICS = "vsanfileanalytics";
   public static final String VSAN_HCI_MESH_POLICY = "hcimeshpolicy";
   public static final String DATA_PERSISTENCE_RESOURCE_CHECK = "datapersistresourcecheck";

   private VsanCapabilityType() {
   }
}