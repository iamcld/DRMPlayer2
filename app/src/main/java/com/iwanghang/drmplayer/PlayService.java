package com.iwanghang.drmplayer;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.douzi.android.view.ILrcView;
import com.douzi.android.view.LrcRow;
import com.douzi.android.view.LrcView;
import com.iwanghang.drmplayer.utils.MediaUtils;
import com.iwanghang.drmplayer.vo.Mp3Info;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.iwanghang.drmplayer.PlayActivity.*;

/**
 * 音乐播放的服务组件
 * 实现功能:
 * 播放
 * 暂停
 * 下一首
 * 上一首
 * 获取当前歌曲的播放进度
 * <p/>
 * 需要在AndroidManifest.xml添加以下代码:
 * <service
 * android:name=".PlayService"
 * android:enabled="true"
 * android:exported="true">
 * </service>
 * <p/>
 * 实现功能(播放模式play_mode):
 * 顺序播放
 * 随机播放
 * 单曲循环
 */

//谁要用service谁就去绑定它，从而可以获得service中的方法,不用了就解绑
//因为在playActivity和MyMusicListFragment中都会显示播放，暂停，下一首等控制音乐状态的控件，为了同步这些控件的状态，即
//假如在playActivity中按下暂停，则MyMusicListFragment中的图标也应该是暂停的状态，所以可以在service中来处理这些公共的状态.
//这个时候playActivity和MyMusicListFragment就得去绑定该PlayService，从而可以得到play,pause等方法,即体现出绑定服务的作用
public class PlayService extends Service implements OnCompletionListener, OnErrorListener {
    private MediaPlayer mPlayer;
    private int currentPosition;//当前正在播放的歌曲的位置
    ArrayList<Mp3Info> mp3Infos;

    private MusicUpdatrListener musicUpdatrListener;

    //歌词
    private LrcView lrcView;// 自定义歌词视图
    ILrcView mLrcView;
    public final static String TAG = "PlayActivity";
    private int mPalyTimerDuration = 1000;
    private Timer mTimer;
    private TimerTask mTask;


    //创建一个单实力的线程,用于更新音乐信息
    private ExecutorService es = Executors.newSingleThreadExecutor();

    //播放模式
    public static final int ORDER_PLAY = 1;//顺序播放
    public static final int RANDOM_PLAY = 2;//随机播放
    public static final int SINGLE_PLAY = 3;//单曲循环
    private int play_mode = ORDER_PLAY;//播放模式,默认为顺序播放

    /**
     * @param play_mode ORDER_PLAY = 1;//顺序播放
     *                  RANDOM_PLAY = 2;//随机播放
     *                  SINGLE_PLAY = 3;//单曲循环
     */
    //set方法
    public void setPlay_mode(int play_mode) {
        this.play_mode = play_mode;
    }

    //get方法
    public int getPlay_mode() {
        return play_mode;
    }

    private boolean isPause = false;//歌曲播放中的暂停状态
    public boolean isPause() {
        return isPause;
    }

    //切换播放列表,默认MY_MUSIC_LIST
    public static final int MY_MUSIC_LIST = 1;//本地音乐
    public static final int LOVE_MUSIC_LIST = 2;//收藏音乐
    public static final int RECORD_MUSIC_LIST = 3;//最近播放
    private int changePlayList = MY_MUSIC_LIST;
    public int getChangePlayList() {//get方法
        return changePlayList;
    }
    public void setChangePlayList(int changePlayList) {//set方法
        this.changePlayList = changePlayList;
    }

    public PlayService() {
    }

    //ArrayList<Mp3Info> mp3Infos; set方法
    public void setMp3Infos(ArrayList<Mp3Info> mp3Infos) {
        this.mp3Infos = mp3Infos;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    private Random random = new Random();//创建随机对象

    //MediaPlayer.Completion 播放完成 实现播放下一首功能
    //播放完成以后,判断播放模式(曲目循环方式)
    //为了实现循环后,可以显示音乐信息,需要在PlayAcivity(随机 ，顺序，单曲等控件是在这个activity中)的change里添加对应代码
    @Override
    public void onCompletion(MediaPlayer mp) {
        switch (play_mode) {
            case ORDER_PLAY://顺序播放
                next();//下一首
                break;
            case RANDOM_PLAY://随机播放
                //currentPosition = random.nextInt(mp3Infos.size());//随机下标为mp3Infos.size()
                //play(currentPosition);
                play(random.nextInt(mp3Infos.size()));
                break;
            case SINGLE_PLAY://单曲循环
                play(currentPosition);
                break;
            default:
                break;
        }
    }

    //MediaPlayer.Error 播放错误 处理实现播放下一首功能出现的错误
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();//重启
        return false;
    }

    //内部类PlayBinder实现Binder,得到当前PlayService对象
    //因为onBind方法返回的就是一个IBinder
    //谁绑定service，谁就能得到这个service的对象(技巧)
    class PlayBinder extends Binder {
        public PlayService getPlayService() {
            System.out.println("PlayService #1 " + PlayService.this);
            return PlayService.this;//得到当前PlayService对象，这样activity或fragment就可以获得这个
                                    //PlayService对象，从而可以调用该PlayService的方法了
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new PlayBinder();//通过PlayBinder拿到PlayService,给Activity调用
    }

