package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Сущность PageEntity описывает отдельную веб-страницу сайта.
 *
 * Класс соответствует таблице page и хранит данные,
 * полученные при обходе сайта:
 *  - путь страницы
 *  - HTTP-код ответа
 *  - HTML-контент
 *  - заголовок страницы
 *
 * Каждая страница всегда принадлежит конкретному сайту (SiteEntity).
 */
@Entity
@Table(name = "page", indexes = {
        @Index(name = "index_page_path", columnList = "path")
})
@Getter
@Setter
@NoArgsConstructor
public class PageEntity {

    /**
     * Уникальный идентификатор страницы в базе данных.
     * Генерируется автоматически.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Связь с сайтом, к которому принадлежит страница.
     *
     * Тип связи: Many-to-One — у одного сайта может быть много страниц.
     * Используется ленивый тип загрузки (LAZY),
     * чтобы не загружать сайт без необходимости.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    /**
     * Путь страницы относительно корня сайта.
     *
     * Например:
     * /about
     * /news/2024
     *
     * Полный URL формируется как site.url + path.
     */
    @Column(length = 255, nullable = false)
    private String path;

    /**
     * HTTP-код ответа, полученный при загрузке страницы.
     *
     * Используется для:
     * - анализа доступности страницы
     * - фильтрации ошибок (404, 500 и т.д.)
     * - соблюдения требований ТЗ
     */
    @Column(nullable = false)
    private int code;

    /**
     * HTML-контент страницы.
     *
     * Хранится полностью, так как используется
     * для последующей лемматизации и поиска.
     *
     * Тип MEDIUMTEXT позволяет хранить большие HTML-документы.
     */
    @Lob
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    /**
     * Заголовок страницы (тег <title>).
     *
     * Используется для:
     * - отображения результатов поиска
     * - повышения релевантности страниц
     */
    @Column(length = 255)
    private String title;
}
