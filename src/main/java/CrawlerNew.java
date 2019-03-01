import com.google.gson.JsonObject;
import io.appium.java_client.AppiumDriver;
import org.apache.commons.cli.*;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.exec.OS;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import util.*;

import java.io.File;
import java.util.*;


public class CrawlerNew {


    public static Logger log = LoggerFactory.getLogger(Crawler.class);
    private static Date beginTime = new Date();
    private static String logName;
    private static Map<String, String> summaryMap;//= new ListOrderedMap();
    private static boolean isMonkey = false;
    private static List<String> crashFileList;
    private static boolean isReported = false;
    private static String udid;
    private static String outputDir;
    private boolean writeToDB;


    ////////////////////////////////////
    private JsonObject gray_activity_record = new JsonObject();//记录遍历过程中灰activity的次数
    private JsonObject gray_activity_traverse_iteself_time = new JsonObject();//记录灰activity在自身activity遍历的次数


    private List<String> caputure_onlyone_white_page = new ArrayList<>();//白名单acitivity中只抓取一次的记录
    private List<String> need_scrolled_xpath = new ArrayList<>();//避免在需要滚动的页面点击一个button后，多次滚动查找,判断只滚动一次
    private JsonObject caputure_onlyone_record = new JsonObject();//白名单acitivity中只抓取一次的记录
    private JsonObject need_scrolled_xpath_record = new JsonObject();//避免在需要滚动的页面点击一个button后，多次滚动查找,判断只滚动一次



    private String configFile;
    private XPathUtil xpathUtil;
    private List<String> root_list;

    public CrawlerNew(){

    }

    private static class CtrlCHandler extends Thread {
        @Override
        public void run() {
            log.info("Pressing Ctrl + C...\n");
            PerfUtil.closeDataFile();

            if (!isMonkeyMode()) {
                XPathUtil.showFailure();
            }

            if (!isReported) {
                log.info("Handling Ctrl + C shut down event...");
                executeTask();
                log.info("Everything is done. Both video and report are generated.");
            }
        }
    }

    private static void executeTask() {
        generateReport();
        if (ConfigUtil.isGenerateVideo()) {
            generateVideo();
        }
        isReported = true;
    }

    private static boolean isMonkeyMode() {
        return isMonkey;
    }


