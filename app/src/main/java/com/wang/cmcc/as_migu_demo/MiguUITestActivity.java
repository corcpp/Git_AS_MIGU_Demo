package com.wang.cmcc.as_migu_demo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
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

    private static final int AUTO_LOGIN_ERROR = -1;
    private static final int LOGIN_SUCCESS = 0;
    private static final int LOGIN_ERROR = 1;
    private static final int LOGIN_CUSTOMERID = 2;
    private static final int LOGIN_THIRDPART = 3;

    private Button mLoginBtn;
    private Button mSmsLoginBtn;
    private Button mfindPwdBtn;
    private TextView mLoginInfoTv;
    private Button mExitLoginBtn;
    private Button mUpgradeBtn;
    private String userName; //用户名
    private String nickName; //第三方账号昵称
    private boolean isNeedUpgrade; //账号是否需要升级
    private EditText mSourceIdEt;
    private ProgressDialog dialog;

    private AuthnHelper mAuthnHelper;
    private ThirdEventListener mThirdEventListener; //第三方登录回调接口

    private boolean isThirdLoginWay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_migu_ui_test);
        Log.d(TAG, "onCreate...");
        initViews();
        setListener();

        setMiguUIParams();

        //自动登录
//        tryAutoLogin();

    }

    /**
     * 先尝试自动登录
     */
    private void tryAutoLogin() {

        showProgressDialog(this);
        mAuthnHelper.getAccessToken(HostConfig.APP_ID, HostConfig.APP_KEY, null, SsoSdkConstants.LOGIN_TYPE_DEFAULT, new TokenListener() {
            @Override
            public void onGetTokenComplete(JSONObject jsonObject) {
                Log.d(TAG, " auto login jsonObject : " + jsonObject.toString());
                dismissProgressDialog();
                if (jsonObject == null) {
                    Message msg = Message.obtain();
                    msg.what = AUTO_LOGIN_ERROR;
                    msg.obj = "自动登录失败";
                    mHandler.sendMessage(msg);
                    return;
                }

                int resultCode = jsonObject.optInt(SsoSdkConstants.VALUES_KEY_RESULT_CODE, -1);

                if (resultCode != AuthnConstants.CLIENT_CODE_SUCCESS) {
                    Message msg = Message.obtain();
                    msg.what = AUTO_LOGIN_ERROR;
                    msg.obj = jsonObject.optString(SsoSdkConstants.VALUES_KEY_RESULT_STRING);
                    mHandler.sendMessage(msg);
                    return;
                }

                String token = jsonObject.optString(SsoSdkConstants.VALUES_KEY_TOKEN);
                //拿到token后，进行token 校验
                JSONObject jsonResult = userCheckToken(token);
                Log.d(TAG, "Auto Login tokenValidate " + jsonResult.toString());

                boolean result = jsonResult.optBoolean(MiguUIConstants.KEY_RESULT);
                //如果token校验成功
                if (result) {
                    userName = jsonResult.optString(MiguUIConstants.KEY_LOGIN_ACCOUNT);
                    mHandler.sendEmptyMessage(LOGIN_SUCCESS);
                    return;
                }
                //如果token校验失败
                int errorCode = jsonResult.optInt(MiguUIConstants.KEY_ERROR_CODE);
                String errorString = jsonResult.optString(MiguUIConstants.KEY_ERROR_STRING);
                Log.d(TAG, "Auto Login Failed." + errorCode + "  " + errorString);
                mHandler.sendEmptyMessage(AUTO_LOGIN_ERROR);
            }
        });
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
    protected void onDestroy() {
        Log.d(TAG, "onDestroy...");
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {

        if(!TextUtils.isEmpty(mSourceIdEt.getText().toString()) ) {
            HostConfig.APP_ID = mSourceIdEt.getText().toString();
        }

        switch (v.getId()) {
            case R.id.begin_login_btn:
                mAuthnHelper.getAccessTokenByCondition(HostConfig.APP_ID, HostConfig.APP_KEY, SsoSdkConstants.AUTHN_ACCOUNT_PASSWORD, null, null, null);
                break;

            case R.id.sms_login_btn:
                mAuthnHelper.getAccessTokenByCondition(HostConfig.APP_ID, HostConfig.APP_KEY, SsoSdkConstants.AUTHN_ACCOUNT_SMSCODE, null, null, null);
                break;

            case R.id.find_pwd_btn:
                mAuthnHelper.resetPassword(HostConfig.APP_ID, HostConfig.APP_KEY, null, null, null, null);
                break;

            case R.id.exit_login_btn:
                mLoginInfoTv.setText("未登录");
                userName = null;
                // TODO: 2016/4/14  不需调用cleansso
                mAuthnHelper.cleanSSO(new TokenListener() {
                    @Override
                    public void onGetTokenComplete(JSONObject jsonObject) {
                        Log.d(TAG, "cleanSSO result json = " + jsonObject);
                    }
                });
                break;

            case R.id.upgrade_remind_btn:
                mAuthnHelper.showUpgradeDialog(userName);
                break;

            default:
                break;
        }
    }

    /**
     * 咪咕登录页接口参数设置
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
         * 设置账号升级页面回调接口，升级成功返回true，失败返回false
         */
        mAuthnHelper.setUpgradeCallBack(new BoolCallBack() {
            @Override
            public void callback(boolean b) {
                if (b == true) {
                    Log.d(TAG, "账号升级成功.");
                    //add your code here
                } else {
                    Log.d(TAG, "账号升级失败.");
                    //add your code here
                }
            }
        });

        /**
         * 设置主登录、短信登录、注册、找回密码、账号升级页面中处理token的回调接口，sdk通过此接口将获取token返回给应用。
         * 注意：parseToken，afterLogin两个回调在子线程中执行，不能再这里直接操作ui
         */
        mAuthnHelper.setTokenProcess(new TokenProcess() {
            @Override
            public JSONObject parseToken(final String token) {

                //同步方式校验token
//                return userCheckToken(token);

                //异步方式校验token
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        JSONObject resultJson = userCheckToken(token);
                        //异步方式在token校验结束后，必须主动调用以下方法；通知sdk token校验的结果，否则登录不能正常关闭。
                        mAuthnHelper.notifyLoginResult(resultJson);

                    }
                }).start();

//                异步请求直接返回null
                return null;
            }

            @Override
            public void afterLogin(JSONObject jsonObject) {
                boolean result = jsonObject.optBoolean(MiguUIConstants.KEY_RESULT);

                if (result == true) {
                    userName = jsonObject.optString(MiguUIConstants.KEY_LOGIN_ACCOUNT);
                    isNeedUpgrade = jsonObject.optBoolean("needUpgrade");
                    Log.d(TAG, "Login Success. " + userName);
                    mHandler.sendEmptyMessage(LOGIN_SUCCESS);
                    return;
                }

                int errorCode = jsonObject.optInt(MiguUIConstants.KEY_ERROR_CODE);
                String errorString = jsonObject.optString(MiguUIConstants.KEY_ERROR_STRING);
                Log.d(TAG, "Login Failed." +  errorCode + "  " + errorString);
                mHandler.sendEmptyMessage(LOGIN_ERROR);
            }
        });

        mThirdEventListener = new ThirdEventListener() {
            @Override
            public void onCallBack(int i, Context context) {
                switch (i) {
                    case MiguUIConstants.EVENT_QQ:
                        Log.d(TAG, "touch QQ");
                        //add your code here
//                        doThirdLogin(QQ.NAME, loginAcitvity);
                        break;

                    case MiguUIConstants.EVENT_WEIBO:
                        Log.d(TAG, "touch WEIBO");
                        //add your code here
//                        doThirdLogin(SinaWeibo.NAME , loginAcitvity);
                        break;

                    case MiguUIConstants.EVENT_WECHAT:
                        Log.d(TAG, "touch WECHAT");
                        //add your code here
//                        doThirdLogin(Wechat.NAME, loginAcitvity);
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
         * 设置自定义的logo，颜色；除了咪咕阅读、咪咕音乐、咪咕视频、咪咕游戏、咪咕动漫其他应用需要调用以下两个方法设置logo和主调颜色。
         */
        mAuthnHelper.setLogo(R.drawable.ic_launcher);
        mAuthnHelper.setThemeColor(R.color.purple);

//        /**
//         * 设置游客账号获取的回调接口
//         */
//        mAuthnHelper.setVisitorCallBack(new JSONCallBack() {
//
//            @Override
//            public void callback(JSONObject jsonObject) {
//                LogUtil.info("in visitor callback..");
//                //成功获取到游客账号
//                //add your code here
//                Log.d(TAG, "CustomerId json : " + jsonObject.toString() );
//                int resultCode = jsonObject.optInt(SsoSdkConstants.VALUES_KEY_RESULT_CODE, -1);
//                String resultString = jsonObject.optString(SsoSdkConstants.VALUES_KEY_RESULT_STRING);
//
//                if(resultCode == 102000) {
//                    String customerid = jsonObject.optString(SsoSdkConstants.VALUES_KEY_CUSTOMERID);
//                    String deviceidtype = jsonObject.optString(SsoSdkConstants.VALUES_KEY_DEVICEIDTYPE);
//                    String deviceid = jsonObject.optString(SsoSdkConstants.VALUES_KEY_DEVICEID);
//                    Log.d(TAG, "CustomerId : " + customerid );
//                    mHandler.sendMessage(Message.obtain(mHandler, LOGIN_CUSTOMERID, customerid + "\n deviceidtype : " + deviceidtype + " , deviceid : " + deviceid));
//                }
//
//            }
//        });


        /**
         * 咪咕阅读密保找回接口设置，其他应用无需设置
         */
        mAuthnHelper.setPwdSafeCallBack(new ICallBack() {
            @Override
            public void callback() {
                LogUtil.debug(TAG, "pwdSafe callback...");
                //如果需要关闭找密码页面，请调用finishTopMiguActivity方法
                mAuthnHelper.finishTopMiguActivity();
                //add your code here
            }
        });


//        /**
//         * 监听登录页面的返回键
//         */
//        mAuthnHelper.setLoginPageCancelBack(new ICallBack() {
//            @Override
//            public void callback() {
//                LogUtil.debug(TAG, "pressed LoginActivity back button...");
//            }
//        });

    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            dismissProgressDialog();

            switch (msg.what) {

                case AUTO_LOGIN_ERROR:
                    if (msg.obj != null)
                        mLoginInfoTv.setText("自动登录失败." + msg.obj.toString());
                    //自动登录失败，跳转到登录页面，应用自选
//                    mAuthnHelper.getAccessTokenByCondition(HostConfig.APP_ID, HostConfig.APP_KEY, SsoSdkConstants.AUTHN_ACCOUNT_PASSWORD, null, null, null);
                    break;

                case LOGIN_SUCCESS:
                    mLoginInfoTv.setText("登录账号： " + userName);
                    if(isNeedUpgrade) {mAuthnHelper.showUpgradeDialog(userName);}
                    break;

                case LOGIN_ERROR:
                    if (msg.obj != null)
                        mLoginInfoTv.setText("登录失败." + msg.obj.toString());
                    break;

                case LOGIN_THIRDPART:
                    //关闭LoginActivity
                    mAuthnHelper.finishTopMiguActivity();
                    mLoginInfoTv.setText("第三方登录账号： " + nickName);
                    break;

                case LOGIN_CUSTOMERID:
                    if(msg.obj != null)
                        mLoginInfoTv.setText("游客账号： " + msg.obj.toString());
                    break;

                default:
                    break;
            }
        }
    };

    /**
     * 解析token并返回Json数据回调给登录页，这是需要用户自己实现的方法
     * @param tokenString token
     * @return  约定格式的JSONObject
     */
    private JSONObject userCheckToken(final String tokenString) {

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

        } catch (Exception e) {

            e.printStackTrace();

        }

        Log.w(TAG, "登录失败， userCheckToken 返回格式错误" );
        return resultJson;
    }

    /**
     * 解析token校验返回的结果
     * @param tokenCheckResult token校验的结果
     * @return  JSONObject 约定格式的JSONObject
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
                    String loginAccount = jsonObject.optJSONObject("body").optString("loginid");
//                    String loginAccount = jsonObject.optJSONObject("body").optString("msisdn");
//
//                    if (TextUtils.isEmpty(loginAccount)) {
//                        loginAccount = jsonObject.getJSONObject("body").optString("email", "");
//                    }
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

                    /**
                     * 第三方登录，需返回字段
                     */
                    /**
                     * 第三方登录，还需取昵称nickname
                     */
                    if(authtype.equalsIgnoreCase("QQ") || authtype.equalsIgnoreCase("WEIBO") || authtype.equalsIgnoreCase("WECHAT")) {
                        isThirdLoginWay = true;
                        resultJson.put("authtype", authtype);
                        String nickname = jsonObject.getJSONObject("body").optString("nickname");
                        resultJson.put("nickname", nickname);
                    } else {
                        isThirdLoginWay = false;
                    }
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
                default:
                    //token校验失败的处理，清缓存
                    cleanSSO();

                    resultJson.put(MiguUIConstants.KEY_RESULT, false);
                    resultJson.put(MiguUIConstants.KEY_ERROR_CODE, resultCode);
                    String resultString = jsonObject.optJSONObject("body").optString("resultstring");
                    resultJson.put(MiguUIConstants.KEY_ERROR_STRING, resultString);

                    break;
            }

            return resultJson;

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

    }


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
     * 第三方登录，需要应用自己实现
     * @param authType
     */
