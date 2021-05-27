package com.vmware.vsan.client.services.common;

import com.vmware.vise.data.query.PropertyProviderAdapter;
import com.vmware.vise.data.query.PropertyRequestSpec;
import com.vmware.vise.data.query.ResultSet;
import com.vmware.vsan.client.sessionmanager.common.util.RequestUtil;
import com.vmware.vsan.client.sessionmanager.resource.CachedResourceFactory;
import com.vmware.vsphere.client.vsan.util.QueryUtil;
import com.vmware.vsphere.client.vsan.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public abstract class VsanBasePropertyProviderAdapter implements PropertyProviderAdapter {
   private final Log logger = LogFactory.getLog(this.getClass());
   @Autowired
   @Qualifier("vsanFactory")
   private CachedResourceFactory vsanFactory;

   public ResultSet getProperties(PropertyRequestSpec propertyRequest) {
      this.logger.trace("Entering VsanBasePropertyProviderAdapter with " + Utils.toString(propertyRequest));
      ResultSet result;
      if (!QueryUtil.isValidRequest(propertyRequest)) {
         result = new ResultSet();
         result.totalMatchedObjectCount = 0;
         this.logger.warn("Exiting " + this.getClass().getSimpleName() + " with " + Utils.toString(result));
         return result;
      } else {
         RequestUtil.assignVsanRequestId();
         result = null;

         try {
            this.logger.debug("Before processing property provider request");
            long startTime = System.currentTimeMillis();
            result = this.getResult(propertyRequest);
            long endTime = System.currentTimeMillis();
            this.logger.debug("Processing property request took: " + (endTime - startTime) + "ms");
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
               this.logger.trace("Exiting VsanBasePropertyProviderAdapter with " + Utils.toString(result));
            }
         }

         return result;
      }
   }

   protected abstract ResultSet getResult(PropertyRequestSpec var1);
}
