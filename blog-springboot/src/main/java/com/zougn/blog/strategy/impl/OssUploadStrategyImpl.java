package com.zougn.blog.strategy.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.zougn.blog.config.OssConfigProperties;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * oss上传策略
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Service("ossUploadStrategyImpl")
@AllArgsConstructor
public class OssUploadStrategyImpl extends AbstractUploadStrategyImpl {
    private OssConfigProperties ossConfigProperties;

    @Override
    public boolean exists(String filePath) {
        return getOssClient().doesObjectExist(ossConfigProperties.getBucketName(), filePath);
    }

    @Override
    public void upload(String path, String fileName, InputStream inputStream) {
        getOssClient().putObject(ossConfigProperties.getBucketName(), path + fileName, inputStream);
    }

    @Override
    public String getFileAccessUrl(String filePath) {
        return ossConfigProperties.getUrl() + filePath;
    }

    /**
     * 获取ossClient
     *
     * @return {@link OSS} ossClient
     */
    private OSS getOssClient() {
        return new OSSClientBuilder().build(ossConfigProperties.getEndpoint(), ossConfigProperties.getAccessKeyId(), ossConfigProperties.getAccessKeySecret());
    }

}
