package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

/**
 * Репозиторий IndexRepository отвечает за работу
 * с поисковым индексом (IndexEntity).
 *
 * Используется:
 *  - при индексации страниц
 *  - при удалении старых данных
 *  - при выполнении поисковых запросов
 */
@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    /**
     * Удаляет все индексные записи, связанные с конкретной страницей.
     *
     * Используется при:
     * - повторной индексации страницы
     * - удалении страницы из базы
     *
     * Позволяет поддерживать целостность данных.
     */
    void deleteAllByPage(PageEntity page);

    /**
     * Проверяет, существует ли индексная запись
     * для конкретной пары страница–лемма.
     *
     * Используется для предотвращения
     * дублирования индекса.
     */
    boolean existsByPageAndLemma(PageEntity page, LemmaEntity lemma);

    /**
     * Получает все индексные записи
     * для указанной леммы.
     *
     * Используется при поиске страниц,
     * содержащих конкретное слово.
     */
    List<IndexEntity> findAllByLemma(LemmaEntity lemma);

    /**
     * Получает все индексные записи
     * по списку страниц и лемм.
     *
     * Используется при поиске
     * для расчёта релевантности страниц,
     * содержащих все леммы запроса.
     */
    @Query("""
SELECT i FROM IndexEntity i
WHERE i.page IN :pages
AND i.lemma IN :lemmas
""")
    List<IndexEntity> findAllByPagesAndLemmas(
            @Param("pages") List<PageEntity> pages,
            @Param("lemmas") List<LemmaEntity> lemmas
    );

    /**
     * Получает индексные записи по списку лемм
     * вместе с соответствующими страницами и сайтами.
     *
     * Используется для оптимизации поиска:
     * позволяет избежать N+1 запросов
     * при формировании результатов.
     */
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