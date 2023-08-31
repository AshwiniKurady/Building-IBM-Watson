package edu.arizona.cs;


import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.*;
import edu.stanford.nlp.trees.*;

import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserDemo {

    /**
     * The main method demonstrates the easiest way to load a parser.
     * Simply call loadModel and specify the path of a serialized grammar
     * model, which can be a file, a resource on the classpath, or even a URL.
     * For example, this demonstrates loading a grammar from the models jar
     * file, which you therefore need to include on the classpath for ParserDemo
     * to work.
     *
     * Usage: {@code java ParserDemo [[model] textFile]}
     * e.g.: java ParserDemo edu/stanford/nlp/models/lexparser/chineseFactored.ser.gz data/chinese-onesent-utf8.txt
     *
     */
    public static void main(String[] args) {
        String parserModel = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
        if (args.length > 0) {
            parserModel = args[0];
        }
        LexicalizedParser lp = LexicalizedParser.loadModel(parserModel);

        if (args.length == 0) {
            demoAPI(lp,"This person is the queen's representative in Canada; currently the office is held by David Johnston");
        } else {
            String textFile = (args.length > 1) ? args[1] : args[0];
            demoDP(lp, textFile);
        }
    }

    /**
     * demoDP demonstrates turning a file into tokens and then parse
     * trees.  Note that the trees are printed by calling pennPrint on
     * the Tree object.  It is also possible to pass a PrintWriter to
     * pennPrint if you want to capture the output.
     * This code will work with any supported language.
     */
    public static void demoDP(LexicalizedParser lp, String filename) {
        // This option shows loading, sentence-segmenting and tokenizing
        // a file using DocumentPreprocessor.
        TreebankLanguagePack tlp = lp.treebankLanguagePack(); // a PennTreebankLanguagePack for English
        GrammaticalStructureFactory gsf = null;
        if (tlp.supportsGrammaticalStructures()) {
            gsf = tlp.grammaticalStructureFactory();
        }
        // You could also create a tokenizer here (as below) and pass it
        // to DocumentPreprocessor
        for (List<HasWord> sentence : new DocumentPreprocessor(filename)) {
            Tree parse = lp.apply(sentence);
            parse.pennPrint();
            System.out.println();

            if (gsf != null) {
                GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
                Collection tdl = gs.typedDependenciesCCprocessed();
                System.out.println(tdl);
                System.out.println();
            }
        }
    }

    /**
     * demoAPI demonstrates other ways of calling the parser with
     * already tokenized text, or in some cases, raw text that needs to
     * be tokenized as a single sentence.  Output is handled with a
     * TreePrint object.  Note that the options used when creating the
     * TreePrint can determine what results to print out.  Once again,
     * one can capture the output by passing a PrintWriter to
     * TreePrint.printTree. This code is for English.
     */


    ////////////////
    //// This part is modified for parserDemo experiment for the IR system
    //////////////
    public static String demoAPI(LexicalizedParser lp,String sent2) {
        // This option shows parsing a list of correctly tokenized words
        String[] sent = { "This", "is", "an", "easy", "sentence", "." };
        List<CoreLabel> rawWords = SentenceUtils.toCoreLabelList(sent);
        Tree parse = lp.apply(rawWords);
        //parse.pennPrint();
        //System.out.println();

        // This option shows loading and using an explicit tokenizer
        //String sent2 = "Italian for \"leader\", it was especially applied to Benito Mussolini";
        TokenizerFactory<CoreLabel> tokenizerFactory =
                PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
        Tokenizer<CoreLabel> tok =
                tokenizerFactory.getTokenizer(new StringReader(sent2));
        List<CoreLabel> rawWords2 = tok.tokenize();
        parse = lp.apply(rawWords2);

        TreebankLanguagePack tlp = lp.treebankLanguagePack(); // PennTreebankLanguagePack for English
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
        GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
        List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
        //System.out.println(tdl);
        String strr ="";
        String retStr = "";
        for(TypedDependency str : tdl) {
            strr = str.toString();
            Matcher m = Pattern.compile("case\\((.*)-\\d+,.*\\)").matcher(strr);
            if(m.find()) {
                retStr = retStr.concat(m.group(1)).concat(" ");
            }
            Matcher m2 = Pattern.compile("compound\\((.*)-\\d+,.*\\)").matcher(strr);
            if(m2.find()) {
                retStr = retStr.concat(m2.group(1)).concat(" ");
            }
            Matcher m3 = Pattern.compile("cc\\((.*)-\\d+,.*\\)").matcher(strr);
            if(m3.find()) {
                retStr = retStr.concat(m3.group(1)).concat(" ");
            }
        }
        return retStr;

        //System.out.println(tdl);
       /* System.out.println("Heya");

        // You can also use a TreePrint object to print trees and dependencies
        TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
        tp.printTree(parse);*/
    }

    public ParserDemo() {} // static methods only

}
