package plugins.luceneMatching;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import plugins.harmonizationPlugin.CreatePotentialTerms;
import plugins.harmonizationPlugin.HarmonizationModel;
import plugins.harmonizationPlugin.PredictorInfo;
import plugins.luceneIndexer.PorterStemAnalyzer;
import plugins.normalMatching.LinkedInformation;
import plugins.normalMatching.MappingList;
import plugins.ontologyTermInfo.OntologyTermContainer;

public class LuceneMatching
{
	private HarmonizationModel model;
	private File indexDirectory;
	private IndexReader luceneReader;
	private IndexSearcher luceneSearcher;

	public LuceneMatching(HarmonizationModel model) throws CorruptIndexException, IOException
	{
		this.model = model;
		// TODO: the index should be stored in and loaded from the app.
		indexDirectory = new File("/Users/chaopang/Desktop/TempIndex");
		luceneReader = IndexReader.open(FSDirectory.open(indexDirectory), true);
		luceneSearcher = new IndexSearcher(luceneReader);
	}

	public LuceneMatching(File indexDirectory) throws CorruptIndexException, IOException
	{
		this.model = null;
		this.indexDirectory = indexDirectory;
		luceneReader = IndexReader.open(FSDirectory.open(indexDirectory), true);
		luceneSearcher = new IndexSearcher(luceneReader);
	}

	private List<Map<String, Set<OntologyTermContainer>>> getTermExpansion(String label, List<String> buildingBlocks)
			throws IOException
	{
		List<List<String>> potentialBlocks = new ArrayList<List<String>>();
		for (String eachBlock : buildingBlocks)
		{
			potentialBlocks.add(Arrays.asList(eachBlock.split(",")));
		}
		List<String> storeLabel = new ArrayList<String>();
		storeLabel.add(label);
		potentialBlocks.add(storeLabel);
		return combineTermByIndex(potentialBlocks, true);
	}

	private List<Map<String, Set<OntologyTermContainer>>> getTermExpansion(String predictorLabel) throws IOException
	{
		List<List<String>> potentialBlocks = CreatePotentialTerms
				.getTermsLists(Arrays.asList(predictorLabel.split(" ")));
		return combineTermByIndex(potentialBlocks, true);
	}

	public List<Map<String, Set<OntologyTermContainer>>> combineTermByIndex(List<List<String>> potentialBlocks,
			boolean includeChildren) throws IOException
	{
		List<Map<String, Set<OntologyTermContainer>>> listOfDefinitions = new ArrayList<Map<String, Set<OntologyTermContainer>>>();
		for (List<String> eachSetOfBlocks : potentialBlocks)
		{
			Map<String, Set<OntologyTermContainer>> definition = new HashMap<String, Set<OntologyTermContainer>>();
			for (String eachBlock : eachSetOfBlocks)
			{
				Set<OntologyTermContainer> listOfOntologyTerms = null;
				if (includeChildren) listOfOntologyTerms = getOntologyTermsFromIndex(eachBlock);
				else
					listOfOntologyTerms = getOntologyTermSynonymsFromIndex(eachBlock);

				for (OntologyTermContainer ontologyTerm : listOfOntologyTerms)
					if (model != null && !model.getCachedOntologyTerms().containsKey(ontologyTerm.getOntologyTermID())) model
							.getCachedOntologyTerms().put(ontologyTerm.getOntologyTermID(), ontologyTerm);
				if (!definition.containsKey(eachBlock.toLowerCase()))
				{
					definition.put(eachBlock.toLowerCase(), listOfOntologyTerms);
				}
			}
			listOfDefinitions.add(definition);
		}
		return listOfDefinitions;
	}

	public Set<OntologyTermContainer> searchForOntologyTermPaths(String eachBlock, String field) throws IOException
	{
		Set<OntologyTermContainer> listOfOntologyTerms = new HashSet<OntologyTermContainer>();
		BooleanQuery q = new BooleanQuery();
		q.add(new TermQuery(new Term(field, eachBlock.toLowerCase())), BooleanClause.Occur.MUST);
		TopScoreDocCollector collector = TopScoreDocCollector.create(1000, true);
		luceneSearcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		for (int i = 0; i < hits.length; i++)
		{
			int docId = hits[i].doc;
			Document d = luceneSearcher.doc(docId);
			OntologyTermContainer ontologyContainer = new OntologyTermContainer(d.get("ontologyTermIRI"),
					d.get("nodePath"), new ArrayList<String>(), d.get("ontologyTerm"), d.get("ontologyLabel"));
			for (String synonym : d.getValues("ontologyTermSynonym"))
				ontologyContainer.getSynonyms().add(synonym);
			if (!ontologyContainer.getSynonyms().contains(eachBlock)) ontologyContainer.getSynonyms().add(eachBlock);
			if (!listOfOntologyTerms.contains(ontologyContainer)) listOfOntologyTerms.add(ontologyContainer);
		}
		return listOfOntologyTerms;
	}

