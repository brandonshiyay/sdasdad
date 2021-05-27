package com.vmware.vsan.client.services.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.proxygen.ts.TsService;
import com.vmware.vim.binding.vim.option.OptionManager;
import com.vmware.vim.binding.vim.option.OptionValue;
import com.vmware.vim.binding.vmodl.ManagedObjectReference;
import com.vmware.vsan.client.services.VsanUiLocalizableException;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcClient;
import com.vmware.vsan.client.sessionmanager.vlsi.client.vc.VcConnection;
import java.util.Iterator;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CeipService {
   public static final String CONSENT_DATA_PROPERTY = "VirtualCenter.DataCollector.ConsentData";
   private Logger logger = LoggerFactory.getLogger(CeipService.class);
   @Autowired
   private VcClient vcClient;

   @TsService
   public boolean getCeipServiceEnabled(ManagedObjectReference clusterRef) {
      OptionValue[] optionValues;
      try {
         VcConnection vcConnection = this.vcClient.getConnection(clusterRef.getServerGuid());
         Throwable var4 = null;

         try {
            OptionManager optionManager = vcConnection.getOptionManager();
            optionValues = optionManager.queryView("VirtualCenter.DataCollector.ConsentData");
         } catch (Throwable var14) {
            var4 = var14;
            throw var14;
         } finally {
            if (vcConnection != null) {
               if (var4 != null) {
                  try {
                     vcConnection.close();
                  } catch (Throwable var13) {
                     var4.addSuppressed(var13);
                  }
               } else {
                  vcConnection.close();
               }
            }

         }
      } catch (Exception var16) {
         throw new VsanUiLocalizableException("vsan.ceip.consent.data.error", var16);
      }

      return this.isConsentAccepted(optionValues);
   }

   public boolean isConsentAccepted(OptionValue[] values) {
      if (ArrayUtils.isEmpty(values)) {
         return false;
      } else {
         OptionValue optionValue = values[0];
         ObjectMapper mapper = new ObjectMapper();

         try {
            JsonNode rootNode = mapper.readTree((String)optionValue.value);
            JsonNode consentConfigNodes = rootNode.get("consentConfigurations");
            if (consentConfigNodes == null) {
               return false;
            } else {
               Iterator consentNodesIterator = consentConfigNodes.elements();

               while(consentNodesIterator.hasNext()) {
                  JsonNode consentNode = (JsonNode)consentNodesIterator.next();
                  JsonNode consentIdNode = consentNode.get("consentId");
                  if (consentIdNode != null && consentIdNode.intValue() == 2) {
                     JsonNode consentEnabledNode = consentNode.get("consentAccepted");
                     if (consentEnabledNode != null) {
                        return consentEnabledNode.booleanValue();
                     }
                     break;
                  }
               }

               return false;
            }
         } catch (Exception var10) {
            this.logger.error("Error parsing the information for CEIP service enabled", var10);
            return true;
         }
      }
   }
}
