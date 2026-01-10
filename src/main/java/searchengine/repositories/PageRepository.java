package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;

public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    void deleteAllBySite(SiteEntity site);
    long countBySite(SiteEntity site);
    boolean existsBySiteAndPath(SiteEntity site, String path);
    Optional<PageEntity> findBySiteAndPath(SiteEntity site, String path);
}
