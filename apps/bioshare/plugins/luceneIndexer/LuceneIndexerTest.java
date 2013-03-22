package plugins.luceneIndexer;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.molgenis.framework.db.DatabaseException;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import plugins.HarmonizationComponent.OWLModel;
import uk.ac.ebi.ontocat.OntologyServiceException;

public class LuceneIndexerTest
{

	/**
	 * @param args
	 * @throws IOException
	 * @throws LockObtainFailedException
	 * @throws CorruptIndexException
	 * @throws OntologyServiceException
	 * @throws ParseException
	 * @throws DatabaseException
	 * @throws OWLOntologyCreationException
	 */
	public static void main(String[] args) throws CorruptIndexException, LockObtainFailedException, IOException,
			OntologyServiceException, ParseException, DatabaseException, OWLOntologyCreationException
	{
		File indexDirectory = new File("/Users/chaopang/Desktop/TempIndex");
		System.out.println("Started index.....");
		File ontologyFile = new File("/Users/chaopang/Desktop/Ontologies/Thesaurus_12.04e-Processed.owl");
		startIndex(indexDirectory, ontologyFile);
		System.out.println("Finished index.....");
		System.out.println("Started searching index.....");
		searchIndexExact(indexDirectory);
		// searchIndexStandard(indexDirectory);
		// Analyzer analyzer = new Analyzer()
		// {
		// @Override
		// public TokenStream tokenStream(String fieldName, Reader reader)
		// {
		// TokenStream result = new StandardTokenizer(Version.LUCENE_30,
		// reader);
		// result = new StandardFilter(result);
		// result = new LowerCaseFilter(result);
		// result = new PorterStemFilter(result);
		// result = new StopFilter(true, result,
		// NGramMatchingModel.STOPWORDSLIST, true);
		// return result;
		// }
		// };
	}

	private static void searchIndexStandard(File indexDirectory) throws CorruptIndexException, IOException,
			ParseException
	{
		IndexReader reader = IndexReader.open(FSDirectory.open(indexDirectory), true);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(100, true);
		BooleanQuery q = new BooleanQuery();
		String query_one = "White bread";
		String query_two = "amount of alcohol-free or low-alcohol beers per week";
		q.add(new QueryParser(Version.LUCENE_30, "measurement", new PorterStemAnalyzer()).parse(query_one),
				BooleanClause.Occur.SHOULD);
		q.add(new QueryParser(Version.LUCENE_30, "measurement", new PorterStemAnalyzer()).parse(query_two),
				BooleanClause.Occur.SHOULD);
		q.add(new QueryParser(Version.LUCENE_30, "investigation", new PorterStemAnalyzer()).parse("finrisk"),
				BooleanClause.Occur.MUST);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		System.out.println("Found " + hits.length + " hits.");
		for (int i = 0; i < hits.length; ++i)
		{
			int docId = hits[i].doc;
			double score = hits[i].score;
			Document d = searcher.doc(docId);
			System.out.println((i + 1) + ". " + d.get("measurement") + "\t" + score);
		}
		reader.close();
		searcher.close();
	}

	private static void searchIndexExact(File indexDirectory) throws CorruptIndexException, IOException, ParseException
	{
		IndexReader reader = IndexReader.open(FSDirectory.open(indexDirectory), true);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(1000, true);
		BooleanQuery q = new BooleanQuery();
		q.add(new TermQuery(new Term("ontologyTerm", "alcoholic beverage")), BooleanClause.Occur.MUST);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		System.out.println("Found " + hits.length + " hits.");
		String path = null;
		for (int i = 0; i < hits.length; ++i)
		{
			int docId = hits[i].doc;
			double score = hits[i].score;
			Document d = searcher.doc(docId);
			path = d.get("nodePath");
			System.out.println((i + 1) + ". " + d.get("ontologyTerm") + "\t" + score);
		}
		Query queryForChildren = new WildcardQuery(new Term("nodePath", path + "*"));
		collector = TopScoreDocCollector.create(10000, true);
		searcher.search(queryForChildren, collector);
		hits = collector.topDocs().scoreDocs;
		for (int i = 0; i < hits.length; ++i)
		{
			int docId = hits[i].doc;
			double score = hits[i].score;
			Document d = searcher.doc(docId);
			System.out.println((i + 1) + ". " + d.get("ontologyTerm") + "\t" + score);
		}
		reader.close();
		searcher.close();
	}

