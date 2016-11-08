package com.sec.android.app.billing.iap;

import android.os.Bundle;

interface IAPServiceCallback {
	oneway void responseCallback(in Bundle bundle);
}