//    private void doThirdLogin(String authType, Context context) {
//
//        if(!DeviceUtils.hasNetwork(this)) {
//            return;
//        }
//
//        ShareSDK.initSDK(this);
//        Platform platform = ShareSDK.getPlatform(this, authType);
//        if(authType.equals(Wechat.NAME) && !platform.isClientValid()) {
////            Toast.makeText(this, "请先安装微信客户端", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        //应用在这里需实现自己的等待对话框，避免在用户第三方登录时重复点击其他登录按钮。progressDialog形式可由应用自定义
//        showProgressDialog(context);
//
//        if (platform.isAuthValid()) {
//            platform.removeAccount(true);
//        }
//        //使用SSO授权，通过客户单授权
//        platform.SSOSetting(false);
//
//        platform.setPlatformActionListener(new PlatformActionListener() {
//            public void onComplete(Platform plat, int action, HashMap<String, Object> res) {
//
//                if (action == Platform.ACTION_USER_INFOR) {
//
//                    String uid;
//                    String platInfo;
//                    String nickName;
//                    String headUrl;
//                    PlatformDb platDB = plat.getDb();//获取数平台数据DB
//
//                    if (plat.getName().equals(Wechat.NAME)) { //微信请优先取unionid
//                        LogUtil.info("unionid: " + plat.getDb().get("unionid"));
//                        uid = plat.getDb().get("unionid");
//                    } else {  //qq、微博分别取openid 、uid
//                        LogUtil.info("uid/openid: " + platDB.getUserId());
//                        uid = platDB.getUserId();
//                    }
//
//
//                    platInfo = plat.getName();
//                    nickName = platDB.getUserName();
//                    headUrl = platDB.getUserIcon();
//
//                    //平台信息
//                    if (platInfo.equals(SinaWeibo.NAME)) {
//                        platInfo = "WEIBO";
//                    } else if (platInfo.equals(Wechat.NAME)) {
//                        platInfo = "WECHAT";
//                    } else if (platInfo.equals(QQ.NAME)) {
//                        platInfo = "QQ";
//                        //qq优先取空间头像
//                        headUrl = res.get("figureurl_1").toString();
//                        LogUtil.info("figureurl_1 " + res.get("figureurl_1").toString());
//                    }
//
//                    LogUtil.info("platInfo: " + platInfo);
//                    LogUtil.info("nickname: " + nickName);
//                    LogUtil.info("headUrl: " + headUrl);
//                    LogUtil.info(platDB.getUserIcon());
//                    //通过DB获取各种数据
////                    LogUtil.info(platDB.getToken());
////                    LogUtil.info(platDB.getUserGender());
//                    LogUtil.info(res.toString());
//                    /**
//                     * 应用拿到 uid、platInfo、nickname、headUrl后调用以下方法换取登录咪咕账号的token，需要应用主动调用
//                     */
//                    mAuthnHelper.getAccessTokenByThirdLogin(HostConfig.APP_ID, HostConfig.APP_KEY, uid, platInfo, nickName, headUrl, new TokenListener() {
//                        @Override
//                        public void onGetTokenComplete(JSONObject jsonobj) {
//                            parseResponseFromGetToken(jsonobj);
//                        }
//                    });
//                }
//
//            }
//
//            public void onError(Platform plat, int action, Throwable t) {
//                if (action == Platform.ACTION_USER_INFOR) {
//                    Log.i("doThirdLogin ", plat.getName() + " Login onError ...");
//                    dismissProgressDialog();
//
//                }
//                t.printStackTrace();
//            }
//
//            public void onCancel(Platform plat, int action) {
//                if (action == Platform.ACTION_USER_INFOR) {
//                    Log.i("doThirdLogin ", plat.getName() + " Login onCancel ...");
//                    dismissProgressDialog();
//                    Toast.makeText(MiguUITestActivity.this, "授权取消", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//        platform.showUser(null);
//
//    }

    /**
     * 第三方账号登录解析
     * 解析返回的json数据，若返回码是102000，则获取json中token；
     * 再回调mTokenProcess.parseToken(token)获取用户登录的状态。
     * @param json
     */
    private void parseResponseFromGetToken(final JSONObject json)
    {
        Log.d(TAG, "json : " + json.toString());

        if ( json == null )
        {
            Message msg = Message.obtain();
            msg.what = LOGIN_ERROR;
            msg.obj = "登录失败";
            mHandler.sendMessage(msg);
            return;
        }

        int resultCode = json.optInt(SsoSdkConstants.VALUES_KEY_RESULT_CODE, -1);

        if ( resultCode != AuthnConstants.CLIENT_CODE_SUCCESS )
        {
            Message msg = Message.obtain();
            msg.what = LOGIN_ERROR;
            msg.obj = json.optString(SsoSdkConstants.VALUES_KEY_RESULT_STRING);
            mHandler.sendMessage(msg);
            return;
        }

        String token = json.optString(SsoSdkConstants.VALUES_KEY_TOKEN);
        //拿到token后，进行token 校验
        JSONObject jsonResult = userCheckToken(token);
        Log.d( TAG, "Third Login tokenValidate " + jsonResult.toString());

        boolean result = jsonResult.optBoolean(MiguUIConstants.KEY_RESULT);
        //如果token校验成功，且是第三方登录。关闭登录页面
        if (result && isThirdLoginWay) {
            userName = jsonResult.optString(MiguUIConstants.KEY_LOGIN_ACCOUNT);
            nickName = jsonResult.optString("nickname");
            Log.d(TAG, "Login Success. " + "uid : " + userName + ", nickname : " + nickName);
            mHandler.sendEmptyMessage(LOGIN_THIRDPART);
            return;
        }

        int errorCode = jsonResult.optInt(MiguUIConstants.KEY_ERROR_CODE);
        String errorString = jsonResult.optString(MiguUIConstants.KEY_ERROR_STRING);
        Log.d(TAG, "Login Failed." + errorCode + "  " + errorString);
        mHandler.sendEmptyMessage(LOGIN_ERROR);

    }

    private void showProgressDialog (Context context) {


        dialog = new ProgressDialog(context);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMessage("Loading...");

        if(dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    private void dismissProgressDialog () {

        if(dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
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
