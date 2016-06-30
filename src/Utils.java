import java.io.*;
import java.util.*;

import javax.json.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.lang.*;

import edu.washington.cs.knowitall.nlp.*;
import edu.washington.cs.knowitall.extractor.*;
import edu.washington.cs.knowitall.extractor.conf.*;
import edu.washington.cs.knowitall.nlp.extraction.*;

public class Utils {

  public static ReVerbExtractor initExtractor() {
    return new ReVerbExtractor();
  }

  public static ConfidenceFunction initConf() throws IOException {
    return new ReVerbOpenNlpConfFunction();
  }

  public static void shutdown(ExecutorService pool) {
    pool.shutdown();
    try {
      pool.awaitTermination(Main.terminateTime, TimeUnit.DAYS);
    } catch (InterruptedException e) { printError(e); }
  }

  public static void shutdownWriter(BlockingQueue<String> relations, Thread writer) {
    try {
      relations.put(Main.POISON_PILL);
    } catch (InterruptedException e) { 
      exit(e); 
    }

    try {
      writer.join();
    } catch (InterruptedException e) {
      exit(e);
    }
    
  }

  public static void printError(Exception e) {
    System.err.println(e.getMessage());
    e.printStackTrace();
  }

  public static void exit(Exception e) {
    printError(e);
    System.err.println("Exiting...");
    System.exit(1);
  }

  public static String key(int group, int id) {
    return group + "-" + id;
  }

  public static BufferedReader initIn(String inputPath, int group) {
    String name = group + ".in";
    try {
      return new BufferedReader(new FileReader(inputPath + "/" + name));
    } catch (IOException e) {
      exit(e);
    }
    return null;
  }

  public static PrintWriter initOut(String outputPath, int group) {
    String output = group + ".out";
    try {
      return new PrintWriter(outputPath + "/" + output);
    } catch (IOException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    return null;
  }

  public static boolean isCached(String inputPath, int nGroups) {
    File dir = new File(inputPath);
    boolean cached = true;
    if (!dir.exists()) return false;
    for (int i = 0; i < nGroups; i++) {
      System.out.println("Checking " + inputPath + "/" + i + ".in");
      File f = new File(inputPath + "/" + i + ".in");
      if (!f.exists()) {
        System.out.println("Missing file " + inputPath + "/" + i + ".in");
        cached = false;
        break;
      }
    }
    if (new File(inputPath).list().length != nGroups) cached = false;
    if (!cached) for (File file : dir.listFiles()) file.delete();

    return cached;
  }

  public static void distributeInputs(String datasetPath, String workerInput, int total, int nGroups) {
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(datasetPath));
    } catch (IOException e) {
      Utils.exit(e);
    }
    File dir = new File(workerInput);
    dir.mkdir();

    List<PrintWriter> inputWriters = new ArrayList<>();
    for (int i = 0; i < nGroups; i++) {
      PrintWriter w = null;
      try {
        inputWriters.add(new PrintWriter("inputs/" + i + ".in"));
      } catch (Exception e) { Utils.exit(e); }
    }

    long i = 0;
    String line = null;
    while (true) {
      try {
        line = in.readLine();
      } catch (IOException e) { Utils.printError(e); }
      if (line == null) break;
      inputWriters.get((int) i % (inputWriters.size())).println(line);
      i++;
    }
    System.out.println("Finished distributing inputs...now closing input files");
    for (int j = 0; j < nGroups; j++) {
      inputWriters.get(j).close();
    }

    try {
      in.close();
    } catch (IOException e) { Utils.printError(e); }
  } 

  public static JsonObject read(BufferedReader in) {
    String line = null;
    try {
      line = in.readLine();
    } catch (IOException e) {
      Utils.printError(e);
      return null;
    }
    if (line == null) return null;
    JsonReader reader = Json.createReader(new StringReader(line));
    JsonObject object = reader.readObject();
    reader.close();
    return object;
  }

  public static Integer countLines(String filename) {
    Process p = null;
    try {
      p = Runtime.getRuntime().exec("wc -l " + filename);
      p.waitFor();
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(p.getInputStream()));
      String line = reader.readLine();
      return Integer.parseInt(line.split("\\s+")[0]);
    } catch (IOException e) {
      exit(e);
    } catch (InterruptedException e) {
      exit(e);
    }
    return null;
  }

  public static String hostname() {
    Process p = null;
    try {
      p = Runtime.getRuntime().exec("hostname");
      p.waitFor();
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(p.getInputStream()));
      String line = reader.readLine();
      return line.split("\\.")[0];
    } catch (IOException e) {
      exit(e);
    } catch (InterruptedException e) {
      exit(e);
    }
    return null;

  }

  public static String getElapsed() {
    return "[" + TimeUnit.NANOSECONDS.toMinutes(System.nanoTime() - Main.startTime) + " min(s) elapsed]";
  }

  public static void printFailed(int failed) {
    if (failed % Main.logFrequency == 0) {
      System.out.println(getElapsed() + failed + " number of executions have failed.");
    }
  }

}
