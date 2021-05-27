package com.vmware.vsphere.client.vsan.data;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public enum VumBaselineRecommendationType {
   latestPatch,
   latestRelease,
   noRecommendation;
}
