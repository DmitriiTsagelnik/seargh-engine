package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StartIndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class IndexingController {

    private final IndexingService indexingService;

    @GetMapping("/startIndexing")
    public StartIndexingResponse startIndexing() {
        boolean started = indexingService.startIndexing();
        return started ? new StartIndexingResponse(true, null) :
                new StartIndexingResponse(false, "индексация уже запущена");
    }

    @GetMapping("/stopIndexing")
    public StartIndexingResponse stopIndexing() {
        boolean stopped = indexingService.stopIndexing();
        return stopped ? new StartIndexingResponse(true, null) :
                new StartIndexingResponse(false, "Индексация не запущена");
    }

    @PostMapping("/indexPage")
    public StartIndexingResponse indexPage(@RequestParam String url) {
        boolean success = indexingService.indexSinglePage(url);
        return success ? new StartIndexingResponse(true,null) :
                new StartIndexingResponse(false,
                        "Данная страница находится за пределами сайтов, указанных в конфигурации");
    }

    @GetMapping("/indexing/statistics")
    public StatisticsResponse statistics() {
        return indexingService.getStatistics();
    }
}

