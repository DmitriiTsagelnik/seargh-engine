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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;

@Slf4j
@RequiredArgsConstructor
public class SiteCrawl extends RecursiveAction {

    private final String url;
    private final String domain;
    private final SiteEntity site;
    private final Set<String> visited;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexingService indexingService;
    private final String userAgent;
    private final String referrer;
    private final StopFlag stopFlag;

    @Override
    protected void compute() {
        if (!visited.add(url) || stopFlag.isStopped()) {
            return;
        }

        String path = normalizePath(url, site.getUrl());
        if (pageRepository.existsBySiteAndPath(site, path)) return;

        try {
            Thread.sleep(500 + (int) (Math.random() * 4500));

            Connection.Response response = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(10000)
                    .ignoreHttpErrors(true)
                    .execute();

            int statusCode = response.statusCode();

            if (statusCode >= 400) {
                log.warn("Страница {} с ошибкой {}, пропуск", url, statusCode);
                return;
            }

            String html = response.body();

            PageEntity page = new PageEntity();
            page.setSite(site);
            page.setPath(path);
            page.setCode(statusCode);
            page.setContent(html);
            pageRepository.save(page);

            indexingService.processPage(page, html);
            log.info("\nПроиндексирована страница: site={}, path={}, code={}",
                    site.getUrl(), path, statusCode);

            Document doc = Jsoup.parse(html);
            Elements links = doc.select("a[href]");
            List<SiteCrawl> tasks = new ArrayList<>();
            for (Element link : links) {
                String absHref = link.absUrl("href");
                if (absHref.isEmpty() || stopFlag.isStopped()) {
                    continue;
                }

                URI uri = URI.create(absHref);
                if (uri.getHost() == null || !uri.getHost().equals(domain)) {
                    continue;
                }
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

            invokeAll(tasks);

        } catch (IOException e) {
                log.warn("Ошибка загрузки страницы {}: {}", url, e.getMessage());

        } catch (Exception e) {
            log.error("Критическая ошибка " + e.getMessage());
            stopFlag.stop("Критическая ошибка " + e.getMessage());
        }
    }

    private String normalizePath(String fullUrl, String siteUrl) {
        String path = fullUrl.replaceFirst(siteUrl, "");
        if (path.isEmpty()) path = "/";
        int hashIndex = path.indexOf('#');
        if (hashIndex != -1) path = path.substring(0, hashIndex);
        return path;
    }

    public static class StopFlag {
        private volatile boolean stopped = false;
        private volatile String errorMessage;

        public boolean isStopped() {
            return stopped;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void stop(String errorMessage) {
            this.stopped = true;
            this.errorMessage = errorMessage;
        }
    }
}
