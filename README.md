# UICrawler
## 基于Appium 1.8.1开发的App UI遍历 & Monkey 工具

![](https://github.com/lgxqf/UICrawler/blob/master/doc/picToMov.gif)


环境搭建及基本使用说明： https://testerhome.com/topics/14490  （感谢网友harsayer 倾力之作）


## 已解决的问题
一、遍历算法

   深度优先遍历算法

一、当前页面元素如何获取？
   
   通过设置xpath来采集带有content-desc属性且值有文本的元素，
   如果是imagebutton的话有这个熟悉且有文本就能被获取到，如果没有这个属性或者没有值就获取不到
   
二、用什么来判断已经调转到另外一个页面？
   
   设置两个变量lastPageSource和currentPageSource，如果执行点击后重新获取页面的pageSource，如果页面一样说明没跳转，如果不一样说明跳转了

三、什么样的页面不要需要遍历？
   
   理论上页面都是需要遍历的，但是我们知道某些页面没有必要遍历，可以配置某些页面不需要遍历，所有有个黑名单页面的配置

四、如果遍历开始了，会有循环遍历的情况，怎么处理？
  
   在配置文件中增加了一个灰名单，如果每次请求的页面不在灰名单则计入，如果存在则计数，通过配置文件给的计数次数，如果超过的话则返回

五、如果遍历中进入其他APP了怎么处理？
   
   遍历中如果进到其他APP了，则返回
   
六、滚动页面后如何处理？

   如果能滚动的话，最多滚动三次，每次滚动后继续遍历 ，当前页面的元素遍历完了 返回

##当前面临的问题

一、如果是页面是web页面的话，activity的名称都是一样的，所以对于是否是同一个activity的定位不能更细粒度处理

二、如果碰到浮层的话 我们应该怎么处理？没有统一的处理浮层问题

三、获取的元素中可能存在父子关系，到下一个页面会被执行多次



## 3.0 版 功能描述 

### 1.UI遍历及以下功能 Android
* 基于深度优先的原则，点击UI上的元素。当发现Crash时会提供操作步骤截图及相应的Log.
* Android提供logcat的log.     
* 元素遍历结束或按下Ctrl + C键会生成HTML测试报告。测试报告中汇集了测试统计、log及截图的相应信息  
* 同一个元素只会点击一次(白名单中的元素可能会被多次点击)
* 统计每个Activity点击数量(Android)
* 支持滑动动作
* 支持根据配置或者跳出本APP，自动Back key(Android)
* 黑名单支持不同粒度：页面级和关键字级


### 3.微信小程序
* 微信小程序 UI遍历  (Android only)


### 4.其它功能
* 运行时间限制
* 每次点击都会生一个一截图，截图中被点击的位置会用红点标注，方便查找点击位置
* 当检查到Crash时，为每个Crash提供单独的操作步骤截图和mp4格式的视频文件
* 生成整体操作步骤视频，方便重现发现的问题
* 性能数据采集，执行时添加-e参数
*        Android : 每秒采集一次CPU和Memory数据 生成perf_data.txt并写放到influxDB（需单添加-x参数，且influxDB要单独安装）

### 5.待开发功能
* 报告中增加每个activity中click失败和成功的次数统计
* 划动半屏，划动一屏




## 运行工具

### 1.下载Jar包
[UICrawler.jar](https://pan.baidu.com/s/1mNci6SWNHPuLj_mvrfgIbg)

### 2.下载配置文件
[config.yml](https://github.com/lgxqf/UICrawler/blob/master/config.yml) 

### 3.根据待测试App修改配置文件中下列各项的值 [详情见 Config.md](doc/Config.md)
  #### Android
  * ANDROID_PACKAGE
  * ANDROID_MAIN_ACTIVITY

  #### Monkey配置项可选， 详情见 [Monkey配置](https://github.com/lgxqf/UICrawler/blob/master/doc/Config.md#monkey%E5%8A%9F%E8%83%BD%E9%85%8D%E7%BD%AE)  

### 4.启动appium
```bash
appium --session-override -p 4723
-p 设定appium server的端口 , 不加参数默认为4723
```

### 5.1 运行元素遍历(必须有yml配置文件)
```aidl
java -jar UICrawler.jar -f config.yml -u udid -t 4723
-u 指定设备udid
-t 指定appium server的端口（此项为可选项，默认值是4723)
```

### 5.2 运行 Monkey功能
```aidl
java -jar UICrawler.jar -f config.yml -u udid -t 4723 -m
```

### 5.3 运行微信小程序测试，需修改 MINI_PROGRAM_NAME的值，并按照下面的值设置 CRITICAL_ELEMENT中相应的值，才会启动微信进入小程序
```
#小程序
MINI_PROGRAM:
  MINI_PROGRAM_NAME: 此处值为待测的小程序的名字
  MINI_PROGRAM_PROCESS: com.tencent.mm:appbrand1

CRITICAL_ELEMENT:
  #Android 微信
  ANDROID_PACKAGE: com.tencent.mm
  ANDROID_MAIN_ACTIVITY: com.tencent.mm.ui.LauncherUI

  #iOS 微信
  IOS_BUNDLE_ID: com.tencent.xin
  IOS_BUNDLE_NAME: 微信
  IOS_IPA_NAME: wechat
  
```


### 查看支持的参数
```aidl
java -jar UICrawler.jar -h

    -a  Android package's main activity
    -b  iOS bundle id
    -c  Maximum click count
    -d  Maximum crawler UI depth
    -e  Record performance data
    -f  Yaml config  file
    -h  Print this usage information
    -i  Ignore crash
    -l  Execution loop count
    -m  Run monkey
    -p  Android package name
    -r  Crawler running time
    -t  Appium port
    -u  Device serial
    -v  Version
    -w  WDA port for ios
    -x  Write data to influxDB

```

### 一些常用命令
```
查看设备udid
Android:
  adb devices
iOS:
  instruments -s  devices
  idevice_id -l
  
Android 查看apk 和 Main activity
  ./aapt dump badging "apk"  | grep launchable-activity
  aapt 通常在android sdk的 build-tools目录下
  windows中将grep换成findstr
  "apk"是apk文件路径
```

## [配置文件介绍](doc/Config.md)

### 配置文件主要可配置项
* 截图数量控制
* 黑名单、白名单
* 限制遍历深度、次数、时间
* 遍历界面元素的xpath
* 自动登录的用户名和密码及相应的UI元素ID 
* 待输入文本列表及待输入的控件类型


## 测试报告 
在当前工程的crawler_output下生成


## 注意事项
* Android7及以上的手机必须安装Uiautomator2 server 的两个apk(安装deskstop版appium,初次连接appium会自动安装), 也可进入到[apk](https://github.com/lgxqf/UICrawler/tree/master/apk)目录下通过adb安装


## 依赖的工具
* Grafana http://docs.grafana.org/installation/mac/
* InfluxDB https://portal.influxdata.com/downloads


## Known issue
* Android中bounds=[device width, device height]时xpath不能定位到元素.（appium bug)



## 参考内容
* Yaml 文件格式 https://blog.csdn.net/michaelhan3/article/details/69664932 
* Android API level 与version对应关系 http://www.cnblogs.com/jinglecode/p/7753107.html  
    CMD: adb -s uuid shell getprop | grep version.sdk
* iPhone分辨率与坐标系 https://www.paintcodeapp.com/news/ultimate-guide-to-iphone-resolutions
* https://github.com/baozhida/libimobiledevice
* 微信小程序自动化测试 https://testerhome.com/topics/12003?order_by=like
* 手势 https://www.jianshu.com/p/095e81f21e07
* XpathTester https://www.freeformatter.com/xpath-tester.html
* Appium并发测试 https://www.cnblogs.com/testway/p/6140594.html
* Android 性能采集 https://blog.csdn.net/bigconvience/article/details/35553983
