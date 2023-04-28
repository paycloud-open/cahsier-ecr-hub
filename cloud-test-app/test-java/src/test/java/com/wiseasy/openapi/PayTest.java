package com.wiseasy.openapi;


import com.alibaba.fastjson.JSON;
import com.wiseasy.openapi.request.*;
import com.wiseasy.openapi.response.*;
import com.wiseasy.openapi.utils.Constants;

import org.junit.Test;

public class PayTest {

    //实例化客户端
    private static final String APP_RSA_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCJ9N8YTm0wEuBye5zSiZKncNIg6WBvTHjg4dYc9BH0zR+LeHd/jVo+InrMiIFgblnf7eYJ/wlPG5cSMMSyQMHj/7o0prc+VZsqi1mjX88InK4qDl6qFBFrg4dRrCAwILLVOP/ppUU3souMmnYnvOSUyuLBnhSno8qTpFSbcFAkPaHRmUmcTtVPWuNNbPaZVlNn81imIIVw8LKzOzCz9CNNcikkvbUlZp/cZ0Fl3s6icqOCjRvamg8KLZJs9D52S20X60ynreEIn8g6lr77byOGCCRwpdMl9Cl89WmbC3A3RKh7GRPtjyx3B5aQqE3sK5sNT8H8/EqcnQe8QoVBs7x1AgMBAAECggEAbE6vB+oqlt97DuY1TKVtWb+dePFAIKEtFYC4FKsZndOcvGaripxzCO0Q85sH16lLLh8bxyVPLag/hqx7AGcO0e1nRwbMPkf/NfuJOFZzuBMqOSJm96ghtQLiLiCwdJh3Ticd41U5bmziWlS6BqCp5JcUR2XQWXyiAh+1vQMEKC56CNPxr7imXITS7BYdY0qiGiOANcoEJhfQXn4BjzEm2FJufdlHW6/IeRYZ874HF3/7aUOyhbnapxYHU9PzicMc9XwerMcXMGvOfTUnCtRVONLn7jiknbpwdZ1d8PoItUZuaXdAyY6wFHZF+KvyrEoOV0eWRYzPgku2oSMD7IckUQKBgQDy+etogil+3F8S/qW1WiG6l2tLx5cwq3Ak9weN1zOo8A43obtXRxW2b4fDeol8y6LmYaIW9kKmK0qHEx5LoUSH7VN3FigNpFSa6muVJUuY+ob4H3Je+h3PaqbwhVFzjDofuIolPiJZW64LaFPZcxxD9vM2tZ6txLeMUmGftPoVSwKBgQCRWeNxbvTs8U5UknrdDZHUwXUmy5Qg2LUzxDXdbZXgolMh0AgDShcEFvaF+G3OFHj9vovytt0HtEXyC9LEc6w/a08mtGHiAJbjyjrJCHRNrcFflEm1xseIqXQXDj5Tw05XkAnlWN+r0w3Ix45+GrNhMOGfDkVyQeYI5Jsxxt0dPwKBgFJwcWr4HtQoOSnctKSffCovDfycL7QXtukT18BMb/611F0TxtiKCdfoZ4vvm454GUFJhxF7ZIm0zoid9/15LiNgZp1VKynVw878Epx8FvZEql6tbMTE4DBr41BgK46k2WPB3T1do5HmBVthfnGdGM4Gj+bUII6c3BoEKZNieCeZAoGAQ3rx5wXWW/KjpQvkUqAsJhQyqXI2MRGq/n+Hamen/4QdCEOmlLBfAx0OEqCFifljOpquKl7POvZsyrTGg0IYo9DUDGoOT3hqlRKcPBzasf2LGy6jEetZU48oQFPyh7zSsEBE999M6F6xtZdABjerM+IXvVpIz4TcoSBRFMj4es0CgYEAiNCbs2YPKBFpJq9EYsSsx0GwXAItXFAM71TogWs/8InGNq0PYRZTn9Lq6mFEyLkFql1zWQkS7CG3uDPBV+V2G4MbvoZeFgiQmly7uMQJwbEiKjJBsOzkYY2ZhFYLjGUpGer2U82XeEU6F/Vh4FLkVEE2iB3nPpVQs0qPZxfFZvM=";
    private static final String GATEWAY_RSA_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2m4nkQKyQAxJc8VVsz/L6qVbtDWRTBolUK8Dwhi9wH6aygA6363PVNEPM8eRI5W19ssCyfdtNFy6DRAureoYV053ETPUefEA5bHDOQnjbb9PuNEfT651v8cqwEaTptaxj2zujsWI8Ad3R50EyQHsskQWms/gv2aB36XUM4vyOIk4P1f3dxtqigH0YROEYiuwFFqsyJuNSjJzNbCmfgqlQv/+pE/pOV9MIQe0CAdD26JF10QpSssEwKgvKvnXPUynVu09cjSEipev5cLJSApKSDZxrRjSFBXrh6nzg8JK05ehkI8wdsryRUneh0PGN0PgYLP/wjKiqlgTJaItxnb/JQIDAQAB";
    private static final String APP_ID = "wz6012822ca2f1as78";//wzac09fb2b0ad16b28   wz9f2a175d0e0ef632  wz6012822ca2f1as78

    /**
     * Purchase
     */
    @Test
    public void ecrHubCloudPayOrder() {
        OpenApiClient openapiClient = new OpenApiClient(APP_ID, Constants.SANDBOX_GATEWAY_URL, APP_RSA_PRIVATE_KEY, GATEWAY_RSA_PUBLIC_KEY);
        WisehubCloudPayOrderRequest request = new WisehubCloudPayOrderRequest();
        request.setMerchant_no("302300000582");
        request.setStore_no("4023000003");
        request.setTerminal_sn("WPBB002210000234");
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
        request.setMerchant_no("302300000582");
        request.setStore_no("4023000003");
        request.setTerminal_sn("WPBB002210000234");
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
        request.setMerchant_no("302300000582");
        request.setStore_no("4023000003");
        request.setTerminal_sn("WPBB002210000234");
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
