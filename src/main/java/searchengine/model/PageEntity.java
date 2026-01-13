package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "page", indexes = {
        @Index(name = "index_page_path", columnList = "path")
})
@Getter
@Setter
@NoArgsConstructor
public class PageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @Column(length = 255, nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @Column(length = 255)
    private String title;
}
