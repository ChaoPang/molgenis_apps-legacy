package plugins.luceneIndexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.StaleReaderException;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
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
import org.molgenis.io.csv.CsvReader;
import org.molgenis.util.tuple.Tuple;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import plugins.harmonizationPlugin.CreatePotentialTerms;
import plugins.ontologyTermInfo.OWLModel;
import uk.ac.ebi.ontocat.OntologyService;
import uk.ac.ebi.ontocat.OntologyService.SearchOptions;
import uk.ac.ebi.ontocat.OntologyServiceException;
import uk.ac.ebi.ontocat.OntologyTerm;
import uk.ac.ebi.ontocat.bioportal.BioportalOntologyService;
import uk.ac.ebi.ontocat.virtual.CachedServiceDecorator;

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
		// System.out.println("Started index.....");
		// File ontologyFolder = new
		// File("/Users/chaopang/Desktop/Ontologies/");
		// File ontologyTermFromBioportal = new
		// File("/Users/chaopang/Desktop/Variables/importHOP_20_Variables.csv");
		// boolean createIndex = removeExistingIndex(indexDirectory);
		// IndexWriter writer = new
		// IndexWriter(FSDirectory.open(indexDirectory), new KeywordAnalyzer(),
		// createIndex,
		// IndexWriter.MaxFieldLength.UNLIMITED);
		// for (File ontologyFile : ontologyFolder.listFiles())
		// if (ontologyFile.getName().toLowerCase().endsWith(".owl")
		// || ontologyFile.getName().toLowerCase().endsWith(".obo"))
		// startIndex(indexDirectory, ontologyFile,
		// writer);
		// startIndexFromBioPortal(indexDirectory, ontologyTermFromBioportal,
		// writer);
		// writer.close();
		// System.out.println("Finished index.....");
		// System.out.println("Started searching index.....");
		// searchIndexExact(indexDirectory);
		searchIndexStandard(indexDirectory);
	}

	private static void searchIndexStandard(File indexDirectory) throws CorruptIndexException, IOException,
			ParseException
	{
		IndexReader reader = IndexReader.open(FSDirectory.open(indexDirectory), true);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(100, true);
		BooleanQuery q = new BooleanQuery();
		List<String> query_one = Arrays.asList("current", "currently");
		List<String> query_two = Arrays.asList("smoker");
		List<String> query_three = Arrays.asList("cigarette", "beedi");

		q.add(new QueryParser(Version.LUCENE_30, "investigation", new PorterStemAnalyzer()).parse("ncd"),
				BooleanClause.Occur.MUST);
		BooleanQuery groupQuery = new BooleanQuery();
		groupQuery.add(new QueryParser(Version.LUCENE_30, "category", new PorterStemAnalyzer()).parse("current"),
				BooleanClause.Occur.SHOULD);
		groupQuery.add(new QueryParser(Version.LUCENE_30, "category", new PorterStemAnalyzer()).parse("cigarette"),
				BooleanClause.Occur.SHOULD);
		groupQuery.add(new QueryParser(Version.LUCENE_30, "category", new PorterStemAnalyzer()).parse("smoke"),
				BooleanClause.Occur.SHOULD);
		q.add(groupQuery, BooleanClause.Occur.MUST);
		// q.add(new QueryParser(Version.LUCENE_30, "measurement", new
		// PorterStemAnalyzer())
		// .parse("current cigarette smoke"), BooleanClause.Occur.SHOULD);
		// q.add(new QueryParser(Version.LUCENE_30, "category", new
		// PorterStemAnalyzer()).parse("current cigarette smoke"),
		// BooleanClause.Occur.MUST);
		// groupQuery.add(combineQueries(query_two), BooleanClause.Occur.MUST);
		// groupQuery.add(combineQueries(query_three),
		// BooleanClause.Occur.MUST);
		// q.add(groupQuery, BooleanClause.Occur.MUST);
		// q.add(new QueryParser(Version.LUCENE_30, "measurement", new
		// PorterStemAnalyzer())
		// .parse("current smoke cigarette"), BooleanClause.Occur.SHOULD);
		//
		// q.add(new QueryParser(Version.LUCENE_30, "category", new
		// PorterStemAnalyzer()).parse("current smoke cigarette"),
		// BooleanClause.Occur.SHOULD);
		// q.add(new QueryParser(Version.LUCENE_30, "measurement", new
		// PorterStemAnalyzer()).parse("current"),
		// BooleanClause.Occur.MUST);
		// q.add(new QueryParser(Version.LUCENE_30, "measurement", new
		// PorterStemAnalyzer()).parse("cigarette"),
		// BooleanClause.Occur.MUST);
		// q.add(new QueryParser(Version.LUCENE_30, "measurement", new
		// PorterStemAnalyzer()).parse("smoker"),
		// BooleanClause.Occur.MUST);
		// q.add(new QueryParser(Version.LUCENE_30, "measurement", new
		// PorterStemAnalyzer())
		// .parse("current use of alcohol"), BooleanClause.Occur.SHOULD);
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

	public static BooleanQuery combineQueries(List<String> listOfQueries) throws ParseException
	{
		BooleanQuery group = new BooleanQuery();
		for (String query : listOfQueries)
		{
			group.add(new QueryParser(Version.LUCENE_30, "measurement", new PorterStemAnalyzer()).parse(query),
					BooleanClause.Occur.SHOULD);
		}
		return group;
	}

	private static void searchIndexExact(File indexDirectory) throws CorruptIndexException, IOException, ParseException
	{
		IndexReader reader = IndexReader.open(FSDirectory.open(indexDirectory), true);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(1000, true);
		BooleanQuery q = new BooleanQuery();
		q.add(new TermQuery(new Term("ontologyTermSynonym", "sex")), BooleanClause.Occur.MUST);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		System.out.println("Found " + hits.length + " hits.");
		Set<String> path = new HashSet<String>();
		for (int i = 0; i < hits.length; ++i)
		{
			int docId = hits[i].doc;
			double score = hits[i].score;
			Document d = searcher.doc(docId);
			path.add(d.get("nodePath"));
			System.out.println((i + 1) + ". " + d.get("ontologyTerm") + "\t" + score);
			for (String synonym : d.getValues("ontologyTermSynonym"))
			{
				System.out.println(synonym);
			}
		}
		BooleanQuery finalQuery = new BooleanQuery();
		for (String nodePath : path)
		{
			Query queryForChildren = new WildcardQuery(new Term("nodePath", nodePath + "*"));
			finalQuery.add(queryForChildren, Occur.SHOULD);
		}
		collector = TopScoreDocCollector.create(10000, true);
		searcher.search(finalQuery, collector);
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

	private static void startIndexFromBioPortal(File indexDirectory, File indexInfo, IndexWriter writer)
			throws IOException, OntologyServiceException
	{
		CsvReader reader = new CsvReader(indexInfo, ',');
		Iterator<String> columnHeaders = reader.colNamesIterator();
		while (columnHeaders.hasNext())
		{
			System.out.println(columnHeaders.next());
		}
		Iterator<Tuple> rows = reader.iterator();
		Set<OntologyTerm> collectedOntologyTerms = new HashSet<OntologyTerm>();
		while (rows.hasNext())
		{
			Tuple eachRow = rows.next();
			String definition = eachRow.getString("Definition");
			String label = eachRow.getString("Label");
			List<List<String>> blocks = new ArrayList<List<String>>();
			if (definition != null && !definition.isEmpty())
			{
				for (String eachDefinition : definition.split(";"))
				{
					blocks.add(Arrays.asList(eachDefinition.split(",")));
				}
			}
			else
				blocks = CreatePotentialTerms.getTermsLists(Arrays.asList(label.split(" ")));

			for (List<String> groupOfQueries : blocks)
			{
				for (String eachQuery : groupOfQueries)
				{
					OntologyTerm ot = collectOntologyTermFromBioportal(eachQuery);
					if (ot != null) collectedOntologyTerms.add(ot);
				}
			}

		}
		System.out.println("Parent concepts have been collected!");
		collectOntologyTermSubTree(collectedOntologyTerms, writer);
		reader.close();
	}

	private static void collectOntologyTermSubTree(Set<OntologyTerm> collectedOntologyTerms, IndexWriter writer)
			throws OntologyServiceException, CorruptIndexException, IOException
	{
		BioportalOntologyService os = new BioportalOntologyService();
		for (OntologyTerm ot : collectedOntologyTerms)
		{
			String nodePath = ot.getAccession();
			storeBioPortalTerm(nodePath, ot, os, writer);
			for (OntologyTerm subTerm : os.getAllChildren(ot))
			{
				String subNodePath = nodePath + "." + subTerm.getAccession();
				storeBioPortalTerm(subNodePath, subTerm, os, writer);
			}
			System.out.println("OntologyTerm : " + ot.getLabel()
					+ " has been collected for information on all of its children!");
		}
	}

	private static void storeBioPortalTerm(String nodePath, OntologyTerm ot, BioportalOntologyService os,
			IndexWriter writer) throws OntologyServiceException, CorruptIndexException, IOException
	{
		Document document = new Document();
		document.add(new Field("nodePath", nodePath, Field.Store.YES, Field.Index.ANALYZED));
		document.add(new Field("ontologyTerm", ot.getLabel().toLowerCase(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		document.add(new Field("ontologyTermIRI", ot.getURI().toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		document.add(new Field("ontologyTermSynonym", ot.getLabel().toLowerCase(), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		document.add(new Field("ontologyLabel", ot.getLabel().toLowerCase(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		for (String synonym : os.getSynonyms(ot))
		{
			document.add(new Field("ontologyTermSynonym", synonym.toLowerCase(), Field.Store.YES,
					Field.Index.NOT_ANALYZED));
		}
		writer.addDocument(document);
	}

	private static OntologyTerm collectOntologyTermFromBioportal(String eachQuery) throws OntologyServiceException
	{
		OntologyService os = CachedServiceDecorator.getService(new BioportalOntologyService());
		List<OntologyTerm> ontologyTermResults = os.searchOntology("1353", eachQuery, SearchOptions.EXACT);
		OntologyTerm ot = null;
		if (ontologyTermResults.size() > 0) ot = ontologyTermResults.get(0);
		return ot;
	}

	private static boolean removeExistingIndex(File indexDirectory) throws CorruptIndexException, StaleReaderException,
			LockObtainFailedException, IOException
	{
		boolean createIndex = true;
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
				else if (term.field().equals("ontologyTermSynonym")) reader.deleteDocuments(term);
				else if (term.field().equals("nodePath")) reader.deleteDocuments(term);
			}
			reader.close();
			createIndex = false;
		}
		return createIndex;
	}

	private static void startIndex(File indexDirectory, File ontologyFile, IndexWriter writer)
			throws CorruptIndexException, LockObtainFailedException, IOException, OntologyServiceException,
			OWLOntologyCreationException
	{

		OWLModel owlModel = new OWLModel(ontologyFile.getAbsolutePath());
		int count = 0;
		for (OWLClass subClass : owlModel.getTopClasses())
		{
			indexEachOntologyTerm("0." + count, owlModel.getOntologyLabel(), subClass, owlModel, writer);
			count++;
		}
	}

	public static void indexEachOntologyTerm(String path, String ontologyLabel, OWLClass term, OWLModel owlModel,
			IndexWriter writer) throws CorruptIndexException, IOException
	{
		Document document = new Document();
		document.add(new Field("nodePath", path, Field.Store.YES, Field.Index.ANALYZED));
		document.add(new Field("ontologyTerm", owlModel.getLabel(term).toLowerCase(), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		document.add(new Field("ontologyTermIRI", term.getIRI().toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		document.add(new Field("ontologyTermSynonym", owlModel.getLabel(term).toLowerCase(), Field.Store.YES,
				Field.Index.NOT_ANALYZED));
		document.add(new Field("ontologyLabel", ontologyLabel.toLowerCase(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		for (String synonym : owlModel.getSynonyms(term))
		{
			document.add(new Field("ontologyTermSynonym", synonym.toLowerCase(), Field.Store.YES,
					Field.Index.NOT_ANALYZED));
		}
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
}