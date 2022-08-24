// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.LruCache;
import android.util.SparseArray;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsSeekBar;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.bytedance.applog.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author shiyanlong
 */
@TargetApi(12)
public class ViewUtils {

    private static SparseArray<String> mIdMap;

    private static Set<Integer> mBlackListId;

    private static LruCache<Class, String> sClassNameCache = new LruCache<>(100);

    static String getSimpleClassName(Class clazz) {
        String name = sClassNameCache.get(clazz);
        if (TextUtils.isEmpty(name)) {
            name = clazz.getSimpleName();
            if (TextUtils.isEmpty(name)) {
                name = "Anonymous";
            }

            sClassNameCache.put(clazz, name);
            ClassHelper.checkCustomRecyclerView(clazz, name);
        }
        return name;
    }

    static ArrayList<String> getViewContent(View view) {
        ArrayList<String> result = null;
        String value = null;
        Object contentTag = view.getTag(ViewHelper.TAG_CONTENT);
        if (contentTag != null) {
            value = String.valueOf(contentTag);
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            final int count = vg.getChildCount();
            result = new ArrayList<>(count);
            for (int i = 0; i < count && vg.getChildAt(i).getVisibility() == View.VISIBLE; ++i) {
                result.addAll(getViewContent(vg.getChildAt(i)));
            }
        } else {
            if (view instanceof EditText) {
                if (view.getTag(ViewHelper.TAG_WATCH_TEXT) != null
                        && !isPasswordInputType(((EditText) view).getInputType())) {
                    CharSequence sequence = getEditTextText((EditText) view);
                    value = sequence == null ? "" : sequence.toString();
                }
            } else if (view instanceof RatingBar) {
                value = String.valueOf(((RatingBar) view).getRating());
            } else {
                View selected;
                if (view instanceof Spinner) {
                    Object item = ((Spinner) view).getSelectedItem();
                    if (item instanceof String) {
                        value = (String) item;
                    } else {
                        selected = ((Spinner) view).getSelectedView();
                        if (selected instanceof TextView
                                && ((TextView) selected).getText() != null) {
                            value = ((TextView) selected).getText().toString();
                        }
                    }
                } else if (view instanceof SeekBar) {
                    value = String.valueOf(((SeekBar) view).getProgress());
                } else if (view instanceof RadioGroup) {
                    RadioGroup group = (RadioGroup) view;
                    selected = group.findViewById(group.getCheckedRadioButtonId());
                    if (selected != null
                            && selected instanceof RadioButton
                            && ((RadioButton) selected).getText() != null) {
                        value = ((RadioButton) selected).getText().toString();
                    }
                } else if (view instanceof TextView) {
                    if (((TextView) view).getText() != null) {
                        value = ((TextView) view).getText().toString();
                    }
                } else {
                    String url;
                    if (view instanceof WebView && !isDestroyed((WebView) view)) {
                        url = ((WebView) view).getUrl();
                        if (url != null) {
                            value = url;
                        }
                    } else if (ClassHelper.isX5WebView(view)) {
                        try {
                            Method getUrl = view.getClass().getMethod("getUrl");
                            url = (String) getUrl.invoke(view);
                            if (url != null) {
                                value = url;
                            }
                        } catch (Throwable e) {
                            TLog.ysnp(e);

                        }
                    }
                }
            }
        }
        if (result == null) {
            if (TextUtils.isEmpty(value)) {
                if (view.getContentDescription() != null) {
                    value = view.getContentDescription().toString();
                }
                value = truncateContent(value);
            }
            result = new ArrayList<>(1);
            if (!TextUtils.isEmpty(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private static final int LIMIT_CONTENT = 20;

    static String truncateContent(String value) {
        if (value == null) {
            return "";
        } else {
            if (!TextUtils.isEmpty(value) && value.length() > LIMIT_CONTENT) {
                value = value.substring(0, LIMIT_CONTENT);
            }

            return encryptContent(value);
        }
    }

    static boolean isListView(View view) {
        return view instanceof AdapterView
                || ClassHelper.isAndroidXRecyclerView(view)
                || ClassHelper.isAndroidXViewPager(view)
                || ClassHelper.isSupportRecyclerView(view)
                || ClassHelper.isSupportViewPager(view);
    }

    static String getIdName(View view, boolean fromTagOnly) {
        Object idTag = view.getTag(ViewHelper.TAG_USER_ID);
        if (idTag != null && idTag instanceof String) {
            return (String) idTag;
        } else if (fromTagOnly) {
            return null;
        } else {
            if (mIdMap == null) {
                mIdMap = new SparseArray();
            }

            if (mBlackListId == null) {
                mBlackListId = new HashSet();
            }

            int id = view.getId();
            if (id > 2130706432 && !mBlackListId.contains(id)) {
                String idName = (String) mIdMap.get(id);
                if (idName != null) {
                    return idName;
                }

                try {
                    idName = view.getResources().getResourceEntryName(id);
                    mIdMap.put(id, idName);
                    return idName;
                } catch (Exception var6) {
                    mBlackListId.add(id);
                }
            }

            return null;
        }
    }

    public static boolean isIgnoredView(View view) {
        return view == null || view.getTag(R.id.applog_tag_ignore) != null;
    }

    public static boolean isViewClickable(View view) {
        boolean result =
                view.isClickable()
                        //                || view instanceof RadioGroup
                        //                || view instanceof Spinner
                        || view instanceof AbsSeekBar;
        if (view.getParent() instanceof AdapterView) {
            AdapterView av = (AdapterView) view.getParent();
            result |=
                    av.isClickable()
                            || av.getOnItemClickListener() != null
                            || av.getOnItemSelectedListener() != null;
        }
        return result;
    }

    @TargetApi(11)
    private static boolean isPasswordInputType(int inputType) {
        int variation = inputType & 4095;
        return variation == 129 || variation == 225 || variation == 18 || variation == 145;
    }

    private static CharSequence getEditTextText(TextView textView) {
        try {
            Field mText = TextView.class.getDeclaredField("mText");
            mText.setAccessible(true);
            return (CharSequence) mText.get(textView);
        } catch (Throwable var2) {
            TLog.ysnp(var2);
            return null;
        }
    }

    private static String encryptContent(String content) {
        // TODO: 2019/2/12
        return content;
    }

    public static int getDisplayId(View view) {
        int displayId = 0;
        if (view == null) {
            return displayId;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Display display = view.getDisplay();
            if (display != null) {
                displayId = display.getDisplayId();
            }
        }
        return displayId;
    }

    public static boolean isMainDisplay(Context context, int displayId) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                DisplayManager displayManager =
                        (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
                Display display = displayManager.getDisplays()[0];
                return display.getDisplayId() == displayId;
            } else {
                return true;
            }
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean isDestroyed(WebView webView) {
        try {
            Field providerField = WebView.class.getDeclaredField("mProvider");
            providerField.setAccessible(true);
            Object provider = providerField.get(webView);
            if ("android.webkit.WebViewClassic".equals(provider)) {
                return isDestroyedWebViewClassic(provider);
            }

            Field awContentField = provider.getClass().getDeclaredField("mAwContents");
            awContentField.setAccessible(true);
            Object awContent = awContentField.get(provider);
            Method isDestroyed =
                    awContent.getClass().getDeclaredMethod("isDestroyed", Integer.TYPE);
            isDestroyed.setAccessible(true);
            Object isDestroy = isDestroyed.invoke(awContent, 0);
            if (isDestroy instanceof Boolean) {
                return (Boolean) isDestroy;
            }
        } catch (Exception var7) {
            TLog.e("isDestroyed(): ", var7);
        }

        return false;
    }

    private static boolean isDestroyedWebViewClassic(Object webViewClassic) throws Exception {
        Field field = webViewClassic.getClass().getDeclaredField("mWebViewCore");
        field.setAccessible(true);
        return field.get(webViewClassic) == null;
    }
}
