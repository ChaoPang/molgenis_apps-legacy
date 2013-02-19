package plugins.harmonization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import uk.ac.ebi.ontocat.OntologyService.SearchOptions;
import uk.ac.ebi.ontocat.OntologyServiceException;
import uk.ac.ebi.ontocat.bioportal.BioportalOntologyService;

public class TermExpansionJob implements Job
{
	@Override
	@SuppressWarnings("unchecked")
	public void execute(JobExecutionContext context) throws JobExecutionException
	{
		try
		{
			if (context.getJobDetail().getJobDataMap().get("predictors") instanceof List<?>)
			{
				List<PredictorInfo> predictors = (List<PredictorInfo>) context.getJobDetail().getJobDataMap()
						.get("predictors");

				HarmonizationModel model = (HarmonizationModel) context.getJobDetail().getJobDataMap().get("model");
				Set<String> stopWords = model.getMatchingModel().getStopWords();
				int count = 0;
				BioportalOntologyService os = new BioportalOntologyService();

				for (PredictorInfo predictor : predictors)
				{
					if (predictor.getBuildingBlocks().size() > 0)
					{
						for (String eachBlock : predictor.getBuildingBlocks())
						{
							predictor.getExpandedQuery().putAll(
									expandQueryByDefinedBlocks(eachBlock.split(","), stopWords, model, os));
						}
					}
					else
					{
						predictor.getExpandedQuery().putAll(
								expandByPotentialBuildingBlocks(predictor.getLabel(), stopWords, model, os));
					}
					predictor.getExpandedQuery().put(predictor.getLabel(), predictor.getLabel());
					model.setTotalNumber((model.getTotalNumber() + predictor.getExpandedQuery().size())
							* model.getSelectedValidationStudy().size());
					count++;
					model.incrementFinishedJob();
					System.out.println("Finished: " + count + " out of " + predictors.size() + ". The predictor "
							+ predictor.getLabel() + " has " + predictor.getExpandedQuery().size()
							+ " expanded queries!");
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public Map<String, String> expandByPotentialBuildingBlocks(String predictorLabel, Set<String> stopWords,
			HarmonizationModel model, BioportalOntologyService os) throws OntologyServiceException
	{
		Map<String, String> expandedQueries = new HashMap<String, String>();
		List<List<String>> potentialBlocks = CreatePotentialTerms
				.getTermsLists(Arrays.asList(predictorLabel.split(" ")));
		Map<String, Set<OntologyTermContainer>> mapForBlocks = new LinkedHashMap<String, Set<OntologyTermContainer>>();
		boolean possibleBlocks = false;

		for (List<String> eachSetOfBlocks : potentialBlocks)
		{
			for (String eachBlock : eachSetOfBlocks)
			{
				if (!stopWords.contains(eachBlock.toLowerCase()))
				{
					mapForBlocks.put(eachBlock, collectInfoFromOntology(eachBlock.toLowerCase().trim(), model, os));
					if (mapForBlocks.get(eachBlock).size() > 1) possibleBlocks = true;
				}
			}
			if (possibleBlocks) expandedQueries
					.putAll(resursiveCombineList(mapForBlocks, new HashMap<String, String>()));

		}
		return expandedQueries;
	}

	public Map<String, String> expandQueryByDefinedBlocks(String[] buildingBlocksArray, Set<String> stopWords,
			HarmonizationModel model, BioportalOntologyService os) throws OntologyServiceException
	{
		Map<String, Set<OntologyTermContainer>> mapForBlocks = new LinkedHashMap<String, Set<OntologyTermContainer>>();
		Map<String, String> expandedQueries = new HashMap<String, String>();
		for (String eachBlock : Arrays.asList(buildingBlocksArray))
		{
			if (!stopWords.contains(eachBlock.toLowerCase()))
			{
				mapForBlocks.put(eachBlock, collectInfoFromOntology(eachBlock.toLowerCase().trim(), model, os));
			}
		}
		expandedQueries.putAll(resursiveCombineList(mapForBlocks, new HashMap<String, String>()));
		return expandedQueries;
	}

	public Map<String, String> resursiveCombineList(Map<String, Set<OntologyTermContainer>> mapForBlocks,
			Map<String, String> combinedLists)
	{
		Map<String, String> newCombinedLists = new HashMap<String, String>();
		if (mapForBlocks.size() > 0)
		{
			int count = 0;
			String lastKey = null;

			for (Entry<String, Set<OntologyTermContainer>> entry : mapForBlocks.entrySet())
			{
				if (count > 0) break;
				lastKey = entry.getKey();

				if (entry.getValue().size() == 0)
				{
					StringBuilder matchedString = new StringBuilder();
					StringBuilder displayedString = new StringBuilder();
					if (combinedLists.size() == 0) combinedLists.put(entry.getKey(), entry.getKey());
					else
					{
						for (Entry<String, String> mapEntry : combinedLists.entrySet())
						{
							matchedString.delete(0, matchedString.length());
							displayedString.delete(0, displayedString.length());
							matchedString.append(mapEntry.getKey()).append(' ').append(lastKey);
							displayedString.append(mapEntry.getValue()).append(' ').append(lastKey);
							newCombinedLists.put(matchedString.toString(), displayedString.toString());
						}
					}
				}
				else
				{
					for (OntologyTermContainer ontologyTerm : entry.getValue())
					{
						String ontologyTermId = ontologyTerm.getOntologyTermID();
						ontologyTerm.getSynonyms().add(ontologyTerm.getLabel());
						for (String eachSynonym : ontologyTerm.getSynonyms())
						{
							StringBuilder matchedString = new StringBuilder();
							StringBuilder displayedString = new StringBuilder();
							if (combinedLists.size() == 0)
							{
								displayedString.delete(0, displayedString.length());
								displayedString.append("<span id=\"").append(ontologyTermId.toString())
										.append("\" class=\"label label-info\">").append(eachSynonym).append("</span>");
								newCombinedLists.put(eachSynonym, displayedString.toString());
							}
							else
							{
								for (Entry<String, String> mapEntry : combinedLists.entrySet())
								{
									matchedString.delete(0, matchedString.length());
									displayedString.delete(0, displayedString.length());
									matchedString.append(mapEntry.getKey()).append(' ').append(eachSynonym);
									displayedString.append(mapEntry.getValue()).append(' ').append("<span id=\"")
											.append(ontologyTermId.toString()).append("\" class=\"label label-info\">")
											.append(eachSynonym).append("</span>");
									newCombinedLists.put(matchedString.toString(), displayedString.toString());
								}
							}
						}
					}
				}
				count++;
			}
			if (lastKey != null) mapForBlocks.remove(lastKey);
		}
		if (mapForBlocks.size() > 0) newCombinedLists = resursiveCombineList(mapForBlocks, newCombinedLists);

		return newCombinedLists;
	}

	public Set<OntologyTermContainer> collectInfoFromOntology(String queryToExpand, HarmonizationModel model,
			BioportalOntologyService os) throws OntologyServiceException
	{
		Set<OntologyTermContainer> expandedQueries = new HashSet<OntologyTermContainer>();
		for (uk.ac.ebi.ontocat.OntologyTerm ot : os.searchAll(queryToExpand, SearchOptions.EXACT))
		{
			if (model.getOntologyAccessions().contains(ot.getOntologyAccession()))
			{
				String label = null;
				if (ot.getLabel() != null) label = ot.getLabel();
				List<String> definition = null;
				if (os.getDefinitions(ot) != null) definition = os.getDefinitions(ot);
				OntologyTermContainer ontologyTerm = new OntologyTermContainer(ot.getURI().toString(), definition,
						label, ot.getOntologyAccession());
				expandedQueries.add(ontologyTerm);
				model.getCachedOntologyTerms().put(ontologyTerm.getOntologyTermID().toString(), ontologyTerm);
				if (os.getSynonyms(ot) != null)
				{
					for (String synonym : os.getSynonyms(ot))
					{
						ontologyTerm.getSynonyms().add(synonym);
					}
				}
				try
				{
					if (os.getChildren(ot) != null) recursiveAddTerms(ot, expandedQueries, model, os);
				}
				catch (Exception e)
				{
					System.out.println(ot.getLabel() + " does not have children!");
				}
			}
		}
		return expandedQueries;
	}

	private void recursiveAddTerms(uk.ac.ebi.ontocat.OntologyTerm ot, Set<OntologyTermContainer> expandedQueries,
			HarmonizationModel model, BioportalOntologyService os) throws OntologyServiceException
	{
		for (uk.ac.ebi.ontocat.OntologyTerm childOt : os.getChildren(ot))
		{
			if (!containsParent(ot, childOt, os))
			{
				String label = null;
				if (childOt.getLabel() != null) label = childOt.getLabel();
				List<String> definition = null;
				if (os.getDefinitions(childOt) != null) definition = os.getDefinitions(childOt);
				OntologyTermContainer ontologyTerm = new OntologyTermContainer(childOt.getURI().toString(), definition,
						label, childOt.getOntologyAccession());
				model.getCachedOntologyTerms().put(ontologyTerm.getOntologyTermID().toString(), ontologyTerm);
				if (os.getSynonyms(childOt) != null)
				{
					for (String synonym : os.getSynonyms(childOt))
					{
						ontologyTerm.getSynonyms().add(synonym);
					}
				}
				expandedQueries.add(ontologyTerm);
				try
				{
					if (os.getChildren(childOt) != null) recursiveAddTerms(childOt, expandedQueries, model, os);
				}
				catch (Exception e)
				{
				}
			}
		}
	}

	public boolean containsParent(uk.ac.ebi.ontocat.OntologyTerm parentTerm, uk.ac.ebi.ontocat.OntologyTerm childTerm,
			BioportalOntologyService os) throws OntologyServiceException
	{
		String parentLabel = (parentTerm.getLabel() == null ? StringUtils.EMPTY : parentTerm.getLabel().toLowerCase());
		String childLabel = (childTerm.getLabel() == null ? StringUtils.EMPTY : childTerm.getLabel().toLowerCase());

		if (!parentLabel.isEmpty() && !childLabel.isEmpty() && childLabel.contains(parentLabel)) return true;

		List<String> synonymsForParentTerm = os.getSynonyms(parentTerm);
		List<String> synonymsForChildTerm = os.getSynonyms(childTerm);

		if (synonymsForChildTerm != null && synonymsForParentTerm != null)
		{
			if (!parentLabel.isEmpty()) synonymsForParentTerm.add(parentLabel);
			if (!childLabel.isEmpty()) synonymsForChildTerm.add(childLabel);
			for (String synonymForParentString : synonymsForParentTerm)
			{
				for (String synonymForChildString : synonymsForChildTerm)
				{
					if (synonymForChildString.toLowerCase().contains(synonymForParentString.toLowerCase())) return true;
				}
			}
		}
		return false;
	}

	public List<String> uniqueList(List<String> uncleanedList)
	{
		List<String> uniqueList = new ArrayList<String>();

		for (String eachString : uncleanedList)
		{
			if (!uniqueList.contains(eachString.toLowerCase().trim())) uniqueList.add(eachString.toLowerCase().trim());
		}

		return uniqueList;
	}

	public List<String> combineLists(List<String> listOne, List<String> listTwo)
	{
		Set<String> combinedList = new HashSet<String>();
		StringBuilder combinedString = new StringBuilder();

		for (String first : listOne)
		{
			for (String second : listTwo)
			{
				combinedString.delete(0, combinedString.length());
				combinedString.append(first).append(' ').append(second);
				if (!combinedList.contains(combinedString.toString())) combinedList.add(combinedString.toString());
			}
		}
		return new ArrayList<String>(combinedList);
	}
}