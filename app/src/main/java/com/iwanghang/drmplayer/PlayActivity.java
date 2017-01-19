package com.iwanghang.drmplayer;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.douzi.android.view.DefaultLrcBuilder;
import com.douzi.android.view.ILrcBuilder;
import com.douzi.android.view.ILrcView;
import com.douzi.android.view.LrcRow;
import com.douzi.android.view.LrcView;
import com.iwanghang.drmplayer.adapter.ViewPagerAdapter;
import com.iwanghang.drmplayer.utils.Constant;
import com.iwanghang.drmplayer.utils.DownloadUtils;
import com.iwanghang.drmplayer.utils.ImageUtils;
import com.iwanghang.drmplayer.utils.MediaUtils;
import com.iwanghang.drmplayer.utils.SearchMusicUtils;
import com.iwanghang.drmplayer.vo.Mp3Info;
import com.iwanghang.drmplayer.vo.SearchResult;
import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.exception.DbException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.iwanghang.drmplayer.PlayService.ORDER_PLAY;
import static com.iwanghang.drmplayer.PlayService.RANDOM_PLAY;
import static com.iwanghang.drmplayer.PlayService.SINGLE_PLAY;


/**
 * PlayActivity 点击MyMusicListFragment(本地音乐)底部UI中的专辑封面图片打开的Activity
 */
public class PlayActivity extends BaseActivity implements OnClickListener, OnSeekBarChangeListener ,OnPageChangeListener {
    private TextView textView2_title;//歌名
    private ImageView imageView1_ablum;//专辑封面图片
    private SeekBar seekBar1;//进度条
    private TextView textView1_start_time, textView1_end_time;//开始时间,结束时间
    private ImageView imageView1_play_mode;//菜单
    private ImageView imageView3_previous, imageView2_play_pause, imageView1_next;//上一首,播放暂停,下一首
    private ImageView imageView1_ablum_reflection;//专辑封面图片倒影
    private ImageView imageView1_favorite;//收藏按钮

    //private ArrayList<Mp3Info> mp3Infos;
    //private int position;//当前播放的位置
    private boolean isPause = false;//当前播放的是否为暂停状态
    private static final int UPDATE_TIME = 0x10;//更新播放事件的标记

    private DRMPlayerApp app;//取出全局对象 方便调用

