package com.iwanghang.drmplayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import com.iwanghang.drmplayer.adapter.MyMusicListAdapter;
import com.iwanghang.drmplayer.utils.Constant;
import com.iwanghang.drmplayer.utils.DownloadUtils;
import com.iwanghang.drmplayer.utils.MediaUtils;
import com.iwanghang.drmplayer.vo.Mp3Info;
import com.iwanghang.drmplayer.vo.SearchResult;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by iwanghang on 16/5/3.
 * DownloadDialogFargment
 */
public class DownloadDialogFragment extends DialogFragment {
    private MyMusicListFragment myMusicListFragment;

    private ArrayList<Mp3Info> mp3Infos;
    private MyMusicListAdapter myMusicListAdapter;



    private SearchResult searchResult;//当前要下载的歌曲对象
    private MainActivity mainActivity;
    public static DownloadDialogFragment newInstance(SearchResult searchResult){
        DownloadDialogFragment downloadDialogFragment = new DownloadDialogFragment();
        downloadDialogFragment.searchResult = searchResult;
        return downloadDialogFragment;
    }

    private String[] items;

    //最先执行
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity) getActivity();
        //items = new String[]{"下载","取消"};
        items = new String[]{getString(R.string.download),getString(R.string.cancel)};
    }

    // 回调接口
    //public interface DownloadSelectListener { // 是否选择(点击)下载.监听.按钮回调接口
    //    void downloadSelectListener(int isDownload); // 回传一个int,是否选择(点击)下载按钮 ; 回传0,表示下载;回传1,表示取消
    //}

    //创建对话框的事件方法，系统调用
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setCancelable(true);//返回键可以取消
        builder.setItems(items,new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        //执行下载
                        downloadMusic();
                        break;
                    case 1://取消
                        dialog.dismiss();
                        break;
                }
                //DownloadSelectListener listener = (DownloadSelectListener) getActivity(); // 回调接口
                //listener.downloadSelectListener(which);//回传0,表示下载;回传1,表示取消
            }
        });
        return builder.show();
    }

    // 回调接口
    public interface DownloadSuccessListener { // 下载是否成功.监听.按钮回调接口
        void downloadSuccessListener(String isDownloadSuccess); // 回传一个字符串
    }


    //下载音乐
    private void downloadMusic() {
        Toast.makeText(mainActivity, "正在下载:" + searchResult.getMusicName(), Toast.LENGTH_LONG).show();
        //下载是个异步过程，应该去写一个监听事件(观察者设计模式)
        //异步处理结束后，使用handler来回调事件的相应方法
        DownloadUtils.getsInstance().setListener(new DownloadUtils.OnDownloadListener() {
            @Override
            public void onDowload(String mp3Url) { //下载成功,接收下载完成事件
                Toast.makeText(mainActivity, "下载成功", Toast.LENGTH_SHORT).show();


                System.out.println("DownloadDialogFragment.downloadMusic @ = " + Environment.getExternalStorageDirectory()+Constant.DIR_MUSIC + "/" + mp3Url);
                //更新媒体库
                scanFile(mainActivity,Environment.getExternalStorageDirectory()+Constant.DIR_MUSIC + "/" + mp3Url);
                //更新 本地音乐列表 , 这个功能放在MainActivity实现

                //DownloadSuccessListener listener = (DownloadSuccessListener) getActivity(); // 空指针异常,因为Fragment已经销毁,所以getActivity()==null,需要使用下面的写法
                DownloadSuccessListener listener = mainActivity; // 回调接口
                System.out.println("DownloadDialogFragment.downloadMusic.listener = " + listener);
                listener.downloadSuccessListener(mp3Url); // 回传一个字符串 ,回传什么都行 ,只是告诉MainActivity ,已经下载成功了新的歌曲
            }


            @Override
            public void onFailed(String error) { //下载失败，接收下载失败事件
                Toast.makeText(mainActivity, error, Toast.LENGTH_SHORT).show();

            }
        }).download(searchResult);
    }

    /**
     * 通知媒体库更新文件
     * @param context
     * @param filePath 文件全路径
     *
     * */
    public void scanFile(Context context, String filePath) {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(new File(filePath)));
        context.sendBroadcast(scanIntent);
    }
    /**
     * 更新本地音乐列表
     */
    //private void loadDataAndChangeUI() {
    //    if(myMusicListFragment==null){
    //        myMusicListFragment = MyMusicListFragment.newInstance();
    //    }
    //    //mp3Infos = MediaUtils.getMp3Infos(mainActivity);
    //    //myMusicListAdapter = new MyMusicListAdapter(mainActivity,mp3Infos);
    //    //listView_my_music.setAdapter(myMusicListAdapter);
    //    myMusicListFragment.loadData();//初始化数据
    //    myMusicListFragment.changeUIStatusOnPlay(0);
    //}


}
