package com.vmware.vsan.client.services.obfuscation;

import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vim.vsan.binding.vim.VsanPhoneHomeSystem;
import com.vmware.vise.data.query.ObjectReferenceService;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vsan.VsanConnection;
import com.vmware.vsphere.client.vsan.base.util.VsanProfiler;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(
   value = {"/support/obfuscation"},
   method = {RequestMethod.GET}
)
public class ObfuscationController {
   private Logger logger = LoggerFactory.getLogger(ObfuscationController.class);
   private static final VsanProfiler _profiler = new VsanProfiler(ObfuscationController.class);
   @Autowired
   private ObjectReferenceService objRefService;
   @Autowired
   private VsanClient vsanClient;

   @RequestMapping(
      value = {"/{operationType}/{objectId}"},
      method = {RequestMethod.GET}
   )
   public void downloadObfuscationMap(@PathVariable("operationType") String operationType, @PathVariable("objectId") String objectId, HttpServletResponse response) throws Exception {
      ManagedObjectReference clusterRef = (ManagedObjectReference)this.objRefService.getReference(objectId);
      String obfuscationMap = null;
      VsanConnection conn = this.vsanClient.getConnection(clusterRef.getServerGuid());
      Throwable var7 = null;

      try {
         VsanPhoneHomeSystem phoneHomeSystem = conn.getPhoneHomeSystem();

         try {
            VsanProfiler.Point point = _profiler.point("phoneHomeSystem.vsanGetPhoneHomeObfuscationMap");
            Throwable var10 = null;

            try {
               obfuscationMap = phoneHomeSystem.vsanGetPhoneHomeObfuscationMap(clusterRef);
            } catch (Throwable var35) {
               var10 = var35;
               throw var35;
            } finally {
               if (point != null) {
                  if (var10 != null) {
                     try {
                        point.close();
                     } catch (Throwable var34) {
                        var10.addSuppressed(var34);
                     }
                  } else {
                     point.close();
                  }
               }

            }
         } catch (Exception var37) {
            this.logger.error("Failed to download the obfuscation map data.", var37);
            throw var37;
         }
      } catch (Throwable var38) {
         var7 = var38;
         throw var38;
      } finally {
         if (conn != null) {
            if (var7 != null) {
               try {
                  conn.close();
               } catch (Throwable var33) {
                  var7.addSuppressed(var33);
               }
            } else {
               conn.close();
            }
         }

      }

      response.setCharacterEncoding("UTF-8");
      if ("view".equals(operationType)) {
         response.setContentType("text/plain");
         response.setHeader("Content-Disposition", "inline");
      } else {
         response.setContentType("application/text");
         response.setHeader("Content-Disposition", "attachment; filename=\"obfuscatedMap.txt\"");
      }

      response.setContentLength(obfuscationMap.getBytes().length);
      response.getOutputStream().write(obfuscationMap.getBytes(StandardCharsets.UTF_8));
      response.flushBuffer();
   }
}