    //歌词
    private ViewPager viewPager;
    private LrcView lrcView;// 自定义歌词视图
    private ArrayList<View> views = new ArrayList<>();
    private static final int UPDATE_LRC = 0x20;//更新播放事件的标记
    ILrcView mLrcView;
    public final static String TAG = "PlayActivity";
    private MediaPlayer mPlayer;
    private ViewPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_play);
        //取出全局对象 方便调用
        app = (DRMPlayerApp) getApplication();
        //初始化界面信息
        //textView2_title = (TextView) findViewById(R.id.textView2_title);//歌名
        //imageView1_ablum = (ImageView) findViewById(R.id.imageView1_ablum);//专辑封面图片
        seekBar1 = (SeekBar) findViewById(R.id.seekBar1);//进度条
        textView1_start_time = (TextView) findViewById(R.id.textView1_start_time);//开始时间
        textView1_end_time = (TextView) findViewById(R.id.textView1_end_time);//结束时间
        imageView1_play_mode = (ImageView) findViewById(R.id.imageView1_play_mode);//菜单
        imageView3_previous = (ImageView) findViewById(R.id.imageView3_previous);//上一首
        imageView2_play_pause = (ImageView) findViewById(R.id.imageView2_play_pause);//播放暂停
        imageView1_next = (ImageView) findViewById(R.id.imageView1_next);//下一首
        imageView1_favorite = (ImageView) findViewById(R.id.imageView1_favorite);//收藏按钮
        lrcView = (LrcView) findViewById(R.id.lrcView);//自定义歌词视图

        //注册按钮点击监听事件
        imageView1_play_mode.setOnClickListener(this);
        imageView2_play_pause.setOnClickListener(this);
        imageView3_previous.setOnClickListener(this);
        imageView1_next.setOnClickListener(this);
        seekBar1.setOnSeekBarChangeListener(this);
        imageView1_favorite.setOnClickListener(this);


        //mp3Infos = MediaUtils.getMp3Infos(this);
        //bindPlayService();//绑定服务,异步过程,绑定后需要取得相应的值,来更新界面
        myHandler = new MyHandler(this);

        //独立音乐播放界面 和 歌词界面
        //viewPager = (ViewPager) findViewById(R.id.fgv_player_main);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        initViewPager();//初始化
        

        //以下直接调用change()是不行的,因为异步问题,会发生NullPointerException空指针异常
        //从MyMusicListFragment的专辑封面图片点击时间传过来的position(当前播放的位置)
        //position = getIntent().getIntExtra("position",0);
        //change(position);

        //通过在BaseActivity中绑定Service,添加如下代码实现change()
        //musicUpdatrListener.onChange(playService.getCurrentProgeress());

        //从MyMusicListFragment的专辑封面图片点击时间传过来的isPause(当前播放的是否为暂停状态)
        //isPause = getIntent().getBooleanExtra("isPause",false);

        //        mLrcView = new LrcView(this, null);
        //        setContentView((View) mLrcView);
        //
        //        //在目标位置读取lrc文件
        //        File LrcDirFile = new File(Environment.getExternalStorageDirectory() + Constant.DIR_LRC);
        //        System.out.println("LrcDirFile : " + LrcDirFile);
        //        if (!LrcDirFile.exists()) {
        //            LrcDirFile.mkdirs();
        //        }
        //        //把lrc转成字符串
        //        String lrc = LrcDirFile + "/" + "山丘" + ".lrc";
        //        //Log.d的输出颜色是蓝色的，仅输出debug调试的意思，但他会输出上层的信息，过滤起来可以通过DDMS的Logcat标签来选择.
        //        Log.d(TAG, "lrc:" + lrc);
        //
        //        //把lrc的字符串 转成数组
        //        ILrcBuilder builder = new DefaultLrcBuilder();
        //        List<LrcRow> rows = builder.getLrcRows(lrc);
        //
        //        //把lrc数组 设置到mLrcView里
        //        mLrcView.setLrc(rows);
        //        //beginLrcPlay();
        //
        //        //设置监听器,监听歌词滚动
        //        mLrcView.setListener(new ILrcView.LrcViewListener() {
        //            public void onLrcSeeked(int newPosition, LrcRow row) {
        //                if (mPlayer != null) {
        //                    Log.d(TAG, "onLrcSeeked:" + row.time);
        //                    mPlayer.seekTo((int)row.time);//用户滑动歌词界面,调整进度
        //                }
        //            }
        //        });
    }


    private void initViewPager() {//专辑封面图片Pager与歌词Pager
        //View album_image_layout = getLayoutInflater().inflate(R.layout.album_image_layout, null);
        //System.out.println("PlayActivity.initViewPager.album_image_layout:" + album_image_layout);

        View album_image_layout = LayoutInflater.from(getApplicationContext()).inflate(R.layout.album_image_layout, null);
        System.out.println("PlayActivity.initViewPager.album_image_layout:" + album_image_layout);
        //初始化界面信息

        //textView2_title = (TextView) findViewById(R.id.textView2_title);//歌名
        //这里要注意下,直接findViewById,返回的是textView2_title是null;像下面这样,加上layout才可以,否则在change的时候会报空指针异常
        textView2_title = (TextView) album_image_layout.findViewById(R.id.textView2_title);//歌名
        //System.out.println("PlayActivity.initViewPager.textView2_title:" + textView2_title);
        //System.out.println("PlayActivity.initViewPager.textView2_title.getText:" + textView2_title.getText());


        imageView1_ablum = (ImageView) album_image_layout.findViewById(R.id.imageView1_ablum);//专辑封面图片

        imageView1_ablum_reflection = (ImageView) album_image_layout.findViewById(R.id.imageView1_ablum_reflection);//专辑封面图片倒影

        views.add(album_image_layout);//添加专辑封面图片Pager



        //View lrc_layout = getLayoutInflater().inflate(R.layout.lrc_layout, null);


        View lrc_layout = LayoutInflater.from(getApplicationContext()).inflate(R.layout.lrc_layout, null);
        System.out.println("PlayActivity.initViewPager.lrc_layout:" + lrc_layout);



        lrcView = (LrcView) lrc_layout.findViewById(R.id.lrcView);
        //设置滚动事件
        lrcView.setListener(new ILrcView.LrcViewListener() {
            @Override
            public void onLrcSeeked(int newPosition, LrcRow row) {
                if (playService.isPlaying()) {
                    playService.seekTo((int) row.time);
                }
            }
        });
        lrcView.setLoadingTipText("正在加载歌词......");
        lrcView.setBackgroundResource(R.mipmap.app_splash_bg);
        lrcView.getBackground().setAlpha(150);//背景透明度0-255
        views.add(lrc_layout);
        System.out.println("PlayActivity.initViewPager.views:" + views);



        //adapter = new ViewPagerAdapter(views);
        adapter = new ViewPagerAdapter(views);
        viewPager.setAdapter(adapter);




        //viewPager.setAdapter(new ViewPagerAdapter(views));
        System.out.println("PlayActivity.initViewPager.viewPager:" + viewPager);
        viewPager.addOnPageChangeListener(this);
        System.out.println("PlayActivity.initViewPager.viewPager:" + viewPager);
    }






    //把播放服务的绑定和解绑放在onResume,onPause里,好处是,每次回到当前Activity就获取一次播放状态，即更新UI界面的状态
    //当涉及到多个activity共同的的状态时，最好使用服务
    @Override
    protected void onResume() {
        super.onResume();
        bindPlayService();//绑定服务，目的是为了可以调用service中的方法或对象
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindPlayService();//解绑服务
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindPlayService();//解绑服务
    }

    //Handler用于更新已经播放时间
    private static MyHandler myHandler;

    //进度条改变 (fromUser 是否来自用户的改变 , 而不是程序自身控制的改变)
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //如果是来自用户的拖动
        if (fromUser) {
            //playService.pause();//暂停
            playService.seekTo(progress);//寻找指定的时间位置 (跳到某个时间点进行播放)
            //playService.start();//播放
        }
    }

    //进度条开始触摸，开始拖进度条
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    //进度条停止触摸
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    //页面滚动
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    //页面选择
    @Override
    public void onPageSelected(int position) {

    }

    //页面滚动状态改变
    @Override
    public void onPageScrollStateChanged(int state) {

    }


    static class MyHandler extends Handler {
        private PlayActivity playActivity;
        private WeakReference<PlayActivity> weak;//弱引用

        public MyHandler(PlayActivity playActivity) {
            //this.playActivity = playActivity;
            weak = new WeakReference<PlayActivity>(playActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //System.out.println("PlayActivity.MyHandler.weak = " + weak);
            playActivity = weak.get();
            //System.out.println("PlayActivity.MyHandler.playActivity = " + playActivity);
            if (playActivity != null) {
                switch (msg.what) {
                    case UPDATE_TIME://更新时间(已经播放时间)
                        playActivity.textView1_start_time.setText(MediaUtils.formatTime((int) msg.obj));
                        break;
                    case UPDATE_LRC:
                        playActivity.lrcView.seekLrcToTime((int) msg.obj);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void publish(int progress) {
        //以下是是直接调用线程,去做更新ui组件，但是不能这样做,会报错,线程异常,
        //textView1_start_time.setText(MediaUtils.formatTime(progress));//更新时间
        //所以,我们需要使用Handler
        //Message msg = myHandler.obtainMessage(UPDATE_TIME);//用于更新已经播放时间
        //msg.arg1 = progress;//用于更新已经播放时间
        //myHandler.sendMessage(msg);//用于更新已经播放时间

        //seekBar1.setProgress(progress);

        myHandler.obtainMessage(UPDATE_TIME, progress).sendToTarget();
        seekBar1.setProgress(progress);
        myHandler.obtainMessage(UPDATE_LRC, progress).sendToTarget();
        //System.out.println("PlayActivity.publish.myHandler = " + myHandler);
        //System.out.println("PlayActivity.publish.UPDATE_LRC = " + UPDATE_LRC);
        //System.out.println("PlayActivity.publish.progress = " + progress);
    }

    @Override
    public void change(int position) {//初始化,独立播放界面的歌曲切换后的初始化界面上的歌曲信息
        //if (this.playService.isPlaying()) {//获取是否为播放状态,服务绑定是个异步过程，如果服务还没绑定好就使用
                                             //this.playService.isPlaying()会报空指针异常
        //System.out.println("PlayActivity.change.position = " + position);
        Mp3Info mp3Info = playService.mp3Infos.get(position);
        //System.out.println("PlayActivity.change.getTitle = " + mp3Info.getTitle());
        textView2_title.setText(mp3Info.getTitle());//设置歌名
        //textView2_title.setTtileText(mp3Info.getTitle());//设置歌名
        //System.out.println("PlayActivity.change.textView2_title = " + textView2_title);
        //System.out.println("PlayActivity.change.getText : " + textView2_title.getText());
        //System.out.println("PlayActivity.change.getText : " + textView2_title.getTtileText());


        //下面这句话是提交更新UI的,但是这个功能在adapter里面实现了,但是实现的方式,只适合UI需要更新的内容很少的时候
        //而且,就算使用,也是设置了专辑图片和倒影,以后使用,不是这里.
        //之所以放在这里,是因为我开始调试的时候,无法setText,最后通过Debug找到了问题所在
        //以后我会专门开个博文,介绍一下Debug
        //adapter.notifyDataSetChanged();

        //setText后,歌名没有显示,但是可以getText,尝试隐藏/显示,来刷新UI,结果歌名还是没有显示
        //viewPager.setVisibility(View.GONE);
        //getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.activity_music_play);
        //viewPager.setVisibility(View.VISIBLE);

        //setText后,歌名没有显示,但是可以getText,尝试隐藏/显示,来刷新UI,结果歌名还是没有显示
        //viewPager.setVisibility(View.GONE);
        //viewPager.setVisibility(View.VISIBLE);


        //获取专辑封面图片
        Bitmap albumBitmap = MediaUtils.getArtwork(this, mp3Info.getId(), mp3Info.getAlbumId(), true, false);
        //改变播放界面专辑封面图片
        imageView1_ablum.setImageBitmap(albumBitmap);
        textView1_end_time.setText(MediaUtils.formatTime(mp3Info.getDuration()));//设置结束时间
        imageView2_play_pause.setImageResource(R.mipmap.app_music_pause);//设置暂停图片
        seekBar1.setProgress(0);//设置当前进度为0
        seekBar1.setMax((int) mp3Info.getDuration());//设置进度条最大值为MP3总时间
        if (playService.isPlaying()) {
            imageView2_play_pause.setImageResource(R.mipmap.app_music_pause);
        } else {
            imageView2_play_pause.setImageResource(R.mipmap.app_music_play);
        }

        if (imageView1_ablum != null) {
            imageView1_ablum_reflection.setImageBitmap(ImageUtils.createReflectionBitmapForSingle(albumBitmap));//显示倒影
        }
        switch (playService.getPlay_mode()) {
            case ORDER_PLAY://顺序播放
                imageView1_play_mode.setImageResource(R.mipmap.app_mode_order);
                //imageView2_play_pause.setTag(ORDER_PLAY);
                break;
            case PlayService.RANDOM_PLAY://随机播放
                imageView1_play_mode.setImageResource(R.mipmap.app_mode_random);
                //imageView2_play_pause.setTag(RANDOM_PLAY);
                break;
            case PlayService.SINGLE_PLAY://单曲循环
                imageView1_play_mode.setImageResource(R.mipmap.app_mode_single);
                //imageView2_play_pause.setTag(SINGLE_PLAY);
                break;
            default:
                break;
        }

        long id = getId(mp3Info);

        //初始化收藏状态
        try {
            Mp3Info loveMp3Info = app.dbUtils.findFirst(Selector.from(Mp3Info.class).where("mp3InfoId", "=", mp3Info.getMp3InfoId()));//查出歌曲,SQL语句
            System.out.println("初始化收藏状态" + loveMp3Info);
//            if (loveMp3Info != null) {
//                imageView1_favorite.setImageResource(R.mipmap.app_love_selected);
//            } else {
//                imageView1_favorite.setImageResource(R.mipmap.app_love_unselected);
//            }
            if (loveMp3Info != null) {
                System.out.println("loveMp3Info.getIsLove() = " + loveMp3Info.getIsLove());
                if (loveMp3Info.getIsLove() == 0) {//返回值不为null,且,isLove为0时,也显示为'未收藏'
                    imageView1_favorite.setImageResource(R.mipmap.app_love_unselected);
                }else {//返回值为null,且,isLove为1时,一定显示为'已收藏'
                    imageView1_favorite.setImageResource(R.mipmap.app_love_selected);
                }
            } else {//返回值为null,一定显示为'未收藏'
                imageView1_favorite.setImageResource(R.mipmap.app_love_unselected);
            }
        } catch (DbException e) {
            e.printStackTrace();
        }

        //歌词
        String songName = mp3Info.getTitle();
        String artistName = mp3Info.getArtist();
        //歌词功能还不完善,后面会博文,会继续完善,暂时使用我已经下载好的lrc测试效果.
        //songName = "山丘";//测试用的lrc的歌名
        String lrcPath = Environment.getExternalStorageDirectory() + Constant.DIR_LRC + "/" + songName + ".lrc";
        File lrcFile = new File(lrcPath);
        if (!lrcFile.exists()){
//            //下载歌词
//            System.out.println("下载歌词");
//            DownloadUtils.getsInstance().downloadLRC(songName,artistName,myHandler);
//            Toast.makeText(PlayActivity.this, "请下次播放本首歌曲时,将显示歌词", Toast.LENGTH_SHORT).show();
            //下载歌词
            SearchMusicUtils.getInstance().setListener(new SearchMusicUtils.OnSearchResultListener() {
                @Override
                public void onSearchResult(ArrayList<SearchResult> results) {
                    SearchResult searchResult = results.get(0);//取搜索结果榜单中的第一首歌
                    System.out.println(searchResult);
                    String url = Constant.BAIDU_LRC_SEARCH_HEAD + searchResult.getUrl();
                    DownloadUtils.getsInstance().downloadLRC(url, searchResult.getMusicName(), myHandler);
                }
            }).search(songName+" "+mp3Info.getArtist(), 1);//使用歌名跟歌手来搜索歌词，防止一歌多人唱
        }else{
            loadLRC(lrcFile);
        }
    }

    //如果是本地音乐,id就是id;如果是收藏音乐,id则是mp3InfoId
    //提供给 收藏按钮 点击事件时 调用.
    private long getId(Mp3Info mp3Info){
        //初始收藏状态
        long id = 0;
        switch (playService.getChangePlayList()){
            case PlayService.MY_MUSIC_LIST:
                System.out.println("当前为本地列表");
                id = mp3Info.getId();
                System.out.println("id =" + id);
                break;
            case PlayService.LOVE_MUSIC_LIST:
                System.out.println("当前为收藏列表");
                id = mp3Info.getMp3InfoId();
                System.out.println("id =" + id);
                break;
            default:
                break;
        }
        return id;
    }

    //点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageView2_play_pause: {//播放暂停按钮
                if (playService.isPlaying()) {//如果是播放状态
                    imageView2_play_pause.setImageResource(R.mipmap.app_music_play);//设置播放图片
                    playService.pause();
                } else {
                    if (playService.isPause()) {
                        imageView2_play_pause.setImageResource(R.mipmap.app_music_pause);//设置暂停图片
                        playService.start();//播放事件
                    } else {
                        //只有打开APP没有点击任何歌曲,同时,直接点击暂停播放按钮时.才会调用
                        //playService.play(0);

                        //只有打开APP没有点击任何歌曲,同时,直接点击暂停播放按钮时.才会调用
                        //默认播放的是,在PlayService的onCreate中 恢复状态值
                        playService.play(playService.getCurrentPosition());
                    }
                }
                break;
            }
            case R.id.imageView1_next: {//下一首按钮
                playService.next();//下一首
                break;
            }
            case R.id.imageView3_previous: {//上一首按钮
                playService.prev();//上一首
                break;
            }
            case R.id.imageView1_play_mode: {//循环模式按钮
                //使用settag和gettag方法就可以获得imageView1_play_mode里面的值
                //int mode = (int) imageView1_play_mode.getTag();
                /**
                 * 以下Tosat内容,在strings.xml里,添加对应代码
                 *<string name="order_play">顺序播放</string>
                 *<string name="random_play">随机播放</string>
                 *<string name="single_play">单曲循环</string>
                 */
                switch (playService.getPlay_mode()) {
                    case ORDER_PLAY:
                        imageView1_play_mode.setImageResource(R.mipmap.app_mode_random);
                        //imageView1_play_mode.setTag(RANDOM_PLAY);//将imageView1_play_mode重新设置为RANDOM_PLAY
                        playService.setPlay_mode(RANDOM_PLAY);
                        //Toast.makeText(getApplicationContext(),"随机播放",Toast.LENGTH_SHORT).show();//这句也可以
                        //Toast.makeText(PlayActivity.this, "随机播放", Toast.LENGTH_SHORT).show();//这句也可以
                        Toast.makeText(PlayActivity.this, getString(R.string.random_play), Toast.LENGTH_SHORT).show();
                        break;
                    case RANDOM_PLAY:
                        imageView1_play_mode.setImageResource(R.mipmap.app_mode_single);
                        //imageView1_play_mode.setTag(SINGLE_PLAY);
                        playService.setPlay_mode(SINGLE_PLAY);
                        //Toast.makeText(getApplicationContext(),"单曲循环",Toast.LENGTH_SHORT).show();//这句也可以
                        //Toast.makeText(PlayActivity.this, "单曲循环", Toast.LENGTH_SHORT).show();//这句也可以
                        Toast.makeText(PlayActivity.this, getString(R.string.single_play), Toast.LENGTH_SHORT).show();
                        break;
                    case SINGLE_PLAY:
                        imageView1_play_mode.setImageResource(R.mipmap.app_mode_order);
                        //imageView1_play_mode.setTag(ORDER_PLAY);
                        playService.setPlay_mode(ORDER_PLAY);
                        //Toast.makeText(getApplicationContext(),"顺序播放",Toast.LENGTH_SHORT).show();//这句也可以
                        //Toast.makeText(PlayActivity.this, "顺序播放", Toast.LENGTH_SHORT).show();//这句也可以
                        Toast.makeText(PlayActivity.this, getString(R.string.order_play), Toast.LENGTH_SHORT).show();
                        break;
                }
                break;
            }
            case R.id.imageView1_favorite: {//收藏按钮  //在vo.Mp3Info里  private long mp3InfoId;//在收藏音乐时用于保存原始ID
                Mp3Info mp3Info = playService.mp3Infos.get(playService.getCurrentPosition());//查出歌曲
                System.out.println(mp3Info);
                try {
                    Mp3Info loveMp3Info = app.dbUtils.findFirst(Selector.from(Mp3Info.class).where("mp3InfoId","=",getId(mp3Info)));//查出歌曲,SQL语句
                    System.out.println(loveMp3Info);
                    if (loveMp3Info==null){//返回值为null,则一定需要save,没找到，则保存
                        System.out.println("不在音乐收藏数据库中 保存音乐数据 原始数据: " + mp3Info);
                        mp3Info.setMp3InfoId(mp3Info.getId());//把原本的id设置给mp3InfoId
                        mp3Info.setIsLove(1);
                        System.out.println(mp3Info);
                        app.dbUtils.save(mp3Info);//在音乐收藏数据库 保存音乐
                        System.out.println("save");
                        imageView1_favorite.setImageResource(R.mipmap.app_love_selected);

                        //以下是:调试使用,保存以后再查一遍
                        loveMp3Info = app.dbUtils.findFirst(Selector.from(Mp3Info.class).where("mp3InfoId","=",getId(mp3Info)));//查出歌曲,SQL语句
                        System.out.println("调试使用,保存以后再查一遍 最新数据: " + loveMp3Info);
                    }else {//返回值不为null,则一定需要update
                        System.out.println("在音乐收藏数据库中 更新音乐数据 原始数据: " + loveMp3Info);
                        int isLove = loveMp3Info.getIsLove();
                        if (isLove==1){//返回值不为null,且,isLove为1时;设置isLove为0,同时显示为'未收藏'
                            loveMp3Info.setIsLove(0);
                            imageView1_favorite.setImageResource(R.mipmap.app_love_unselected);
                        }else {//返回值不为null,且,isLove为0时;设置isLove为1,同时显示为'已收藏'
                            loveMp3Info.setIsLove(1);
                            imageView1_favorite.setImageResource(R.mipmap.app_love_selected);
                        }
                        System.out.println("update");
                        app.dbUtils.update(loveMp3Info,"isLove");//更新loveMp3Info数据


                        //以下是:调试使用,更新以后再查一遍
                        loveMp3Info = app.dbUtils.findFirst(Selector.from(Mp3Info.class).where("mp3InfoId", "=", getId(mp3Info)));//查出歌曲,SQL语句
                        System.out.println("调试使用,更新以后再查一遍 最新数据: " + loveMp3Info);
                    }
                } catch (DbException e) {
                    e.printStackTrace();
                }
                break;
            }
            default:
                break;
        }
    }


    //加载歌词
    private void loadLRC(File lrcFile){
        StringBuffer buf = new StringBuffer(1024 * 10);//读取本地文件，歌词读出来需要放在一个StringBuffer中
        char[] chars = new char[1024];
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(lrcFile)));
            int len = -1;
            while ((len = in.read(chars)) != -1){
                buf.append(chars,0,len);
            }
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
        ILrcBuilder builder = new DefaultLrcBuilder();
        List<LrcRow> rows = builder.getLrcRows(buf.toString());
        lrcView.setLrc(rows);
        //加载专辑封面图片为背景的方法(实际使用,发现效果不理想)
        //long id = mp3Info.getMp3InfoId()==0?mp3Info.getId:mp3Info.getMp3InfoId();
        //Bitmap bg = MediaUtils.getArtwork(this, id ,mp3Info.getAlbumId(),false,false);
        //if(bg != null){
        //    lrcView.getBackground(new BitmapDrawable(getResources(),bg));
        //   lrcView.getBackground().setAlpha(120);
        //}
    }

}