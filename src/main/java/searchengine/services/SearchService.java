package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.statistics.SearchResponse;
import searchengine.dto.statistics.SearchResult;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    // Сервис для работы с леммами (извлечение из текста)
    private final LemmaService lemmaService;

    // Репозитории для работы с базой
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;

    /**
     * Основной метод поиска.
     *
     * @param query   текст запроса
     * @param siteUrl (опционально) URL сайта для поиска
     * @param offset  сдвиг для пагинации (не используется, можно добавить)
     * @param limit   количество результатов (не используется, можно добавить)
     * @return SearchResponse с результатами поиска
     */
    @Transactional(readOnly = true)
    public SearchResponse search(String query, String siteUrl, int offset, int limit) {

        SearchResponse response = new SearchResponse();

        // Если запрос пустой — возвращаем пустой результат
        if (query == null || query.isBlank()) {
            response.setResult(false);
            response.setData(List.of());
            response.setCount(0);
            return response;
        }

        // Извлекаем леммы из запроса
        Set<String> queryLemmas = lemmaService.getLemmasWithCount(query).keySet();
        if (queryLemmas.isEmpty()) { // если лемм нет — возвращаем пустой результат
            response.setResult(true);
            response.setCount(0);
            response.setData(List.of());
            return response;
        }

        // Определяем сайты для поиска: либо все сайты, либо конкретный
        List<SiteEntity> sites = (siteUrl == null || siteUrl.isBlank())
                ? siteRepository.findAll()
                : siteRepository.findByUrl(siteUrl).map(List::of).orElse(List.of());

        if (sites.isEmpty()) { // если сайтов нет — пустой результат
            response.setResult(true);
            response.setCount(0);
            response.setData(List.of());
            return response;
        }

        // Получаем леммы из базы, соответствующие запросу и выбранным сайтам
        List<LemmaEntity> lemmas = lemmaRepository.findAllByLemmaInAndSiteIn(
                queryLemmas,
                sites
        );

        if (lemmas.isEmpty()) { // если таких лемм нет — пустой результат
            response.setResult(true);
            response.setCount(0);
            response.setData(List.of());
            return response;
        }

        // Сортировка лемм по частоте (меньшая частота = более релевантная)
        lemmas.sort(Comparator.comparing(LemmaEntity::getFrequency));

        // Получаем все индексы для найденных лемм вместе с страницами
        List<IndexEntity> indexes = indexRepository.findAllByLemmasWithPages(lemmas);

        if (indexes.isEmpty()) { // если нет индекса — пустой результат
            response.setResult(true);
            response.setCount(0);
            response.setData(List.of());
            return response;
        }

        // Расчёт абсолютной релевантности для каждой страницы
        Map<PageEntity, Float> absRelevance = new HashMap<>();
        for (IndexEntity index : indexes) {
            absRelevance.merge(
                    index.getPage(),
                    index.getRank(),  // суммируем ранги лемм на странице
                    Float::sum
            );
        }

        // Находим максимальную релевантность для нормализации
        float maxAbsRelevance = absRelevance.values().stream()
                .max(Float::compare)
                .orElse(1f);

        // Формируем SearchResult для каждой страницы
        List<SearchResult> results = absRelevance.entrySet().stream()
                .map(e -> {
                    PageEntity page = e.getKey();
                    float relevance = e.getValue() / maxAbsRelevance; // нормализуем релевантность

                    // Создаём сниппет для страницы
                    SnippetCreator snippetCreator = new SnippetCreator();

                    return new SearchResult(
                            page.getSite().getUrl(),   // URL сайта
                            page.getSite().getName(),  // имя сайта
                            page.getPath(),            // путь страницы
                            page.getTitle(),           // заголовок
                            snippetCreator.createSnippet(
                                    page.getContent(), // HTML страницы
                                    lemmas.stream()
                                            .map(LemmaEntity::getLemma)
                                            .collect(Collectors.toSet()) // набор лемм
                            ),
                            relevance                 // нормализованная релевантность
                    );
                })
                // сортировка результатов по релевантности по убыванию
                .sorted(Comparator.comparing(SearchResult::getRelevance).reversed())
                .toList();

        // Формируем ответ
        response.setResult(true);
        response.setCount(absRelevance.size());
        response.setData(results);
        return response;
    }

    /**
     * Вспомогательный метод извлечения заголовка страницы из HTML.
     * Используется для формирования SearchResult (не основной в текущей реализации).
     */
    private String pageTitle(String html) {
        if (html == null || html.isBlank()) return "";
        Document doc = Jsoup.parse(html);
        return doc.title();
    }
}
