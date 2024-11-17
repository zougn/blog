package com.zougn.blog.handler;

import com.alibaba.fastjson.JSON;
import com.zougn.blog.vo.Result;
import com.zougn.blog.constant.CommonConst;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用户未登录处理
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException {
        httpServletResponse.setContentType(CommonConst.APPLICATION_JSON);
        httpServletResponse.getWriter().write(JSON.toJSONString(Result.fail("用户未登录")));
    }

}
