package com.vmware.vsphere.client.vsan.util;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.binding.vmodl.fault.ManagedObjectNotFound;
import com.vmware.vise.data.Constraint;
import com.vmware.vise.data.ParameterSpec;
import com.vmware.vise.data.PropertySpec;
import com.vmware.vise.data.ResourceSpec;
import com.vmware.vise.data.query.Comparator;
import com.vmware.vise.data.query.CompositeConstraint;
import com.vmware.vise.data.query.Conjoiner;
import com.vmware.vise.data.query.DataService;
import com.vmware.vise.data.query.ObjectIdentityConstraint;
import com.vmware.vise.data.query.ObjectReferenceService;
import com.vmware.vise.data.query.PropertyConstraint;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.QuerySpec;
import com.vmware.vise.data.query.RelationalConstraint;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vise.data.query.Response;
import com.vmware.vise.data.query.ResultItem;
import com.vmware.vise.data.query.ResultSet;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@TsModel
public class QueryUtil {
   private static ObjectReferenceService _objectReferenceService;
   private static final String ENTITY_REF_ID_SEPARATOR = ":";
   private static DataService _dataService;
   public static final String SERVER_GUID_PROPERTY = "serverGuid";
   public static final String NAME_PROPERTY = "name";
   public static final String PRIMARY_ICON_ID_PROPERTY = "primaryIconId";
   public static final String CLUSTER_PROPERTY = "cluster";
   public static final String CLUSTER_HOST_PROPERTY = "host";
   public static final String VM_PROPERTY = "vm";
   public static final String VM_COUNT_PROPERTY = "vm._length";
   public static final String RESOURCE_POOL_PROPERTY = "resourcePool";
   public static final String RESOURCE_POOL_OWNER_PROPERTY = "owner";
   public static final String DATASTORE_PROPERTY = "datastore";
   public static final String CLUSTER_HOST_COUNT_PROPERTY = "host._length";
   public static final String HOST_VSAN_NODE_UUID_PROPERTY = "config.vsanHostConfig.clusterInfo.nodeUuid";
   public static final String HOST_CONNECTION_STATE_PROPERTY = "runtime.connectionState";
   public static final String HOST_MAINTENANCE_MODE_PROPERTY = "runtime.inMaintenanceMode";
   public static final String HOST_QUARANTINE_MODE_PROPERTY = "runtime.inQuarantineMode";
   public static final String WITNESS_HOST_RELATION = "witnessHost";
   public static final String IS_WITNESS_HOST_PROPERTY = "isWitnessHost";
   public static final String IS_WITNESS_VLCM_PROPERTY = "isVsanWitnessLifecycleManaged";
   public static final String ALL_VSAN_HOSTS_RELATION = "allVsanHosts";
   public static final String VSAN_DISK_GROUP_PROPERTY_NAME = "vsanDisksAndGroupsData";
   public static final String VSAN_PHYSICAL_DISK_VIRTUAL_MAPPING = "vsanPhysicalDiskVirtualMapping";
   public static final String VSAN_HOST_STORAGE_ADAPTER_DEVICES = "vsanStorageAdapterDevices";
   public static final String VM_DEVICES_PROPERTY = "config.hardware.device";
   public static final String VM_NAMESPACE_CAPABILITY_METADATA = "namespaceCapabilityMetadata";
   public static final String VM_PATH_NAME = "summary.config.vmPathName";
   public static final String VM_HOST = "summary.runtime.host";
   public static final String VM_VSAN_NODE_UUID_PROPERTY = "config.instanceUuid";
   public static final String VSAN_ENABLED_PROPERTY = "configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.enabled";
   public static final String HOST_VERSION_PROPERTY = "config.product.version";
   public static final String HOST_VSAN_CONFIG_PROPERTY = "config.vsanHostConfig";
   public static final String HOST_VSAN_ENABLED_PROPERTY = "config.vsanHostConfig.enabled";
   public static final String VSAN_DISK_VERSION_PROPERTY_NAME = "vsanDiskVersionsData";
   public static final String CLUSTER_VSAN_CONFIG_UUID_PROPERTY = "configurationEx[@type='ClusterConfigInfoEx'].vsanConfigInfo.defaultConfig.uuid";
   public static final String CLUSTER_DRS_ENABLED = "configuration.drsConfig";
   public static final String VM_STORAGE_OBJECT_ID_PROPERTY = "config.vmStorageObjectId";
   public static final String IS_POD_VM = "isPodVM";
   public static final String DATASTORE_TYPE_PROPERTY = "summary.type";
   public static final String DATASTORE_URL = "summary.url";
   public static final String DATACENTER_RELATION = "dc";
   public static final String DATASTORE_HOST_MOUNTS = "host";
   public static final String DATASTORE_SUMMARY = "summary";
   public static final String DATASTORE_SUMMARY_CAPACITY = "summary.capacity";
   public static final String DATASTORE_SUMMARY_FREE_SPACE = "summary.freeSpace";
   public static final String DATASTORE_CONTAINER_ID = "info.containerId";
   public static final String DATASTORE_SERVER_HOSTS = "serverHosts";
   public static final String PREFERRED_FD_PROPERTY = "preferredFaultDomain";
   public static final String HOST_FAULT_DOMAIN = "config.vsanHostConfig.faultDomainInfo.name";
   public static final String VM_IS_TEMPLATE = "config.template";
   public static final String VM_POWER_STATE = "powerState";
   public static final String VM_SWAP_STORAGE_OBJECT_ID = "config.swapStorageObjectId";
   public static final String WCP_NAMESPACE = "workload";
   public static final String TANZU_KUBERNETES_CLUSTER = "com.vmware.wcp.TanzuKubernetesCluster";
   public static final String PARENT_PROPERTY = "parent";
   public static final String VIRTUAL_NIC_PROPERTY = "config.network.vnic";
   public static final String VSAN_SEMI_AUTO_DISKS_PROPERTY_NAME = "vsanSemiAutoClaimDisksData";
   public static final String HOST_PNIC = "config.network.pnic";
   public static final String HOST_PORTGROUP = "config.network.portgroup";
   public static final String HOST_PROXY_SWITCH = "config.network.proxySwitch";
   public static final String HOST_OPAQUE_NETWORK = "config.network.opaqueNetwork";
   public static final String HOST_OPAQUE_SWITCH = "config.network.opaqueSwitch";
   public static final String NETWORK_PROPERTY = "network";
   public static final String ACTIVE_UPLINK_PORT_PROPERTY = "config.defaultPortConfig.uplinkTeamingPolicy.uplinkPortOrder.activeUplinkPort";
   public static final String DISTRIBUTED_VIRTUAL_SWITCH_PROPERTY = "config.distributedVirtualSwitch";
   public static final String HOST_VSANCONFIG_DISK_MAPPING_PROPERTY = "config.vsanHostConfig.storageInfo.diskMapping";
   public static final String VSAN_HOST_CONFIG_NETWORKINFO_PORT_PROPERTY = "config.vsanHostConfig.networkInfo.port";
   public static final String PMEM_STORAGE_UUID = "info.pmem.uuid";
   public static final String PMEM_STORAGE_STATUS = "overallStatus";
   private static Log logger = LogFactory.getLog(QueryUtil.class);

