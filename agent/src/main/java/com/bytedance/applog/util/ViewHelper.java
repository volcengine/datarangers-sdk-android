// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.app.Activity;
import android.graphics.Rect;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bytedance.applog.R;
import com.bytedance.applog.collector.Navigator;
import com.bytedance.applog.store.Click;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * @author shiyanlong
 */
public class ViewHelper {

    static final int TAG_USER_ID = 84159242;

    static final int TAG_CONTENT = 84159244;

    static final int TAG_WATCH_TEXT = 84159251;

    public static View getMenuItemView(MenuItem menuItem) {
        if (menuItem != null) {
            WindowUtils.initialize();
            View[] windows = WindowUtils.getWindowViews();

            try {
                for (View window : windows) {
                    if (window.getClass() == WindowUtils.sPopupWindowClass) {
                        final View menuView = findMenuItemView(window, menuItem);
                        if (menuView != null) {
                            return menuView;
                        }
                    }
                }
            } catch (InvocationTargetException e) {
                TLog.e(e);
            } catch (IllegalAccessException e) {
                TLog.e(e);
            }

        }
        return null;
    }

    /**
     * 根据view获取click
     *
     * @param view             view
     * @param filterIgnoreView 是否过滤带ignore tag的view, web圈选的时候要求获取完整的dom,不过滤带ignore tag的view
     * @return
     */
    public static Click getClickViewInfo(final View view, boolean filterIgnoreView) {
        Activity activity;
        if ((activity = ActivityUtil.findActivity(view.getContext())) != null
                || !ViewUtils.isMainDisplay(view.getContext(), ViewUtils.getDisplayId(view))) {
            if (filterIgnoreView && ViewUtils.isIgnoredView(view)) {
                return null;
            }
            WindowUtils.initialize();

            ArrayList<View> views = new ArrayList<>();
            views.add(view);

            ViewParent parent = view.getParent();
            while (parent instanceof ViewGroup) {
                views.add((ViewGroup) parent);
                parent = parent.getParent();
            }

            final int lastIndex = views.size() - 1;
            View rootView = views.get(lastIndex);

            StringBuilder path = new StringBuilder(WindowUtils.getWindowPrefix(rootView));

            boolean hasUserId = false;
            if (!WindowUtils.isDecorView(rootView) && !(rootView.getParent() instanceof View)) {
                path.append("/").append(ViewUtils.getSimpleClassName(rootView.getClass()));
                String id = ViewUtils.getIdName(rootView, false);
                if (id != null) {
                    if (rootView.getTag(TAG_USER_ID) != null) {
                        hasUserId = true;
                    }
                    path.append("#").append(id);
                }
            }
            int viewPos;
            ArrayList<String> positionList = null;
            if (rootView instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) rootView;

                for (int i = lastIndex - 1; i >= 0; i--) {
                    View childView = views.get(i);
                    Object viewName = childView.getTag(R.id.applog_tag_view_name);
                    if (viewName == null) {
                        viewName = ViewUtils.getSimpleClassName(childView.getClass());
                        viewPos = viewGroup.indexOfChild(childView);
                        if (viewGroup instanceof AdapterView) {
                            viewPos += ((AdapterView) viewGroup).getFirstVisiblePosition();
                        } else if (ClassHelper.isRecyclerView(viewGroup)) {
                            int adapterPosition =
                                    getChildAdapterPositionInRecyclerView(childView, viewGroup);
                            if (adapterPosition >= 0) {
                                viewPos = adapterPosition;
                            }
                        } else if (ClassHelper.isSupportViewPager(viewGroup)) {
                            viewPos = ((ViewPager) viewGroup).getCurrentItem();
                        } else if (ClassHelper.isAndroidXViewPager(viewGroup)) {
                            viewPos =
                                    ((androidx.viewpager.widget.ViewPager) viewGroup)
                                            .getCurrentItem();
                        }

                        if (ViewUtils.isListView(viewGroup)) {
                            if (positionList == null) {
                                positionList = new ArrayList<>();
                            }
                            positionList.add(String.valueOf(viewPos));
                            path.append("/").append(viewName).append("[-]");
                        } else if (!ClassHelper.isAndroidXSwipeRefreshLayout(viewGroup)
                                && !ClassHelper.isSupportSwipeRefreshLayout(viewGroup)) {
                            path.append("/").append(viewName).append("[").append(viewPos).append("]");
                        } else {
                            path.append("/").append(viewName).append("[0]");
                        }

                        String id = ViewUtils.getIdName(childView, hasUserId);
                        if (id != null) {
                            if (childView.getTag(TAG_USER_ID) != null) {
                                hasUserId = true;
                            }

                            path.append("#").append(id);
                        }
                    } else {
                        path.append("/").append(viewName);
                    }

                    if (!(childView instanceof ViewGroup)) {
                        break;
                    }

                    viewGroup = (ViewGroup) childView;
                }
            }

            String pageName;
            int displayId = ViewUtils.getDisplayId(view);
            if (ViewUtils.isMainDisplay(view.getContext(), displayId)) {
                pageName = Navigator.getPageName();
                if (TextUtils.isEmpty(pageName)) {
                    pageName = activity.getClass().getName();
                }
            } else {
                pageName = Navigator.getPresentationPageName(displayId);
            }
            int width = view.getWidth();
            int height = view.getHeight();
            return new Click(
                    pageName,
                    PageUtils.getTitle(activity),
                    path.toString(),
                    WidgetUtils.getId(view),
                    WidgetUtils.getType(view),
                    width,
                    height,
                    width / 2,
                    height / 2,
                    ViewUtils.getViewContent(view),
                    positionList);
        }
        return null;
    }

    @RequiresApi(api = 11)
    public static boolean isViewSelfVisible(View mView) {
        if (mView == null) {
            return false;
        } else if (WindowUtils.isDecorView(mView)) {
            return true;
        } else if ((mView.getWidth() <= 0 || mView.getHeight() <= 0 || mView.getAlpha() <= 0.0F)
                || !mView.getLocalVisibleRect(new Rect())) {
            return false;
        } else if (mView.getVisibility() != View.VISIBLE
                && mView.getAnimation() != null
                && mView.getAnimation().getFillAfter()) {
            return true;
        } else {
            return mView.getVisibility() == View.VISIBLE;
        }
    }

    public static boolean isViewVisibleInParents(View view) {
        boolean visible = isViewSelfVisible(view);
        if (visible) {
            ViewParent viewParent = view.getParent();
            while (true) {
                if (viewParent == null) {
                    visible = false;
                } else if (viewParent instanceof View) {
                    if (isViewSelfVisible((View) viewParent)) {
                        viewParent = viewParent.getParent();
                        continue;
                    } else {
                        visible = false;
                    }
                }
                break;
            }
        }
        return visible;
    }

    @RequiresApi(api = 11)
    private static int getChildAdapterPositionInRecyclerView(View childView, ViewGroup parentView) {
        if (ClassHelper.isAndroidXRecyclerView(parentView)) {
            return ((RecyclerView) parentView).getChildAdapterPosition(childView);
        } else if (ClassHelper.isSupportRecyclerView(parentView)) {
            try {
                return ((android.support.v7.widget.RecyclerView) parentView)
                        .getChildAdapterPosition(childView);
            } catch (Throwable var3) {
                return ((android.support.v7.widget.RecyclerView) parentView)
                        .getChildPosition(childView);
            }
        } else {
            return ClassHelper.sCustomRecyclerView
                    ? ClassHelper.invokeCustomGetChildAdapterPositionMethod(parentView, childView)
                    : -1;
        }
    }

    private static View findMenuItemView(View view, MenuItem item)
            throws InvocationTargetException, IllegalAccessException {
        if (WindowUtils.getMenuItemData(view) == item) {
            return view;
        } else {
            if (view instanceof ViewGroup) {
                for (int i = 0; i < ((ViewGroup) view).getChildCount(); ++i) {
                    View menuView = findMenuItemView(((ViewGroup) view).getChildAt(i), item);
                    if (menuView != null) {
                        return menuView;
                    }
                }
            }

            return null;
        }
    }
}
