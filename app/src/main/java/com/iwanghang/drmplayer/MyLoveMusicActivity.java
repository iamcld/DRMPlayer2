package com.iwanghang.drmplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import com.iwanghang.drmplayer.adapter.MyMusicListAdapter;
import com.iwanghang.drmplayer.vo.Mp3Info;
import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.exception.DbException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by iwanghang on 16/4/27.
 * 我的收藏音乐列表界面
 */
public class MyLoveMusicActivity extends BaseActivity implements OnItemClickListener {

    private ListView listView_love;
    private DRMPlayerApp app;//取出全局对象 方便调用
    private ArrayList<Mp3Info> loveMp3Infos;
    private MyMusicListAdapter adapter;
    private boolean isChange = false;//表示当前播放列表是否为收藏列表

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (DRMPlayerApp) getApplication();
        setContentView(R.layout.avtivity_my_love_music_list);//绑定布局
        listView_love = (ListView) findViewById(R.id.listView_love);//实例化布局
        listView_love.setOnItemClickListener(this);
        initData();//初始化数据
        
        //listView_love.setAdapter();
    }

    private void initData() {//初始化数据
        try {
            List<Mp3Info> list = app.dbUtils.findAll(Selector.from(Mp3Info.class).where("isLove","=","1"));//查找所有已收藏音乐
            if (list==null || list.size()==0){
                return;
            }
            loveMp3Infos = (ArrayList<Mp3Info>) list;
            adapter = new MyMusicListAdapter(this,loveMp3Infos);
            listView_love.setAdapter(adapter);
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    //把播放服务的绑定和解绑放在onResume,onPause里,好处是,每次回到当前Activity就获取一次播放状态
    @Override
    protected void onResume() {
        super.onResume();
        bindPlayService();//绑定服务
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

    @Override
    public void publish(int progress) {

    }

    @Override
    public void change(int progress) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //如果当前播放列表不是收藏列表
        if (playService.getChangePlayList() != playService.LOVE_MUSIC_LIST){
            playService.setMp3Infos(loveMp3Infos);//播放列表切换为收藏列表
            playService.setChangePlayList(playService.LOVE_MUSIC_LIST);
            System.out.println("播放列表切换为收藏列表");
        }
        playService.play(position);

        Mp3Info loveMp3Info = loveMp3Infos.get(position);//查出歌曲
        System.out.println("收藏列表 : " + loveMp3Info);

        //保存播放时间
        savePlayRecord();
    }

    //保存播放记录
    private void savePlayRecord(){
        //获取当前正在播放的音乐对象
        Mp3Info mp3Info = playService.getMp3Infos().get(playService.getCurrentPosition());
        try {
            Mp3Info playRecordMp3Info = app.dbUtils.findFirst(Selector.from(Mp3Info.class).where("mp3InfoId", "=", mp3Info.getMp3InfoId()));//查出歌曲,SQL语句
            if (playRecordMp3Info==null){
                //mp3Info.setMp3InfoId(mp3Info.getId());
                mp3Info.setPlayTime(System.currentTimeMillis());
                app.dbUtils.save(mp3Info);
            }else {
                playRecordMp3Info.setPlayTime(System.currentTimeMillis());//设置当前播放时间
                app.dbUtils.update(playRecordMp3Info,"playTime");
            }
        }catch (DbException e){
            e.printStackTrace();
        }
    }
}
