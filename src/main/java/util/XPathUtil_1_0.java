package util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.appium.java_client.MobileElement;
import org.openqa.selenium.By;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.*;


public class XPathUtil_1_0 {
    public static final int SLEEP_TIME = 4;//second
    public static final int BACK_SLEEP_TIME = 2;
    public static org.slf4j.Logger log = LoggerFactory.getLogger(XPathUtil_1_0.class);
    public static XPath xpath = XPathFactory.newInstance().newXPath();
    private static DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

    private static Map<String, Map<String, Long>> monkeyClickedMap = new HashMap<>();
    private static Map<String, Long> clickedActivityMap = new HashMap<>();
    private static HashSet<String> set = new LinkedHashSet<>();
    private static DocumentBuilder builder;
    private static boolean stop = false;
    private static String appName;
    private static String appNameXpath;
    private static List<String> packageNameList;
    private static List<String> nodeBlackList;
    private static List<String> nodeWhiteList;
    private static List<String> nodeNameExcludeList;
    private static List<String> structureNodeNameExcludeList;
    private static List<String> pressBackPackageList;
    private static List<String> backKeyTriggerList;
    ////////
    private static List<String> blackPages;
    ///////
    private static List<String> xpathNotFoundElementList = new ArrayList<>();
    private static List<String> clickFailureElementList = new ArrayList<>();
    private static String clickXpath;
    private static String tabBarXpath;
    private static String firstLoginElemXpath;
    private static ArrayList<Map> loginElemList;
    private static Set<String> xpathBlackSet;
    private static Set<String> nodeXpathBlackSet;
    private static int scale;
    private static boolean ignoreCrash;
    private static boolean removedBounds = false;
    private static boolean swipeVertical = ConfigUtil.getBooleanValue(ConfigUtil.ENABLE_VERTICAL_SWIPE);

    //按back键回到主屏后 重启app的次数
    private static int pressBackCount = 3;
    private static long clickCount = 0;
    private static long maxDepth = 0;
    private static String pic = null;
    private static String backKeyXpath = null;
    private static int deviceHeight;
    private static int deviceWidth;

    //Monkey related configuration
    private static Map<String,Long> monkeyEventRatioMap = new HashMap<>();
    private static Map<String,Long> monkeyEventSummaryRatioMap = new HashMap<>();
    private static List<Point> specialPointList = new ArrayList<>();
    private static List<Point> longPressPointList = new ArrayList<>();
    private static List<String> xpathItemList = new ArrayList<>();
    private static long runningTime;
    private static long testStartTime;// = System.currentTimeMillis();
    private static StringBuilder repoStep = new StringBuilder();

    public static HashSet<String> getSet() {
        return set;
    }
    public static Map<String, Long> getClickedActivityMap() {
        return clickedActivityMap;
    }
    public static Map<String, Map<String, Long>> getMonkeyClickedMap() {
        return monkeyClickedMap;
    }


    ////////////////////////
    private JsonObject traverse_route = new JsonObject();//node与click_path隐射
    private List<String> already_find_node = new ArrayList<String>();
    private JsonObject access_root_error_record = new JsonObject();//记录没有找到某个node对应的次数
    private Set<String> traverse_track_record = new HashSet<>();//遍历路径记录
    private List<String> new_nodes = new ArrayList<>();
    private Node first_root_node = null;
    private String last_page_source = null;
    private String current_page_source = null;
    private JsonObject gray_activity_record = new JsonObject();//遍历过程中灰activity的遍历次数
    private JsonObject gray_activity_record_itslef = new JsonObject();//遍历过程中灰activity自己遍历自己的遍历次数
    private static int gray_activity_max_traverse;//灰名页面遍历的最大次数（两个页面进入循环）
    private static int gray_activity_max_traverse_itself;//灰名页面遍历自己遍历最大次数（单个页面进入死循环了）
    private boolean findNewNode = true;
    //////////////////////////
    private static LinkedHashMap<String,LinkedHashMap<String,LinkedHashMap>> node_neighbors = new LinkedHashMap<>();
    private  Set<String> nodeVisited = new HashSet<>();
    //记录各个页面的节点
    private HashMap<String,List<String>> nodesData = new HashMap<>();

    private HashMap<String,Rect> pageScrollFlagCache = new HashMap<>();
    private HashMap<String,Integer> pageScrollTime = new HashMap<>();
    private String currentPageName = "";
    private String lastPageName = "";

    private String fisrtPageName = "";

    /**
     * 初始化
     * @param udid
     */
    public static void initialize(String udid){
        log.info("Method: initialize");
        stop = false;
        //运行时间限制
        runningTime = ConfigUtil.getLongValue(ConfigUtil.CRAWLER_RUNNING_TIME);
        testStartTime = System.currentTimeMillis();
        removedBounds = ConfigUtil.boundRemoved();


        log.info("Running time is " + runningTime);

        if(runningTime <=0){
            runningTime = 24*6*7;
        }

        log.info("Crawler running time is " + runningTime);

        //先遍历TabBar内的元素 再遍历 TabBar上的元素

        //从XML中获取包名
        if(Util.isAndroid(udid)){
            appNameXpath = "(//*[@package!=''])[1]";
            appName = ConfigUtil.getPackageName().trim();
        }

        packageNameList = new ArrayList<>(Arrays.asList(appName));

        //添加合法包名
        if(Util.isAndroid(udid)){
            packageNameList.addAll(ConfigUtil.getListValue(ConfigUtil.ANDROID_VALID_PACKAGE_LIST));
            ///
        }else{
            packageNameList.addAll(ConfigUtil.getListValue(ConfigUtil.IOS_VALID_BUNDLE_LIST));
        }

        //灰名页面遍历的最大次数（两个页面进入循环）
        gray_activity_max_traverse = ConfigUtil.getGrayActivityMaxTraberse();
        //灰名页面遍历自己遍历最大次数（单个页面进入死循环了）
        gray_activity_max_traverse_itself = ConfigUtil.getGrayActivityMaxTraberseItself();


        //页面黑名单 不在点击范围内
        blackPages = ConfigUtil.getListValue(ConfigUtil.BLACK_PAGES);

        //元素黑名单 不在点击范围内
        nodeBlackList = new ArrayList<>();
        nodeBlackList.addAll(ConfigUtil.getListValue(ConfigUtil.ITEM_BLACKLIST));
        xpathBlackSet = getBlackKeyXpathSet(nodeBlackList);

        //白名单 可以点击多次的
        nodeWhiteList = new ArrayList<>();
        nodeWhiteList.addAll(ConfigUtil.getListValue(ConfigUtil.ITEM_WHITE_LIST));

        try{
            builder =  builderFactory.newDocumentBuilder();
        }catch (Exception e){
            log.error("!!!!!!!!!!document builder is null!!!!!!!!!!");
            e.printStackTrace();
        }

        //获取第一个登录操作元素的Xpath
        if(Util.isAndroid(udid)){
            loginElemList = ConfigUtil.getListMapValue(ConfigUtil.ANDROID_LOGIN_ELEMENTS);
        }

        if(loginElemList.size() != 0){
            String key = (String)loginElemList.get(0).keySet().toArray()[0];
            firstLoginElemXpath = (( Map<String,String>)loginElemList.get(0).get(key)).get("XPATH");
        }

        //构建查找元素时用到的Xpath
        StringBuilder clickBuilder = new StringBuilder();
        StringBuilder tabBuilder = null;

        //Android
        if(Util.isAndroid(udid)){
            //构建查找tab bar的Xpath
            String androidBottomBarID =  ConfigUtil.getStringValue(ConfigUtil.ANDROID_BOTTOM_TAB_BAR_ID);
            clickBuilder.append("//*[");
            clickBuilder.append(ConfigUtil.getStringValue(ConfigUtil.ANDROID_CLICK_XPATH_HEADER));//@clickable="true"
            if(androidBottomBarID != null) {
                //tabBuilder = new StringBuilder("//*[@resource-id=\"" + androidBottomBarID + "\"]/descendant-or-self::*[@clickable=\"true\"]");
                tabBuilder = new StringBuilder("//*[" + androidBottomBarID +"]/descendant-or-self::*[@clickable=\"true\"]");
                clickBuilder.append(" and not(ancestor-or-self::*[" +androidBottomBarID +"])");
            }

            //构建查找元素的xPath
            for(String item: ConfigUtil.getListValue(ConfigUtil.ANDROID_EXCLUDE_TYPE)){
                clickBuilder.append(" and @class!=" + "\"" + item + "\"");
            }
        }

        clickBuilder.append("]");
        //不需要点击的
        clickXpath = clickBuilder.toString();
        if(tabBuilder != null) {
            tabBarXpath = tabBuilder.toString();
            log.info("tab bar xpath: " + tabBarXpath);
        }else {
            log.info("tab bar has no xpath");
        }

        log.info("clickable elements xpath: " + clickXpath);

        //Get screen scale
        scale = Driver.getScreenScale();
        deviceHeight = Driver.getDeviceHeight();
        deviceWidth = Driver.getDeviceWidth();

        ignoreCrash = ConfigUtil.getBooleanValue(ConfigUtil.IGNORE_CRASH);
        //xpath中排除以下属性, 仅限android  小写字母
        nodeNameExcludeList = ConfigUtil.getListValue(ConfigUtil.NODE_NAME_EXCLUDE_LIST);
        //xpath中排除以下属性, android和iOS  小写字母
        structureNodeNameExcludeList = ConfigUtil.getListValue(ConfigUtil.STRUCTURE_NODE_NAME_EXCLUDE_LIST);
        maxDepth = ConfigUtil.getDepth();
        pressBackPackageList = ConfigUtil.getListValue(ConfigUtil.PRESS_BACK_KEY_PACKAGE_LIST);

        if(Util.isAndroid()){
            backKeyXpath = ConfigUtil.getStringValue(ConfigUtil.ANDROID_BACK_KEY);
        }

        backKeyTriggerList = ConfigUtil.getListValue(ConfigUtil.BACK_KEY_TRIGGER_LIST);
    }


