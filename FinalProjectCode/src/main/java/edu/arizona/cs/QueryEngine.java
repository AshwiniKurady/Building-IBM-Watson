//////////////////////////////////////////////////////
//// FINAL PROJECT
//// Information Retrieval System - A BABY WATSON !!!!
////
////  Name : Ashwini Kurady
////  Email : ashwinikurady@email.arizona.edu
////  Date : Dec 9, 2020
////
//////////////////////////////////////////////////////



package edu.arizona.cs;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.tartarus.snowball.SnowballProgram;
import org.tartarus.snowball.ext.EnglishStemmer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.logging.Level.SEVERE;

//main QueryEngine class for the system
public class QueryEngine {
    private static final String CONTENTS = "";
    private static String resourcesPath;
    private boolean indexExists = false;
    private File filePath;
    //Initialize Index
    private StandardAnalyzer analyzer;
    private Directory index;
    private IndexWriterConfig config;
    private IndexWriter writer;
    StanfordLemmatizer slem = new StanfordLemmatizer();
  //  ParserDemo parDemo = new ParserDemo();
    public int N = 10;
    public static String indexPath;


   // String parserModel = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
   // LexicalizedParser lp = LexicalizedParser.loadModel(parserModel);

    public int buildFlag = 0; // Make this 1 to enable building index
    public int BM25 = 1; // Make this 0 to enable tf-idf similarity

    private final static Logger logger = Logger.getLogger(QueryEngine.class.getName());

    //QueryEngine function to initialize the Index and calls buildIndex() and readQuestions() method
    public QueryEngine(File directoryPath) throws IOException {
       filePath = directoryPath;
        //Initialize Index
        index = FSDirectory.open(Paths.get(indexPath));
        //index = new RAMDirectory();
        analyzer = new StandardAnalyzer();
        config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        try {
            writer = new IndexWriter(index, config);
        } catch (final IOException e) {
            logger.log(SEVERE, "Query Engine Initialization failed", e);
        }
        if(buildFlag==1)
        {
            buildIndex();
        }
        readQuestions();
    }

