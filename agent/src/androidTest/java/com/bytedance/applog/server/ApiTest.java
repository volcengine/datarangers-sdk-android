// Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.
// package com.bytedance.applog.server;
//
// import android.support.test.runner.AndroidJUnit4;
// import android.text.TextUtils;
//
// import com.bytedance.applog.AppLog;
// import com.bytedance.applog.BaseAppLogTest;
// import com.bytedance.applog.manager.ConfigManager;
// import com.bytedance.applog.network.RangersHttpException;
// import com.bytedance.applog.network.DefaultClient;
// import com.bytedance.applog.util.ReflectUtils;
//
// import org.junit.After;
// import org.junit.Assert;
// import org.junit.Before;
// import org.junit.Test;
// import org.junit.runner.RunWith;
//
// import java.util.Map;
//
/// **
// * @author shiyanlong
// * @date 2019/1/20
// */
// @RunWith(AndroidJUnit4.class)
// public class ApiTest extends BaseAppLogTest {
//
//    private ConfigManager configManager;
//
//    private MockClient mMockClient;
//
//    @Before
//    public void setUp() {
//        configManager = ReflectUtils.getFieldValue(AppLog.class, "sConfig");
//        mMockClient = new MockClient();
//        ReflectUtils.setFieldValue(configManager.getInitConfig(), "mWrapperClient", mMockClient);
//    }
//
//    @After
//    public void tearDown() {
//        ReflectUtils.setFieldValue(configManager.getInitConfig(), "mWrapperClient", null);
//    }
//
//    @Test
//    public void send() {
//        int code;
//        // 测试拥塞控制关闭
//        configManager.getInitConfig().setCongestionControlEnable(false);
//
//        // 重试所有，返回最后结果500
//        mMockClient.resetCount();
//        code =
//                Api.send(
//                        new String[] {
//                            MockClient.NULL,
//                            MockClient.NULL,
//                            MockClient.RANGERS_CONGEST_EX,
//                            MockClient.RANGERS_EX,
//                            MockClient.RANGERS_CONGEST_EX
//                        },
//                        null,
//                        configManager);
//        Assert.assertEquals(5, mMockClient.getCount());
//        Assert.assertEquals(500, code);
//
//        // 重试所有,返回最后一次请求的结果300
//        mMockClient.resetCount();
//        code =
//                Api.send(
//                        new String[] {MockClient.NULL, MockClient.NULL, MockClient.RANGERS_EX},
//                        null,
//                        configManager);
//        Assert.assertEquals(3, mMockClient.getCount());
//        Assert.assertEquals(300, code);
//
//        // 重试所有, 返回空
//        mMockClient.resetCount();
//        code = Api.send(new String[] {MockClient.NULL, MockClient.NULL}, null, configManager);
//        Assert.assertEquals(2, mMockClient.getCount());
//        Assert.assertEquals(102, code);
//
//        // 只会执行到成功返回
//        mMockClient.resetCount();
//        code =
//                Api.send(
//                        new String[] {
//                            MockClient.NULL,
//                            MockClient.N200_SUCCESS,
//                            MockClient.RANGERS_EX,
//                            MockClient.RANGERS_CONGEST_EX
//                        },
//                        null,
//                        configManager);
//        Assert.assertEquals(2, mMockClient.getCount());
//        Assert.assertEquals(200, code);
//
//        // 只会执行到成功返回
//        mMockClient.resetCount();
//        code =
//                Api.send(
//                        new String[] {
//                            MockClient.NULL,
//                            MockClient.N200_FAIL,
//                            MockClient.RANGERS_EX,
//                            MockClient.RANGERS_CONGEST_EX
//                        },
//                        null,
//                        configManager);
//        Assert.assertEquals(4, mMockClient.getCount());
//        Assert.assertEquals(500, code);
//
//        // 只会执行到成功返回
//        mMockClient.resetCount();
//        code =
//                Api.send(
//                        new String[] {
//                            MockClient.NULL,
//                            MockClient.N200_NO_DATA,
//                            MockClient.RANGERS_EX,
//                            MockClient.RANGERS_CONGEST_EX
//                        },
//                        null,
//                        configManager);
//        Assert.assertEquals(4, mMockClient.getCount());
//        Assert.assertEquals(500, code);
//    }
//
//    @Test
//    public void sendCongest() {
//        mMockClient.resetCount();
//        // 测试拥塞控制打开
//        configManager.getInitConfig().setCongestionControlEnable(true);
//        // 会在收到拥塞信号时取消重试
//        int code =
//                Api.send(
//                        new String[] {
//                            MockClient.NULL,
//                            MockClient.RANGERS_EX,
//                            MockClient.RANGERS_CONGEST_EX,
//                            MockClient.NULL,
//                            MockClient.NULL
//                        },
//                        null,
//                        configManager);
//        Assert.assertEquals(3, mMockClient.getCount());
//        Assert.assertEquals(500, code);
//    }
//
//    static class MockClient extends DefaultClient {
//
//        static final String NULL = "null";
//
//        // 200但没数据
//        static final String N200_NO_DATA = "200";
//
//        // 200且成功
//        static final String N200_SUCCESS = "200_success";
//
//        // 200但不成功
//        static final String N200_FAIL = "200_fail";
//
//        static final String RANGERS_EX = "rangers_ex";
//
//        static final String RANGERS_CONGEST_EX = "rangers_congest_ex";
//
//        public MockClient(Api api) {
//            super(api);
//        }
//
//        @Override
//        public String get(final String url, final Map<String, String> requestHeaders)
//                throws RangersHttpException {
//            String mockResp = mock(url);
//            if (TextUtils.equals(mockResp, "default")) {
//                return super.get(url, requestHeaders);
//            } else {
//                return mockResp;
//            }
//        }
//
//        @Override
//        public String post(
//                final String url, final byte[] data, final Map<String, String> requestHeaders)
//                throws RangersHttpException {
//            String mockResp = mock(url);
//            if (TextUtils.equals(mockResp, "default")) {
//                return super.post(url, data, requestHeaders);
//            } else {
//                return mockResp;
//            }
//        }
//
//        private String mock(String url) throws RangersHttpException {
//            count++;
//            switch (url) {
//                case N200_NO_DATA:
//                    // 200但没数据
//                    return "{}";
//                case N200_SUCCESS:
//                    // 200且成功
//                    return "{\"magic_tag\": \"ss_app_log\",\"message\":\"success\"}";
//                case N200_FAIL:
//                    // 200但不成功
//                    return "{\"magic_tag\": \"ss_app_log\",\"message\":\"fail\"}";
//                case RANGERS_EX:
//                    throw new RangersHttpException(300, "模拟服务器错误码");
//                case RANGERS_CONGEST_EX:
//                    throw new RangersHttpException(500, "模拟拥塞控制请求");
//                case NULL:
//                    return null;
//                default:
//                    count--;
//                    return "default";
//            }
//        }
//
//        // 记录试了多少个测试url
//        private int count = 0;
//
//        int getCount() {
//            return count;
//        }
//
//        void resetCount() {
//            count = 0;
//        }
//    }
// }
