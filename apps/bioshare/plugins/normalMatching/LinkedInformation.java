package plugins.normalMatching;

public class LinkedInformation implements Comparable<LinkedInformation>
{
	private final Integer featureID;
	private final String expandedQuery;
	private final String displayedLabel;
	private double similarity;

	public LinkedInformation(String expandedQuery, String displayedLabel, Integer featureID, double similarity)
	{

		if (expandedQuery == null || featureID == null) throw new IllegalArgumentException(
				"Parameters have to be not null!");

		if (expandedQuery.isEmpty()) throw new IllegalArgumentException("Parameters have to be not empty");
		this.expandedQuery = expandedQuery;
		this.featureID = featureID;
		this.displayedLabel = displayedLabel;
		this.setSimilarity(similarity);
	}

	@Override
	public int compareTo(LinkedInformation o)
	{
		return Double.compare(this.getSimilarity(), o.getSimilarity());
	}

	public String getExpandedQuery()
	{
		return expandedQuery;
	}

	public String getDisplayedLabel()
	{
		return displayedLabel;
	}

	public Double getSimilarity()
	{
		return similarity;
	}

	public void setSimilarity(Double similarity)
	{
		this.similarity = similarity;
	}

	public Integer getFeatureID()
	{
		return featureID;
	}
}