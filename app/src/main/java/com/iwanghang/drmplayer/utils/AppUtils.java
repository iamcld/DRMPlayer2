package com.iwanghang.drmplayer.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.iwanghang.drmplayer.DRMPlayerApp;

/**
 * Created by iwanghang on 16/4/30.
 * APP工具类
 * 在DRMPlayerApp创建context上下文,在这里获取并调用
 */
public class AppUtils {
    //隐藏键盘
    public static void hideInputMethod(View view){
        InputMethodManager imm = (InputMethodManager) DRMPlayerApp.context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()){//判断键盘是否经激活(弹出)
            imm.hideSoftInputFromWindow(view.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);//隐藏键盘
        }
    }
}
