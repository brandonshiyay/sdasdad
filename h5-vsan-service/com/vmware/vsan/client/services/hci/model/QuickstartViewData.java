package com.vmware.vsan.client.services.hci.model;

import com.vmware.proxygen.ts.TsModel;

@TsModel
public class QuickstartViewData {
   public String header;
   public String text;
   public boolean showSendFeedbackLink;
   public boolean showCloseQuickstartButton;
   public ConfigCardData[] configurationCards;
   public boolean extendCard;
   public boolean isVsanEnabled;
}
