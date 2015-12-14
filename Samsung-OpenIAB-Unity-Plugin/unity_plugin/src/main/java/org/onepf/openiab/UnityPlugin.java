/*******************************************************************************
 * Copyright 2012-2014 One Platform Foundation
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 ******************************************************************************/

package org.onepf.openiab;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Debug;
import android.util.Log;
import com.unity3d.player.UnityPlayer;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.SkuManager;
import org.onepf.oms.appstore.googleUtils.*;
import org.onepf.oms.util.Logger;
import org.onepf.oms.appstore.SamsungApps;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Native java part of the Unity plugin
 * All methods are called from the Unity side via JNI
 */
public class UnityPlugin {

    public static final String TAG = "OpenIAB-UnityPlugin";
    private static final String EVENT_MANAGER = "OpenIABEventManager"; /**< Name of the event handler object in Unity */
    private static final String MAP_SKU_FAILED_CALLBACK = "OnMapSkuFailed";
    private static final String BILLING_SUPPORTED_CALLBACK = "OnBillingSupported";
    private static final String BILLING_NOT_SUPPORTED_CALLBACK = "OnBillingNotSupported";
    private static final String QUERY_INVENTORY_SUCCEEDED_CALLBACK = "OnQueryInventorySucceeded";
    private static final String QUERY_INVENTORY_FAILED_CALLBACK = "OnQueryInventoryFailed";
	private static final String QUERY_SKU_LIST_SUCCEEDED_CALLBACK = "OnQuerySkuListSucceeded";
    private static final String QUERY_SKU_LIST_FAILED_CALLBACK = "OnQuerySkuListFailed";
    private static final String PURCHASE_SUCCEEDED_CALLBACK = "OnPurchaseSucceeded";
    private static final String PURCHASE_FAILED_CALLBACK = "OnPurchaseFailed";
    private static final String CONSUME_PURCHASE_SUCCEEDED_CALLBACK = "OnConsumePurchaseSucceeded";
    private static final String CONSUME_PURCHASE_FAILED_CALLBACK = "OnConsumePurchaseFailed";

    public static final int RC_REQUEST = 10001; /**< (arbitrary) request code for the purchase flow */
    public static boolean sendRequest = false;

    private static UnityPlugin _instance;
    private OpenIabHelper _helper;

    public OpenIabHelper getHelper() {
        return _helper;
    }

    public IabHelper.OnIabPurchaseFinishedListener getPurchaseFinishedListener() {
        return _purchaseFinishedListener;
    }
	/**
	 * Return _setupFinishedListener
	**/
	public IabHelper.OnIabSetupFinishedListener getSetupBridgeFinishedListener() {
        return _setupFinishedListener;
    }
	
	/**
	 * Return _querySkuListFinishedListener
	**/
	public IabHelper.QuerySkuListFinishedListener getQuerySkuListFinishedListener() {
        return _querySkuListFinishedListener;
    }


    public static UnityPlugin instance() {
        if (_instance == null) {
            _instance = new UnityPlugin();
        }
        return _instance;
    }

    /**
     * @deprecated Use {@link org.onepf.oms.SkuManager#mapSku(String, String, String)}
     *
     * @param sku
     * @param storeName
     * @param storeSku
     */
    public void mapSku(String sku, String storeName, String storeSku) {
        try {
            SkuManager.getInstance().mapSku(sku, storeName, storeSku);
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            UnityPlayer.UnitySendMessage(EVENT_MANAGER, MAP_SKU_FAILED_CALLBACK, e.toString());
        }
    }

    public void init(final HashMap<String, String> storeKeys) {
        OpenIabHelper.Options options = new OpenIabHelper.Options.Builder()
                .addStoreKeys(storeKeys)
                .build();
        initWithOptions(options);
    }

