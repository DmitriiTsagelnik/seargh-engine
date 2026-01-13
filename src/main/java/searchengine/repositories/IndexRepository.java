package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    void deleteAllByPage(PageEntity page);
    boolean existsByPageAndLemma(PageEntity page, LemmaEntity lemma);
    List<IndexEntity> findAllByLemma(LemmaEntity lemma);

    @Query("""
SELECT i FROM IndexEntity i
WHERE i.page IN :pages
AND i.lemma IN :lemmas
""")
    List<IndexEntity> findAllByPagesAndLemmas(
            @Param("pages") List<PageEntity> pages,
            @Param("lemmas") List<LemmaEntity> lemmas
    );

    @Query("""
    SELECT i
    FROM IndexEntity i
    JOIN FETCH i.page p
    JOIN FETCH p.site s
    WHERE i.lemma IN :lemmas
""")
    List<IndexEntity> findAllByLemmasWithPages(
            @Param("lemmas") List<LemmaEntity> lemmas
    );
}