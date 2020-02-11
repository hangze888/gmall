package com.atguigu.gmall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2016101500692513";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCEvlmL+mL1Xvho99MdTrXwF6dsIdsGjjKgQBbTkeWjTmq8mkKlZlBZR9dfUFPcMf134gWOW1Tzk+8mXiiYe2lY9ce88E7qbOsBXNPQlFJtmzDiCh/Nl/a3gpLruo5ep5efYKpmzAkqvvoJKu1icmUrKl6mFb7DP+tpbBVO3MNuHEgk15VAl01nMfddA4PWO+MlWnaf67xtxTw9wRM3dMUf6GFD+L4IpVyOZbsGHHuj7QTTy3z+0kg/Ev1Dx2kiqu6zS78GTDcU6H6XSpdVAjtXacIMawwJEbu9+YoYh7E4SInJbvTxqqUgwQGpM2Gkfz4TA3mp9TTI/y55gYw9wHB3AgMBAAECggEATSNgXhuOzg/2Qulg4TMQPwk/3XJ/GPwhJ0aW/TNXQuMa26a4++zM3FUHLvg7A0Z5iOTrXaJtZ26cFqu444Mz4OJSw9TRxyCsIAqVfCv1CoSYj4JaoNqiIFncjufGO/MgBRnjbAE9gn2gRxuO/xqC9+aTQeh5BnVqk8SfTUi0uJMXQSHTcOLqa1pNpW+kORrjXaAdAy4m2hQWMUJmEk0AMN2uu4C3weNz6mJN+XLPnBC4BRHRJ0+32zSp/+RQpz9cYIJDkQPUmBn8X7Bu9vVUEXRWMaa6Z91hh+LI/eoUh8k52YId9HRBiEdpJY1Cskq3tCZkvEx2KU5HMb43Ksa0wQKBgQDNpzIYlJd4tVU4CUTBfE9rGXoeyMJZxENyb23skjwpb+tFZi5qwGbYMGe3Fyc5QraqZ4AtbF2DsJUcWvM5fCNvaVVrdz0vcbon4Wz/oZboQJxWmVw1l+1vjbpsFmS3/JM3gTXwwvUSsNXNs/6+UNWtJR22R+lau1x2bUKuyxlDaQKBgQClPbn/ptIuBzMhr1f7e6yNm2hZ+CilUNZ04oN//UIp27kIA94KBZ8IWQDITDM6lvxovgVqBbEjBWndpv3b2ADDqnH7xkpjN9qbQ2zVaanmOl9Xx+/cxSQgfK/TVfq7l/LwA+aU4EMmiEXcBx+p6tf8+Pz2J/y5ZkFauDz6DMn43wKBgFwr7JMBviXWt/wmg09o5LtursIvXpQXF9epQ449OIOl6r+u/WzREQciIsn3sZfDY9VaECPLptoIAby0ssB39/XOMv+4cI7cHQAU0+J6zQUJOEjD8YT/s+ZEkmIxYzDnHiBJZCClcGvRH/JTEwC/gBVbRkbVUixdPyLA85sd4hPJAoGAOUhHtV9gV0J5sEC30F9UNxbbuuyzMnPulEB9852ZFSetYiDtFS5TrjIkeksoKzRyNeFBwTpN6OUUiRk9wbuEsE/jCbP4z6JqLw7VqLnjgwNTT/voo/DXkEeJMEsLC4MmXZwMkCmaPb5Z8Weqy121GGY8Ux0TmDXhjBB4WvqZ/aMCgYEAkr9zigeRElKFWFqjxpVW9UwZMg1VBZTZbwOS5bNJDznI/ohUVA/xceGEGhewj18Vke5ET6FvldQdI6UAijV/hw4z3X35z9bTPMeXnaMZwSZUwPUh66jJwzV9IGzLabQXwGo0qILpPqor3rO+o1lJDTE7E771QFiwq0zj5SlZ8Ks=";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnHMHxNNaveCu0vJW6N/wdE+EA5abb+VCwujaFxCRRNuZAgrR1Y3rcNtrAQZ7znDavlKfSKqpyx5GUraacDVph6MbFFA2QNQ2LSz9aD79cjf8a++Nb6FzzXbtrV1WC4/nRLkFNTTqpbq28xFg8mQAq2b9Oq7fwWA85VPbSPVQ2hNuAqh3eNSqq/JhU4hCF1jlDAgxm49jeC8vZk5uCJMkLU/TaESHJiV0i1zJSfjNu07sbUOny2iDK9G72L5ehj5HPXreMFvixjeGyU3lWczP+nvc991nJX3rXd0k2QcJiRRmgCFKgxcJw0TNNDsck3yvzBDTf1hRmgrwX0V8yqW98QIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url;

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url;

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
