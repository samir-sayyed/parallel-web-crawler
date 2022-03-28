package com.udacity.webcrawler.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Utility class to write a {@link CrawlResult} to file.
 */
public final class CrawlResultWriter {
  private final CrawlResult result;

  /**
   * Creates a new {@link CrawlResultWriter} that will write the given {@link CrawlResult}.
   */
  public CrawlResultWriter(CrawlResult result) {
    this.result = Objects.requireNonNull(result);
  }

  /**
   * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Path}.
   *
   * <p>If a file already exists at the path, the existing file should not be deleted; new data
   * should be appended to it.
   *
   * @param path the file path where the crawl result data should be written.
   */
  public void write(Path path) {
    try (Writer file = Files.newBufferedWriter(path)){
//      Writer file = Files.newBufferedWriter(path);
      file.flush();
    }catch (Exception e){
      System.out.println(e.getMessage());
    }
  }

  /**
   * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Writer}.
   *
   * @param writer the destination where the crawl result data should be written.
   */
  @JsonDeserialize(builder = CrawlResult.Builder.class)
  public void write(Writer writer) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
      objectMapper.writeValue(writer, this.result);
    }catch (Exception e){
//      System.out.println("aaaaaaaaaaaaa");
      System.out.println(e.getMessage());
    }
  }
}
