package com.zougn.blog.strategy.context;

import com.zougn.blog.strategy.UploadStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static com.zougn.blog.enums.UploadModeEnum.getStrategy;


/**
 * 上传策略上下文
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Service
public class UploadStrategyContext {
    /**
     * 上传模式
     */

    private String uploadMode;

    private Map<String, UploadStrategy> uploadStrategyMap;

    public UploadStrategyContext(@Value("${upload.mode}") String uploadMode, Map<String, UploadStrategy> uploadStrategyMap) {
        this.uploadMode = uploadMode;
        this.uploadStrategyMap = uploadStrategyMap;
    }

    /**
     * 上传文件
     *
     * @param file 文件
     * @param path 路径
     * @return {@link String} 文件地址
     */
    public String executeUploadStrategy(MultipartFile file, String path) {
        return uploadStrategyMap.get(getStrategy(uploadMode)).uploadFile(file, path);
    }


}
