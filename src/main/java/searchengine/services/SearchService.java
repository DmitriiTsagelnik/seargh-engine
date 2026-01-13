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

    private final LemmaService lemmaService;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;

    @Transactional(readOnly = true)
    public SearchResponse search(String query, String siteUrl, int offset, int limit) {

        SearchResponse response = new SearchResponse();

        if (query == null || query.isBlank()) {
            response.setResult(false);
            response.setData(List.of());
            response.setCount(0);
            return response;
        }

        Set<String> queryLemmas = lemmaService.getLemmasWithCount(query).keySet();
        if (queryLemmas.isEmpty()) {
            response.setResult(true);
            response.setCount(0);
            response.setData(List.of());
            return response;
        }

        List<SiteEntity> sites = (siteUrl == null || siteUrl.isBlank())
                ? siteRepository.findAll()
                : siteRepository.findByUrl(siteUrl).map(List::of).orElse(List.of());

        if (sites.isEmpty()) {
            response.setResult(true);
            response.setCount(0);
            response.setData(List.of());
            return response;
        }

        List<LemmaEntity> lemmas = lemmaRepository.findAllByLemmaInAndSiteIn(
                queryLemmas,
                sites
        );

        if (lemmas.isEmpty()) {
            response.setResult(true);
            response.setCount(0);
            response.setData(List.of());
            return response;
        }

        lemmas.sort(Comparator.comparing(LemmaEntity::getFrequency));

        List<IndexEntity> indexes =
                indexRepository.findAllByLemmasWithPages(lemmas);

        if (indexes.isEmpty()) {
            response.setResult(true);
            response.setCount(0);
            response.setData(List.of());
            return response;
        }

        Map<PageEntity, Float> absRelevance = new HashMap<>();

        for (IndexEntity index : indexes) {
            absRelevance.merge(
                    index.getPage(),
                    index.getRank(),
                    Float::sum
            );
        }

        float maxAbsRelevance = absRelevance.values().stream()
                .max(Float::compare)
                .orElse(1f);

        List<SearchResult> results = absRelevance.entrySet().stream()
                .map(e -> {
                    PageEntity page = e.getKey();
                    float relevance = e.getValue() / maxAbsRelevance;

                    SnippetCreator snippetCreator = new SnippetCreator();

                    return new SearchResult(
                            page.getSite().getUrl(),
                            page.getSite().getName(),
                            page.getPath(),
                            page.getTitle(),
                            snippetCreator.createSnippet(
                                    page.getContent(),
                                    lemmas.stream()
                                            .map(LemmaEntity::getLemma)
                                            .collect(Collectors.toSet())
                            ),
                            relevance
                    );
                })
                .sorted(Comparator.comparing(SearchResult::getRelevance).reversed())
                .toList();

        response.setResult(true);
        response.setCount(absRelevance.size());
        response.setData(results);
        return response;
    }

    private String pageTitle(String html) {
        if (html == null || html.isBlank()) return "";
        Document doc = Jsoup.parse(html);
        return doc.title();
    }
}