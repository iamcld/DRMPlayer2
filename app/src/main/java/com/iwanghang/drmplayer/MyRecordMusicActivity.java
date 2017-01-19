package com.iwanghang.drmplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.iwanghang.drmplayer.adapter.MyMusicListAdapter;
import com.iwanghang.drmplayer.utils.Constant;
import com.iwanghang.drmplayer.vo.Mp3Info;
import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.exception.DbException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by iwanghang on 16/4/27.
 * 最近播放的音乐列表界面
 */
public class MyRecordMusicActivity extends BaseActivity implements OnItemClickListener {

    private ListView listView_record;
    private TextView textView2_no_data;
    private DRMPlayerApp app;
    private ArrayList<Mp3Info> recordMp3Infos;
    private MyMusicListAdapter adapter;
    //private boolean isChange = false;//表示当前播放列表是否为收藏列表

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (DRMPlayerApp) getApplication();
        setContentView(R.layout.activity_my_record_music_list);//绑定布局
        listView_record = (ListView) findViewById(R.id.listView_record);//实例化布局
        textView2_no_data = (TextView) findViewById(R.id.textView2_no_data);
        listView_record.setOnItemClickListener(this);
        initData();//初始化数据
        
        //listView_love.setAdapter();
    }

    private void initData() {//初始化数据
        try {
            //查询最近播放的记录
            List<Mp3Info> list = app.dbUtils.findAll(Selector.from(Mp3Info.class).where("playTime","!=","0").orderBy("playTime",true).limit(Constant.MY_RECORD_NUM));//查找5首最近播放的音乐
            if (list==null || list.size()==0){
                textView2_no_data.setVisibility(View.VISIBLE);//温馨提示.显示
                listView_record.setVisibility(View.GONE);//最近播放列表.隐藏
            }else {
                textView2_no_data.setVisibility(View.GONE);//温馨提示.隐藏
                listView_record.setVisibility(View.VISIBLE);//最近播放列表.显示
                recordMp3Infos = (ArrayList<Mp3Info>) list;
                adapter = new MyMusicListAdapter(this,recordMp3Infos);
                listView_record.setAdapter(adapter);
            }
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
        if (playService.getChangePlayList() != PlayService.RECORD_MUSIC_LIST){
            playService.setMp3Infos(recordMp3Infos);//播放列表切换为最近播放
            playService.setChangePlayList(PlayService.RECORD_MUSIC_LIST);
            System.out.println("播放列表切换为最近播放");
        }
        playService.play(position);
    }
}