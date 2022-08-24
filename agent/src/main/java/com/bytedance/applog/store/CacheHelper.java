// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
package com.bytedance.applog.store;

import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;

import com.bytedance.applog.server.Api;
import com.bytedance.applog.util.SensitiveUtils;
import com.bytedance.applog.util.Utils;

/**
 * 先从几个目录把数据读取出来，如果数据存在，就倒序依次保存到被清除的目录里面。 如果所有备份读取失败，而且从设备硬件也获取失败(一般被用户清除)，这时就没法保存。
 * 否则，每次读取都需要保证几个目录里面的数据同步。
 *
 * <p>一般会传递进来2个值，第一个value,第二个candidate 判断有效顺序是value > cached > candidate
 *
 * <p>Created by qianhong on 2017/5/5.
 */
public abstract class CacheHelper {
    private CacheHelper mSuccessor;

    private CacheHelper getSuccessor() {
        return mSuccessor;
    }

    public void setSuccessor(CacheHelper successor) {
        this.mSuccessor = successor;
    }

    Handler mHandler;

    public void setHandler(Handler handler) {
        CacheHelper successor = getSuccessor();
        if (successor != null) {
            successor.setHandler(handler);
        }
        mHandler = handler;
    }

    /**
     * if value isn't default value or empty value,should return it and save it. if value is default
     * value or empty value,load the openUdid from storage. and return it. if storage is empty ,save
     * candidate
     */
    public String loadOpenUdid(String value, final String candidate) {
        final String cachedKey = Api.KEY_OPEN_UDID;
        ICacheAgent<String> agent =
                new ICacheAgent<String>() {
                    @Override
                    public String getCache() {
                        return getCachedString(cachedKey);
                    }

                    @Override
                    public boolean checkValid(String value) {
                        return Utils.isValidUDID(value);
                    }

                    @Override
                    public void restoreCache(String value) {
                        cacheString(cachedKey, value);
                    }

                    @Override
                    public String load(String value, String candidate, CacheHelper successor) {
                        if (successor == null) {
                            return value;
                        }
                        return successor.loadOpenUdid(value, candidate);
                    }

                    @Override
                    public boolean isValueEqual(String target, String original) {
                        return Utils.equals(target, original);
                    }
                };
        return load(value, candidate, agent);
    }

    public String loadClientUdid(String value, String candidate) {
        final String cachedKey = Api.KEY_C_UDID;
        ICacheAgent<String> agent =
                new ICacheAgent<String>() {
                    @Override
                    public String getCache() {
                        return getCachedString(cachedKey);
                    }

                    @Override
                    public boolean checkValid(String value) {
                        return Utils.isValidUDID(value);
                    }

                    @Override
                    public void restoreCache(String value) {
                        cacheString(cachedKey, value);
                    }

                    @Override
                    public String load(String value, String candidate, CacheHelper successor) {
                        if (successor == null) {
                            return value;
                        }
                        return successor.loadClientUdid(value, candidate);
                    }

                    @Override
                    public boolean isValueEqual(String target, String original) {
                        return Utils.equals(target, original);
                    }
                };
        return load(value, candidate, agent);
    }

    public String loadSerialNumber(String value, String candidate) {
        ICacheAgent<String> agent =
                new ICacheAgent<String>() {
                    @Override
                    public String getCache() {
                        return getCachedString(Api.KEY_SERIAL_NUMBER);
                    }

                    @Override
                    public boolean checkValid(String value) {
                        return !TextUtils.isEmpty(value) && !TextUtils.equals(value, Build.UNKNOWN);
                    }

                    @Override
                    public void restoreCache(String value) {
                        cacheString(Api.KEY_SERIAL_NUMBER, value);
                    }

                    @Override
                    public String load(String value, String candidate, CacheHelper successor) {
                        if (successor == null) {
                            return value;
                        }
                        return successor.loadSerialNumber(value, candidate);
                    }

                    @Override
                    public boolean isValueEqual(String target, String original) {
                        return Utils.equals(target, original);
                    }
                };
        return load(value, candidate, agent);
    }

    public String[] loadAccId(String[] value, String[] candidate) {
        final String cachedKey = Api.KEY_SIM_SERIAL_NUMBER;
        ICacheAgent<String[]> agent =
                new ICacheAgent<String[]>() {
                    @Override
                    public String[] getCache() {
                        return getCachedStringArray(cachedKey);
                    }

                    @Override
                    public boolean checkValid(String[] value) {
                        return value != null && value.length > 0;
                    }

                    @Override
                    public void restoreCache(String[] value) {
                        cacheStringArray(cachedKey, value);
                    }

                    @Override
                    public String[] load(
                            String[] value, String[] candidate, CacheHelper successor) {
                        if (successor == null) {
                            return value;
                        }
                        return successor.loadAccId(value, candidate);
                    }

                    @Override
                    public boolean isValueEqual(String[] target, String[] original) {
                        if (target == original) {
                            return true;
                        }
                        if (target == null || original == null) {
                            return false;
                        }
                        if (target.length != original.length) {
                            return false;
                        }
                        for (String s : target) {
                            boolean contains = false;
                            for (String o : original) {
                                contains = Utils.equals(o, s) || contains;
                            }
                            if (!contains) {
                                return false;
                            }
                        }
                        return true;
                    }
                };
        return load(value, candidate, agent);
    }

