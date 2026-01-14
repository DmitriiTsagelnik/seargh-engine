package searchengine.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Site {
    // URL сайта, например "https://example.com"
    private String url;

    // Название сайта, которое будет использоваться в статистике и поиске
    private String name;
}

