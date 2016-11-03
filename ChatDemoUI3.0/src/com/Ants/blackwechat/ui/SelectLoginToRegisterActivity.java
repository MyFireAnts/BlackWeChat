package com.Ants.blackwechat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.Ants.blackwechat.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class SelectLoginToRegisterActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_login_to_register);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.select_login_button, R.id.select_register_button})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.select_login_button:
                startActivity(new Intent(SelectLoginToRegisterActivity.this, LoginActivity.class));
                break;
            case R.id.select_register_button:
                startActivity(new Intent(SelectLoginToRegisterActivity.this, RegisterActivity.class));
                break;
        }
    }
}
