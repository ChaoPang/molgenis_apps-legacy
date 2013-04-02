package plugins.harmonizationPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.molgenis.pheno.Measurement;

import plugins.normalMatching.LinkedInformation;
import plugins.normalMatching.MappingList;

public class PredictorInfo
{
	private Integer id = null;
	private String name = null;
	private String label = null;
	private String identifier = null;
	private String leadingElement = null;
	private List<String> buildingBlocks = new ArrayList<String>();
	private Map<String, String> expandedQuery = new HashMap<String, String>();
	private Map<String, Measurement> finalMappings = new HashMap<String, Measurement>();
	private Map<SimilarityScore, Double> similarity = new LinkedHashMap<SimilarityScore, Double>();
	private Map<Integer, List<String>> expandedQueryPerMapping = new LinkedHashMap<Integer, List<String>>();
	private Map<String, MappingList> mappingsForStudies = new HashMap<String, MappingList>();

	public void setName(String name)
	{
		this.name = name;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public void setIdentifier(String identifier)
	{
		this.identifier = identifier;
	}

	public void setBuildingBlocks(List<String> buildingBlocks)
	{
		this.buildingBlocks = buildingBlocks;
	}

	public void setLeadingElement(String leadingElement)
	{
		this.leadingElement = leadingElement;
	}

	public void setBuildingBlocks(String... buildingBlocks)
	{
		this.buildingBlocks.clear();

		for (String eachBlock : buildingBlocks)
		{
			this.buildingBlocks.add(eachBlock.trim());
		}
	}

	public void setFinalMappings(HashMap<String, Measurement> finalMaapings)
	{
		this.finalMappings = finalMaapings;
	}

	public void addFinalMappings(List<Measurement> finalMappings)
	{
		for (Measurement m : finalMappings)
		{
			if (!this.finalMappings.containsKey(m.getName()))
			{
				this.finalMappings.put(m.getName(), m);
			}
		}
	}

	public void setMappings(Map<String, MappingList> mappingsForStudies)
	{
		this.mappingsForStudies = mappingsForStudies;

		for (Entry<String, MappingList> entry : mappingsForStudies.entrySet())
		{
			MappingList mappings = entry.getValue();
			List<LinkedInformation> allMappings = mappings.getSortedInformation();
			for (int i = allMappings.size(); i > 0; i--)
			{
				LinkedInformation eachRow = allMappings.get(i - 1);
				// String expandedQuery = eachRow.getExpandedQuery();
				String expandedQuery = eachRow.getDisplayedLabel();
				Integer featureID = eachRow.getFeatureID();
				double similarity = eachRow.getSimilarity();
				SimilarityScore similarityScore = new SimilarityScore(featureID, expandedQuery);
				if (!this.similarity.containsKey(similarityScore))
				{
					this.similarity.put(similarityScore, similarity);
				}
				List<String> temp = null;
				if (this.expandedQueryPerMapping.containsKey(featureID))
				{
					temp = this.expandedQueryPerMapping.get(featureID);
				}
				else
				{
					temp = new ArrayList<String>();
				}
				if (!temp.contains(expandedQuery)) temp.add(expandedQuery);
				this.expandedQueryPerMapping.put(featureID, temp);
			}
		}
	}

	public String getName()
	{
		return name;
	}

	public String getLeadingElement()
	{
		return leadingElement;
	}

	public List<String> getBuildingBlocks()
	{
		return buildingBlocks;
	}

	public Map<String, MappingList> getMappings()
	{
		return mappingsForStudies;
	}

	public Map<String, String> getExpandedQuery()
	{
		return expandedQuery;
	}

	public Map<String, Measurement> getFinalMappings()
	{
		return finalMappings;
	}

	public String getLabel()
	{
		return label;
	}

	public boolean hasMappingResult()
	{

		if (expandedQueryPerMapping.keySet().size() > 0) return true;
		else
			return false;

	}

	public List<Integer> getMappedVariableIDs()
	{
		List<Integer> listOfVariableIDs = new ArrayList<Integer>();

		for (Integer featureID : expandedQueryPerMapping.keySet())
		{
			listOfVariableIDs.add(featureID);
		}

		return listOfVariableIDs;
	}

	public List<String> getExpandedQueryForOneMapping(Integer featureID)
	{
		if (expandedQueryPerMapping.containsKey(featureID))
		{
			return expandedQueryPerMapping.get(featureID);
		}
		else
			return null;
	}

	public Double getSimilarity(Integer featureID, String expandedQuery)
	{
		SimilarityScore mea = new SimilarityScore(featureID, expandedQuery);

		if (similarity.containsKey(mea)) return similarity.get(mea);
		else
			return null;
	}

	public String getIdentifier()
	{
		return identifier;
	}

	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	public static class MeasurementPerStudy
	{
		private final String investigationName;
		private final Integer featureID;

		public MeasurementPerStudy(Integer featureID, String investigationName)
		{
			this.investigationName = investigationName;
			this.featureID = featureID;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((featureID == null) ? 0 : featureID.hashCode());
			result = prime * result + ((investigationName == null) ? 0 : investigationName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			MeasurementPerStudy other = (MeasurementPerStudy) obj;
			if (featureID == null)
			{
				if (other.featureID != null) return false;
			}
			else if (!featureID.equals(other.featureID)) return false;
			if (investigationName == null)
			{
				if (other.investigationName != null) return false;
			}
			else if (!investigationName.equals(other.investigationName)) return false;
			return true;
		}
	}

	public static class SimilarityScore
	{
		private final Integer featureID;
		private final String expandedQuery;

		public SimilarityScore(Integer featureID, String expandedQuery)
		{
			this.expandedQuery = expandedQuery;
			this.featureID = featureID;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((expandedQuery == null) ? 0 : expandedQuery.hashCode());
			result = prime * result + ((featureID == null) ? 0 : featureID.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			SimilarityScore other = (SimilarityScore) obj;
			if (expandedQuery == null)
			{
				if (other.expandedQuery != null) return false;
			}
			else if (!expandedQuery.equals(other.expandedQuery)) return false;
			if (featureID == null)
			{
				if (other.featureID != null) return false;
			}
			else if (!featureID.equals(other.featureID)) return false;
			return true;
		}

	}
}