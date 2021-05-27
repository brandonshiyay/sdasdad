package com.vmware.vsan.client.util.dataservice.query;

import com.google.common.collect.ImmutableMap;
import com.vmware.vise.data.query.PropertyValue;
import com.vmware.vise.data.query.Response;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vsphere.client.vsan.util.DataServiceResponse;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;

public class QueryExecutorResult {
   private Map queryResults;

   public static QueryExecutorResult fromDataServiceResponse(Response response) {
      Validate.notNull(response);
      return fromResultSet(response.resultSet);
   }

   public static QueryExecutorResult fromResultSet(ResultSet... resultSets) {
      Validate.notEmpty(resultSets);
      QueryExecutorResult obj = new QueryExecutorResult();
      obj.queryResults = (Map)Arrays.stream(resultSets).map(QueryResult::fromResultSet).collect(Collectors.toMap((queryResult) -> {
         return queryResult.name;
      }, (queryResult) -> {
         return queryResult;
      }));
      return obj;
   }

   public Map getDataServiceResponses() {
      return (Map)this.queryResults.keySet().stream().collect(Collectors.toMap((queryKey) -> {
         return queryKey;
      }, (queryKey) -> {
         return new DataServiceResponse((PropertyValue[])((QueryResult)this.queryResults.get(queryKey)).items.stream().flatMap((queryModel) -> {
            return queryModel.properties.entrySet().stream().map((stringObjectEntry) -> {
               return QueryUtil.newProperty((String)stringObjectEntry.getKey(), stringObjectEntry.getValue(), queryModel.id);
            });
         }).toArray((x$0) -> {
            return new PropertyValue[x$0];
         }), (String[])((QueryResult)this.queryResults.get(queryKey)).items.stream().flatMap((queryModel) -> {
            return queryModel.properties.keySet().stream();
         }).distinct().toArray((x$0) -> {
            return new String[x$0];
         }));
      }));
   }

   public DataServiceResponse getDataServiceResponse() {
      QueryResult[] results = (QueryResult[])this.queryResults.values().toArray(new QueryResult[0]);
      if (results.length != 1) {
         throw new IllegalArgumentException("More than 1 query result");
      } else {
         QueryResult queryResult = results[0];
         if (queryResult.exception != null) {
            throw new QueryExecutorException(queryResult.exception);
         } else {
            return new DataServiceResponse((PropertyValue[])queryResult.items.stream().flatMap((queryModel) -> {
               return queryModel.properties.entrySet().stream().map((stringObjectEntry) -> {
                  return QueryUtil.newProperty((String)stringObjectEntry.getKey(), stringObjectEntry.getValue(), queryModel.id);
               });
            }).toArray((x$0) -> {
               return new PropertyValue[x$0];
            }), (String[])queryResult.items.stream().flatMap((queryModel) -> {
               return queryModel.properties.keySet().stream();
            }).distinct().toArray((x$0) -> {
               return new String[x$0];
            }));
         }
      }
   }

   public Map getQueryResults() {
      return ImmutableMap.copyOf(this.queryResults);
   }

   public QueryResult getQueryResult() {
      if (this.queryResults.size() != 1) {
         throw new RuntimeException("More than one QueryResult items!");
      } else {
         return (QueryResult)this.queryResults.values().iterator().next();
      }
   }

   public QueryResult getQueryResult(String queryName) {
      return (QueryResult)this.queryResults.get(queryName);
   }

   public List getExceptions() {
      return (List)this.queryResults.values().stream().filter((queryResult) -> {
         return queryResult.exception != null;
      }).map((queryResult) -> {
         return queryResult.exception;
      }).collect(Collectors.toList());
   }
}
