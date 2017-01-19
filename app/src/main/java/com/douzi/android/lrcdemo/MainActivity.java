package com.douzi.android.lrcdemo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import com.douzi.android.view.DefaultLrcBuilder;
import com.douzi.android.view.ILrcBuilder;
import com.douzi.android.view.ILrcView;
import com.douzi.android.view.ILrcView.LrcViewListener;
import com.douzi.android.view.LrcRow;
import com.douzi.android.view.LrcView;
import com.iwanghang.drmplayer.R;
import com.iwanghang.drmplayer.utils.Constant;

public class MainActivity extends Activity {

	public final static String TAG = "MainActivity";
	ILrcView mLrcView;
    private int mPalyTimerDuration = 1000;
    private Timer mTimer;
    private TimerTask mTask;

    public String getFromAssets(String fileName){//文件夹中获取文件并读取数据
        try {
            InputStreamReader inputReader = new InputStreamReader( getResources().getAssets().open(fileName) );
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line="";
            String Result="";
            while((line = bufReader.readLine()) != null){
            	if(line.trim().equals(""))
            		continue;
            	Result += line + "\r\n";
            }
            return Result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLrcView = new LrcView(this, null);
        setContentView((View) mLrcView);


        File LrcDirFile = new File(Environment.getExternalStorageDirectory() + Constant.DIR_LRC);
        System.out.println("LrcDirFile : " + LrcDirFile);
        if (!LrcDirFile.exists()) {
            LrcDirFile.mkdirs();
        }
        String lrc = LrcDirFile + "/" + "山丘" + ".lrc";


        //file:///android_asset/test.lrc;
        //String lrc = getFromAssets(Environment.getExternalStorageDirectory()+ Constant.DIR_MUSIC) + musicName + ".lrc";
        //String lrc = getFromAssets("file:///android_asset/test.lrc");
        Log.d(TAG, "lrc:" + lrc);

        ILrcBuilder builder = new DefaultLrcBuilder();
        List<LrcRow> rows = builder.getLrcRows(lrc);

        mLrcView.setLrc(rows);
        beginLrcPlay();

        mLrcView.setListener(new LrcViewListener() {

			public void onLrcSeeked(int newPosition, LrcRow row) {
				if (mPlayer != null) {
					Log.d(TAG, "onLrcSeeked:" + row.time);
					mPlayer.seekTo((int)row.time);
				}
			}
		});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if (mPlayer != null) {
    		mPlayer.stop();
    	}
    }


    MediaPlayer mPlayer;
    public void beginLrcPlay(){

    	mPlayer = new MediaPlayer();
    	try {
            /**
             播放assets目录下的音乐

             首先，获取通过openFd()的方法获取asset目录下指定文件的AssetFileDescriptor对象。
             其次，通过MediaPlayer对象的setDataSource (FileDescriptorfd,longoffset, long length)方法加载音乐文件。

             最后，调用prepare方法准备音乐，start方法开始播放音乐。

             预备知识：

             AssetFileDescriptor简介：

             在AssetManager中一项的文件描述符。这提供你自己打开的FileDescriptor可用于读取的数据，以及在文件中的

             偏移量和长度的该项的数据。

             可以通过AssetManager的openFd()的方法获取asset目录下指定文件的AssetFileDescriptor对象。
             */
    		mPlayer.setDataSource(getAssets().openFd("m.mp3").getFileDescriptor());
    		mPlayer.setOnPreparedListener(new OnPreparedListener() {

				public void onPrepared(MediaPlayer mp) {
					Log.d(TAG, "onPrepared");
					mp.start();
			        if(mTimer == null){
			        	mTimer = new Timer();
			        	mTask = new LrcTask();
			        	mTimer.scheduleAtFixedRate(mTask, 0, mPalyTimerDuration);
			        }
				}
			});
    		mPlayer.setOnCompletionListener(new OnCompletionListener() {

				public void onCompletion(MediaPlayer mp) {
					stopLrcPlay();
				}
			});
    		mPlayer.prepare();
    		mPlayer.start();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

    }

    public void stopLrcPlay(){
        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }
    }

    class LrcTask extends TimerTask{

        long beginTime = -1;

        @Override
        public void run() {
            if(beginTime == -1) {
                beginTime = System.currentTimeMillis();
            }

            final long timePassed = mPlayer.getCurrentPosition();
            MainActivity.this.runOnUiThread(new Runnable() {

                public void run() {
                    mLrcView.seekLrcToTime(timePassed);
                }
            });

        }
    };
}
