import java.io.*;
import java.util.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.naturalli.*;
import edu.stanford.nlp.ie.*;
import edu.stanford.nlp.ie.util.*;


import edu.stanford.nlp.hcoref.data.CorefChain;
import edu.stanford.nlp.hcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ie.machinereading.structure.EntityMention;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;
import edu.stanford.nlp.ie.machinereading.structure.RelationMention;
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.naturalli.OpenIE;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

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
    Annotation annotation = new Annotation(obj.getString("text"));
    Main.pipeline.annotate(annotation);
    JsonObjectBuilder article = buildJson(annotation);
    try {
      relations_queue.put(article.build().toString());
    } catch (InterruptedException e) {
      int failed = Main.failed.incrementAndGet();
      Utils.printFailed(failed);
      Main.backlog.release();
    }

  }

  private JsonObjectBuilder buildJson(Annotation annotation) {
    List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class); 
    JsonObjectBuilder article = Json.createObjectBuilder();
    for (int i = 0; i < sentences.size(); i++) {
      JsonArrayBuilder sentenceRelations = Json.createArrayBuilder();
       
      CoreMap sentence = sentences.get(i);
      String text = sentence.get(CoreAnnotations.TextAnnotation.class);
      sentenceRelations.add(Json.createObjectBuilder().add("s", text));

      List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
      Collection<RelationTriple> openieTriples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
      if (openieTriples != null && openieTriples.size() > 0) {
        for (RelationTriple triple : openieTriples) {
          sentenceRelations.add(Json.createObjectBuilder()
              .add("r", Utils.tripleToString(triple)));
        }
      }
      article.add(i + "", sentenceRelations);
    }
    article.add("length", sentences.size() + "");
    article.add("articleId", obj.getInt("articleId") + "");
    article.add("date", obj.getString("date"));
    return article;
  }

}
