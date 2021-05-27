package com.vmware.vsan.client.util.dataservice.query;

import com.vmware.vise.data.query.DataService;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vise.data.query.Response;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import com.vmware.vsphere.client.vsan.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QueryExecutor {
   private static final VsanProfiler profiler = new VsanProfiler(QueryExecutor.class);
   private static final Logger logger = LoggerFactory.getLogger(QueryExecutor.class);
   @Autowired
   private DataService dataService;

   public QueryExecutorResult execute(RequestSpec requestSpec) {
      logger.info("Calling DataService: " + Utils.toString(requestSpec));
      Response response = null;
      VsanProfiler.Point p = profiler.point("DataService.getData");
      Throwable var4 = null;

      try {
         response = this.dataService.getData(requestSpec);
      } catch (Throwable var13) {
         var4 = var13;
         throw var13;
      } finally {
         if (p != null) {
            if (var4 != null) {
               try {
                  p.close();
               } catch (Throwable var12) {
                  var4.addSuppressed(var12);
               }
            } else {
               p.close();
            }
         }

      }

      logger.info("DataService response: " + Utils.toString(response));
      return QueryExecutorResult.fromDataServiceResponse(response);
   }
}
