package me.yluo.ruisiapp.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import me.yluo.ruisiapp.App;
import me.yluo.ruisiapp.R;
import me.yluo.ruisiapp.adapter.BaseAdapter;
import me.yluo.ruisiapp.adapter.PostListAdapter;
import me.yluo.ruisiapp.api.entity.ApiForumList;
import me.yluo.ruisiapp.api.entity.ApiResult;
import me.yluo.ruisiapp.api.entity.ForumThreadlist;
import me.yluo.ruisiapp.database.MyDB;
import me.yluo.ruisiapp.listener.HidingScrollListener;
import me.yluo.ruisiapp.listener.LoadMoreListener;
import me.yluo.ruisiapp.model.ArticleListData;
import me.yluo.ruisiapp.myhttp.HttpUtil;
import me.yluo.ruisiapp.myhttp.ResponseHandler;
import me.yluo.ruisiapp.utils.DimmenUtils;
import me.yluo.ruisiapp.utils.GetId;
import me.yluo.ruisiapp.utils.UrlUtils;
import me.yluo.ruisiapp.widget.MyListDivider;

/**
 * 一般文章列表
 * 链接到校园网时 getPostsRs
 * 外网时 getPostsMe
 * 2个是不同的
 */
public class PostsActivity extends BaseActivity implements
        LoadMoreListener.OnLoadMoreListener, View.OnClickListener {

    private int FID = 72;
    private String TITLE;
    protected SwipeRefreshLayout refreshLayout;
    FloatingActionButton btnRefresh;
    RecyclerView mRecyclerView;
    private View myToolbar;
    //当前页数
    int currentPage = 1;
    int maxPage = 1;
    boolean isEnableLoadMore = false;
    RecyclerView.LayoutManager mLayoutManager;
    private TabLayout tab;

    boolean isHideZhiding = false;
    //一般板块/图片板块/手机板块数据列表
    private List<ArticleListData> datas;
    private PostListAdapter adapter;
    private MyDB myDB = null;
    private final String[] orders = new String[]{
            "&filter=lastpost&orderby=lastpost",
            "&filter=heat&orderby=heats",
            "&filter=hot",
            "&filter=digest&digest=1"
    };
    private int currentTabindex = 0;

    private static final Type forumListType = new TypeReference<ApiResult<ApiForumList>>() {
    }.getType();


    public static void open(Context context, int fid, String title) {
        Intent intent = new Intent(context, PostsActivity.class);
        intent.putExtra("FID", fid);
        intent.putExtra("TITLE", title);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        datas = new ArrayList<>();
        setContentView(R.layout.activity_posts);
        if (getIntent().getExtras() != null) {
            FID = getIntent().getExtras().getInt("FID");
            TITLE = getIntent().getExtras().getString("TITLE");
        }
        initToolBar(true, TITLE);
        myToolbar = findViewById(R.id.myToolBar);
        btnRefresh = findViewById(R.id.btn);
        mRecyclerView = findViewById(R.id.recycler_view);
        tab = findViewById(R.id.tab);

        refreshLayout = findViewById(R.id.refresh_layout);
        refreshLayout.setColorSchemeResources(R.color.red_light, R.color.green_light, R.color.blue_light, R.color.orange_light);
        int top = DimmenUtils.dip2px(this, 60);
        refreshLayout.setProgressViewOffset(true, top, top + 60);

        isHideZhiding = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("setting_hide_zhidin", true);
        if (getType() == PostListAdapter.TYPE_IMAGE) {
            isEnableLoadMore = false;
            mLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
            mRecyclerView.setHasFixedSize(false);
            addToolbarMenu(R.drawable.ic_column_change_24dp).setOnClickListener(this);
        } else {
            mLayoutManager = new LinearLayoutManager(this);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.addItemDecoration(new MyListDivider(this, MyListDivider.VERTICAL));
            mRecyclerView.addOnScrollListener(new LoadMoreListener((LinearLayoutManager) mLayoutManager, this, 8));
            addToolbarMenu(R.drawable.ic_edit).setOnClickListener(this);
        }

        tab.addTab(tab.newTab().setText("最新"));
        tab.addTab(tab.newTab().setText("热门"));
        tab.addTab(tab.newTab().setText("热帖"));
        tab.addTab(tab.newTab().setText("精华"));

        adapter = new PostListAdapter(this, datas, getType());
        if (getType() == PostListAdapter.TYPE_IMAGE) {
            adapter.setEnablePlaceHolder(false);
        }
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(adapter);
        myDB = new MyDB(this);
        datas.clear();
        btnRefresh.setOnClickListener(v -> refresh());
        init();
        //子类实现获取数据
        getData();
    }

    private int getType() {
        if (App.IS_SCHOOL_NET && (FID == 561 || FID == 157 || FID == 13)) {
            return PostListAdapter.TYPE_IMAGE;
        } else if (App.IS_SCHOOL_NET) {
            return PostListAdapter.TYPE_NORMAL;
        } else {
            return PostListAdapter.TYPE_NORMAL_MOBILE;
        }
    }

    @Override
    public void onLoadMore() {
        if (isEnableLoadMore) {
            if (currentPage < maxPage) {
                currentPage++;
            }
            isEnableLoadMore = false;
            getData();
        }
    }

    private void init() {
        btnRefresh.hide();
        refreshLayout.setRefreshing(true);
        refreshLayout.setOnRefreshListener(this::refresh);

        //隐藏按钮
        mRecyclerView.addOnScrollListener(new HidingScrollListener(getResources().getDimensionPixelSize(R.dimen.toolbarHeight)) {
            @Override
            public void onHide() {
                int distanceToScroll = btnRefresh.getHeight() + DimmenUtils.dip2px(PostsActivity.this, 16);
                btnRefresh.animate().translationY(distanceToScroll).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(200);

                //隐藏toolbar
                myToolbar.animate().translationY(-myToolbar.getHeight()).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(200);
                tab.animate().translationY(-myToolbar.getHeight()).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(200);
            }

            @Override
            public void onShow() {
                btnRefresh.animate().translationY(0).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(200);

                //显示toolbar
                myToolbar.animate().translationY(0).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(200);
                tab.animate().translationY(0).setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(200);
            }
        });


        tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabindex = tab.getPosition();
                refresh();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                mRecyclerView.scrollToPosition(0);
            }
        });
    }

    private void refresh() {
        btnRefresh.hide();
        refreshLayout.setRefreshing(true);
        currentPage = 1;
        maxPage = 1;
        getData();
    }


    private void getData() {
        adapter.changeLoadMoreState(BaseAdapter.STATE_LOADING);
        String url;
        if (!App.IS_SCHOOL_NET) {
            url = UrlUtils.getArticleListUrl(FID, currentPage, false);
        } else if (getType() == PostListAdapter.TYPE_IMAGE) {
            url = UrlUtils.getArticleListUrl(FID, currentPage, true);
        } else {
            url = UrlUtils.getArticleListApiUrl(FID, currentPage);
        }

        url = url + orders[currentTabindex];

        HttpUtil.get(url, new ResponseHandler() {
            @Override
            public void onSuccess(byte[] response) {
                switch (getType()) {
                    case PostListAdapter.TYPE_IMAGE:
                        new getImagePosts().execute(new String(response));
                        break;
                    case PostListAdapter.TYPE_NORMAL:
                        new getPostsApi().execute(response);
                        break;
                    case PostListAdapter.TYPE_NORMAL_MOBILE:
                        //外网
                        new getPostsMe().execute(new String(response));
                        break;
                }
            }

            @Override
            public void onFailure(Throwable e) {
                refreshLayout.postDelayed(() -> refreshLayout.setRefreshing(false), 500);
                adapter.changeLoadMoreState(BaseAdapter.STATE_LOAD_FAIL);
            }
        });
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.menu:
                if (getType() == PostListAdapter.TYPE_IMAGE) {
                    StaggeredGridLayoutManager m = (StaggeredGridLayoutManager) mLayoutManager;
                    int span = m.getSpanCount();
                    if (span == 1) {
                        m.setSpanCount(2);
                    } else {
                        m.setSpanCount(1);
                    }
                } else {
                    if (isLogin()) {
                        Intent i = new Intent(this, NewPostActivity.class);
                        i.putExtra("FID", FID);
                        i.putExtra("TITLE", TITLE);
                        startActivityForResult(i, 0);
                    }
                }
        }
    }


    //接受发帖是否成功
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            //发帖成功 刷新
            refresh();
        }
    }

    //校园网状态下获得一个普通板块文章列表数据 根据html获得数据
    private class getPostsRs extends AsyncTask<String, Void, List<ArticleListData>> {
        @Override
        protected List<ArticleListData> doInBackground(String... params) {
            String res = params[0];
            List<ArticleListData> tempDatas = new ArrayList<>();
            Document document = Jsoup.parse(res);
            Elements list = document.select("#threadlist tbody");
            ArticleListData temp;
            for (Element li : list) {

                String type = "normal";
                if (li.attr("id").contains("stickthread")) {
                    if (isHideZhiding)
                        continue;
                    type = "置顶";
                } else {
                    Element element = li.selectFirst("tr > th > span.xi1");
                    if (element != null && element.text().contains("回帖奖励")) {
                        type = "金币:" + GetId.getNumber(element.text());
                    } else {
                        Element e = li.selectFirst(".icn a");
                        if (e != null) {
                            String title = e.attr("title");
                            if (title.startsWith("投票")) {
                                type = "投票";
                            } else if (title.startsWith("关闭的主题")) {
                                type = "关闭";
                            }
                        }
                    }
                }

                Element titleElement = li.selectFirst("tr > th > a.s.xst");
                if (titleElement == null) continue;
                String title = titleElement.text();
                String titleUrl = titleElement.attr("href");
                int titleColor = GetId.getColor(PostsActivity.this, titleElement.attr("style"));

                //#ajaxid_0\2e 0612111796964776

                String author, authorUrl;
                //Element authorNode = li.selectFirst("tr > td:nth-child(3) > cite > a");
                Element authorNode = li.selectFirst("tr > td > cite > a");
                if (authorNode == null) {
                    author = "未知";
                    authorUrl = "";
                } else {
                    author = authorNode.text();
                    authorUrl = authorNode.attr("href");
                }

                String time, viewcount, replaycount;
                //Element timeNode = li.selectFirst("tr > td:nth-child(3) > em");
                Element timeNode = li.selectFirst("tr > td > em");
                if (timeNode == null) {
                    time = "未知时间";
                } else {
                    time = timeNode.text();
                }

                //Element viewsNode = li.selectFirst("tr > td:nth-child(4) > em");
                Elements viewsAndReplyNode = li.select("tr").select("td.num");
                if (viewsAndReplyNode != null) {
                    // 拿到了查看数量和回复数量
                    //Element viewsNode = li.selectFirst("tr > td:nth-child(4) > em");
                    replaycount = viewsAndReplyNode.select("a").first().text();
                    viewcount = viewsAndReplyNode.select("em").first().text();
                } else {
                    viewcount = "0";
                    replaycount = "0";
                }
                /*
                if (viewsNode == null) {
                    viewcount = "0";
                } else {
                    viewcount = viewsNode.text();
                }

                //Element replysNode = li.selectFirst("tr > td:nth-child(4) > a");
                Element replysNode = li.selectFirst("tr > td > a");
                if (replysNode == null) {
                    replaycount = "0";
                } else {
                    replaycount = replysNode.text();
                }*/

                String tag = li.select("em a[href^=forum.php?mod=forumdisplay]").text();
                if (title.length() > 0 && author.length() > 0) {
                    temp = new ArticleListData(type, title, titleUrl, author, authorUrl, time, viewcount, replaycount, titleColor);
                    if (!TextUtils.isEmpty(tag)) temp.tag = tag;
                    tempDatas.add(temp);
                }

            }

            Element page = document.select("#fd_page_bottom .pg").first();
            if (page == null) {
                maxPage = currentPage;
            } else {
                maxPage = GetId.getNumber(page.select("label span").text());
            }

            return myDB.handReadHistoryList(tempDatas);
        }

        @Override
        protected void onPostExecute(List<ArticleListData> dataset) {
            getDataCompete(dataset);
        }
    }

    //校园网状态下解析Api,目前外网不支持API不然就切换到API了
    private class getPostsApi extends AsyncTask<byte[], Void, List<ArticleListData>> {
        @Override
        protected List<ArticleListData> doInBackground(byte[]... params) {
            ApiResult<ApiForumList> res = JSON.parseObject(params[0], forumListType);
            List<ForumThreadlist> topics = res.Variables.forum_threadlist;
            List<ArticleListData> tempDatas = new ArrayList<>();
            ArticleListData temp;

            for (ForumThreadlist topic : topics) {
                String type = "normal";
                if (topic.displayorder == 1) {
                    if (isHideZhiding) {
                        continue;
                    }
                    type = "置顶";
                }

                //TODO 金币 投票 关闭
                //TODO color
                int color = ContextCompat.getColor(PostsActivity.this, R.color.text_color_pri);
                String url = "这里是URl.php?mod=viewthread&tid="+topic.tid;
                temp = new ArticleListData(type, topic.subject,url, topic.author, topic.authorid,
                        topic.dateline.replace("&nbsp;", " "), topic.views, topic.replies, color);
                //if (!TextUtils.isEmpty(tag)) temp.tag = tag;
                tempDatas.add(temp);
            }

            maxPage = Integer.MAX_VALUE;
            if (topics.size() == 0) {
                maxPage = currentPage;
            }

            return myDB.handReadHistoryList(tempDatas);
        }

        @Override
        protected void onPostExecute(List<ArticleListData> dataset) {
            getDataCompete(dataset);
        }
    }

    //非校园网状态下获得一个板块文章列表数据
    //根据html获得数据
    //调用的手机版
    private class getPostsMe extends AsyncTask<String, Void, List<ArticleListData>> {
        @Override
        protected List<ArticleListData> doInBackground(String... params) {
            //chiphell
            String res = params[0];
            List<ArticleListData> dataset = new ArrayList<>();
            Document doc = Jsoup.parse(res);
            Elements body = doc.select("div[class=threadlist]"); // 具有 href 属性的链接
            ArticleListData temp;
            Elements links = body.select("li");
            for (Element src : links) {
                String url = src.select("a").attr("href");
                int titleColor = GetId.getColor(PostsActivity.this, src.select("a").attr("style"));
                String author = src.select(".by").text();
                src.select("span.by").remove();
                String title = src.select("a").text();
                String replyCount = src.select("span.num").text();
                String img = src.select("img").attr("src");
                boolean hasImage = img.contains("icon_tu.png");
                temp = new ArticleListData(hasImage, title, url, author, replyCount, titleColor);
                dataset.add(temp);
            }

            Element page = doc.select(".pg").first();
            if (page == null) {
                maxPage = currentPage;
            } else {
                maxPage = GetId.getNumber(page.select("label span").text());
            }

            return myDB.handReadHistoryList(dataset);
        }

        @Override
        protected void onPostExecute(List<ArticleListData> dataset) {
            getDataCompete(dataset);
        }
    }


    //校园网状态下获得图片板块数据 图片链接、标题等  根据html获得数据
    private class getImagePosts extends AsyncTask<String, Void, List<ArticleListData>> {

        @Override
        protected List<ArticleListData> doInBackground(String... params) {
            String response = params[0];
            Document document = Jsoup.parse(response);
            Elements list = document.select("ul[id=waterfall]");
            Elements imagelist = list.select("li");
            List<ArticleListData> temps = new ArrayList<>();
            for (Element tmp : imagelist) {
                //链接不带前缀
                //http://rs.xidian.edu.cn/
                String img = tmp.select("img").attr("src");
                String url = tmp.select("h3.xw0").select("a[href^=forum.php]").attr("href");
                String title = tmp.select("h3.xw0").select("a[href^=forum.php]").text();
                String author = tmp.select("a[href^=home.php]").text();
                String replyCount = tmp.select(".xg1.y").select("a[href^=forum.php]").text();
                tmp.select(".xg1.y").select("a[href^=forum.php]").remove();
                temps.add(new ArticleListData(title, url, img, author, replyCount));
            }

            Element page = document.select("#fd_page_bottom .pg").first();
            if (page == null) {
                maxPage = currentPage;
            } else {
                maxPage = GetId.getNumber(page.select("label span").text());
            }

            return temps;
        }

        @Override
        protected void onPostExecute(List<ArticleListData> dada) {
            getDataCompete(dada);
        }
    }

    private void getDataCompete(List<ArticleListData> dataset) {
        btnRefresh.show();
        if (currentPage == 1) {
            datas.clear();
            adapter.notifyDataSetChanged();
        }
        int start = datas.size();
        datas.addAll(dataset);

        adapter.notifyItemRangeInserted(start, dataset.size());

        if (currentPage < maxPage) {
            adapter.changeLoadMoreState(BaseAdapter.STATE_LOADING);
        } else {
            adapter.changeLoadMoreState(BaseAdapter.STATE_LOAD_NOTHING);
        }
        isEnableLoadMore = true;
        refreshLayout.postDelayed(() -> refreshLayout.setRefreshing(false), 500);
    }
}
