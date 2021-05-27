package com.vmware.vsan.client.services.common;

import com.vmware.vise.data.query.DataProviderAdapter;
import com.vmware.vise.data.query.RequestSpec;
import com.vmware.vise.data.query.Response;
import com.vmware.vsan.client.sessionmanager.common.util.RequestUtil;
import com.vmware.vsan.client.sessionmanager.resource.CachedResourceFactory;
import com.vmware.vsphere.client.vsan.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public abstract class VsanBaseDataProviderAdapter implements DataProviderAdapter {
   private final Log logger = LogFactory.getLog(this.getClass());
   @Autowired
   @Qualifier("vsanFactory")
   private CachedResourceFactory vsanFactory;

   public Response getData(RequestSpec requestSpec) throws Exception {
      this.logger.trace("Entering VsanBaseDataProviderAdapter with " + Utils.toString(requestSpec));
      if (requestSpec != null && requestSpec.querySpec != null) {
         RequestUtil.assignVsanRequestId();
         Response response = null;

         try {
            this.logger.debug("Before processing data provider request");
            long startTime = System.currentTimeMillis();
            response = this.getResponse(requestSpec);
            long endTime = System.currentTimeMillis();
            this.logger.debug("Processing data service request took: " + (endTime - startTime) + "ms");
         } catch (Exception var31) {
            this.logger.error("Failed to get data service request", var31);
            throw var31;
         } finally {
            try {
               this.vsanFactory.removeRequestEntries();
            } catch (Exception var29) {
               this.logger.error("Failed to remove request entries from cache", var29);
               throw var29;
            } finally {
               RequestUtil.removeVsanRequestId();
               this.logger.trace("Exiting VsanBaseDataProviderAdapter with " + Utils.toString(response));
            }
         }

         return response;
      } else {
         throw new IllegalArgumentException("requestSpec");
      }
   }

   protected abstract Response getResponse(RequestSpec var1) throws Exception;
}
