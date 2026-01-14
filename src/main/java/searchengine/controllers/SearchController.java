package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.SearchResponse;
import searchengine.services.SearchService;

@RestController
@RequestMapping("/api") // Все эндпоинты начинаются с /api
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService; // Сервис поиска

    /**
     * GET /api/search
     * Поиск по сайтам по текстовому запросу
     *
     * @param query  строка поиска (обязательный параметр)
     * @param site   URL конкретного сайта (необязательный параметр)
     * @param offset смещение для пагинации (по умолчанию 0)
     * @param limit  количество результатов (по умолчанию 20)
     * @return SearchResponse — DTO с результатами поиска
     */
    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam("query") String query,
            @RequestParam(value = "site", required = false) String site,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {

        // Вызывает сервис поиска с параметрами запроса
        return searchService.search(query, site, offset, limit);
    }
}

