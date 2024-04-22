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
import android.view.View;

import androidx.annotation.Nullable;

import com.bytedance.applog.AppLogHelper;
import com.bytedance.applog.AppLogInstance;
import com.bytedance.applog.event.AutoTrackEventType;
import com.bytedance.applog.event.DurationEvent;
import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.server.Api;
import com.bytedance.applog.store.BaseData;
import com.bytedance.applog.store.EventV3;
import com.bytedance.applog.store.Page;
import com.bytedance.applog.util.PageUtils;
import com.bytedance.applog.util.WidgetUtils;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 导航记录器，记录本进程内页面流转的信息，生成{@link Page}上报事件。 跨进程的页面流转，在{@link com.bytedance.applog.engine.Session}中处理
 *
 * @author shiyanlong
 * @date 2019/1/16
 */
public class Navigator implements ActivityLifecycleCallbacks {

    public static final int DELAY_MULTY_PROC = 500;

    /**
     * 应用使用时长
     */
    private static final DurationEvent appUseDuration = new DurationEvent(null, "@APPLOG_APP_USE");

    /**
     * 是否处于前台
     */
    private static boolean isInFrontend = false;

    /**
     * 特殊的Fragment类
     */
    public static final List<String> LIFECYCLE_REPORT_FRAGMENT_NAMES =
            Arrays.asList(
                    "android.arch.lifecycle.ReportFragment", "androidx.lifecycle.ReportFragment");

    public static final List<String> BLACK_FRAGMENT_NAMES =
            Collections.singletonList("com.bumptech.glide.manager.SupportRequestManagerFragment");

    private static int sActiveCount = 0;
    private static Page sCurActPage;
    private static Page sCurFragPage;
    private static long sLastActivityPauseTs;
    private static String sLastActivity;
    private static Object sCurActivity;
    private static final Map<Integer, List<Page>> sPresentationMap = new HashMap<>();
    private static final Map<Integer, PageInfo> sVisibleFragmentCache = new ConcurrentHashMap<>();

    /**
     * 最后的Page对象
     */
    private static Page lastPage;

    private static final HashSet<Integer> sNewActivity = new HashSet<>(8);

    /**
     * 全局的Navigator对象，仅监听一次即可
     */
    private static volatile Navigator globalNavigator = null;

    public Navigator() {
    }

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
        long curTs = System.currentTimeMillis();
        appUseDuration.start(curTs);
        isInFrontend = true;

        String title = PageUtils.getTitle(activity);
        LoggerImpl.global().debug("[Navigator] onActivityResumed:{} {}", title, activity.getClass().getName());

