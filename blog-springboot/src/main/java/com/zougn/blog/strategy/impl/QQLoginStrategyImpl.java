package com.zougn.blog.strategy.impl;

import com.alibaba.fastjson.JSON;
import com.zougn.blog.config.QQConfigProperties;
import com.zougn.blog.constant.SocialLoginConst;
import com.zougn.blog.dao.UserAuthDao;
import com.zougn.blog.dao.UserInfoDao;
import com.zougn.blog.dao.UserRoleDao;
import com.zougn.blog.dto.QQTokenDTO;
import com.zougn.blog.dto.QQUserInfoDTO;
import com.zougn.blog.dto.SocialTokenDTO;
import com.zougn.blog.dto.SocialUserInfoDTO;
import com.zougn.blog.enums.LoginTypeEnum;
import com.zougn.blog.exception.BizException;
import com.zougn.blog.service.impl.UserDetailsServiceImpl;
import com.zougn.blog.util.CommonUtils;
import com.zougn.blog.vo.QQLoginVO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.zougn.blog.constant.SocialLoginConst.*;
import static com.zougn.blog.enums.StatusCodeEnum.QQ_LOGIN_ERROR;

/**
 * qq登录策略实现
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Service("qqLoginStrategyImpl")
public class QQLoginStrategyImpl extends AbstractSocialLoginStrategyImpl {
    private final QQConfigProperties qqConfigProperties;
    private final RestTemplate restTemplate;

    public QQLoginStrategyImpl(UserAuthDao userAuthDao, UserInfoDao userInfoDao, UserRoleDao userRoleDao, UserDetailsServiceImpl userDetailsService, HttpServletRequest request, QQConfigProperties qqConfigProperties, RestTemplate restTemplate) {
        super(userAuthDao, userInfoDao, userRoleDao, userDetailsService, request);
        this.qqConfigProperties = qqConfigProperties;
        this.restTemplate = restTemplate;
    }

    @Override
    public SocialTokenDTO getSocialToken(String data) {
        QQLoginVO qqLoginVO = JSON.parseObject(data, QQLoginVO.class);
        // 校验QQ token信息
        checkQQToken(qqLoginVO);
        // 返回token信息
        return SocialTokenDTO.builder()
                .openId(qqLoginVO.getOpenId())
                .accessToken(qqLoginVO.getAccessToken())
                .loginType(LoginTypeEnum.QQ.getType())
                .build();
    }

    @Override
    public SocialUserInfoDTO getSocialUserInfo(SocialTokenDTO socialTokenDTO) {
        // 定义请求参数
        Map<String, String> formData = new HashMap<>(3);
        formData.put(QQ_OPEN_ID, socialTokenDTO.getOpenId());
        formData.put(ACCESS_TOKEN, socialTokenDTO.getAccessToken());
        formData.put(OAUTH_CONSUMER_KEY, qqConfigProperties.getAppId());
        // 获取QQ返回的用户信息
        QQUserInfoDTO qqUserInfoDTO = JSON.parseObject(restTemplate.getForObject(qqConfigProperties.getUserInfoUrl(), String.class, formData), QQUserInfoDTO.class);
        // 返回用户信息
        return SocialUserInfoDTO.builder()
                .nickname(Objects.requireNonNull(qqUserInfoDTO).getNickname())
                .avatar(qqUserInfoDTO.getFigureurl_qq_1())
                .build();
    }

    /**
     * 校验qq token信息
     *
     * @param qqLoginVO qq登录信息
     */
    private void checkQQToken(QQLoginVO qqLoginVO) {
        // 根据token获取qq openId信息
        Map<String, String> qqData = new HashMap<>(1);
        qqData.put(SocialLoginConst.ACCESS_TOKEN, qqLoginVO.getAccessToken());
        try {
            String result = restTemplate.getForObject(qqConfigProperties.getCheckTokenUrl(), String.class, qqData);
            QQTokenDTO qqTokenDTO = JSON.parseObject(CommonUtils.getBracketsContent(Objects.requireNonNull(result)), QQTokenDTO.class);
            // 判断openId是否一致
            if (!qqLoginVO.getOpenId().equals(qqTokenDTO.getOpenid())) {
                throw new BizException(QQ_LOGIN_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException(QQ_LOGIN_ERROR);
        }
    }

}
