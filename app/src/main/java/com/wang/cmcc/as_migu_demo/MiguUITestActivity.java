package com.wang.cmcc.as_migu_demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cmcc.migusso.sdk.auth.AuthnConstants;
import com.cmcc.migusso.sdk.auth.AuthnHelper;
import com.cmcc.migusso.sdk.auth.TokenListener;
import com.cmcc.migusso.sdk.common.BoolCallBack;
import com.cmcc.migusso.sdk.common.ICallBack;
import com.cmcc.migusso.sdk.common.MiguUI;
import com.cmcc.migusso.sdk.common.MiguUIConstants;
import com.cmcc.migusso.sdk.common.ThirdEventListener;
import com.cmcc.migusso.sdk.common.TokenProcess;
import com.cmcc.migusso.sdk.util.LogUtil;
import com.cmcc.migusso.sdk.util.SsoSdkConstants;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by wang on 2015/11/27.
 */
public class MiguUITestActivity extends Activity implements View.OnClickListener{

    private final String TAG = "MiguUITestActivity";
    private Button mLoginBtn;
    private Button mSmsLoginBtn;
    private Button mfindPwdBtn;
    private TextView mLoginInfoTv;
    private Button mExitLoginBtn;
    private Button mUpgradeBtn;
    private String userName; //用户名
    private boolean isNeedUpgrade; //账号是否需要升级
    private EditText mSourceIdEt;

