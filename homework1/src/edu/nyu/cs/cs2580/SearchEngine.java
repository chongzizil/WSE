package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.HttpServer;

public class SearchEngine {
  private static final Logger logger = LogManager.getLogger(SearchEngine.class);

  // @CS2580: please use a port number 258XX, where XX corresponds
  // to your group number.
  public static void main(String[] args) throws IOException {
    logger.debug("Search engine server starting...");
    // Create the server.
    if (args.length < 2) {
      System.out
          .println("arguments for this program are: [PORT] [PATH-TO-CORPUS]");
      return;
    }
    int port = Integer.parseInt(args[0]);
    String indexPath = args[1];
    InetSocketAddress addr = new InetSocketAddress(port);
    HttpServer server = HttpServer.create(addr, -1);

    // Attach specific paths to their handlers.
    server.createContext("/search", new QueryHandler(indexPath));
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
    System.out.println("Listening on port: " + Integer.toString(port));
    logger.debug("Search engine server listening...");
  }
}
