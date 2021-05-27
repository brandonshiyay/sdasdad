package com.vmware.vsan.client.util.dataservice.query;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vise.data.query.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryResult {
   private static final Logger logger = LoggerFactory.getLogger(QueryResult.class);
   public String name;
   public List items;
   public Exception exception;
   public int totalItemsCount;

   public Map getObjectToPropMap(String property) {
      Map result = new HashMap();
      Iterator var3 = this.items.iterator();

      while(var3.hasNext()) {
         QueryModel item = (QueryModel)var3.next();
         result.put(item.id, item.properties.getOrDefault(property, (Object)null));
      }

      return result;
   }

   public Set getResourceObjects() {
      Set moRefs = (Set)this.items.stream().map((queryModel) -> {
         return (ManagedObjectReference)queryModel.id;
      }).collect(Collectors.toSet());
      return moRefs;
   }

   static QueryResult fromResultSet(ResultSet resultSet) {
      logger.debug("Converting ResultSet to QueryResult: " + resultSet);
      if (resultSet == null) {
         return null;
      } else {
         QueryResult queryResult = new QueryResult();
         queryResult.name = resultSet.queryName;
         queryResult.items = (List)Arrays.stream(resultSet.items).map(QueryModel::fromResultItem).collect(Collectors.toList());
         queryResult.totalItemsCount = resultSet.totalMatchedObjectCount;
         queryResult.exception = resultSet.error;
         logger.debug("QueryResult: " + queryResult);
         return queryResult;
      }
   }

   public String toString() {
      return "QueryResult{name='" + this.name + '\'' + ", items=" + this.items + ", totalItemsCount=" + this.totalItemsCount + '}';
   }
}
