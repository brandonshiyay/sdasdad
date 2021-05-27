package com.vmware.vsan.client.sessionmanager.vlsi.client.explorer;

import com.vmware.vim.binding.lookup.ServiceRegistration;
import com.vmware.vim.binding.lookup.ServiceRegistration.Filter;
import com.vmware.vim.binding.lookup.ServiceRegistration.Info;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

public abstract class AbstractLsExplorer {
   private final ServiceRegistration lookupService;

   public AbstractLsExplorer(ServiceRegistration lookupService) {
      this.lookupService = lookupService;
   }

   public Object get(UUID uuid) {
      Object result = this.map().get(uuid);
      if (result != null) {
         return result;
      } else {
         throw new IllegalStateException("Service registration not found: " + uuid);
      }
   }

   public Set list() {
      return new HashSet(this.map().values());
   }

   public Map map() {
      List infos = this.getServiceInfos();
      if (CollectionUtils.isEmpty(infos)) {
         return Collections.emptyMap();
      } else {
         Map result = new HashMap();
         Iterator var3 = infos.iterator();

         while(var3.hasNext()) {
            Info info = (Info)var3.next();
            Object registration = this.createRegistration(info);
            this.mapRegistration(registration, result);
         }

         return result;
      }
   }

   protected List getServiceInfos() {
      Filter filter = this.getFilter();
      Info[] infos = this.lookupService.list(filter);
      return ArrayUtils.isEmpty(infos) ? Collections.EMPTY_LIST : Arrays.asList(infos);
   }

   protected abstract Object createRegistration(Info var1);

   protected abstract void mapRegistration(Object var1, Map var2);

   protected abstract Filter getFilter();
}
