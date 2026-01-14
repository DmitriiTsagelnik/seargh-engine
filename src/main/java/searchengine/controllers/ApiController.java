package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api") // Все эндпоинты начинаются с /api
public class ApiController {

    private final StatisticsService statisticsService; // Сервис для получения статистики

    // Конструктор для внедрения сервиса
    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * GET /api/statistics
     * Возвращает текущую статистику работы поискового движка
     *
     * @return ResponseEntity с DTO StatisticsResponse
     */
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        // Получаем статистику от сервиса и возвращаем HTTP 200 OK
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
}

