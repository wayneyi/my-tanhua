package com.tanhua.sso.service;//接口类型：互亿无线触发短信接口，支持发送验证码短信、订单通知短信等。
// 账户注册：请通过该地址开通账户http://sms.ihuyi.com/register.html
// 注意事项：
//（1）调试期间，请用默认的模板进行测试，默认模板详见接口文档；
//（2）请使用APIID（查看APIID请登录用户中心->验证码短信->产品总览->APIID）及 APIkey来调用接口；
//（3）该代码仅供接入互亿无线短信接口参考使用，客户可根据实际需要自行编写；

import com.tanhua.sso.vo.ErrorResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SmsService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 发送短信验证码
     * 实现: 发送完成短信验证码后,需要将验证码保存到redis中
     *
     * @param phone
     * @return
     */
    public ErrorResult sendCheckCode(String phone) {
        String redisKey = "CHECK_CODE_" + phone;

        //先判断该手机号发送的验证码是否还未失效
        String value = this.redisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isNotEmpty(value)) {
            String msg = "上一次发送的验证码还未失效";
            return ErrorResult.builder().errCode("000001").errMessage(msg).build();
        }

        String code = sendSms(phone);

        if (StringUtils.isEmpty(code)) {
            String msg = "发送验证码失败";
            return ErrorResult.builder().errCode("000002").errMessage(msg).build();
        }

        //短信发送成功,将验证码保存到redis中
//        this.redisTemplate.opsForValue().set(redisKey, code, Duration.ofMillis(5));
		this.redisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(5));
        String msg = "发送验证码成功";
        return ErrorResult.builder().errCode("000003").errMessage(msg).build();
    }

    private String sendSms(String phone) {
        return "111111";
    }

//	public static final String REDIS_KEY_PREFIX = "CHECK_CODE_";
//	/**
//	 * 发送验证码
//	 *
//	 * @param mobile
//	 * @return
//	 */
//	public Map<String, Object> sendCheckCode(String mobile) {
//		Map<String, Object> result = new HashMap<>(2);
//		try {
//			String redisKey = REDIS_KEY_PREFIX + mobile;
//			String value = this.redisTemplate.opsForValue().get(redisKey);
//			if (StringUtils.isNotEmpty(value)) {
//				result.put("code", 1);
//				result.put("msg", "上一次发送的验证码还未失效");
//				return result;
//			}
//			String code = "123456"; //使用固定验证码
////			String code = sendSms(mobile);	//互亿无线-发送验证码
////			if (null == code) {
////				result.put("code", 2);
////				result.put("msg", "发送短信验证码失败");
////				return result;
////			}
//
//			//发送验证码成功
//			result.put("code", 3);
//			result.put("msg", "ok");
//
//			//将验证码存储到Redis,2分钟后失效
//			this.redisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(5));
//
//			return result;
//		} catch (Exception e) {
//			LOGGER.error("发送验证码出错！" + mobile, e);
//			result.put("code", 4);
//			result.put("msg", "发送验证码出现异常");
//			return result;
//		}
//	}

    //https://www.ihuyi.com/  互亿无线   短信   官网
//	private static String Url = "http://106.ihuyi.cn/webservice/sms.php?method=Submit";
//
//	public static void main(String [] args) {
//		public String sendSms(String mobile){
//			String Url = "http://106.ihuyi.cn/webservice/sms.php?method=Submit";
//
//			HttpClient client = new HttpClient();
//			PostMethod method = new PostMethod(Url);
//
//			client.getParams().setContentCharset("GBK");
//			method.setRequestHeader("ContentType","application/x-www-form-urlencoded;charset=GBK");
//
//			int mobile_code = (int)((Math.random()*9+1)*100000);
//
//			String content = new String("您的验证码是：" + mobile_code + "。请不要把验证码泄露给其他人。");
//
//			NameValuePair[] data = {//提交短信
//					new NameValuePair("account", "C98241495"), //查看用户名是登录用户中心->验证码短信->产品总览->APIID
//					new NameValuePair("password", "2225a38d9313482ca8616badcfde040e"),  //查看密码请登录用户中心->验证码短信->产品总览->APIKEY
//					//new NameValuePair("password", util.StringUtil.MD5Encode("密码")),
//					new NameValuePair("mobile", mobile),
//					new NameValuePair("content", content),
//			};
//			method.setRequestBody(data);
//
//			try {
//				client.executeMethod(method);
//
//				String SubmitResult =method.getResponseBodyAsString();
//
//				//System.out.println(SubmitResult);
//
//				Document doc = DocumentHelper.parseText(SubmitResult);
//				Element root = doc.getRootElement();
//
//				String code = root.elementText("code");
//				String msg = root.elementText("msg");
//				String smsid = root.elementText("smsid");
//
//				System.out.println(code);
//				System.out.println(msg);
//				System.out.println(smsid);
//
//				if("2".equals(code)){
//					System.out.println("短信提交成功");
//				}
//
//				return String.valueOf(mobile_code);
//
//			} catch (HttpException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (DocumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			return null;
//		}
//	}

}