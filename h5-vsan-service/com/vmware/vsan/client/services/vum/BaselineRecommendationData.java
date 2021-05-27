package com.vmware.vsan.client.services.vum;

import com.vmware.proxygen.ts.TsModel;
import com.vmware.vsphere.client.vsan.data.VumBaselineRecommendationType;

@TsModel
public class BaselineRecommendationData {
   public VumBaselineRecommendationType clusterRecommendation;
   public VumBaselineRecommendationType vcRecommendation;
}
