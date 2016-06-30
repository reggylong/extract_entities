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
  private BlockingQueue<String> relations_queue;

  Extractor(BlockingQueue<String> relations_queue, JsonObject obj) {
    this.obj = obj;
    // Used to feed input to ExtractionWriter
    this.relations_queue = relations_queue;
  }

  public void run() {
    ReVerbExtractor reverb = null;
    OpenNlpSentenceChunker chunker = null;
    ConfidenceFunction confFunc = null;
    try {
      chunker = new OpenNlpSentenceChunker();
      confFunc = Utils.initConf();
      reverb = Utils.initExtractor();
    } catch (IOException e) {
      System.err.println("Unable to initialize pipeline");
      Utils.printError(e);
      Main.backlog.release();
      return;
    }

    Document doc = new Document(obj.getString("text"));

    List<Sentence> sentences = doc.sentences();
    JsonObjectBuilder article = Json.createObjectBuilder();
    for (int i = 0; i < sentences.size(); i++) {
      JsonArrayBuilder sentenceRelations = Json.createArrayBuilder();
      ChunkedSentence sent = chunker.chunkSentence(sentences.get(i).toString());
      for (ChunkedBinaryExtraction extr : reverb.extract(sent)) {
        sentenceRelations.add(Json.createObjectBuilder()
            .add("relation", getTuple(confFunc, extr)));
      }
      article.add(i + "", sentenceRelations);
    }
    article.add("length", sentences.size() + "");
    article.add("articleId", obj.getInt("articleId") + "");
    article.add("date", obj.getString("date"));
    try {
      relations_queue.put(article.build().toString());
    } catch (InterruptedException e) {
      int failed = Main.failed.incrementAndGet();
      Utils.printFailed(failed);
      Main.backlog.release();
    }

  }

  private String getTuple(ConfidenceFunction confFunc, ChunkedBinaryExtraction extr) {
    return "(" + extr.getArgument1() + ", " + extr.getRelation() + ", " + extr.getArgument2() + ", " + confFunc.getConf(extr) + ")";
  }

}
