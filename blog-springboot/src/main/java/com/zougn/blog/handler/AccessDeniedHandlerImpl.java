package com.zougn.blog.handler;

import com.alibaba.fastjson.JSON;
import com.zougn.blog.constant.CommonConst;
import com.zougn.blog.vo.Result;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用户权限处理
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException e) throws IOException {
        httpServletResponse.setContentType(CommonConst.APPLICATION_JSON);
        httpServletResponse.getWriter().write(JSON.toJSONString(Result.fail("权限不足")));
    }

}
