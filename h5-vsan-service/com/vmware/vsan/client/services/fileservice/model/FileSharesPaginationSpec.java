package com.vmware.vsan.client.services.fileservice.model;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vim.vsan.binding.vim.vsan.FileShareQuerySpec;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

@TsModel
public class FileSharesPaginationSpec {
   public static final int MAX_SHARE_QUERY_SIZE = 32;
   private static final String NAME_PROPERTY = "config.name";
   private static final String PROTOCOL_PROPERTY = "config.protocol";
   public int pageSize;
   public int pageIndex;
   public String domainName;
   public String shareType;
   public FileShareFilter[] filters;
   public VsanFileShareQueryProperties queryProperties;

   public FileShareQuerySpec toVmodl() {
      FileShareQuerySpec querySpec = new FileShareQuerySpec();
      querySpec.limit = (long)this.pageSize;
      querySpec.pageNumber = this.pageIndex == 0 ? null : (long)this.pageIndex;
      if (StringUtils.isNotBlank(this.shareType)) {
         querySpec.managedBy = new String[]{this.shareType};
      }

      if (StringUtils.isNotBlank(this.domainName)) {
         querySpec.domainName = this.domainName;
      }

      if (!ArrayUtils.isEmpty(this.filters)) {
         FileShareFilter[] var2 = this.filters;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            FileShareFilter filter = var2[var4];
            if ("config.name".equals(filter.key) && ArrayUtils.isNotEmpty(filter.value)) {
               querySpec.names = filter.value;
            }

            if ("config.protocol".equals(filter.key)) {
               VsanFileShareProtocol protocol = VsanFileShareProtocol.parse(filter.value);
               querySpec.protocols = VsanFileShareProtocol.toVmodlProtocols(protocol);
            }
         }
      }

      if (this.queryProperties != null) {
         querySpec.properties = this.queryProperties.toVmodl();
      }

      return querySpec;
   }

   public static FileSharesPaginationSpec getDefaultPaginationSpec() {
      FileSharesPaginationSpec spec = new FileSharesPaginationSpec();
      spec.pageIndex = 1;
      spec.pageSize = 32;
      spec.queryProperties = VsanFileShareQueryProperties.getFileSharesQueryProperties(false, false, false, false);
      return spec;
   }
}
