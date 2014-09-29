package edu.nyu.cs.cs2580;

import java.util.Comparator;
import java.util.Vector;

public class NumviewsRanker implements BaseRanker {
  private Index _index;

  public NumviewsRanker(String index_source) {
    _index = new Index(index_source);
  }

  @Override
  public Vector<ScoredDocument> runQuery(String query) {
    Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();

    for (int docId = 0; docId < _index.numDocs(); ++docId) {
      Document document = _index.getDoc(docId);
      retrieval_results.add(new ScoredDocument(docId,
          document.get_title_string(), document.get_numviews()));
    }

    WSEUtil.sortScore(retrieval_results);

    return retrieval_results;
  }
}