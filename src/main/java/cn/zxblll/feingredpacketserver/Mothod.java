package cn.zxblll.feingredpacketserver;

import cn.zxblll.feingredpacketserver.entity.Suesses;
import cn.zxblll.feingredpacketserver.mapper.SuessesMapper;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyResponse;
import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.bean.request.WxPayOrderQueryRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryResult;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderResult;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@Api("微信支付")
@RestController
@RequestMapping("/pay")
@AllArgsConstructor
public class Mothod {
    private WxPayService wxService;

    /**
     * 调用统一下单接口，并组装生成支付所需参数对象.
     *
     * @param request 统一下单请求参数
     * @param <T>     请使用{@link com.github.binarywang.wxpay.bean.order}包下的类
     * @return 返回 {@link com.github.binarywang.wxpay.bean.order}包下的类对象
     */
    @ApiOperation(value = "统一下单，并组装所需支付参数")
    @PostMapping("/createOrder")
    public <T> T createOrder(@RequestBody WxPayUnifiedOrderRequest request) throws WxPayException {
        return this.wxService.createOrder(request);
    }

    /**
     * 统一下单(详见https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_1)
     * 在发起微信支付前，需要调用统一下单接口，获取"预支付交易会话标识"
     * 接口地址：https://api.mch.weixin.qq.com/pay/unifiedorder
     *
     * @param request 请求对象，注意一些参数如appid、mchid等不用设置，方法内会自动从配置对象中获取到（前提是对应配置中已经设置）
     */
    @ApiOperation(value = "原生的统一下单接口")
    @PostMapping("/unifiedOrder")
    public WxPayUnifiedOrderResult unifiedOrder(@RequestBody WxPayUnifiedOrderRequest request) throws WxPayException {
        return this.wxService.unifiedOrder(request);
    }

    @ApiOperation(value = "查询订单")
    @GetMapping("/queryOrder")
    public WxPayOrderQueryResult queryOrder(@RequestParam(required = false) String transactionId,
                                            @RequestParam(required = false) String outTradeNo)
            throws WxPayException {
        return this.wxService.queryOrder(transactionId, outTradeNo);
    }

    @ApiOperation(value = "查询订单")
    @PostMapping("/queryOrder")
    public WxPayOrderQueryResult queryOrder(@RequestBody WxPayOrderQueryRequest wxPayOrderQueryRequest) throws WxPayException {
        return this.wxService.queryOrder(wxPayOrderQueryRequest);
    }

    @ApiOperation(value = "支付回调通知处理")
    @PostMapping("/notify/order")
    public String parseOrderNotifyResult(@RequestBody String xmlData) throws WxPayException {
        final WxPayOrderNotifyResult notifyResult = this.wxService.parseOrderNotifyResult(xmlData);
        // TODO 根据自己业务场景需要构造返回对象
        String accessToken = getAccessToken();
        //获取openid
        String openid = notifyResult.getOpenid();
        //如果获取的不是数字,就成功
        String url = sendRedPacketCover(accessToken, openid);
        if(url.length() > 8) {
            //存储到数据库
            addData(openid, url);
            //返回成功
            return WxPayNotifyResponse.success("成功，跳转到" + url + "页面领取红包封面");
        }else {
            return WxPayNotifyResponse.fail("失败,错误码:" + url);
        }


    }
    @ApiOperation(value = "获取AccessToken")
    @GetMapping("/notify/getAccessToken")
    //发送GET请求 获取AccessToken
    public String getAccessToken(){
        // 请求地址 https://api.weixin.qq.com/cgi-bin/token
        // 请求方式 GET
        // 请求参数 grant_type=client_credential&appid=APPID&secret=APPSECRET
        String appid = "";//你的appid
        String secret = "";//你的secret

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET".replace("APPID",appid).replace("APPSECRET",secret);
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        String jsonString = response.getBody();
        //使用GSON解析json
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
        String access_token = jsonObject.get("access_token").getAsString();

        return access_token;
    }
    @ApiOperation(value = "发送红包领取链接")
    @GetMapping("/notify/sendRedPacket")
    //封装发送POST请求 获取红包封面兑换码 回传String
    public String sendRedPacketCover(String accessToken,String openid){
        // 需要传入的参数
        // openid

        // 请求地址 https://api.weixin.qq.com/cgi-bin/message/mass/send?access_token=ACCESS_TOKEN
        // 请求方式 POST
        // 请求参数 openid access_token ctoken
        String ctoken = "";//你的ctoken

        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.weixin.qq.com/cgi-bin/message/mass/send?access_token=ACCESS_TOKEN".replace("ACCESS_TOKEN",accessToken);
        //封装参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("openid",openid);
        params.add("ctoken",ctoken);
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        String jsonString = response.getBody();
        //使用GSON解析json
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
        //获取Data中 url
        String coreUrl = jsonObject.get("data").getAsJsonObject().get("url").getAsString();
        String errcode = jsonObject.get("errcode").getAsString();
        if (errcode.equals("0")){
            return coreUrl;
        }else {
            return errcode;
        }


    }
    @Autowired
    private static SuessesMapper suessesMapper;
    //封装MybatisPlus 添加数据方法
    public static void addData(String openid,String url){
        int paymoney = 0;
        Suesses suesses = new Suesses();
        if (url.length() < 8){
            //插入一条数据
            suesses.setOpenid(openid);
            suesses.setState(String.valueOf(0));
            suesses.setPaymoney(String.valueOf(paymoney));
            suessesMapper.insert(suesses);

        }
    }


}
