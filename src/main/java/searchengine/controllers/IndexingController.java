package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class IndexingController {

    private final IndexingService indexingService;

    @GetMapping("/startIndexing")
    public Map<String, Object> startIndexing() {
        boolean start = indexingService.startIndexing();
        return start ? Map.of("result", true) :
                Map.of("result", false, "error", "индексация уже запущена");
    }

    @GetMapping("/stopIndexing")
    public Map<String, Object> stopIndexing() {
        boolean stopped = indexingService.stopIndexing();
        return stopped ? Map.of("result", true) :
                Map.of("result", false, "error", "Индексация не запущена");
    }

    @PostMapping("/indexPage")
    public Map<String, Object> indexPage(@RequestParam String url) {
        boolean success = indexingService.indexSinglePage(url);
        return success ? Map.of("result", true) :
                Map.of("result", false, "error",
                        "Данная страница находится за пределами сайтов, указанных в конфигурации");
    }

    @GetMapping("/indexing/statistics")
    public StatisticsResponse statistics() {
        return indexingService.getStatistics();
    }
}

