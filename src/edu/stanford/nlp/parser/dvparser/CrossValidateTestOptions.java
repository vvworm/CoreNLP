package edu.stanford.nlp.parser.dvparser;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.parser.common.ArgUtils;
import edu.stanford.nlp.parser.lexparser.EvaluateTreebank;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Treebank;
import edu.stanford.nlp.util.Pair;

public class CrossValidateTestOptions {
  public static final double[] weights = { 0.0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.4, 0.5, 1.0 };

  public static void main(String[] args) throws IOException, ClassNotFoundException {
    String dvmodelFile = null;
    String lexparserFile = null;
    String testTreebankPath = null;
    FileFilter testTreebankFilter = null;

    List<String> unusedArgs = new ArrayList<String>();
    for (int argIndex = 0; argIndex < args.length; ) {
      if (args[argIndex].equalsIgnoreCase("-lexparser")) {
        lexparserFile = args[argIndex + 1];
        argIndex += 2;
      } else if (args[argIndex].equalsIgnoreCase("-testTreebank")) {
        Pair<String, FileFilter> treebankDescription = ArgUtils.getTreebankDescription(args, argIndex, "-testTreebank");
        argIndex = argIndex + ArgUtils.numSubArgs(args, argIndex) + 1;
        testTreebankPath = treebankDescription.first();
        testTreebankFilter = treebankDescription.second();
      } else {
        unusedArgs.add(args[argIndex++]);
      }
    }

    System.err.println("Loading lexparser from: " + lexparserFile);
    String[] newArgs = unusedArgs.toArray(new String[unusedArgs.size()]);
    LexicalizedParser lexparser = LexicalizedParser.loadModel(lexparserFile, newArgs);
    System.err.println("... done");

    Treebank testTreebank = null;
    if (testTreebankPath != null) {
      System.err.println("Reading in trees from " + testTreebankPath);
      if (testTreebankFilter != null) {
        System.err.println("Filtering on " + testTreebankFilter);
      }
      testTreebank = lexparser.getOp().tlpParams.memoryTreebank();;
      testTreebank.loadPath(testTreebankPath, testTreebankFilter);
      System.err.println("Read in " + testTreebank.size() + " trees for testing");
    }     

    double[] labelResults = new double[weights.length];
    double[] tagResults = new double[weights.length];

    for (int i = 0; i < weights.length; ++i) {
      lexparser.getOp().baseParserWeight = weights[i];
      EvaluateTreebank evaluator = new EvaluateTreebank(lexparser);
      evaluator.testOnTreebank(testTreebank);
      labelResults[i] = evaluator.getLBScore();
      tagResults[i] = evaluator.getTagScore();
    }

    for (int i = 0; i < weights.length; ++i) {
      System.err.println("LexicalizedParser weight " + weights[i] + ": labeled " + labelResults[i] + " tag " + tagResults[i]);
    }
  }
}
