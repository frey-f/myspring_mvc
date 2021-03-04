package com.frey.demo.xcontroller;

import com.frey.demo.xservice.ITestXService;
import com.frey.xspring.annotation.XAutowired;
import com.frey.xspring.annotation.XController;
import com.frey.xspring.annotation.XRequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author wff
 * @description
 * @date 2021/3/3
 */
@XController
@XRequestMapping("/test")
public class TestController {
    @XAutowired
    ITestXService iTestXService;
    @XRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response){
        if (request.getParameter("username") == null){
            try {
                response.getWriter().write("parameter is null");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String paramName = request.getParameter("username");
            try {
                response.getWriter().write("parameter is " + paramName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("[INFO - req] new request parameter is " + paramName);
        }
    }

    /**
     * 测试方法，
     * @param request
     * @param response
     */
    public void listClassName(HttpServletRequest request, HttpServletResponse response){
        String str = iTestXService.listClassName();
        System.out.println("iTestXservice----->" + str);
        try {
            response.getWriter().write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
