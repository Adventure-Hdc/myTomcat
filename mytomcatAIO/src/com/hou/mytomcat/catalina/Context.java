package com.hou.mytomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import com.hou.mytomcat.exception.WebConfigDuplicatedException;
import com.hou.mytomcat.http.ApplicationContext;
import com.hou.mytomcat.http.StandardFilterConfig;
import com.hou.mytomcat.http.StandardServletConfig;
import com.hou.mytomcat.myClassLoader.WebappClassLoader;
import com.hou.mytomcat.util.ContextXMLUtil;
import com.hou.mytomcat.watcher.ContextFileChangeWatcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.print.Doc;
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.*;


public class Context {

    //相对路径
    private String path;
    //绝对路径
    private String docBase;
    private File contextWebXmlFile;
    //一个web应用对应一个classLoader，将应用之间隔离开
    private WebappClassLoader webappClassLoader;
    //这个就是用来存放属性的
    private ServletContext servletContext;

    private Map<String, String> url_servletClassName;
    private Map<String, String> url_servletName;
    //全限定名与类名的映射
    private Map<String, String> servletName_className;
    private Map<String, String> className_servletName;
    private List<String> loadOnStartupServletClassNames;

    private Host host;
    private boolean reloadable;
    private ContextFileChangeWatcher contextFileChangeWatcher;
    private Map<Class<?>, HttpServlet> servletPool;
    private Map<String, Map<String, String>> servlet_className_init_params;

    private Map<String, Filter> filterPool;

    private Map<String, List<String>> url_filterClassName;
    private Map<String, List<String>> url_filterNames;
    private Map<String, String> filterName_className;
    private Map<String, String> className_filterName;
    private Map<String, Map<String, String>> filter_className_init_params;

    public Context(String path, String docBase, Host host, boolean reloadable){
        this.path = path;
        this.docBase = docBase;
        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResource());
        this.url_servletClassName = new HashMap<>();
        this.url_servletName = new HashMap<>();
        this.servletName_className = new HashMap<>();
        this.className_servletName = new HashMap<>();
        this.servletContext = new ApplicationContext(this);
        this.servletPool = new HashMap<>();
        this.host = host;
        this.reloadable = reloadable;
        this.servlet_className_init_params = new HashMap<>();
        this.loadOnStartupServletClassNames = new ArrayList<>();
        this.filterPool = new HashMap<>();
        this.url_filterClassName = new HashMap<>();
        this.url_filterNames = new HashMap<>();
        this.filterName_className = new HashMap<>();
        this.className_filterName = new HashMap<>();
        this.filter_className_init_params = new HashMap<>();

        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader();
        //将通用类加载器作为应用类加载器的父加载器
        this.webappClassLoader = new WebappClassLoader(docBase, commonClassLoader);