    /**
     * 读取命令行
     * @param args
     * @return
     */
    public CommandLine readCommandLine(String[] args){
        try{
            String version = "2.24 ---DEC/12/2018";
            log.info("Version is " + version);
            args = new String[]{"-f", "config.yml", "-u", "6f65c4f5"};// 6f65c4f5 vivo //5e11c594 小米
            log.info("Version is " + version);
            log.info("PC platform : " + System.getProperty("os.name"));
            log.info("System File Encoding: " + System.getProperty("file.encoding"));
            CommandLineParser parser = new DefaultParser();
            Options options = new Options();
            options.addOption("h", "help", false, "Print this usage information");
            options.addOption("a", "activity", true, "Android package's main activity");
            options.addOption("b", "ios_bundle_id", true, "iOS bundle id");
            options.addOption("c", "run_count", true, "Maximum click count");
            options.addOption("d", "crawler_ui_depth", true, "Maximum Crawler UI Depth");
            options.addOption("e", "performance", false, "record performance data");
            options.addOption("f", "config", true, "Yaml config  file");
            options.addOption("i", "ignore crash", false, "Ignore crash");
            options.addOption("l", "loop count", true, "Crawler loop count");
            options.addOption("m", "run monkey", false, "run in monkey mode");
            options.addOption("n", "ios_bundle_name", false, "ios bundle");
            options.addOption("o", "output_dir", true, "ouptut directory");
            options.addOption("p", "package", true, "Android package name");
            options.addOption("r", "crawler_running_time", true, "minutes of running crawler ");
            options.addOption("s", "server_ip", true, "appium server ip");
            options.addOption("t", "port", true, "appium port");
            options.addOption("u", "udid", true, "device serial");
            options.addOption("v", "version", false, "build version with date");
            options.addOption("w", "wda_port", true, "wda port for ios");
            options.addOption("x", "write_to_db", false, "write performance data to influxDB");

            CommandLine commandLine = parser.parse(options, args);

            if (commandLine.hasOption('h')) {
                log.info(
                        "\n" +
                                "    -a  Android package's main activity\n" +
                                "    -b  iOS bundle ID\n" +
                                "    -c  Maximum click count \n" +
                                "    -d  Maximum crawler UI depth \n" +
                                "    -e  Record performance data \n" +
                                "    -f  Yaml config  file\n" +
                                "    -h  Print this usage information\n" +
                                "    -i  Ignore crash\n" +
                                "    -l  Execution loop count\n" +
                                "    -m  Run monkey\n" +
                                "    -o  Output directory" +
                                "    -p  Android package name\n" +
                                "    -r  Crawler running time\n" +
                                "    -s  Appium server ip\n" +
                                "    -t  Appium port\n" +
                                "    -u  Device serial\n" +
                                "    -v  Version\n" +
                                "    -w  WDA port for ios\n" +
                                "    -x  Write data to influx db"
                );
                return null;
            }

            if (commandLine.hasOption("v")) {
                log.info(version);
                return null;
            }

            if (commandLine.hasOption("f")) {
                configFile = System.getProperty("user.dir") + File.separator + commandLine.getOptionValue('f');
                configFile = configFile.trim();
                log.info(configFile);
            } else {
                log.info("Please provide config file");
                return null;
            }

            if (commandLine.hasOption("m")) {
                isMonkey  = true;
            }else{
                isMonkey = false;
            }

            if (commandLine.hasOption("u")) {
                udid = commandLine.getOptionValue('u');
            } else {
                log.info("Please provide device serial");
                return null;
            }

            if (commandLine.hasOption("o")) {
                outputDir = commandLine.getOptionValue('o').trim();

                if (Util.isDir(outputDir)) {
                    ConfigUtil.setOutputDir(outputDir);
                } else {
                    log.info("output directory " + outputDir + " is not a directory or doesn't exist");
                    return null;
                }
            }



            if (commandLine.hasOption("s")) {
                ConfigUtil.setServerIp(commandLine.getOptionValue('s').trim());
            }

            if (commandLine.hasOption("a")) {
                ConfigUtil.setActivityName(commandLine.getOptionValue('a'));
            }


            if (commandLine.hasOption("c")) {
                ConfigUtil.setClickCount(Long.valueOf(commandLine.getOptionValue('c')));
            }

            if (commandLine.hasOption("d")) {
                ConfigUtil.setCrawlerRunningTime(commandLine.getOptionValue('d'));
            }

            if (commandLine.hasOption("x")) {
                writeToDB = true;
                DBUtil.initialize();
            }

            if (commandLine.hasOption("i")) {
                ConfigUtil.setIgnoreCrash(true);
            }

            if (commandLine.hasOption("p")) {
                ConfigUtil.setPackageName(commandLine.getOptionValue('p'));
            }



            //下面的值会修改配置文件初始化后得到的默认值
            if (commandLine.hasOption("r")) {
                ConfigUtil.setCrawlerRunningTime(commandLine.getOptionValue('r'));
            }

            if (commandLine.hasOption("t")) {
                ConfigUtil.setPort(commandLine.getOptionValue('t'));
            }

            return commandLine;
        }catch (Exception e){
            e.printStackTrace();
        }

        return  null;
    }

    public boolean initCrawler(){

        String cmdResult = Util.exeCmd(String.format("adb -s %s shell pm list package -3|grep '%s'",udid,ConfigUtil.getPackageName()),false);
        if(!cmdResult.contains(ConfigUtil.getPackageName())){
            log.error("手机中还没有安装："+ConfigUtil.getPackageName());
            return false;
        }

        //白名单acitivity中只抓取一次的记录
        caputure_onlyone_white_page = ConfigUtil.getCaputureOnlyoneWhitePage();
        if(caputure_onlyone_white_page!=null && caputure_onlyone_white_page.size()>0){
            for(String each_gray_xpath_str:caputure_onlyone_white_page){
                caputure_onlyone_record.addProperty(each_gray_xpath_str,false);
            }
        }
        //初始化符合滚动xpath页面只抓取一次的状态
        need_scrolled_xpath = ConfigUtil.getNeedScrolledXpath();
        if(need_scrolled_xpath!=null && need_scrolled_xpath.size()>0){
            for(String each_need_scrolled_xpath:need_scrolled_xpath){
                need_scrolled_xpath_record.addProperty(each_need_scrolled_xpath,false);
            }
        }

        //初始化Xpath内容
        XPathUtil.initialize(udid);
        xpathUtil = new XPathUtil();
        /*//如果从首页开始 根据命令来判断
        getFirstPageNodes*/
        return  true;
    }




