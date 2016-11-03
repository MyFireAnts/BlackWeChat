/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.Ants.blackwechat.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.Ants.blackwechat.DemoApplication;
import com.Ants.blackwechat.DemoHelper;
import com.Ants.blackwechat.R;
import com.Ants.blackwechat.bean.Result;
import com.Ants.blackwechat.data.NetDao;
import com.Ants.blackwechat.data.OkHttpUtils;
import com.Ants.blackwechat.db.DemoDBManager;
import com.Ants.blackwechat.db.UserDao;
import com.Ants.blackwechat.utils.L;
import com.Ants.blackwechat.utils.MD5;
import com.Ants.blackwechat.utils.MFGT;
import com.Ants.blackwechat.utils.ResultUtils;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.utils.EaseCommonUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Login screen
 *
 */
public class LoginActivity extends BaseActivity {
    private static final String TAG = "LoginActivity";
    public static final int REQUEST_CODE_SETNICK = 1;
    @BindView(R.id.img_back)
    ImageView mImgBack;
    @BindView(R.id.txt_title)
    TextView mTxtTitle;
    @BindView(R.id.username)
    EditText mEtUsername;
    @BindView(R.id.password)
    EditText mEtPassword;

    private boolean progressShow;
    private boolean autoLogin = false;

    String currentUsername;
    String currentPassword;
    ProgressDialog pd;
    LoginActivity mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // enter the main activity if already logged in
        if (DemoHelper.getInstance().isLoggedIn()) {
            autoLogin = true;
            startActivity(new Intent(LoginActivity.this, MainActivity.class));

            return;
        }
        setContentView(R.layout.em_activity_login);
        ButterKnife.bind(this);

