package searchengine.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.ExecutorConfig;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.*;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaService lemmaService;
    private final SitesList sitesList;
    private final Executor indexingExecutor;

    private final AtomicBoolean indexingIsRunning = new AtomicBoolean(false);
    private final AtomicInteger runningSites = new AtomicInteger();
    private final ConcurrentHashMap<String, SiteCrawl.StopFlag> stopFlags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> lemmaLocks = new ConcurrentHashMap<>();

    public boolean startIndexing() {
        if (!indexingIsRunning.compareAndSet(false, true)) {
            return false;
        }

        List<Site> sites = sitesList.getSites();
        runningSites.set(sites.size());

        for (Site site : sites) {
            indexingExecutor.execute(() ->
            indexSite(site.getUrl(), site.getName()));
        }

        return true;
    }

    public boolean isIndexing() {
        return indexingIsRunning.get();
    }

    public boolean stopIndexing() {
        if (!indexingIsRunning.get()) return false;

        stopFlags.values().forEach(
                flag -> flag.stop("Индексация остановлена пользователем")
        );
        List<SiteEntity> indexingSites = siteRepository.findAllByStatus(SiteStatus.INDEXING);
        for (SiteEntity site : indexingSites) {
            site.setStatus(SiteStatus.FAILED);
            site.setLastError("Индексация остановлена пользователем");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }
        stopFlags.clear();
        indexingIsRunning.set(false);
        return true;
    }

    public void indexSite(String baseUrl, String siteName) {
        boolean siteHasError = false;
        SiteEntity siteEntity = new SiteEntity();
        SiteCrawl.StopFlag stopFlag = null;

        try {
            siteRepository.findByUrl(baseUrl).ifPresent(oldSite -> {
                pageRepository.deleteAllBySite(oldSite);
                siteRepository.delete(oldSite);
            });

            siteEntity.setUrl(baseUrl);
            siteEntity.setName(siteName);
            siteEntity.setStatus(SiteStatus.INDEXING);
            siteEntity.setLastError(null);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);

            String domain = URI.create(baseUrl).getHost();
            if (domain == null) {
                throw new IllegalArgumentException("Некорректный URL: " + baseUrl);
            }

            Set<String> visited = ConcurrentHashMap.newKeySet();
            stopFlag = new SiteCrawl.StopFlag();
            stopFlags.put(baseUrl, stopFlag);

            ForkJoinPool pool = new ForkJoinPool();
            pool.invoke(new SiteCrawl(
                    baseUrl,
                    domain,
                    siteEntity,
                    visited,
                    siteRepository,
                    pageRepository,
                    this,
                    sitesList.getUserAgent(),
                    sitesList.getReferrer(),
                    stopFlag
            ));

        } catch (Exception e) {
            siteHasError = true;
            siteEntity.setStatus(SiteStatus.FAILED);
            siteEntity.setLastError("Критическая ошибка " + e.getMessage());
        } finally {
            siteEntity.setStatusTime(LocalDateTime.now());
            if (siteEntity.getLastError() != null || siteHasError || (stopFlag != null && stopFlag.isStopped())) {
                siteEntity.setStatus(SiteStatus.FAILED);
                log.error("\n\nИндексация сайта {} завершилась с ошибкой: {}\n", siteEntity.getUrl(),
                        siteEntity.getLastError());
            } else {
                siteEntity.setStatus(SiteStatus.INDEXED);
                log.info("\n\nИндексация сайта {} завершена\n", siteEntity.getUrl());
            }

            siteRepository.save(siteEntity);
            stopFlags.remove(baseUrl);

            int remaining = runningSites.decrementAndGet();
            if (remaining == 0) {
                indexingIsRunning.set(false);
                log.info("\n\nИндексация всех сайтов завершена\n");
            }
        }
    }

    public boolean indexSinglePage(String url) {
        try {
            URI uri = URI.create(url);
            String siteUrl = uri.getScheme() + "://" + uri.getHost();

            boolean siteAllowed = sitesList.getSites().stream()
                    .anyMatch(s -> s.getUrl().equals(siteUrl));
            if (!siteAllowed) return false;

            FetchResult fetchResult = fetchPage(url);
            savePageAndIndex(siteUrl, fetchResult);

            return true;
        } catch (Exception e) {
            log.error("Ошибка индексации страницы {}", url, e);
            return false;
        }
    }

    @Transactional
    public void processPage(PageEntity page, String html) {
        Map<String, Integer> lemmaCount = lemmaService.getLemmasWithCount(html);

        for (Map.Entry<String, Integer> entry : lemmaCount.entrySet()) {
            String lemmaText = entry.getKey();
            int count = entry.getValue();

            Object lock = lemmaLocks.computeIfAbsent(lemmaText + "-" + page.getSite().getId(), k -> new Object());

            synchronized (lock) {
                LemmaEntity lemmaEntity = lemmaRepository.findByLemmaAndSiteId(lemmaText, page.getSite().getId())
                        .orElseGet(() -> {
                            LemmaEntity newLemma = new LemmaEntity();
                            newLemma.setLemma(lemmaText);
                            newLemma.setSite(page.getSite());
                            newLemma.setFrequency(0);
                            lemmaRepository.save(newLemma);
                            return newLemma;
                        });

                if (!indexRepository.existsByPageAndLemma(page, lemmaEntity)) {
                    lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
                    lemmaRepository.save(lemmaEntity);

                    IndexEntity indexEntity = new IndexEntity();
                    indexEntity.setPage(page);
                    indexEntity.setLemma(lemmaEntity);
                    indexEntity.setRank((float) count);
                    indexRepository.save(indexEntity);
                }
            }
        }
    }

    private FetchResult fetchPage(String url) throws Exception {
        URI uri = URI.create(url);

        String path = uri.getPath();
        if (path.isEmpty()) {
            path = "/";
        }

        Connection.Response response = Jsoup.connect(url)
                .userAgent(sitesList.getUserAgent())
                .referrer(sitesList.getReferrer())
                .timeout(10000)
                .ignoreHttpErrors(true)
                .execute();

        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP error: " + response.statusCode());
        }

        FetchResult result = new FetchResult(
        response.statusCode(),
        response.body(),
        path
        );

        return result;
    }

    @Transactional
    protected void savePageAndIndex(String siteUrl, FetchResult fetchResult) {

        SiteEntity siteEntity = siteRepository.findByUrl(siteUrl)
                .orElseGet(() -> {
                    SiteEntity newSite = new SiteEntity();
                    newSite.setUrl(siteUrl);
                    newSite.setName(siteUrl);
                    newSite.setStatus(SiteStatus.INDEXED);
                    newSite.setStatusTime(LocalDateTime.now());
                    siteRepository.save(newSite);
                    return newSite;
                });

        pageRepository.findBySiteAndPath(siteEntity, fetchResult.getPath())
                .ifPresent(oldPage -> {
                    indexRepository.deleteAllByPage(oldPage);
                    pageRepository.delete(oldPage);
                });

        PageEntity page = new PageEntity();
        page.setSite(siteEntity);
        page.setPath(fetchResult.getPath());
        page.setCode(fetchResult.getStatusCode());
        page.setContent(fetchResult.getHtml());

        Document doc = Jsoup.parse(fetchResult.getHtml());
        page.setTitle(doc.title());

        pageRepository.save(page);

        processPage(page, fetchResult.getHtml());
    }

    public StatisticsResponse getStatistics() {
        List<SiteEntity> sites = siteRepository.findAll();

        long totalPages = sites.stream().mapToLong(pageRepository::countBySite).sum();
        long totalLemmas = lemmaRepository.count();

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.size());
        total.setPages((int) totalPages);
        total.setLemmas((int) totalLemmas);
        total.setIndexing(indexingIsRunning.get());

        List<DetailedStatisticsItem> detailed = sites.stream()
                .map(s -> {
                    int pagesCount = (int) pageRepository.countBySite(s);
                    int lemmasCount = lemmaRepository.countBySiteId(s.getId());

                    DetailedStatisticsItem item = new DetailedStatisticsItem();
                    item.setUrl(s.getUrl());
                    item.setName(s.getName());
                    item.setStatus(s.getStatus().name());
                    item.setStatusTime(s.getStatusTime() != null ?
                            s.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0);
                    item.setError(s.getLastError());
                    item.setPages(pagesCount);
                    item.setLemmas(lemmasCount);
                    return item;
                })
                .toList();

        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);

        StatisticsResponse response = new StatisticsResponse();
        response.setResult(true);
        response.setStatistics(data);
        return response;
    }
}