    public void initWithOptions(final OpenIabHelper.Options options) {
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @Override
			public void run() {
                _helper = new OpenIabHelper(UnityPlayer.currentActivity, options);
                createBroadcasts();

                // Start setup. This is asynchronous and the specified listener
                // will be called once setup completes.
                Log.d(TAG, "Starting setup.");

				sendRequest = false;
				Intent i = new Intent(UnityPlayer.currentActivity, UnityProxyActivity.class);
				Logger.d("Starting UnityProxyActivity for setup");
				UnityPlayer.currentActivity.startActivity(i);
            }
        });
    }

    public void unbindService() {
        if (_helper != null) {
            _helper.dispose();
            _helper = null;
        }
        destroyBroadcasts();
    }

    public boolean areSubscriptionsSupported() {
        return _helper.subscriptionsSupported();
    }

    public void queryInventory(final Object... extraParams) {
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _helper.queryInventoryAsync(_queryInventoryListener, convertParams(extraParams));
            }
        });
    }

    public void queryInventory(final String[] itemSkus, final Object... extraParams) {
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _helper.queryInventoryAsync(true, Arrays.asList(itemSkus), _queryInventoryListener, convertParams(extraParams));
            }
        });
    }

    public void queryInventory(final String[] itemSkus, final String[] subsSkus, final Object... extraParams) {
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _helper.queryInventoryAsync(true, Arrays.asList(itemSkus), Arrays.asList(subsSkus), _queryInventoryListener, convertParams(extraParams));
            }
        });
    }
	
	//TODO
	public void querySkuList() {
		Log.d(TAG, "querySkuList() in java called");
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _helper.querySkuListAsync(_querySkuListFinishedListener);
            }
        });
    }
	
	//TODO
	public void querySkuList(final String skuType) {
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _helper.querySkuListAsync(_querySkuListFinishedListener, skuType);
            }
        });
    }
	
	/**
	 * Converts the extraParams to a List of Objects
	**/
	private List<Object> convertParams(Object... extraParams){
		final List<Object> params;
		if(null!=extraParams && extraParams.length > 0){
			params = Arrays.asList(extraParams);
		}else{
			params = new ArrayList<Object>();
			params.add("false");
		}
		return params;
	}

    public void purchaseProduct(final String sku, final String developerPayload) {
        startProxyPurchaseActivity(sku, true, developerPayload);
    }

    public void purchaseSubscription(final String sku, final String developerPayload) {
        startProxyPurchaseActivity(sku, false, developerPayload);
    }

    public void consumeProduct(final String json) {
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    String appstoreName = jsonObject.getString("appstoreName");
                    String jsonPurchaseInfo = jsonObject.getString("originalJson");
					String packageName = jsonObject.getString("packageName");
					String token = jsonObject.getString("token");
					String sku = jsonObject.getString("sku");
                    Purchase p;
                    if (jsonPurchaseInfo == null || jsonPurchaseInfo.equals("") || jsonPurchaseInfo.equals("null")) {
                        UnityPlayer.UnitySendMessage(EVENT_MANAGER, CONSUME_PURCHASE_FAILED_CALLBACK, "Original json is invalid: " + json);
                        return;
                    } else {
                        String itemType = jsonObject.getString("itemType");
                        String signature = jsonObject.getString("signature");
                        p = new Purchase(itemType, jsonPurchaseInfo, signature, appstoreName);
                    }
					p.setPackageName(packageName);
					p.setToken(token);
					if(p.getSku() == null || p.getSku().isEmpty()){
						p.setSku(sku);					
					}

                    _helper.consumeAsync(p, _consumeFinishedListener);
                } catch (org.json.JSONException e) {
                    e.printStackTrace();
                    UnityPlayer.UnitySendMessage(EVENT_MANAGER, CONSUME_PURCHASE_FAILED_CALLBACK, "Invalid json: " + json + ". " + e);
                }
            }
        });
    }

    private void startProxyPurchaseActivity(String sku, boolean inapp, String developerPayload) {

        if (UnityPlugin.instance().getHelper() == null) {
            Log.e(UnityPlugin.TAG, "OpenIAB UnityPlugin not initialized!");
            return;
        }

        sendRequest = true;
        Intent i = new Intent(UnityPlayer.currentActivity, UnityProxyActivity.class);
        i.putExtra("sku", sku);
        i.putExtra("inapp", inapp);
        i.putExtra("developerPayload", developerPayload);

        // Launch proxy purchase Activity - it will close itself down when we have a response
        UnityPlayer.currentActivity.startActivity(i);
    }

    /**
     * Listener that's called when we finish querying the items and subscriptions we own
     */
    IabHelper.QueryInventoryFinishedListener _queryInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");
            if (result.isFailure()) {
                UnityPlayer.UnitySendMessage(EVENT_MANAGER, QUERY_INVENTORY_FAILED_CALLBACK, result.getMessage());
                return;
            }

            Log.d(TAG, "Query inventory was successful.");
            String jsonInventory;
            try {
                jsonInventory = inventoryToJson(inventory);
            } catch (JSONException e) {
                UnityPlayer.UnitySendMessage(EVENT_MANAGER, QUERY_INVENTORY_FAILED_CALLBACK, "Couldn't serialize the inventory");
                return;
            }
            UnityPlayer.UnitySendMessage(EVENT_MANAGER, QUERY_INVENTORY_SUCCEEDED_CALLBACK, jsonInventory);
        }
    };
	
	/**
	 * Callback when query sku list is complete
     */
	 IabHelper.QuerySkuListFinishedListener _querySkuListFinishedListener = new IabHelper.QuerySkuListFinishedListener() {
		public void onQuerySkuListFinished(IabResult result, List<SkuDetails> skuDetails, String type) {
			
			Log.d(TAG, "Query Sku List finished.");
	
			if (result.isFailure()) {
				// Oh noes, there was a problem.
				Log.e(TAG, "Problem setting up in-app billing: " + result);
				UnityPlayer.UnitySendMessage(EVENT_MANAGER, QUERY_SKU_LIST_FAILED_CALLBACK, result.getMessage());
				return;
			}
			
			Log.d(TAG, "Query Sku list successful.");
			String jsonSkuDetails;
				try {
					JSONStringer json = new JSONStringer().object();
					json.key("skuList").array();
					for (SkuDetails skuItem: skuDetails) {
						json.array();
						json.value(skuDetailsToJson(skuItem));
						json.endArray();
					}
					json.endArray();
					json.endObject();
					jsonSkuDetails = json.toString();
				} catch (JSONException e) {
					UnityPlayer.UnitySendMessage(EVENT_MANAGER, QUERY_SKU_LIST_FAILED_CALLBACK, "Couldn't serialize the skuDetails");
					return;
				}
			
          
			UnityPlayer.UnitySendMessage(EVENT_MANAGER, QUERY_SKU_LIST_SUCCEEDED_CALLBACK,  jsonSkuDetails);
		
		}
	 };

    /**
     * Callback for when a purchase is finished
     */
    IabHelper.OnIabPurchaseFinishedListener _purchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            UnityPlayer.currentActivity.sendBroadcast(new Intent(UnityProxyActivity.ACTION_FINISH));
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            if (result.isFailure()) {
                Log.e(TAG, "Error purchasing: " + result);
                UnityPlayer.UnitySendMessage(EVENT_MANAGER, PURCHASE_FAILED_CALLBACK, result.getResponse()+"|"+result.getMessage());
                return;
            }
            Log.d(TAG, "Purchase successful.");
            String jsonPurchase;
            try {
                jsonPurchase = purchaseToJson(purchase);
            } catch (JSONException e) {
                UnityPlayer.UnitySendMessage(EVENT_MANAGER, PURCHASE_FAILED_CALLBACK, "-1|Couldn't serialize the purchase");
                return;
            }
            UnityPlayer.UnitySendMessage(EVENT_MANAGER, PURCHASE_SUCCEEDED_CALLBACK, jsonPurchase);
        }
    };

	/**
	 * Callback when setup is complete
     */
	 IabHelper.OnIabSetupFinishedListener _setupFinishedListener = new IabHelper.OnIabSetupFinishedListener() {
		public void onIabSetupFinished(IabResult result){
			UnityPlayer.currentActivity.sendBroadcast(new Intent(UnityProxyActivity.ACTION_FINISH));
			Log.d(TAG, "Setup finished.");
	
			if (result.isFailure()) {
				// Oh noes, there was a problem.
				Log.e(TAG, "Problem setting up in-app billing: " + result);
				UnityPlayer.UnitySendMessage(EVENT_MANAGER, BILLING_NOT_SUPPORTED_CALLBACK, result.getMessage());
				return;
			}

			// Hooray, IAB is fully set up
			Log.d(TAG, "Setup successful.");
			UnityPlayer.UnitySendMessage(EVENT_MANAGER, BILLING_SUPPORTED_CALLBACK, "");
		
		}
	 };

    /**
     * Callback for when a consumption is complete
     */
    IabHelper.OnConsumeFinishedListener _consumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

            purchase.setSku(SkuManager.getInstance().getSku(purchase.getAppstoreName(), purchase.getSku()));

            if (result.isFailure()) {
                Log.e(TAG, "Error while consuming: " + result);
                UnityPlayer.UnitySendMessage(EVENT_MANAGER, CONSUME_PURCHASE_FAILED_CALLBACK, result.getMessage());
                return;
            }
            Log.d(TAG, "Consumption successful. Provisioning.");
            String jsonPurchase;
            try {
                jsonPurchase = purchaseToJson(purchase);
            } catch (JSONException e) {
                UnityPlayer.UnitySendMessage(EVENT_MANAGER, CONSUME_PURCHASE_FAILED_CALLBACK, "Couldn't serialize the purchase");
                return;
            }
            UnityPlayer.UnitySendMessage(EVENT_MANAGER, CONSUME_PURCHASE_SUCCEEDED_CALLBACK, jsonPurchase);
        }
    };


    private String inventoryToJson(Inventory inventory) throws JSONException {
        JSONStringer json = new JSONStringer().object();

        json.key("purchaseMap").array();
        for (Map.Entry<String, Purchase> entry : inventory.getPurchaseMap().entrySet()) {
            json.array();
            json.value(entry.getKey());
            json.value(purchaseToJson(entry.getValue()));
            json.endArray();
        }
        json.endArray();

        json.key("skuMap").array();
        for (Map.Entry<String, SkuDetails> entry : inventory.getSkuMap().entrySet()) {
            json.array();
            json.value(entry.getKey());
            json.value(skuDetailsToJson(entry.getValue()));
            json.endArray();
        }
        json.endArray();

        json.endObject();
        return json.toString();
    }

    /**
     * Serialize purchase data to json
     * @param purchase purchase data
     * @return json string
     * @throws JSONException
     */
    private String purchaseToJson(Purchase purchase) throws JSONException {
        return new JSONStringer().object()
                .key("itemType").value(purchase.getItemType())
                .key("orderId").value(purchase.getOrderId())
                .key("packageName").value(purchase.getPackageName())
                .key("sku").value(purchase.getSku())
                .key("purchaseTime").value(purchase.getPurchaseTime())
                .key("purchaseState").value(purchase.getPurchaseState())
                .key("developerPayload").value(purchase.getDeveloperPayload())
                .key("token").value(purchase.getToken())
                .key("originalJson").value(purchase.getOriginalJson())
                .key("signature").value(purchase.getSignature())
                .key("appstoreName").value(purchase.getAppstoreName())
                .endObject().toString();
    }

    /**
     * Serialize sku details data to json
     * @param skuDetails sku details data
     * @return json string
     * @throws JSONException
     */
    private String skuDetailsToJson(SkuDetails skuDetails) throws JSONException {
        return new JSONStringer().object()
                .key("itemType").value(skuDetails.getItemType())
                .key("sku").value(skuDetails.getSku())
                .key("type").value(skuDetails.getType())
                .key("price").value(skuDetails.getPrice())
                .key("title").value(skuDetails.getTitle())
                .key("description").value(skuDetails.getDescription())
                .key("json").value(skuDetails.getJson())
                .endObject().toString();
    }

    private void createBroadcasts() {
        Log.d(TAG, "createBroadcasts");
        IntentFilter filter = new IntentFilter(YANDEX_STORE_ACTION_PURCHASE_STATE_CHANGED);
        UnityPlayer.currentActivity.registerReceiver(_billingReceiver, filter);
    }

    private void destroyBroadcasts() {
        Log.d(TAG, "destroyBroadcasts");
        try {
            UnityPlayer.currentActivity.unregisterReceiver(_billingReceiver);
        } catch (Exception ex) {
            Log.d(TAG, "destroyBroadcasts exception:\n" + ex.getMessage());
        }
    }

    // Yandex specific
    public static final String YANDEX_STORE_SERVICE = "com.yandex.store.service";
    public static final String YANDEX_STORE_ACTION_PURCHASE_STATE_CHANGED = YANDEX_STORE_SERVICE + ".PURCHASE_STATE_CHANGED";

    private BroadcastReceiver _billingReceiver = new BroadcastReceiver() {
        private static final String TAG = "YandexBillingReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive intent: " + intent);

            if (YANDEX_STORE_ACTION_PURCHASE_STATE_CHANGED.equals(action)) {
                purchaseStateChanged(intent);
            }
        }

        private void purchaseStateChanged(Intent data) {
            Log.d(TAG, "purchaseStateChanged intent: " + data);
            _helper.handleActivityResult(RC_REQUEST, Activity.RESULT_OK, data);
        }
    };
	// TODO
    public void samsungIapMode(int mode) {
        SamsungApps.samsungIapMode = mode;
    }
	// TODO
	public void samsungForcedMode(boolean enabled){
		SamsungApps.isSamsungForced = enabled;
	}

    // Additional logging
    public boolean isDebugLog() {
        return Logger.isLoggable();
    }

    public void enableDebugLogging(boolean enabled) {
        Logger.setLoggable(enabled);
    }

    public void enableDebugLogging(boolean enabled, String tag) {
        Logger.setLoggable(enabled);
    }
}
