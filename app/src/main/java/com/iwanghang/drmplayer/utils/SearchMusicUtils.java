package com.iwanghang.drmplayer.utils;

import android.os.Handler;
import android.os.Message;

import com.iwanghang.drmplayer.vo.SearchResult;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by iwanghang on 16/4/30.
 * 搜索音乐工具类
 */
public class SearchMusicUtils {
    private static final int SIZE = 20;//查询歌曲数量
    //private static final String URL = Constant.BAIDU_URL + Constant.BAIDU_SEARCH;
    //private static String URL = Constant.BAIDU_URL + Constant.BAIDU_SEARCH;
    private static SearchMusicUtils sInstance;
    private OnSearchResultListener mListener;

    private ExecutorService mThreadPool;//线程池

    public synchronized static SearchMusicUtils getInstance(){
        if (sInstance == null){
            try {
                sInstance = new SearchMusicUtils();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        }
        return sInstance;
    }

    //设置搜索事件
    private SearchMusicUtils() throws ParserConfigurationException{
        mThreadPool = Executors.newSingleThreadExecutor();//单任务线程池
    }

    public SearchMusicUtils setListener(OnSearchResultListener l){
        mListener = l;
        return this;
    }

    public void search(final String key,final int page){
        final Handler handler = new Handler(){//handler 用于返回数据
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case Constant.SUCCESS:
                        if (mListener != null) mListener.onSearchResult((ArrayList<SearchResult>)msg.obj);
                        break;
                    case Constant.FAILED:
                        if (mListener != null) mListener.onSearchResult(null);
                        break;
                }
            }
        };

        //执行线程
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {//run方法
                ArrayList<SearchResult> results = getMusicList(key,page);
                if(results == null){
                    handler.sendEmptyMessage(Constant.FAILED);
                    return;
                }
                handler.obtainMessage(Constant.SUCCESS,results).sendToTarget();
            }
        });

    }

    //使用Jsoup组件请求网络,并解析音乐数据
    private ArrayList<SearchResult> getMusicList(final String key,final int page){
        final String start = String.valueOf((page - 1) * SIZE);
        ArrayList<SearchResult> searchResults = new ArrayList<SearchResult>();
        Document doc = null;
        String URL = null;
        try {

            //经过测试 获取页面 如果key不转码 无法打开正确连接
            //使用URLEncoder.encode转码,转为utf8
            //冰雨 转换为 %E6%B2%A1%E6%9C%89";
            String keyUrlEnCode = URLEncoder.encode(key, "utf8");
            URL = Constant.MIGU_SEARCH_HEAD + keyUrlEnCode +  Constant.MIGU_SEARCH_FOOT;
            System.out.println(URL);
            doc = Jsoup.connect(URL)
                    .data("query", "Java")
                    .userAgent(Constant.USER_AGENT)
                    .timeout(60 * 1000).get();
            //System.out.println("~~doc = " + doc);

            Elements songTitles = doc.select("span.fl.song_name");
            System.out.println(songTitles);
            Elements artists = doc.select("span.fl.singer_name.mr5");
            System.out.println(artists);
            for (int i=0;i<songTitles.size();i++){
                SearchResult searchResult = new SearchResult();
                //System.out.println("@searchResult : " + searchResult);

                //<a href="/song/121353608"
                //<a href="/song/264506450"
                //a链接,存在urls集合中;即,歌曲url集合;
                //a链接,第一个a连接,href属性的值;即,最终的url;
                //a链接,第一个a连接,text(a链接的内容,例:>半壶纱<,半壶纱就是a链接的内容);即,最终的歌名;
                Elements urls = songTitles.get(i).getElementsByTag("a");
                //System.out.println("@urls : " + urls);
                searchResult.setUrl(urls.get(0).attr("href"));//设置最终的url
                searchResult.setMusicName(urls.get(0).text());//设置最终的歌名

                //a链接,存在urls集合中;即,歌曲url集合;
                Elements artistElements = artists.get(i).getElementsByTag("a");
                //System.out.println("@artistElements : " + artistElements);
                searchResult.setArtist(artistElements.get(0).text());//设置最终的歌手

                searchResult.setAlbum("华语榜");//设置最终的专辑

                System.out.println("@searchResult : " + searchResult);
                searchResults.add(searchResult);//把最终的所有信息,添加到集合
            }
            System.out.println("@searchResults : " + searchResults);

            return searchResults;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public interface OnSearchResultListener {
        public void onSearchResult(ArrayList<SearchResult> results);
    }

}
