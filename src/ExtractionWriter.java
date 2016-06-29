import java.io.*;
import java.util.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.lang.*;

class ExtractionWriter implements Runnable {
 
  private final BlockingQueue<Pair<String, String>> relations_queue;
  private final PrintWriter writer;
  private long count = 0;
  

  // <article id, relations>
  ExtractionWriter(BlockingQueue<Pair<String,String>> relations_queue, PrintWriter writer) {
    this.relations_queue = relations_queue; 
    this.writer = writer;
  }

  public void run() {
    while (true) {
      Pair<String,String> relations = null;
      try {
        relations = relations_queue.take();
      } catch (InterruptedException e) {
        Utils.printError(e);
        continue;
      }
      if (relations == Main.POISON_PILL) return;
      writer.println(relations.first);
      writer.println(relations.second);
      writer.println();
      count++;
      if (count % Main.logFrequency == 0) {
        System.out.println(Utils.getElapsed() + count + " written examples");
      }
    }
  }
}

