/*
 * Copyright 2012-2014 One Platform Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onepf.oms.appstore;

import android.app.Activity;

import org.jetbrains.annotations.NotNull;
import org.onepf.oms.Appstore;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.DefaultAppstore;
import org.onepf.oms.OpenIabHelper;

import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import org.onepf.oms.SkuManager;
import org.onepf.oms.appstore.googleUtils.IabException;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.util.CollectionUtils;
import org.onepf.oms.util.Logger;
import org.onepf.oms.util.Utils;

import java.util.concurrent.CountDownLatch;

/**
 * <p>
 * {@link #isPackageInstaller(String)} - there is no known reliable way to understand
 * SamsungApps is installer of Application
 * If you want SamsungApps to be used for purhases specify it in preffered stores by
 * {@link OpenIabHelper#OpenIabHelper(android.content.Context, org.onepf.oms.OpenIabHelper.Options)}  </p>
 * <p/>
 * Supported purchase details
 * <pre>
 * PurchaseInfo(type:inapp): {
 *     "orderId"            :TPMTID20131011RUI0515895,    // Samsung's payment id
 *     "packageName"        :org.onepf.trivialdrive,
 *     "productId"          :sku_gas,
 *     "purchaseTime"       :1381508784209,               // time in millis
 *     "purchaseState"      :0,                           // will be always zero
 *     "developerPayload"   :,                            // available only in Purchase which return in OnIabPurchaseFinishedListener and
 *                                                        // in OnConsumeFinishedListener. In other places it's equal empty string
 *     "token"              :3218a5f30dd56ca459b16155a207e8af7b2cfe80a54f2aed846b2bbbd547c400
 * }
 * </pre>
 *
 * @author Ruslan Sayfutdinov
 * @since 10.10.2013
 */
public class SamsungApps extends DefaultAppstore {

	public static final int SAMSUNG_IAP_MODE_UNSET = -100;
    public static final String SAMSUNG_INSTALLER = "com.sec.android.app.samsungapps";

    private static final int IAP_SIGNATURE_HASHCODE = 0x7a7eaf4b;
	private static final int GAPPS_SIGNATURE_HASHCODE = 0x79998d13;
    public static final String IAP_PACKAGE_NAME = "com.sec.android.iap";
    public static final String IAP_SERVICE_NAME = "com.sec.android.iap.service.iapService";
	public static final String IAP_GALAXY_APPS_PACKAGE_NAME = "com.sec.android.app.samsungapps";


    private AppstoreInAppBillingService billingService;
    private Activity activity;
    private OpenIabHelper.Options options;

    // samsungTestMode = true -> always returns Samsung Apps is installer and billing is available
    public static boolean isSamsungTestMode;

    public static int samsungIapMode = SAMSUNG_IAP_MODE_UNSET;
	public static boolean isSamsungForced = false;

    private Boolean isBillingAvailable;

    public SamsungApps(Activity activity, OpenIabHelper.Options options) {
        this.activity = activity;
        this.options = options;
    }

    @Override
    public boolean isPackageInstaller(String packageName) {
        return Utils.isPackageInstaller(activity, SAMSUNG_INSTALLER) || isSamsungTestMode || isSamsungForced;
    }

    /**
     * @return true if Samsung Apps is installed in the system and a returned inventory contains info about the app's skus
     */
    @Override
    public boolean isBillingAvailable(String packageName) {
        if (isBillingAvailable != null) {
            return isBillingAvailable;
        }

        if (Utils.uiThread()) {
            throw new IllegalStateException("Must no be called from UI thread.");
        }

        boolean iapInstalled = false;
		boolean gAppsInstalled = false;
		PackageManager pm = activity.getPackageManager();
        try {
            pm.getApplicationInfo(IAP_PACKAGE_NAME, PackageManager.GET_META_DATA);
			iapInstalled = true;
        } catch (Exception e) {
            Logger.d("isBillingAvailable() Samsung IAP Service is not installed");
			Logger.d("Attempting to install Samsung IAP...");
			iapInstalled = false;
			//Galaxy apps should be enabled in order to download the Samsung IAP 
			
			try{
				Logger.d("Trying to check if Galaxy Apps is enabled");
				pm.getApplicationInfo(IAP_GALAXY_APPS_PACKAGE_NAME, PackageManager.GET_META_DATA);
				gAppsInstalled = true;
				
				if(!isAppEnabled(IAP_GALAXY_APPS_PACKAGE_NAME)){
					Logger.d("Galaxy Apps: disabled, Galaxy Apps should be enabled to download Samsung IAP");
					showAppDetails(IAP_GALAXY_APPS_PACKAGE_NAME);
					return false;
				}

			}catch(Exception ex){
				gAppsInstalled = false;
				Logger.d("Galaxy Apps is not installed or needs update " + ex.getMessage() );
			}
			
			if(!gAppsInstalled){
				return false;
			}
			try{
				Intent intent = new Intent();
				intent.setData(Uri.parse("samsungapps://ProductDetail/com.sec.android.iap"));
				if (Build.VERSION.SDK_INT >= 12) {
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | 32);
				} else {
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
				}
				activity.startActivity(intent);
				activity.finish();
			}catch(Exception err){
				Logger.d("Error: " + err.getMessage());
				return false;
			}

        }

