package plugins.harmonization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import plugins.HarmonizationComponent.MappingList;
import plugins.HarmonizationComponent.NGramMatchingModel;

public class StringMatchingJob implements StatefulJob
{
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException
	{
		try
		{
			PredictorInfo predictor = (PredictorInfo) context.getJobDetail().getJobDataMap().get("predictor");
			HarmonizationModel model = (HarmonizationModel) context.getJobDetail().getJobDataMap().get("model");
			NGramMatchingModel matchingModel = (NGramMatchingModel) context.getJobDetail().getJobDataMap()
					.get("matchingModel");
			Map<String, MappingList> mappingsForStudies = new HashMap<String, MappingList>();
			for (Entry<String, Map<Integer, List<Set<String>>>> entry : model.getnGramsMapForMeasurements().entrySet())
			{
				String investigationName = entry.getKey();
				MappingList mappingList = new MappingList();
				for (Entry<String, String> eachQueryEntry : predictor.getExpandedQuery().entrySet())
				{
					executeMapping(matchingModel, eachQueryEntry, mappingList, entry.getValue());
					model.incrementFinishedQueries();
				}
				mappingsForStudies.put(investigationName, mappingList);
			}
			model.incrementFinishedJob();
			predictor.setMappings(mappingsForStudies);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void executeMapping(NGramMatchingModel matchingModel, Entry<String, String> eachQueryEntry,
			MappingList mappingList, Map<Integer, List<Set<String>>> measurementMap) throws Exception
	{
		Set<String> tokens = matchingModel.createNGrams(
				eachQueryEntry.getKey().toLowerCase().trim().replaceAll("[^a-zA-Z 0-9]", ""), true);

		for (Integer featureID : measurementMap.keySet())
		{
			for (Set<String> eachNGrams : measurementMap.get(featureID))
			{
				if (eachNGrams.size() != 0) mappingList.add(eachQueryEntry.getValue(), featureID,
						matchingModel.calculateScore(eachNGrams, tokens));
			}
		}
	}
}