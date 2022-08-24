// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.collector;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.Presentation;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;

import androidx.annotation.Nullable;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.store.BaseData;
import com.bytedance.applog.store.EventV3;
import com.bytedance.applog.store.Page;
import com.bytedance.applog.util.PageUtils;
import com.bytedance.applog.util.TLog;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 导航记录器，记录本进程内页面流转的信息，生成{@link Page}上报事件。 跨进程的页面流转，在{@link com.bytedance.applog.engine.Session}中处理
 *
 * @author shiyanlong
 * @date 2019/1/16
 */
public class Navigator implements ActivityLifecycleCallbacks {

    public static final int DELAY_MULTY_PROC = 500;
    private static final int DELAY_SAME_PROC = 300;

    /** 特殊的Fragment类 */
    public static final List<String> LIFECYCLE_REPORT_FRAGMENT_NAMES =
            Arrays.asList(
                    "android.arch.lifecycle.ReportFragment", "androidx.lifecycle.ReportFragment");

    private static int sActiveCount = 0;
    private static Page sCurActPage;
    private static Page sCurFragPage;
    private static long sLastActivityPauseTs;
    private static String sLastActivity;
    private static int sRootWindowHash = -1;
    private static Object sCurActivity;
    private static Object sCurFragment;
    private static long sLastFragPauseTs;
    private static String sLastFragName;
    private static final Map<Integer, List<Page>> sPresentationMap = new HashMap<>();

    /** 最后的Page对象 */
    private static Page lastPage;

    private static final HashSet<Integer> sNewActivity = new HashSet<>(8);

    /** 全局的Navigator对象，仅监听一次即可 */
    private static volatile Navigator globalNavigator = null;

    public Navigator() {}

    /**
     * 全局监听Activity生命周期
     *
     * @param application Application
     * @return Navigator
     */
    public static synchronized Navigator registerGlobalListener(Application application) {
        if (null == globalNavigator) {
            globalNavigator = new Navigator();
            application.registerActivityLifecycleCallbacks(globalNavigator);
        }
        return globalNavigator;
    }

    public static Activity getForegroundActivity() {
        return (Activity) sCurActivity;
    }

    public static int getRootWindowHash() {
        return sRootWindowHash;
    }

    public static Page getCurPage() {
        Page page = null;
        Page activity = sCurActPage;
        Page fragment = sCurFragPage;
        if (fragment != null) {
            page = fragment;
        } else if (activity != null) {
            page = activity;
        }
        return page;
    }

    @Nullable
    public static String getPageName() {
        Page page = getCurPage();
        return page != null ? page.name : "";
    }

    @Override
    public void onActivityResumed(final Activity activity) {
        TLog.d(
                new TLog.LogGetter() {
                    @Override
                    public String log() {
                        return "onActivityResumed " + PageUtils.getTitle(activity);
                    }
                });
        long curTs = System.currentTimeMillis();
        sCurActPage =
                resumePage(
                        activity.getClass(),
                        false,
                        activity.getClass().getName(),
                        "",
                        PageUtils.getTitle(activity),
                        PageUtils.getPath(activity),
                        curTs,
                        sLastActivity,
                        PageUtils.getTrackProperties(activity));
        // 上面的resumePage中已经把page交给Engine了，这里又修改了page的back字段。没问题，但是很不好看。
        sCurActPage.back = !sNewActivity.remove(activity.hashCode()) ? 1 : 0;
        if (!activity.isChild()) {
            sRootWindowHash = activity.getWindow().getDecorView().hashCode();
            sCurActivity = activity;
        }
    }

    /** 对齐内部,这个方法没有传入Activity,无法修改这sRootWindowHash,sCurActivity两个变量,这两个变量目前似乎没什么用 */
    public void onActivityResumed(Activity activity, int hashCode) {
        long curTs = System.currentTimeMillis();
        sCurActPage =
                resumePage(
                        activity.getClass(),
                        false,
                        activity.getClass().getName(),
                        "",
                        PageUtils.getTitle(activity),
                        PageUtils.getPath(activity),
                        curTs,
                        sLastActivity,
                        PageUtils.getTrackProperties(activity));
        // 上面的resumePage中已经把page交给Engine了，这里又修改了page的back字段。没问题，但是很不好看。
        sCurActPage.back = !sNewActivity.remove(hashCode) ? 1 : 0;
    }

