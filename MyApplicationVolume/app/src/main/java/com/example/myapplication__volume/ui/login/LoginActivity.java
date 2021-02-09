package com.example.myapplication__volume.ui.login;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.datastore.PreferenceLogin;
import com.example.myapplication__volume.MainActivity;
import com.example.myapplication__volume.Nim.DemoCache;
import com.example.myapplication__volume.R;
import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nim.uikit.common.ToastHelper;
import com.netease.nim.uikit.common.util.log.LogUtil;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.auth.LoginInfo;

public class LoginActivity extends AppCompatActivity {

    private static final String KICK_OUT = "KICK_OUT";
    private static final String KICK_OUT_DESC = "KICK_OUT_DESC";

    private LoginViewModel loginViewModel;

    EditText usernameEditText;
    EditText passwordEditText;
    Button loginButton;
    ProgressBar loadingProgressBar;
    Button registerButton;
    CheckBox remember_pwd;
    PreferenceLogin preferenceLogin;

    private SoundPool soundPool;
    private int soundId;

    final private int SIGN_IN_ON_CLICK = 1;
    final private String TAG = "LoginActivity";

    @SuppressLint("HandlerLeak")
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case SIGN_IN_ON_CLICK:
                    loadingProgressBar.setVisibility(View.VISIBLE);
                    loginViewModel.login(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());
                    Log.d("LoginButton:", "onClickkkkkkkk");
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + msg.what);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginViewModel = ViewModelProviders.of(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        preferenceLogin = new PreferenceLogin(this);
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);
        loadingProgressBar = findViewById(R.id.loading);
        registerButton = findViewById(R.id.goto_register);
        remember_pwd = findViewById(R.id.remember_pwd);

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 5);
        soundId = soundPool.load(this, R.raw.button01, 1);

        if (preferenceLogin.getRem_or_not()){

            usernameEditText.setText(preferenceLogin.getUsername());
            passwordEditText.setText(preferenceLogin.getPassword());
            remember_pwd.setChecked(true);
            loginButton.setEnabled(true);

//            loginViewModel.login(usernameEditText.getText().toString(),
//                    passwordEditText.getText().toString());
        }

        loginViewModel.getLoginFormState().observe(this, new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
                loginButton.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null) {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null) {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });


        loginViewModel.getLoginResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                Log.d("LoginResultOnChanged", "innnnnn");
                if (loginResult == null) {
                    return;
                }
                loadingProgressBar.setVisibility(View.GONE);
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    Log.d("LoginResultOnChanged", "getSuccess");

//                    MainActivity.actionStart(LoginActivity.this, loginResult.getSuccess().getDisplayName());
//                    updateUiWithUser(loginResult.getSuccess());
//                    finish();
                    if (remember_pwd.isChecked()){
                        preferenceLogin.setPref(usernameEditText.getText().toString(),
                                passwordEditText.getText().toString(),true);
                    }else {
                        preferenceLogin.setPref("","",false);
                    }


                    loginNim(loginResult.getSuccess().getDisplayName(), passwordEditText.getText().toString(), loginResult);
                }
                setResult(Activity.RESULT_OK);

                //Complete and destroy login activity once successful
//                finish();
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };

        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
//                    loginViewModel.login(usernameEditText.getText().toString(),
//                            passwordEditText.getText().toString());
                    handler.sendEmptyMessage(SIGN_IN_ON_CLICK);
                }
                return false;
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);

                handler.sendEmptyMessage(SIGN_IN_ON_CLICK);
                if (remember_pwd.isChecked()){
                    preferenceLogin.setPref(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString(),true);
                }else {
                    preferenceLogin.setPref("","",false);
                }
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                RegisterActivity.actionStart(LoginActivity.this);
            }
        });
    }

    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName() + " !";
        // TODO : initiate successful logged in experience
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    public static void actionStart(Context context){
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }



    private void loginNim(String account, String password, LoginResult loginResult){
        LogUtil.i(TAG, "account: " + account);
        LogUtil.i(TAG, "password: " + password);

        NimUIKit.login(new LoginInfo(account, password),
                new RequestCallback<LoginInfo>() {

                    @Override
                    public void onSuccess(LoginInfo param) {
                        LogUtil.i(TAG, "login success");
                        DemoCache.setAccount(account);
                        NimUIKit.loginSuccess(account);

                        // 进入主界面
                        MainActivity.actionStart(LoginActivity.this, loginResult.getSuccess().getDisplayName());
                        updateUiWithUser(loginResult.getSuccess());
                        finish();
                    }

                    @Override
                    public void onFailed(int code) {
                        if (code == 302 || code == 404) {
                            ToastHelper.showToast(LoginActivity.this,
                                    R.string.login_failed);
                        } else if(code == 408) {
                            ToastHelper.showToast(LoginActivity.this,
                                    "连接超时： " + account);
                        }else {
                            ToastHelper.showToast(LoginActivity.this,
                                    "登录失败: " + code);
                        }
                    }

                    @Override
                    public void onException(Throwable exception) {
                        ToastHelper.showToast(LoginActivity.this,
                                "Exception When Login !");
                    }
                });
    }



    public static void start(Context context) {
        start(context, false, "");
    }

    public static void start(Context context, boolean kickOut, String kickOutDesc) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(KICK_OUT, kickOut);
        intent.putExtra(KICK_OUT_DESC, kickOutDesc);
        context.startActivity(intent);
    }



}