package remix.myplayer.activities;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.facebook.common.internal.Supplier;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import remix.myplayer.R;
import remix.myplayer.adapters.SlideMenuAdapter;
import remix.myplayer.fragments.AllSongFragment;
import remix.myplayer.fragments.BottomActionBarFragment;
import remix.myplayer.fragments.MainFragment;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.services.MusicService;
import remix.myplayer.services.NotifyService;
import remix.myplayer.ui.TimerPopupWindow;
import remix.myplayer.utils.CommonUtil;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.utils.QQApi;
import remix.myplayer.utils.SharedPrefsUtil;
import remix.myplayer.utils.XmlUtil;

public class MainActivity extends AppCompatActivity implements MusicService.Callback{
    public static MainActivity mInstance = null;
    private MusicService mService;
    private BottomActionBarFragment mBottomBar;
    private final static String TAG = "MainActivity";
    private boolean mFromNotify = false;
    private ListView mSlideMenuList;
    private DrawerLayout mDrawerLayout;
    private LinearLayout mDrawerMenu;
    private ImageButton mSlideMenuBtn;
    private ImageButton mSlideMenuAbout;
    private ImageButton mSlideMenuExit;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar mToolBar;
//    private ServiceConnection mConnecting = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            mService = ((MusicService.PlayerBinder)service).getService();
//            mService.addCallback(MainActivity.this);
//        }
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            mService = null;
//        }
//    };
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new Thread(){
            @Override
            public void run(){
                String temp = QQApi.Test("七里香","周杰伦");
            }
        }.start();


        initUtil();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        mInstance = this;

