package plugins.harmonization;

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