# 增长分析营销套件 Android SDK
## 接入
> 参考官网接入文档：https://www.volcengine.com/docs/6285/65980

初始化 SDK
```java
public class TheApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        /* 初始化SDK开始 */
        // 第一个参数APPID: 参考2.1节获取
        // 第二个参数CHANNEL: 填写渠道，请注意不能为空
        final InitConfig config = new InitConfig("{{APPID}}", "{{CHANNEL}}");
        // 设置私有化部署数据上送地址，参考2.2节获取，{{REPORT_URL}} 例如 https://yourdomain.com，注意域名后不要加“/”
        config.setUriConfig(UriConfig.createByDomain("{{REPORT_URL}}", null));
        config.setAutoTrackEnabled(true); // 全埋点开关，true开启，false关闭
        config.setLogEnable(false); // true:开启日志，参考4.3节设置logger，false:关闭日志
        AppLog.setEncryptAndCompress(true); // 加密开关，true开启，false关闭
        AppLog.init(this, config);
        /* 初始化SDK结束 */
    }
}
```
开启加密需要添加加密库或自定义加密
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
      // 自定义加密
    }
  });
```