        mFromNotify = getIntent().getBooleanExtra("Notify",false);
        if(!mFromNotify) {
            loadsongs();
            startService(new Intent(this,MusicService.class));
            //NofityService
            startService(new Intent(this, NotifyService.class));
        }
        //播放的service
        MusicService.addCallback(MainActivity.this);
        //加载主页fragment
        initMainFragment();
        //初始化测滑菜单
        initDrawerLayout();
        //初始化底部状态栏
        mBottomBar = (BottomActionBarFragment)getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);

        initToolbar();

        if(DBUtil.mPlayingList == null || DBUtil.mPlayingList.size() == 0)
            return;

        boolean mFir = SharedPrefsUtil.getValue(getApplicationContext(),"setting","mFirst",true);
        int mPos = SharedPrefsUtil.getValue(getApplicationContext(),"setting","mPos",-1);
        SharedPrefsUtil.putValue(getApplicationContext(),"setting","mFirst",false);

        //第一次启动添加我的收藏列表
        if(mFir){
            XmlUtil.addPlaylist("我的收藏");
        }
        //如果是第一次启动软件,将第一首歌曲设置为正在播放的
        if(mFir || mPos < 0)
            mBottomBar.UpdateBottomStatus(DBUtil.getMP3InfoById(DBUtil.mPlayingList.get(0)),mFromNotify);
        else
            mBottomBar.UpdateBottomStatus(DBUtil.getMP3InfoById(DBUtil.mPlayingList.get(mPos)), mFromNotify);
    }

    private void initToolbar() {
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle("");

        setSupportActionBar(mToolBar);
        mToolBar.setNavigationIcon(R.drawable.btn_toolbar_menu);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(mDrawerMenu);
            }
        });
        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.toolbar_search:
                        startActivity(new Intent(MainActivity.this, SearchActivity.class));
                        break;
                    case R.id.toolbar_timer:
                        startActivity(new Intent(MainActivity.this, TimerPopupWindow.class));
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }


    private void initUtil() {
        //初始化库和工具类
        XmlUtil.setContext(getApplicationContext());
        DBUtil.setContext(getApplicationContext());
        CommonUtil.setContext(getApplicationContext());

        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setBitmapMemoryCacheParamsSupplier(new Supplier<MemoryCacheParams>() {
                    @Override
                    public MemoryCacheParams get() {
                        //50M内存缓存
                        return new MemoryCacheParams(50 * 1024 * 1024,10,2048,5,1024);
                    }
                }).build();
        Fresco.initialize(this,config);

        DisplayImageOptions option = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.default_recommend)
                .showImageOnFail(R.drawable.default_recommend                                                                                                                                                                                                                                                                                         )
                .resetViewBeforeLoading(false)
                .cacheOnDisk(true)
                .cacheInMemory(true)
                .build();

        ImageLoaderConfiguration config1 = new ImageLoaderConfiguration.Builder(this)
                .diskCacheSize(50 * 1024 * 1024) // 50 Mb sd卡(本地)缓存的最大值
                .diskCacheFileCount(50)
                .defaultDisplayImageOptions(option)
                .build();
        ImageLoader.getInstance().init(config1);
    }

    private void initMainFragment() {


        getSupportFragmentManager().beginTransaction().add(R.id.main_fragment_container, new MainFragment(), "MainFragment").addToBackStack(null).commit();
    }

    private void initDrawerLayout(){
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this,mDrawerLayout,mToolBar,R.string.drawerlayout_open,R.string.drawerlayout_open);
//        mDrawerLayout.setDrawerListener(mDrawerToggle);
//        mDrawerToggle.syncState();

        mDrawerMenu = (LinearLayout)findViewById(R.id.slide_menu);
        mSlideMenuList = (ListView) mDrawerMenu.findViewById(R.id.slide_menu_list);
        mSlideMenuList.setAdapter(new SlideMenuAdapter(getLayoutInflater()));
        mSlideMenuList.setOnItemClickListener(new SlideMenuListener(this));

        mSlideMenuAbout = (ImageButton)findViewById(R.id.drawer_about);
        mSlideMenuExit = (ImageButton)findViewById(R.id.drawer_exit);
        mSlideMenuExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast(new Intent(Constants.EXIT));
            }
        });

    }

    class SlideMenuListener implements AdapterView.OnItemClickListener {
        private Context mContext;

        public SlideMenuListener(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (view.getId()) {
                case 0:
                    mDrawerLayout.closeDrawer(mDrawerMenu);
                    MainFragment.mInstance.getViewPager().setCurrentItem(0);
//                    mDrawerMenu.toggle();
//                    mViewPager.setCurrentItem(0);
                    break;
                case 1:
                    startActivity(new Intent(MainActivity.this, PlayListActivity.class));
                    break;
                case 2:
                    Intent intent = new Intent(Constants.CTL_ACTION);
                    intent.putExtra("Control", Constants.PREV);
                    sendBroadcast(intent);
                    break;
                default:break;
            }
        }
    }

    //读取sd卡歌曲信息
    public static void loadsongs()
    {
        //读取所有歌曲信息
        FutureTask<ArrayList<Long>> task = new FutureTask<ArrayList<Long>>(new Callable<ArrayList<Long>>() {
            @Override
            public ArrayList<Long> call() throws Exception {
                return DBUtil.getAllSongsId();
            }
        });
        new Thread(task, "getInfo").start();
        try {
            DBUtil.mAllSongList = task.get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //读取正在播放列表信息
        FutureTask<ArrayList<Long>> task1 = new FutureTask<ArrayList<Long>>(new Callable<ArrayList<Long>>() {
            @Override
            public ArrayList<Long> call() throws Exception {
                return XmlUtil.getPlayingList();
            }
        });
        new Thread(task1, "getPlayingList").start();
        try {
            DBUtil.mPlayingList = task1.get();
            if(DBUtil.mPlayingList == null || DBUtil.mPlayingList.size()  == 0)
                DBUtil.mPlayingList = (ArrayList<Long>)task.get().clone();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if(mDrawerLayout.isDrawerOpen(mDrawerMenu))
            mDrawerLayout.closeDrawer(mDrawerMenu);
        else {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
            sendBroadcast(new Intent(Constants.NOTIFY));
        }
    }

    //后退返回桌面
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if(keyCode == KeyEvent.KEYCODE_BACK) {
//            Intent home = new Intent(Intent.ACTION_MAIN);
//            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            home.addCategory(Intent.CATEGORY_HOME);
//            startActivity(home);
//            sendBroadcast(new Intent(Constants.NOTIFY));
//        }
//        return super.onKeyDown(keyCode, event);
//    }

    @Override
    public void UpdateUI(MP3Info MP3info, boolean isplay){
        MP3Info temp = MP3info;
        mBottomBar.UpdateBottomStatus(MP3info, isplay);
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        for(Fragment fragment : fragmentList){
            if(fragment instanceof AllSongFragment){
                ((AllSongFragment) fragment).getAdapter().notifyDataSetChanged();
            }
        }
    }

    @Override
    public int getType() {
        return 0;
    }

}

