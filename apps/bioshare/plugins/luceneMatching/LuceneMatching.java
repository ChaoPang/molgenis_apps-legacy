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
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
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
import plugins.normalMatching.MappingList;
import plugins.ontologyTermInfo.OntologyTermContainer;
import plugins.quartzJob.TermExpansionJob;

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

	private Map<String, String> getTermExpansion(List<String> buildingBlocks) throws IOException
	{
		List<List<String>> potentialBlocks = new ArrayList<List<String>>();
		for (String eachBlock : buildingBlocks)
		{
			potentialBlocks.add(Arrays.asList(eachBlock.split(",")));
		}
		return combineTermByIndex(potentialBlocks, true);
	}

	private Map<String, String> getTermExpansion(String predictorLabel) throws IOException
	{
		List<List<String>> potentialBlocks = CreatePotentialTerms
				.getTermsLists(Arrays.asList(predictorLabel.split(" ")));
		return combineTermByIndex(potentialBlocks, true);
	}

	public Map<String, String> combineTermByIndex(List<List<String>> potentialBlocks, boolean includeChildren)
			throws IOException
	{
		Map<String, String> expandedQueries = new HashMap<String, String>();
		Map<String, Set<OntologyTermContainer>> mapForBlocks = new HashMap<String, Set<OntologyTermContainer>>();
		boolean possibleBlocks = false;
		for (List<String> eachSetOfBlocks : potentialBlocks)
		{
			for (String eachBlock : eachSetOfBlocks)
			{
				Set<OntologyTermContainer> listOfOntologyTerms = null;
				if (includeChildren) listOfOntologyTerms = getOntologyTermsFromIndex(eachBlock);
				else
					listOfOntologyTerms = getOntologyTermSynonymsFromIndex(eachBlock);

				for (OntologyTermContainer ontologyTerm : listOfOntologyTerms)
					if (model != null && !model.getCachedOntologyTerms().containsKey(ontologyTerm.getOntologyTermID())) model
							.getCachedOntologyTerms().put(ontologyTerm.getOntologyTermID(), ontologyTerm);
				mapForBlocks.put(eachBlock, listOfOntologyTerms);
				if (mapForBlocks.get(eachBlock).size() > 0) possibleBlocks = true;
			}
			if (possibleBlocks) expandedQueries.putAll(TermExpansionJob.resursiveCombineList(mapForBlocks,
					new HashMap<String, String>()));
		}
		return expandedQueries;
	}

	public Set<String> searchForOntologyTermPaths(String eachBlock) throws IOException
	{
		Set<String> nodePathSet = new HashSet<String>();
		BooleanQuery q = new BooleanQuery();
		q.add(new TermQuery(new Term("ontologyTerm", eachBlock.toLowerCase())), BooleanClause.Occur.MUST);
		TopScoreDocCollector collector = TopScoreDocCollector.create(1000, true);
		luceneSearcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		for (int i = 0; i < hits.length; i++)
		{
			int docId = hits[i].doc;
			Document d = luceneSearcher.doc(docId);
			nodePathSet.add(d.get("nodePath"));
		}
		return nodePathSet;
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
					new ArrayList<String>(), d.get("ontologyTerm"), d.get("ontologyLabel"));
			for (String synonym : d.getValues("ontologyTermSynonym"))
				ontologyContainer.getSynonyms().add(synonym);
			if (!listOfOntologyTerms.contains(ontologyContainer)) listOfOntologyTerms.add(ontologyContainer);
		}
		return listOfOntologyTerms;
	}

	public Set<OntologyTermContainer> getOntologyTermsFromIndex(String eachBlock) throws IOException
	{
		Set<OntologyTermContainer> listOfOntologyTerms = new HashSet<OntologyTermContainer>();
		if (eachBlock == null) eachBlock = StringUtils.EMPTY;
		Set<String> nodePathSet = searchForOntologyTermPaths(eachBlock);
		BooleanQuery finalQuery = new BooleanQuery();
		for (String nodePath : nodePathSet)
			finalQuery.add(new WildcardQuery(new Term("nodePath", nodePath + "*")), Occur.SHOULD);
		TopScoreDocCollector collector = TopScoreDocCollector.create(10000, true);
		luceneSearcher.search(finalQuery, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		for (int i = 0; i < hits.length; ++i)
		{
			int docId = hits[i].doc;
			Document d = luceneSearcher.doc(docId);
			OntologyTermContainer ontologyContainer = new OntologyTermContainer(d.get("ontologyTermIRI"),
					new ArrayList<String>(), d.get("ontologyTerm"), d.get("ontologyLabel"));
			for (String synonym : d.getValues("ontologyTermSynonym"))
				ontologyContainer.getSynonyms().add(synonym);
			if (!listOfOntologyTerms.contains(ontologyContainer)) listOfOntologyTerms.add(ontologyContainer);
		}
		return listOfOntologyTerms;
	}

	public void getMatchingResult() throws NumberFormatException, Exception
	{
		for (PredictorInfo predictor : model.getPredictors().values())
		{
			if (predictor.getBuildingBlocks().size() > 0) predictor.getExpandedQuery().putAll(
					getTermExpansion(predictor.getBuildingBlocks()));
			else
				predictor.getExpandedQuery().putAll(getTermExpansion(predictor.getLabel()));
			predictor.getExpandedQuery().put(predictor.getLabel(), predictor.getLabel());
			Map<String, MappingList> mappingsForStudies = new HashMap<String, MappingList>();
			Map<String, String> expandedQueries = predictor.getExpandedQuery();
			for (String eachStudy : model.getSelectedValidationStudy())
			{
				MappingList mapping = new MappingList();
				for (Entry<String, String> entry : expandedQueries.entrySet())
				{
					BooleanQuery q = new BooleanQuery(true);
					String matchingString = entry.getKey().replaceAll("[^a-zA-Z0-9 ]", " ");
					q.add(new QueryParser(Version.LUCENE_30, "investigation", new PorterStemAnalyzer()).parse(eachStudy
							.toLowerCase()), BooleanClause.Occur.MUST);
					q.add(new QueryParser(Version.LUCENE_30, "measurement", new PorterStemAnalyzer())
							.parse(matchingString), BooleanClause.Occur.SHOULD);
					q.add(new QueryParser(Version.LUCENE_30, "category", new PorterStemAnalyzer())
							.parse(matchingString), BooleanClause.Occur.SHOULD);
					TopScoreDocCollector collector = TopScoreDocCollector.create(100, true);
					luceneSearcher.search(q, collector);
					ScoreDoc[] hits = collector.topDocs().scoreDocs;
					for (ScoreDoc hit : hits)
					{
						int docId = hit.doc;
						double score = hit.score;
						Document d = luceneSearcher.doc(docId);
						DecimalFormat df = new DecimalFormat("#0.000");
						score = Double.parseDouble(df.format(score));
						mapping.add(entry.getValue(), Integer.parseInt(d.get("measurementID")), score);
					}
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