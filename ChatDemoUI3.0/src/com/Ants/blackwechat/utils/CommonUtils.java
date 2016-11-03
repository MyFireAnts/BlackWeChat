package com.Ants.blackwechat.utils;

import android.widget.Toast;

import com.Ants.blackwechat.I;
import com.Ants.blackwechat.R;
import com.Ants.blackwechat.DemoApplication;

public class CommonUtils {
    public static void showLongToast(String msg) {
        Toast.makeText(DemoApplication.applicationContext, msg, Toast.LENGTH_LONG).show();
    }

    public static void showShortToast(String msg) {
        Toast.makeText(DemoApplication.applicationContext, msg, Toast.LENGTH_SHORT).show();
    }

    public static void showLongToast(int rId) {
        showLongToast(DemoApplication.applicationContext.getString(rId));
    }

    public static void showShortToast(int rId) {
        showShortToast(DemoApplication.applicationContext.getString(rId));
    }

    public static void showMsgShortToast(int msgId) {
        if (msgId > 0) {
            showShortToast(DemoApplication.getInstance().getResources()
                    .getIdentifier(I.MSG_PREFIX_MSG + msgId, "string",
                            DemoApplication.getInstance().getPackageName()));
        } else {
            showShortToast(R.string.msg_1);
        }
    }
}
