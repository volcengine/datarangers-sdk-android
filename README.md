English | [简体中文](./README.zh-CN.md)

# DataRangers Android SDK
## Usage
> For more information：[Integration Document](https://docs.byteplus.com/data-intelligence/reference/android-sdk-integration)

### 1. Initialize the SDK
Initialize SDK in the Application.OnCreate (initialize as early as possible).
```java
// Demo Application
public class TheApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
     
        /* Initialization started */
        final InitConfig config = new InitConfig("your_appid", "your_channel"); // AppID and channel. Contact your customer success manager if you are not sure about the AppID.
     
        config.setUriConfig(UriConfig.createByDomain("your_report_url", null)); //data report url
        config.setAbEnable(true); // enables AB test    
        config.setPicker(new Picker(this, config)); // Enables event monitoring
        config.setAutoTrackEnabled (true); //Enables visual events
        config.setLogEnable(false); // true: have logs，false:no logs. `false` by default
        config.setH5CollectEnable (false);//Turn off embedded H5 events tracking

        // Enables encryptions and support to SDK 5.5.1 and above
        AppLog.setEncryptAndCompress(true);
      
        AppLog.init(this, config);
        /* Initialization ended */
     
    }
}
```
### 2. Enable encryption for data report
Use default encryption or custom encryption.
```groovy
// default encryption library
implementation 'com.bytedance.frameworks:encryptor:0.0.9-rc.2-private'
```
```java
// custom encryption
config.setEncryptor(
  new IEncryptor() {
    @Override
    public byte[] encrypt(byte[] bytes, int i) {
      
    }
  });
```
### 3. Report behavior tracking events through code
User behavior logs take the form of events (event) + parameters (params) and one event can contain multiple parameters. In general, ensure that your tracking is in line with your businesses' overarching Tracking Plan which your product operations specialists/data analysts should have designed.

Example: Reporting the play-video behavior of a user.
```java
// When the user clicks the Play button of a video
// Report event only
AppLog.onEventV3("play_video");


// Or:

// When the user clicks the Play button of a video
// Report event + params
JSONObject paramsObj = new JSONObject();
try {
    paramsObj.put("video_title", "Lady Gaga on Oscar"); //Event parameter：video title 
    paramsObj.put("duration", 20); //Event parameter：play duration 
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
