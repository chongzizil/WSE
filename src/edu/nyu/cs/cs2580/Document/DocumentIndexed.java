package edu.nyu.cs.cs2580.document;

import java.util.Vector;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document {
  private static final long serialVersionUID = 9184892508124423115L;
  private Vector<String> _links;

  public DocumentIndexed(int docid) {
    super(docid);
    _links = new Vector<String>();
  }

  public Vector<String> get_links() {
    return _links;
  }

  public void set_links(Vector<String> _links) {
    this._links = _links;
  }
}
