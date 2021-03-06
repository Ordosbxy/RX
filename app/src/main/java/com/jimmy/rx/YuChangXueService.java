package com.jimmy.rx;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.hjq.xtoast.XToast;
import com.jimmy.tool.ToastUtils;
import com.jimmy.tool.Utils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static com.jimmy.rx.AbstractTF.isEmptyArray;


/**
 * 电视台观看视频的时候，是不会有点阅号的。
 * 阅读文章的时候要特别关注，是不是会有。
 */
public class YuChangXueService extends AccessibilityService {
    LinkedList linkedList = new LinkedList();//用来存放当天视频主题，文章主题。防止看重复

    private final String TAG = getClass().getName();
    public static int videoNum = 0;
    public static int bookNum = 0;
    public static int collectNum = 0;
    private String Dianshitai = "cn.xuexi.android:id/home_bottom_tab_button_contact";
    private String Bailing = "cn.xuexi.android:id/home_bottom_tab_button_ding";
    private String Wenzhang = "cn.xuexi.android:id/home_bottom_tab_button_work";
    private String score = "cn.xuexi.android:id/comm_head_xuexi_score";


    private long videoTime = 120 * 1000;
    private int videoNumMax = 12;
    private long bookTime = 120 * 1000;
    private int bookNumMax = 12;

//    private long videoTime = 20 * 1000;
//    private int videoNumMax = 1;
//    private long bookTime = 20 * 1000;
//    private int bookNumMax = 1;

    private String BOOK_KEY = "bookkey";

    private boolean isSearWebView = false;//是否去搜寻webview的Des字段（）


    public static YuChangXueService mService;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0://模拟点击视屏
//                    if (isHome()) {
                    videoNum++;
                    updateShow("点击视频,视频数增加");
                    dispatchGestureClick(400, MainActivity.height / 3);//模拟点击一条视频新闻
                    isSearWebView = true;
                    mHandler.sendEmptyMessageDelayed(1, videoTime);
//                        mHandler.sendEmptyMessageDelayed(12, 8000);//8秒后检测，是否处在视频详情页面
                    updateShow("第" + videoNum + "条视频,正在观看" + videoTime / 1000 + "秒,请勿关闭");
//                    } else {
//                        updateShow("检测当前非首页，请返回首页");
//                        ServiceUtils.performClickWithID(YuChangXueService.this, Dianshitai);
//                        mHandler.sendEmptyMessageDelayed(0, 2000);
//                    }
                    break;
                case 1://播放视频界面停留之后，点击返回
                    //在播放视频界面，10s之后，触发返回
//                    if (isInApp(packageName)) {
                    if (!isHome()) {
                        isSearWebView = false;
                        updateShow("全局检测，非主页视频界面返回" + isPingLun());
                        YuChangXueService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                        //返回之后,要求在首页才会去触发滑动
                        mHandler.sendEmptyMessageDelayed(2, 500);
                    } else {
                        updateShow("检测当前页面处在首页，前一次视频播放失败");
                        mHandler.sendEmptyMessageDelayed(2, 500);
                    }
//                    } else {
//                        updateShow("检测<学习强国>非前台运行，请将应用打开");
//                        mHandler.sendEmptyMessageDelayed(1, 1000);
//                    }
                    break;
//                case 12://检测是否处在视频播放界面
//                    if (isPingLun()) {
//                        updateShow("检测：当前处在视频播放界面，执行观看等待…………");
//                    } else {
//                        updateShow("检测：当前处在非视频播放界面！！！！");
//                        mHandler.removeMessages(1);//停掉视频观看
//                        videoNum--;
//                        mHandler.sendEmptyMessageDelayed(1, 500);
//                    }
//                    break;
                case 2://回到首页，滑动
                    if (videoNum >= videoNumMax) {
                        updateShow("设定的视频数量，已经播放完毕");
                        mHandler.sendEmptyMessageDelayed(3, 1000);//去跳转到文章阅读
                        return;
                    } else {
                        if (isHome()) {
                            updateShow("首页视频列表开始滚动");
                            Path path = new Path();
                            path.moveTo(400, MainActivity.height / 2);
                            path.lineTo(400, 0);
                            dispatchGestureMove(path, 40);
                            mHandler.sendEmptyMessageDelayed(0, 2000);
                        } else {
                            updateShow("未检测到首页，请自动恢复页面到软件首页");
                            mHandler.sendEmptyMessageDelayed(2, 1000);
                        }
                    }
                    break;
                case 3://学习文章
                    if (isHome()) {
                        updateShow("开始学习文章");
                        ServiceUtils.performClickWithID(YuChangXueService.this, Wenzhang);
                        mHandler.sendEmptyMessageDelayed(4, 500);
                    } else {
                        updateShow("未检测到首页，请自动恢复《学习强国》至首页");
                        mHandler.sendEmptyMessageDelayed(3, 1000);
                    }
                    break;

