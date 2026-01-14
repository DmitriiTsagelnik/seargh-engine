package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Сущность SiteEntity описывает сайт, участвующий в индексации.
 *
 * Класс соответствует таблице site в базе данных и хранит
 * основную информацию о сайте и состоянии его индексации.
 *
 * Данная сущность является корневой для всего поискового движка:
 *  - с неё начинается процесс индексации
 *  - к ней привязываются страницы, леммы и индексы
 */
@Entity
@Table(name = "site")
@Getter
@Setter
@NoArgsConstructor
public class SiteEntity {

    /**
     * Уникальный идентификатор сайта в базе данных.
     * Генерируется автоматически СУБД.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Текущий статус индексации сайта.
     *
     * Возможные значения:
     * - INDEXING — сайт находится в процессе индексации
     * - INDEXED  — сайт полностью проиндексирован
     * - FAILED   — индексация завершилась с ошибкой
     *
     * Статус хранится в виде строки, что делает данные
     * читаемыми и удобными для отладки.
     */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING','INDEXED','FAILED')", nullable = false)
    private SiteStatus status;

    /**
     * Время последнего изменения статуса индексации.
     *
     * Используется:
     * - для отображения актуального состояния сайта
     * - в статистике
     * - при контроле длительных процессов индексации
     */
    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    /**
     * Сообщение об ошибке индексации.
     *
     * Заполняется только в случае статуса FAILED.
     * Позволяет понять причину сбоя без просмотра логов.
     */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * URL сайта, который индексируется.
     *
     * Используется как уникальный логический идентификатор сайта
     * во всей системе.
     */
    @Column(name = "url", length = 255, nullable = false)
    private String url;

    /**
     * Человекочитаемое имя сайта.
     *
     * Используется для отображения в статистике
     * и пользовательском интерфейсе.
     */
    @Column(name = "name", length = 255, nullable = false)
    private String name;
}