    /** TODO: 这里会导致系统弹窗时走到该生命周期，可能出现前台Launch丢失page信息的问题 */
    @Override
    public void onActivityPaused(final Activity activity) {
        TLog.d(
                new TLog.LogGetter() {
                    @Override
                    public String log() {
                        return "onActivityPaused " + PageUtils.getTitle(activity);
                    }
                });
        if (sCurFragPage != null) {
            onFragPause(sCurFragment);
        }

        if (sCurActPage != null) {
            sLastActivity = sCurActPage.name;
            sLastActivityPauseTs = System.currentTimeMillis();
            pausePage(false, sCurActPage, sLastActivityPauseTs);
            sCurActPage = null;
            if (activity != null && !activity.isChild()) {
                sRootWindowHash = -1;
                sCurActivity = null;
            }
        }
    }

    public static Page resumePage(
            Class<?> clazz,
            boolean isFragment,
            String actName,
            String fragName,
            String title,
            String path,
            long curTs,
            String last,
            JSONObject properties) {
        Page page = new Page();
        page.clazz = clazz;
        if (!TextUtils.isEmpty(fragName)) {
            page.name = actName + ":" + fragName;
        } else {
            page.name = actName;
        }
        page.setTs(curTs);
        page.duration = -1L;
        page.last = last != null ? last : "";
        page.title = title != null ? title : "";
        page.referTitle = null != lastPage ? lastPage.title : "";
        page.path = path != null ? path : "";
        page.referPath = null != lastPage ? lastPage.path : "";
        page.properties = properties;
        autoReceivePage(page, isFragment);

        // 保存最后的页面
        lastPage = page;

        return page;
    }

    public static Page pausePage(boolean isFragment, Page curr, long ts) {
        Page page = (Page) curr.clone();
        page.setTs(ts);
        long duration = ts - curr.ts;
        if (duration <= 0) {
            duration = 1000L;
        }
        page.duration = duration;
        autoReceivePage(page, isFragment);

        // 离开页面事件
        receivePageLeave(page);
        return page;
    }

    /**
     * 采集页面离开事件
     *
     * @param page Page
     */
    private static void receivePageLeave(final Page page) {
        AppLogHelper.receiveIf(
                new AppLogHelper.BaseDataLoader() {
                    @Override
                    public BaseData load() {
                        Page p = (Page) page.clone();
                        JSONObject pJson = p.toPackJson();
                        // 复制page的param字段
                        JSONObject params = pJson.optJSONObject(BaseData.COL_PARAM);
                        if (null == params) {
                            params = new JSONObject();
                        }

                        // 添加duration字段
                        try {
                            params.put(Api.KEY_PAGE_DURATION, p.duration);
                        } catch (Throwable e) {
                            TLog.e(e);
                        }

                        // 转换成一个EventV3事件
                        EventV3 leaveEvent = new EventV3(Api.LEAVE_PAGE_EVENT_NAME);
                        leaveEvent.setTs(0);
                        leaveEvent.setProperties(params);
                        return leaveEvent;
                    }
                },
                new AppLogHelper.AppLogInstanceMatcher() {
                    @Override
                    public boolean match(AppLogInstance instance) {
                        return null != instance.getInitConfig()
                                && instance.getInitConfig().isTrackPageLeaveEnabled();
                    }
                });
    }

    /**
     * 采集页面事件
     *
     * @param page Page
     * @param isFragment 是否为Fragment
     */
    private static void autoReceivePage(final Page page, final boolean isFragment) {
        AppLogHelper.receiveIf(
                page,
                new AppLogHelper.AppLogInstanceMatcher() {
                    @Override
                    public boolean match(AppLogInstance instance) {
                        // 过滤未开启全埋点的实例
                        if (!AppLogHelper.isHandleLifecycleMatcher.match(instance)) {
                            return false;
                        }
                        // 过滤忽略的页面类型
                        if (instance.isAutoTrackPageIgnored(page.clazz)) {
                            return false;
                        }

                        // 处理fragment开关
                        if (isFragment) {
                            return null == instance.getInitConfig()
                                    || instance.getInitConfig().isAutoTrackFragmentEnabled();
                        }
                        return true;
                    }
                });
    }

    @Override
    public void onActivityStarted(final Activity activity) {
        ++sActiveCount;
    }