        setListener();
        initView();
        mContext = this;

    }

    private void initView() {
        if (DemoHelper.getInstance().getCurrentUsernName() != null) {
            mEtUsername.setText(DemoHelper.getInstance().getCurrentUsernName());
        }
        mImgBack.setVisibility(View.VISIBLE);
        mTxtTitle.setVisibility(View.VISIBLE);
        mTxtTitle.setText(R.string.login);
    }

    private void setListener() {
        // if user changed, clear the password
        mEtUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mEtPassword.setText(null);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    /**
     * login
     */
    public void login() {
        if (!EaseCommonUtils.isNetWorkConnected(this)) {
            Toast.makeText(this, R.string.network_isnot_available, Toast.LENGTH_SHORT).show();
            return;
        }
        currentUsername = mEtUsername.getText().toString().trim();
        currentPassword = mEtPassword.getText().toString().trim();

        if (TextUtils.isEmpty(currentUsername)) {
            Toast.makeText(this, R.string.User_name_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(this, R.string.Password_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        progressShow = true;
        pd = new ProgressDialog(mContext);
        pd.setCanceledOnTouchOutside(false);
        pd.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, "EMClient.getInstance().onCancel");
                progressShow = false;
            }
        });
        pd.setMessage(getString(R.string.Is_landing));
        pd.show();

        loginEMServer();
    }

    private void loginEMServer() {
        // After logout，the DemoDB may still be accessed due to async callback, so the DemoDB will be re-opened again.
        // close it before login to make sure DemoDB not overlap
        DemoDBManager.getInstance().closeDB();

        // reset current user name before login
        DemoHelper.getInstance().setCurrentUserName(currentUsername);

        final long start = System.currentTimeMillis();
        // call login method
        Log.d(TAG, "EMClient.getInstance().login");
        EMClient.getInstance().login(currentUsername, MD5.getMessageDigest(currentPassword), new EMCallBack() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "login: onSuccess");

                loginAppServer();
            }

            @Override
            public void onProgress(int progress, String status) {
                Log.d(TAG, "login: onProgress");
            }

            @Override
            public void onError(final int code, final String message) {
                Log.d(TAG, "login: onError: " + code);
                if (!progressShow) {
                    return;
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        pd.dismiss();
                        Toast.makeText(getApplicationContext(), getString(R.string.Login_failed) + message,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void loginAppServer() {
        NetDao.login(mContext, currentUsername, currentPassword, new OkHttpUtils.OnCompleteListener<String>() {
            @Override
            public void onSuccess(String s) {
                L.e(TAG,"s="+s);
                if(s!=null && s!=""){
                    Result result = ResultUtils.getResultFromJson(s, User.class);
                    if(result!=null && result.isRetMsg()){
                        User user = (User) result.getRetData();
                        if(user!=null) {
                            UserDao dao = new UserDao(mContext);
                            dao.saveAppContact(user);
                            loginSuccess();
                        }
                    }else{
                        pd.dismiss();
                        L.e(TAG,"login fail,"+result);
                    }
                }else{
                    pd.dismiss();
                }
            }

            @Override
            public void onError(String error) {
                pd.dismiss();
                L.e(TAG,"onError="+error);
            }
        });
    }

    private void loginSuccess() {
        // ** manually load all local groups and conversation
        EMClient.getInstance().groupManager().loadAllGroups();
        EMClient.getInstance().chatManager().loadAllConversations();

        // update current user's display name for APNs
        boolean updatenick = EMClient.getInstance().updateCurrentUserNick(
                DemoApplication.currentUserNick.trim());
        if (!updatenick) {
            Log.e("LoginActivity", "update current user nick fail");
        }

        if (!LoginActivity.this.isFinishing() && pd.isShowing()) {
            pd.dismiss();
        }
        // get user's info (this should be get from App's server or 3rd party service)
        DemoHelper.getInstance().getUserProfileManager().asyncGetCurrentUserInfo();

        Intent intent = new Intent(LoginActivity.this,
                MainActivity.class);
        startActivity(intent);

        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (autoLogin) {
            return;
        }
        if (DemoHelper.getInstance().getCurrentUsernName() != null) {
            mEtUsername.setText(DemoHelper.getInstance().getCurrentUsernName());
        }
    }

    @OnClick({R.id.img_back, R.id.btn_login, R.id.btn_register})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.img_back:
                MFGT.finish(this);
                break;
            case R.id.btn_login:
                login();
                break;
            case R.id.btn_register:
                MFGT.gotoRegister(this);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(pd!=null) {
            pd.dismiss();
        }
    }

}


/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *//*

package com.Ants.blackwechat.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.Ants.blackwechat.DemoApplication;
import com.Ants.blackwechat.DemoHelper;
import com.Ants.blackwechat.R;
import com.Ants.blackwechat.bean.Result;
import com.Ants.blackwechat.data.NetDao;
import com.Ants.blackwechat.data.OkHttpUtils;
import com.Ants.blackwechat.db.DemoDBManager;
import com.Ants.blackwechat.db.UserDao;
import com.Ants.blackwechat.utils.L;
import com.Ants.blackwechat.utils.MFGT;
import com.Ants.blackwechat.utils.ResultUtils;
import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.utils.EaseCommonUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

*/
/**
 * Login screen -->登录页面
 *//*

public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";
    public static final int REQUEST_CODE_SETNICK = 1;
    @BindView(R.id.img_back)
    ImageView imgBack;
    @BindView(R.id.txt_title)
    TextView txtTitle;

    LoginActivity mContext;

    private EditText usernameEditText;
    private EditText passwordEditText;

    String currentUsername;
    String currentPassword;

    ProgressDialog pd;

    private boolean progressShow;
    private boolean autoLogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // enter the main activity if already logged in -->如果已经登录，则直接进入到首页
        if (DemoHelper.getInstance().isLoggedIn()) {
            autoLogin = true;
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            return;
        }

        setContentView(R.layout.em_activity_login);
        ButterKnife.bind(this);

        usernameEditText = (EditText) findViewById(R.id.username);
        passwordEditText = (EditText) findViewById(R.id.password);

        // if user changed, clear the password -->如果用户更改，请清除密码
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordEditText.setText(null);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //判断当前用户名是否为空，如果不为空，则获取当前用户名称并将用户名设置显示在usernameEditText
        if (DemoHelper.getInstance().getCurrentUsernName() != null) {
            usernameEditText.setText(DemoHelper.getInstance().getCurrentUsernName());
        }

        initView();

        mContext = this;
    }

    //显示标题头返回按钮、及文本框，并设置标题名称
    private void initView() {
        imgBack.setVisibility(View.VISIBLE);
        txtTitle.setVisibility(View.VISIBLE);
        txtTitle.setText(R.string.login);
    }

    */
/**
     * login -->点击登录按钮
     *
     * @param view
     *//*

    public void login(View view) {

        //判断网络状态
        if (!EaseCommonUtils.isNetWorkConnected(this)) {
            Toast.makeText(this, R.string.network_isnot_available, Toast.LENGTH_SHORT).show();
            return;
        }

        //获取页面编辑的内容
        currentUsername = usernameEditText.getText().toString().trim();
        currentPassword = passwordEditText.getText().toString().trim();

        //判断输入的内容是否为空
        if (TextUtils.isEmpty(currentUsername)) {
            Toast.makeText(this, R.string.User_name_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(this, R.string.Password_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        progressShow = true;
        pd = new ProgressDialog(LoginActivity.this);
        pd.setCanceledOnTouchOutside(false);
        pd.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, "EMClient.getInstance().onCancel");
                progressShow = false;
            }
        });
        pd.setMessage(getString(R.string.Is_landing));
        pd.show();

        //在环信服务器上进行登录
        // After logout，the DemoDB may still be accessed due to async callback, so the DemoDB will be re-opened again.
        //注销后，由于异步回调，DemoDB可能仍然被访问，因此DemoDB将被重新打开。
        // close it before login to make sure DemoDB not overlap -->在登录前关闭它，以确保DemoDB不重叠
        DemoDBManager.getInstance().closeDB();

        // reset current user name before login -->在登录前重置当前用户名
        DemoHelper.getInstance().setCurrentUserName(currentUsername);

        final long start = System.currentTimeMillis();
        // call login method -->调用登录方法
        Log.d(TAG, "EMClient.getInstance().login");
        EMClient.getInstance().login(currentUsername, currentPassword, new EMCallBack() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "login: onSuccess");

                //在本地服务器上进行登录
                loginAppServer();
            }

            @Override
            public void onProgress(int progress, String status) {
                Log.d(TAG, "login: onProgress");
            }

            @Override
            public void onError(final int code, final String message) {
                Log.d(TAG, "login: onError: " + code);
                if (!progressShow) {
                    return;
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        pd.dismiss();
                        Toast.makeText(getApplicationContext(), getString(R.string.Login_failed) + message,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    //在本地服务器上进行登录
    private void loginAppServer() {
        NetDao.login(mContext, currentUsername, currentPassword, new OkHttpUtils.OnCompleteListener<String>() {
            @Override
            public void onSuccess(String result) {
                L.e(TAG, "onSuccess-->result: " + result);
                if (result != null && result != "") {
                    Result resultJSON = ResultUtils.getResultFromJson(result, User.class);
                    if (resultJSON != null && resultJSON.isRetMsg()) {
                        User user = (User) resultJSON.getRetData();
                        if (user != null) {
                            UserDao userDao = new UserDao(mContext);
                            userDao.saveAppContact(user);
                            //登录成功
                            loginSuccess();
                        }
                    } else {
                        pd.dismiss();
                        L.e(TAG, "login fail" + resultJSON);
                    }
                } else {
                    pd.dismiss();
                }
            }

            @Override
            public void onError(String error) {
                pd.dismiss();
                L.e(TAG, "onError" + error);
            }
        });
    }

    //登录成功后执行的方法
    private void loginSuccess() {

        // ** manually load all local groups and conversation -->手动加载所有本地组和对话
        EMClient.getInstance().groupManager().loadAllGroups();
        EMClient.getInstance().chatManager().loadAllConversations();

        // update current user's display name for APNs -->更新APN的当前用户的显示名称
        boolean updatenick = EMClient.getInstance().updateCurrentUserNick(
                DemoApplication.currentUserNick.trim());
        if (!updatenick) {
            Log.e("LoginActivity", "update current user nick fail");
        }

        if (!LoginActivity.this.isFinishing() && pd.isShowing()) {
            pd.dismiss();
        }
        // get user's info (this should be get from App's server or 3rd party service) -->获取用户信息（应该从应用服务器或第三方服务获取）
        DemoHelper.getInstance().getUserProfileManager().asyncGetCurrentUserInfo();

        //跳转到首页
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);

        finish();
    }


    */
/**
     * register -->点击注册按钮跳转到注册页面
     *
     * @param view
     *//*

    public void register(View view) {
        startActivityForResult(new Intent(this, RegisterActivity.class), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (autoLogin) {
            return;
        }
         if (DemoHelper.getInstance().getCurrentUsernName() != null) {
            mEtUsername.setText(DemoHelper.getInstance().getCurrentUsernName());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pd != null) {
            pd.dismiss();
        }
    }

    //点击标题头返回按钮
    @OnClick(R.id.img_back)
    public void onClick() {
        MFGT.finish(this);
    }
}
*/
