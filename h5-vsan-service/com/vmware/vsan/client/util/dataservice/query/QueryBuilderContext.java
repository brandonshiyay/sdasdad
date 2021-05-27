package com.vmware.vsan.client.util.dataservice.query;

import java.util.LinkedList;
import java.util.List;

class QueryBuilderContext {
   public String name;
   public List properties;
   public LinkedList tables = new LinkedList();
   public String orderBy;
   public boolean isAscending = true;
   public int startFrom;
   public Integer maxResultsCount;
}
