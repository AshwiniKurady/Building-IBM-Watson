# Building-IBM-Watson
IBM’s Watson is a Question Answering (QA) system that is similar to TV quiz show, Jeopardy.” However, the answers to many of the Jeopardy questions are actually titles of Wikipedia pages. The focus of this project is to classify the wikipedia pages and to find which page is the most likely answer to the given clue

# How to run the jar file and directory structure:
a. Running on terminal
Download the index folder, wiki-subset folder and questions.txt into a single folder (please note that names of the folders need to match). Then, on the terminal, run the following command :

java -jar FinalProject-1.0-SNAPSHOT-jar-with-dependencies.jar “<Downloaded_folder_containing_subfolders_for_index_wiki_questions>”

# b. Running on IntelliJ

Download the source code from the link given above. Download the index folder, wiki-subset folder and questions.txt into a single folder (please note that names of the folders need to match) into the resources folder of the project as shown below :

Place where files are read from :
questions.txt – “src/main/resources/questions.txt”
index - “src/main/resources/index/”
wiki subset - “src/main/resources/wiki-subset/”

# 1. IndexingandRetrieval

Function buildIndex()
a. Wikipedia collection has been indexed in lucene.
b. Initializing Index by declaring variables such as StandardAnalyzer, FSDirectory, InderWriter
Config and IndexWriter – FS directory is used to store the index.
a. The index is stored inside resources/index/
c. Each Wikipedia page has been taken as a separate document in the Index.
d. Using pattern matcher to extract the content between [[ ]] and store it as “title” and then store the remaining text which appear before the next [[ ]] as “content”. Here, category is also taken within the content.

Function: Lemmatize()
a. Performing Lemmatization using Standford NLP Lemmatizer on the content field of each
page.
b. Each wiki page is categorized to two fields in the index, “title” and “content”.
c. Main issue related to storing Wikipedia content is that they include ASCII characters and URLs
which is difficult to store in index. Hence replaceall () command is used to replace these characters with a “”.

Function addDoc():
a. This function is called within the buildIndex to add the document to the Index by using
IndexWriter.
b. Finally, Index has “title” as the title of the wikipage and “content” has both category and text
of the wiki page.

Function readQuestion():
a. For the retrieval part , we first need to parse the query to required format . Hence
readQuestions() function is called after buildindex().
b. Under readQuestions(), we extract the question file path and split each question to 3 parts –
category, clue and answer.
c. Thecategoryandcluearecombinedtogethertoformaqueryandtheresultfromtheretrieved
query answer (which we shall refer to as Modelanswer) is compared with the answer to the
question.
d. As Query parser (with lemmatizer) does not parse ASCII or UTI characters, by using
readQuestions() function, any number of ASCII characters and the stop words are removed from the category and clue part of the question and pass it to next function called runQueryonIndex().
e. The function removeStopWords removes stop words (only punctuation marks and special characters using standardAnalyzer) and parses the query.

Function runQueryonIndex():
a. Passing the query(category+clue) to runQueryonIndex().
b. Under this function, Query parser is initialized to parse the content field of the Index .
Standard Analyzer is initiated on the query (content+ category) which helps to break the text in content field into terms for searching. A proximity search of “within 5 positions” field is appended at the end to enhance the doc scores.
c. Using Indexsearcher to search the top 30 documents related to the query.
d. By Default, scoring function is taken as BM25.

The values returned from the runQueryOnIndex function (resultclass containing the docid (doc title) and the doc scores) is then checked with the answers from the questions.txt file (present in resources directory). The top relevant documents are used to calculate the score that shows the efficacy of the IR system.

# 2. MeasuringPerformance
Performance of the project is measured using mean reciprocal rank(MRR).
Below are the scores obtained in MRR where N represents total number of the ranked document returned from query parser chosen to calculate the MRR.
#Matches: Total number of matches obtained for respective number of ranking chosen
#Score : MMR Score

   N  #Matches  Score(MRR%)
  ---------------------------
  50      52        25.87
  40      49        25.81
  30      49        25.81
  20      44        25.61
  10      35        25.02
  1       22        22

As seen, after the top 10 results, further looking at lower ranked results does not have significant returns in terms of MRR. So N=10 is chosen (return top 10 ranked docs out of 280000) in this project.

Reason to choose MMR over all others for measuring performance is as follows :
Precision@1 only looks at the top ranked result. Even if the returned result is @ position 2, it is ignored and given a value 0. So, while it is useful in knowing the absolute efficiency of a system where only the correct answer matters (like Jeopardy), it does not give a relative performance metric of the system - even if the returned position is 2, the system performance is not changed.

Mean average position looks at all the returned results (not just the first relevant one). While this is helpful in many situations, for a game like Jeopardy, even when looking for the relative performance of the system compared to another variant, looking at the first relevant result for a query is sufficient and hence MAP is not required.

Mean Reciprocal Rank does a tradeoff of sorts between Precision @ 1 and MAP.
It returns a non-zero value for ranked results which are not at position 1, thereby achieving a kind of smoothing. And it only considers the top ranked result and does not consider any other lower ranked relevant results - this works perfectly fine for a game like Jeopardy.

# 3. Changing the scoring function

Changing the scoring function lowered the performance from (MMR ~ 25%) to (MRR ~ 0.57%). 5 relevant documents were obtained with tf-idf weighting. One observation is that all these documents were available for BM25 too but with high rank. As per latest update, Indexwriterconfig.setSimilarity(new classicSimilarity) and Indexsearcher.setSimilarity(new classicSimilarity) was added but to no avail. The same results were obtained. Hence, this system performs very well with the default BM25 similarity.

# 4. ErrorAnalysis

Following is the split-up of the answers from the IR system for Top-10 & Top-30 answers :

   N   #ExactMatch    #TotalMatch                             MRR%
  --------------------------------------------------------------------
  10       22         35 (13 top relevant but not Top@1)     25.02
  30       22         49 (27 top relevant but not Top@1)     25.81

Looking at the questions that are answered correctly, following are the common factors observed which help the simple IR system to perform well :
a. Clearersentenceformationandmoregrammaticallycorrect
b. Morenounsincludingnamesofpeople/places

Looking at the questions that are answered incorrectly, following are the common factors observed which limit the performance of the designed IR system:
a. Shortersentences(clues)
b. Morenumbersinclues
c. Quotes and punctuation marks - these are removed by standard analyzer
and hence the quotation context is lost.

# 5. ImprovementstotheIRsystem

Some of the approaches tried/implemented to improve the IR system are : a. Implementation of Stanford CoreNLP lemmatizer
i. Lemmatizer was implemented while creating the index. This helped to improve the MRR score ~17% to ~22%.
ii. A cons to this approach is that indexing took about ~5hours on a 2GB RAM system. Storing the index on directory helped immensely.
b. Pseudo proximity search is implemented to help improve the MRR score – pseudo because the proximity is only added at the end.
c. One of the avenues explored (late in the project) was to implement synctactic dependencies for parsing. The DependencyParserDemo.java from Stanford CoreNLP was
used (and changed for an adhoc implementation, albeit unsuccessfully). ParserDemo class in the project contains the adhoc code for this derived from the Stanford NLP files. Though, in all honesty, this part of the code isn’t understood completely and is probably why giving lower MRR results.

# Snapshot of sample run of JAR file on terminal:


   

  
