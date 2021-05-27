package com.vmware.vsphere.client.vsan.util;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.PropertyValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataServiceResponse {
   private static final Logger logger = LoggerFactory.getLogger(DataServiceResponse.class);
   public static final String RESOURCE_OBJECT = "__resourceObject";
   private final PropertyValue[] propertyValues;
   private Map mappedProperties;
   private final String[] properties;

   public DataServiceResponse(PropertyValue[] propertyValues, String[] properties) {
      this.properties = properties;
      this.propertyValues = propertyValues;
   }

   public String[] getRequestedProperties() {
      return this.properties;
   }

   public PropertyValue[] getPropertyValues() {
      return this.propertyValues;
   }

   public Map getMap() {
      if (ArrayUtils.isEmpty(this.properties) && ArrayUtils.isEmpty(this.propertyValues)) {
         return MapUtils.EMPTY_SORTED_MAP;
      } else {
         if (this.mappedProperties == null) {
            if (this.propertyValues.length % this.properties.length != 0) {
               logger.warn("The DataService didn't return data for all the requested properties!", this.propertyValues);
            }

            this.mappedProperties = new HashMap();
            PropertyValue[] var1 = this.propertyValues;
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
               PropertyValue propertyValue = var1[var3];
               if (!(propertyValue.resourceObject instanceof Object)) {
                  throw new IllegalStateException("unknown resource object: " + propertyValue.resourceObject);
               }

               ManagedObjectReference resourceObject = (ManagedObjectReference)propertyValue.resourceObject;
               Map resourceProperties = (Map)this.mappedProperties.get(resourceObject);
               if (resourceProperties == null) {
                  resourceProperties = new HashMap();
                  ((Map)resourceProperties).put("__resourceObject", resourceObject);
                  this.mappedProperties.put(resourceObject, resourceProperties);
               }

               ((Map)resourceProperties).put(propertyValue.propertyName, propertyValue.value);
            }
         }

         return Collections.unmodifiableMap(this.mappedProperties);
      }
   }

   public Set getResourceObjects() {
      return this.getMap().keySet();
   }

   public boolean hasProperty(Object resourceObject, String property) {
      Map objectProperties = (Map)this.getMap().get(resourceObject);
      return objectProperties != null && objectProperties.get(property) != null;
   }

   public Object getProperty(Object resourceObject, String property) {
      Map objectProperties = (Map)this.getMap().get(resourceObject);
      return objectProperties.get(property);
   }
}
