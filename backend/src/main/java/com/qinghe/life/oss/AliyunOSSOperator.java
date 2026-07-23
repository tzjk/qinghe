package com.qinghe.life.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.qinghe.life.config.AliyunOSSProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;

@Component
public class AliyunOSSOperator {
    private static final Logger log = LoggerFactory.getLogger(AliyunOSSOperator.class);

    private final AliyunOSSProperties properties;

    public AliyunOSSOperator(AliyunOSSProperties properties) {
        this.properties = properties;
    }

    public String upload(String objectKey, InputStream content, long contentLength, String contentType) {
        OSS client = createClient();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            metadata.setContentType(contentType);
            client.putObject(properties.getBucketName(), objectKey, content, metadata);
            return publicUrl(objectKey);
        } finally {
            client.shutdown();
        }
    }

    public void deleteObject(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return;
        }
        OSS client = createClient();
        try {
            client.deleteObject(properties.getBucketName(), objectKey);
        } finally {
            client.shutdown();
        }
    }

    /** Returns a recognised current or legacy avatar key for this bucket; never trusts arbitrary URLs. */
    public String ownAvatarKey(Long userId, String avatarUrl) {
        if (userId == null || avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return null;
        }
        try {
            URI uri = URI.create(avatarUrl);
            String expectedHost = properties.getBucketName() + "." + endpointHost();
            String path = uri.getPath();
            String key = path == null ? null : path.replaceFirst("^/", "");
            boolean legacyKey = key != null && key.matches("avatars/" + userId + "/\\d{4}/\\d{2}/[0-9a-f]{32}\\.webp");
            boolean currentKey = key != null && key.matches("qinghe-life-service/\\d{4}/\\d{2}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp");
            if (!expectedHost.equalsIgnoreCase(uri.getHost()) || !(legacyKey || currentKey)) {
                return null;
            }
            return key;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /** Returns a current shop-cover key only when it belongs to this bucket and the dedicated shops prefix. */
    public String ownShopCoverKey(String coverUrl) {
        if (coverUrl == null || coverUrl.trim().isEmpty()) {
            return null;
        }
        try {
            URI uri = URI.create(coverUrl);
            String expectedHost = properties.getBucketName() + "." + endpointHost();
            String path = uri.getPath();
            String key = path == null ? null : path.replaceFirst("^/", "");
            boolean shopKey = key != null && key.matches("qinghe-life-service/shops/\\d{4}/\\d{2}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp");
            return expectedHost.equalsIgnoreCase(uri.getHost()) && shopKey ? key : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
    public String ownGoodsImageKey(String imageUrl) { return ownPrefixKey(imageUrl, "qinghe-life-service/goods/"); }
    private String ownPrefixKey(String imageUrl, String prefix) { if (imageUrl == null || imageUrl.trim().isEmpty()) return null; try { URI uri=URI.create(imageUrl); String key=uri.getPath()==null?null:uri.getPath().replaceFirst("^/",""); String host=properties.getBucketName()+"."+endpointHost(); return host.equalsIgnoreCase(uri.getHost())&&key!=null&&key.matches(prefix+"\\d{4}/\\d{2}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp")?key:null; } catch (IllegalArgumentException e) { return null; } }

    private OSS createClient() {
        if (blank(properties.getEndpoint()) || blank(properties.getBucketName())
                || blank(properties.getAccessKeyId()) || blank(properties.getAccessKeySecret())) {
            throw new IllegalStateException("对象存储服务未配置");
        }
        return new OSSClientBuilder().build(properties.getEndpoint(), properties.getAccessKeyId(), properties.getAccessKeySecret());
    }

    private String publicUrl(String objectKey) {
        return "https://" + properties.getBucketName() + "." + endpointHost() + "/" + objectKey;
    }

    private String endpointHost() {
        return properties.getEndpoint().replaceFirst("^https?://", "").replaceAll("/+$", "");
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
