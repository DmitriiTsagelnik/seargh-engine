package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;

/**
 * Репозиторий PageRepository предоставляет доступ
 * к данным сущности PageEntity.
 *
 * Используется для управления страницами сайтов,
 * полученными в процессе индексации.
 */
@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    /**
     * Удаляет все страницы, принадлежащие указанному сайту.
     *
     * Используется при:
     * - повторной индексации сайта
     * - остановке и перезапуске индексации
     *
     * Позволяет очистить старые данные перед
     * сохранением новых страниц.
     */
    void deleteAllBySite(SiteEntity site);

    /**
     * Подсчитывает количество страниц у сайта.
     *
     * Используется:
     * - в статистике
     * - для отображения прогресса индексации
     */
    long countBySite(SiteEntity site);

    /**
     * Проверяет, существует ли страница
     * с указанным путём у данного сайта.
     *
     * Используется для предотвращения
     * повторной индексации одной и той же страницы.
     */
    boolean existsBySiteAndPath(SiteEntity site, String path);

    /**
     * Поиск страницы по сайту и пути.
     *
     * Используется:
     * - для получения сохранённой страницы
     * - при обновлении или анализе данных
     */
    Optional<PageEntity> findBySiteAndPath(SiteEntity site, String path);
}
