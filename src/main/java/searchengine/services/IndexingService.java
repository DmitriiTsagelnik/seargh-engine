package searchengine.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
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


/**
 * Сервис IndexingService отвечает за индексацию сайтов и страниц.
 *
 * Основные задачи:
 * - запуск и остановка индексации сайтов
 * - загрузка страниц и извлечение контента
 * - формирование лемм и их подсчёт
 * - создание записей в поисковом индексе
 * - предоставление статистики по сайтам и индексации
 *
 * Использует:
 * - репозитории: SiteRepository, PageRepository, LemmaRepository, IndexRepository
 * - сервис: LemmaService (для выделения лемм)
 * - конфигурацию сайтов SitesList
 * - многопоточность через Executor / ForkJoinPool
 *
 * Логика потокобезопасна:
 * - AtomicBoolean и AtomicInteger контролируют состояние индексации
 * - ConcurrentHashMap хранит флаги остановки и блокировки по леммам
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {

    // Репозитории для работы с БД
    private final LemmaRepository lemmaRepository;       // репозиторий лемм
    private final IndexRepository indexRepository;       // репозиторий индекса
    private final SiteRepository siteRepository;         // репозиторий сайтов
    private final PageRepository pageRepository;         // репозиторий страниц

    // Сервис для извлечения лемм из текста
    private final LemmaService lemmaService;

    // Конфигурация сайтов (список сайтов, user-agent, referrer)
    private final SitesList sitesList;

    // Executor для многопоточной индексации
    private final Executor indexingExecutor;

    // Флаги и структуры для потокобезопасной работы
    private final AtomicBoolean indexingIsRunning = new AtomicBoolean(false);  // флаг индексации
    private final AtomicInteger runningSites = new AtomicInteger();           // количество сайтов в индексации
    private final ConcurrentHashMap<String, SiteCrawl.StopFlag> stopFlags = new ConcurrentHashMap<>(); // флаги остановки сайтов
    private final ConcurrentHashMap<String, Object> lemmaLocks = new ConcurrentHashMap<>(); // блокировки для лемм

    /**
     * Метод запускает индексацию всех сайтов из конфигурации.
     * Проверяет, идет ли индексация, и если нет — запускает потоки для каждого сайта.
     */
    public boolean startIndexing() {
        if (!indexingIsRunning.compareAndSet(false, true)) { // проверка и установка флага
            return false; // если уже идёт индексация — возвращаем false
        }

        List<Site> sites = sitesList.getSites(); // получаем список сайтов
        runningSites.set(sites.size()); // сохраняем количество сайтов

        for (Site site : sites) {
            // Для каждого сайта создаем отдельный поток в executor
            indexingExecutor.execute(() -> indexSite(site.getUrl(), site.getName()));
        }

        return true;
    }

    /**
     * Проверяет, выполняется ли индексация.
     */
    public boolean isIndexing() {
        return indexingIsRunning.get();
    }

    /**
     * Останавливает текущую индексацию.
     * - ставит флаги остановки для всех сайтов
     * - меняет статус сайтов на FAILED
     * - сохраняет сообщение об ошибке
     */
    public boolean stopIndexing() {
        if (!indexingIsRunning.get()) return false;

        // Останавливаем все текущие потоки индексации сайтов
        stopFlags.values().forEach(flag -> flag.stop("Индексация остановлена пользователем"));

        // Меняем статус сайтов на FAILED и сохраняем в базу
        List<SiteEntity> indexingSites = siteRepository.findAllByStatus(SiteStatus.INDEXING);
        for (SiteEntity site : indexingSites) {
            site.setStatus(SiteStatus.FAILED);
            site.setLastError("Индексация остановлена пользователем");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }

        stopFlags.clear(); // очищаем флаги остановки
        indexingIsRunning.set(false); // снимаем флаг работы индексации
        return true;
    }

    /**
     * Индексация одного сайта.
     * Основной метод для обхода и сохранения страниц сайта.
     */
    public void indexSite(String baseUrl, String siteName) {
        boolean siteHasError = false;
        SiteEntity siteEntity = new SiteEntity();
        SiteCrawl.StopFlag stopFlag = null;

        try {
            // Если сайт уже есть — удаляем старые страницы и сам сайт
            siteRepository.findByUrl(baseUrl).ifPresent(oldSite -> {
                pageRepository.deleteAllBySite(oldSite);
                siteRepository.delete(oldSite);
            });

            // Создаем новую запись сайта
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

            Set<String> visited = ConcurrentHashMap.newKeySet(); // множество посещенных URL
            stopFlag = new SiteCrawl.StopFlag(); // создаем stop-флаг для сайта
            stopFlags.put(baseUrl, stopFlag);

            // Создаем ForkJoinPool для рекурсивного обхода сайта
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
            // Обработка критических ошибок
            siteHasError = true;
            siteEntity.setStatus(SiteStatus.FAILED);
            siteEntity.setLastError("Критическая ошибка " + e.getMessage());
        } finally {
            // Обновление статуса сайта после завершения
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

            // Проверяем, завершились ли все сайты
            int remaining = runningSites.decrementAndGet();
            if (remaining == 0) {
                indexingIsRunning.set(false);
                log.info("\n\nИндексация всех сайтов завершена\n");
            }
        }
    }

    /**
     * Индексация отдельной страницы.
     * Проверяет разрешение сайта, загружает страницу и индексирует её.
     */
    public boolean indexSinglePage(String url) {
        try {
            URI uri = URI.create(url);
            String siteUrl = uri.getScheme() + "://" + uri.getHost();

            // Проверяем, разрешен ли сайт для индексации
            boolean siteAllowed = sitesList.getSites().stream()
                    .anyMatch(s -> s.getUrl().equals(siteUrl));
            if (!siteAllowed) return false;

            // Загружаем страницу
            FetchResult fetchResult = fetchPage(url);
            savePageAndIndex(siteUrl, fetchResult);

            return true;
        } catch (Exception e) {
            log.error("Ошибка индексации страницы {}", url, e);
            return false;
        }
    }

    /**
     * Обработка страницы: выделение лемм и формирование индекса.
     * Потокобезопасная запись лемм и индекса в базу.
     */
    @Transactional
    public void processPage(PageEntity page, String html) {
        Map<String, Integer> lemmaCount = lemmaService.getLemmasWithCount(html);

        for (Map.Entry<String, Integer> entry : lemmaCount.entrySet()) {
            String lemmaText = entry.getKey();
            int count = entry.getValue();

            // Потокобезопасная блокировка по лемме и сайту
            Object lock = lemmaLocks.computeIfAbsent(lemmaText + "-" + page.getSite().getId(), k -> new Object());

            synchronized (lock) {
                // Находим или создаем лемму
                LemmaEntity lemmaEntity = lemmaRepository.findByLemmaAndSiteId(lemmaText, page.getSite().getId())
                        .orElseGet(() -> {
                            LemmaEntity newLemma = new LemmaEntity();
                            newLemma.setLemma(lemmaText);
                            newLemma.setSite(page.getSite());
                            newLemma.setFrequency(0);
                            lemmaRepository.save(newLemma);
                            return newLemma;
                        });

                // Если индекс для страницы и леммы ещё не создан
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

    /**
     * Загрузка страницы через Jsoup.
     * Возвращает FetchResult с кодом статуса, HTML и путём.
     */
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

        return new FetchResult(
                response.statusCode(),
                response.body(),
                path
        );
    }

    /**
     * Сохраняет страницу и индексирует её леммы.
     * - Создаёт PageEntity
     * - Сохраняет HTML и заголовок
     * - Вызывает processPage
     */
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

    /**
     * Формирует объект StatisticsResponse для API.
     * - собирает все сайты
     * - считает общее количество страниц и лемм
     * - формирует TotalStatistics и DetailedStatisticsItem
     */
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


