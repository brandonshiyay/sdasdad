package com.vmware.vsan.client.util.dataservice.query;

import com.vmware.vise.data.query.ResultItem;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryModel {
   private static final Logger logger = LoggerFactory.getLogger(QueryModel.class);
   public Object id;
   public Map properties;

   static QueryModel fromResultItem(ResultItem resultItem) {
      logger.debug("Converting ResultItem to Model: " + resultItem);
      if (resultItem == null) {
         return null;
      } else {
         QueryModel model = new QueryModel();
         model.id = resultItem.resourceObject;
         model.properties = (Map)Arrays.stream(resultItem.properties).collect(Collectors.toMap((prop) -> {
            return prop.propertyName;
         }, (prop) -> {
            return prop.value;
         }));
         logger.debug("Model: " + model);
         return model;
      }
   }

   public String toString() {
      return "Model{type=" + this.id + ", properties=" + this.properties + '}';
   }
}