    public void depthFirstSearch(){
        List<String> nodes = xpathUtil.getNodeData(xpathUtil.getCurrentPageName());
        if(nodes==null || nodes.size()==0){
            return;
        }
        xpathUtil.dfs(nodes.get(0));
    }

    /**
     * 获取第一个页面的nodes
     */
    public void getFirstPageNodes(){
        root_list = xpathUtil.findAllClickables("",false);
        //判断是否能够滑动
        xpathUtil.setFirstPageName();
        if(root_list==null){
            return ;
        }
        xpathUtil.add_nodes(root_list);
        for(String node : root_list){
            xpathUtil.addTraverseRoute(node,node);
            xpathUtil.addAlreadyFindNode(node);
        }
        xpathUtil.addNodesData(xpathUtil.getCurrentPageName(),root_list);
        //1、获取根页面的所有合法节点
        //2、添加到数据结构中add_nodes
        //3、遍历根节点中的Nodes加入路径  和 加入找到的集合中
    }

    public static void main(String[] args) throws Exception {
        CrawlerNew crawlerNew = new CrawlerNew();
        //读取命令行配置
        CommandLine commandLine = crawlerNew.readCommandLine(args);
        //初始化配置文件 config.yml
        ConfigUtil.initialize(crawlerNew.configFile, udid);

        AppiumDriver appiumDriver=null;

        //启动Appium
        if (Util.isAndroid(udid)) {
            appiumDriver = Driver.prepareForAppiumAndroid(ConfigUtil.getPackageName(), ConfigUtil.getActivityName(), ConfigUtil.getUdid(), ConfigUtil.getPort());
        }
        if (appiumDriver == null) {
            log.error("Fail to start appium server!");
            return;
        }

        crawlerNew.initCrawler();

        summaryMap = (Map<String, String>) new ListOrderedMap();
        isReported = false;
        beginTime = new Date();

        Util.createDir(ConfigUtil.getRootDir());




        // 开启log日志获取task
        logName = Driver.startLogRecord();
        //初始化报告
        initReport();

        Runtime.getRuntime().addShutdownHook(new CtrlCHandler());

        try {
            //等待App完全启动,否则遍历不到元素
            Driver.sleep(5);
            if (commandLine.hasOption("e") && Util.isAndroid()) {
                PerfUtil.writeDataToFileAsync(crawlerNew.writeToDB);
            }

            if (commandLine.hasOption("e") && !Util.isAndroid()) {
                Driver.startPerfRecordiOS();
            }




            if (isMonkey) {
                String pageSource = Driver.getPageSource();
                //开始Monkey测试
                log.info("----------------Run in monkey mode-----------");
                XPathUtil.monkey(pageSource);
            } else {
                //开始遍历UI
                log.info("------------Run in crawler mode----------------");
                // 先跳转到我的淘宝
                WebElement myTaobao = Driver.findElementsByAccessibilityId("我的淘宝");//("//*[@class='android.widget.FrameLayout'][5]");
                if (myTaobao != null) {
                    log.info("-************************************进入个人信息");
                    if (myTaobao.isEnabled()) {
                        myTaobao.click();
                    }
                }
                log.info("-************************************休息3秒");
                Driver.sleep(3);
                String pageSource = Driver.getPageSource();
                log.info("-************************************开始遍历个人信息页");

                crawlerNew.getFirstPageNodes();
                /*crawlerNew.xpathUtil.scrollToBottom(new XPathUtil.Rect(512,0,512,1788));
                Driver.sleep(3);
                crawlerNew.xpathUtil.scrollToBottom(new XPathUtil.Rect(512,0,512,2500));*/
                crawlerNew.depthFirstSearch();
                //XPathUtil.getNodesFromFile(pageSource, 0);//cky
            }

            log.info("------------------------------Complete testing. Please refer to report.html for detailed information.----------------");
            log.info("------------------------------Press Ctrl + C to generate video file and report.----------------");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("------------------------------ERROR. Testing stopped unexpectedly!!!!----------------");
        }

        Driver.sleep(5);

        PerfUtil.stop();

        executeTask();

        if (isMonkeyMode()) {
            log.info("Complete testing in monkey mode");
        }

        Driver.driver.quit();

    }