                case 4://点击book
                    if (isHome()) {
                        bookNum++;
                        updateShow("点击文章");
                        dispatchGestureClick(400, MainActivity.height / 3);//模拟点击一条文章
                        isSearWebView = true;
                        if (collectNum < 4) {//去收藏
                            updateShow("收藏数量小于4，需要收藏");
                            collectNum++;
                            mHandler.sendEmptyMessageDelayed(7, 2000);//去收藏
                        }
                        mHandler.sendEmptyMessageDelayed(11, 1000);//视听
                        //去执行阅读
                        mHandler.sendEmptyMessageDelayed(5, bookTime);
                        updateShow("第" + bookNum + "篇," + "阅读文章" + bookTime / 1000 + "秒，请勿关闭");
                    } else {
                        updateShow("未检测到首页，请自动恢复《学习强国》至首页");
                        mHandler.sendEmptyMessageDelayed(4, 1000);
                    }

                    break;
                case 7://点击收藏
//                    if (webNode != null && webNode.getContentDescription() != null) {
//                        Log.d("YuChangXueService", webNode.getContentDescription().toString());
//                    }
                    clickCollect();
                    mHandler.sendEmptyMessageDelayed(8, 1000);
//                    if (isCanClickCollect()) {
//                        dispatchGestureClick(900, 1820);//模拟点击最右边的更多
//                    }
////                    dispatchGestureClick(50, 1900);//模拟点击最右边的更多
//                    mHandler.sendEmptyMessageDelayed(8, 1000);
                    break;
                case 8://弹出分享框
                    clickShare();
                    break;
                case 9://点击分享按钮
                    clickWXshare();
                    break;
                case 10://从强国的分享返回
                    if (!isHome()) {
                        updateShow("分享成功，从分享页面返回");
                        YuChangXueService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                    }
                    break;
                case 11://开启播放语音
                    updateShow("播放视听");
                    clickSound();
                    break;
                case 5://从文章页面返回
                    if (!isHome()) {
                        updateShow("从文章页面返回");
                        YuChangXueService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                        isSearWebView = false;
                    }
                    //返回之后,要求在首页才会去触发滑动
                    mHandler.sendEmptyMessageDelayed(6, 1000);
                    break;
                case 6://首页去滑动文章页面
                    if (bookNum >= bookNumMax) {
                        updateShow("文章阅读完毕，准备完成阅读本地频道任务");
                        mHandler.sendEmptyMessageDelayed(995, 1000);
                        return;
                    }

