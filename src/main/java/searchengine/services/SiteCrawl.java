package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.RecursiveAction;

@Slf4j
@RequiredArgsConstructor
public class SiteCrawl extends RecursiveAction {

    // URL текущей страницы
    private final String url;

    // Домен сайта (для ограничения переходов по внешним ссылкам)
    private final String domain;

    // Сущность сайта из базы
    private final SiteEntity site;

    // Множество уже посещённых URL (для предотвращения циклов)
    private final Set<String> visited;

    // Репозитории для сохранения страниц и обновления статуса сайта
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    // Сервис индексации (для обработки текста страницы)
    private final IndexingService indexingService;

    // HTTP-заголовки
    private final String userAgent;
    private final String referrer;

    // Флаг остановки индексации
    private final StopFlag stopFlag;

    /**
     * Основной метод обхода страницы.
     * Выполняется рекурсивно в рамках ForkJoinPool
     */
    @Override
    protected void compute() {

        // Если URL уже посещён или индексация остановлена — выходим
        if (!visited.add(url) || stopFlag.isStopped()) {
            return;
        }

        // Нормализуем путь (отбрасываем домен и hash)
        String path = normalizePath(url, site.getUrl());

        // Если страница уже есть в БД — пропускаем
        if (pageRepository.existsBySiteAndPath(site, path)) return;

        try {
            // Рандомная задержка между запросами (500–5000 мс), чтобы не нагружать сайт
            Thread.sleep(500 + (int) (Math.random() * 4500));

            // Отправляем HTTP-запрос через Jsoup
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(10000)
                    .ignoreHttpErrors(true)
                    .execute();

            int statusCode = response.statusCode();

            // Если ошибка HTTP (400+) — пропускаем страницу
            if (statusCode >= 400) {
                log.warn("Страница {} с ошибкой {}, пропуск", url, statusCode);
                return;
            }

            String html = response.body();

            // Сохраняем страницу в БД
            PageEntity page = new PageEntity();
            page.setSite(site);
            page.setPath(path);
            page.setCode(statusCode);
            page.setContent(html);
            pageRepository.save(page);

            // Обрабатываем страницу: разбиваем на леммы и создаём индекс
            indexingService.processPage(page, html);

            log.info("\nПроиндексирована страница: site={}, path={}, code={}",
                    site.getUrl(), path, statusCode);

            // Парсим ссылки на странице и создаём новые задачи для рекурсии
            Document doc = Jsoup.parse(html);
            Elements links = doc.select("a[href]");
            List<SiteCrawl> tasks = new ArrayList<>();
            for (Element link : links) {
                String absHref = link.absUrl("href");

                // Пропускаем пустые ссылки и если индексация остановлена
                if (absHref.isEmpty() || stopFlag.isStopped()) {
                    continue;
                }

                // Проверяем, что ссылка принадлежит тому же домену
                URI uri = URI.create(absHref);
                if (uri.getHost() == null || !uri.getHost().equals(domain)) {
                    continue;
                }

                // Создаём новую задачу для ForkJoinPool
                tasks.add(new SiteCrawl(
                        absHref,
                        domain,
                        site,
                        visited,
                        siteRepository,
                        pageRepository,
                        indexingService,
                        userAgent,
                        referrer,
                        stopFlag
                ));
            }

            // Запускаем все задачи параллельно
            invokeAll(tasks);

        } catch (IOException e) {
            log.warn("Ошибка загрузки страницы {}: {}", url, e.getMessage());

        } catch (Exception e) {
            // Если критическая ошибка — останавливаем индексацию
            log.error("Критическая ошибка " + e.getMessage());
            stopFlag.stop("Критическая ошибка " + e.getMessage());
        }
    }

    /**
     * Преобразование полного URL в путь страницы
     */
    private String normalizePath(String fullUrl, String siteUrl) {
        String path = fullUrl.replaceFirst(siteUrl, "");
        if (path.isEmpty()) path = "/";
        int hashIndex = path.indexOf('#'); // убираем якорь
        if (hashIndex != -1) path = path.substring(0, hashIndex);
        return path;
    }

    /**
     * Класс для управления остановкой индексации
     */
    public static class StopFlag {
        private volatile boolean stopped = false;   // индикатор остановки
        private volatile String errorMessage;       // сообщение об ошибке

        public boolean isStopped() {
            return stopped;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        // Остановка индексации с указанием причины
        public void stop(String errorMessage) {
            this.stopped = true;
            this.errorMessage = errorMessage;
        }
    }
}

