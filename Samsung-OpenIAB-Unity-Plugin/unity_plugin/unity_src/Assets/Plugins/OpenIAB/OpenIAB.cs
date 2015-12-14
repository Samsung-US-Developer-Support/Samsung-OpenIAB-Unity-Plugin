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

using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using System;

namespace OnePF
{
    /** 
     * Main class 
     */
    public class OpenIAB
    {
        public static GameObject EventManager { get { return GameObject.Find(typeof(OpenIABEventManager).ToString()); } }

        static IOpenIAB _billing;

        /**
         * Static constructor 
         * Creates billing instance
         */
        static OpenIAB()
        {
#if UNITY_ANDROID
			_billing = new OpenIAB_Android();
            Debug.Log("********** Android OpenIAB plugin initialized **********");
#elif UNITY_IOS
			_billing = new OpenIAB_iOS();
            Debug.Log("********** iOS OpenIAB plugin initialized **********");
#elif UNITY_WP8
            _billing = new OpenIAB_WP8();
            Debug.Log("********** WP8 OpenIAB plugin initialized **********");
#else
			Debug.LogError("OpenIAB billing currently not supported on this platform. Sorry.");
#endif
        }

        /**
         * Must be only called before init
         * @param sku product ID
         * @param storeName name of the store
         * @param storeSku product ID in the store
         */ 
        public static void mapSku(string sku, string storeName, string storeSku)
        {
            _billing.mapSku(sku, storeName, storeSku);
        }

        /**
         * Starts up the billing service. This will also check to see if in app billing is supported and fire the appropriate event
         * @param options library options instance
         */
        public static void init(Options options)
        {
            _billing.init(options);
        }

        /**
         * Unbinds and shuts down the billing service
         */ 
        public static void unbindService()
        {
            _billing.unbindService();
        }

        /**
         * Checks if subscriptions are supported. Currently used only on Android
         * @return true if subscriptions are supported on the device
         */
        public static bool areSubscriptionsSupported()
        {
            return _billing.areSubscriptionsSupported();
        }

        /**
         * Sends a request to get all completed purchases
		 *  Added extraParams parameter to match function signature 
		 * 	@param extraParams this allows additional parameters if needed. Function will still work in legacy code.
         */ 
        public static void queryInventory(params object[] extraParams)
        {
            _billing.queryInventory(extraParams);
        }

        /**
         * Sends a request to get all completed purchases and specified skus information
         * @param skus product IDs
		 *  Added extraParams parameter to match function signature 
		 * 	@param extraParams this allows additional parameters if needed. Function will still work in legacy code.
         */ 

        public static void queryInventory(string[] skus, params object[] extraParams)
        {
            _billing.queryInventory(skus, extraParams);
        }

        /**
         * Purchases the product with the given sku and developerPayload
         * @param product ID
         * @param developerPayload payload to verify transaction
         */
        public static void purchaseProduct(string sku, string developerPayload = "")
        {
            _billing.purchaseProduct(sku, developerPayload);
        }

        /**
         * Purchases the subscription with the given sku and developerPayload
         * @param sku product ID
         * @param developerPayload payload to verify transaction
         */
        public static void purchaseSubscription(string sku, string developerPayload = "")
        {
            _billing.purchaseSubscription(sku, developerPayload);
        }

        /**
         * Sends out a request to consume the product
         * @param purchase purchase data holder
         */ 
        public static void consumeProduct(Purchase purchase)
        {
            _billing.consumeProduct(purchase);
        }

        /**
         * Restore purchased items. iOS AppStore requirement
         */ 
        public static void restoreTransactions()
        {
            _billing.restoreTransactions();
        }
		
        /**
		 * Force Samsung Test
         * @param enabled if samsung is forced
         */ 
		//TODO
        public static void samsungIapMode(int mode)
        {
            _billing.samsungIapMode(mode);
        }
		
		//TODO
		public static void samsungForcedMode(bool enabled){
			_billing.samsungForcedMode(enabled);
		}

        /**
         * Is verbose logging enabled
         * @return true if logging is enabled
         */ 
		public static bool isDebugLog()
        {
            return _billing.isDebugLog();
        }

        /**
         * Get more debug information
         * @param enabled if logging is enabled
         */ 
        public static void enableDebugLogging(bool enabled)
        {
            _billing.enableDebugLogging(enabled);
        }

        /**
         * Get more debug information
         * @param enabled if logging is enabled
         * @param tag Android log tag
         */
        public static void enableDebugLogging(bool enabled, string tag)
        {
            _billing.enableDebugLogging(enabled, tag);
        }
		
		
		/**
         * TODO
         */ 
        public static void querySkuList()
        {
            _billing.querySkuList();
        }

        /**
         * TODO
         */
        public static void querySkuList(string skuType)
        {
            _billing.querySkuList(skuType);
        }
    }
}