    public static void showFailure(){
        log.info("Method: showFailure");

        int size = clickFailureElementList.size();
        if(size !=0){
            log.error("\n==============================Fail to click " + clickFailureElementList.size() + " elements \n" );

            for(String str : clickFailureElementList){
                log.info(str);
            }
        }else{
            log.info("Cool ! All the found elements are clicked");
        }


        size = xpathNotFoundElementList.size();
        if(size != 0){
            log.error("\n\n==============================Fail to find " + size + " elements by xpath\n");

            for( String str: xpathNotFoundElementList){
                log.info(str);
            }
        }else{
            log.info("Congratulations ! All the xpath elements are found");
        }

        log.error("\n==============================");
    }

    /**
     * monkey 测试初始化
     */
    private static void initMonkey(){
        log.info("Method: initMonkey");

        testStartTime = System.currentTimeMillis();
        //Init money ratio data
        for(String event : ConfigUtil.MONKEY_EVENT_RATION_LIST){
            long ratio = ConfigUtil.getLongValue(event);
            if(ratio > 0){
                monkeyEventRatioMap.put(event,ratio);
            }
        }

        List<String> list = ConfigUtil.getListValue(ConfigUtil.MONKEY_SPECIAL_POINT_LIST);

        for(String str : list){
            str = str.trim();
            String[] value = str.split(",");
            int x = Integer.parseInt(value[0]);
            int y = Integer.parseInt(value[1]);

            specialPointList.add(new Point(x,y));
            log.info("Special point x: " + x + " y: " + y);
        }

        list = ConfigUtil.getListValue(ConfigUtil.LONG_PRESS_LIST);
        for(String str : list){
            str = str.trim();
            String[] value = str.split(",");
            int x = Integer.parseInt(value[0]);
            int y = Integer.parseInt(value[1]);

            longPressPointList.add(new Point(x,y));
            log.info("Long press point x: " + x + " y: " + y);
        }

        xpathItemList = ConfigUtil.getListValue(ConfigUtil.CLICK_ITEM_XPATH_LIST);

        log.info("Monkey running time is " + runningTime + " seconds");
        log.info("Monkey event list and ratio : \n" + monkeyEventRatioMap );
    }

    /**
     * 过滤黑元素中的xpath
     * @param list
     * @return
     */
    private static Set<String> getBlackKeyXpathSet(List<String> list){
        Set<String> set = new HashSet<>();

        for(String item : list){
            if(Util.isXpath(item)){
                set.add(item);
            }
        }

        log.info("Blacklist length is " + set.size());
        return set;
    }


    public static PackageStatus isValidPackageName(String packageName){
        return isValidPackageName(packageName,true);

    }

    //检查包名，返回false时 说明已经crash或跳出了当前app
    //此时如果ignoreCrash=true, stop会被重新设成false, 然后重启app
    //此时如果ignoreCrash=fasle, stop会被高成true，但不会重启app
    public static PackageStatus isValidPackageName(String packageName, boolean restart){
        log.info("Method: isValidPackageName");
        PackageStatus isValid = PackageStatus.PRESS_BACK;

        //判断当前包名是否合法
        for(String name : packageNameList){
            if(null != packageName && packageName.contains(name)){
                isValid = PackageStatus.VALID;
                break;
            }
        }

        log.info(packageName + ": isValid=" + isValid);

        return isValid;
    }

    public static String clickElement(MobileElement elem, String xml){
        log.info("Method: clickElement");

        String page = xml;

        try {
            if(Util.isAndroid()){
                String activityName = Driver.getCurrentActivity();
                Long clickCount = clickedActivityMap.get(activityName);

                if(clickCount == null){
                    clickCount = 1L;
                }else{
                    clickCount ++;
                }

                clickedActivityMap.put(activityName,clickCount);
                log.info("clickedActivityMap = " + clickedActivityMap.toString());
            }

            //Sometimes, UIA2 will throw StaleObjectException Exception here
            int x = elem.getCenter().getX();
            int y = elem.getCenter().getY();

            pic = PictureUtil.takeAndModifyScreenShot(x*scale,y*scale);

            //String appName;
            clickCount++;
            repoStep.append("CLICK : " + x + "," + y + "\n");

            clickFailureElementList.add(elem.toString());
            elem.click();
            clickFailureElementList.remove(elem.toString());

            log.info("------------------------CLICK " + clickCount + "  X: " + x + " Y: " + y +" --------------------------");

            //等待新UI初始化 时间需要
            Driver.sleep(SLEEP_TIME);
            page = Driver.getPageSource();
            String appName = getAppName(page);

            PackageStatus status = isValidPackageName(appName,false);

            if(PackageStatus.VALID != status){
                page = Driver.getPageSource();
            }

            /*if(clickCount >= ConfigUtil.getClickCount()){
                stop = true;
            }*/

           /* String temp ="";

            try{
                String elemStr = elem.toString();
                List<String> inputClassList = ConfigUtil.getListValue(ConfigUtil.INPUT_CLASS_LIST);
                List<String> inputTextList = ConfigUtil.getListValue(ConfigUtil.INPUT_TEXT_LIST);
                int size = inputClassList.size();

                for(String elemClass : inputClassList){
                    temp = elemClass;
                    if(elemStr.contains(elemClass)){
                        int index = Util.internalNextInt(0,size);
                        String text = inputTextList.get(index);
                        elem.setValue(text );
                        repoStep.append("INPUT :" + elem.toString() + " ; " + text + "\n");
                        log.info("Element " + temp + " set text : " + text);
                        break;
                    }
                }
            }catch (Exception e){
                log.error("fail to set text for class : " + temp);
            }*/

        }catch (Exception e){
            e.printStackTrace();
            log.error("\n!!!!!!Fail to click elem : " + elem);
        }

        return  page;
    }