	private static void startIndex(File indexDirectory, File ontologyFile) throws CorruptIndexException,
			LockObtainFailedException, IOException, OntologyServiceException, OWLOntologyCreationException
	{
		boolean createIndex = true;
		// Delete existing index
		if (IndexReader.indexExists(FSDirectory.open(indexDirectory)))
		{
			IndexReader reader = IndexReader.open(FSDirectory.open(indexDirectory), false);
			TermEnum termsEnum = reader.terms();
			while (termsEnum.next())
			{
				Term term = termsEnum.term();
				if (term.field().equals("ontologyTerm")) reader.deleteDocuments(term);
				else if (term.field().equals("synonym")) reader.deleteDocuments(term);
				else if (term.field().equals("ontologyLabel")) reader.deleteDocuments(term);
				else if (term.field().equals("parent")) reader.deleteDocuments(term);
				else if (term.field().equals("parentSynonym")) reader.deleteDocuments(term);
				else if (term.field().equals("nodePath")) reader.deleteDocuments(term);
			}
			reader.close();
			createIndex = false;
		}
		IndexWriter writer = new IndexWriter(FSDirectory.open(indexDirectory), new KeywordAnalyzer(), createIndex,
				IndexWriter.MaxFieldLength.UNLIMITED);
		// OntologyService os = new FileOntologyService(ontologyFile.toURI());
		// for (Ontology ontology : os.getOntologies())
		// {
		// List<OntologyTerm> allTerms = os.getRootTerms(ontology);
		// for (OntologyTerm ot : allTerms)
		// System.out.println(ot);
		// String ontologyLabel = null;
		// if (allTerms != null)
		// {
		// int count = 0;
		// for (OntologyTerm term : allTerms)
		// {
		// if (os.getParents(term).size() == 0)
		// {
		// if (ontologyLabel == null)
		// {
		// if (ontology.getLabel() == null || ontology.getLabel().isEmpty())
		// ontologyLabel = ontology
		// .getOntologyAccession();
		// else
		// ontologyLabel = ontology.getLabel();
		// }
		// indexEachOntologyTerm("0." + count, ontologyLabel, term, os, writer);
		// count++;
		// }
		// }
		// }
		// }

		OWLModel owlModel = new OWLModel(ontologyFile.getAbsolutePath());
		int count = 0;
		for (OWLClass subClass : owlModel.getTopClasses())
		{
			indexEachOntologyTerm("0." + count, owlModel.getOntologyLabel(), subClass, owlModel, writer);
			count++;
		}

		writer.close();
	}

	public static void indexEachOntologyTerm(String path, String ontologyLabel, OWLClass term, OWLModel owlModel,
			IndexWriter writer) throws CorruptIndexException, IOException
	{
		Document document = new Document();
		document.add(new Field("nodePath", path, Field.Store.YES, Field.Index.ANALYZED));
		document.add(new Field("ontologyTerm", owlModel.getLabel(term).toLowerCase(), Field.Store.YES,
				Field.Index.ANALYZED));
		document.add(new Field("ontologyLabel", ontologyLabel.toLowerCase(), Field.Store.YES, Field.Index.ANALYZED));
		writer.addDocument(document);
		Set<OWLClass> listOfChildren = owlModel.getChildClass(term);
		if (listOfChildren.size() > 0)
		{
			int i = 0;
			for (OWLClass childClass : listOfChildren)
			{
				String childTermPath = path + "." + i;
				indexEachOntologyTerm(childTermPath, ontologyLabel, childClass, owlModel, writer);
				i++;
			}
		}
	}
	// private static String recursiveGet(Document document, OntologyTerm term,
	// OntologyService os)
	// {
	// List<OntologyTerm> listOfParentTerms = null;
	// try
	// {
	// listOfParentTerms = os.getParents(term);
	// }
	// catch (Exception e)
	// {
	// System.out.println("No parents!");
	// }
	// if (listOfParentTerms != null)
	// {
	// for (OntologyTerm parentTerm : listOfParentTerms)
	// {
	// document.add(new Field("parent", parentTerm.getLabel().toLowerCase(),
	// Field.Store.YES,
	// Field.Index.ANALYZED));
	// recursiveGet(document, parentTerm, os);
	// }
	// }
	// }
}