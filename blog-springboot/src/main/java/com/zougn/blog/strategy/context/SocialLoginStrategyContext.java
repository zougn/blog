package com.zougn.blog.strategy.context;

import com.zougn.blog.dto.UserInfoDTO;
import com.zougn.blog.enums.LoginTypeEnum;
import com.zougn.blog.strategy.SocialLoginStrategy;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;


/**
 * 第三方登录策略上下文
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Service
@AllArgsConstructor
public class SocialLoginStrategyContext {

    private Map<String, SocialLoginStrategy> socialLoginStrategyMap;

    /**
     * 执行第三方登录策略
     *
     * @param data          数据
     * @param loginTypeEnum 登录枚举类型
     * @return {@link UserInfoDTO} 用户信息
     */
    public UserInfoDTO executeLoginStrategy(String data, LoginTypeEnum loginTypeEnum) {
        return socialLoginStrategyMap.get(loginTypeEnum.getStrategy()).login(data);
    }

}
