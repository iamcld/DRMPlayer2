package com.iwanghang.drmplayer;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.iwanghang.drmplayer.utils.Constant;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by iwanghang on 16/4/19.
 * MainActivity继承BaseActivity,实现APP所有绑定功能
 * 继承后,直接调用子类方法,就可以进行绑定和解除,bindPlayService,unbindPlayService
 *
 * 1、模板设计模式,给FragmentActivity做了一个抽象,用来绑定服务
 *
 *模板方法模式:baseActivity和MainActivity
 *
 * 所有的activity都继承这个baseactivity，所有activity的父类
 */
public abstract class BaseActivity extends FragmentActivity{

    protected PlayService playService;
    private boolean isBound = false;//是否已经绑定

    private ArrayList<Activity> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        list.add(this);

    }

    //全部activity的退出实现,其他activity都可以调用该方法来实现全部activity的退出功能
    public void exit(){
        for (int i = 0; i<list.size(); i++){
            list.get(i).finish();
        }
    }

    //绑定Service
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {//服务连接
            //转换
            PlayService.PlayBinder playBinder = (PlayService.PlayBinder) service;
            //绑定播放服务
            playService = playBinder.getPlayService();
            
            //设置监听器
            playService.setMusicUpdatrListener(musicUpdatrListener);
            //真正调用的是PlayActivity里面change()
            //谁一打开，谁就会马上去更新ui
            musicUpdatrListener.onChange(playService.getCurrentPosition());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {//服务断开
            playService = null;
            isBound = false;
        }
    };

    //实现MusicUpdatrListener的相关方法,把PlayService.MusicUpdatrListener的具体内容,
    // 延迟到子类来具体实现(把具体的操作步骤在子类中实现)
    private PlayService.MusicUpdatrListener musicUpdatrListener = new PlayService.MusicUpdatrListener() {
        @Override
        public void onPublish(int progress) {
            publish(progress);
        }

        @Override
        public void onChange(int progress) {
            change(progress);
        }
    };
    //抽象类(目的是让子类来具体实现,用于更新UI)
    //强调子类一定要实现该方法，从而来更新UI
    public abstract void publish(int progress);
    public abstract void change(int progress);



    //绑定服务(子类决定什么时候调用,比如在onCreate时调用,或者在onResume,onPause时调用)
    public void bindPlayService(){
        if(!isBound) {
            Intent intent = new Intent(this, PlayService.class);
            bindService(intent, conn, Context.BIND_AUTO_CREATE);
            isBound = true;
        }
    }
    //解绑服务(子类决定什么时候调用,比如在onCreate时调用,或者在onResume,onPause时调用)
    public void unbindPlayService(){
        if(isBound) {
            unbindService(conn);
            isBound = false;
        }
    }

    //点击事件
    public void onClick(View v){

    }

}