   public static void setObjectReferenceService(ObjectReferenceService objectReferenceService) {
      _objectReferenceService = objectReferenceService;
   }

   public static void setDataService(DataService dataService) {
      _dataService = dataService;
   }

   public static Object getProperty(ManagedObjectReference target, String propertyName, Object parameter) throws Exception {
      ResourceSpec rs = new ResourceSpec();
      rs.constraint = createObjectIdentityConstraint(target, target.getType());
      PropertySpec ps = new PropertySpec();
      ps.propertyNames = new String[]{propertyName};
      if (parameter != null) {
         ParameterSpec paramSpec = new ParameterSpec();
         paramSpec.parameter = parameter;
         paramSpec.propertyName = propertyName;
         ps.parameters = new ParameterSpec[]{paramSpec};
      } else {
         ps.parameters = new ParameterSpec[0];
      }

      rs.propertySpecs = new PropertySpec[]{ps};
      QuerySpec query = new QuerySpec();
      query.resourceSpec = rs;
      RequestSpec request = new RequestSpec();
      request.querySpec = new QuerySpec[]{query};
      if (target.getServerGuid().isEmpty()) {
         logger.warn("Calling DataService for moRef: " + target.getValue() + " with missing serverGuid. This will give no result in multiVC env.");
      }

      logger.info("Calling DataService: " + Utils.toString(request));
      Response response = _dataService.getData(request);
      logger.info("DataService response: " + Utils.toString(response));
      if (response.resultSet.length == 1 && response.resultSet[0].items.length == 1) {
         if (response.resultSet[0].error != null) {
            throw response.resultSet[0].error;
         } else {
            PropertyValue[] var8 = response.resultSet[0].items[0].properties;
            int var9 = var8.length;

            for(int var10 = 0; var10 < var9; ++var10) {
               PropertyValue pv = var8[var10];
               if (pv.propertyName.equals(propertyName)) {
                  return pv.value;
               }
            }

            return null;
         }
      } else {
         throw new IllegalStateException("illegal resource, 1 item expected");
      }
   }

