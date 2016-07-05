import java.io.*;
import java.util.*;

import edu.stanford.nlp.hcoref.data.CorefChain;
import edu.stanford.nlp.hcoref.CorefCoreAnnotations;
import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.*;

import edu.washington.cs.knowitall.nlp.ChunkedSentence;
import edu.washington.cs.knowitall.nlp.OpenNlpSentenceChunker;
import edu.washington.cs.knowitall.extractor.*;
import edu.washington.cs.knowitall.extractor.conf.*;
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction;
import edu.stanford.nlp.io.*;
import javax.json.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;
import java.lang.*;

public class Main {

  public static final String dataset = "release/crawl";
  public static final String inputPath = "inputs";
  public static String outputPath;
  public static final String failedPath = "failed";
  public static int nGroups = 15;
  public static final int nWorkers = 7;
  public static int group = 0;
  public static final AtomicInteger count = new AtomicInteger(0);
  public static final AtomicInteger failed = new AtomicInteger(0);
  public static final AtomicInteger malformed = new AtomicInteger(0);
  public static int timeout = 600;
  public static final int terminateTime = 7;
  public static long startTime; 
  // no clean way to prevent string interning
  public static final String POISON_PILL = "POISON PILL END WRITING QUEUE ";
  // # of examples
  public static final int logFrequency = 10;
  public static final int MAX_BACKLOG = 20;
  public static final Semaphore backlog = new Semaphore(MAX_BACKLOG);


  public static void main(String[] args) throws IOException {
    startTime = System.nanoTime();
    if (args.length > 0) {
      group = Integer.parseInt(args[0]);
    }
    outputPath = "/" + Utils.hostname() + "/scr1/reglong/extract_outputs/";
    File dir = new File(outputPath);
    dir.mkdirs();
    //redirect(); 

    StanfordCoreNLP pipeline = Utils.initPipeline();
    Annotation annotation = new Annotation("Obama was born in Hawaii. He is our president");
    pipeline.annotate(annotation);

    List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
    /*
    if (sentences != null) {
      for (int i = 0; i < sentences.size(); i++) {
        CoreMap sentence = sentences.get(i);
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        String text = sentence.get(CoreAnnotations.TextAnnotation.class);
        System.out.println("Text: " + text);
        Collection<RelationTriple> openieTriples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
        if (openieTriples != null && openieTriples.size() > 0) {
          System.out.println("Extracted the following triples");
          for (RelationTriple triple : openieTriples) {
            System.out.println(tripletoString(triple));
          }
        }
      }
    }*/










    System.exit(0);
    System.out.println(Utils.hostname());
    System.out.println(Utils.getDate());
    System.out.println("Determining size of dataset");
    int lines = Utils.countLines(dataset);

    if (!Utils.isCached(inputPath, nGroups)) {
      System.out.println("Distributing inputs");
      Utils.distributeInputs(dataset, inputPath, lines, nGroups);
      System.out.println("Finished distributing inputs");
    } else {
      System.out.println("Using cached inputs...");
    }
    System.out.println("Processing articles...");
    processArticles(group);
    System.out.println("Finished processing articles");
  }

  private static void redirect() {
    String logPath = "/" + Utils.hostname() + "/scr1/reglong/extract_logs/";
    File dir = new File(logPath);
    dir.mkdirs();

    try {
      PrintStream out = new PrintStream(new FileOutputStream(logPath + "/" + group + ".out"));
      PrintStream err = new PrintStream(new FileOutputStream(logPath + "/" + group + ".err"));
      System.setOut(out);
      System.setErr(err);
    } catch (FileNotFoundException e) {
      Utils.exit(e);
    }
  }

  private static void processArticles(int group) {
    System.out.println("Determining lines of work");
    int lines = Utils.countLines(inputPath + "/" + group + ".in");
    System.out.println(lines + " examples");
    File dir = new File(outputPath);
    dir.mkdir();

    File failedDir = new File(failedPath);
    failedDir.mkdir();

    ExecutorService scheduler = Executors.newFixedThreadPool(nWorkers);

    ExecutorService pool = Executors.newFixedThreadPool(nWorkers);

    PrintWriter w = new PrintWriter(Utils.initOut(outputPath, group));
    PrintWriter failedW = new PrintWriter(Utils.initOut(failedPath, group));
    BlockingQueue<String> relations = new LinkedBlockingQueue<>();
    Thread writer = new Thread(new ExtractionWriter(relations, w));
    writer.start();

    BufferedReader in = Utils.initIn(inputPath, group);

    ReVerbExtractor reverb = null;
    OpenNlpSentenceChunker chunker = null;
    ConfidenceFunction confFunc = null;
    try {
      reverb = new ReVerbExtractor();
      chunker = new OpenNlpSentenceChunker();
      confFunc = new ReVerbOpenNlpConfFunction();
    } catch (IOException e) {
      Utils.exit(e);
    }

    JsonObject obj = null;
    while ( (obj = Utils.read(in)) != null) {
      String id = "";
      try {
        // Check fields exist
        id = obj.getInt("articleId") + "";
        String text = obj.getString("text");
        String date = obj.getString("date");
      } catch (Exception e) {
        Utils.printError(e);
        int malformedCount = malformed.incrementAndGet();
        if (malformedCount % logFrequency == 0) {
          System.out.println(malformedCount + " number of malformed examples");
        }
        continue;
      }
      Runnable runner = new Extractor(relations, reverb, chunker, confFunc, obj);
      scheduler.submit(new TimeoutRunner(pool, id, failedW, runner, timeout));
    }

    Utils.shutdown(scheduler);
    Utils.shutdown(pool);
    Utils.shutdownWriter(relations, writer);

    IOUtils.closeIgnoringExceptions(w);
    IOUtils.closeIgnoringExceptions(failedW);
  }
}