    public String loadUdid(String value, String candidate) {
        final String cachedKey = Api.KEY_UDID;
        ICacheAgent<String> agent =
                new ICacheAgent<String>() {
                    @Override
                    public String getCache() {
                        return getCachedString(cachedKey);
                    }

                    @Override
                    public boolean checkValid(String value) {
                        return Utils.isValidUDID(value);
                    }

                    @Override
                    public void restoreCache(String value) {
                        cacheString(cachedKey, value);
                    }

                    @Override
                    public String load(String value, String candidate, CacheHelper successor) {
                        if (successor == null) {
                            return value;
                        }
                        return successor.loadUdid(value, candidate);
                    }

                    @Override
                    public boolean isValueEqual(String target, String original) {
                        return Utils.equals(target, original);
                    }
                };
        return load(value, candidate, agent);
    }

    public String loadUdidList(String value, String candidate) {
        final String cachedKey = Api.KEY_UDID_LIST;
        ICacheAgent<String> agent =
                new ICacheAgent<String>() {
                    @Override
                    public String getCache() {
                        return getCachedString(cachedKey);
                    }

                    @Override
                    public boolean checkValid(String value) {
                        return SensitiveUtils.validMultiImei(value);
                    }

                    @Override
                    public void restoreCache(String value) {
                        cacheString(cachedKey, value);
                    }

                    @Override
                    public String load(String value, String candidate, CacheHelper successor) {
                        if (successor == null) {
                            return value;
                        }
                        return successor.loadUdidList(value, candidate);
                    }

                    @Override
                    public boolean isValueEqual(String target, String original) {
                        return Utils.equals(target, original);
                    }
                };
        return load(value, candidate, agent);
    }

    public String loadDeviceId(String value, String candidate) {
        final String cachedKey = Api.KEY_DEVICE_ID;
        ICacheAgent<String> agent =
                new ICacheAgent<String>() {
                    @Override
                    public String getCache() {
                        return getCachedString(cachedKey);
                    }

                    @Override
                    public boolean checkValid(String value) {
                        return !TextUtils.isEmpty(value);
                    }

                    @Override
                    public void restoreCache(String value) {
                        cacheString(cachedKey, value);
                    }

                    @Override
                    public String load(String value, String candidate, CacheHelper successor) {
                        if (successor == null) {
                            return value;
                        }
                        return successor.loadDeviceId(value, candidate);
                    }

                    @Override
                    public boolean isValueEqual(String target, String original) {
                        return Utils.equals(target, original);
                    }
                };
        return load(value, candidate, agent);
    }

    /** 如果传递来的value有效，保存之。否则以读取到的为准 */
    private <T> T load(T value, T candidate, ICacheAgent<T> agent) {
        if (agent == null) {
            throw new IllegalArgumentException("agent == null");
        }
        CacheHelper successor = getSuccessor();
        T cachedValue = agent.getCache();
        T backup = cachedValue;
        boolean originalValueIsValid = agent.checkValid(value);
        boolean storageValueIsValid = agent.checkValid(cachedValue);
        // if original value is invalid but storage value is valid , then save storage and return
        // it.
        if (!originalValueIsValid && storageValueIsValid) {
            value = cachedValue;
        }
        if (successor != null) {
            cachedValue = agent.load(value, candidate, successor);
            if (!agent.isValueEqual(cachedValue, backup)) {
                agent.restoreCache(cachedValue);
            }
            return cachedValue;
        }
        boolean change = false;
        // 到最后发现都无效，此时应该以candidate作为新的值了
        if (!originalValueIsValid && !storageValueIsValid) {
            value = candidate;
            change = true;
        }
        if ((change && agent.checkValid(value))
                || (originalValueIsValid && !agent.isValueEqual(value, backup))) {
            agent.restoreCache(value);
        }
        return value;
    }

    public void clear(String key) {
        CacheHelper successor = getSuccessor();
        if (successor != null) {
            successor.clear(key);
        }
    }

    protected abstract void cacheString(String key, String value);

    protected abstract String getCachedString(String key);

    protected abstract String[] getCachedStringArray(String key);

    protected abstract void cacheStringArray(String cachedKey, String[] value);

    interface ICacheAgent<L> {
        L getCache();

        boolean checkValid(L value);

        void restoreCache(L value);

        L load(L value, L candidate, CacheHelper successor);

        boolean isValueEqual(L target, L original);
    }
}
