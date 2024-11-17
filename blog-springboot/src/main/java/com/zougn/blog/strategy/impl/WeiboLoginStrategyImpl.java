package com.zougn.blog.strategy.impl;

import com.alibaba.fastjson.JSON;
import com.zougn.blog.config.WeiboConfigProperties;
import com.zougn.blog.dao.UserAuthDao;
import com.zougn.blog.dao.UserInfoDao;
import com.zougn.blog.dao.UserRoleDao;
import com.zougn.blog.dto.SocialTokenDTO;
import com.zougn.blog.dto.SocialUserInfoDTO;
import com.zougn.blog.dto.WeiboTokenDTO;
import com.zougn.blog.dto.WeiboUserInfoDTO;
import com.zougn.blog.enums.LoginTypeEnum;
import com.zougn.blog.exception.BizException;
import com.zougn.blog.service.impl.UserDetailsServiceImpl;
import com.zougn.blog.vo.WeiboLoginVO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.zougn.blog.constant.SocialLoginConst.*;
import static com.zougn.blog.enums.StatusCodeEnum.WEIBO_LOGIN_ERROR;

/**
 * 微博登录策略实现
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Service("weiboLoginStrategyImpl")
public class WeiboLoginStrategyImpl extends AbstractSocialLoginStrategyImpl {
    private final WeiboConfigProperties weiboConfigProperties;
    private final RestTemplate restTemplate;


    public WeiboLoginStrategyImpl(UserAuthDao userAuthDao, UserInfoDao userInfoDao, UserRoleDao userRoleDao, UserDetailsServiceImpl userDetailsService, HttpServletRequest request, WeiboConfigProperties weiboConfigProperties, RestTemplate restTemplate) {
        super(userAuthDao, userInfoDao, userRoleDao, userDetailsService, request);
        this.weiboConfigProperties = weiboConfigProperties;
        this.restTemplate = restTemplate;
    }

    @Override
    public SocialTokenDTO getSocialToken(String data) {
        WeiboLoginVO weiBoLoginVO = JSON.parseObject(data, WeiboLoginVO.class);
        // 获取微博token信息
        WeiboTokenDTO weiboToken = getWeiboToken(weiBoLoginVO);
        if (null == weiboToken) {
            throw new BizException(WEIBO_LOGIN_ERROR);
        }
        // 返回token信息
        return SocialTokenDTO.builder()
                .openId(weiboToken.getUid())
                .accessToken(weiboToken.getAccess_token())
                .loginType(LoginTypeEnum.WEIBO.getType())
                .build();
    }

    @Override
    public SocialUserInfoDTO getSocialUserInfo(SocialTokenDTO socialTokenDTO) {
        // 定义请求参数
        Map<String, String> data = new HashMap<>(2);
        data.put(UID, socialTokenDTO.getOpenId());
        data.put(ACCESS_TOKEN, socialTokenDTO.getAccessToken());
        // 获取微博用户信息
        WeiboUserInfoDTO weiboUserInfoDTO = restTemplate.getForObject(weiboConfigProperties.getUserInfoUrl(), WeiboUserInfoDTO.class, data);
        // 返回用户信息
        return SocialUserInfoDTO.builder()
                .nickname(Objects.requireNonNull(weiboUserInfoDTO).getScreen_name())
                .avatar(weiboUserInfoDTO.getAvatar_hd())
                .build();
    }

    /**
     * 获取微博token信息
     *
     * @param weiBoLoginVO 微博登录信息
     * @return {@link WeiboTokenDTO} 微博token
     */
    private WeiboTokenDTO getWeiboToken(WeiboLoginVO weiBoLoginVO) {
        // 根据code换取微博uid和accessToken
        MultiValueMap<String, String> weiboData = new LinkedMultiValueMap<>();
        // 定义微博token请求参数
        weiboData.add(CLIENT_ID, weiboConfigProperties.getAppId());
        weiboData.add(CLIENT_SECRET, weiboConfigProperties.getAppSecret());
        weiboData.add(GRANT_TYPE, weiboConfigProperties.getGrantType());
        weiboData.add(REDIRECT_URI, weiboConfigProperties.getRedirectUrl());
        weiboData.add(CODE, weiBoLoginVO.getCode());
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(weiboData, null);
        try {
            return restTemplate.exchange(weiboConfigProperties.getAccessTokenUrl(), HttpMethod.POST, requestEntity, WeiboTokenDTO.class).getBody();
        } catch (Exception e) {
            throw new BizException(WEIBO_LOGIN_ERROR);
        }
    }

}
