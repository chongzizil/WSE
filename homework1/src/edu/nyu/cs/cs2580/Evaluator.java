package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class Evaluator {

  private static final Logger logger = LogManager.getLogger(Evaluator.class);
  private static final double[] STANDARD_RECALL_LEVELS = new double[]{0.0,
      0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
  private static final int[] K = new int[]{1, 5, 10};
  private static final double ALPHA = 0.50;

  public static void main(String[] args) throws IOException {
    // <String: query, <Integer: documentID, RelevancePair: binary relevance and categorical relevance pair>>
    Map<String, HashMap<Integer, RelevancePair>> relevanceJudgments = new HashMap<String, HashMap<Integer, RelevancePair>>();
    if (args.length < 1) {
      System.out.println("Need to provide relevanceJudgments...");
      return;
    }
    String judgePath = args[0];

    // First read the relevance judgments into the map
    readRelevanceJudgments(judgePath, relevanceJudgments);

    // now evaluate the results from stdin
    String output = evaluateStdin(relevanceJudgments);

    // Write a file
    Utility.WriteToFile(output, "hw1.3-unknownRanker.tsv", false);

    // Print it in the terminal...
    System.out.printf(output);
  }

  public static void readRelevanceJudgments(String judgePath,
                                            // <String: query, <Integer: documentID, RelevancePair: binary relevance and categorical relevance pair>>
                                            Map<String, HashMap<Integer, RelevancePair>> relevanceJudgments) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(judgePath));
      try {
        String line;
        while ((line = reader.readLine()) != null) {
          // parse the query,docId,relevance line
          Scanner s = new Scanner(line).useDelimiter("\t");
          String query = s.next();
          int docId = Integer.parseInt(s.next());
          String grade = s.next();

          if (!relevanceJudgments.containsKey(query)) {
            HashMap<Integer, RelevancePair> tmpMap = new HashMap<Integer, RelevancePair>();
            RelevancePair relevancePair = getRelevancePair(grade);
            tmpMap.put(docId, relevancePair);
            relevanceJudgments.put(query, tmpMap);
          } else {
            RelevancePair relevancePair = getRelevancePair(grade);
            relevanceJudgments.get(query).put(docId, relevancePair);
          }
        }
      } finally {
        reader.close();
      }
    } catch (IOException ioe) {
      System.err.println("Oops " + ioe.getMessage());
    }
  }

  // Return the relevance pair generated by the grade
  private static RelevancePair getRelevancePair(String grade) {
    double rel = 0.0;
    double crel = 0.0;

    // Get the relevance points. Both binary and category
    if ((grade.equals("Perfect")) || (grade.equals("Excellent"))
        || (grade.equals("Good"))) {
      rel = 1.0;
      if (grade.equals("Perfect")) {
        crel = 10.0;
      } else if (grade.equals("Excellent")) {
        crel = 7.0;
      } else {
        crel = 5.0;
      }
    } else if (grade.equals("Fair")) {
      crel = 1.0;
    } else {
      crel = 0.0;
    }

    return new RelevancePair(rel, crel);
  }

  // Return the precisions with K = {1, 5, 10}
  private static Map<Integer, Double> getPrecisionMap(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    // <K, Precision>
    Map<Integer, Double> precisionMap = new HashMap<Integer, Double>();

    for (int i = 0; i < K.length; i++) {
      int k = K[i];
      double rr = 0.0;
      double res = 0.0;

      for (int j = 0; j < k; j++) {
        int docId = rankedList.get(j);
        if (relevanceJudgments.containsKey(docId) && relevanceJudgments.get(docId).getBinaryRelevance() == 1) {
          rr++;
        }
      }

      res = rr / k;
      if (Double.isNaN(res)) {
        res = 0.0;
      }

      precisionMap.put(k, res);
    }

    return precisionMap;
  }

  // Return the recalls with K = {1, 5, 10}
  private static Map<Integer, Double> getRecallMap(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    // <K, Recall>
    Map<Integer, Double> recallMap = new HashMap<Integer, Double>();
    int countOfRelevant = 0;

    for (Entry entry : relevanceJudgments.entrySet()) {
      RelevancePair relevancePair = (RelevancePair) entry.getValue();
      if (relevancePair.getBinaryRelevance() == 1) {
        countOfRelevant++;
      }
    }

    for (int i = 0; i < K.length; i++) {
      int k = K[i];
      double rr = 0.0;
      double res = 0.0;
      for (int j = 0; j < k; j++) {
        int docId = rankedList.get(j);
        if (relevanceJudgments.containsKey(docId) && relevanceJudgments.get(docId).getBinaryRelevance() == 1) {
          rr++;
        }
      }

      res = rr / countOfRelevant;

      if (Double.isNaN(res)) {
        res = 0.0;
      }

      recallMap.put(k, res);
    }

    return recallMap;
  }

  // Return the Fs with K = {1, 5, 10}
  private static Map<Integer, Double> getFMap(Map<Integer, Double> precisionMap, Map<Integer, Double> recallMap) {
    // <K, F>
    Map<Integer, Double> FMap = new HashMap<Integer, Double>();

    for (int i = 0; i < K.length; i++) {
      int k = K[i];
      double p = precisionMap.get(k);
      double r = recallMap.get(k);
      double f = 1 / (ALPHA * (1 / p) + (1 - ALPHA) * (1 / r));

      if (Double.isNaN(f)) {
        f = 0.0;
      }

      FMap.put(k, f);
    }

    return FMap;
  }

  //Return the average precision
  private static double getAveragePrecision(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    int lengthOfRankedList = rankedList.size();
    double ap = 0.0;
    double rr = 0.0;
    double res = 0.0;

    for (int i = 0; i < lengthOfRankedList; i++) {
      int docId = rankedList.get(i);
      if (relevanceJudgments.containsKey(docId) && relevanceJudgments.get(docId).getBinaryRelevance() == 1.0) {
        rr += 1.0;
        ap += rr / (i + 1);
      }
    }

    res = ap / rr;

    if (Double.isNaN(res)) {
      res = 0.0;
    }

    return res;
  }

  // Return the reciprocal rank
  private static double getReciprocal(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    double i = 0.0;
    for (int docId : rankedList) {
      i++;
      if (relevanceJudgments.containsKey(docId) && relevanceJudgments.get(docId).getBinaryRelevance() == 1) {
        return 1 / i;
      }
    }

    return 0;
  }

  // Return the DCG with K = {1, 5, 10}
  private static Map<Integer, Double> getDCG(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    Map<Integer, Double> DCG = new HashMap<Integer, Double>();
    double dcg = 0.0;

    for (int i = 0; i < K.length; i++) {
      int k = K[i];
      for (int j = 0; j < k; j++) {
        int docId = rankedList.get(j);
        if (relevanceJudgments.containsKey(docId)) {
          dcg += relevanceJudgments.get(docId).getCategoricalRelevance() / Math.log(j + 2);
        }
      }

      if (Double.isNaN(dcg)) {
        dcg = 0.0;
      }

      DCG.put(k, dcg);
    }

    return DCG;
  }

  // Return the IDCG with K = {1, 5, 10}
  private static Map<Integer, Double> getIDCG(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    Map<Integer, Double> IDCG = new HashMap<Integer, Double>();
    List<Double> ideaRankedList = new ArrayList<Double>();
    double idcg = 0.0;

    for (Entry entry : relevanceJudgments.entrySet()) {
      RelevancePair relevancePair = (RelevancePair) entry.getValue();
      double categoryRel = relevancePair.getCategoricalRelevance();
      ideaRankedList.add(relevancePair.getCategoricalRelevance());
    }

    Collections.sort(ideaRankedList, new Comparator<Double>() {
      @Override
      public int compare(Double o1, Double o2) {
        return o1 > o2 ? -1 :
            o1 < o2 ? 1 : 0;
      }
    });

    if (ideaRankedList.size() < K[K.length - 1]) {
      for (int i = ideaRankedList.size() - 1; i < K[K.length - 1]; i++) {
        ideaRankedList.add(0.0);
      }
    }

    for (int i = 0; i < K.length; i++) {
      int k = K[i];
      for (int j = 0; j < k; j++) {
        idcg += ideaRankedList.get(j) / Math.log(j + 2);
      }

      if (Double.isNaN(idcg)) {
        idcg = 0.0;
      }

      IDCG.put(k, idcg);
    }

    return IDCG;
  }

  // Return the NDCG with K = {1, 5, 10}
  private static Map<Integer, Double> getNDCG(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    Map<Integer, Double> NDCG = new HashMap<Integer, Double>();
    Map<Integer, Double> DCG = getDCG(relevanceJudgments, rankedList);
    Map<Integer, Double> IDCG = getIDCG(relevanceJudgments, rankedList);

    for (int i = 0; i < K.length; i++) {
      int k = K[i];
      double ndcg = DCG.get(k) / IDCG.get(k);

      if (Double.isNaN(ndcg)) {
        ndcg = 0.0;
      }

      NDCG.put(k, ndcg);
    }

    return NDCG;
  }

  private static List<Double> getPrecisionAtStandardRecalls(HashMap<Integer, RelevancePair> relevanceJudgments, List<Integer> rankedList) {
    List<Double> precisionAtStandardRecalls = new ArrayList<Double>();

    List<PRPair> precisionRecallPairList = new ArrayList<PRPair>();

    // Get all recalls...
    int countOfRelevant = 0;

    for (Entry entry : relevanceJudgments.entrySet()) {
      RelevancePair relevancePair = (RelevancePair) entry.getValue();
      if (relevancePair.getBinaryRelevance() == 1) {
        countOfRelevant++;
      }
    }

    for (int i = 0; i < rankedList.size(); i++) {
      double recall = 0.0;
      double rr = 0.0;

      for (int j = 0; j <= i; j++) {
        int docId = rankedList.get(j);
        if (relevanceJudgments.containsKey(docId) && relevanceJudgments.get(docId).getBinaryRelevance() == 1) {
          rr++;
        }
      }

      PRPair prPair = new PRPair();
      recall = rr / countOfRelevant;
      prPair.setRecall(recall);
      precisionRecallPairList.add(prPair);
    }


    // Get all precision
    for (int i = 0; i < rankedList.size(); i++) {
      double rr = 0.0;
      double res = 0.0;
      for (int j = 0; j <= i; j++) {
        int docId = rankedList.get(j);
        if (relevanceJudgments.containsKey(docId) && relevanceJudgments.get(docId).getBinaryRelevance() == 1) {
          rr++;
        }
      }

      res = rr / (i + 1);
      if (Double.isNaN(res)) {
        res = 0.0;
      }

      precisionRecallPairList.get(i).setPrecision(res);
    }

    // Sort the pairs by recall increasingly
    Collections.sort(precisionRecallPairList, new Comparator<PRPair>() {
      @Override
      public int compare(PRPair o1, PRPair o2) {
        return o1.getRecall() > o2.getRecall() ? 1 :
            o1.getRecall() < o2.getRecall() ? -1 : 0;
      }
    });

    for (int i = 0; i < STANDARD_RECALL_LEVELS.length; i++) {
      double recall = STANDARD_RECALL_LEVELS[i];
      double precision = 0.0;

      for (int j = precisionRecallPairList.size() - 1; j >= 0; j--) {
        PRPair prPair = precisionRecallPairList.get(j);

        if (prPair.getRecall() < recall) {
          break;
        }

        precision = Math.max(precision, prPair.getPrecision());
      }

      precisionAtStandardRecalls.add(precision);
    }

    return precisionAtStandardRecalls;
  }

  public static String evaluateStdin(
//      <String: query, <Integer: documentID, RelevancePair: binary relevance and categorical relevance pair>>
      Map<String, HashMap<Integer, RelevancePair>> relevanceJudgments) {

    Map<Integer, Double> precisionMap;
    Map<Integer, Double> recallMap;
    Map<Integer, Double> FMap;
    Map<Integer, Double> NDCGMap;
    List<Double> precisionAtStandardRecalls;

    double averagePrecision;
    double reciprocal;

    StringBuilder res = new StringBuilder();

    String query = "";
    // Index: rank, Integer: docId
    List<Integer> rankedList = new ArrayList<Integer>();

    try {
//      BufferedReader reader = new BufferedReader(new FileReader(path));
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      try {
        String line;
        while ((line = reader.readLine()) != null) {
          Scanner scanner = new Scanner(line);
          scanner.useDelimiter("\t");
          query = scanner.next();
          int docId = Integer.parseInt(scanner.next());
          rankedList.add(docId);
        }
      } finally {
        logger.info("Closing the file...");
        reader.close();
      }
    } catch (IOException ioe) {
      logger.error("Oops... {}", ioe.getMessage());
    }

    // Get the precision and recall evaluations
    precisionMap = getPrecisionMap(relevanceJudgments.get(query), rankedList);
    recallMap = getRecallMap(relevanceJudgments.get(query), rankedList);
    FMap = getFMap(precisionMap, recallMap);
    NDCGMap = getNDCG(relevanceJudgments.get(query), rankedList);
    precisionAtStandardRecalls = getPrecisionAtStandardRecalls(relevanceJudgments.get(query), rankedList);
    averagePrecision = getAveragePrecision(relevanceJudgments.get(query), rankedList);
    reciprocal = getReciprocal(relevanceJudgments.get(query), rankedList);

    if (Double.isNaN(averagePrecision)) {
      averagePrecision = 0.0;
    }

    if (Double.isNaN(reciprocal)) {
      reciprocal = 0.0;
    }

    res.append(query);
    // Precision: 1, 5, 10
    res.append("\t" + precisionMap.get((Integer) K[0]));
    res.append("\t" + precisionMap.get((Integer) K[1]));
    res.append("\t" + precisionMap.get((Integer) K[2]));
    // Recall: 1, 5, 10
    res.append("\t" + recallMap.get((Integer) K[0]));
    res.append("\t" + recallMap.get((Integer) K[1]));
    res.append("\t" + recallMap.get((Integer) K[2]));
    // F_0.50: 1, 5, 10
    res.append("\t" + FMap.get((Integer) K[0]));
    res.append("\t" + FMap.get((Integer) K[1]));
    res.append("\t" + FMap.get((Integer) K[2]));
    // Precision at recall points {0.0 - 1.0}
    for (int i = 0; i < STANDARD_RECALL_LEVELS.length; i++) {
      res.append("\t" + precisionAtStandardRecalls.get(i));
    }
    // Average precision
    res.append("\t" + averagePrecision);
    // NDCG: 1, 5, 10
    res.append("\t" + NDCGMap.get((Integer) K[0]));
    res.append("\t" + NDCGMap.get((Integer) K[1]));
    res.append("\t" + NDCGMap.get((Integer) K[2]));
    // Reciprocal
    res.append("\t" + reciprocal);

//    System.out.println(res.toString());

    return res.toString();
  }
}

class RelevancePair {
  private final double binaryRelevance;
  private final double categoricalRelevance;

  public RelevancePair(double binaryRelevance, double categoricalRelevance) {
    this.binaryRelevance = binaryRelevance;
    this.categoricalRelevance = categoricalRelevance;
  }

  public double getBinaryRelevance() {
    return this.binaryRelevance;
  }

  public double getCategoricalRelevance() {
    return this.categoricalRelevance;
  }
}

class PRPair {
  private double precision;
  private double recall;

  public PRPair() {
    this.precision = 0.0;
    this.recall = 0.0;
  }

  public double getRecall() {
    return recall;
  }

  public void setRecall(double recall) {
    this.recall = recall;
  }

  public double getPrecision() {
    return precision;
  }

  public void setPrecision(double precision) {
    this.precision = precision;
  }
}
