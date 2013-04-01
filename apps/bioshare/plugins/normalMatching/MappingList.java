package plugins.normalMatching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingList
{
	// private Map<ExpandedQueryObject, Double> uniqueElements = new
	// HashMap<ExpandedQueryObject, Double>();
	private Map<Integer, ExpandedQueryObject> uniqueElements = new HashMap<Integer, ExpandedQueryObject>();

	public void add(String expandedQuery, String displayedLabel, Integer featureID, double similarity) throws Exception
	{
		if (expandedQuery == null || featureID == null) throw new Exception("Parameters have to be not null!");
		if (expandedQuery.isEmpty()) throw new Exception("Parameters have to be not empty");

		if (uniqueElements.containsKey(featureID))
		{
			if (similarity > uniqueElements.get(featureID).getSimilarity())
			{
				uniqueElements.get(featureID).setExpandedQuery(expandedQuery);
				uniqueElements.get(featureID).setDisplayedLabel(displayedLabel);
				uniqueElements.get(featureID).setSimilarity(similarity);
			}
		}
		else
		{
			ExpandedQueryObject uniqueMapping = new ExpandedQueryObject(expandedQuery, displayedLabel, featureID);
			uniqueMapping.setSimilarity(similarity);
			uniqueElements.put(featureID, uniqueMapping);
		}
		// if (uniqueElements.containsKey(uniqueMapping))
		// {
		// if (similarity > uniqueElements.get(uniqueMapping))
		// uniqueElements.put(uniqueMapping, similarity);
		// }
		// else
		// uniqueElements.put(uniqueMapping, similarity);

	}

	public void updateScore(Integer featureID, double similarity)
	{
		if (uniqueElements.containsKey(featureID)) uniqueElements.get(featureID).setSimilarity(similarity);
	}

	public List<LinkedInformation> getSortedInformation()
	{
		List<LinkedInformation> sortedLinks = new ArrayList<LinkedInformation>(uniqueElements.size());

		for (Map.Entry<Integer, ExpandedQueryObject> entry : uniqueElements.entrySet())
		{
			sortedLinks.add(new LinkedInformation(entry.getValue().getExpandedQuery(), entry.getValue()
					.getDisplayedLabel(), entry.getKey(), entry.getValue().getSimilarity()));
		}
		// for (Map.Entry<ExpandedQueryObject, Double> entry :
		// uniqueElements.entrySet())
		// {
		// sortedLinks.add(new
		// LinkedInformation(entry.getKey().getExpandedQuery(),
		// entry.getKey().getDisplayedLabel(),
		// entry.getKey().getFeatureID(), entry.getValue()));
		// }
		Collections.sort(sortedLinks);
		return (sortedLinks.size() > 5000 ? sortedLinks.subList(sortedLinks.size() - 5000, sortedLinks.size())
				: sortedLinks);
	}

	private static class ExpandedQueryObject
	{
		private final Integer featureID;
		private String expandedQuery;
		private String displayedLabel;
		private double similarity;

		public ExpandedQueryObject(String expandedQuery, String displayedLabel, Integer featureID)
		{
			this.expandedQuery = expandedQuery;
			this.displayedLabel = displayedLabel;
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
			ExpandedQueryObject other = (ExpandedQueryObject) obj;
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

		public String getExpandedQuery()
		{
			return expandedQuery;
		}

		public String getDisplayedLabel()
		{
			return displayedLabel;
		}

		public Integer getFeatureID()
		{
			return featureID;
		}

		public double getSimilarity()
		{
			return similarity;
		}

		public void setSimilarity(double similarity)
		{
			this.similarity = similarity;
		}

		public void setExpandedQuery(String expandedQuery)
		{
			this.expandedQuery = expandedQuery;
		}

		public void setDisplayedLabel(String displayedLabel)
		{
			this.displayedLabel = displayedLabel;
		}
	}
}