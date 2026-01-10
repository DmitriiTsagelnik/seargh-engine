import java.sql.*;
import java.util.*;

public class SearchTest {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/search_engine?useSSL=false&serverTimezone=UTC";
        String user = "root";
        String password = "1524314Chest";

        String lemmaToSearch = "тупица";

        try {
            // Регистрация драйвера MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(url, user, password)) {

                String query = """
                    SELECT p.id AS pageId, p.path AS pagePath, s.url AS siteUrl
                    FROM page_index pi
                    JOIN lemma l ON pi.lemma_id = l.id
                    JOIN page p ON pi.page_id = p.id
                    JOIN site s ON p.site_id = s.id
                    WHERE l.lemma = ?
                """;

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, lemmaToSearch);

                    try (ResultSet rs = stmt.executeQuery()) {
                        Map<String, List<String>> sitePages = new HashMap<>();

                        while (rs.next()) {
                            String pagePath = rs.getString("pagePath");
                            String siteUrl = rs.getString("siteUrl");

                            sitePages.computeIfAbsent(siteUrl, k -> new ArrayList<>()).add(pagePath);
                        }

                        // Вывод результатов
                        sitePages.forEach((site, pages) -> {
                            System.out.println("Сайт: " + site);
                            pages.forEach(page -> System.out.println("  Страница: " + page));
                        });
                    }
                }
            }

        } catch (ClassNotFoundException e) {
            System.err.println("MySQL драйвер не найден!");
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}