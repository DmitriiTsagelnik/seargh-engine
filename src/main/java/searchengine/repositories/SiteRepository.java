package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий SiteRepository предоставляет доступ
 * к данным сущности SiteEntity.
 *
 * Использует Spring Data JPA, что позволяет
 * работать с базой данных без написания
 * SQL-запросов вручную.
 *
 * Наследование от JpaRepository даёт
 * готовый набор CRUD-операций.
 */
@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    /**
     * Поиск сайта по его URL.
     *
     * Используется:
     * - при запуске индексации
     * - для проверки, был ли сайт уже добавлен в систему
     *
     * Optional используется для безопасной обработки
     * отсутствующего результата.
     */
    Optional<SiteEntity> findByUrl(String url);

    /**
     * Получение всех сайтов с указанным статусом индексации.
     *
     * Используется:
     * - для получения списка индексируемых сайтов
     * - в статистике
     * - при управлении процессами индексации
     */
    List<SiteEntity> findAllByStatus(SiteStatus status);
}
