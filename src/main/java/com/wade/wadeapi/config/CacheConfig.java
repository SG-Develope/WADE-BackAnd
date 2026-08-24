package com.wade.wadeapi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // 기존 캐시(aiGuide, cctvUrls): 10분 (CCTV 스트림 URL은 만료가 빠르므로 짧게 유지)
        CaffeineCacheManager manager = new CaffeineCacheManager("aiGuide", "cctvUrls");
        manager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(50)
        );

        // 산책로 GPX 파싱 결과: 거의 안 바뀌므로 6시간/400개로 넉넉히 별도 등록
        manager.registerCustomCache(
            "trailPath",
            Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .maximumSize(400)
                .build()
        );
        return manager;
    }
}
