package edu.nyu.cs.cs2580.rankers;

import edu.nyu.cs.cs2580.SearchEngine.Options;
import edu.nyu.cs.cs2580.document.Document;
import edu.nyu.cs.cs2580.document.DocumentNews;
import edu.nyu.cs.cs2580.document.ScoredDocument;
import edu.nyu.cs.cs2580.handler.CgiArguments;
import edu.nyu.cs.cs2580.index.Indexer;
import edu.nyu.cs.cs2580.index.IndexerInvertedCompressed;
import edu.nyu.cs.cs2580.preprocess.FilePreprocess;
import edu.nyu.cs.cs2580.query.Query;

import java.util.*;

/**
 * Created by tanis on 12/15/14.
 */
public class NewsRankerComprehensive extends Ranker {
  private final static double LAMDA = 0.50;
  IndexerInvertedCompressed indexerInvertedCompressed;

  public NewsRankerComprehensive(Options options, CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    this.indexerInvertedCompressed = (IndexerInvertedCompressed) this._indexer;
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    System.out.println("Running query...");
    Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
    int nextDocid = -1;

    while (true) {
      Document document = indexerInvertedCompressed.nextDoc(query,
          nextDocid);
      if (document == null) {
        break;
      }

      rankQueue.add(scoreDocument(query, document._docid));
      nextDocid = document._docid;
      if (rankQueue.size() > numResults) {
        rankQueue.poll();
      }
    }

    Vector<ScoredDocument> results = new Vector<ScoredDocument>();
    ScoredDocument scoredDoc = null;

    while ((scoredDoc = rankQueue.poll()) != null) {
      results.add(scoredDoc);
    }

    Collections.sort(results, Collections.reverseOrder());

    return results;
  }

  /**
   * Score the document...
   *
   * @param query the query
   * @param docId document ID
   * @return a ScoreDocument
   */
  public ScoredDocument scoreDocument(Query query, int docId) {
    ScoredDocument scoredDocument = null;
    // C is the total number of word occurrences in the collection.
    long C = indexerInvertedCompressed.totalTermFrequency();

    // Query vector
    List<String> queryList = new ArrayList<String>();
    for (String term : query._tokens) {
      queryList.add(term);
    }

    DocumentNews document = (DocumentNews) indexerInvertedCompressed.getDoc(docId);

    // Score the document. Here we have provided a very simple ranking model,
    // where a document is scored 1.0 if it gets hit by at least one query
    // term.
    double score = 0.0;

    for (int i = 0; i < queryList.size(); ++i) {
      String qi = queryList.get(i);

      // fqi_D is the number of times word qi occurs in document D.
      int fqi_D = indexerInvertedCompressed.documentTermFrequency(qi,
          document._docid);
      // cqi is the number of times a query word occurs in the collection of
      // documents
      int cqi = indexerInvertedCompressed.corpusDocFrequencyByTerm(qi);
      // D is the number of words in D.
      double D = document.getTotalDocTerms();

      score += Math.log((1 - LAMDA) * (fqi_D / D) + LAMDA * (cqi / C));
    }

    score = Math.exp(score);

    // Considered page rank scores...
    if (document.getPageRank() > 0) {
      score = score * document.getPageRank();
    }

    if (indexerInvertedCompressed.getTotalNumViews() > 0) {
      score = score + score * 0.1 * ((double) document.getNumViews() / (double) indexerInvertedCompressed.getTotalNumViews());
    }

    // Check for title
    if (indexerInvertedCompressed.isQueryInTitle(query, docId)) {
      score *= 1.5;
    }

    int timeLength = 15;
    Date time = document.getTime();
    if (time.compareTo(FilePreprocess.dates[0]) < 0) {
      timeLength = 15;
    } else {
      for (int i = 1; i < FilePreprocess.dates.length; i++) {
        if (time.compareTo(FilePreprocess.dates[i]) < 0) {
          timeLength -= i;
          break;
        }
        if (i == FilePreprocess.dates.length - 1) {
          timeLength -= i;
        }
      }
    }

    score = score / (Math.log(1.95 + timeLength * .05) / Math.log(2));

    scoredDocument = new ScoredDocument(document, score, document.getPageRank(), document.getNumViews());

    return scoredDocument;
  }
}
