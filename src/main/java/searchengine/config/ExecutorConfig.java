package searchengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ExecutorConfig {

    /**
     * Создаёт Executor для индексации сайтов
     *
     * @return Executor с настройками пула потоков
     */
    @Bean(name = "indexingExecutor")
    public Executor indexingExecutor() {
        // Используем ThreadPoolTaskExecutor для управления пулом потоков
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Минимальное количество потоков, которые всегда будут активны
        executor.setCorePoolSize(4);

        // Максимальное количество потоков, которое может быть создано при нагрузке
        executor.setMaxPoolSize(12);

        // Вместимость очереди задач перед созданием новых потоков
        executor.setQueueCapacity(100);

        // Префикс имени для потоков (удобно при логах)
        executor.setThreadNamePrefix("indexing");

        // Инициализация пула
        executor.initialize();

        return executor;
    }
}

