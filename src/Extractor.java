import java.io.*;
import java.util.*;

import edu.washington.cs.knowitall.nlp.ChunkedSentence;
import edu.washington.cs.knowitall.nlp.OpenNlpSentenceChunker;
import edu.washington.cs.knowitall.extractor.*;
import edu.washington.cs.knowitall.extractor.conf.*;
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction;

import javax.json.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;
import java.lang.*;
import edu.stanford.nlp.simple.*;

class Extractor implements Runnable {
  private JsonObject obj;
  private BlockingQueue<Pair<String, String>> relations_queue;

  Extractor(BlockingQueue<Pair<String,String>> relations_queue, JsonObject obj) {
    this.obj = obj;
    // Used to feed input to ExtractionWriter
    this.relations_queue = relations_queue;
  }

  public void run() {
    if (obj.getJsonString("text") != null && obj.getJsonString("date") != null) {
      Document doc = new Document(obj.getString("text"));
      StringBuilder builder = new StringBuilder();
      OpenNlpSentenceChunker chunker = new OpenNlpSentenceChunker();
      for (Sentence sent : doc.sentences()) {

      }
      try {
        relations_queue.put(new Pair<>(obj.getInt("articleId") + " " + obj.getString("date"), builder.toString()));
      } catch (InterruptedException e) {
        int failed = Main.failed.incrementAndGet();
        Utils.printFailed(failed);
      }
    } else {
      int malformed = Main.malformed.incrementAndGet();
      if (malformed % Main.logFrequency == 0) {
        System.out.println(malformed + " number of malformed examples");
      }
    }
  }

}
