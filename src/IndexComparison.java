
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

//Task 2: Test different analyzers
//Keyword analyzer: doesnt split the field text at all and treats it as a single token. No tokenization, stemming, stoplisting. 
//Standard analyzer: (default analyzer) uses spaces & punctuations as split points. Tokenization is applied. No stemming. Stopwords are removed.
//Stop analyzer: Tokenization is applied. Stopwords are removed.
//Simple analyzer: Tokenization is applied. No stemming, stoplisting.

public class IndexComparison {

	public static void main(String[] args) {
		
		//paths to directories where the results of Lucene index are written for different analyzers.
		Directory dir, dirKeyword, dirSimple, dirStop;
		String indexPath = "./Results/StandardAnalyzer";
		String indexPathKeyword = "./Results/KeywordAnalyzer";
		String indexPathSimple = "./Results/SimpleAnalyzer";
		String indexPathStop = "./Results/StopAnalyzer";
		
		//inputPath: path from which the documents to be indexed are read.
		File inputPath = new File("./corpus");
		File[] files = inputPath.listFiles(new FilenameFilter() {
		    public boolean accept(File inputPath, String fileName) {
		        return fileName.endsWith(".trectext");
		    }
		});
		
		try {
			//creating instances of file system directory for different analyzers, it returns FSDirectory implementation.
			dir = FSDirectory.open(Paths.get(indexPath));
			dirKeyword = FSDirectory.open(Paths.get(indexPathKeyword));
			dirSimple = FSDirectory.open(Paths.get(indexPathSimple));
			dirStop = FSDirectory.open(Paths.get(indexPathStop));
			
			Analyzer analyzer = new StandardAnalyzer();
			Analyzer keywordAnalyzer = new KeywordAnalyzer();
			Analyzer simpleAnalyzer = new SimpleAnalyzer();
			Analyzer stopAnalyzer = new StopAnalyzer();
			
			//IndexWriterConfig takes analyzer as argument. Default analyzer is standard analyzer.
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			IndexWriterConfig iwcKeyword = new IndexWriterConfig(keywordAnalyzer);
			IndexWriterConfig iwcSimple = new IndexWriterConfig(simpleAnalyzer);
			IndexWriterConfig iwcStop = new IndexWriterConfig(stopAnalyzer);
			
			//creates a new index or overwrites an existing one.
			iwc.setOpenMode(OpenMode.CREATE);
			iwcKeyword.setOpenMode(OpenMode.CREATE);
			iwcSimple.setOpenMode(OpenMode.CREATE);
			iwcStop.setOpenMode(OpenMode.CREATE);
			
			try {
				IndexWriter writer = new IndexWriter(dir, iwc);
				IndexWriter writerKeyword = new IndexWriter(dirKeyword, iwcKeyword);
				IndexWriter writerSimple = new IndexWriter(dirSimple, iwcSimple);
				IndexWriter writerStop = new IndexWriter(dirStop, iwcStop);
				
				//loops over files in the directory, converts them to documents and adds them to the index.
				for(File file : files){
					System.out.println(file);
					BufferedReader fileReader = null;
					
					//adds 'TEXT' as valid field name to the 'fieldsToGet' list. 
					ArrayList<String> fieldsToGet = new ArrayList<>();
					fieldsToGet.add("TEXT");
					
					try {
					    fileReader = new BufferedReader(new FileReader(file));
					    String line = null;
					    while ((line = fileReader.readLine()) != null) {
					    	//read each line in a file from directory and check if root tag <DOC> exists.
					    	//if it exists create a new document and add field name, text.
					        if(line.contains("<DOC>")){	
					        	Document luceneDoc = new Document();
					        	HashMap<String, String> map = new HashMap<>();
					        						        	
					        	while (!(line = fileReader.readLine()).contains("</DOC>")){
					        		String tag = "";
					        		if(!line.contains("<")){
					        			continue;
					        		}
					        		
					        		tag = line.substring(line.indexOf("<") + 1, line.indexOf(">"));
					        		//check if tag is 'TEXT' and get the value of this field in 'value'
					        		if(fieldsToGet.contains(tag)){
					        			
					        			String value = "", tempText = "";
					        			String[] splitText = line.split("<"+tag+">");
					        			if(splitText.length > 1 ){
					        				tempText = splitText[1];
					        			}
					        			while(!tempText.contains("</"+tag+">")){
					        				value += tempText;
					        				line = fileReader.readLine(); 
					        				tempText = line;
					        			}
					        			splitText = tempText.split("</"+tag+">");
					        			if(splitText.length == 1 ){
					        				value += splitText[0].trim();
					        			}
				        				
					        			//check if map already has an associated value for the key "TEXT". If yes, append the new value to old one.
				        				String fieldValue = null;
				        				if(map.get(tag) == null){
				        					fieldValue = value;
				        				}else{
				        					String tempValue = map.get(tag);
				        					fieldValue = tempValue + " " + value;
				        				}
				        				map.put(tag, fieldValue);
					        		}
					        	}
					        	
					        	//if "TEXT" field has an associated value in map, add this field to the document.
					        	//3 args for field: field name, field value, (Field.Store.YES) setting the flag to save raw value of field in index.
						        if(map.get("TEXT") != null){
						        	luceneDoc.add(new TextField("TEXT", map.get("TEXT"), Field.Store.YES));
						        }
						        //adds created documents to the index.
						        writer.addDocument(luceneDoc);
						        writerKeyword.addDocument(luceneDoc);
						        writerSimple.addDocument(luceneDoc);
						        writerStop.addDocument(luceneDoc);
					        }
					    }
					} catch (FileNotFoundException ex) {
					    ex.printStackTrace();
					} catch (IOException ex) {
					    ex.printStackTrace();
					} finally {
					    try {
					        if (fileReader != null) {
					        	fileReader.close();
					        }
					    } catch (IOException ex) {}
					}
				}
				//after indexing all documents, merge the indexes into a single segment by passing 1 as argument to forceMerge method.
				writer.forceMerge(1);
				writerKeyword.forceMerge(1);
				writerSimple.forceMerge(1);
				writerStop.forceMerge(1);
				
				//close the index writers.
				writer.close();
				writerKeyword.close();
				writerSimple.close();
				writerStop.close();

				dir.close();
				dirKeyword.close();
				dirSimple.close();
				dirStop.close();
				
				//read data from the index using index readers.
				IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
				IndexReader readerKeyword = DirectoryReader.open(FSDirectory.open(Paths.get(indexPathKeyword)));
				IndexReader readerSimple = DirectoryReader.open(FSDirectory.open(Paths.get(indexPathSimple)));
				IndexReader readerStop = DirectoryReader.open(FSDirectory.open(Paths.get(indexPathStop)));
				
				//print the total number of documents in the corpus.
				System.out.println("\nTotal number of documents in the corpus with Standard Analyzer:"+reader.maxDoc());
				System.out.println("Total number of documents in the corpus with Keyword Analyzer:"+readerKeyword.maxDoc());
				System.out.println("Total number of documents in the corpus with Simple Analyzer:"+readerSimple.maxDoc());
				System.out.println("Total number of documents in the corpus with Stop Analyzer:"+readerStop.maxDoc());
				
				
				//number of documents containing the term "new" in <field>TEXT</field>.
			    System.out.println("\nNumber of documents containing the term \"new\" for field \"TEXT\": "+reader.docFreq(new Term("TEXT", "new")));
			    //total number of occurrences of the term "new" across all documents for <field>TEXT</field>.
			    System.out.println("Number of occurrences of \"new\" in the field \"TEXT\": "+reader.totalTermFreq(new Term("TEXT","new"))); 
				
				//construct terms from the <field> TEXT.
				Terms vocabulary = MultiFields.getTerms(reader, "TEXT");
				Terms vocabularyKeyword = MultiFields.getTerms(readerKeyword, "TEXT");
				Terms vocabularySimple = MultiFields.getTerms(readerSimple, "TEXT");
				Terms vocabularyStop = MultiFields.getTerms(readerStop, "TEXT");
				
				//print count of terms in the dictionary with different analyzers.
				System.out.println("\nNumber of terms in the dictionary with Standard Analyzer:"+vocabulary.size());
				System.out.println("Number of terms in the dictionary with Keyword Analyzer:"+vocabularyKeyword.size());
				System.out.println("Number of terms in the dictionary with Simple Analyzer:"+vocabularySimple.size());
				System.out.println("Number of terms in the dictionary with Stop Analyzer:"+vocabularyStop.size());
				
				//print tokens count for the <field> TEXT.
				System.out.println("\nNumber of tokens for the field \"TEXT\" with Standard Analyzer:"+vocabulary.getSumTotalTermFreq());
				System.out.println("Number of tokens for the field \"TEXT\" with Keyword Analyzer:"+vocabularyKeyword.getSumTotalTermFreq());
				System.out.println("Number of tokens for the field \"TEXT\" with Simple Analyzer:"+vocabularySimple.getSumTotalTermFreq());
				System.out.println("Number of tokens for the field \"TEXT\" with Stop Analyzer:"+vocabularyStop.getSumTotalTermFreq());
				
				
				//print the total number of postings for <field> TEXT.
				System.out.println("\nNumber of postings for the field \"TEXT\" with Standard Analyzer: "+vocabulary.getSumDocFreq());
				System.out.println("Number of postings for the field \"TEXT\" with Keyword Analyzer: "+vocabularyKeyword.getSumDocFreq());
				System.out.println("Number of postings for the field \"TEXT\" with Simple Analyzer: "+vocabularySimple.getSumDocFreq());
				System.out.println("Number of postings for the field \"TEXT\" with Stop Analyzer: "+vocabularyStop.getSumDocFreq());
				
				//print the total number of documents that have at least one term for <field> TEXT.
				System.out.println("\nNumber of documents that have at least one term for the field \"TEXT\" with Standard Analyzer: "+vocabulary.getDocCount());
				System.out.println("Number of documents that have at least one term for the field \"TEXT\" with Keyword Analyzer: "+vocabularyKeyword.getDocCount());
				System.out.println("Number of documents that have at least one term for the field \"TEXT\" with Simple Analyzer: "+vocabularySimple.getDocCount());
				System.out.println("Number of documents that have at least one term for the field \"TEXT\" with Stop Analyzer: "+vocabularyStop.getDocCount());
				
				
				/*TermsEnum iterator = vocabulary.iterator();
			    BytesRef byteRef = null;
			    System.out.println("\n*******Vocabulary-Start**********");
			    while((byteRef = iterator.next()) != null) {
			    	String term = byteRef.utf8ToString();
			    	System.out.print(term+"\t");
			    }
			    System.out.println("\n*******Vocabulary-End**********");  */ 
				
				
				//close the index readers.
				reader.close();
				readerKeyword.close();
				readerSimple.close();
				readerStop.close();

			} catch (IOException ex) {
				ex.printStackTrace();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}	
	}
}
