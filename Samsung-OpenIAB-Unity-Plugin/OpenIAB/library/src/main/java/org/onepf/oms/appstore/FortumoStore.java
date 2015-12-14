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

import android.content.Context;
import android.os.Build;

import org.jetbrains.annotations.NotNull;
import org.onepf.oms.Appstore;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.DefaultAppstore;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabException;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.util.Logger;

import java.util.concurrent.CountDownLatch;

/**
 * Fortumo, an international mobile payment provider, is not actually an app store.
 * This class was made to provide in-app purchasing compatibility with other, "real", stores.
 *
 * @author akarimova@onepf.org
 * @since 23.12.13
 */
public class FortumoStore extends DefaultAppstore {
    private boolean isNookDevice;
    private Boolean isBillingAvailable;

    /**
     * Contains information about all in-app products
     */
    public static final String IN_APP_PRODUCTS_FILE_NAME = "inapps_products.xml";

    /**
     * Contains additional information about Fortumo services
     */
    public static final String FORTUMO_DETAILS_FILE_NAME = "fortumo_inapps_details.xml";

    private Context context;
    private FortumoBillingService billingService;

    public FortumoStore(@NotNull Context context) {
        this.context = context.getApplicationContext();
        isNookDevice = isNookDevice();
    }

    /**
     * Fortumo doesn't have an app store. It can't be an installer.
     *
     * @return false
     */
    @Override
    public boolean isPackageInstaller(String packageName) {
        return false;
    }

    @Override
    public boolean isBillingAvailable(String packageName) {
        if (isBillingAvailable != null) {
            return isBillingAvailable;
        }
        billingService = (FortumoBillingService) getInAppBillingService();
        isBillingAvailable = billingService.setupBilling(isNookDevice);
        Logger.d("isBillingAvailable: ", isBillingAvailable);
        return isBillingAvailable;
    }

    @Override
    public int getPackageVersion(String packageName) {
        return Appstore.PACKAGE_VERSION_UNDEFINED;
    }

    @Override
    public String getAppstoreName() {
        return OpenIabHelper.NAME_FORTUMO;
    }

    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (billingService == null) {
            billingService = new FortumoBillingService(context, isNookDevice);
        }
        return billingService;
    }

    //todo check for different devices
    private static boolean isNookDevice() {
        String brand = Build.BRAND;
        String manufacturer = System.getProperty("ro.nook.manufacturer");
        return ((brand != null && brand.equalsIgnoreCase("nook")) ||
                manufacturer != null && manufacturer.equalsIgnoreCase("nook"));
    }

    //todo rename the method
    public static FortumoStore initFortumoStore(@NotNull Context context, final boolean checkInventory) {
        final FortumoStore[] storeToReturn = {null};
        final FortumoStore fortumoStore = new FortumoStore(context);
        if (fortumoStore.isBillingAvailable(context.getPackageName())) {
            final CountDownLatch latch = new CountDownLatch(1);
            fortumoStore.getInAppBillingService().startSetup(new IabHelper.OnIabSetupFinishedListener() {
                @Override
                public void onIabSetupFinished(@NotNull IabResult setupResult) {
                    if (setupResult.isSuccess()) {
                        if (checkInventory) {
                            try {
                                final Inventory inventory = fortumoStore.getInAppBillingService().queryInventory(false, null, null);
                                if (!inventory.getAllPurchases().isEmpty()) {
                                    storeToReturn[0] = fortumoStore;
                                } else {
                                    Logger.d("Purchases not found");
                                }
                            } catch (IabException e) {
                                Logger.e("Error while requesting purchases", e);
                            }
                        } else {
                            storeToReturn[0] = fortumoStore;
                        }
                    }
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Logger.e("Setup was interrupted", e);
            }
        }
        return storeToReturn[0];
    }

}