   public static Object getProperty(ManagedObjectReference target, String propertyName) throws Exception {
      return getProperty(target, propertyName, (Object)null);
   }

   public static DataServiceResponse getProperties(ManagedObjectReference obj, String[] properties) throws Exception {
      return getProperties(new ManagedObjectReference[]{obj}, properties);
   }

   public static DataServiceResponse getProperties(ManagedObjectReference[] objs, String[] properties) throws Exception {
      if (objs != null && objs.length != 0 && properties != null && properties.length != 0) {
         Object obj = objs[0];
         QuerySpec query = buildQuerySpec(objs, properties);
         query.name = _objectReferenceService.getUid(obj) + ".properties";
         ResultSet resultSet = getData(query);
         return getDataServiceResponse(resultSet, properties);
      } else {
         throw new Exception("Invalid parameters for getProperties");
      }
   }

   public static DataServiceResponse getPropertyForRelatedObjects(ManagedObjectReference object, String relationship, String targetType, String property) throws Exception {
      return getPropertiesForRelatedObjects(object, relationship, targetType, new String[]{property});
   }

   public static DataServiceResponse getPropertiesForRelatedObjects(ManagedObjectReference obj, String relationship, String targetType, String[] properties) throws Exception {
      if (obj != null && properties != null && properties.length != 0) {
         if (relationship != null && relationship.length() != 0) {
            ObjectIdentityConstraint objectConstraint = createObjectIdentityConstraint(obj);
            RelationalConstraint relationalConstraint = createRelationalConstraint(relationship, objectConstraint, true, targetType);
            QuerySpec query = buildQuerySpec((Constraint)relationalConstraint, properties);
            query.name = _objectReferenceService.getUid(obj) + "." + relationship + ".properties";
            ResultSet resultSet = getData(query);
            return getDataServiceResponse(resultSet, properties);
         } else {
            return getProperties(obj, properties);
         }
      } else {
         throw new Exception("invalid parameters in getPropertiesForRelatedObjects");
      }
   }

