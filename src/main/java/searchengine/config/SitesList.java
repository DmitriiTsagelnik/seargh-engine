package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesList {

    // Максимальная глубина обхода сайта (для рекурсивного обхода)
    private int maxDepth;

    // Список сайтов для индексации
    private List<Site> sites;

    // User-Agent, который будет использоваться при HTTP-запросах
    private String userAgent;

    // Referrer, который будет указываться при HTTP-запросах
    private String referrer;
}