    private AuthnHelper mAuthnHelper;
    private ThirdEventListener mThirdEventListener; //第三方登录回调接口
    private TokenProcess mTokenProcess;     //默认界面获取Token后，回调的接口

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_migu_ui_test);
        initViews();
        setListener();

        setMiguUIParams();

    }

    @Override
    protected void onDestroy() {

        mAuthnHelper.unRegisterCallBacks();

        super.onDestroy();
    }

    private void initViews() {
        mLoginBtn = (Button) findViewById(R.id.begin_login_btn);
        mLoginInfoTv = (TextView) findViewById(R.id.login_state_tv);
        mSmsLoginBtn = (Button) findViewById(R.id.sms_login_btn);
        mfindPwdBtn = (Button) findViewById(R.id.find_pwd_btn);
        mExitLoginBtn = (Button) findViewById(R.id.exit_login_btn);
        mUpgradeBtn = (Button) findViewById(R.id.upgrade_remind_btn);
        mSourceIdEt = (EditText) findViewById(R.id.sourceid_input_edt);
    }

    private void setListener() {
        mLoginBtn.setOnClickListener(this);
        mSmsLoginBtn.setOnClickListener(this);
        mfindPwdBtn.setOnClickListener(this);
        mExitLoginBtn.setOnClickListener(this);
        mUpgradeBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.begin_login_btn:
                if(TextUtils.isEmpty(mSourceIdEt.getText().toString())) {
                    mAuthnHelper.getAccessTokenByCondition(HostConfig.APP_ID, HostConfig.APP_KEY, SsoSdkConstants.AUTHN_ACCOUNT_PASSWORD, null, null, null);
                } else {
                    mAuthnHelper.getAccessTokenByCondition(mSourceIdEt.getText().toString(), HostConfig.APP_KEY, SsoSdkConstants.AUTHN_ACCOUNT_PASSWORD, null, null, null);
                }
                break;

            case R.id.sms_login_btn:
                if(TextUtils.isEmpty(mSourceIdEt.getText().toString())) {
                    mAuthnHelper.getAccessTokenByCondition(HostConfig.APP_ID, HostConfig.APP_KEY, SsoSdkConstants.AUTHN_ACCOUNT_SMSCODE, null, null, null);
                } else {
                    mAuthnHelper.getAccessTokenByCondition(mSourceIdEt.getText().toString(), HostConfig.APP_KEY, SsoSdkConstants.AUTHN_ACCOUNT_SMSCODE, null, null, null);
                }
                break;

            case R.id.find_pwd_btn:
                if(TextUtils.isEmpty(mSourceIdEt.getText().toString())) {
                    mAuthnHelper.resetPassword(HostConfig.APP_ID, HostConfig.APP_KEY, null, null, null, null);
                } else {
                    mAuthnHelper.resetPassword(mSourceIdEt.getText().toString(), HostConfig.APP_KEY, null, null, null, null);
                }
                break;

            case R.id.exit_login_btn:
                mLoginInfoTv.setText("未登录");
                break;

            case R.id.upgrade_remind_btn:
//                mAuthnHelper.showUpgradeDialog("188********");
//                new ChangeHeadImageDialog(this).show();
//                new SimpleToast(this, " 新头像上传成功").show(0, 0);
//                new CommonReminderDialog(this, "昵称设置失败", "网络忙，请稍后重试~").show();
//                startActivity(new Intent(this, com.cmcc.migusso.sdk.activity.ChangeNickNameActivity.class));
//                startActivity(new Intent(this, com.cmcc.migusso.sdk.activity.BindPhoneNumActivity.class));
//                startActivity(new Intent(this, com.cmcc.migusso.sdk.activity.ChangePasswordActivity.class));
//                startActivity(new Intent(this, com.cmcc.migusso.sdk.activity.SetPasswordActivity.class));
//                startActivity(new Intent(this, com.cmcc.migusso.sdk.activity.VerifyOldPhoneActivity.class));
//                startActivity(new Intent(this, com.cmcc.migusso.sdk.activity.BindNewPhoneActivity.class));
                Intent intent = new Intent();
                intent.putExtra("LoginId", userName);
                intent.setClass(this, com.cmcc.migusso.sdk.activity.UserManageActivity.class);
                startActivity(intent);
                mAuthnHelper.showUserInfo(HostConfig.APP_ID, HostConfig.APP_KEY, userName);
                break;

            default:
                break;
        }
    }

    /**
     * 咪咕登录页参数设置
     */
    private void setMiguUIParams() {

        mAuthnHelper = new AuthnHelper(this);
        /**
         * 设置用户协议的内容
         */
        mAuthnHelper.setUserProtocol(R.string.protocolContent);

        /**
         * 设置找回密码页FindPasswordActivity的回调接口
         */
        mAuthnHelper.setFindPwdCallBack(new BoolCallBack() {
            @Override
            public void callback(boolean b) {
                if (b == true) {
                    Log.d(TAG, "找回密码成功.");
                    //add your code here
                } else {
                    Log.d(TAG, "找回密码失败.");
                    //add your code here
                }
            }
        });

        /**
         * 设置LoginActivity、SmsLoginActivity、RegisterActivity三个登录页的回调接口，回调发生在获取token成功后
         * 注意：parseToken，afterLogin两个回调在子线程中执行，不能再这里直接操作ui
         */
        mAuthnHelper.setTokenProcess(new TokenProcess() {
            @Override
            public JSONObject parseToken(String token) {
                return userCheckToken(token);
            }

            @Override
            public void afterLogin(JSONObject jsonObject) {
                boolean result = jsonObject.optBoolean(MiguUIConstants.KEY_RESULT);

                if (result == true) {
                    userName = jsonObject.optString(MiguUIConstants.KEY_LOGIN_ACCOUNT);
                    isNeedUpgrade = jsonObject.optBoolean("needUpgrade");
                    Log.d(TAG, "Login Success. " + userName);
                    mHandler.sendEmptyMessage(0);
                    return;
                }

                int errorCode = jsonObject.optInt(MiguUIConstants.KEY_ERROR_CODE);
                String errorString = jsonObject.optString(MiguUIConstants.KEY_ERROR_STRING);
                Log.d(TAG, "Login Failed." +  errorCode + "  " + errorString);
                mHandler.sendEmptyMessage(1);
            }
        });

        mThirdEventListener = new ThirdEventListener() {
            @Override
            public void onCallBack(int i) {
                switch (i) {
                    case MiguUIConstants.EVENT_QQ:
                        Log.d(TAG, "touch QQ");
                        //add your code here
                        break;
                    case MiguUIConstants.EVENT_WEIBO:
                        Log.d(TAG, "touch WEIBO");
                        //add your code here
                        break;
                    case MiguUIConstants.EVENT_WECHAT:
                        Log.d(TAG, "touch WECHAT");
                        //add your code here
                        break;
                    default:
                        break;
                }
            }
        };


        /**
         * 设置第三方登录，微信、微博、QQ的回调接口
         */
        mAuthnHelper.setThirdAuthn(MiguUIConstants.EVENT_QQ, mThirdEventListener);
        mAuthnHelper.setThirdAuthn(MiguUIConstants.EVENT_WEIBO, mThirdEventListener);
        mAuthnHelper.setThirdAuthn(MiguUIConstants.EVENT_WECHAT, mThirdEventListener);

        /**
         * 设置自定义的logo，颜色；5个子公司不要调用
         */
        mAuthnHelper.setLogo(R.drawable.ic_launcher);
        mAuthnHelper.setThemeColor(R.color.purple);

        mAuthnHelper.setVisitorCallBack(new ICallBack() {
            @Override
            public void callback() {
                LogUtil.info("in visitor callback..");
                //如果需要关闭登录页，请调用finishTopMiguActivity方法
                mAuthnHelper.finishTopMiguActivity();
                //add your code here
            }
        });

        mAuthnHelper.setPwdSafeCallBack(new ICallBack() {
            @Override
            public void callback() {
                LogUtil.info("in pwdSafe callback..");
                //如果需要关闭找密码页面，请调用finishTopMiguActivity方法
                mAuthnHelper.finishTopMiguActivity();
                //add your code here
            }
        });

    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:

                    mLoginInfoTv.setText("登录账号： " + userName);
//                    if(isNeedUpgrade) {mAuthnHelper.showUpgradeDialog(userName);}
                    break;

                case 1:
                    mLoginInfoTv.setText("登录失败.");
                    break;

                default:
                    break;
            }
        }
    };


    /**
     * 解析token并返回Json数据回调给登录页，这是需要用户自己实现的方法
     * @param tokenString
     * @return
     */
    private JSONObject userCheckToken(String tokenString) {

        HttpClient httpClient = new DefaultHttpClient();
        // 请求超时
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 20000);
        // 读取超时
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 20000);
        Log.d(TAG, "server url is " + HostConfig.TOKEN_CHECK_URL);
        HttpPost httpPost = new HttpPost(HostConfig.TOKEN_CHECK_URL);

        JSONObject resultJson = new JSONObject();
        try
        {
            StringEntity s = new StringEntity(getMsg(tokenString, HostConfig.SOURCE_ID).toString());
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            httpPost.setEntity(s);

            Log.d(TAG, "start request.");
            HttpResponse response = httpClient.execute(httpPost);
            int responseCode = response.getStatusLine().getStatusCode();
            Log.d(TAG, "responseCode = " + responseCode);
            if ( HttpStatus.SC_OK == responseCode )
            {
                Header[] headers = response.getAllHeaders();
                for (Header header : headers)
                {
                    Log.d(TAG, header.getName() + " : " + header.getValue());
                }

                String tokenCheckResult = EntityUtils.toString(response.getEntity());
                return userCallBackResult(tokenCheckResult);
            }

            return resultJson;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return resultJson;
    }

    /**
     * 解析token校验返回的结果
     * @param tokenCheckResult token校验的结果
     * @return  JSONObject 返回的Json数据
     * @throws JSONException
     */
    private JSONObject userCallBackResult(String tokenCheckResult) {

        Log.d(TAG, "tokenCheckResult : " + tokenCheckResult);
        try {

            JSONObject jsonObject = new JSONObject(tokenCheckResult);
            JSONObject resultJson = new JSONObject();
            int resultCode = jsonObject.getJSONObject("header").getInt("resultcode");
            Log.d(TAG, "resultCode: "  + resultCode);

            switch (resultCode) {
                case AuthnConstants.SERVER_CODE_SUCCESS: //token校验成功
                    String loginAccount = jsonObject.optJSONObject("body").optString("msisdn");
                    if (TextUtils.isEmpty(loginAccount)) {
                        loginAccount = jsonObject.getJSONObject("body").optString("email", "");
                    }
                    //是否是隐式账号
                    String implicit = jsonObject.optJSONObject("body").optString("implicit");
                    //是否是业务账号
                    String authtype = jsonObject.getJSONObject("body").optString("authtype");

                    /**
                     * 成功至少返回{"reuslt":true}
                     */
                    resultJson.put(MiguUIConstants.KEY_RESULT, true);
                    resultJson.put(MiguUIConstants.KEY_LOGIN_ACCOUNT, loginAccount);
                    resultJson.put("needUpgrade", ifNeedUpgrade(implicit, authtype));
                    break;

                // 如下错误码，需要调用中间件cleanSSO接口清理环境，然后重新执行登录
                // 以下几个case可以删除，只保留defualt代码段
                case AuthnConstants.SERVER_CODE_BTID_NOEXIST_CODE:
                case AuthnConstants.SERVER_CODE_TOKEN_VALIDATE_ERROR:
                case AuthnConstants.SERVER_CODE_KS_EXPIRE_ERROR:
                case AuthnConstants.SERVER_CODE_KS_NO_EXIST:
                case AuthnConstants.SERVER_CODE_TV_SQN_ERROR:
                case AuthnConstants.SERVER_CODE_MAC_ERROR:
                case AuthnConstants.SERVER_CODE_SOURCEID_NOEXIST:
                case AuthnConstants.SERVER_CODE_BTID_NOT_EXIST:
                default://token校验失败的处理

                    /**
                     * 第一次登录token校验失败，调用cleansso之后重新调用登录流程。
                     * 应用集成时此处if...else...逻辑必须加上
                     */
                    // if(firstTry) {

                        cleanSSO(); // 第一次token校验失败，清ks
                        // 第一次token校验失败，重新调用登录流程进行第二次尝试
//                        mAuthnHelper.getAccessToken(HostConfig.APP_ID, HostConfig.APP_KEY, null, SsoSdkConstants.LOGIN_TYPE_DEFAULT, new TokenListener() {
//                            @Override
//                            public void onGetTokenComplete(JSONObject jsonObject) {
//                                checkToken();
//                            }
//                        });
                    //} else {

                        /**
                         * 第二次登录token校验失败，则判断为登录失败。
                         */
                        resultJson.put(MiguUIConstants.KEY_RESULT, false);
                        resultJson.put(MiguUIConstants.KEY_ERROR_CODE, resultCode);
                        String resultString = jsonObject.optJSONObject("body").optString("resultstring");
                        resultJson.put(MiguUIConstants.KEY_ERROR_STRING, resultString);

                    // }

                    break;
            }

            return resultJson;

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

    }
    /**
     * 解析token校验返回的结果
     * @param token token校验的结果
     * @return  JSONObject 返回的Json数据
     * @throws JSONException
     */
//    private JSONObject parseToken(String token) {
//
//        /**'
//         * 客户端向应用平台发送token，应用平台向携带token去咪咕认证平台校验，并返回校验结果
//         * token校验，checkToken方法由客户端自己实现
//         */
//        String tokenCheckResult = checkToken(token);
//        Log.d(TAG, "tokenCheckResult : " + tokenCheckResult);
//        //下面代码示范如何处理token校验结果，只做参考。具体合格和定义用客户端自行决定
//        try {
//
//            JSONObject jsonObject = new JSONObject(tokenCheckResult);
//            JSONObject resultJson = new JSONObject();
//            int resultCode = jsonObject.getJSONObject("header").getInt("resultcode");
//            switch (resultCode) {
//                case AuthnConstants.SERVER_CODE_SUCCESS: //token校验成功，获取登录用户名
//                    String loginAccount = jsonObject.optJSONObject("body").optString("msisdn");
//                    if (TextUtils.isEmpty(loginAccount)) {
//                        loginAccount = jsonObject.getJSONObject("body").optString("email", "");
//                    }
//                    // 成功至少返回{"reuslt":true}
//                    resultJson.put(MiguUIConstants.KEY_RESULT, true);
//                    resultJson.put(MiguUIConstants.KEY_LOGIN_ACCOUNT, loginAccount);
//                    break;
//
//                // //token校验失败，如下错误码，需要调用中间件cleanSSO接口清理环境，然后重新执行登录
//                // 以下几个case可以删除，只保留defualt代码段
//                case AuthnConstants.SERVER_CODE_BTID_NOEXIST_CODE:
//                case AuthnConstants.SERVER_CODE_TOKEN_VALIDATE_ERROR:
//                case AuthnConstants.SERVER_CODE_KS_EXPIRE_ERROR:
//                case AuthnConstants.SERVER_CODE_KS_NO_EXIST:
//                case AuthnConstants.SERVER_CODE_TV_SQN_ERROR:
//                case AuthnConstants.SERVER_CODE_MAC_ERROR:
//                case AuthnConstants.SERVER_CODE_SOURCEID_NOEXIST:
//                case AuthnConstants.SERVER_CODE_BTID_NOT_EXIST:
//                default:
//
//                    /**
//                     * 第一次登录token校验失败，调用cleansso之后重新调用登录流程。
//                     * 应用集成时此处if...else...逻辑必须加上
//                     */
//                    // if(firstTry) {
//                    cleanSSO(); // 第一次token校验失败，清ks
//                    // 第一次token校验失败，重新调用登录流程进行第二次尝试
//
//                    //} else {
//                    /**
//                     * 第二次登录token校验失败，则判断为登录失败，返回errorCode和errorString
//                     */
//                    resultJson.put(MiguUIConstants.KEY_RESULT, false);
//                    resultJson.put(MiguUIConstants.KEY_ERROR_CODE, resultCode);
//                    String resultString = jsonObject.optJSONObject("body").optString("resultstring");
//                    resultJson.put(MiguUIConstants.KEY_ERROR_STRING, resultString);
//                    // }
//                    break;
//            }
//            return resultJson;
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    private boolean ifNeedUpgrade(String implicit, String authtype) {
        if(implicit.equals("1")) {
            Log.d("NeedUpgrade", "隐式账号");
            return true;
        }

        if(authtype.equals("ServicePassport")) {
            Log.d("NeedUpgrade", "业务账号");
            return true;
        }

        return false;
    }

    /**
     * 返回请求参数
     *
     * @return
     */
    public JSONObject getMsg(String token, String sourceid)
    {
        try
        {
            JSONObject header = new JSONObject();
            JSONObject body = new JSONObject();
            JSONObject msg = new JSONObject();

            String p_pattern = "yyyyMMddHHmmssSSS";
            SimpleDateFormat p_sdf = new SimpleDateFormat(p_pattern, Locale.CHINA);
            // 定义要转换的Date对象，我的例子中使用了当前时间
            Calendar p_cal = Calendar.getInstance();
            Date p_date = p_cal.getTime();
            // 输出结果
            System.out.println("====>" + p_sdf.format(p_date));
            header.put("systemtime", p_sdf.format(p_date).toString());
            header.put("version", "1.0");
            header.put("msgid", "abcde");// 目前没用处
            header.put("apptype", "3");
            header.put("sourceid", sourceid);
            body.put("token", token);
            msg.put("header", header);
            msg.put("body", body);
            Log.d(TAG, "tokenCheck request : " + msg.toString());
            return msg;
        }
        catch (JSONException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 清理SSO环境
     *
     */
    private void cleanSSO()
    {
        mAuthnHelper.cleanSSO(new TokenListener()
        {
            @Override
            public void onGetTokenComplete(JSONObject json)
            {
                Log.d(TAG, "cleanSSO result json = " + json);
            }
        });

    }

}