    @Override
    public void onActivityStopped(final Activity activity) {
        if (sLastActivity != null) { // 如果applog init前存在页面，会导致计数错误，加这个判断解决该错误
            --sActiveCount;
            if (sActiveCount <= 0) {
                sLastActivity = null;
                sLastFragName = null;
                sLastFragPauseTs = 0;
                sLastActivityPauseTs = 0;
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(final Activity activity, final Bundle outState) {
        // do nothing
    }

    @Override
    public void onActivityDestroyed(final Activity activity) {
        sNewActivity.remove(activity.hashCode());
    }

    @Override
    public void onActivityCreated(final Activity activity, final Bundle savedInstanceState) {
        sNewActivity.add(activity.hashCode());
    }

    public static void onPresentationStart(Presentation presentation) {
        int displayId = getDisplayId(presentation);
        List<Page> presentationList;
        if (sPresentationMap.containsKey(displayId)) {
            presentationList = sPresentationMap.get(displayId);
        } else {
            presentationList = new LinkedList<>();
            sPresentationMap.put(displayId, presentationList);
        }
        String pageName = presentation.getClass().getName();
        long ts = System.currentTimeMillis();
        final Activity activity = presentation.getOwnerActivity();
        Page page =
                resumePage(
                        null != activity ? activity.getClass() : presentation.getClass(),
                        false,
                        pageName,
                        "",
                        PageUtils.getTitle(activity),
                        PageUtils.getPath(activity),
                        ts,
                        "",
                        PageUtils.getTrackProperties(activity));
        assert presentationList != null;
        presentationList.add(page);
    }

    public static void onPresentationStop(Presentation presentation) {
        int displayId = getDisplayId(presentation);
        if (sPresentationMap.containsKey(displayId)) {
            LinkedList<Page> presentationList = (LinkedList<Page>) sPresentationMap.get(displayId);
            if (null != presentationList && !presentationList.isEmpty()) {
                Page page = presentationList.removeLast();
                long ts = System.currentTimeMillis();
                pausePage(false, page, ts);
            }
            if (null == presentationList || presentationList.isEmpty()) {
                sPresentationMap.remove(displayId);
            }
        }
    }

    public static String getPresentationPageName(int displayId) {
        String pageName = "";
        if (!sPresentationMap.containsKey(displayId)) {
            return pageName;
        }
        LinkedList<Page> presentationList = (LinkedList<Page>) sPresentationMap.get(displayId);
        if (null != presentationList && !presentationList.isEmpty()) {
            pageName = presentationList.getLast().name;
        }
        return pageName;
    }

    /**
     * Fragment OnResume回调
     *
     * @param frag Fragment对象
     */
    public static void onFragResume(Object frag) {
        if (null == frag) {
            return;
        }
        // 重复过滤
        if (sCurFragment == frag) {
            return;
        }

        // 过滤lifecycle:ReportFragment
        if (isLifeCycleReportFragment(frag)) {
            return;
        }

        long curTs = System.currentTimeMillis();
        String last = null;
        if (curTs - sLastFragPauseTs < DELAY_SAME_PROC) {
            last = sLastFragName;
        } else if (curTs - sLastActivityPauseTs < DELAY_SAME_PROC) {
            last = sLastActivity;
        }

        String fragName = frag.getClass().getName();
        sCurFragPage =
                resumePage(
                        frag.getClass(),
                        true,
                        PageUtils.getFragmentActivityName(frag),
                        fragName,
                        PageUtils.getTitle(frag),
                        PageUtils.getPath(frag),
                        curTs,
                        last,
                        PageUtils.getTrackProperties(frag));
        sCurFragment = frag;
    }

    /**
     * Fragment OnPause回调
     *
     * @param frag Fragment对象
     */
    public static void onFragPause(Object frag) {
        if (sCurFragPage != null && sCurFragment == frag) {
            sLastFragName = sCurFragPage.name;
            sLastFragPauseTs = System.currentTimeMillis();
            pausePage(true, sCurFragPage, sLastFragPauseTs);
            sCurFragPage = null;
            sCurFragment = null;
        }
    }

    private static int getDisplayId(Presentation presentation) {
        int displayId = 0;
        if (presentation == null) {
            return displayId;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Display display = presentation.getDisplay();
            if (display != null) {
                displayId = display.getDisplayId();
            }
        }
        return displayId;
    }

    /**
     * 判断是否为android声明周期的Fragment
     *
     * @param fragment Fragment对象
     * @return true: 是
     */
    private static boolean isLifeCycleReportFragment(Object fragment) {
        return null != fragment
                && LIFECYCLE_REPORT_FRAGMENT_NAMES.contains(fragment.getClass().getName());
    }
}
