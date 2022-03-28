package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;
  private final PageParserFactory parserFactory;

  @Inject
  ParallelWebCrawler(
          Clock clock,
          @Timeout Duration timeout,
          @PopularWordCount int popularWordCount,
          @TargetParallelism int threadCount,
          @MaxDepth int maxDepth,
          @IgnoredUrls List<Pattern> ignoredUrls,
          PageParserFactory parserFactory) {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
    this.parserFactory = parserFactory;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);
    Map<String, Integer> wordCounts = Collections.synchronizedMap(new HashMap<>());
    Set<String> visitedUrls =  Collections.synchronizedSet(new HashSet<>());

    for (String url : startingUrls){
//      System.out.println(url+" "+visitedUrls);
      pool.invoke(new webCrawlerRecursive(wordCounts, deadline, visitedUrls, maxDepth, url) );
    }

    if (wordCounts.isEmpty()){
      return new CrawlResult.Builder()
              .setWordCounts(wordCounts)
              .setUrlsVisited(visitedUrls.size())
              .build();
    }


    return new CrawlResult.Builder()
            .setWordCounts(WordCounts.sort(wordCounts,popularWordCount))
            .setUrlsVisited(visitedUrls.size())
            .build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }


  public class webCrawlerRecursive extends RecursiveAction {
    private final Map<String, Integer> wordCounts;
    private final Instant deadline;
    private final Set<String> visitedUrls;
    private final  int maxDepth;
    private final String url;
    private final Lock lock = new ReentrantLock();


    public webCrawlerRecursive(Map<String, Integer> wordCounts, Instant deadline, Set<String> visitedUrls,  int maxDepth, String url) {
      this.wordCounts = wordCounts;
      this.deadline = deadline;
      this.visitedUrls = visitedUrls;
      this.maxDepth = maxDepth;
      this.url = url;

//      System.out.println(url+" "+visitedUrls);
    }

    @Override
      public void compute(){
          if (maxDepth == 0 || clock.instant().isAfter(deadline)){
            return;
          }

          for (Pattern pattern : ignoredUrls){
            if (pattern.matcher(url).matches()){
              return;
            }
          }
          try {
            lock.lock();
            if (visitedUrls.contains(url)) {
              // return empty result
              return;
            }
            visitedUrls.add(url);
            // process URL and return
          } finally {
            lock.unlock();
          }

//          if (visitedUrls.contains(url)){
//            return;
//          }
//
//          visitedUrls.add(url);

        PageParser.Result result = parserFactory.get(url).parse();

        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()){
          if (wordCounts.containsKey(e.getKey())) {
            wordCounts.put(e.getKey(), e.getValue() + wordCounts.get(e.getKey()));
          } else {
            wordCounts.put(e.getKey(), e.getValue());
          }
        }

  //      Collection<webCrawlerRecursive> tasks = new ArrayList<>();
        for (String subUrls : result.getLinks()){
        invokeAll(new webCrawlerRecursive(wordCounts, deadline, visitedUrls, maxDepth-1, subUrls));
        }
  //      invokeAll(tasks);
    }
   }
  }