    private static Set<String> getBlackNodeXpathSet(Document document) {
        String xpathStr = "";
        Set<String> nodeSet =  new HashSet<>();

        try {
            for (String item : xpathBlackSet) {
                xpathStr = item;
                NodeList nodes = (NodeList) xpath.evaluate(item, document, XPathConstants.NODESET);
                int length = nodes.getLength();

                while(-- length >= 0){
                    Node tmpNode = nodes.item(length);
                    String nodeXpath = getNodeXpath(tmpNode);

                    if(nodeXpath != null){
                        nodeSet.add(nodeXpath);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            log.error("Fail to deal with black key xpath " + xpathStr);
        }

        log.info("Black xpath set length is : "  + nodeSet.size());
        return nodeSet;
    }

    public static String getNodesFromFile(String xml, long currentDepth) throws Exception{
        log.info("Method: getNodesFromFile");

        log.info("Context: " + Driver.driver.getContextHandles().toString());

        //检查运行时间
        long endTime = System.currentTimeMillis();

        if((endTime - testStartTime) > ( runningTime * 60 * 1000)) {
            log.info("已运行" + (endTime - testStartTime)/60/1000 + "分钟，任务即将结束");
            stop = true;
            return xml;
        }

        Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        NodeList nodes = (NodeList) xpath.evaluate(clickXpath, document, XPathConstants.NODESET);

        log.info(String.valueOf("UI nodes length : " + nodes.getLength()));

        int length = nodes.getLength();

        String previousPageStructure = Driver.getPageStructure(xml,clickXpath);
        String afterPageStructure = previousPageStructure;
        String currentXML = xml;

        //检查stop为true时快速退出
        if(stop){
            log.info("-----stop=true, Fast exit");
            return currentXML;
        }

        log.info("Back key list size is " + backKeyTriggerList.size());

        //根据关键字触发Back Key
        if( (backKeyTriggerList != null)  && (backKeyTriggerList.size() > 0) ){
            for(String key : backKeyTriggerList){
                if (currentXML.contains(key)){
                    log.info("Back key trigger : " + key + " is found, press back key");
                    Driver.takeScreenShot();
                    Driver.pressBack(repoStep);
                    Driver.sleep(BACK_SLEEP_TIME);
                    //cky 上传当前黄金页面
                    currentXML = Driver.getPageSource();
                    return currentXML;
                }
            }
        }

        //遍历前检查 如果不是小程序的话不用关心
        // 1.包名是否合法，包名不合法，不会重启
        // 2.检查Depth,超过预定值就返回
        // 3.是否有可遍历的元素,如果没有遍历TabBar,如果无TabBar
        String packageName = getAppName(currentXML);

        if (packageName.equals("com.tencent.mm") || packageName.equals("com.tencent.xin")){
            if(currentXML.contains("附近的小程序")){
                log.info("已遍历完小程序，跳转到了小程序主页面，遍历停止");
                stop = true;

                return currentXML;
            }
        }

        log.info("++++++++++++++++++ Activity Name : " + Driver.getCurrentActivity() +"+++++++++++++++++++++++");

        //1.检查当前UI的包名是否正确，一定要先查包名！因为其内部控制了stop的值, 包名不合法，-是否应该重启app?--
        if(PackageStatus.VALID != isValidPackageName(packageName,true)){
            log.info("=====================package: "+ packageName + " is invalid, return ....==============================");
            currentXML = Driver.getPageSource();
            return currentXML;
        }

        // 2.检查Depth,超过预定值就返回
        currentDepth++;
        log.info("------Depth: " + currentDepth);
        if(currentDepth > maxDepth){
            stop = true;
            log.info("Return because exceed max depth: " + maxDepth);
            //Driver.pressBack(repoStep);
            currentXML = Driver.getPageSource();
            return currentXML;
        }

        log.info("node length is " + length);

        //3.检查页面内有没有可以遍历的元素, 跳过循环，然后查找是否有TabBar元素，此处不可用return，不然tabBar不会被遍历
        if(length == 0){
            log.error("========!!!!!!!No UI node found in current page!!!!! Begin to find tab bar element... =====");
        }

        showTabBarElement(currentXML,tabBarXpath);

        //处理黑名单xpath
        nodeXpathBlackSet =  getBlackNodeXpathSet(document);
        int blackNodeXpathSize = nodeXpathBlackSet.size();
        log.info("black node Xpath size is " + blackNodeXpathSize);

        //遍历UI内的Node元素
        while(--length >= 0 && !stop){
            log.info("Element index is : " + length);

            Node tmpNode = nodes.item(length);
            String nodeXpath = getNodeXpath(tmpNode);

            if(nodeXpath == null){
                log.error("Null nodeXpath , continue.");
                continue;
            }

            if(blackNodeXpathSize != 0){
                if(nodeXpathBlackSet.contains(nodeXpath)){
                    log.info("Ignore black xpath item : " + nodeXpath);
                    continue;
                }
            }

            //Comment this if not in GraphLoopTest mode
            //nodeXpath = showNodes(currentXML,nodeXpath);

            //判断当前元素是否点击过
            if(set.add(nodeXpath)){

                //处理白名单
                for (String item : nodeWhiteList) {
                    if (nodeXpath.contains(item.trim())) {
                        log.info("-------remove " + item + " since it is in white list : " + nodeWhiteList);
                        set.remove(nodeXpath);
                    }
                }

                MobileElement elem = Driver.findElementWithoutException(By.xpath(nodeXpath));

                if(null == elem){
                    //元素未找到，重新遍历当前页面
                    xpathNotFoundElementList.add(nodeXpath);
                    log.info("---------Node not found in current UI!!!!!!! Stop current iteration.-----------" );
                    break;
                }

                currentXML = clickElement(elem,currentXML);
                afterPageStructure = Driver.getPageStructure(currentXML,clickXpath);

                //点击后进入到了新的页面
                if(!stop && !isSamePage(previousPageStructure,afterPageStructure)) {
                    log.info("========================================New Child UI================================");

                    //遍历子UI前 先检查包名是否合法
                    packageName = getAppName(currentXML);

                    if(PackageStatus.VALID != isValidPackageName(packageName)){
                        currentXML = Driver.getPageSource();
                        afterPageStructure = Driver.getPageStructure(currentXML,clickXpath);
                        break;
                    }

                    //遍历子UI
                    getNodesFromFile(currentXML,currentDepth);

                    //子页面返回后检查
                    // 1.包名是否合法
                    // 2.stop是否在子页面中设置为了true

                    //在子页面中发现了包名变化， 停止当前遍历
                    if(stop){
                        break;
                    }

                    //从子UI返回后，检查包名
                    currentXML = Driver.getPageSource();
                    packageName = getAppName(currentXML);
                    if(PackageStatus.VALID != isValidPackageName(packageName,false)){
                        break;
                    }

                    //判断从子页面返回后，父页面是否发生了变化
                    afterPageStructure = Driver.getPageStructure(currentXML,clickXpath);

                    if(isSamePage(previousPageStructure,afterPageStructure)){
                        log.info("Parent page stay the same after returning from child page");
                        //页面未变化， 继续遍历
                    }else{
                        log.info("Parent page changed after returning from child page");
                        //从子页面返回后 父页面发了生变化 停止遍历当前页面
                        break;
                    }
                }else{
                    log.info("========================================Same UI");
                }
            }else {
                //元素已经点击过
                log.info("---existed--- " + nodeXpath + "\n");
            }
        }

        log.info("\n\n\n!!!!!!!!!!!!!Done!!!!!!!!!!!!!!! stop: " + stop +"\n\n\n");

        //currentDepth --;

        //包名发生变化退出遍历
        if(stop){
            log.info("\n\n\nPackage name changed: "+ packageName + " set stop to true and return!!!!!!\n\n\n");
            return currentXML;
        }

        log.info("node length after while is " + length);
        boolean shouldPressBack = true;

        //遍历完页面所有元素,且UI未发生变化，回退到上一级
        if(length < 0 && isSamePage(previousPageStructure,afterPageStructure)) {

            //如果有滑动操作
            if(swipeVertical){
                log.info("Swipe vertical is enabled.");

                Driver.swipeVertical(false,repoStep);
                currentXML = Driver.getPageSource();

                previousPageStructure = afterPageStructure;
                afterPageStructure = Driver.getPageStructure(currentXML,clickXpath);

                //如果划动后界面发生了变化
                if(!isSamePage(previousPageStructure,afterPageStructure)){
                    log.info("Page changes after vertical swipe");
                    shouldPressBack = false;
                    currentXML = getNodesFromFile(currentXML,currentDepth);
                }else{
                    log.info("No change found after vertical swipe");
                    //shouldPressBack = true;
                }
            }

            log.info("all the non tab bar nodes in current UI are iterated.");

            //划动完成后才press back
            if(shouldPressBack){
                if(backKeyXpath !=null){
                    log.info("Finding back key ");

                    MobileElement elem = Driver.findElementWithoutException(By.xpath(backKeyXpath));

                    if(elem!=null){
                        log.info("Back key found , clicking back key...");
                        currentXML = clickElement(elem,currentXML);
                        return currentXML;
                    }else{
                        log.info("Back key is not found");
                    }
                }

                //检查是否有TabBar, 如果有TabBar 点击TabBar了的元素 然后返回new page UI XML
                currentXML = clickTabBarElement(currentXML, tabBarXpath);

                if(null != currentXML){
                    log.info("========================================TabBar found, click TabBar and iterate new UI");
                    getNodesFromFile(currentXML,currentDepth);
                }else{
                    //log.info("========================================No TabBar element found");
                    //无TabBar或TabBar上所有的元素都点击过了，按下back key
                    Driver.pressBackAndTakesScreenShot();
                    log.info("========================================press back");

                    currentXML = Driver.getPageSource();
                    packageName = getAppName(currentXML);

                    if(pressBackCount<=0){
                        log.info("Press count is 0, so stop testing");
                        stop = true;
                    }

                    //按back Key键后 返回到了Home Screen 重新启动app
                    if(pressBackCount > 0 && isValidPackageName(packageName,false) != PackageStatus.VALID ){
                        log.info("Returned to Home Screen,due to press back key, so restart app... pressBackCount is :" + pressBackCount);
                        Driver.appRelaunch(repoStep);

                        pressBackCount--;
                        currentXML =  Driver.getPageSource();
                        getNodesFromFile(currentXML,currentDepth);
                    }
                }
            }
        }else{
            //遍历并未完成或因页面发生变化 从break处跳出 ，遍历变化后的页面
            log.info("========================================Stop current UI page testing. Due to UI change");
            currentXML = Driver.getPageSource();
            getNodesFromFile(currentXML,currentDepth);

        }

        log.info("========================================Complete iterating current UI with following elements: ");
        //log.info( "\n\n\n" + previousPageStructure + "\n\n\n");
        log.info("depth before return is " + currentDepth);
        return currentXML;
    }


    private static NodeList getNodeListByXpath(String xml, String xpathExpr) throws Exception{
        Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        return (NodeList) xpath.evaluate(xpathExpr, document, XPathConstants.NODESET);
    }


    private static String getAppName(String xml){
        log.info("Method: getAPPName");
        String name = null;
        NodeList nodeList = null;

        try{
            nodeList = getNodeListByXpath(xml,appNameXpath);
        }catch (Exception e){
            e.printStackTrace();
        }

        if(null == nodeList || nodeList.getLength() == 0){
           log.error("null app name get from xml file");

           if(!Util.isAndroid()){
               //TODO:有时iOS中没有XCUIAPPLICATION
               name = ConfigUtil.getStringValue("IOS_BUNDLE_NAME");
           }

           return name;
        }

        Node node = nodeList.item(0);

        String key = "package";

        //ios查app的名字
        if(!Util.isAndroid()){
            key = "name";
        }

        name = node.getAttributes().getNamedItem(key).getNodeValue();

        log.info("-------AppName---------" + name);
        return name;
    }

    @SuppressWarnings("unchecked")
    public static String userLogin(String xml){
        log.info("Method: userLogin");

        if(firstLoginElemXpath == null){
            return xml;
        }
        try {
            Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
            NodeList nodes = (NodeList) xpath.evaluate(firstLoginElemXpath, document, XPathConstants.NODESET);

            log.info("firstLoginElemXpath : " + firstLoginElemXpath + " login element  : " + nodes.getLength());

            //找到了登录元素
            if(nodes.getLength() == 1){
                for(Map map : loginElemList){
                    String key = (String) map.keySet().toArray()[0];
                    map = ((Map<String,String>) map.get(key));
                    String xpath = map.get(ConfigUtil.XPATH).toString();
                    String action =  map.get(ConfigUtil.ACTION).toString();
                    String value =  String.valueOf(map.get(ConfigUtil.VALUE));

                    try {
                        triggerElementAction(xpath, action, value);
                    }catch (Exception e){
                        log.error("Element " + xpath + "is not found.");
                    }
                }

                //等待新页面初始化
                Driver.sleep(10);
                xml = Driver.getPageSource();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return xml;
    }

    private static void triggerElementAction(String xpath, String action, Object value){
        MobileElement element = Driver.findElement(By.xpath(xpath));

        log.info("Trigger element : " + xpath + " action : " + action + " value : " + value );

        switch (action.toCharArray()[0]){
            case 'c'://click
                element.click();

                if(value != null){
                    Driver.sleep(Integer.parseInt(value.toString()));
                }
                break;
            case 'i'://input
                element.clear();
                element.setValue(value.toString());
                break;
            case 'd'://drag
                List<String> pointsList = Arrays.asList(value.toString().split(","));
                Driver.drag(pointsList);
            default:
                break;
        }
    }

    protected static boolean isSamePage(String pre,String after){
        log.info("Method: isSamePage");
        boolean ret = pre.equals(after);

        //退出时不截图, 只在界面发生变化且stop=false才截图。
        if(!ret && !stop) {
            //TODO:Remove comment
            //Driver.takeScreenShot();
            log.info("++++++++++++++++++ Activity Name  :  " + Driver.getCurrentActivity() + "+++++++++++++++++++++++");
        }

        return ret;
    }

    /**
     * 获取单个Node的 xpath
     * @param node
     * @return
     */
    private static String getNodeXpath(Node node){
        return getNodeXpath(node,false);
    }


    //分析当前节点 根据节点属性来分析
    public static String getNodeXpath(Node node, boolean structureOnly){
        //当前节点的属性
        int length = node.getAttributes().getLength();
        //设置xpath 并获取当前节点的name
        StringBuilder nodeXpath = new StringBuilder("//" + node.getNodeName() + "[");
        //xpath中排除以下属性, 仅限android  小写字母
        //final List<String> nodeNameExcludeList = new ArrayList<>(Arrays.asList("selected","instance","checked","naf","content-desc"));
        //xpath中排除以下属性, android和iOS  小写字母
        //inal List<String> structureNodeNameExcludeList = new ArrayList<>(Arrays.asList("value","lable","name" ,"text"));

        String bounds = "[" + deviceWidth + ","  + deviceHeight + "]";

        while(--length >= 0){

            Node tmpNode = node.getAttributes().item(length);

            String nodeName = tmpNode.getNodeName();
            String nodeValue = tmpNode.getNodeValue();

            if(nodeValue.length() == 0){
                continue;
            }

            //TODO: 当bounds的值为width时 Xpath无法找到该元素。如：当"允许"出现在底部时 ，Xpath中若有bounds的值  查找该元素会失败。
            if(removedBounds && nodeValue.contains(bounds)){
                log.info("Remove bounds,since its value is "+ nodeValue +  " same as screen height and width");
                continue;
            }

            //处理黑名单
            for(Object item : nodeBlackList){
                if(nodeValue.contains(String.valueOf(item))){
                    log.info("-----------------black list item---" + nodeValue);
                    return null;
                }
            }

            //去除不应包含的node属性
            if(nodeNameExcludeList.contains(nodeName.toLowerCase())){
                continue;
            }

            //UI结构 不加入 "value","lable","name"等node属性
            if(structureOnly && structureNodeNameExcludeList.contains(nodeName.toLowerCase())){
                continue;
            }

            //待查找的元素不能超出屏幕的width和height, Android only
            if(Util.isAndroid() && nodeName.equals("bounds")){
                //String value="[1080,468][2160,885]";
                String value = nodeValue;
                value =value.replace("][",",");//"[1080,468,2160,885]";

                int index = value.indexOf(",");
                int startX = Integer.valueOf(value.substring(1,index));

                int indexNext = value.indexOf(",",index+1);
                int startY = Integer.valueOf(value.substring(index+1,indexNext));

                index = value.indexOf(",",indexNext + 1);
                int endX = Integer.valueOf(value.substring(indexNext +1,index));
                int endY = Integer.valueOf(value.substring(index +1,value.length()-1));

                //log.info(String.valueOf(startX));log.info(String.valueOf(startY));log.info(String.valueOf(endX));log.info(String.valueOf(endY));

                //元素显示在界面范围内
                if(startX <0 || startY <0 || endX <0 || endY <0){
                    log.info("<0 ----Removed-----" +nodeValue);
                    return null;
                }

                if(startX > deviceWidth || endX > deviceWidth ||startY > deviceHeight || endY > deviceHeight){
                    log.info(">max ----Removed-----" +nodeValue);
                    return null;
                }

                //太小的元素 或webview中 屏幕没有显示出来的元素排队在外
                //TODO:do it for ios, 太小的元素 或webview中 屏幕没有显示出来的元素排队在外
                int tolerance = 5;
                if(Math.abs(endX - startX) < tolerance || Math.abs(endY - startY) < tolerance){
                    log.info("Removed due to exceed tolerance : " + tolerance + " StartX: " + String.valueOf(startX) + " StartY: "  + String.valueOf(startY) + " EndX: " + String.valueOf(endX) + "EndY: " + String.valueOf(endY));
                    return null;
                }
            }

            nodeXpath.append("@");
            nodeXpath.append(nodeName);
            nodeXpath.append("=\"");
            nodeXpath.append(nodeValue);
            nodeXpath.append("\"");

            if (length > 0) {
                nodeXpath.append(" and ");
            }
        }

        nodeXpath.append("]");

        //log.info(nodeXpath.toString());
        return nodeXpath.toString().replace(" and ]","]");
    }

    public static String clickTabBarElement(String xml, String expression) throws Exception{
        log.info("Method clickTabBarElement");

        String newXml = null;

        if(expression == null){
            return newXml;
        }

        log.info("Tab bar xpath expression: " + expression);

        Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        NodeList nodes = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);

        log.info(String.valueOf("TabBar nodes length : " + nodes.getLength()));

        int length = nodes.getLength();

        if(length == 0) {
            log.info("No TabBar elements found!");
            return newXml;
        }

        while(length > 0 ){
            length --;
            Node tmpNode = nodes.item(length);
            String nodeXpath = getNodeXpath(tmpNode);

            if(nodeXpath==null){
                continue;
            }

            //判断当前元素是否点击过
            if(set.add(nodeXpath)){
                MobileElement elem = Driver.findElementWithoutException(By.xpath(nodeXpath));

                if(null == elem){
                    log.info("---------TabBar Element Elem not found!!!!!!! Stop current iteration.-----------" );
                    break;
                }

                log.info("------------------------CLICK TabBar Element-------------------------- \n" + nodeXpath);
                newXml = clickElement(elem,xml);

                //TODO: Remove this
                PictureUtil.takeAndModifyScreenShot(400,400,600,"click-tabbar");
                break;
            }
        }

        if(length == 0){
            log.info("All the tab bar elements are clicked!");
        }

        return newXml;
    }

    public static String showTabBarElement(String xml, String expression) throws Exception{
        log.info("Method showTabBarElement");

        String newXml = null;

        if(expression == null){
            return newXml;
        }

        log.info("Tab bar xpath expression: " + expression);

        Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        NodeList nodes = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);

        log.info(String.valueOf("tab bar nodes length : " + nodes.getLength()));

        int length = nodes.getLength();

        if(length == 0){
            log.info("No tab bar elements found!");
            return newXml;
        }

        while(length > 0 ) {
            length--;
            Node tmpNode = nodes.item(length);
            String nodeXpath = getNodeXpath(tmpNode);

            if (nodeXpath == null) {
                continue;
            }

            log.info("Tab " + nodeXpath);
        }

        return newXml;
    }

    public static long getClickCount() {
        log.info("Method: getClickCount");
        return clickCount;
    }

    private static ArrayList<String> initEventMap(){
        log.info("Method: initEventMap");

        ArrayList<String> array = new ArrayList<>();

        for(String event : monkeyEventRatioMap.keySet()){
            array.add(event);
            monkeyEventSummaryRatioMap.put(event,0L);
        }

        log.info(array.toString());
        return array;
    }



    public static void monkey(String pageSource){
        log.info("Method: monkey");

        userLogin(pageSource);
        initMonkey();

        boolean isLandscape = Driver.isLandscape();

        int GAP_X = 80;
        int GAP_Y = 80;
        int x,y;
        int index;

        if(!Util.isAndroid()){
            GAP_X = 50;
            GAP_Y = 50;
        }

        //避免点击顶部状态条
        int actualWidth = deviceWidth - GAP_X;
        int actualHeight = deviceHeight - GAP_Y;
        log.info("Actual width : "+deviceWidth*scale + " Actual height : " + deviceHeight*scale);

        int centerX = deviceWidth/2;
        int centerY = deviceHeight/2;

        if(isLandscape){
            centerX = deviceHeight/2;
            centerY = deviceWidth/2;
        }

        longPressPointList.add(new Point(centerX, centerY));
        log.info("Center X " + centerX + " Center Y " + centerY);

        Driver.sleep(SLEEP_TIME);

        ArrayList<String> ratioList = initEventMap();

        if(ratioList.size()==0){
            log.error("Available list size is 0");
            return;
        }

        log.info("Monkey running time is (minutes): " + runningTime);

        int specialPointSize = specialPointList.size();
        int spIndex = 0;

        int longPressPointSize = longPressPointList.size();
        int lpIndex = 0;

        int xpathListSize = xpathItemList.size();
        int xpIndex = 0;

        while (true) {
            long endTime = System.currentTimeMillis();

            if((endTime - testStartTime) > ( runningTime * 60 * 1000)) {
                log.info("已运行" + (endTime - testStartTime)/60/1000 + "分钟，任务即将结束");
                break;
            }

            log.info("Available event list : " + ratioList);
            x = Util.internalNextInt(GAP_X, actualWidth);
            y = Util.internalNextInt(GAP_Y, actualHeight);

            if(isLandscape){
                //Exchange X,Y in landscape mode
                x=x+y;
                y=x-y;
                x=x-y;
            }

            int length = ratioList.size() ;

            if(length <= 0){
                ratioList = initEventMap();
                length = ratioList.size();
                log.info("----------------------------------------------Reinitialize ration-----------------");
            }

            index = Util.internalNextInt(0, length);
            String event = ratioList.get(index);

            log.info("index is "  + index );

            Long count = monkeyEventSummaryRatioMap.get(event);

            if(count >= monkeyEventRatioMap.get(event)){
                log.info("---------Remove event " + event);
                ratioList.remove(index);
                continue;
            }else{
                count ++;
                monkeyEventSummaryRatioMap.put(event,count);
                log.info("\n\n================Event " + event + " count is " + count + " ====================\n");
            }

            //iOS has no concept of activity
            if(Util.isAndroid()){
                String eventType = event.replace("_RATIO","");
                String currentActivityName = Driver.getCurrentActivity();
                Map<String, Long> eventCountMap = monkeyClickedMap.get(currentActivityName);

                Long eventCount = 1L;

                if(eventCountMap == null){
                    eventCountMap = new HashMap<>();
                }else{
                    eventCount = eventCountMap.get(eventType);

                    if(eventCount != null){
                        eventCount ++;
                    }else{
                        eventCount = 1L;
                    }
                }

                log.info("Event count " + eventCount );

                eventCountMap.put(eventType,eventCount);
                monkeyClickedMap.put(currentActivityName,eventCountMap);

                log.info("Current activity : " + currentActivityName);
                log.info(monkeyClickedMap.toString());
            }

            int endX= Util.internalNextInt(GAP_X, actualWidth);
            int endY= Util.internalNextInt(GAP_Y, actualHeight);

            List<Point> pointList = new ArrayList<>();
            pointList.add(new Point(x*scale,y*scale));
            pointList.add(new Point(endX*scale,endY*scale));

            try{
                switch (event){
                    case ConfigUtil.CLICK_RATIO:
                        PictureUtil.takeAndModifyScreenShotAsyn(x * scale, y * scale);
                        Driver.clickByCoordinate(x, y);
                        break;
                    case ConfigUtil.SWIPE_RATIO:
                        Driver.takeScreenShot();

                        if(isLandscape){
                            //Exchange X,Y in landscape mode
                            endX=endX+endY;
                            endY=endX-endY;
                            endX=endX-endY;
                        }
                        Driver.swipe(x, y, endX, endY);//防止下拉后锁屏
                        break;
                    case ConfigUtil.RESTART_APP_RATIO:
                        Driver.appRelaunch();
                        break;
                    case ConfigUtil.HOME_KEY_RATIO:
                        Driver.pressHomeKey();
                        break;
                    case ConfigUtil.CLICK_SPECIAL_POINT_RATIO:
                        log.info("Special point list size : " + specialPointSize);
                        log.info("Special point list  : " + specialPointList);
                        log.info("spIndex  is " + spIndex);

                        spIndex = spIndex % specialPointSize;
                        //int random = Util.internalNextInt(0,size);
                        Point point = specialPointList.get(spIndex++);

                        PictureUtil.takeAndModifyScreenShotAsyn(x * scale, y * scale);
                        Driver.clickByCoordinate(point.x, point.y);
                        break;
                    case ConfigUtil.LONG_PRESS_RATIO:
                        //random = Util.internalNextInt(0,size);
                        lpIndex = lpIndex % longPressPointSize;
                        point = longPressPointList.get(lpIndex++);

                        PictureUtil.takeAndModifyScreenShotAsyn(x * scale, y * scale);
                        Driver.LongPressCoordinate(point.x, point.y);
                        break;
                    case ConfigUtil.DOUBLE_TAP_RATIO:
                        PictureUtil.takeAndModifyScreenShotAsyn(x * scale, y * scale);
                        Driver.doubleClickByCoordinate(x, y);
                        break;
                    case ConfigUtil.PINCH_RATIO:
                        PictureUtil.takeAndModifyScreenShotAsyn(pointList,"pinch");
                        Driver.pinch(x,y,endX,endY,false);
                        break;
                    case ConfigUtil.UNPINCH_RATIO:
                        PictureUtil.takeAndModifyScreenShotAsyn(pointList,"unpinch");
                        Driver.pinch(x,y,endX,endY,true);
                        break;
                    case ConfigUtil.DRAG_RATIO:
                        PictureUtil.takeAndModifyScreenShotAsyn(pointList,"drag");
                        Driver.drag(x,y,endX,endY);
                        break;
                    case ConfigUtil.BACK_KEY_RATIO:
                        Driver.pressBack();
                    case ConfigUtil.CLICK_ITEM_BY_XPATH_RATIO:
                        if(xpathListSize == 0){
                            log.error("xpath list is 0");
                            break;
                        }

                        log.info("Xpath item list size : " + specialPointSize);
                        log.info("Xpath item list  : " + specialPointList);
                        log.info("xpathIndex is " + xpIndex++);

                        xpIndex = xpIndex % xpathListSize;

                        MobileElement elem = Driver.findElementWithoutException(By.xpath(xpathItemList.get(xpIndex)));
                        if(elem == null){
                            log.error("!!!Element is not found by xpath : " + xpathItemList.get(xpIndex));
                        }else{
                            log.info("Element is found by xpath : " + xpathItemList.get(xpIndex));
                            PictureUtil.takeAndModifyScreenShotAsyn(elem.getCenter().getX() * scale, elem.getCenter().getY() * scale);
                        }

                        break;
                }

                log.info(monkeyEventRatioMap.toString());
                log.info(monkeyEventSummaryRatioMap.toString());

                String xml = Driver.getPageSource(0);
                String packageName=getAppName(xml);

                if (PackageStatus.VALID != isValidPackageName(packageName, ignoreCrash)) {
                    if (!ignoreCrash) {
                        break;
                    }
                }
                if (packageName.equals("com.tencent.mm") || packageName.equals("com.tencent.xin")){
                    if(xml.contains("附近的小程序")){
                        log.info("跳转到了小程序主页面，重启app");
                        Driver.appRelaunch();
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                log.error("Unexpected error found, continue testing");
            }
        }
    }

    public static StringBuilder getRepoStep(){
        return repoStep;
    }

    public static String showNodes(String xml, String oldNodePath) throws Exception{
        Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        NodeList nodes = (NodeList) xpath.evaluate(clickXpath, document, XPathConstants.NODESET);

        int length = nodes.getLength();

        log.info(String.valueOf("UI nodes length : " + length));
        String temp = oldNodePath;


        while(--length >=0){
            Node node = nodes.item(length);
            String xpath = getNodeXpath(node);
            if(null != xpath) {
                log.info("getNodeXpath: " + xpath +"\n");
            }
        }

        log.info("!!!!! " + temp);
        return temp;
    }

    ///////////////////////////////////////////

    /**
     * 深度优先遍历算法
     * @param node
     */
    public void dfs(String node){
        if(null==node){
            log.error("node 为空");
        }

        //检查运行时间
        long endTime = System.currentTimeMillis();

        if((endTime - testStartTime) > ( runningTime * 60 * 1000)) {
            log.info("已运行" + (endTime - testStartTime)/60/1000 + "分钟，任务即将结束");
            stop = true;
            return ;
        }

        String clickPath = traverse_route.get(node).getAsString();
        log.info("the element path will click is : " + clickPath);
        String[] clickStep = clickPath.split("--->");

        if(clickStep.length>maxDepth){
            log.info("停止，已经到达的层级为 " + clickPath);
            findNewNode  = false;
        }else{
            findNewNode  = true;
        }

        if(findNewNode){
            //node 点击这个node com.xsj.activityname###xpath
            String[] pageNameAndXpath = node.split("###");
            String pageName = pageNameAndXpath[0];

            String xpath = pageNameAndXpath[1];

           // MobileElement elem = Driver.findElementWithoutException(By.xpath(xpath));
            MobileElement elem = Driver.findElementByXPath(xpath);
            //记录遍历过的路径
            traverse_track_record.add(clickPath);
            nodeVisited.add(node);
            if(null == elem){
                //继续遍历
                log.info("---------Node not found in current UI!!!!!!! Stop current iteration.-----------" );
                List<String> nodesKey = getNodeData(currentPageName);
                Iterator<String> iterator = nodesKey.iterator();
                while (iterator.hasNext()) {
                    String nodeStr = iterator.next();
                    if (!nodeVisited.contains(nodeStr)) {
                        dfs(nodeStr);
                        return;
                    }
                }

                //如果当前页面没有了，就看看是否可以滚动
                Rect rect = getPageScrollFlg(currentPageName);
                if(rect==null){//不可以滚动
                    doBackDFS(node);
                }else{//可以滚动
                    if(pageScrollTime.containsKey(currentPageName)){
                        int count = pageScrollTime.get(currentPageName);
                        if(count>=3){
                            removeScrollFlag(currentPageName);
                            //当前页面没有了，就返回继续迭代上一个页面的元素
                            doBackDFS(node);
                            return;
                        }else{
                            pageScrollTime.put(currentPageName,count+1);
                        }
                    }else{
                        pageScrollTime.put(currentPageName,1);
                    }
                    decreaseGrayActivityCount(currentPageName);
                    scrollToBottom(rect);


                    last_page_source = current_page_source;
                    current_page_source =  Driver.getPageSource();
                    if(last_page_source.equals(current_page_source)){
                        removeScrollFlag(currentPageName);
                        //当前页面没有了，就返回继续迭代上一个页面的元素
                        doBackDFS(node);
                    }else{// 页面不一样
                        newPageDeal(pageName,node);
                    }
                }


            }else{
                last_page_source  = current_page_source;
                current_page_source = clickElement(elem,current_page_source);
                // 页面没有变化，currentPageName 没有变化
                if(current_page_source.equals(last_page_source)){
                    //说明点击无效 继续遍历
                    List<String> nodesKey = getNodeData(currentPageName);
                    Iterator<String> iterator = nodesKey.iterator();
                    while (iterator.hasNext()){
                        String nodeStr = iterator.next();
                        if(!nodeVisited.contains(nodeStr)){
                            dfs(nodeStr);
                            return;
                        }
                    }

                    //如果当前页面没有了，就看看是否可以滚动
                    Rect rect = getPageScrollFlg(currentPageName);
                    if(rect==null){//不可以滚动
                        doBackDFS(node);
                    }else{//可以滚动
                        if(pageScrollTime.containsKey(currentPageName)){
                            int count = pageScrollTime.get(currentPageName);
                            if(count>=3){
                                removeScrollFlag(currentPageName);
                                //当前页面没有了，就返回继续迭代上一个页面的元素
                                doBackDFS(node);
                                return;
                            }else{
                                pageScrollTime.put(currentPageName,count+1);
                            }
                        }else{
                            pageScrollTime.put(currentPageName,1);
                        }
                        decreaseGrayActivityCount(currentPageName);
                        scrollToBottom(rect);
                        last_page_source = current_page_source;
                        current_page_source =  Driver.getPageSource();
                        if(last_page_source.equals(current_page_source)){
                            removeScrollFlag(currentPageName);
                            //当前页面没有了，就返回继续迭代上一个页面的元素
                            doBackDFS(node);
                        }else{// 页面不一样
                            newPageDeal(pageName,node);
                        }
                    }
                }else{
                    //新的页面
                    newPageDeal(pageName,node);
                }
            }
        }
    }


    private void newPageDeal(String pageName,String node){
        //新的页面
        new_nodes = findAllClickables(pageName);

        if(new_nodes!=null && new_nodes.size()>0){

            for(String newNode : new_nodes){
                // #记录其轨迹类似于1--->2--->3
                addTraverseRoute(newNode,getTraverseRoute(node)+"--->"+newNode);
                addAlreadyFindNode(newNode);

                addEdge(node,newNode);
            }
            //记录完后进行遍历
            addNodesData(getCurrentPageName(),new_nodes);
            dfs(new_nodes.get(0));
        }else{
            //当前页面没有可点击的元素继续遍历
            Rect rect = getPageScrollFlg(currentPageName);
            if(rect==null){//不可以滚动
                doBackDFS(node);
            }else{//可以滚动
                if(pageScrollTime.containsKey(currentPageName)){
                    int count = pageScrollTime.get(currentPageName);
                    if(count>=3){
                        removeScrollFlag(currentPageName);
                        //当前页面没有了，就返回继续迭代上一个页面的元素
                        doBackDFS(node);
                        return;
                    }else{
                        pageScrollTime.put(currentPageName,count+1);
                    }
                }else{
                    pageScrollTime.put(currentPageName,1);
                }
                decreaseGrayActivityCount(currentPageName);
                scrollToBottom(rect);
                last_page_source = current_page_source;
                current_page_source =  Driver.getPageSource();
                if(last_page_source.equals(current_page_source)){
                    removeScrollFlag(currentPageName);
                    //当前页面没有了，就返回继续迭代上一个页面的元素
                    doBackDFS(node);
                }else{// 页面不一样
                    newPageDeal(pageName,node);
                }
            }
        }
    }

    private void doBackDFS(String node){

        Driver.pressBack();
        Driver.sleep(BACK_SLEEP_TIME);
        lastPageName = currentPageName;
        currentPageName = Driver.getCurrentActivity();
        last_page_source = current_page_source;
        current_page_source =  Driver.getPageSource();
        List<String> nodesKey = getNodeData(currentPageName);
        Iterator<String> iterator = nodesKey.iterator();
        while (iterator.hasNext()){
            String nodeStr = iterator.next();
            if(!nodeVisited.contains(nodeStr)){
                dfs(nodeStr);
                return;
            }
        }
        Rect rect = getPageScrollFlg(currentPageName);
        if(rect==null){//不可以滚动
            doBackDFS(node);
        }else{//可以滚动
            scrollToBottom(rect);
            last_page_source = current_page_source;
            current_page_source =  Driver.getPageSource();
            if(last_page_source.equals(current_page_source)){
                removeScrollFlag(currentPageName);
                //当前页面没有了，就返回继续迭代上一个页面的元素
                doBackDFS(node);
            }else{// 页面不一样
                newPageDeal(lastPageName,node);
            }
        }
        //如果回退到root节点了，而且都遍历完了，那么退出
        if(currentPageName.equals(fisrtPageName)){
            return;
        }else{//如果没有到root节点则继续回退，继续遍历
            doBackDFS(node);
        }
    }

    public void setFirstPageName(){
        fisrtPageName = Driver.getCurrentActivity();

        Driver.setViewPortScreenSize(Driver.getDeviceWidth(),Driver.getDeviceHeight()-200);
    }

    /**
     * 找出该页面的所有可用节点 com.arong.activityname###.//[@]
     *
     *
     * @param activityNameBeforeClick 跟节点传 ""
     * @return
     */
    public List<String> findAllClickables(String activityNameBeforeClick){
        String pageContent = Driver.getPageSource();
        current_page_source = pageContent;
        String packageName=getAppName(pageContent);
        String activityIdentification = Driver.getCurrentActivity();
        lastPageName = currentPageName;
        currentPageName = activityIdentification;
        List<String> node_on_activity = new ArrayList<>();
        List<String> node_on_activity_temp = new ArrayList<>();

        /////////////////////////////////1、页面级过滤
        if(pageContent==null){
            log.info(activityIdentification+"当前页面内容获取为空");
            return null;
        }
        //进入到其他APP的页面了
        if (PackageStatus.VALID != isValidPackageName(packageName, ignoreCrash)) {
            log.info("进入到了"+packageName+"应用");
            int deepth = 1;
            //如果深度>0的话就返回
            if(deepth>0){
                log.info("进入到了"+packageName+"应用"+activityIdentification+"--->返回");
               // Driver.pressBack();
                Driver.takeScreenShot();
                String processName = appName;
                //程序未crash时 说明只是跳出了当前app 把isValid设成true,返回
                if(Util.isProcessExist(ConfigUtil.getUdid(),processName)){
                    Driver.takesScreenShotAndPressBack(repoStep);
                    Driver.sleep(BACK_SLEEP_TIME);
                    lastPageName = currentPageName;
                    currentPageName = Driver.getCurrentActivity();
                    last_page_source = current_page_source;
                    current_page_source =  Driver.getPageSource();
                }else{
                    //程序crash了，如果ignoreCrash为true,则重启app继续遍历
                    stop = !ignoreCrash;
                    Util.renameAndCopyCrashFile(pic);
                    Driver.takeScreenShot();
                    log.error("===================app crashed!!!");
                }
                return  null;
            }else{
                log.info("进入到了"+packageName+"应用"+activityIdentification+"--->退出");
                //退出
                stop = true;
            }
        }else if(judgeBlackActivity(pageContent,activityIdentification)){
            log.info(activityIdentification+"在黑名单中"+"--->返回");
            //Driver.pressBack();
            return  null;
        }else if(judgeGrayActivity(activityNameBeforeClick,activityIdentification)){
            log.info(activityIdentification+"在灰名单中"+"--->返回");
            return  null;
        }

        //根据关键字触发Back Key
        if( (backKeyTriggerList != null)  && (backKeyTriggerList.size() > 0) ){
            for(String key : backKeyTriggerList){
                if (pageContent.contains(key) && !"".equals(activityNameBeforeClick)){
                    Driver.takeScreenShot();
                    Driver.sleep(BACK_SLEEP_TIME);
                    //cky 上传当前黄金页面
                    return null;
                }
            }
        }

        current_page_source = pageContent;
        if(current_page_source.equals(last_page_source)){
            log.info(activityIdentification+"页面没有发生变化");
            return null;
        }

        /////////////////////////////////2、元素级过滤
        try {
            node_on_activity_temp = getNodesFromFile(pageContent,activityIdentification);
            if(node_on_activity_temp==null){
               node_on_activity =  node_on_activity_temp;
            }else{
                //过滤黄金page的黄金item
                if(ConfigUtil.getGoldConfigPageAndItsItems()!=null  &&  ConfigUtil.getGoldConfigPageAndItsItems().size()>0){
                    for(ConfigUtil.GoldPage goldPage:ConfigUtil.getGoldConfigPageAndItsItems()){
                        if(current_page_source.contains(goldPage.pageName)){
                            for(String nodeStr:node_on_activity_temp){
                                if(goldPage.goldItems!=null){
                                    for(String goldItem : goldPage.goldItems){
                                        if(nodeStr.contains(goldItem)){
                                            node_on_activity.add(nodeStr);
                                        }
                                    }
                                }else{
                                    node_on_activity =  node_on_activity_temp;
                                }
                            }
                        }else{
                            node_on_activity =  node_on_activity_temp;
                        }
                    }
                }else{
                    node_on_activity =  node_on_activity_temp;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        //如果有可点击的元素并且可点击元素的个数 超过10个，就去判断该页面是否可滑动
        try{
            if(node_on_activity!=null && node_on_activity.size()>2){
                Document document = builder.parse(new ByteArrayInputStream(pageContent.getBytes()));
                NodeList nodes = (NodeList) xpath.evaluate(ConfigUtil.getStringValue(ConfigUtil.CAN_SCROLL_PAGE_XPATH), document, XPathConstants.NODESET);
                int length = nodes.getLength();
                if(length>0){//第一个滚动的元素就是最外层的滚动，我们只关心最外层滚动
                    Node scrollNode = nodes.item(0);
                    NamedNodeMap attributes = scrollNode.getAttributes();
                    int lengthAttr = attributes.getLength();
                    for(int i=0;i<lengthAttr;i++){
                        Node tmpNode = attributes.item(i);
                        String nodeName = tmpNode.getNodeName();
                        String nodeValue = tmpNode.getNodeValue();
                        if(nodeName.contains("bounds")){
                            String value = nodeValue;
                            value =value.replace("][",",");//"[1080,468,2160,885]";

                            int index = value.indexOf(",");
                            int startX = Integer.valueOf(value.substring(1,index));

                            int indexNext = value.indexOf(",",index+1);
                            int startY = Integer.valueOf(value.substring(index+1,indexNext));

                            index = value.indexOf(",",indexNext + 1);
                            int endX = Integer.valueOf(value.substring(indexNext +1,index));
                            int endY = Integer.valueOf(value.substring(index +1,value.length()-1));

                            if(endY<startY){
                                int tmp = startY;
                                startY = endY;
                                endY = tmp;
                            }
                            Rect scrollRect = new Rect(startX,startY,endX,endY);
                            setPageScrollFlag(currentPageName,scrollRect);
                            break;
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return  node_on_activity;
    }


    /**
     * 获取当前页面的所有合法节点Node
     * @param xml
     * @return
     * @throws Exception
     */
    public static List<String> getNodesFromFile(String xml,String pageName) throws Exception {
        ArrayList<String> nodeXpaths = new ArrayList<>();
        NodeList nodes = null;
        String activityIdentification = Driver.getCurrentActivity();
        log.info("Method: getNodesFromFile");

        log.info("Context: " + Driver.driver.getContextHandles().toString());

        //检查运行时间
        long endTime = System.currentTimeMillis();

        if ((endTime - testStartTime) > (runningTime * 60 * 1000)) {
            log.info("已运行" + (endTime - testStartTime) / 60 / 1000 + "分钟，任务即将结束");
            stop = true;
            return null;
        }

        Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        //ANDROID_CLICK_XPATH_HEADER 根据' ( string-length(@text)<10  or contains(@text,"允许") ) ' 配置来获取所有合法的elemtns
        nodes = (NodeList) xpath.evaluate(clickXpath, document, XPathConstants.NODESET);

        log.info(String.valueOf("UI nodes length : " + nodes.getLength()));

        int length = nodes.getLength();

        if (length == 0) {
            log.error("当前" + activityIdentification + "没有可点击的元素");
            return null;
        }
        String currentXML = xml;

        //检查stop为true时快速退出
        if (stop) {
            log.info("-----stop=true, Fast exit");
            return null;
        }

        log.info("Back key list size is " + backKeyTriggerList.size());

        //根据关键字触发Back Key
        if ((backKeyTriggerList != null) && (backKeyTriggerList.size() > 0)) {
            for (String key : backKeyTriggerList) {
                if (currentXML.contains(key)) {
                    log.info("Back key trigger : " + key + " is found, press back key");
                    Driver.takeScreenShot();
                    Driver.pressBack(repoStep);
                    currentXML = Driver.getPageSource();
                    //cky 此处是不是应该记录下来？ 截图或者源xml
                    return null;
                }
            }
        }

        //遍历前检查 如果不是小程序的话不用关心
        // 1.包名是否合法，包名不合法，不会重启
        // 2.检查Depth,超过预定值就返回
        // 3.是否有可遍历的元素,如果没有遍历TabBar,如果无TabBar
        String packageName = getAppName(currentXML);

        if (packageName.equals("com.tencent.mm") || packageName.equals("com.tencent.xin")) {
            if (currentXML.contains("附近的小程序")) {
                log.info("已遍历完小程序，跳转到了小程序主页面，遍历停止");
                stop = true;
                //cky 此处是不是应该记录下来？ 截图或者源xml
                return null;
            }
        }
        //////////////////////////////

        log.info("++++++++++++++++++ Activity Name : " + Driver.getCurrentActivity() + "+++++++++++++++++++++++");

        //1.检查当前UI的包名是否正确，一定要先查包名！因为其内部控制了stop的值, 包名不合法，-是否应该重启app?--
        if (PackageStatus.VALID != isValidPackageName(packageName, true)) {
            log.info("=====================package: " + packageName + " is invalid, return ....==============================");
            currentXML = Driver.getPageSource();
            return null;
        }

        //处理黑名单xpath
        nodeXpathBlackSet = getBlackNodeXpathSet(document);
        int blackNodeXpathSize = nodeXpathBlackSet.size();
        log.info("black node Xpath size is " + blackNodeXpathSize);

        //遍历UI内的Node元素
        while (--length >= 0 && !stop) {
            log.info("Element index is : " + length);

            Node tmpNode = nodes.item(length);
            String nodeXpath = getNodeXpath(tmpNode);

            if (nodeXpath == null) {
                log.error("Null nodeXpath , continue.");
                continue;
            }

            if (blackNodeXpathSize != 0) {
                if (nodeXpathBlackSet.contains(nodeXpath)) {
                    log.info("Ignore black xpath item : " + nodeXpath);
                    continue;
                }
            }

            log.info("========================================Complete iterating current UI with following elements: ");
            //log.info( "\n\n\n" + previousPageStructure + "\n\n\n");
            //log.info("depth before return is " + currentDepth);
            nodeXpaths.add(pageName+"###"+nodeXpath);
        }
        return nodeXpaths;
    }


    /**
     * 添加节点 只在第一个页面的时候添加
     * @param nodeList
     */
    public void add_nodes(List<String> nodeList){
        if(nodeList==null){
            log.error("nodeList 为空");
            return;
        }

        if(node_neighbors==null){
            log.error("node_neighbors 为空");
            return;
        }

        for(String nodeXpath:nodeList){
            if(!nodes().contains(nodeXpath)){
                node_neighbors.put(nodeXpath,new LinkedHashMap<String,LinkedHashMap>());
            }
        }
    }

    /**
     * 节点集合
     * @return
     */
    public Set<String> nodes(){
        if(node_neighbors!=null){
            return node_neighbors.keySet();
        }
        return null;
    }

    /**
     * 判断该页面是否在黑名单中
     * @param pageContent
     * @param pageActivity
     * @return
     */
    public boolean judgeBlackActivity(String pageContent,String pageActivity){
        if(null == pageContent && pageActivity == pageActivity){
            return false;
        }
        if(blackPages==null || blackPages.size()==0){
            return false;
        }

        for(String oneBlackPage:blackPages){
            if(pageActivity.toLowerCase().contains(oneBlackPage.toLowerCase())){
                return true;
            }
        }


        for(String oneBlackPage:blackPages){
            if(pageContent.toLowerCase().contains(oneBlackPage.toLowerCase())){
                return true;
            }
        }

        return  false;
    }

    /**
     * 判断是正在迭代中的页面吗
     * @param activityBeforeClick 之前的activityName
     * @param activityIdentification 当前的acitivityName
     * @return
     */
    public boolean judgeGrayActivity(String activityBeforeClick,String activityIdentification){
        if(activityBeforeClick==null && activityIdentification == null){
            log.error("judgeGrayActivity方法传入的参数有误");
            return false;
        }
        //如果灰名单activity在点击之前与点击之后相同,应该是在同一页面点击的结果
        if(!activityBeforeClick.equals(activityIdentification)){
            if(!gray_activity_record.has(activityIdentification)){
                gray_activity_record.addProperty(activityIdentification,1);
            }else{
                gray_activity_record.addProperty(activityIdentification,gray_activity_record.get(activityIdentification).getAsInt()+1);
            }
            int browseCount = gray_activity_record.get(activityIdentification).getAsInt();
            if(browseCount > gray_activity_max_traverse){
                log.warn(String.format("当前灰名单activity:%s为已经寻找过%s次跳过寻找新的node...",activityIdentification, gray_activity_max_traverse));
                return true;
            }
        }else{
            //如果灰名单activity在点击之前与点击之后相同,应该是在同一页面点击的结果
            if(!gray_activity_record_itslef.has(activityIdentification)){
                gray_activity_record_itslef.addProperty(activityIdentification,1);
            }else{
                gray_activity_record_itslef.addProperty(activityIdentification,gray_activity_record_itslef.get(activityIdentification).getAsInt()+1);
            }
            int browseSelfCount = gray_activity_record_itslef.get(activityIdentification).getAsInt();
            if(browseSelfCount > gray_activity_max_traverse_itself){
                log.warn(String.format("当前灰名单activity:%s在自身activity已经寻找过%s次跳过寻找新的node...",activityIdentification, gray_activity_max_traverse_itself));
                return true;
            }
        }

        return  false;
    }


    /**
     * 添加 遍历数据结构
     * @param rootNode
     * @param newNode
     */
    public void addEdge(String rootNode,String newNode){
        LinkedHashMap<String,LinkedHashMap> neighbors = node_neighbors.get(rootNode);
        if(neighbors!=null){
            if(!neighbors.containsKey(newNode)){
                neighbors.put(newNode,new LinkedHashMap());
            }
        }
    }


    /**
     * 记录所有的页面名称对应的pageNode
     * @param activityName
     * @param pageNodes
     */
    public void addNodesData(String activityName,List<String> pageNodes){
        if(activityName==null){
            return ;
        }
        nodesData.put(activityName,pageNodes);
    }

    public List<String> getNodeData(String activityName){
        if(activityName == null){
            return  null;
        }
        return  nodesData.get(activityName);
    }
    ///////////////

    public String getCurrentPageName(){
        return currentPageName;
    }


    public void addTraverseRoute(String node,String nodePath){
        traverse_route.addProperty(node,nodePath);
    }

    public String getTraverseRoute(String nodeStr){
        if(nodeStr==null){
            return nodeStr+"无记录";
        }
        JsonElement router =  traverse_route.get(nodeStr);
        if(router!=null){
            return router.getAsString();
        }else{
            return nodeStr+"无记录";
        }
    }

    public void addAlreadyFindNode(String nodeStr){
        already_find_node.add(nodeStr);
    }


    public void decreaseGrayActivityCount(String activityIdentification){
        if(gray_activity_record.has(activityIdentification)){
            int browseCount = gray_activity_record.get(activityIdentification).getAsInt();
            if(browseCount > gray_activity_max_traverse){
                log.warn(String.format("当前灰名单activity:%s为已经寻找过%s次跳过寻找新的node...",activityIdentification, gray_activity_max_traverse));
                gray_activity_record.addProperty(activityIdentification,browseCount-1);
            }
        }
    }


    /**
     * 判断是否滑动到底部了
     * @return
     */
    public void scrollToBottom(Rect rect){
        //Driver.scrollUp(rect);
    }

    /**
     * 保存改页面是否可以滚动
     * @param pageName
     * @param rect
     */
    public void setPageScrollFlag(String pageName ,Rect rect){
        if(this.pageScrollFlagCache==null){
            this.pageScrollFlagCache = new HashMap<>();
        }
        this.pageScrollFlagCache.put(pageName,rect);
    }

    /**
     * 判断页面是否能够滑动
     * @return
     */
    public Rect getPageScrollFlg(String pageName){
        return this.pageScrollFlagCache.get(pageName);
    }

    public void removeScrollFlag(String pageName){
        this.pageScrollFlagCache.remove(pageName);
    }

    public static class Rect{
        public int startX;
        public int startY;
        public int endX;
        public int endY;
        public Rect(int startX,int startY,int endX,int endY){
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }
    }
}




