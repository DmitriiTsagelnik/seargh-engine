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

    private final float MAX_FREQUENCY = 0.8f;

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

        List<LemmaEntity> lemmas = new ArrayList<>();
        for (SiteEntity site : sites) {
            for (String lemmaText : queryLemmas) {
                lemmaRepository.findByLemmaAndSiteId(lemmaText, site.getId())
                        .ifPresent(lemmas::add);
            }
        }

        if (lemmas.isEmpty()) {
            response.setResult(true);
            response.setData(List.of());
            response.setCount(0);
            return response;
        }

        lemmas.sort(Comparator.comparing(LemmaEntity::getFrequency));

        Set<PageEntity> pages = new HashSet<>();
        for (LemmaEntity lemma : lemmas) {
            List<IndexEntity> idxList = indexRepository.findAllByLemma(lemma);
            for (IndexEntity idx : idxList) {
                pages.add(idx.getPage());
            }
        }

        if (pages.isEmpty()) {
            response.setResult(true);
            response.setData(List.of());
            response.setCount(0);
            return response;
        }

        List<IndexEntity> indexes = indexRepository.findAllByPagesAndLemmas(new ArrayList<>(pages), lemmas);

        Map<PageEntity, Float> absRelevance = new HashMap<>();
        for (IndexEntity index : indexes) {
            absRelevance.merge(index.getPage(), index.getRank(), Float::sum);
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
                            pageTitle(page.getContent()),
                            snippetCreator.createSnippet(page.getContent(),
                                    lemmas.stream().map(LemmaEntity::getLemma).collect(Collectors.toSet())),
                            relevance
                    );
                })
                .sorted(Comparator.comparing(SearchResult::getRelevance).reversed())
                .toList();

        response.setResult(true);
        response.setCount(results.size());
        response.setData(results.stream()
                .skip(offset)
                .limit(limit)
                .toList());

        return response;
    }

    private String pageTitle(String html) {
        if (html == null || html.isBlank()) return "";
        Document doc = Jsoup.parse(html);
        return doc.title();
    }
}