package com.sec.android.app.billing.iap;

import com.sec.android.app.billing.iap.IAPServiceCallback;

interface IAPConnector {

	boolean requestCmd(IAPServiceCallback callback, in Bundle bundle);
	
	boolean unregisterCallback(IAPServiceCallback callback);
	
	Bundle init(int mode);
	
	Bundle getItemList(int mode, String packageName, String itemGroupId, int startNum, int endNum, String itemType);
	
	Bundle getItemsInbox(String packageName, String itemGroupId, int startNum, int endNum, String startDate, String endDate);
	
	Bundle getItemsInbox2(String packageName, String itemIds);
}