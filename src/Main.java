import java.io.*;
import java.util.*;

import edu.washington.cs.knowitall.extractor.ReVerbExtractor;
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
  public static int nGroups = 10;
  public static final int nWorkers = 28;
  public static int group = 0;
  public static final AtomicInteger count = new AtomicInteger(0);
  public static final AtomicInteger failed = new AtomicInteger(0);
  public static final AtomicInteger malformed = new AtomicInteger(0);
  public static int timeout = 30;
  public static final int terminateTime = 7;
  public static long startTime; 
  public static final Pair<String, String> POISON_PILL = new Pair<>("", "");
  // # of examples
  public static final int logFrequency = 10;

  public static void main(String[] args) throws IOException {
    startTime = System.nanoTime();
    if (args.length > 0) {
      group = Integer.parseInt(args[0]);
    }
    outputPath = "/" + Utils.hostname() + "/scr1/reglong/entities_output/";
    File dir = new File(outputPath);
    dir.mkdirs();
    redirect(); 

    System.out.println(Utils.hostname());
    System.out.println("Determining size of dataset");
    int lines = Utils.countLines(dataset);

    if (!Utils.isCached(inputPath, nGroups)) {
      System.out.println("Distributing inputs");
      Utils.distributeInputs(dataset, inputPath, lines, nGroups);
    } else {
      System.out.println("Using cached inputs...");
    }
    System.out.println("Processing articles...");
    processAnnotations(group);
    System.out.println("Finished processing articles");
  }

  private static void redirect() {
    String logPath = "logs";
    File dir = new File(logPath);
    dir.mkdir();

    try {
      PrintStream out = new PrintStream(new FileOutputStream(logPath + "/" + group + ".out"));
      PrintStream err = new PrintStream(new FileOutputStream(logPath + "/" + group + ".err"));
      System.setOut(out);
      System.setErr(err);
    } catch (FileNotFoundException e) {
      Utils.exit(e);
    }
  }

  private static void processAnnotations(int group) {
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
    BlockingQueue<Pair<String, String>> relations = new LinkedBlockingQueue<>();
    BlockingQueue<ReVerbExtractor> resources = new ArrayBlockingQueue<>(nWorkers);
    for (int i = 0; i < nWorkers; i++) {
      resources.put(Utils.initExtractor());
    }

    Thread writer = new Thread(new ExtractionWriter(relations, w));
    writer.start();

    BufferedReader in = Utils.initIn(inputPath, group);

    JsonObject obj = null;
    while ( (obj = Utils.read(in)) != null) {
      try {
        String id = obj.getInt("articleId") + ""; 
        Runnable runner = new Extractor(relations, obj);
        scheduler.submit(new TimeoutRunner(pool, id, failedW, runner, timeout));
      } catch (Exception e) { Utils.printError(e); }
    }

    Utils.shutdown(scheduler);
    Utils.shutdown(pool);
    Utils.shutdownWriter(relations, writer);

    IOUtils.closeIgnoringExceptions(w);
    IOUtils.closeIgnoringExceptions(failedW);
  }
}