                    if (bookNum == 3 || bookNum == 7) {//观看到这几个的时候，需要横向滑动
                        if (isHome()) {
                            updateShow("开始横向滑动，切换文章卡片（尽量在前几个卡片滑动）");
                            Path path = new Path();
                            path.moveTo(800, MainActivity.height / 5 * 4);
                            path.lineTo(0, MainActivity.height / 5 * 4);
                            dispatchGestureMove(path, 50);
                        }
                    } else {
                        if (isHome()) {
                            updateShow("开始纵向滑动，切换文章列表");
                            Path path = new Path();
                            path.moveTo(400, MainActivity.height / 2);
                            path.lineTo(400, 0);
                            dispatchGestureMove(path, 20);
                        }
                    }
                    mHandler.sendEmptyMessageDelayed(4, 2500);
                    break;
                case 995://点击分数
                    updateShow("点击分数，进入任务界面");
                    ServiceUtils.performClickWithID(YuChangXueService.this, score);
                    mHandler.sendEmptyMessageDelayed(999, 2000);
                    break;
                case 999://进入到分数界面之后，滑动到最下面
                    updateShow("进入到页面，准备滑动到最底部");
                    Path path = new Path();
                    path.moveTo(400, MainActivity.height / 2 + 200);
                    path.lineTo(400, 0);
                    dispatchGestureMove(path, 20);
                    mHandler.sendEmptyMessageDelayed(1000, 2000);
                    break;

                case 1000://点击本地频道,去看看按钮
                    updateShow("点击本地频道去看看按钮");
                    dispatchGestureClick(1050, 2130);
                    mHandler.sendEmptyMessageDelayed(1001, 2000);
                    break;
                case 1001:
                    if (isHome()) {
                        updateShow("当前处在首页，点击本地频道文章，阅读文章");
                        dispatchGestureClick(400, MainActivity.height / 3);//模拟点击一条本地文章
                        mHandler.sendEmptyMessageDelayed(1002, bookTime);//查看时间，时间到达
                    } else {
                        updateShow("本地频道任务结束,返回首页");
                        mHandler.sendEmptyMessageDelayed(1002, 1000);
                    }
                    break;
                case 1002:
                    if (!isHome()) {
                        updateShow("返回，从本地任务页面返回");
                        YuChangXueService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                    }
                    updateShow("========关闭学习辅助========");
                    updateShow("全部任务执行完毕，3秒准备关闭...");
                    mHandler.sendEmptyMessageDelayed(1011, 3000);
                case 1011:
//                    YuChangXueService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                    if (t != null) {
                        t.cancel();
                    }
                    break;
            }
        }
    };


    public static boolean canHome = true;//是否是从首页进入


    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        ToastUtils.showShort("服务已经连接");
        mService = this;
        AccessibilityServiceInfo serviceInfo = new AccessibilityServiceInfo();
//        serviceInfo.eventTypes = TYPE_WINDOW_STATE_CHANGED;
        serviceInfo.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        serviceInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        serviceInfo.packageNames = new String[]{"cn.xuexi.android", "com.android.systemui"};// 监控的app
        serviceInfo.notificationTimeout = 100;
        //设置可以监控webview
        serviceInfo.flags = serviceInfo.flags | AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY;
        final AccessibilityServiceInfo info = getServiceInfo();
        //获取到webview的内容
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY;

        setServiceInfo(serviceInfo);
    }

    AccessibilityEvent accessibilityEvent;
    String packageName = "";

    //实现辅助功能
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        this.accessibilityEvent = accessibilityEvent;
        Log.d("YuChangXueService", accessibilityEvent.getPackageName() + "\n" + accessibilityEvent.getClassName());
//        Log.d("YuChangXueService", "accessibilityEvent.getEventType():" + accessibilityEvent.getEventType());
        packageName = accessibilityEvent.getPackageName().toString();
//        Log.d("YuChangXueService", "accessibilityEvent.getPackageName():" + accessibilityEvent.getPackageName());
//        Log.d("YuChangXueService", "accessibilityEvent.getClassName():" + accessibilityEvent.getClassName());

        //每次进入页面都要去检查一下
