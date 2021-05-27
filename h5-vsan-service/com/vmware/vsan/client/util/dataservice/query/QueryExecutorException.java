package com.vmware.vsan.client.util.dataservice.query;

public class QueryExecutorException extends RuntimeException {
   public QueryExecutorException(Exception ex) {
      this(ex.getLocalizedMessage());
   }

   public QueryExecutorException(String message) {
      super(message);
   }
}
