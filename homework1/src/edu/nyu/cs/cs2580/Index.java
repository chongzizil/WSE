package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

class Index {
  public Vector<Document> _documents;

  public Index(String index_source) {
    System.out.println("Indexing documents ...");

    _documents = new Vector<Document>();
    try {
      BufferedReader reader = new BufferedReader(new FileReader(index_source));
      try {
        String line = null;
        int did = 0;
        while ((line = reader.readLine()) != null) {
          Document d = new Document(did, line);
          _documents.add(d);
          did++;
        }
      } finally {
        reader.close();
      }
    } catch (IOException ioe) {
      System.err.println("Oops " + ioe.getMessage());
    }
    System.out.println("Done indexing " + Integer.toString(_documents.size())
        + " documents...");
  }

  public int documentFrequency(String term) {
    return Document.documentFrequency(term);
  }

  public int termFrequency(String term) {
    return Document.termFrequency(term);
  }

  public int termFrequency() {
    return Document.termFrequency();
  }

  public int numTerms() {
    return Document.numTerms();
  }

  public String getTerm(int index) {
    return Document.getTerm(index);
  }

  public int numDocs() {
    return _documents.size();
  }

  public Document getDoc(int did) {
    return (did >= _documents.size() || did < 0) ? null : _documents.get(did);
  }
}