    public ArrayList<Mp3Info> getMp3Infos() {
        return mp3Infos;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //恢复状态值
        DRMPlayerApp app = (DRMPlayerApp) getApplication();
        currentPosition = app.sp.getInt("currentPosition", 0);
        //currentPosition = DRMPlayerApp.sp.getInt("currentPosition", 0);//静态，直接通过类名访问
        play_mode = app.sp.getInt("play_mode", PlayService.ORDER_PLAY);
        //创建DRMPlayerApp继承Application,同时需要在把AndroidManiFest中的public换成DRMPlayerApp
        //在DRMPlayerApp的onCreate中 实例化 SharedPreferences
        //在MainActivity的onDestroy中 保存状态值
        //在PlayService的onCreate中 恢复状态值

        mPlayer = new MediaPlayer();
        mPlayer.setOnCompletionListener(this);//注册播放完成事件
        mPlayer.setOnErrorListener(this);//注册播放错误事件
        mp3Infos = MediaUtils.getMp3Infos(this);//获取Mp3列表
        System.out.println("PlayService.onCreate.mp3Infos = " + mp3Infos);
        es.execute(updateSteatusRunnable);//更新进度值


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //回收线程
        if (es != null && !es.isShutdown()) {//当进度值等于空,并且,进度值没有关闭
            es.shutdown();
            es = null;
        }
        mPlayer =null;
        mp3Infos = null;
        musicUpdatrListener = null;

    }

    //利用Runnable来实现多线程
    /**
     * Runnable
     * Java中实现多线程有两种途径:继承Thread类或者实现Runnable接口.
     * Runnable接口非常简单,就定义了一个方法run(),继承Runnable并实现这个
     * 方法就可以实现多线程了,但是这个run()方法不能自己调用,必须由系统来调用,否则就和别的方法没有什么区别了.
     * 好处:数据共享
     */
    Runnable updateSteatusRunnable = new Runnable() {//更新状态
        @Override
        public void run() {
            //不断更新进度值
            while (true) {
                //音乐更新监听不为空,并且,媒体播放不为空,并且媒体播放为播放状态
                if (musicUpdatrListener != null && mPlayer != null && mPlayer.isPlaying()) {
                    musicUpdatrListener.onPublish(getCurrentProgress());//获取当前的进度值
                }
                try {
                    Thread.sleep(500);//500毫秒更新一次，时间和进度条
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    //播放
    public void play(int position) {
        mp3Infos = MediaUtils.getMp3Infos(this);//获取Mp3列表
        System.out.println("PlayService.play.position1 = " + position);
        System.out.println("PlayService.play.mp3Infos.size1 = " + mp3Infos.size());
        Mp3Info mp3Info;
        if (position >= 0 && position < mp3Infos.size()) {
            System.out.println("PlayService.play.position2 = " + position);
        } else {
            position = 0;
            System.out.println("PlayService.play.position3 = " + position);
        }
        System.out.println("PlayService.play.mp3Infos = " + mp3Infos);
        if (mp3Infos == null){
            return;
        }
        System.out.println("PlayService.play.position4 = " + position);
        mp3Info = mp3Infos.get(position);//获取mp3Info对象
        System.out.println("PlayService.play.mp3Info = " + mp3Info);
        //进行播放,播放前判断
        try {
            mPlayer.reset();//重启
            mPlayer.setDataSource(this, Uri.parse(mp3Info.getUrl()));//资源解析,Mp3地址
            mPlayer.prepare();//准备
            mPlayer.start();//开始(播放)
            currentPosition = position;//保存当前位置到currentPosition,比如第一首,currentPosition = 1
        } catch (IOException e) {
            e.printStackTrace();
        }

        //只要ui界面状态一改变就调用接口里面的事件
        if (musicUpdatrListener != null) {
            musicUpdatrListener.onChange(currentPosition);//更新当前位置
        }
    }


    //暂停
    public void pause() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
            isPause = true;
        }
    }

    //下一首
    public void next() {
        if (currentPosition >= mp3Infos.size() - 1) {//如果超出最大值,(因为第一首是0),说明已经是最后一首
            currentPosition = 0;//回到第一首
        } else {
            currentPosition++;//下一首
        }
        play(currentPosition);
    }

    //上一首 previous
    public void prev() {
        if (currentPosition - 1 < 0) {//如果上一首小于0,说明已经是第一首
            currentPosition = mp3Infos.size() - 1;//回到最后一首
        } else {
            currentPosition--;//上一首
        }
        play(currentPosition);
    }

    //默认开始播放的方法
    public void start() {
        if (mPlayer != null && !mPlayer.isPlaying()) {//判断当前歌曲不等于空,并且没有在播放的状态
            mPlayer.start();

        }
    }

    //获取当前是否为播放状态,提供给MyMusicListFragment的播放暂停按钮点击事件判断状态时调用
    public boolean isPlaying() {
        if (mPlayer != null) {
            return mPlayer.isPlaying();
        }
        return false;
    }

    //获取当前的进度值
    public int getCurrentProgress() {
        if (mPlayer != null && mPlayer.isPlaying()) {//mPlayer不为空,并且,为播放状态
            return mPlayer.getCurrentPosition();
        }
        return 0;
    }

    //getDuration 获取文件的持续时间
    public int getDuration() {
        return mPlayer.getDuration();
    }

    //seekTo 寻找指定的时间位置 (跳到某个时间点进行播放)
    public void seekTo(int msec) {
        mPlayer.seekTo(msec);
    }

    //2、观察监听者模式
    //更新UI状态的接口(PlayService的内部接口),并在BaseActivity中实现
    public interface MusicUpdatrListener {//音乐更新监听器

        public void onPublish(int progress);//发表进度事件(更新进度条)

        public void onChange(int position); //更新歌曲位置.按钮的状态等信息
        //声明MusicUpdatrListener后,添加set方法
    }

    //set方法
    public void setMusicUpdatrListener(MusicUpdatrListener musicUpdatrListener) {
        this.musicUpdatrListener = musicUpdatrListener;
    }



    //歌词

    public void stopLrcPlay(){
        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }
    }

}
