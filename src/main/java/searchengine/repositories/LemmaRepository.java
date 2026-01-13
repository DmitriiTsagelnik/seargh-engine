package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {

    Optional<LemmaEntity> findByLemmaAndSiteId(String lemma, int siteId);

    @Query("SELECT COUNT(l) FROM LemmaEntity l WHERE l.site.id = :siteId")
    int countBySiteId(@Param("siteId") int siteId);

    @Query("""
    SELECT l
    FROM LemmaEntity l
    WHERE l.lemma IN :lemmas
      AND l.site IN :sites
""")
    List<LemmaEntity> findAllByLemmaInAndSiteIn(
            @Param("lemmas") Set<String> lemmas,
            @Param("sites") List<SiteEntity> sites
    );

}