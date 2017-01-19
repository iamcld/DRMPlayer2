package com.iwanghang.drmplayer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.andraskindler.quickscroll.QuickScroll;
import com.iwanghang.drmplayer.adapter.MyMusicListAdapter;
import com.iwanghang.drmplayer.utils.MediaUtils;
import com.iwanghang.drmplayer.vo.Mp3Info;
import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.exception.DbException;

import java.util.ArrayList;

/**
 * Created by iwanghang on 16/4/16.
 * 实现OnItemClickListener(Item点击事件监听),OnClickListener(按钮点击事件监听)
 */
public class MyMusicListFragment extends Fragment implements OnItemClickListener,OnClickListener {
//public class MyMusicListFragment extends Fragment {

    private ListView listView_my_music;
    private ImageView imageView_album;//专辑封面图片
    private TextView textView_songName,textView2_singer;//歌名,歌手
    private ImageView imageView2_play_pause,imageView3_next;//暂停播放,下一首
    private ArrayList<Mp3Info> mp3Infos;
    private MyMusicListAdapter myMusicListAdapter;
    private QuickScroll quickScroll;

    private MainActivity mainActivity;

    //DRMPlayerApp app;//取出全局对象 方便调用

    //private boolean isPause = false;//歌曲播放中的暂停状态

    private int position = 0;//当前播放的位置,提供给PlayActivity

    //onAttach(),当fragment被绑定到activity时被调用(Activity会被传入.).
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity) context;//当fragment获得到mainActivity对象时，就可以在fragment中
        //直接调用mainActivity父类(BaseActivity)中的bindPlayService和unbindPlayService函数，playService对象
    }

    public static MyMusicListFragment newInstance() {
        MyMusicListFragment my = new MyMusicListFragment();
        return my;
    }

    //onCreateView(),创建和fragment关联的view hierarchy时调用.
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //UI组件初始化
        View view = inflater.inflate(R.layout.my_music_list_layout,null);
        //item
        listView_my_music = (ListView) view.findViewById(R.id.listView_my_music);
        //取出全局对象 方便调用
        //app = (DRMPlayerApp) getApplication();
        //findViewById
        imageView_album = (ImageView) view.findViewById(R.id.imageView_album);
        textView_songName = (TextView) view.findViewById(R.id.textView_songName);
        textView2_singer = (TextView) view.findViewById(R.id.textView2_singer);
        imageView2_play_pause = (ImageView) view.findViewById(R.id.imageView2_play_pause);
        imageView3_next = (ImageView) view.findViewById(R.id.imageView3_next);


        quickScroll = (QuickScroll) view.findViewById(R.id.quickscroll);
        //Item点击事件监听
        listView_my_music.setOnItemClickListener(this);
        //按钮点击事件监听
        imageView2_play_pause.setOnClickListener(this);
        imageView3_next.setOnClickListener(this);
        //专辑封面图片点击事件监听
        imageView_album.setOnClickListener(this);
        //loadData();//初始化数据
        //绑定服务
        //服务在加载SplashActivity(欢迎页面)的时候,已经启动
        //mainActivity.bindPlayService();
        return view;
    }

    //把播放服务的绑定和解绑放在onResume,onPause里,好处是,每次回到当前Activity就获取一次播放状态
    @Override
    public void onResume() {
        super.onResume();
        //绑定播放服务
        System.out.println("myMusicListFragment     onResume...");
        mainActivity.bindPlayService();//目的是为和可以调用service中的方法或对象
    }

    @Override
    public void onPause() {
        super.onPause();
        //解绑播放服务
        mainActivity.unbindPlayService();
        System.out.println("myMusicListFragment     onPause...");
    }

    //onDestroyView(),当和fragment关联的view hierarchy正在被移除时调用.
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //解绑服务
        //在Activity销毁时,解绑
        mainActivity.unbindPlayService();
    }

    /**
     * 初始化数据,加载本地音乐列表
     * 在MainActivity里调用
     */
    public void loadData(){
        mp3Infos = MediaUtils.getMp3Infos(mainActivity);
        //mp3Infos = mainActivity.playService.mp3Infos;
        myMusicListAdapter = new MyMusicListAdapter(mainActivity,mp3Infos);
        listView_my_music.setAdapter(myMusicListAdapter);

        initQuickscroll();
    }

