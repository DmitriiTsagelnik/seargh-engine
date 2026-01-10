import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class TestCrawl {

    static class CrawlTask extends RecursiveAction {
        private final String url;
        private final int depth;
        private final int maxDepth;

        CrawlTask(String url, int depth, int maxDepth) {
            this.url = url;
            this.depth = depth;
            this.maxDepth = maxDepth;
        }

        @Override
        protected void compute() {
            System.out.println("compute() called: url=" + url + ", depth=" + depth);
            if (depth >= maxDepth) return;
            CrawlTask t1 = new CrawlTask(url + "/a", depth + 1, maxDepth);
            CrawlTask t2 = new CrawlTask(url + "/b", depth + 1, maxDepth);
            invokeAll(t1, t2);
        }
    }

    public static void main(String[] args) {
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new CrawlTask("http://example.com", 0, 2));
    }
}