        sCurActPage =
                resumePage(
                        activity.getClass(),
                        false,
                        activity.getClass().getName(),
                        "",
                        title,
                        PageUtils.getPath(activity),
                        curTs,
                        PageUtils.getTrackProperties(activity));
        // 上面的resumePage中已经把page交给Engine了，这里又修改了page的back字段。没问题，但是很不好看。
        sCurActPage.back = !sNewActivity.remove(activity.hashCode()) ? 1 : 0;
        if (!activity.isChild()) {
            sCurActivity = activity;
        }
    }

    /**
     * 对齐内部,这个方法没有传入Activity,无法修改这sRootWindowHash,sCurActivity两个变量,这两个变量目前似乎没什么用
     */
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
                        PageUtils.getTrackProperties(activity));
        // 上面的resumePage中已经把page交给Engine了，这里又修改了page的back字段。没问题，但是很不好看。
        sCurActPage.back = !sNewActivity.remove(hashCode) ? 1 : 0;
    }

    /**
     * TODO: 这里会导致系统弹窗时走到该生命周期，可能出现前台Launch丢失page信息的问题
     */
    @Override
    public void onActivityPaused(final Activity activity) {
        long curTs = System.currentTimeMillis();
        appUseDuration.pause(curTs);
        isInFrontend = false;

        LoggerImpl.global()
                .debug(
                        "[Navigator] onActivityPaused:{}",
                        activity != null ? activity.getClass().getName() : "");

        notifyVisibleFragmentPause();

        if (sCurActPage != null) {
            sLastActivity = sCurActPage.name;
            sLastActivityPauseTs = curTs;
            pausePage(false, sCurActPage, sLastActivityPauseTs);
            sCurActPage = null;
            if (activity != null && !activity.isChild()) {
                sCurActivity = null;
            }
        }
    }

    private void notifyVisibleFragmentPause() {
        for (PageInfo value : sVisibleFragmentCache.values()) {
            if (value != null ) {
                onFragPause(value.fragment.get());
            }
        }
        sVisibleFragmentCache.clear();
    }

    public static Page resumePage(
            Class<?> clazz,
            boolean isFragment,
            String actName,
            String fragName,
            String title,
            String path,
            long curTs,
            JSONObject properties) {
        Page page = new Page();
        page.clazz = clazz;
        if (!TextUtils.isEmpty(fragName)) {
            page.name = actName + ":" + fragName;
        } else {
            page.name = actName;
        }
        page.setTs(curTs);
        page.resumeAt = curTs;
        page.duration = -1L;
        page.last = null != lastPage ? lastPage.name : "";
        page.title = title != null ? title : "";
        page.referTitle = null != lastPage ? lastPage.title : "";
        page.path = path != null ? path : "";
        page.referPath = null != lastPage ? lastPage.path : "";
        page.properties = properties;
        page.isFragment = isFragment;
        autoReceivePage(page);
        // 保存最后的页面
        lastPage = page;
        LoggerImpl.global().debug("[Navigator] resumePage page.name：{}", page.name);

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
        page.isFragment = isFragment;
        autoReceivePage(page);
        LoggerImpl.global().debug("[Navigator] pausePage page.name：{}, duration：{}", page.name, page.duration);
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
                            LoggerImpl.global().error("[Navigator] JSON handle failed", e);
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
                        return instance.isBavEnabled()
                                && null != instance.getInitConfig()
                                && AutoTrackEventType.hasEventType(
                                instance.getInitConfig().getAutoTrackEventType(),
                                AutoTrackEventType.PAGE_LEAVE);
                    }
                });
    }

    /**
     * 采集页面事件
     *
     * @param page Page
     */
    private static void autoReceivePage(final Page page) {
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
                        if (page.isFragment) {
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
                onAppEnterBackground();
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

    /**
     * 当 APP 进入后台
     */
    private void onAppEnterBackground() {
        sLastActivity = null;
        sLastActivityPauseTs = 0;

        // 进入后台立即触发一次上报
        AppLogHelper.flushIfBackgroundSendEnabled();
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
     * Fragment显示
     *
     * @param frag      Fragment
     * @param isVisible 是否显示了
     */
    public static void onFragResume(Object frag, boolean isVisible) {
        if (null == frag || !isVisible) {
            return;
        }

        // 重复过滤
        if (inFragmentCache(frag)) {
            LoggerImpl.global().debug("[Navigator] onFragResume return {} inFragmentCache", frag);
            return;
        }

        // 过滤lifecycle:ReportFragment
        if (isLifeCycleReportFragment(frag)) {
            return;
        }

        // 过滤黑名单fragment
        if (isBlackFragment(frag)) {
            LoggerImpl.global().debug("[Navigator] onFragResume return {} isBlackFragment", frag, true);
            return;
        }
        LoggerImpl.global().debug("[Navigator] onFragResume:frag：{} isVisible：{}", frag, true);
        long curTs = System.currentTimeMillis();
        String fragName = frag.getClass().getName();
        Page page =
                resumePage(
                        frag.getClass(),
                        true,
                        PageUtils.getFragmentActivityName(frag, sCurActivity),
                        fragName,
                        PageUtils.getTitle(frag),
                        PageUtils.getPath(frag),
                        curTs,
                        PageUtils.getTrackProperties(frag));
        sCurFragPage = page;
        // 缓存
        sVisibleFragmentCache.put(frag.hashCode(), new PageInfo(page, frag));
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
        onFragResume(frag, isVisibleFragment(frag));
    }

    /**
     * Fragment OnPause回调
     *
     * @param frag Fragment对象
     */
    public static void onFragPause(Object frag) {
        LoggerImpl.global().debug("[Navigator] onFragPause:frag：{}", frag);
        if (inFragmentCache(frag)) {
            // 移除
            Page page = sVisibleFragmentCache.get(frag.hashCode()).fragmentPage;
            sVisibleFragmentCache.remove(frag.hashCode());
            LoggerImpl.global().debug("[Navigator] onFragPause:page：{}", page);
            if (page != null) {
                pausePage(true, page, System.currentTimeMillis());
            }
            sCurFragPage = null;
        } else {
            LoggerImpl.global().debug("[Navigator] onFragPause not in cache：{}", frag);
        }
    }

    /**
     * 获取应用使用时长
     *
     * @return mills
     */
    public static long getAppUseTimeToNow() {
        return appUseDuration.end(System.currentTimeMillis());
    }

    /**
     * 判断应用是否处于前台
     *
     * @return true: 前台
     */
    public static boolean isAppInFrontend() {
        return isInFrontend;
    }

    /**
     * 获取View所在的Fragment，无则返回null
     *
     * @param view View
     * @return Fragment | null
     */
    public static Object getFragmentByView(View view) {
        if (sVisibleFragmentCache.isEmpty()) {
            return null;
        }
        for (PageInfo value : sVisibleFragmentCache.values()) {
            if (value != null && value.fragment.get() != null) {
                Object fragment = value.fragment.get();
                View root = PageUtils.getFragmentRootView(fragment);
                if (WidgetUtils.isParentView(root, view)) {
                    return fragment;
                }
            }
        }
        return null;
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

    /**
     * 判断是否为黑名单Fragment
     *
     * @param fragment Fragment对象
     * @return true: 是
     */
    private static boolean isBlackFragment(Object fragment) {
        return null != fragment && BLACK_FRAGMENT_NAMES.contains(fragment.getClass().getName());
    }

    /**
     * 判断Fragment是否可见
     *
     * @param fragment Fragment对象
     * @return true: 可见
     */
    private static boolean isVisibleFragment(Object fragment) {
        Object parent = PageUtils.getParentFragment(fragment);
        if (null != parent && !isVisibleFragment(parent)) {
            return false;
        }
        return PageUtils.isFragmentResumed(fragment)
                && !PageUtils.isFragmentHidden(fragment)
                && PageUtils.isFragmentUserVisibleHint(fragment);
    }

    public static Page getLastPage() {
        return lastPage;
    }

    private static boolean inFragmentCache(Object frag) {
        if (frag == null) return false;
        if (sVisibleFragmentCache.isEmpty()) return false;
        if (!sVisibleFragmentCache.containsKey(frag.hashCode())) return false;
        PageInfo cacheFrag = sVisibleFragmentCache.get(frag.hashCode());
        if (cacheFrag.fragment.get() == null) {
            // null remove
            sVisibleFragmentCache.remove(frag.hashCode());
            LoggerImpl.global().debug("[Navigator] inFragmentCache frag already recycle：{}", frag);
        }
        return cacheFrag.fragment.get() == frag;
    }

    static class PageInfo {
        Page fragmentPage;
        WeakReference<Object> fragment;

        public PageInfo(Page fragmentPage, Object fragment) {
            this.fragmentPage = fragmentPage;
            this.fragment = new WeakReference<>(fragment);
        }
    }
}