//        if (isSearWebView) {
//        getBookWebView(accessibilityEvent.getSource());
//        }

        if (!canHome) {
            return;
        }
        if (!isShow) {
            show();//开启面板
            //回到桌面
            YuChangXueService.this.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        }
        if (isHome()) {
//            ToastUtils.showShort("当前处在首页");//只有在首页才能进行下面的操作
            //点击电视台
            canHome = false;
            updateShow("发现处于首页，请等待2秒自动操作！！！！！！！！！");
            updateShow("发现处于首页，请等待2秒自动操作！！！！！！！！！");
            updateShow("发现处于首页，请等待2秒自动操作！！！！！！！！！");
            ServiceUtils.performClickWithID(YuChangXueService.this, Dianshitai);
            mHandler.sendEmptyMessageDelayed(0, 2000);
        }

    }


    String contentDes = "";
    AccessibilityNodeInfo webNode = null;

    /**
     * 遍历出webview
     *
     * @param source
     */
    public void getBookWebView(AccessibilityNodeInfo source) {
        Log.d("YuChangXueService", "getBookWebView");

        if (source == null) {
            return;
        }
//        AccessibilityNodeInfo tiaozhanInfo = ServiceUtils.findNodeInfoByViewId(source, "cn.xuexi.android:id/webview_frame");
//
//        if (tiaozhanInfo != null) {
//            Log.d("YuChangXueService", "tiaozhanInfo:" + tiaozhanInfo);
//
//            for (int i = 0; i < tiaozhanInfo.getChildCount(); i++) {
//                Log.d("YuChangXueService", "tiaozhanInfo.getChild(i):" + tiaozhanInfo.getChild(i));
//                Log.d("YuChangXueService", "tiaozhanInfo.getChild(i).getContentDescription():" + tiaozhanInfo.getChild(i).getContentDescription());
//            }
//        }


        if (source.getChildCount() > 0) {
            for (int i = 0; i < source.getChildCount(); i++) {
                if (source.getChild(i) != null) {
                    if ("android.webkit.WebView".equals(source.getChild(i).getClassName())) {
                        if ("com.uc.webview.export.WebView".equals(source.getChild(i).getClassName())) {
                            if (source.getChild(i) != null && source.getChild(i).getContentDescription() != null) {
                                linkedList.add(source.getChild(i).getContentDescription());
                                updateShow(source.getChild(i).getContentDescription().toString());
                                Log.d("YuChangXueService", "webNode.getContentDescription():" + source.getChild(i).getContentDescription());
                            } else {
                                getBookWebView(source.getChild(i));
                            }
                        }
                    }
                } else {
                    getBookWebView(source.getChild(i));
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        ToastUtils.showShort("学习功能被迫中断");
        mService = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        ToastUtils.showShort("学习功能已关闭");
        mService = null;
    }


    /**
     * 辅助功能是否启动
     */
    public static boolean isStart() {
        return mService != null;
    }

    //点击收藏
    public void clickCollect() {
        AccessibilityNodeInfo accessibilityNodeInfo = ServiceUtils.findNodeInfoByText(this.getRootInActiveWindow(), "欢迎发表你的观点");
        if (accessibilityNodeInfo != null && accessibilityNodeInfo.getClassName().equals("android.widget.TextView")) {
//            Log.d("YuChangXueService", "找到了这个控件");
            updateShow("查询到EditText控件");
            AccessibilityNodeInfo parent = accessibilityNodeInfo.getParent();
            if (parent != null && parent.getChildCount() > 0 && parent.getChildCount() == 9) {
//                Log.d("YuChangXueService", "parent.getChildCount():" + parent.getChildCount());
                updateShow("查询到收藏ImageView控件");
                updateShow("Click 收藏的ImageView控件");
                parent.getChild(7).performAction(AccessibilityNodeInfo.ACTION_CLICK);//
//                for (int i = 0; i < parent.getChildCount(); i++) {
//                    AccessibilityNodeInfo child = parent.getChild(i);
//                    if (child == null) {
//                        continue;
//                    }
//                    if (child.isClickable() && child.isEnabled() && "android.widget.ImageView".equals(child.getClassName())) {
////                        Log.d("YuChangXueService", "i:" + i);
////                        Log.d("YuChangXueService", "child.getClassName():" + child.getClassName());
//                        switch (i) {
//                            case 7://8为分享。7为收藏.(文章学习界面)，2为返回，4为分享
//                                parent.getChild(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);//
//                                break;
//                        }
//                    }
//                }
            }
        } else {
            updateShow("未查询到EditText控件");
        }
    }


    //点击分享按钮
    public void clickShare() {
        AccessibilityNodeInfo accessibilityNodeInfo = ServiceUtils.findNodeInfoByText(this.getRootInActiveWindow(), "欢迎发表你的观点");
        if (accessibilityNodeInfo != null && accessibilityNodeInfo.getClassName().equals("android.widget.TextView")) {
            updateShow("找到了输入框");
            AccessibilityNodeInfo parent = accessibilityNodeInfo.getParent();
            updateShow("找到了输入框的父布局");
            if (parent != null && parent.getChildCount() > 0 && parent.getChildCount() == 9) {
                updateShow("检测到了弹出分享的按钮,点击弹出");
                parent.getChild(4).performAction(AccessibilityNodeInfo.ACTION_CLICK);//
                mHandler.sendEmptyMessageDelayed(9, 1000);
//                for (int i = 0; i < parent.getChildCount(); i++) {
//                    AccessibilityNodeInfo child = parent.getChild(i);
//                    if (child == null) {
//                        continue;
//                    }
//                    if (child.isClickable() && child.isEnabled() && "android.widget.ImageView".equals(child.getClassName())) {
//                        Log.d("YuChangXueService", "i:" + i);
//                        Log.d("YuChangXueService", "child.getClassName():" + child.getClassName());
//                        switch (i) {
//                            case 4://8为分享。7为收藏.(文章学习界面)，2为返回，4为分享
//                                parent.getChild(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);//
//                                mHandler.sendEmptyMessageDelayed(9, 1000);
//                                break;
//                        }
//                    }
//                }
            } else {
                updateShow("未找到弹出分享按钮的弹窗,本次分享终止");
            }
        } else {
            updateShow("未找到输入框，本来检索分享按钮失败，本次分享操作终止");
        }
    }

    //点击分享强国
    public void clickWXshare() {
        AccessibilityNodeInfo accessibilityNodeInfo = ServiceUtils.findNodeInfoByViewId(this.getRootInActiveWindow(), "cn.xuexi.android:id/img_gv_item");
        if (accessibilityNodeInfo != null) {
            updateShow("检测到分享按钮，不支持点击，其父布局支持");
            accessibilityNodeInfo.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            mHandler.sendEmptyMessageDelayed(10, 500);//500毫秒后，自动返回
        } else {
            updateShow("未检测到分享按钮，本次分享终止");
        }
//        Log.d("YuChangXueService", "accessibilityNodeInfo:" + accessibilityNodeInfo);
//        Log.d("YuChangXueService", "accessibilityNodeInfo.getChildCount():" + accessibilityNodeInfo.getChildCount());
    }


    //点击视听
    private void clickSound() {
        Log.d("YuChangXueService", "播放语音");
        AccessibilityNodeInfo accessibilityNodeInfo = ServiceUtils.findNodeInfoByText(this.getRootInActiveWindow(), "欢迎发表你的观点");
        if (accessibilityNodeInfo != null && accessibilityNodeInfo.getClassName().equals("android.widget.TextView")) {
            Log.d("YuChangXueService", "找到了这个控件");
            AccessibilityNodeInfo parent = accessibilityNodeInfo.getParent();
            if (parent != null && parent.getChildCount() > 0 && parent.getChildCount() == 9) {
//                parent.getChild(4).performAction(AccessibilityNodeInfo.ACTION_CLICK);//
//                mHandler.sendEmptyMessageDelayed(9, 1000);
                Rect rect = new Rect();
                parent.getChild(4).getBoundsInScreen(rect);
                ToastUtils.showShort("开启播放视听");
                dispatchGestureClick(rect.left - 10, rect.top + 10);//模拟点击最右边的更多
                Log.d("YuChangXueService", "rect.left:" + rect.left);
                Log.d("YuChangXueService", "rect.top:" + rect.top);
            }
        }
    }

    /**
     * 立即发送移动的手势
     * 注意7.0以上的手机才有此方法，请确保运行在7.0手机上
     *
     * @param path  移动路径
     * @param mills 持续总时间
     */
    @RequiresApi(24)
    public void dispatchGestureMove(Path path, long mills) {
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                (path, 0, mills)).build(), null, null);
    }

    /**
     * 点击指定位置
     * 注意7.0以上的手机才有此方法，请确保运行在7.0手机上
     */
    @RequiresApi(24)
    public void dispatchGestureClick(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                (path, 0, 100)).build(), null, null);
    }

    /**
     * 由于太多,最好回收这些AccessibilityNodeInfo
     */
    public static void recycleAccessibilityNodeInfo(List<AccessibilityNodeInfo> listInfo) {
        if (isEmptyArray(listInfo)) return;

        for (AccessibilityNodeInfo info : listInfo) {
            info.recycle();
        }
    }


    /**
     * 判断当前页面view在不在
     *
     * @param packageAndId
     * @return
     */
    public boolean isViewExist(String packageAndId) {
        AccessibilityNodeInfo nodeInfo = this.getRootInActiveWindow();
        if (nodeInfo == null) {
            return false;
        }
        AccessibilityNodeInfo targetNode = ServiceUtils.findNodeInfoByViewId(nodeInfo, packageAndId);
        nodeInfo.recycle();
        if (targetNode != null) {
            return true;
        }
        return false;
    }

    /**
     * 是否处于视频的评论以及文章的评论页面
     *
     * @return
     */
    public boolean isPingLun() {
        AccessibilityNodeInfo accessibilityNodeInfo = ServiceUtils.findNodeInfoByText(this.getRootInActiveWindow(), "欢迎发表你的观点");
        if (accessibilityNodeInfo != null && accessibilityNodeInfo.getClassName().equals("android.widget.TextView")) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否在首页
     *
     * @return
     */
    public boolean isHome() {
        return isViewExist("cn.xuexi.android:id/home_bottom_tab_button_ding") && isViewExist("cn.xuexi.android:id/home_bottom_tab_button_message");
    }

    public boolean isCanClickCollect() {
        return isViewExist("cn.xuexi.android:id/common_webview");
    }

    /**
     * 是否在应用内
     *
     * @param str
     * @return
     */
    public boolean isInApp(String str) {
        return str.equals("cn.xuexi.android");
    }

    XToast t;
    public static boolean isShow = false;

    public void show() {
        if (t != null) {
            t.cancel();
        }
        isShow = true;
        t = new XToast(Utils.getApp())
                .setView(R.layout.toast)
                // 设置成可拖拽的
                //.setDraggable()
                // 设置显示时长
                .setGravity(Gravity.CENTER)
                .setDuration(5000 * 1000)
                // 设置动画样式
                .setAnimStyle(android.R.style.Animation_Translucent)
                .setText(R.id.tv_toast, "========开启学习辅助========\n目标学习" + videoNumMax + "个视频，每个视频花费时长"
                        + videoTime / 1000 + "秒" + ";\n目的学习" + bookNumMax + "个文章，每个文章花费" + bookTime / 1000 + "秒")
//                .setOnClickListener(R.id.tv_toast, new OnClickListener<TextView>() {
//
//                    @Override
//                    public void onClick(XToast toast, TextView view) {
//                        // 点击这个 View 后消失
//                        toast.cancel();
////                        mHandler.removeCallbacksAndMessages(null);
//                        // 跳转到某个Activity
//                        // toast.startActivity(intent);
//                    }
//                })
                .show();

        updateShow("请打开学习强国，使学习强国处在首页");
    }

    public void updateShow(String mes) {
        TextView textView = (TextView) t.findViewById(R.id.tv_toast);
        ScrollView scrollView = (ScrollView) t.findViewById(R.id.scrollView);
        textView.setText(textView.getText().toString() + "\n" + mes);
        scrollView.smoothScrollBy(0, 500);
    }


    public static void setZERO() {
        videoNum = 0;
        bookNum = 0;
        collectNum = 0;
        isShow = false;
        YuChangXueService.canHome = true;
    }


}