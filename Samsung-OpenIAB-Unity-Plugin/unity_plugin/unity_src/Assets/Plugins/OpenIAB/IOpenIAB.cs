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

namespace OnePF
{
    /**
     * Implement this to create billing service for new platform
     */ 
    public interface IOpenIAB
    {
        void init(Options options);
        void mapSku(string sku, string storeName, string storeSku);
        void unbindService();
        bool areSubscriptionsSupported();
		/** 
		 *  Added extraParams parameter
		 * 	this allows additional parameters if needed.
		 *  Function can still be used in legacy code.
		 **/
        void queryInventory(params object[] extraParams);
        void queryInventory(string[] inAppSkus, params object[] extraParams);
		//TODO
		void querySkuList();
		void querySkuList(string skuType);
		
        void purchaseProduct(string sku, string developerPayload = "");
        void purchaseSubscription(string sku, string developerPayload = "");
        void consumeProduct(Purchase purchase);
        void restoreTransactions();
		/**
		 * Force Samsung Test
         * @param enabled if samsung is forced
         */ 
		//TODO
		void samsungIapMode(int mode);
		void samsungForcedMode(bool enabled);

        bool isDebugLog();
        void enableDebugLogging(bool enabled);
        void enableDebugLogging(bool enabled, string tag);
    }
}