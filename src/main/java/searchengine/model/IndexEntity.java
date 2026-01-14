package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Сущность IndexEntity описывает индексную запись,
 * связывающую страницу и лемму.
 *
 * Данная таблица является связующей (junction table)
 * между PageEntity и LemmaEntity и используется
 * для реализации поискового индекса.
 *
 * Каждая запись показывает:
 *  - какая лемма встречается на какой странице
 *  - с какой значимостью (rank)
 */
@Entity
/**
 * Уникальное ограничение гарантирует,
 * что одна и та же лемма не будет
 * дважды привязана к одной странице.
 */
@Table(name = "page_index", uniqueConstraints = {@UniqueConstraint(columnNames = {"page_id", "lemma_id"})})
@Getter
@Setter
@NoArgsConstructor
public class IndexEntity {

    /**
     * Уникальный идентификатор индексной записи.
     * Генерируется автоматически.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Страница, на которой встречается лемма.
     *
     * Связь Many-to-One:
     *  - одна страница содержит множество лемм
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private PageEntity page;

    /**
     * Лемма, присутствующая на странице.
     *
     * Связь Many-to-One:
     *  - одна лемма может встречаться на множестве страниц
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private LemmaEntity lemma;

    /**
     * Ранг (вес) леммы на конкретной странице.
     *
     * Показывает, насколько часто и значимо
     * данная лемма встречается в тексте страницы.
     *
     * Используется при расчёте релевантности
     * страниц в поисковой выдаче.
     */
    @Column(name = "`rank`", nullable = false)
    private float rank;
}