	public Set<OntologyTermContainer> getOntologyTermSynonymsFromIndex(String eachBlock) throws IOException
	{
		Set<OntologyTermContainer> listOfOntologyTerms = new HashSet<OntologyTermContainer>();
		BooleanQuery q = new BooleanQuery();
		q.add(new TermQuery(new Term("ontologyTermSynonym", eachBlock.toLowerCase())), BooleanClause.Occur.MUST);
		TopScoreDocCollector collector = TopScoreDocCollector.create(1000, true);
		luceneSearcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		for (int i = 0; i < hits.length; i++)
		{
			int docId = hits[i].doc;
			Document d = luceneSearcher.doc(docId);
			OntologyTermContainer ontologyContainer = new OntologyTermContainer(d.get("ontologyTermIRI"),
					d.get("nodePath"), new ArrayList<String>(), d.get("ontologyTerm"), d.get("ontologyLabel"));
			for (String synonym : d.getValues("ontologyTermSynonym"))
				ontologyContainer.getSynonyms().add(synonym);
			if (!listOfOntologyTerms.contains(ontologyContainer)) listOfOntologyTerms.add(ontologyContainer);
		}
		if (listOfOntologyTerms.size() == 0)
		{
			OntologyTermContainer originalTerm = new OntologyTermContainer("original", "", new ArrayList<String>(),
					eachBlock.toLowerCase(), "local");
			listOfOntologyTerms.add(originalTerm);
		}
		return listOfOntologyTerms;
	}

