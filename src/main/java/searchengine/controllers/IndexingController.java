package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StartIndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;

@RestController
@RequestMapping("/api") // Все эндпоинты начинаются с /api
@RequiredArgsConstructor
public class IndexingController {

    private final IndexingService indexingService; // Сервис индексации

    /**
     * GET /api/startIndexing
     * Запускает индексацию всех сайтов из конфигурации
     */
    @GetMapping("/startIndexing")
    public StartIndexingResponse startIndexing() {
        boolean started = indexingService.startIndexing(); // Вызывает метод сервиса
        // Возвращаем DTO с результатом
        return started ? new StartIndexingResponse(true, null) :
                new StartIndexingResponse(false, "индексация уже запущена");
    }

    /**
     * GET /api/stopIndexing
     * Останавливает текущую индексацию
     */
    @GetMapping("/stopIndexing")
    public StartIndexingResponse stopIndexing() {
        boolean stopped = indexingService.stopIndexing();
        return stopped ? new StartIndexingResponse(true, null) :
                new StartIndexingResponse(false, "Индексация не запущена");
    }

    /**
     * POST /api/indexPage?url={url}
     * Индексация отдельной страницы по URL
     */
    @PostMapping("/indexPage")
    public StartIndexingResponse indexPage(@RequestParam String url) {
        boolean success = indexingService.indexSinglePage(url);
        return success ? new StartIndexingResponse(true, null) :
                new StartIndexingResponse(false,
                        "Данная страница находится за пределами сайтов, указанных в конфигурации");
    }

    /**
     * GET /api/indexing/statistics
     * Получение текущей статистики индексации
     */
    @GetMapping("/indexing/statistics")
    public StatisticsResponse statistics() {
        return indexingService.getStatistics(); // Возвращает DTO с детальной статистикой
    }
}


