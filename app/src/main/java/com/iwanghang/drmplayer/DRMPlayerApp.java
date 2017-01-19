package com.iwanghang.drmplayer;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.iwanghang.drmplayer.utils.Constant;
import com.lidroid.xutils.DbUtils;

/**
 * Created by iwanghang on 16/4/26.
 */

//application：整个应用程序得全局对象，所以该应用中的所有组件(activity、fragment，服务，广播等)都可以使用
//该类中的对象和方法,相当于C语言中的全局变量,可以把一些公共的东西都放在这里面

//每次使用getApplication()就是指该Application,
//继承Application需要再清单文件中添加如下,
//<!--android:name=".DRMPlayerApp"表示使用自己定义的application，而不是系统默认的application-->
//        <application
//        android:name=".DRMPlayerApp"

public class DRMPlayerApp extends Application{

    //SharedPreferences是Android平台上一个轻量级的存储类，用来保存应用的一些常用配置
    public static SharedPreferences sp;//SharedPreferences 直译为 共享偏好,静态，可通过类名直接使用该变量
    //xutils 用于音乐收藏数据库 https://github.com/wyouflf/xUtils
    public static DbUtils dbUtils;//声明全局的变量
    //context 上下文 提供给AppUtils获取上下文
    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();

        //实例化,存储名称为SP_NAME,存储模式为私有
        sp = getSharedPreferences(Constant.SP_NAME, Context.MODE_PRIVATE);
        //目的,比如在退出Activity时,保存循环模式,歌曲位置(第几首歌曲)
        //这里,我在MainActivity的onDestroy时,调用SharedPreferences,保存进度值

        //实例化,存储名称为DB_NAME
        dbUtils = DbUtils.create(getApplicationContext(),Constant.DB_NAME);

        //context 上下文 提供给AppUtils获取上下文
        context = getApplicationContext();
    }
}
