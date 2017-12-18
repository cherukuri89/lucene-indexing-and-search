
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
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

//Task 1: Generating Lucene Index for Experiment Corpus (AP89)
public class GenerateIndex {

	public static void main(String[] args) {
		
		Directory dir;
		
		//indexPath: path to which the Lucene index is written.
		//inputPath: path from which the documents to be indexed are read.
		String indexPath = "./Results/IndexFiles";
		File inputPath = new File("./corpus");
		
		File[] files = inputPath.listFiles(new FilenameFilter() {
		    public boolean accept(File inputPath, String fileName) {
		        return fileName.endsWith(".trectext");
		    }
		});
		
		//Document is viewed as a collection of fields and each field has a name, value.
		//Lucene uses analyzer to convert the field value into a stream of searchable tokens. 
		try {
			//an instance of file system directory is created, it returns FSDirectory implementation.
			dir = FSDirectory.open(Paths.get(indexPath));
			Analyzer analyzer = new StandardAnalyzer();
			
			//IndexWriterConfig takes the default analyzer as argument and sets its properties to default values.
			//Properties of analyzer can be updated later using setter, getter methods.
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			
			//specify the config for the index writer (CREATE), that creates a new index or overwrites an existing one. 
			iwc.setOpenMode(OpenMode.CREATE);
			
			try {
				//IndexWriter class is used by Lucene to add documents to an index.
				IndexWriter writer = new IndexWriter(dir, iwc);
				
				//loops over files in the directory, converts them to documents and adds them to the index.
				//add the list of field names we are looking for to index to 'fieldsToGet'.
				for(File file : files){
					System.out.println(file);
					BufferedReader fileReader = null;
					
					//fields in the documents that we have to index are maintained in 'fieldsToGet' list. 
					ArrayList<String> fieldsToGet = new ArrayList<>();
					fieldsToGet.add("DOCNO");
					fieldsToGet.add("HEAD");
					fieldsToGet.add("BYLINE");
					fieldsToGet.add("DATELINE");
					fieldsToGet.add("TEXT");
					
					//Generate Lucene index with the fields 1.DOCNO, 2.HEAD, 3.BYLINE, 4.DATELINE, and 5.TEXT.
					try {
					    fileReader = new BufferedReader(new FileReader(file));
					    String line = null;
					    
					    //read each line in a file from directory and check if root tag <DOC> exists.
					    while ((line = fileReader.readLine()) != null) {
					        if(line.contains("<DOC>")){       	
					        	//convert the file to a document (by creating a new document for the file and adding fields).
					        	Document luceneDoc = new Document();
					        	HashMap<String, String> maps = new HashMap<>();
					        	
					        	while (!(line = fileReader.readLine()).contains("</DOC>")){
					        		String tag = "";
					        		if(!line.contains("<")){
					        			continue;
					        		}
					        		tag = line.substring(line.indexOf("<") + 1, line.indexOf(">"));
					        		
					        		//check if value in 'tag' is a valid field that exists in 'fieldsToGet' list.
					        		//if yes, get and save the field value into 'value'
					        		if(fieldsToGet.contains(tag)){
					        			String value = "", tempText = "";
					        			String[] splitText = line.split("<"+tag+">");
					        			if(splitText.length > 1 ){
					        				tempText = splitText[1];
					        			}
					        			
					        			//append the tag value from multiple lines in file to 'value'
					        			while(!tempText.contains("</"+tag+">")){
					        				value += tempText;
					        				line = fileReader.readLine(); 
					        				tempText = line;
					        			}
					        			
					        			splitText = tempText.split("</"+tag+">");
					        			if(splitText.length == 1 ){
					        				value += splitText[0].trim();
					        			}
				        				
					        			//check if there are multiple fields with same name in file having a value in map.
					        			//if map already has a value for the field then append the new value to it. 
				        				String fieldValue = null;
				        				if(maps.get(tag) == null){
				        					fieldValue = value;
				        				}else{
				        					String tempValue = maps.get(tag);
				        					fieldValue = tempValue + " " + value;
				        				}
				        				//save the field name and its associated new value in the map.
				        				maps.put(tag, fieldValue);
					        		}
					        	}
					        	
					        	
					        	//add fields to the document
					        	// Field.Store.YES indicates raw value of field is stored in index and can be retrieved during search.
					        	if(maps.get("DOCNO") != null){
					        		luceneDoc.add(new StringField("DOCNO", maps.get("DOCNO"), Field.Store.YES));
					        	}
					        	if(maps.get("HEAD")!= null){
					        		luceneDoc.add(new TextField("HEAD", maps.get("HEAD"), Field.Store.YES));
					        	}
					        	if(maps.get("BYLINE") != null){
					        		luceneDoc.add(new TextField("BYLINE", maps.get("BYLINE"), Field.Store.YES));
					        	}
					        	if(maps.get("DATELINE") != null){
					        		luceneDoc.add(new TextField("DATELINE", maps.get("DATELINE"), Field.Store.YES));
					        	}
						        if(maps.get("TEXT") != null){
						        	luceneDoc.add(new TextField("TEXT", maps.get("TEXT"), Field.Store.YES));
						        }
						        writer.addDocument(luceneDoc);
					        }
					    }
					} catch (FileNotFoundException e) {
					    e.printStackTrace();
					} catch (IOException e) {
					    e.printStackTrace();
					} finally {
					    try {
					        if (fileReader != null) {
					        	fileReader.close();
					        }
					    } catch (IOException e) {}
					}
				}
				writer.forceMerge(1);
				writer.close();
				
				IndexReader docReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
				
				//print total number of documents in the corpus
				System.out.println("Number of documents in the corpus: "+docReader.maxDoc());
				                             
				docReader.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}	
	}
}