        if (!iapInstalled) {
            return false;
        }
		
		//Check if Samsung IAP is enabled
        if(!isAppEnabled(IAP_PACKAGE_NAME)){
            Logger.d("Samsung In-App Purchase is disabled");
			showAppDetails(IAP_PACKAGE_NAME);
            return false;
        }

        if (isSamsungTestMode || isSamsungForced) {
            Logger.d("isBillingAvailable() billing is supported in test mode.");
            isBillingAvailable = true;
            return true;
        }

        isBillingAvailable = false;
        final CountDownLatch mainLatch = new CountDownLatch(1);
        getInAppBillingService().startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(@NotNull final IabResult result) {
                if (result.isSuccess()) {
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                Inventory inventory = getInAppBillingService()
                                        .queryInventory(true, SkuManager.getInstance()
                                                .getAllStoreSkus(OpenIabHelper.NAME_SAMSUNG), null);
                                if (inventory != null && !CollectionUtils.isEmpty(inventory.getSkuMap())) {
                                    isBillingAvailable = true;
                                }
                            } catch (IabException e) {
                                Logger.e("isBillingAvailable() failed", e);
                            } finally {
                                getInAppBillingService().dispose();
                                mainLatch.countDown();
                            }
                        }
                    }).start();
                } else {
                    getInAppBillingService().dispose();
                    mainLatch.countDown();
                }
            }
        });

        try {
            mainLatch.await();
        } catch (InterruptedException e) {
            Logger.e("isBillingAvailable() interrupted", e);
        }

        return isBillingAvailable;
    }

    @Override
    public int getPackageVersion(String packageName) {
        return Appstore.PACKAGE_VERSION_UNDEFINED;
    }

    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (billingService == null) {
            billingService = new SamsungAppsBillingService(activity, options);
        }
        return billingService;
    }

    @Override
    public String getAppstoreName() {
        return OpenIabHelper.NAME_SAMSUNG;
    }

	/**
	 * Validates sku 
	 * Accepts two formats <numeric groupId>/<itemId> or <itemId>
	**/
    public static void checkSku(@NotNull String sku) {
        String[] skuParts = sku.split("/");
        String groupId="";
        String itemId="";
        if(skuParts.length == 1){
            itemId = skuParts[0];
        }else if (skuParts.length==2){
            groupId = skuParts[0];
            itemId = skuParts[1];
            if (TextUtils.isEmpty(groupId) || !TextUtils.isDigitsOnly(groupId)) {
                throw new SamsungSkuFormatException("Samsung SKU must contain numeric ITEM_GROUP_ID.");
            }
        }else{
            throw new SamsungSkuFormatException("Samsung SKU must contain ITEM_ID and/or ITEM_GROUP_ID.");
		}
        if (TextUtils.isEmpty(itemId)) {
            throw new SamsungSkuFormatException("Samsung SKU must contain ITEM_ID.");
        }
    }
	
	/**
    * Checks if app is enabled
    **/
    private boolean isAppEnabled(String appname){
		if(activity.getPackageManager()!=null){
			try {
				ApplicationInfo appToCheck = 
					activity.getPackageManager().getApplicationInfo(appname,0);
				boolean appStatus = appToCheck.enabled;
				
				return appStatus;
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
				return false;
			}	
		}
		return false;
    }
	
	/**
	* Redirect to application detail. Users can enable the application there.
    **/
    public void showAppDetails(String appname) {
        Intent intent = new Intent();
		
        if (Build.VERSION.SDK_INT < 9) {
            String appPkgName="";
            if(Build.VERSION.SDK_INT == 8){
                appPkgName = "pkg";
            }else{
                appPkgName = "com.android.settings.ApplicationPkgName";
            }
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            intent.putExtra(appPkgName, appname);
        } else {
            intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + appname));
        }

        //Get activity public name
        ApplicationInfo appToCheck = null;
		if(activity.getPackageManager()!=null){
			try {
				appToCheck =
						activity.getPackageManager().getApplicationInfo(appname,0);

			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}
			// Start Activity
			final String appPublicName = (String)((appToCheck != null) ? activity.getPackageManager().getApplicationLabel(appToCheck) : appname);
			Logger.d(appname+"/"+appPublicName + " is disabled, redirecting to Application Info");
			activity.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(activity, "Please enable " + appPublicName + ". Please restart the app for changes to take effect.", Toast.LENGTH_LONG).show();
				}
			});	
		}
        activity.startActivity(intent);
    }
}
