// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.app.Activity;
import android.graphics.Rect;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ExpandableListView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bytedance.applog.R;
import com.bytedance.applog.collector.Navigator;
import com.bytedance.applog.log.LoggerImpl;
import com.bytedance.applog.store.Click;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author shiyanlong
 */
public class ViewHelper {

    static final int TAG_USER_ID = 84159242;

    private static final int TAG_BANNERS = 84159247;

    static final int TAG_CONTENT = 84159244;

    static final int TAG_WATCH_TEXT = 84159251;

    public static View getMenuItemView(MenuItem menuItem) {
        if (menuItem == null) {
            return null;
        } else {
            WindowHelper.init();
            View[] windows = WindowHelper.getWindowViews();

            try {
                for (View window : windows) {
                    if (window.getClass() == WindowHelper.sPopupWindowClazz) {
                        final View menuView = findMenuItemView(window, menuItem);
                        if (menuView != null) {
                            return menuView;
                        }
                    }
                }
            } catch (Throwable e) {
                LoggerImpl.global()
                        .error(
                                Collections.singletonList("ViewHelper"),
                                "getMenuItemView failed",
                                e);
            }

            return null;
        }
    }

    /**
     * 根据view获取click
     *
     * @param view view
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
            ArrayList<View> viewTreeList = new ArrayList<>(8);
            ViewParent parent = view.getParent();
            viewTreeList.add(view);

            while (parent instanceof ViewGroup) {
                viewTreeList.add((ViewGroup) parent);
                parent = parent.getParent();
            }

            int endIndex = viewTreeList.size() - 1;
            View rootView = viewTreeList.get(endIndex);
            WindowHelper.init();
            String bannerText = null;
            int viewPosition = 0;
            ArrayList<String> listPositions = null;
            boolean hasUserId = false;
            String opx = WindowHelper.getSubWindowPrefix(rootView);
            String px = opx;
            if (!WindowHelper.isDecorView(rootView) && !(rootView.getParent() instanceof View)) {
                px = opx = opx + "/" + ViewUtils.getSimpleClassName(rootView.getClass());
                String id = ViewUtils.getIdName(rootView, false);
                if (id != null) {
                    if (rootView.getTag(TAG_USER_ID) != null) {
                        hasUserId = true;
                    }

                    px = opx = opx + "#" + id;
                }
            }

            if (rootView instanceof ViewGroup) {
                ViewGroup parentView = (ViewGroup) rootView;

                for (int i = endIndex - 1; i >= 0; --i) {
                    View childView = viewTreeList.get(i);
                    Object viewName = childView.getTag(R.id.applog_tag_view_name);
                    if (viewName != null) {
                        opx = "/" + viewName;
                        px = px + "/" + viewName;
                    } else {
                        viewName = ViewUtils.getSimpleClassName(childView.getClass());
                        viewPosition = parentView.indexOfChild(childView);
                        if (ClassHelper.instanceOfAndroidXViewPager(parentView) || ClassHelper.instanceOfSupportViewPager(parentView)) {
                            try {
                                Method method = view.getClass().getMethod("getCurrentItem");
                                viewPosition = (int) method.invoke(view);
                            } catch (Throwable e) {
                                viewPosition = parentView.indexOfChild(childView);
                            }
                        } else if (parentView instanceof AdapterView) {
                            AdapterView listView = (AdapterView) parentView;
                            viewPosition += listView.getFirstVisiblePosition();
                        } else if (ClassHelper.instanceOfRecyclerView(parentView)) {
                            int adapterPosition =
                                    getChildAdapterPositionInRecyclerView(childView, parentView);
                            if (adapterPosition >= 0) {
                                viewPosition = adapterPosition;
                            }
                        }

                        if (parentView instanceof ExpandableListView) {
                            ExpandableListView listParent = (ExpandableListView) parentView;
                            long elp = listParent.getExpandableListPosition(viewPosition);
                            int footerIndex;
                            if (ExpandableListView.getPackedPositionType(elp)
                                    == ExpandableListView.PACKED_POSITION_TYPE_NULL) {
                                if (viewPosition < listParent.getHeaderViewsCount()) {
                                    opx = opx + "/ELH[" + viewPosition + "]/" + viewName + "[0]";
                                    px = px + "/ELH[" + viewPosition + "]/" + viewName + "[0]";
                                } else {
                                    footerIndex =
                                            viewPosition
                                                    - (listParent.getCount()
                                                            - listParent.getFooterViewsCount());
                                    opx = opx + "/ELF[" + footerIndex + "]/" + viewName + "[0]";
                                    px = px + "/ELF[" + footerIndex + "]/" + viewName + "[0]";
                                }
                            } else {
                                footerIndex = ExpandableListView.getPackedPositionGroup(elp);
                                int childIdx = ExpandableListView.getPackedPositionChild(elp);
                                if (childIdx != -1) {
                                    if (listPositions == null) {
                                        listPositions = new ArrayList<>(4);
                                    }
                                    listPositions.add(String.valueOf(footerIndex));
                                    listPositions.add(String.valueOf(childIdx));
                                    px = px + "/ELVG[-]/ELVC[-]/" + viewName + "[0]";
                                    opx =
                                            opx
                                                    + "/ELVG["
                                                    + footerIndex
                                                    + "]/ELVC["
                                                    + childIdx
                                                    + "]/"
                                                    + viewName
                                                    + "[0]";
                                } else {
                                    if (listPositions == null) {
                                        listPositions = new ArrayList<>(4);
                                    }
                                    listPositions.add(String.valueOf(footerIndex));
                                    px = px + "/ELVG[-]/" + viewName + "[0]";
                                    opx = opx + "/ELVG[" + footerIndex + "]/" + viewName + "[0]";
                                }
                            }
                        } else if (ViewUtils.isListView(parentView)) {
                            Object bannerTag = parentView.getTag(TAG_BANNERS);
                            if (bannerTag instanceof List && ((List) bannerTag).size() > 0) {
                                final List<String> banners = (List) bannerTag;
                                viewPosition =
                                        ViewUtils.calcBannerItemPosition(banners, viewPosition);
                                bannerText = ViewUtils.truncateContent(banners.get(viewPosition));
                            }

                            if (listPositions == null) {
                                listPositions = new ArrayList<>(4);
                            }
                            listPositions.add(String.valueOf(viewPosition));
                            px = px + "/" + viewName + "[-]";
                            opx = opx + "/" + viewName + "[" + viewPosition + "]";
                        } else if (!ClassHelper.instanceofAndroidXSwipeRefreshLayout(parentView)
                                && !ClassHelper.instanceOfSupportSwipeRefreshLayout(parentView)) {
                            opx = opx + "/" + viewName + "[" + viewPosition + "]";
                            px = px + "/" + viewName + "[" + viewPosition + "]";
                        } else {
                            opx = opx + "/" + viewName + "[0]";
                            px = px + "/" + viewName + "[0]";
                        }

                        String id = ViewUtils.getIdName(childView, hasUserId);
                        if (id != null) {
                            if (childView.getTag(TAG_USER_ID) != null) {
                                hasUserId = true;
                            }

                            opx = opx + "#" + id;
                            px = px + "#" + id;
                        }
                    }

                    if (!(childView instanceof ViewGroup)) {
                        break;
                    }

                    parentView = (ViewGroup) childView;
                }
            }

            String pageName;
            String pageTitle;
            int displayId = ViewUtils.getDisplayId(view);
            if (ViewUtils.isMainDisplay(view.getContext(), displayId)) {
                // 读取fragment信息
                Object fragment = Navigator.getFragmentByView(view);
                if (null != fragment) {
                    pageName = fragment.getClass().getName();
                    pageTitle = PageUtils.getTitle(fragment);
                } else {
                    pageName =
                            null != activity
                                    ? activity.getClass().getName()
                                    : Navigator.getPageName();
                    pageTitle = PageUtils.getTitle(activity);
                }
            } else {
                pageName = Navigator.getPresentationPageName(displayId);
                pageTitle = PageUtils.getTitle(activity);
            }
            int width = view.getWidth();
            int height = view.getHeight();
            return new Click(
                    pageName,
                    pageTitle,
                    px,
                    WidgetUtils.getId(view),
                    WidgetUtils.getType(view),
                    width,
                    height,
                    width / 2,
                    height / 2,
                    ViewUtils.getViewContent(view, bannerText),
                    listPositions);
        }
        return null;
    }

    @RequiresApi(api = 11)
    public static boolean isViewSelfVisible(View mView) {
        if (mView == null) {
            return false;
        } else if (WindowHelper.isDecorView(mView)) {
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
        if (ClassHelper.instanceOfAndroidXRecyclerView(parentView)) {
            return ((RecyclerView) parentView).getChildAdapterPosition(childView);
        } else if (ClassHelper.instanceOfSupportRecyclerView(parentView)) {
            try {
                Method method = childView.getClass().getMethod("getChildAdapterPosition");
                return (int) method.invoke(childView);
            } catch (Throwable var3) {
                try {
                    Method method = childView.getClass().getMethod("getChildPosition");
                    return (int) method.invoke(childView);
                } catch (Throwable e) {
                    return -1;
                }
            }
        } else {
            return ClassHelper.sHasCustomRecyclerView
                    ? ClassHelper.invokeCRVGetChildAdapterPositionMethod(parentView, childView)
                    : -1;
        }
    }

    private static View findMenuItemView(View view, MenuItem item) {
        if (WindowHelper.getMenuItemData(view) == item) {
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
