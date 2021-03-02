package com.frey.xspring.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
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
        doDispatch(req, res);
    }

    /**
     * 运行方法，进行拦截，匹配
     * @param req 请求
     * @param res 响应
     */
    protected void doDispatch(HttpServletRequest req, HttpServletResponse res){
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

    }
}