    private static void generateVideo() {
        log.info("Method : generateVideo()");

        List<String> fullList = Util.getFileList(ConfigUtil.getScreenShotDir(), ".png", true);

        //Generate full video
        try {
            log.info("Generating full video file, please wait...");
            PictureUtil.picToVideo(ConfigUtil.getRootDir() + File.separator + "testing_steps.mp4", fullList);
        } catch (Exception e) {
            log.error("Fail to generate full.mp4 file");
            e.printStackTrace();
        }

        //Generate crash video
        try {
            if (crashFileList.size() > 0) {

                log.info("Generating " + crashFileList.size() + " crash video files...");
                List<String> fullListWithoutPath = Util.getFileList(ConfigUtil.getScreenShotDir(), ".png", false);
                log.info("Generating crash video file, please wait...");
                int beginIndex = 0;

                int size = fullListWithoutPath.size();
                for (String crashStep : crashFileList) {
                    int endIndex = fullListWithoutPath.indexOf(crashStep);
                    //显示一张crash后的照片
                    if (-1 != endIndex && endIndex <= size) {
                        if (endIndex + 1 < size) {
                            endIndex++;
                        }
                        String fileName = ConfigUtil.getRootDir() + File.separator + "crash" + File.separator + fullListWithoutPath.get(endIndex - 1).replace(".png", ".mp4");
                        PictureUtil.picToVideo(fileName, fullList.subList(beginIndex, endIndex + 1));
                        beginIndex = endIndex + 1;
                    }
                }
            }

        } catch (Exception e) {
            log.error("Fail to generate crash.mp4 file");
            e.printStackTrace();
        }

        log.info("Complete generating video file!");
    }

    private static List<String> getCrashSteps(String crashName) {
        List<String> stepList = new ArrayList<>();

        int picCount = (int) ConfigUtil.getLongValue(ConfigUtil.CRASH_PIC_COUNT);
        List<String> screenshotList = Util.getFileList(ConfigUtil.getScreenShotDir(), ".png", false);
        int index = screenshotList.indexOf(crashName);

        if (-1 == index) {
            log.error("Fail to find crash file " + crashName + " in screenshot folder");
            return stepList;
        }

        int length = screenshotList.size();

        int startIndex = index - picCount + 2;
        int endIndex = index + 2;

        log.info("Init StartIndex " + startIndex + " Init EndIndex " + endIndex);

        if (startIndex < 0) {
            while (startIndex != 0) {
                startIndex++;
            }
        }

        if (endIndex >= length) {
            endIndex = index + 1;
        }

        log.info("StartIndex " + startIndex + " EndIndex " + endIndex);
        stepList = screenshotList.subList(startIndex, endIndex);

        log.info(stepList.toString());

        return stepList;
    }

    private static void initReport() {
        summaryMap.put("手机系统 - Mobile operating system", Driver.getPlatformName());
        summaryMap.put("系统版本 - OS version", Driver.getPlatformVersion());
        summaryMap.put("设备UUID - Device UUID", udid);
        summaryMap.put("测试开始时间 - Testing start time", Util.getTimeString(beginTime));

        if (Util.isAndroid()) {
            summaryMap.put("包名 - Package name", ConfigUtil.getPackageName());
            summaryMap.put("主活动 - Main Activity", ConfigUtil.getActivityName());
        }
    }

