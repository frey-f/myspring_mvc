package com.frey.xspring.servlet;

import com.frey.xspring.annotation.XAutowired;
import com.frey.xspring.annotation.XController;
import com.frey.xspring.annotation.XService;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author wff
 * @description 自定义dispatchServlet 核心servlet
 * @date 2021/3/2
 */

public class XDispatchServlet extends HttpServlet {
    /**
     * 属性配置文件
     */
    private Properties contextConfig = new Properties();
    private List<String> classNameList = new ArrayList<>();
    /**
     * IOC容器
     */
    Map<String, Object> iocMap = new HashMap<>();
    //定义的map中 有一个value的属性是属于反射的方法对象
    Map<String, Method> handlerMapping = new HashMap<>();

    /**
     * 重写HttpServlet中的doGet方法
     * @param req 请求接口
     * @param res 返回接口
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res){
        //为何这里是doPost呢 是说 doGet也是走doPost方法
        this.doPost(req, res);
    }

    /**
     * 重写HttpServlet中的doPost方法
     * @param req 请求接口
     * @param res 返回接口
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res){
        //运行阶段
        try {
            doDispatch(req, res);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 运行方法，进行拦截，匹配
     * @param req 请求
     * @param res 响应
     */
    protected void doDispatch(HttpServletRequest req, HttpServletResponse res) throws InvocationTargetException, IllegalAccessException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        System.out.println("[INFO-7 request url-->" + url);
        //判断传入的url是否在web.xml配置的通配路径中
        if(!this.handlerMapping.containsKey(url)) {
            //如果不在 写入返回的错误信息
            try {
                res.getWriter().write("404 NOT FOUND");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /**
         * 这一步的作用是？
         */
        Method method = this.handlerMapping.get(url);
        System.out.println("[INFO-7] method-->" + method);
        //获取bean 的name ,并把所有的beaname的首字母都转化为小写
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        //从ioc容器的MAP中获取beanName对应的value
        System.out.println("[INFO-7] iocMap.get(beanName)-->" + iocMap.get(beanName));

        //第一个参数是获取方法，后面是参数，多个参数直接加，按顺序对应
        method.invoke(iocMap.get(beanName), req, res);
        System.out.println("[INFO-7]method.invoke put {" + iocMap.get(beanName) + "}");

    }

    /**
     * 这个初始化方法的作用是？
     * @param servletConfig
     */
    @Override
    public void init(ServletConfig servletConfig){
        //1.加载配置文件
        doLoadConfig(servletConfig.getInitParameter("contextConfigLocation"));
        //2.扫描相关的类
        doScanner(contextConfig.getProperty("scan-package"));
        //3.初始化IOC容器，将所有相关的类实例保存到IOC容器中
        try {
            doInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //4.依赖注入
        doAutowired();
        //5.初始化HandlerMapping
        initHandlerMapping();
        System.out.println("XSpring FrameWork is init.");
        //6.打印数据
        doTestPringData();
    }

    /**
     * 依赖注入
     */
    private void doAutowired() {
        if (iocMap.isEmpty()){
            return;
        }
        for (Map.Entry<String, Object> entry : iocMap.entrySet()){
            //通过反射得到class的域
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            //循环域
            for (Field field : fields){
                //如果不是使用XAutowired注解的类，就跳过
                if (!field.isAnnotationPresent(XAutowired.class)){
                    continue;
                }
                System.out.println("[INFO-4] Existence XAutowired.");
                //获取注解对应的类
                XAutowired xAutowired = field.getAnnotation(XAutowired.class);
                String beanName = xAutowired.value().trim();
                //获取XAutowired 注解的值
                if ("".equals(beanName)){
                    System.out.println("[INFO] XAutowired.value{} is null");
                    beanName = field.getType().getName();
                }
                try {
                    field.set(entry.getValue(),iocMap.get(beanName));
                    System.out.println("[INFO-4] field set {" + entry.getValue() + "} - {" + iocMap.get(beanName) + "].");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 初始化IOC容器，即将所有相关的类石磊保存到IOC容器中
     */
    private void doInstance() throws Exception {
        if (classNameList.isEmpty()){
            return;
        }
        for (String className: classNameList){
            //运用反射技术 获取到类属性 ---通过类名
            try {
                Class<?> clazz = Class.forName(className);
                //Controller 类
                if (clazz.isAnnotationPresent(XController.class)){
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    try {
                        Object instance = clazz.newInstance();
                        //保存在ioc容器中
                        iocMap.put(beanName, instance);
                        System.out.println("[INFO-3{]" + beanName + "} has been saved in iocMap");

                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    //Service 类
                } else if (clazz.isAnnotationPresent(XService.class)){
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    //如果注解包含自定义名称
                    XService xService = clazz.getAnnotation(XService.class);
                    if (!"".equals(xService.value())){
                        beanName = xService.value();
                    }
                        Object instance = clazz.newInstance();
                        iocMap.put(beanName, instance);
                        System.out.println("[INFO-3]{" + beanName + "} has been saved in iocMap");
                    //找类的接口
                    for (Class<?>  i : clazz.getInterfaces()){
                        if (iocMap.containsKey(i.getName())) {
                            throw new Exception("The bean name is Exist.");
                        }
                        iocMap.put(i.getName(), instance);
                        System.out.println("[INFO -3]{" + i.getName() + "} hsa been in iocMap");
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    private String toLowerFirstCase(String className){
        char[] charArray = className.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }
    /**
     * 扫描相关的类
     * @param property 扫描的包的配置
     */
    private void doScanner(String property) {
        //读取配置获得对应的URL
        URL resourcePath = this.getClass().getClassLoader().getResource("/" + property.replaceAll("\\.", "/"));
        if(resourcePath == null){
            return;
        }
        //通过路径获取class文件
        File classPath = new File(resourcePath.getFile());
        for (File file : classPath.listFiles()){
            //判断文件是否是目录
            if (file.isDirectory()) {
                System.out.println("[INFO-2]{" + file.getName() + "}");
                //子目录递归
                doScanner(property + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")){
                    System.out.println("[IFNO-2]{" + file.getName() + "} is not a class file.");
                    continue;
                }
                String className = (property + "." + file.getName()).replace(".class","");
                //保存在内存
                classNameList.add(className);

                System.out.println("[INFO-2]{" + className + "] has been saved in classNameList");
            }
        }
    }

    /**
     * 初始化配置文件
     * @param contextConfigLocation
     */
    private void doLoadConfig(String contextConfigLocation) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        //保存在内存中
        try {
            contextConfig.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 打印数据
     */
    private void doTestPringData() {
        System.out.println("[INFO-6--data------]");
        System.out.println("contextConfig.propertyNames()---->" + contextConfig.propertyNames());
        System.out.println("[classNameList]---->");
        for (String str:classNameList){
            System.out.println(str);
        }
        System.out.println("[iocMap]-->");
        for (Map.Entry<String,Object> entry :iocMap.entrySet()){
            System.out.println(entry);
        }
        System.out.println("[handlerMapping]--->");
        for (Map.Entry<String, Method> entry : handlerMapping.entrySet()){
            System.out.println(entry);
        }

        System.out.println("[INFO-6]----done-------");
        System.out.println("----启动成功------");
        System.out.println("测试地址：http://localhost:8080/test/query?username=frey");
        System.out.println("测试地址：http://localhost:8080/test/listClassName");

    }

    /**
     * 初始化hanglerMapping
     */
    private void initHandlerMapping(){
        //ioc容器里是空的
        if(iocMap.isEmpty()){
            return;
        }
    }
}