        // TODO: 多次解析加载context xml文件了
        deploy();
    }

    //解析xml看哪些类需要做自启动
    public void parseLoadOnStartup(Document d) {
        Elements elements = d.select("load-on-startup");
        for(Element e : elements) {
            String loadOnStartupServletClassName = e.parent().select("servlet-class").text();
            loadOnStartupServletClassNames.add(loadOnStartupServletClassName);
        }
    }

    public void handleLoadOnStartup() {
        for(String loadOnStartupServletClassName : loadOnStartupServletClassNames) {
            try {
                Class<?> clazz = webappClassLoader.loadClass(loadOnStartupServletClassName);
                //实例化+初始化
                getServlet(clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //解析初始化参数
    private void parseServletInitParams(Document d) {
        Elements servletClassNameElements = d.select("servlet-class");
        for(Element servletClassNameElement : servletClassNameElements) {
            String servletClassName = servletClassNameElement.text();
            Elements initElements = servletClassNameElement.parent().select("init-param");
            if(initElements.isEmpty())
                continue;
            Map<String, String> initParams = new HashMap<>();
            for(Element element : initElements) {
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name, value);
            }
            servlet_className_init_params.put(servletClassName, initParams);
        }
    }

    /**
     *  单例设计模式 --- 放池子：
     *  根据类对象获取Servlet（后端er写的类）
     *  第一次获取时，进到if分支，在池子里添加类对象和servlet对象的映射
     *  以后再通过类对象获取时，都是从池子里通过类对象获取servlet，获取的都是同一个servlet对象
     *
     *  第二次修改：
     *  servlet对象放入池子中前先做下初始化
     */
    public synchronized HttpServlet getServlet(Class<?> clazz) throws InstantiationException, IllegalAccessException, ServletException {
        HttpServlet servlet = servletPool.get(clazz);
        //首次获取servlet
        if(servlet == null) {
            //实例化
            servlet = (HttpServlet) clazz.newInstance();
            //初始化
            ServletContext servletContext = this.getServletContext();
            String className = clazz.getName();
            String servletName = className_servletName.get(className);
            Map<String, String> initParameters = servlet_className_init_params.get(className);
            ServletConfig servletConfig = new StandardServletConfig(servletContext, servletName, initParameters);
            servlet.init(servletConfig);

            servletPool.put(clazz, servlet);
        }
        return servlet;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public WebappClassLoader getWebappClassLoader() {
        return webappClassLoader;
    }

    private void deploy() {
        TimeInterval timeInterval = DateUtil.timer();
        LogFactory.get().info("Deploying web application directory {}", this.docBase);
        init();
        LogFactory.get().info("Deploying of web application directory {} has finished in {} ms",
                this.docBase, timeInterval.intervalMs());
        if(reloadable) {
            contextFileChangeWatcher = new ContextFileChangeWatcher(this);
            contextFileChangeWatcher.start();
        }
    }

    //销毁所有servlet
    private void destroyServlets() {
        Collection<HttpServlet> servlets = servletPool.values();
        for(HttpServlet servlet : servlets) {
            servlet.destroy();
        }
    }

    public void stop() {
        webappClassLoader.stop();
        contextFileChangeWatcher.stop();
        destroyServlets();
    }

    public void reload() {
        //context本身不容易创建一个context，所以由它的父容器host创建context
        host.reload(this);
    }
    public void parseServletMapping(Document d) {
        Elements mappingUrlElements = d.select("servlet-mapping url-pattern");
        for(Element e : mappingUrlElements) {
            String urlPattern = e.text();
            String servletName = e.parent().select("servlet-name").first().text();
            url_servletName.put(urlPattern, servletName);
        }
        Elements nameClassElements = d.select("servlet servlet-name");
        for(Element e : nameClassElements) {
            String servletName = e.text();
            String servletClass = e.parent().select("servlet-class").first().text();
            servletName_className.put(servletName, servletClass);
            className_servletName.put(servletClass, servletName);
        }
        Set<String> urls = url_servletName.keySet();
        for(String url : urls) {
            String servletName = url_servletName.get(url);
            String servletClassName = servletName_className.get(servletName);
            url_servletClassName.put(url, servletClassName);
        }
    }

    public void parseFilterMapping(Document d) {
        Elements mappingUrlElements = d.select("filter-mapping url-pattern");
        for(Element e : mappingUrlElements) {
            String urlPattern = e.text();
            String filterName = e.parent().select("filter-name").first().text();

            List<String> filterNames1 = url_filterNames.get(urlPattern);
            if (null == filterNames1) {
                filterNames1 = new ArrayList<>();
                url_filterNames.put(urlPattern, filterNames1);
            }
            filterNames1.add(filterName);
        }
        Elements filterNameElements = d.select("filter filter-name");
        for(Element e : filterNameElements) {
            String filterName = e.text();
            String filterClass = e.parent().select("filter-class").first().text();
            filterName_className.put(filterName, filterClass);
            className_filterName.put(filterClass, filterName);
        }
        Set<String> urls = url_filterNames.keySet();
        for(String url : urls) {
            List<String> filterNames2 = url_filterNames.get(url);
            if(filterNames2 == null) {
                filterNames2 = new ArrayList<>();
                url_filterNames.put(url, filterNames2);
            }
            for(String filterName : filterNames2) {
                String filterClassName = filterName_className.get(filterName);
                List<String> filterClassNames = url_filterClassName.get(url);
                if(filterClassNames == null) {
                    filterClassNames = new ArrayList<>();
                    url_filterClassName.put(url, filterClassNames);
                }
                filterClassNames.add(filterClassName);
            }
        }
    }

    private void parseFilterInitParams(Document d) {
        Elements filterClassNameElements = d.select("filter-class");
        for(Element e : filterClassNameElements) {
            String filterClassName = e.text();

            Elements initElements = e.parent().select("init-param");
            if(initElements.isEmpty()) {
                continue;
            }

            Map<String, String> initParams = new HashMap<>();
            for(Element element : initElements) {
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name, value);
            }
            filter_className_init_params.put(filterClassName, initParams);
        }
    }
    private void initFilter() {
        Set<String> classNames = className_filterName.keySet();
        for(String className : classNames) {
            try {
                Class clazz = this.getWebappClassLoader().loadClass(className);
                Map<String, String> initParameters = filter_className_init_params.get(className);
                String filterName = className_filterName.get(className);
                FilterConfig filterConfig = new StandardFilterConfig(servletContext, initParameters, filterName);
                Filter filter = filterPool.get(clazz);
                if(filter == null) {
                    filter = (Filter) ReflectUtil.newInstance(clazz);
                    filter.init(filterConfig);
                    filterPool.put(className, filter);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    //检查是否有重复元素
    private void checkDuplicated(Document d, String mapping, String desc) throws com.hou.mytomcat.exception.WebConfigDuplicatedException {
        Elements es = d.select(mapping);
        Set<String> checkDuplicated = new HashSet<>();
        for(Element e : es) {
            if(checkDuplicated.contains(e.text())) {
                throw new com.hou.mytomcat.exception.WebConfigDuplicatedException(StrUtil.format(desc, e.text()));
            } else {
                checkDuplicated.add(e.text());
            }
        }
    }

    private void checkDuplicated() throws com.hou.mytomcat.exception.WebConfigDuplicatedException {
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);
        checkDuplicated(d, "servlet-mapping url-pattern", "servlet url重复, 请保持其唯一性:{}");
        checkDuplicated(d, "servlet servlet-name", "servlet名称重复, 请保持其唯一性:{}");
        checkDuplicated(d, "servlet servlet-class", "servlet类名重复, 请保持其唯一性:{}");
    }
    //初始化解析servletMapping
    private void init() {
        if(!contextWebXmlFile.exists())
            return;
        try {
            checkDuplicated();
        } catch (WebConfigDuplicatedException e) {
            e.printStackTrace();
            return;
        }
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);
        parseServletMapping(d);
        parseServletInitParams(d);
        parseLoadOnStartup(d);
        handleLoadOnStartup();
        parseFilterMapping(d);
        parseFilterInitParams(d);
        initFilter();
    }

    public String getServletClassName(String uri) {
        return url_servletClassName.get(uri);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDocBase() {
        return docBase;
    }

    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    //根据uri获取匹配的过滤器集合
    public List<Filter> getMatchedFilters(String uri) {
        List<Filter> filters = new ArrayList<>();
        Set<String> patterns = url_filterClassName.keySet();
        Set<String> matchedPatterns = new HashSet<>();
        for(String pattern : patterns) {
            if(match(pattern, uri)) {
                matchedPatterns.add(pattern);
            }
        }
        Set<String> matchedFilterClassNames = new HashSet<>();
        for(String pattern : matchedPatterns) {
            List<String> filterClassName = url_filterClassName.get(pattern);
            matchedFilterClassNames.addAll(filterClassName);
        }
        for(String filterClassName : matchedFilterClassNames) {
            Filter filter = filterPool.get(filterClassName);
            filters.add(filter);
        }
        return filters;
    }

    private boolean match(String pattern, String uri) {
        //完全匹配
        if(StrUtil.equals(pattern, uri)) {
            return true;
        }
        // 通配符匹配
        if(StrUtil.equals(pattern, "/*"))
            return true;
        return false;
    }
}
