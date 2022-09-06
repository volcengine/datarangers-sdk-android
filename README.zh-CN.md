[English](./README.md) | 简体中文

# 增长分析营销套件 Android SDK
## 使用
> 更多信息请查看：[集成文档](https://www.volcengine.com/docs/6285/65980)

### 1. 初始化 SDK
在 `Application.OnCreate` 中初始化 SDK。
```java
// Demo Application
public class TheApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
     
        /* 初始化开始 */
        final InitConfig config = new InitConfig("your_appid", "your_channel"); // AppID 和 channel
     
        config.setUriConfig(UriConfig.createByDomain("your_report_url", null)); // 数据上报地址
        config.setAbEnable(true); // 开启 AB 测试
        config.setAutoTrackEnabled(true); // 开启全埋点
        config.setLogEnable(false); // 开启日志，默认 false

        // 开启加密
        AppLog.setEncryptAndCompress(true);
      
        AppLog.init(this, config);
        /* 初始化结束 */
        /* Initialization started */
     
    }
}
```
### 2. 开启数据上报啊机密
可以使用默认加密库或者自定义加密.
```groovy
// 默认加密库
implementation 'com.bytedance.frameworks:encryptor:0.0.9-rc.2-private'
```
```java
// 自定义加密
config.setEncryptor(
  new IEncryptor() {
    @Override
    public byte[] encrypt(byte[] bytes, int i) {
      
    }
  });
```
### 3. 上报事件
用户行为日志采用事件event+属性params的形式，事件一般对应多个属性，也可以仅有事件没有属性。代码埋点方案一般由数据分析师或产品运营设计。  
实例: 上报用户播放视频行为事件
```java
// 当用户点击视频播放按钮
// 仅记录事件
AppLog.onEventV3("play_video");


// 或者:

// 当用户点击视频播放按钮
// 记录事件和属性
JSONObject paramsObj = new JSONObject();
try {
    paramsObj.put("video_title", "视频标题"); // 事件属性：视频标题 
    paramsObj.put("duration", 20); // 事件属性：视频时长
} catch (JSONException e) { 
    e.printStackTrace();
}
AppLog.onEventV3("play_video", paramsObj);
```

## License

Copyright 2022 Beijing Volcano Engine Technology Ltd. All Rights Reserved.

The DataRangers SDK was developed by Beijing Volcanoengine Technology Ltd. (hereinafter “Volcanoengine”). Any copyright or patent right is owned by and proprietary material of the Volcanoengine.

DataRangers SDK is available under the Volcanoengine and licensed under the commercial license.  Customers can contact service@volcengine.com for commercial licensing options.  Here is also a link to subscription services agreement: https://www.volcengine.com/docs/6285/69647

Without Volcanoengine's prior written permission, any use of DataRangers SDK, in particular any use for commercial purposes, is prohibited. This includes, without limitation, incorporation in a commercial product, use in a commercial service, or production of other artefacts for commercial purposes.

Without Volcanoengine's prior written permission, the DataRangers SDK may not be reproduced, modified and/or made available in any form to any third party.
