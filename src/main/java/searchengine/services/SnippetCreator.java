package searchengine.services;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SnippetCreator {

    private static final int MAX_SNIPPET_CHARS = 200;

    public String createSnippet(String htmlContent, Set<String> lemmas) {
        if (htmlContent == null || htmlContent.isBlank() || lemmas.isEmpty()) return "";

        String text = Jsoup.parse(htmlContent).text();
        String lowerText = text.toLowerCase();

        int firstIndex = -1;
        String foundLemma = null;
        for (String lemma : lemmas) {
            int idx = lowerText.indexOf(lemma.toLowerCase());
            if (idx >= 0 && (firstIndex == -1 || idx < firstIndex)) {
                firstIndex = idx;
                foundLemma = lemma;
            }
        }

        int start = 0;
        int end = Math.min(text.length(), MAX_SNIPPET_CHARS);
        if (firstIndex >= 0) {
            start = Math.max(0, firstIndex - MAX_SNIPPET_CHARS / 2);
            end = Math.min(text.length(), start + MAX_SNIPPET_CHARS);
        }

        String snippet = text.substring(start, end);

        for (String lemma : lemmas) {
            snippet = snippet.replaceAll(
                    "(?iu)(" + Pattern.quote(lemma) + ")",
                    "<b>$1</b>"
            );
        }

        if (start > 0) snippet = "..." + snippet;
        if (end < text.length()) snippet = snippet + "...";

        return snippet;
    }
}
