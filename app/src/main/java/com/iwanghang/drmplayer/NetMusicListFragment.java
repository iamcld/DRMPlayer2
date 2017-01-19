package com.iwanghang.drmplayer;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.iwanghang.drmplayer.adapter.NetMusicListAdapter;
import com.iwanghang.drmplayer.utils.AppUtils;
import com.iwanghang.drmplayer.utils.Constant;
import com.iwanghang.drmplayer.utils.SearchMusicUtils;
import com.iwanghang.drmplayer.vo.SearchResult;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

public class NetMusicListFragment extends Fragment implements OnItemClickListener,OnClickListener {

    private ListView listView_net_music;
    private LinearLayout load_layout;
    private LinearLayout ll_search_btn_container;//查询按钮的容器
    private LinearLayout ll_search_container;//查询按钮的容器
    private ImageButton ib_search_btn;
    private EditText et_search_content;
    private NetMusicListAdapter netMusicListAdapter;

    //存放 网络音乐 的集合
    private ArrayList<SearchResult> searchResults = new ArrayList<SearchResult>();

    private int page = 1;//搜索音乐的页码

    private MainActivity mainActivity;

    //private boolean isPause = false;//歌曲播放中的暂停状态

    private int position = 0;//当前播放的位置,提供给PlayActivity

    //onAttach(),当fragment被绑定到activity时被调用(Activity会被传入.).
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //mainActivity = (MainActivity) context;
        mainActivity = (MainActivity) getActivity();
    }

    public static NetMusicListFragment newInstance() {
        NetMusicListFragment net = new NetMusicListFragment();
        return net;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //UI组件初始化
        View view = inflater.inflate(R.layout.net_music_list_layout,null);
        //item
        listView_net_music = (ListView) view.findViewById(R.id.listView_net_music);
        //findViewById
        load_layout = (LinearLayout) view.findViewById(R.id.load_layout);
        ll_search_btn_container = (LinearLayout) view.findViewById(R.id.ll_search_btn_container);
        ll_search_container = (LinearLayout) view.findViewById(R.id.ll_search_container);
        ib_search_btn = (ImageButton) view.findViewById(R.id.ib_search_btn);
        et_search_content = (EditText) view.findViewById(R.id.et_search_content);

        //Item点击事件监听
        listView_net_music.setOnItemClickListener(this);
        //按钮点击事件监听
        ll_search_btn_container.setOnClickListener(this);
        ib_search_btn.setOnClickListener(this);

        loadNetData();//加载网络音乐
        return view;
    }

    private void loadNetData() {
        load_layout.setVisibility(View.VISIBLE);
        //加载网络音乐的异步任务
        new LoadNetDataTask().execute(Constant.MIGU_CHINA);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ll_search_btn_container:
                ll_search_btn_container.setVisibility(View.GONE);
                ll_search_container.setVisibility(View.VISIBLE);
                break;
            case R.id.ib_search_btn:
                //搜索事件
                searchMusic();
                break;
        }
    }

    //搜索音乐
    private void searchMusic() {
        //隐藏键盘
        AppUtils.hideInputMethod(et_search_content);
        ll_search_btn_container.setVisibility(View.VISIBLE);
        ll_search_container.setVisibility(View.GONE);
        //获取输入的文字
        String key = et_search_content.getText().toString();
        if (TextUtils.isEmpty(key)){//如果为空,则,Toast提示
            Toast.makeText(mainActivity,"请输入歌手或歌词",Toast.LENGTH_SHORT).show();
            return;
        }
        load_layout.setVisibility(View.VISIBLE);//加载layout效果.显示
        //填充item 使用SearchMusicUtils搜索音乐工具类,并,使用观察者设计模式,自己回调,自己监听
        SearchMusicUtils.getInstance().setListener(new SearchMusicUtils.OnSearchResultListener(){
            @Override
            public void onSearchResult(ArrayList<SearchResult> results) {
                ArrayList<SearchResult> sr = netMusicListAdapter.getSearchResults();
                sr.clear();
                sr.addAll(results);
                netMusicListAdapter.notifyDataSetChanged();//更新网络音乐列表
                load_layout.setVisibility(View.GONE);
            }
        }).search(key,page);
    }

    //列表项的单击事件
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position >= netMusicListAdapter.getSearchResults().size() || position < 0) return;
        showDownloadDialog(position);
    }

    //下载弹窗
    private void showDownloadDialog(final int position){
        //DownloadDialogFragment downloadDialogFragment = new DownloadDialogFragment(); // 不传值的写法
        // 以下是,通过newInstance,把searchResults.get(position)传递给downloadDialogFragment的写法
        DownloadDialogFragment downloadDialogFragment = DownloadDialogFragment.newInstance(searchResults.get(position));
        downloadDialogFragment.show(getFragmentManager(),"download");
    }

    //加载网络音乐的异步任务
    //Android1.5提供了 工具类 android.os.AsyncTask，它使创建异步任务变得更加简单，不再需要编写任务线程和Handler实例即可完成相同的任务。
    class LoadNetDataTask extends AsyncTask<String,Integer,Integer>{
        //onPreExecute方法用于在执行后台任务前做一些UI操作
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            load_layout.setVisibility(View.VISIBLE);//加载layout.显示
            listView_net_music.setVisibility(View.GONE);//item.隐藏
            searchResults.clear();//搜索结果.清理
        }

        //doInBackground方法内部执行后台任务,不可在此方法内修改UI
        @Override
        protected Integer doInBackground(String... params) {
            String url = params[0];
            try {

                //url = "http://music.migu.cn/webfront/searchNew/searchAll.do?keyword=%E5%88%98%E5%BE%B7%E5%8D%8E&keytype=all&pagesize=20&pagenum=1";

                //使用Jsoup组件请求网络,并解析音乐数据,get请求
                Document doc = Jsoup.connect(url).userAgent(Constant.USER_AGENT).timeout(6000).get();
                System.out.println("start**********doc**********doc**********doc**********");
                System.out.println(doc);
                System.out.println(" end **********doc**********doc**********doc**********");

                //寻找该页面中自己需要的内容
                //doc实际上就是页面右击显示的源代码
                //从doc分析以上html代码;把所有span标签下fl song_name查找出来,存在songTitles集合中;即,歌名集合;
                //从doc分析以上html代码;把所有span标签下fl singer_name.mr5t查找出来,存在artists集合中;即,歌手集合;
                //查找标签为span，class为fl 名字为song_name的内容(css)，获得“<a href”(a链接)中的url,title(歌名)等
                Elements songTitles = doc.select("span.fl.song_name");//歌名
                System.out.println(songTitles);
                Elements artists = doc.select("span.fl.singer_name.mr5");//歌手
                System.out.println(artists);

                for (int i=0;i<songTitles.size();i++){
                    SearchResult searchResult = new SearchResult();

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
                //System.out.println("@songTitles.size() : " + searchResults.size());
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
            return 1;
        }

        //onPostExecute方法用于在执行完后台任务后更新UI,显示结果
        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result==1){
                netMusicListAdapter = new NetMusicListAdapter(mainActivity,searchResults);
                //System.out.println(searchResults);
                listView_net_music.setAdapter(netMusicListAdapter);
                //提示列表已经到最后了
                listView_net_music.addFooterView(LayoutInflater.from(mainActivity).inflate(R.layout.footviwe_layout,null));
            }
            load_layout.setVisibility(View.GONE);
            listView_net_music.setVisibility(View.VISIBLE);

            }
        }

    }

