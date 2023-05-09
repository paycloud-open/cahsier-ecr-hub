package com.wiseasy.openapi;


import com.alibaba.fastjson.JSON;
import com.wiseasy.openapi.request.*;
import com.wiseasy.openapi.response.*;
import com.wiseasy.openapi.utils.Constants;

import org.junit.Test;

public class PayTest {

    //实例化客户端
    private static final String APP_RSA_PRIVATE_KEY = "Your app RSA private key";
    private static final String GATEWAY_RSA_PUBLIC_KEY = "RSA public key provided by payment gateway";
    private static final String APP_ID = "Your app app Id";//wzac09fb2b0ad16b28   wz9f2a175d0e0ef632  wz6012822ca2f1as78

    /**
     * Purchase
     */
    @Test
    public void ecrHubCloudPayOrder() {
        OpenApiClient openapiClient = new OpenApiClient(APP_ID, Constants.SANDBOX_GATEWAY_URL, APP_RSA_PRIVATE_KEY, GATEWAY_RSA_PUBLIC_KEY);
        WisehubCloudPayOrderRequest request = new WisehubCloudPayOrderRequest();
        request.setMerchant_no("Your merchant no");
        request.setStore_no("Your store no");
        request.setTerminal_sn("Your terminal sn");
        request.setMessage_receiving_application("WISECASHIER");
        request.setPay_method_category("BANKCARD");
//        request.setPay_method_id("Visa");
        request.setPrice_currency("ZAR");
        request.setOrder_amount(100.0);
        request.setTrans_type(1);
        request.setMerchant_order_no("" + System.currentTimeMillis());
        request.setDescription("test");
        try {
            openapiClient.execute(request);
        } catch (OpenApiException e) {
            return;
        }
    }


    /**
     * Refund
     */
    @Test
    public void ecrHubCloudPayRefund() {
        OpenApiClient openapiClient = new OpenApiClient(APP_ID, Constants.SANDBOX_GATEWAY_URL, APP_RSA_PRIVATE_KEY, GATEWAY_RSA_PUBLIC_KEY);
        WisehubCloudPayOrderRequest request = new WisehubCloudPayOrderRequest();
        request.setMerchant_no("Your merchant no");
        request.setStore_no("Your store no");
        request.setTerminal_sn("Your terminal sn");
        request.setMessage_receiving_application("WISECASHIER");
        request.setPay_method_category("BANKCARD");
        request.setPay_method_id("Visa");
        request.setPrice_currency("ZAR");
        request.setOrder_amount(100.0);
        request.setTrans_type(3);
        request.setMerchant_order_no("" + System.currentTimeMillis());
        request.setOrig_merchant_order_no("1682661311505");
        request.setDescription("iPhone");
        try {
            openapiClient.execute(request);
        } catch (OpenApiException e) {
            return;
        }
    }

    /**
     *Void
     */
    @Test
    public void ecrHubCloudPayVoid() {
        OpenApiClient openapiClient = new OpenApiClient(APP_ID, Constants.SANDBOX_GATEWAY_URL, APP_RSA_PRIVATE_KEY, GATEWAY_RSA_PUBLIC_KEY);
        WisehubCloudPayOrderRequest request = new WisehubCloudPayOrderRequest();
        request.setMerchant_no("Your merchant no");
        request.setStore_no("Your store no");
        request.setTerminal_sn("Your terminal sn");
        request.setMessage_receiving_application("WISECASHIER");
        request.setPay_method_category("BANKCARD");
        request.setPay_method_id("Visa");
        request.setPrice_currency("ZAR");
        request.setOrder_amount(100.0);
        request.setTrans_type(2);
        request.setMerchant_order_no("" + System.currentTimeMillis());
        request.setOrig_merchant_order_no("1682666690514");
        request.setDescription("iPhone");
        try {
            openapiClient.execute(request);
        } catch (OpenApiException e) {
            return;
        }
    }
}
