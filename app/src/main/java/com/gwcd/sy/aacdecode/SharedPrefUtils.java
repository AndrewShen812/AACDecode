/**
 * 项目名称：AACDecode
 * 创建日期：2016年05月31日
 * Copyright 2016 GALAXYWIND Network Systems Co.,Ltd.All rights reserved.
 */
package com.gwcd.sy.aacdecode;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * 类描述：<br>
 * 创建者：shenyong<br>
 * 创建时间：2016/5/31<br>
 * 修改记录：<br>
 */
public class SharedPrefUtils {

    private static final String KEY_LAST_PATH = "last_input_path";

    private SharedPreferences mSp;

    private static SharedPrefUtils mInstance;

    public static SharedPrefUtils newInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SharedPrefUtils(context);
        }

        return mInstance;
    }

    private SharedPrefUtils(Context context) {
        mSp = context.getApplicationContext().getSharedPreferences("temp_input", Activity.MODE_PRIVATE);
    }

    public String getLastInput() {
        return mSp.getString(KEY_LAST_PATH, "/storage/emulated/0/aac-pcm/test.aac");
    }

    public void setLastInput(String path) {
        mSp.edit().putString(KEY_LAST_PATH, path).commit();
    }
}
