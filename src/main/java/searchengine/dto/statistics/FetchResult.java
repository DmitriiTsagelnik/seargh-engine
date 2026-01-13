package searchengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FetchResult {
    private int statusCode;
    private String html;
    private String path;
}