//    public ListView getListView_my_music() {
//        return listView_my_music;
//    }
//
//    public ArrayList<Mp3Info> getMp3Infos() {
//        return mp3Infos;
//    }
//
//    public MyMusicListAdapter getMyMusicListAdapter() {
//        return myMusicListAdapter;
//    }
//
//    public MainActivity getMainActivity() {
//        return mainActivity;
//    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //如果当前播放列表不是本地音乐列表
        if (mainActivity.playService.getChangePlayList() != PlayService.MY_MUSIC_LIST){
            mainActivity.playService.setMp3Infos(mp3Infos);//播放列表切换为本地音乐列表
            mainActivity.playService.setChangePlayList(PlayService.MY_MUSIC_LIST);
            System.out.println("播放列表切换为本地列表");
        }
        mainActivity.playService.play(position);

        Mp3Info mp3Info = mp3Infos.get(position);//查出歌曲
        System.out.println("本地列表 : " + mp3Info);

        //保存播放时间
        savePlayRecord();
    }

    //保存播放记录
    private void savePlayRecord(){
        //获取当前正在播放的音乐对象
        Mp3Info mp3Info = mainActivity.playService.getMp3Infos().get(mainActivity.playService.getCurrentPosition());
        try {
            Mp3Info playRecordMp3Info = mainActivity.app.dbUtils.findFirst(Selector.from(Mp3Info.class).where("mp3InfoId", "=", mp3Info.getId()));//查出歌曲,SQL语句
            if (playRecordMp3Info==null){
                mp3Info.setMp3InfoId(mp3Info.getId());
                mp3Info.setPlayTime(System.currentTimeMillis());//设置当前播放时间，ms

                mainActivity.app.dbUtils.save(mp3Info);
            }else {
                playRecordMp3Info.setPlayTime(System.currentTimeMillis());//设置当前播放时间
                mainActivity.app.dbUtils.update(playRecordMp3Info,"playTime");
            }
        }catch (DbException e){
            e.printStackTrace();
        }
    }


    //回调播放状态下的UI设置,更新专辑图片，歌手，歌名
    public void changeUIStatusOnPlay(int position){//更新UI状态--当前播放的音乐,传进来当前音乐的下标
        if (position>=0 && position<mainActivity.playService.mp3Infos.size()){
            Mp3Info mp3Info = mainActivity.playService.mp3Infos.get(position);//获取当前播放的音乐信息
            textView_songName.setText(mp3Info.getTitle());//设置歌名
            textView2_singer.setText(mp3Info.getArtist());//设置歌手名
            //imageView2_play_pause.setImageResource(R.mipmap.player_btn_pusr_normal);//更新播放暂停图片

            if (mainActivity.playService.isPlaying()){
                imageView2_play_pause.setImageResource(R.mipmap.app_music_pause);
            }else {
                imageView2_play_pause.setImageResource(R.mipmap.app_music_play);
            }

            //获取专辑封面图片
            Bitmap albumBitmap = MediaUtils.getArtwork(mainActivity,mp3Info.getId(),mp3Info.getAlbumId(),true,true);
            //改变播放界面专辑封面图片
            imageView_album.setImageBitmap(albumBitmap);

            //当前播放的位置,提供给PlayActivity
            this.position = position;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.imageView2_play_pause: {//播放暂停按钮点击事件
                if (mainActivity.playService.isPlaying()) {//如果当前为播放状态
                    //修改按钮图片为播放按钮
                    imageView2_play_pause.setImageResource(R.mipmap.app_music_play);
                    mainActivity.playService.pause();//暂停事件

                } else {
                    if (mainActivity.playService.isPause()) {
                        //修改按钮图片为暂停按钮
                        imageView2_play_pause.setImageResource(R.mipmap.app_music_pause);
                        mainActivity.playService.start();//播放事件
                    } else {
                        //只有打开APP没有点击任何歌曲,同时,直接点击暂停播放按钮时.才会调用
                        //mainActivity.playService.play(0);默认播放第一首音乐
                        //mainActivity.playService.play(0);

                        //只有打开APP没有点击任何歌曲,同时,直接点击暂停播放按钮时.才会调用
                        //默认播放的是,在PlayService的onCreate中 恢复状态值
                        mainActivity.playService.play(mainActivity.playService.getCurrentPosition());
                    }

                }
                break;
            }
            case R.id.imageView3_next: {//下一首按钮点击事件
                mainActivity.playService.next();;
                break;
            }
            case R.id.imageView_album: {//专辑封面图片点击事件
                Intent intent = new Intent(mainActivity,PlayActivity.class);//跳转到PlayActivity
                //intent.putExtra("position",position);//当前播放的位置,提供给PlayActivity
                //intent.putExtra("isPause",isPause);//当前播放的是否为暂停状态,提供给PlayActivity
                startActivity(intent);
                break;
            }
            default:
                break;
        }
    }

    private void initQuickscroll(){
        System.out.println("initQuickscroll");
        quickScroll.init(QuickScroll.TYPE_POPUP_WITH_HANDLE, listView_my_music, myMusicListAdapter, QuickScroll.STYLE_HOLO);
        quickScroll.setFixedSize(1);
        quickScroll.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 48);
        quickScroll.setPopupColor(QuickScroll.BLUE_LIGHT, QuickScroll.BLUE_LIGHT_SEMITRANSPARENT, 1, Color.WHITE, 1);

    }

}