	public Set<OntologyTermContainer> getOntologyTermsFromIndex(String eachBlock) throws IOException
	{
		Set<OntologyTermContainer> listOfOntologyTerms = new HashSet<OntologyTermContainer>();
		if (eachBlock == null) eachBlock = StringUtils.EMPTY;
		else
			eachBlock = eachBlock.toLowerCase();
		Set<OntologyTermContainer> originalTerms = searchForOntologyTermPaths(eachBlock, "ontologyTermSynonym");
		// if (originalTerms.size() == 0) originalTerms =
		// searchForOntologyTermPaths(eachBlock, "ontologyTermSynonym");
		listOfOntologyTerms.addAll(originalTerms);

		BooleanQuery finalQuery = new BooleanQuery();
		for (OntologyTermContainer eachOrignalTerm : originalTerms)
			finalQuery.add(new WildcardQuery(new Term("nodePath", eachOrignalTerm.getNodePath() + ".*")), Occur.SHOULD);
		TopScoreDocCollector collector = TopScoreDocCollector.create(10000, true);
		luceneSearcher.search(finalQuery, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		for (int i = 0; i < hits.length; ++i)
		{
			int docId = hits[i].doc;
			Document d = luceneSearcher.doc(docId);
			OntologyTermContainer ontologyContainer = new OntologyTermContainer(d.get("ontologyTermIRI"), "",
					new ArrayList<String>(), d.get("ontologyTerm"), d.get("ontologyLabel"));
			for (String synonym : d.getValues("ontologyTermSynonym"))
				ontologyContainer.getSynonyms().add(synonym);
			// if (!ontologyContainer.getSynonyms().contains(eachBlock))
			// ontologyContainer.getSynonyms().add(eachBlock);
			if (!listOfOntologyTerms.contains(ontologyContainer)) listOfOntologyTerms.add(ontologyContainer);
		}
		if (listOfOntologyTerms.size() == 0)
		{
			OntologyTermContainer originalTerm = new OntologyTermContainer("original-" + eachBlock, "",
					new ArrayList<String>(), eachBlock, "local");
			originalTerm.getSynonyms().add(eachBlock);
			listOfOntologyTerms.add(originalTerm);
		}
		return listOfOntologyTerms;
	}

	public void normalizeScore(MappingList mapping, String eachStudy) throws ParseException, IOException
	{
		int iterationNumber = 0;
		BooleanQuery finalQuery = new BooleanQuery(true);
		finalQuery.add(new QueryParser(Version.LUCENE_30, "investigation", new PorterStemAnalyzer()).parse(eachStudy
				.toLowerCase()), BooleanClause.Occur.MUST);
		Map<String, String> expandedQueries = new HashMap<String, String>();
		for (LinkedInformation info : mapping.getSortedInformation())
		{
			if (iterationNumber < 1000)
			{
				if (!expandedQueries.containsKey(info.getExpandedQuery())) expandedQueries.put(info.getExpandedQuery(),
						info.getDisplayedLabel());
				iterationNumber++;
			}
			else
				break;
		}
		for (String query : expandedQueries.keySet())
		{
			finalQuery.add(new QueryParser(Version.LUCENE_30, "measurement", new PorterStemAnalyzer()).parse(query
					.toLowerCase()), BooleanClause.Occur.SHOULD);
			finalQuery
					.add(new QueryParser(Version.LUCENE_30, "category", new PorterStemAnalyzer()).parse(query
							.toLowerCase()), BooleanClause.Occur.SHOULD);
		}
		TopScoreDocCollector collector = TopScoreDocCollector.create(1000, true);
		luceneSearcher.search(finalQuery, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		for (ScoreDoc hit : hits)
		{
			int docId = hit.doc;
			double score = hit.score;
			Document d = luceneSearcher.doc(docId);
			DecimalFormat df = new DecimalFormat("#0.000");
			score = Double.parseDouble(df.format(score));
			Integer featureID = Integer.parseInt(d.get("measurementID"));
			mapping.updateScore(featureID, score);
		}
	}

	public DisjunctionMaxQuery createQueriesFromOntologyTerm(String originalTerm,
			Set<OntologyTermContainer> listOfOntologyTerms, Set<OntologyTermContainer> boostedOntologyTerms,
			int maxClauses) throws ParseException
	{
		DisjunctionMaxQuery queryPerTerm = new DisjunctionMaxQuery(0);
		for (OntologyTermContainer ontologyTerm : listOfOntologyTerms)
		{
			boolean boosted = false;
			if (boostedOntologyTerms != null && boostedOntologyTerms.contains(ontologyTerm)) boosted = true;
			for (String synonym : ontologyTerm.getSynonyms())
			{
				DisjunctionMaxQuery queryPerOntologyTerm = new DisjunctionMaxQuery(0);
				synonym = synonym.replaceAll("[^0-9a-zA-Z ]", "").trim().toLowerCase();
				if (boosted)
				{
					String terms[] = synonym.split(" ");
					String combinedTerm = StringUtils.EMPTY;
					for (String eachTerm : terms)
					{
						if (!eachTerm.replaceAll(" ", "").isEmpty()) combinedTerm += eachTerm + "^4 ";
					}
					synonym = combinedTerm.trim();
				}
				queryPerOntologyTerm.add(new QueryParser(Version.LUCENE_30, "measurement", new PorterStemAnalyzer())
						.parse(synonym));
				queryPerOntologyTerm.add(new QueryParser(Version.LUCENE_30, "category", new PorterStemAnalyzer())
						.parse(synonym));
				queryPerTerm.add(queryPerOntologyTerm);
			}
		}
		return queryPerTerm;
	}

	public void getMatchingResult() throws NumberFormatException, Exception
	{
		for (PredictorInfo predictor : model.getPredictors().values())
		{
			List<Map<String, Set<OntologyTermContainer>>> ontologyTermExpansion = null;
			if (predictor.getBuildingBlocks().size() > 0) ontologyTermExpansion = getTermExpansion(
					predictor.getLabel(), predictor.getBuildingBlocks());
			else
				ontologyTermExpansion = getTermExpansion(predictor.getLabel());
			String leadingElement = predictor.getLeadingElement();
			Set<OntologyTermContainer> boostedTerms = null;
			if (leadingElement != null)
			{
				boostedTerms = new HashSet<OntologyTermContainer>();
				for (String eachLeadingElement : leadingElement.split(";"))
					boostedTerms.addAll(getOntologyTermsFromIndex(eachLeadingElement.trim()));
			}
			Map<String, MappingList> mappingsForStudies = new HashMap<String, MappingList>();
			for (String eachStudy : model.getSelectedValidationStudy())
			{
				MappingList mapping = new MappingList();
				DisjunctionMaxQuery finalQuery = new DisjunctionMaxQuery(0);
				for (Map<String, Set<OntologyTermContainer>> eachDefinition : ontologyTermExpansion)
				{
					BooleanQuery groupQuery = new BooleanQuery();
					groupQuery.add(new QueryParser(Version.LUCENE_30, "investigation", new PorterStemAnalyzer())
							.parse(eachStudy.toLowerCase()), BooleanClause.Occur.MUST);
					int defaultMaxClauses = BooleanQuery.getMaxClauseCount();
					for (Entry<String, Set<OntologyTermContainer>> entry : eachDefinition.entrySet())
					{
						groupQuery.add(
								createQueriesFromOntologyTerm(entry.getKey(), entry.getValue(), boostedTerms,
										BooleanQuery.getMaxClauseCount()), BooleanClause.Occur.SHOULD);
					}
					finalQuery.add(groupQuery);
					BooleanQuery.setMaxClauseCount(defaultMaxClauses);
				}
				TopScoreDocCollector collector = TopScoreDocCollector.create(100, true);
				luceneSearcher.search(finalQuery, collector);
				ScoreDoc[] hits = collector.topDocs().scoreDocs;
				for (ScoreDoc hit : hits)
				{
					int docId = hit.doc;
					double score = hit.score;
					Document d = luceneSearcher.doc(docId);
					DecimalFormat df = new DecimalFormat("#0.000");
					// Explanation explain = luceneSearcher.explain(finalQuery,
					// docId);
					// System.out.println(explain);
					score = Double.parseDouble(df.format(score));
					mapping.add(d.get("measurementID").toString(), "", Integer.parseInt(d.get("measurementID")), score);
				}
				mappingsForStudies.put(eachStudy, mapping);
				model.incrementFinishedJob();
			}
			predictor.setMappings(mappingsForStudies);
		}
		luceneReader.close();
		luceneSearcher.close();
	}
}