    private static void generateReport() {
        log.info("Method : generateReport()");

        int index = 0;
        List<ArrayList<String>> detailedList = new ArrayList<>();
        List<ArrayList<String>> clickedList = new ArrayList<>();
        String crashDir = ConfigUtil.getRootDir() + File.separator + "crash" + File.separator;
        log.info("Crash dir is " + crashDir);
        //String crashDir = "./crash" + File.separator;
        crashFileList = Util.getFileList(crashDir);
        int crashCount = crashFileList.size();

        summaryMap.put("总执行时间 - Total running time", Util.timeDifference(beginTime.getTime(), new Date().getTime()));
        if (!isMonkey) {
            summaryMap.put("元素点击数量 - Element clicked count", String.valueOf(XPathUtil.getClickCount()));
        }
        summaryMap.put("系统日志 - System log", "<a href=\"" + logName + "\">" + logName + "</a>");
        summaryMap.put("Crash数量 - Crash count", String.valueOf(crashCount));

        if (isMonkey) {
            summaryMap.put("测试类型 - Test type", "Monkey随机测试");
        } else {
            summaryMap.put("测试类型 - Test type", "UI元素遍历");
        }

        if (crashCount > 0) {
            log.info("Crash count is : " + crashCount);
            int picCount = (int) ConfigUtil.getLongValue(ConfigUtil.CRASH_PIC_COUNT);
            ArrayList<String> headerRow = new ArrayList<>();
            headerRow.add("HEAD");
            headerRow.add("NO");

            for (int i = 1; i < picCount; i++) {
                headerRow.add("Step " + i);
            }
            headerRow.add("Crash");
            headerRow.add("Video");

            detailedList.add(headerRow);
        }

        for (String item : crashFileList) {
            ArrayList<String> row = new ArrayList<>();
            index++;
            row.add("<img width=\"100px\">" + String.valueOf(index) + "</img>");
            List<String> crashStepList = getCrashSteps(item);

            for (String step : crashStepList) {
                row.add("<a href=\"" + crashDir + step + "\">"
                        + " <img width=\"50%\" src=\"" + crashDir + step + "\"/>"
                        + "</a>");
                String dest = crashDir + step;
                String src = ConfigUtil.getRootDir() + File.separator + ConfigUtil.SCREEN_SHOT + File.separator + step;
                Util.copyFile(new File(src), new File(dest));
            }

            item = item.replace(".png", ".mp4");
            row.add("<a href=\"" + crashDir + item + "\"/>" + item + "</a>");
            detailedList.add(row);
        }

        int clickedActivityCount = XPathUtil.getClickedActivityMap().size();

        if (clickedActivityCount > 0) {
            log.info("Clicked Activity count is " + clickedActivityCount);

            ArrayList<String> headerRow = new ArrayList<>();
            headerRow.add("HEAD");
            headerRow.add("Activity");
            headerRow.add("Click Count");
            clickedList.add(headerRow);

            Map<String, Long> map = XPathUtil.getClickedActivityMap();
            for (String activity : map.keySet()) {
                ArrayList<String> row = new ArrayList<>();
                row.add(activity);
                row.add(map.get(activity).toString());
                clickedList.add(row);
            }
        }

        int monkeyClickCount = XPathUtil.getMonkeyClickedMap().size();

        if (monkeyClickCount > 0) {
            Map<String, Map<String, Long>> monkeyMap = XPathUtil.getMonkeyClickedMap();

            ArrayList<String> headerRow = new ArrayList<>();
            headerRow.add("HEAD");
            headerRow.add("Activity");
            headerRow.add("Detail");

            clickedList.add(headerRow);

            for (String newaActivity : monkeyMap.keySet()) {
                ArrayList<String> row = new ArrayList<>();
                row.add(newaActivity);
                row.add(monkeyMap.get(newaActivity).toString());
                clickedList.add(row);
            }
        }

        String reportName = ConfigUtil.getRootDir() + File.separator + "report.html";
        File report = new File(reportName);

        ReportUtil.setSummaryMap(summaryMap);
        ReportUtil.setDetailedList(detailedList);
        ReportUtil.setClickedList(clickedList);
        ReportUtil.generateReport(report);
        log.info("\n\n------------------------------Test report : " + reportName + "\n\n");
    }
}


//    private static void showClickedItems(){
//        HashSet<String> clickedSet = XPathUtil.getSet();
//        log.info(clickedSet.size() + " elements are clicked");
//
//        for(String str: clickedSet){
//            log.info(str);
//        }
//
//        log.info("==============list end==========");
//    }
