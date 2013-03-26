package plugins.ontologyTermInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OntologyTermContainer
{
	private final String ontologyTermID;
	private final String ontologyID;
	private final String label;
	private final List<String> ontologyTermDefinition;
	private Set<OntologyTermContainer> listOfDescendClass;
	private Set<String> synonyms;

	public OntologyTermContainer(String ontologyTermId, List<String> ontologyTermDefinition, String label,
			String ontologyID)
	{
		this.ontologyTermID = ontologyTermId;
		this.ontologyID = ontologyID;
		this.ontologyTermDefinition = ontologyTermDefinition;
		this.label = label;
		this.listOfDescendClass = new HashSet<OntologyTermContainer>();
		this.synonyms = new HashSet<String>();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ontologyTermID == null) ? 0 : ontologyTermID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		OntologyTermContainer other = (OntologyTermContainer) obj;
		if (ontologyTermID == null)
		{
			if (other.ontologyTermID != null) return false;
		}
		else if (!ontologyTermID.equals(other.ontologyTermID)) return false;
		return true;
	}

	public String getOntologyTermID()
	{
		return ontologyTermID;
	}

	public String getOntologyID()
	{
		return ontologyID;
	}

	public String getLabel()
	{
		return label;
	}

	public Set<String> getSynonyms()
	{
		return synonyms;
	}

	public Set<OntologyTermContainer> getListOfDescendClass()
	{
		return listOfDescendClass;
	}

	public List<String> getOntologyTermDefinition()
	{
		return ontologyTermDefinition;
	}
}