    // function to remove stop words.
    public static String removeStopWords(String inputString) throws Exception {
        final String text = inputString;
        String remainingText = "";
        try {
            //StandardAnalyzer to filter punctuation
            Analyzer analyzer = new StandardAnalyzer();
            TokenStream tokenStream = analyzer.tokenStream(CONTENTS, new StringReader(text));
            CharTermAttribute term = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();

            while(tokenStream.incrementToken()) {
                remainingText = remainingText.concat(term.toString()).concat(" ");
            }
            tokenStream.close();
            analyzer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return remainingText;
    }

    //function to lemmatize input string
    public String lemmatize(String input){
        String content = input.trim();
        String[] tmpval = slem.lemmatize(content).toArray(new String[0]);
        String value = String.join(" ", tmpval);
        return value;

    }

    //readQuestions function to read the questions from questions.txt,
    // perform filters and passing results to runQueryOnIndex() method
    private void readQuestions()
    {
        String filepath = resourcesPath.concat("/questions.txt");
        File QfilePath = new File(filepath);

        //Initializing variables
        int count = 0;
        int exactCount = 0;
        double mrr = 0.0;

        try (Scanner inputScanner = new Scanner(QfilePath)) {
            while (inputScanner.hasNextLine()) {
                String catQuest = inputScanner.nextLine();
                //performing replaceAll to remove ASCII characters and
                // standford NLP lemmatizer which returns the root words.
                //catQuest: category Question.
                //clueQues: clue Answeer.
                //ansQuest: Answer.
                catQuest = catQuest.replaceAll("[^\\p{ASCII}]", "");
                catQuest = removeStopWords(catQuest);
                catQuest = lemmatize(catQuest);
                Matcher m = Pattern.compile("^\\s*$").matcher(catQuest);
                if(m.find())
                {
                    continue;
                }
                String clueQuest = inputScanner.nextLine();
                clueQuest = clueQuest.replaceAll("[^\\p{ASCII}]", "");
                clueQuest = removeStopWords(clueQuest);
                clueQuest = lemmatize(clueQuest);

                // Stemming the query
                SnowballProgram stemmer2 = new EnglishStemmer();
                String[] words2 = clueQuest.split("\\s+");
                clueQuest = "";
                for (String word : words2) {
                    stemmer2.setCurrent(word);
                    stemmer2.stem();
                    clueQuest = clueQuest.concat(stemmer2.getCurrent()).concat(" ");
                }
                // End of Stemming
                ///////////////////////////////////////////

                String ansQuest = inputScanner.nextLine();

                // Pass CLUE to runQueryonindex
                String queryStr = clueQuest.concat(" ").concat(catQuest).concat("~5");

                //////commented is the parser to extract syntactic dependency using Standford dependency.
                //https://nlp.stanford.edu/software/stanford-dependencies.shtml

                //String pDemo = parDemo.demoAPI(lp,clueQuest);
                //queryStr = queryStr.concat(pDemo).concat("~5");

                List<ResultClass> resultClasses = runQueryOnIndex(queryStr);
                String[] listofModelAnswers = new String[N];
                ansQuest = ansQuest.replaceAll("\\(.*\\)", "");
                String[] listofQAns = ansQuest.split("\\|");
                int j = 0;
                for (ResultClass result : resultClasses) {
                   listofModelAnswers[j]=result.DocName.getField("docId").stringValue();
                   if (Arrays.asList(listofQAns).contains(listofModelAnswers[j])) {
                       if(j==0)
                       {
                           exactCount++;
                       }
                           count = count + 1;
                       //To display the results like DocRank, ActualAnswer, ModelAnswer, DocScore
                           System.out.format("DocRank : %s, ActualAns : %s, ModelAns: %s, Content : , DocScore: %s\n", j, ansQuest,
                                   result.DocName.getField("docId").stringValue(),
                                   //result.DocName.getField("content").stringValue(),
                                   result.docScore);

                       //To calculate MRR
                       mrr = mrr + 1.0/(j+1);
                       break;
                   }
                   j = j+1;
                }
            }
            System.out.println("Exact matches = " + exactCount);
            System.out.println("Total matches = " + count);
        } catch (FileNotFoundException e) {
            logger.log(SEVERE, "Unable to read file", e);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //To calculate MRR
        mrr = mrr/100*100;
        System.out.println("MRR : "+mrr);
    }
    //buildIndex() to add the documents to the Index
    private void buildIndex() {
        File[] directories = new File(filePath.toString()).listFiles();
        int titleCount = 0;
        for (File file : directories) {
            if(file.getName().endsWith("txt")) {
                String docId = "";
                try (Scanner inputScanner = new Scanner(file)) {
                    String content = "";
                    while (inputScanner.hasNextLine()) {
                        int matchFlag = 0;
                        String lineContent = inputScanner.nextLine();
                        //Using replace-all to remove URL characters from the content
                        lineContent = lineContent.replaceAll("http.*?(\\[|\\s|\\t|.htm|.html)", " ");
                        //Using pattern Matcher to extract data within [[ ]] and assign it as docId
                        Matcher m = Pattern.compile("^\\[\\[(.+)\\]\\]$").matcher(lineContent);
                        if (m.find()) {
                            matchFlag = matchFlag + 1;
                            Matcher m3 = Pattern.compile("^\\s*$").matcher(docId);
                            if (!m3.find()) {
                                titleCount = titleCount + 1;
                                content = content.replaceAll("[^\\p{ASCII}]", "");
                                // adding docId and content with the help of IndexWriter using addDoc function
                                content = lemmatize(content);
                                addDoc(writer, content, docId);
                            }
                            docId = m.group(1);
                            content = "";
                        }
                        if (matchFlag == 0) {
                            Matcher m2 = Pattern.compile("^\\s*$").matcher(lineContent);
                            if (!m2.find()) {
                                content = content.concat(lineContent).concat(" ");
                            }
                        }

                    }
                    titleCount = titleCount + 1;
                    content = content.replaceAll("[^\\p{ASCII}]", "");
                    content = lemmatize(content);
                    addDoc(writer, content, docId);
                    writer.commit();
                } catch (FileNotFoundException e) {
                    logger.log(SEVERE, "Unable to read file", e);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        indexExists = true;
    }
    //function to add the document with fields content and docId
    private static void addDoc(IndexWriter writer, String content, String docId) {
        try {
            Document doc = new Document();
            doc.add(new TextField("content", content, Field.Store.YES));
            doc.add(new StringField("docId", docId, Field.Store.YES));
            writer.addDocument(doc);
        }
        catch (final IOException ex)
        {
            logger.log(SEVERE, "Unable to write document with docId: " + docId, ex);
        }
    }
    //runQueryOnIndex function to perform Query parse, to search the query from Index,
    // to set similarity and  to rank the documents on Docscore.
    public List<ResultClass> runQueryOnIndex(String input) throws IOException {
        String concatenatedInput = input;
        QueryParser parser = new QueryParser("content", analyzer);
        Query query = null;
        try {
            query = parser.parse(concatenatedInput);
        } catch (ParseException e) {
            throw new IOException(e);
        }

        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        //default Similarity is BM25. Below commented is the td-idf similarity.
        if(BM25 ==0) {
            searcher.setSimilarity(new ClassicSimilarity());
        }

        //searcher to search the documents for the query
        TopDocs docs = searcher.search(query, N);

        //creating an Arraylist to store DocName and DocScore
        List<ResultClass> result = new ArrayList<>();
        for (ScoreDoc scoreDoc : docs.scoreDocs) {
            ResultClass resultClass = new ResultClass();
            resultClass.DocName = searcher.doc(scoreDoc.doc);
            resultClass.docScore = scoreDoc.score;
            result.add(resultClass);
        }
        return result;
    }
    //main function to
    public static void main(String[] args) {
        try {
            resourcesPath = "src/main/resources/";
            if (args.length > 0) {
                resourcesPath = args[0];
            }
            indexPath = resourcesPath.concat("/index/");
            String direcPath = resourcesPath.concat("/wiki-subset/");
            File directoryPath = new File(direcPath);
            new QueryEngine(directoryPath);
        } catch (Exception ex) {
            logger.log(SEVERE, "Lucene execution failed", ex);
        }
    }
}
