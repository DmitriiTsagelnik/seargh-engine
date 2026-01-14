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

/**
 * Репозиторий LemmaRepository отвечает за работу
 * с сущностью LemmaEntity.
 *
 * Используется при:
 *  - индексации страниц
 *  - подсчёте частот лемм
 *  - формировании поисковых запросов
 */
@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {

    /**
     * Поиск леммы по её тексту и идентификатору сайта.
     *
     * Используется при индексации:
     * - если лемма уже существует, увеличивается её частота
     * - если нет — создаётся новая запись
     */
    Optional<LemmaEntity> findByLemmaAndSiteId(String lemma, int siteId);

    /**
     * Подсчитывает количество лемм на сайте.
     *
     * Используется в сервисе статистики
     * для отображения общего количества лемм.
     */
    @Query("SELECT COUNT(l) FROM LemmaEntity l WHERE l.site.id = :siteId")
    int countBySiteId(@Param("siteId") int siteId);

    /**
     * Получает список лемм по набору текстов лемм
     * и списку сайтов.
     *
     * Используется в поисковом сервисе
     * для получения всех лемм,
     * соответствующих поисковому запросу.
     *
     * JPQL-запрос позволяет эффективно выбрать
     * только нужные данные без лишних загрузок.
     */
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