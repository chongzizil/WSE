package edu.nyu.cs.cs2580;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class IndexerInvertedOccurrenceTest extends TestCase {
  String optionStr = "homework2/test/edu/nyu/cs/cs2580/engine_doconlyIndexer_test.conf";
  Indexer indexer;
  SearchEngine.Options OPTIONS;

  @Before
  public void setUp() throws Exception {
    OPTIONS = new SearchEngine.Options(optionStr);
    indexer = Indexer.Factory.getIndexerByOption(OPTIONS);
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void testNextDoc() throws Exception {
    indexer.constructIndex();
    Query query = new Query("alaska purchasedq3wfdewrt");
    query.processQuery();
    Document document1 = indexer.nextDoc(query, -1);
    Document document2 = indexer.nextDoc(query, document1._docid);
    Document document3 = indexer.nextDoc(query, document2._docid);
    Document document4 = indexer.nextDoc(query, document3._docid);
  }
}