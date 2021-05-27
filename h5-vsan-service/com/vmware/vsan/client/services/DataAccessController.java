package com.vmware.vsan.client.services;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.ObjectReferenceService;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(
   value = {"/data"},
   method = {RequestMethod.GET}
)
public class DataAccessController extends RestControllerBase {
   private static final String OBJECT_ID = "id";
   private final ObjectReferenceService _objectReferenceService;

   @Autowired
   public DataAccessController(@Qualifier("objectReferenceService") ObjectReferenceService objectReferenceService) {
      this._objectReferenceService = objectReferenceService;
      QueryUtil.setObjectReferenceService(objectReferenceService);
   }

   public DataAccessController() {
      this._objectReferenceService = null;
   }

   @RequestMapping({"/properties/{objectId}"})
   @ResponseBody
   public Map getProperties(@PathVariable("objectId") String encodedObjectId, @RequestParam("properties") String properties) throws Exception {
      ManagedObjectReference ref = this.getDecodedReference(encodedObjectId);
      String objectId = this._objectReferenceService.getUid(ref);
      String[] props = properties.split(",");
      PropertyValue[] pvs = QueryUtil.getProperties(ref, props).getPropertyValues();
      Map propsMap = new HashMap();
      propsMap.put("id", objectId);
      PropertyValue[] var8 = pvs;
      int var9 = pvs.length;

      for(int var10 = 0; var10 < var9; ++var10) {
         PropertyValue pv = var8[var10];
         propsMap.put(pv.propertyName, pv.value);
      }

      return propsMap;
   }

   @RequestMapping({"/multiObjectProperties/{objectIds}"})
   @ResponseBody
   public Object getMultiObjectProperties(@PathVariable("objectIds") String[] objectIds, @RequestParam("properties") String props) throws Exception {
      List objects = new ArrayList();
      String[] properties = objectIds;
      int var5 = objectIds.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         String objectId = properties[var6];
         objects.add(this.getDecodedReference(objectId));
      }

      properties = props.split(",");
      PropertyValue[] pvs = QueryUtil.getProperties((ManagedObjectReference[])objects.toArray(new ManagedObjectReference[0]), properties).getPropertyValues();
      return pvs;
   }

   @RequestMapping({"/propertiesByRelation/{objectId}"})
   @ResponseBody
   public PropertyValue[] getPropertiesForRelatedObject(@PathVariable("objectId") String encodedObjectId, @RequestParam(value = "relation",required = true) String relation, @RequestParam(value = "targetType",required = true) String targetType, @RequestParam(value = "properties",required = true) String properties) throws Exception {
      ManagedObjectReference ref = this.getDecodedReference(encodedObjectId);
      String[] props = properties.split(",");
      PropertyValue[] result = QueryUtil.getPropertiesForRelatedObjects(ref, relation, targetType, props).getPropertyValues();
      return result;
   }

   private ManagedObjectReference getDecodedReference(String encodedObjectId) throws Exception {
      Object ref = this._objectReferenceService.getReference(encodedObjectId, true);
      if (ref == null) {
         throw new Exception("Object not found with id: " + encodedObjectId);
      } else if (!(ref instanceof ManagedObjectReference)) {
         throw new Exception("The only supported object references are of type ManagedObjectReference: " + ref);
      } else {
         return (ManagedObjectReference)ref;
      }
   }
}
