package com.iwanghang.drmplayer.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.andraskindler.quickscroll.Scrollable;
import com.iwanghang.drmplayer.R;
import com.iwanghang.drmplayer.utils.MediaUtils;
import com.iwanghang.drmplayer.vo.Mp3Info;

import java.util.ArrayList;

/**
 * 自定义的音乐列表适配器
 * 为了方便扩展，因为之前没有考虑到显示专辑封面
 * Created by iwanghang on 16/4/16.
 */
public class MyMusicListAdapter extends BaseAdapter implements Scrollable{

    private Context ctx; //上下文对象引用
    private ArrayList<Mp3Info> mp3Infos;//存放Mp3Info引用的集合
    private Mp3Info mp3Info;		//Mp3Info对象引用
    private int pos = -1;			//列表位置

    /**
     * 构造函数
     * @param ctx	上下文
     * @param mp3Infos  集合对象
     */
    public MyMusicListAdapter(Context ctx, ArrayList<Mp3Info> mp3Infos){
        this.ctx = ctx;
        this.mp3Infos = mp3Infos;
        //System.out.println("MyMusicListAdapter.java #0 : ctx = " + ctx + ",mp3Infos = " + mp3Infos.size());
    }

    public ArrayList<Mp3Info> getMp3Infos() {
        System.out.println("MyMusicListAdapter.java #1 : public ArrayList<Mp3Info> getMp3Infos() {");
        return mp3Infos;
    }

    public void setMp3Infos(ArrayList<Mp3Info> mp3Infos) {
        System.out.println("MyMusicListAdapter.java #2 : public void setMp3Infos(ArrayList<Mp3Info> mp3Infos) {");
        this.mp3Infos = mp3Infos;
    }


    @Override
    public int getCount() {
        //System.out.println("MyMusicListAdapter.java #3 : public int getCount() {" + mp3Infos.size());
        //return mp3Infos.size();
        return mp3Infos.size();
    }

    @Override
    public Object getItem(int position) {
        System.out.println("MyMusicListAdapter.java #4 : public Object getItem(int position) {");
        return mp3Infos.get(position);
    }

    @Override
    public long getItemId(int position) {
        //System.out.println("MyMusicListAdapter.java #5 : public long getItemId(int position) {");
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //System.out.println("MyMusicListAdapter.java #6 : public View getView ");
        ViewHolder vh;
        if(convertView==null){
            //vh = new ViewHolder();
            convertView = LayoutInflater.from(ctx).inflate(R.layout.item_my_music_list,null);
            vh = new ViewHolder();
            vh.textView1_title = (TextView) convertView.findViewById(R.id.textView1_title);
            vh.textView2_singer = (TextView) convertView.findViewById(R.id.textView2_singer);
            vh.textView3_time = (TextView) convertView.findViewById(R.id.textView3_time);
            vh.imageView1_ablum = (ImageView) convertView.findViewById(R.id.imageView1_ablum);

            //System.out.println("MyMusicListAdapter.java #7 : textView1_title = " + vh.textView1_title);
            convertView.setTag(vh);//表示给View添加一个格外的数据，
        }else {
            vh = (ViewHolder)convertView.getTag();//通过getTag的方法将数据取出来
        }

        Mp3Info mp3Info = mp3Infos.get(position);
        vh.textView1_title.setText(mp3Info.getTitle());//显示标题
        vh.textView2_singer.setText(mp3Info.getArtist());//显示歌手
        vh.textView3_time.setText(MediaUtils.formatTime(mp3Info.getDuration()));//显示时长

        //获取专辑封面图片
        Bitmap albumBitmapItem = MediaUtils.getArtwork(ctx,mp3Info.getId(),mp3Info.getAlbumId(),true,true);
        //System.out.println("MyMusicListAdapter.java #8 : albumBitmapItem = " + albumBitmapItem.getConfig());

        //改变播放界面专辑封面图片
        vh.imageView1_ablum.setImageBitmap(albumBitmapItem);
        //vh.imageView1_ablum.setImageResource(R.mipmap.music);

        return convertView;
    }

    //滑到时提示的内容
    @Override
    public String getIndicatorForPosition(int childposition, int groupposition) {
        //return Character.toString(mp3Infos.get(childposition).getTitle().charAt(0));//返回歌名的第一个字
        System.out.println("childposition:" +childposition);
        System.out.println("测试1");
        return Character.toString(mp3Infos.get(childposition).getTitle().charAt(0));//返回歌名的第一个字
    }

    //返回位置，即滑到哪里的位置
    @Override
    public int getScrollPosition(int childposition, int groupposition) {
        System.out.println("测试2");
        return childposition;
    }

    /**
     * 定义一个内部类
     * 声明相应的控件引用
     */
    static class ViewHolder{
        //所有控件对象引用
        TextView textView1_title;//标题
        TextView textView2_singer;//歌手
        TextView textView3_time;//时长
        ImageView imageView1_ablum;//专辑封面图片
    }
}