   public static DataServiceResponse getDataServiceResponse(ResultSet resultSet, String[] properties) throws Exception {
      if (resultSet.totalMatchedObjectCount == 0 && resultSet.error != null) {
         throw resultSet.error;
      } else {
         List result = new ArrayList();
         if (resultSet != null && resultSet.items != null) {
            ResultItem[] var3 = resultSet.items;
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               ResultItem item = var3[var5];
               Map resultValues = new HashMap();
               if (item != null && item.properties != null) {
                  PropertyValue[] var8 = item.properties;
                  int var9 = var8.length;

                  int var10;
                  for(var10 = 0; var10 < var9; ++var10) {
                     PropertyValue propValue = var8[var10];
                     if (propValue != null) {
                        if (propValue.resourceObject == null && item.resourceObject != null) {
                           propValue.resourceObject = item.resourceObject;
                        }

                        resultValues.put(propValue.propertyName, propValue);
                        result.add(propValue);
                     }
                  }

                  String[] var13 = properties;
                  var9 = properties.length;

                  for(var10 = 0; var10 < var9; ++var10) {
                     String property = var13[var10];
                     if (!resultValues.containsKey(property)) {
                        PropertyValue pv = new PropertyValue();
                        pv.propertyName = property;
                        pv.resourceObject = item.resourceObject;
                        pv.value = null;
                        result.add(pv);
                     }
                  }
               }
            }
         }

         return new DataServiceResponse((PropertyValue[])result.toArray(new PropertyValue[0]), properties);
      }
   }

   public static ResultSet getData(QuerySpec query) throws Exception {
      return getDataMultiSpec(new QuerySpec[]{query})[0];
   }

   public static ResultSet[] getDataMultiSpec(QuerySpec[] queries) throws Exception {
      RequestSpec requestSpec = new RequestSpec();
      requestSpec.querySpec = queries;
      Response response = _dataService.getData(requestSpec);
      ResultSet[] result = response.resultSet;
      if (result != null && result.length != 0 && result[0] != null) {
         if (response.resultSet[0].error != null) {
            throw response.resultSet[0].error;
         } else {
            return result;
         }
      } else {
         throw new Exception("Empty result");
      }
   }

   public static QuerySpec buildQuerySpec(ManagedObjectReference entity, String[] properties) {
      ObjectIdentityConstraint oc = createObjectIdentityConstraint(entity);
      String targetType = _objectReferenceService.getResourceObjectType(entity);
      Set targetTypes = new HashSet();
      targetTypes.add(targetType);
      QuerySpec query = buildQuerySpec(oc, properties, targetTypes);
      return query;
   }

   public static QuerySpec buildQuerySpec(ManagedObjectReference[] entities, String[] properties) {
      if (entities.length == 1) {
         return buildQuerySpec(entities[0], properties);
      } else {
         CompositeConstraint cc = new CompositeConstraint();
         cc.conjoiner = Conjoiner.OR;
         Constraint[] nestedConstraints = new Constraint[entities.length];
         Set targetTypes = new HashSet();
         String targetType = null;

         for(int index = 0; index < entities.length; ++index) {
            nestedConstraints[index] = createObjectIdentityConstraint(entities[index]);
            targetType = _objectReferenceService.getResourceObjectType(entities[index]);
            targetTypes.add(targetType);
         }

         cc.nestedConstraints = nestedConstraints;
         QuerySpec query = buildQuerySpec(cc, properties, targetTypes);
         return query;
      }
   }

   public static QuerySpec buildQuerySpec(Constraint constraint, String[] properties) {
      QuerySpec query = buildQuerySpec(constraint, properties, (Set)null);
      return query;
   }

   public static QuerySpec buildQuerySpec(Constraint constraint, String[] properties, Set targetTypes) {
      QuerySpec query = new QuerySpec();
      ResourceSpec resourceSpec = new ResourceSpec();
      resourceSpec.constraint = constraint;
      List pSpecs = new ArrayList();
      if (targetTypes != null) {
         Iterator var6 = targetTypes.iterator();

         while(var6.hasNext()) {
            String targetType = (String)var6.next();
            PropertySpec propSpec = createPropertySpec(properties, targetType);
            pSpecs.add(propSpec);
         }
      } else {
         PropertySpec propSpec = createPropertySpec(properties, (String)null);
         pSpecs.add(propSpec);
      }

      resourceSpec.propertySpecs = (PropertySpec[])pSpecs.toArray(new PropertySpec[0]);
      query.resourceSpec = resourceSpec;
      return query;
   }

   public static RelationalConstraint createRelationalConstraint(String relationship, Constraint constraintOnRelatedObject, Boolean hasInverseRelation, String targetType) {
      RelationalConstraint rc = new RelationalConstraint();
      rc.relation = relationship;
      rc.hasInverseRelation = hasInverseRelation;
      rc.constraintOnRelatedObject = constraintOnRelatedObject;
      rc.targetType = targetType;
      return rc;
   }

   public static ObjectIdentityConstraint createObjectIdentityConstraint(Object entity, String targetType) {
      if (entity instanceof ManagedObjectReference) {
         ManagedObjectReference moRef = (ManagedObjectReference)entity;
         if (moRef.getServerGuid().isEmpty()) {
            logger.warn("An objectIdentityConstraint is created for moRef: " + moRef.getValue() + " without serverGuid. This will bring no result in multiVC env");
         }
      }

      ObjectIdentityConstraint oc = new ObjectIdentityConstraint();
      oc.target = entity;
      oc.targetType = targetType;
      return oc;
   }

   public static ObjectIdentityConstraint createObjectIdentityConstraint(Object entity) {
      return createObjectIdentityConstraint(entity, _objectReferenceService.getResourceObjectType(entity));
   }

   public static PropertyConstraint createPropertyConstraint(String targetType, String propertyName, Comparator comparator, Object value) {
      PropertyConstraint propConstraint = new PropertyConstraint();
      propConstraint.targetType = targetType;
      propConstraint.propertyName = propertyName;
      propConstraint.comparableValue = value;
      propConstraint.comparator = comparator;
      return propConstraint;
   }

   public static CompositeConstraint createCompositeConstraint(Conjoiner conjoiner, String targetType, Constraint... nestedConstraints) {
      CompositeConstraint constraint = new CompositeConstraint();
      constraint.targetType = targetType;
      constraint.conjoiner = conjoiner;
      constraint.nestedConstraints = nestedConstraints;
      return constraint;
   }

   private static PropertySpec createPropertySpec(String[] properties, String targetType) {
      PropertySpec propSpec = new PropertySpec();
      propSpec.type = targetType;
      propSpec.propertyNames = properties;
      return propSpec;
   }

   public static Response newResponse(ResultSet... resultSet) {
      Response result = new Response();
      result.resultSet = resultSet;
      return result;
   }

   public static ResultSet newResultSet(ResultItem... items) {
      ResultSet result = new ResultSet();
      result.items = items;
      result.totalMatchedObjectCount = items != null ? items.length : null;
      return result;
   }

   public static ResultSet newResultSetWithErrors(Exception error) {
      ResultSet resultSet = new ResultSet();
      resultSet.error = error;
      return resultSet;
   }

   public static ResultItem newResultItem(Object object, PropertyValue... props) {
      ResultItem result = new ResultItem();
      result.resourceObject = object;
      result.properties = props;
      return result;
   }

   public static PropertyValue newProperty(String name, Object value) {
      PropertyValue result = new PropertyValue();
      result.propertyName = name;
      result.value = value;
      return result;
   }

   public static PropertyValue newProperty(String name, Object value, Object resourceObject) {
      PropertyValue result = newProperty(name, value);
      result.resourceObject = resourceObject;
      return result;
   }

   public static Collection getObjectRefs(Object[] objects) {
      if (objects != null && objects.length != 0) {
         Collection result = new HashSet();
         Object[] var2 = objects;
         int var3 = objects.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Object obj = var2[var4];
            if (!(obj instanceof ManagedObjectReference)) {
               logger.warn("The given object is not a valid MOR: " + obj);
            } else {
               result.add(obj);
            }
         }

         return result;
      } else {
         return new HashSet();
      }
   }

   public static boolean isAnyPropertyRequested(PropertySpec[] propertySpecs, String... properties) {
      if (!ArrayUtils.isEmpty(propertySpecs) && !ArrayUtils.isEmpty(properties)) {
         Set propertiesSet = new HashSet(Arrays.asList(properties));
         boolean result = false;
         PropertySpec[] var4 = propertySpecs;
         int var5 = propertySpecs.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            PropertySpec pSpec = var4[var6];
            String[] var8 = pSpec.propertyNames;
            int var9 = var8.length;

            for(int var10 = 0; var10 < var9; ++var10) {
               String p = var8[var10];
               if (propertiesSet.contains(p)) {
                  result = true;
                  break;
               }
            }
         }

         return result;
      } else {
         return false;
      }
   }

   public static String[] getPropertyNames(PropertySpec[] props) {
      Set allProperties = new HashSet();
      PropertySpec[] var2 = props;
      int var3 = props.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         PropertySpec propSpec = var2[var4];
         if (!ArrayUtils.isEmpty(propSpec.propertyNames)) {
            String[] var6 = propSpec.propertyNames;
            int var7 = var6.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               String propertyName = var6[var8];
               allProperties.add(propertyName);
            }
         }
      }

      return (String[])allProperties.toArray(new String[allProperties.size()]);
   }

   public static Map groupPropertiesByObject(PropertyValue[] properties) {
      Map result = new HashMap();
      PropertyValue[] var2 = properties;
      int var3 = properties.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         PropertyValue property = var2[var4];
         ManagedObjectReference objectMor = (ManagedObjectReference)property.resourceObject;
         if (!result.containsKey(objectMor)) {
            result.put(objectMor, new ArrayList());
         }

         ((List)result.get(objectMor)).add(property);
      }

      return result;
   }

   public static Constraint combineIntoSingleConstraint(Constraint[] constraints, Conjoiner conjoiner) {
      if (constraints != null && constraints.length != 0) {
         return (Constraint)(constraints.length == 1 ? constraints[0] : createCompositeConstraint(constraints, conjoiner));
      } else {
         return null;
      }
   }

   public static CompositeConstraint createCompositeConstraint(Constraint[] nestedConstraints, Conjoiner conjoiner) {
      CompositeConstraint compositeConstraint = new CompositeConstraint();
      compositeConstraint.nestedConstraints = nestedConstraints;
      compositeConstraint.conjoiner = conjoiner;
      return compositeConstraint;
   }

   public static Constraint createConstraintForRelationship(Object object, String relationship, String targetType) {
      ObjectIdentityConstraint objectConstraint = createObjectIdentityConstraint(object);
      RelationalConstraint relationalConstraint = createRelationalConstraint(relationship, objectConstraint, true, targetType);
      return relationalConstraint;
   }

   public static void throwIfObjectNotFound(Object[] targetEntities, ResultSet resultSet) throws ManagedObjectNotFound {
      Object[] deletedObjects = detectDeletedObjects(targetEntities, resultSet);
      if (deletedObjects.length != 0) {
         Object firstObject = deletedObjects[0];
         if (firstObject instanceof ManagedObjectReference) {
            throw new ManagedObjectNotFound((ManagedObjectReference)firstObject);
         } else {
            throw new ManagedObjectNotFound();
         }
      }
   }

   public static Object[] detectDeletedObjects(Object[] targetEntities, ResultSet resultSet) {
      if (targetEntities != null && targetEntities.length != 0) {
         if (resultSet != null && resultSet.error == null) {
            HashMap deletedObjects = new HashMap();
            Object[] var3 = targetEntities;
            int var4 = targetEntities.length;

            int var5;
            for(var5 = 0; var5 < var4; ++var5) {
               Object entity = var3[var5];
               deletedObjects.put(_objectReferenceService.getUid(entity), entity);
            }

            if (resultSet.items != null) {
               ResultItem[] var8 = resultSet.items;
               var4 = var8.length;

               for(var5 = 0; var5 < var4; ++var5) {
                  ResultItem resultItem = var8[var5];
                  Object object = resultItem.resourceObject;
                  if (object != null) {
                     deletedObjects.remove(_objectReferenceService.getUid(object));
                  }
               }
            }

            Collection result = deletedObjects.values();
            return result.toArray();
         } else {
            return (Object[])((Object[])Array.newInstance(targetEntities[0].getClass(), 0));
         }
      } else {
         return targetEntities;
      }
   }

   public static boolean isValidRequest(PropertyRequestSpec propertyRequest) {
      if (propertyRequest == null) {
         return false;
      } else {
         return !ArrayUtils.isEmpty(propertyRequest.objects) && !ArrayUtils.isEmpty(propertyRequest.properties);
      }
   }

   public static PropertyValue createPropValue(String name, Object value, Object provider) {
      PropertyValue propValue = new PropertyValue();
      propValue.propertyName = name;
      propValue.value = value;
      propValue.resourceObject = provider;
      return propValue;
   }

   public static ResultItem createResultItem(String property, Object value, Object provider) {
      ResultItem resultItem = new ResultItem();
      resultItem.resourceObject = provider;
      resultItem.properties = new PropertyValue[]{createPropValue(property, value, provider)};
      return resultItem;
   }

   public static String extractNodeUuid(String entityRefId) {
      return entityRefId.substring(entityRefId.indexOf(":") + 1);
   }
}
