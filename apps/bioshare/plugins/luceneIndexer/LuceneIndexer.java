/* Date:        December 3, 2010
 * Template:	PluginScreenJavaTemplateGen.java.ftl
 * generator:   org.molgenis.generators.ui.PluginScreenJavaTemplateGen 3.3.3
 * 
 */

package plugins.luceneIndexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.framework.db.QueryRule;
import org.molgenis.framework.db.QueryRule.Operator;
import org.molgenis.framework.server.MolgenisRequest;
import org.molgenis.framework.ui.PluginModel;
import org.molgenis.framework.ui.ScreenController;
import org.molgenis.organization.Investigation;
import org.molgenis.pheno.Category;
import org.molgenis.pheno.Measurement;
import org.molgenis.util.Entity;

import plugins.harmonizationPlugin.CreatePotentialTerms;
import plugins.luceneMatching.LuceneMatching;

//import plugins.autohidelogin.AutoHideLoginModel; 

public class LuceneIndexer extends PluginModel<Entity>
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7616951833395875810L;

	public LuceneIndexer(String name, ScreenController<?> parent)
	{
		super(name, parent);
	}

	@Override
	public String getCustomHtmlHeaders()
	{
		StringBuilder s = new StringBuilder();
		s.append("<link rel=\"stylesheet\" href=\"bootstrap/css/bootstrap.min.css\" type=\"text/css\" />");
		s.append("<link rel=\"stylesheet\" href=\"bootstrap/css/bootstrap.css\" type=\"text/css\" />");
		s.append("<script type=\"text/javascript\" src=\"bootstrap/js/bootstrap.min.js\"></script>");
		s.append("<script type=\"text/javascript\" src=\"res/scripts/luceneIndexer.js\"></script>");
		s.append("<link rel=\"stylesheet\" href=\"res/css/bioshare/indexer.css\" type=\"text/css\" />");
		return s.toString();
	}

	@Override
	public String getViewName()
	{
		return "plugins_luceneIndexer_LuceneIndexer";
	}

	@Override
	public String getViewTemplate()
	{
		return "plugins/luceneIndexer/LuceneIndexer.ftl";
	}

	@Override
	public void handleRequest(Database db, MolgenisRequest request) throws Exception
	{
		if ("indexDataItems".equals(request.getAction()))
		{
			System.out.println("Start indexing data items.....");
			File indexDirectory = new File("/Users/chaopang/Desktop/ontologyTermIndex");
			indexBiobankStudies(indexDirectory, db);
			System.out.println("Indexing was finished!");
		}
	}

	@Override
	public void reload(Database db)
	{
	}

	private void indexBiobankStudies(File indexDirectory, Database db) throws DatabaseException, CorruptIndexException,
			LockObtainFailedException, IOException
	{
		List<Investigation> studies = db.find(Investigation.class);
		IndexWriter writer = null;
		try
		{
			boolean createIndex = true;
			if (IndexReader.indexExists(FSDirectory.open(indexDirectory))) createIndex = false;
			{
				IndexReader reader = IndexReader.open(FSDirectory.open(indexDirectory), false);
				TermEnum termsEnum = reader.terms();
				while (termsEnum.next())
				{
					Term term = termsEnum.term();
					if (term.field().equals("type"))
					{
						reader.deleteDocuments(term);
					}
				}
				createIndex = false;
				reader.close();
			}
			writer = new IndexWriter(FSDirectory.open(indexDirectory), new KeywordAnalyzer(), createIndex,
					IndexWriter.MaxFieldLength.UNLIMITED);
			LuceneMatching model = new LuceneMatching(indexDirectory);
			for (Investigation inv : studies)
			{
				for (Measurement m : findFeaturesByInv(inv.getName(), db))
				{
					if (m.getDescription() != null && !m.getDescription().isEmpty())
					{
						Document document = new Document();
						document.add(new Field("type", "dataItem", Field.Store.YES, Field.Index.NOT_ANALYZED));
						document.add(new Field("measurementID", m.getId().toString(), Field.Store.YES,
								Field.Index.NOT_ANALYZED));
						document.add(new Field("measurement", m.getDescription().toLowerCase(), Field.Store.YES,
								Field.Index.ANALYZED));
						document.add(new Field("investigation", m.getInvestigation_Name().toLowerCase(),
								Field.Store.YES, Field.Index.ANALYZED));
						if (inv.getName().equalsIgnoreCase("finRisk")) addExpansionToDocument(model,
								m.getDescription(), "measurementExpansion", document);

						if (m.getCategories_Name().size() > 0)
						{
							for (Category c : findCategoriesByName(m.getCategories_Name(), db))
							{
								document.add(new Field("category", c.getDescription().toLowerCase(), Field.Store.YES,
										Field.Index.ANALYZED));
								StringBuilder combinedDescription = new StringBuilder();
								document.add(new Field("category", combinedDescription
										.append(m.getDescription().toLowerCase()).append(' ')
										.append(c.getDescription().toLowerCase()).toString(), Field.Store.YES,
										Field.Index.ANALYZED));
							}
						}
						writer.addDocument(document);
					}
				}
			}
		}
		finally
		{
			if (writer != null) writer.close();
		}
	}

	private void addExpansionToDocument(LuceneMatching model, String originalText, String fieldName, Document document)
			throws IOException
	{
		List<List<String>> potentialTokens = CreatePotentialTerms.getTermsLists(Arrays.asList(originalText.split(" ")));
		Map<String, String> expandedQueries = model.combineTermByIndex(potentialTokens, false);
		for (String descriptionExpansion : expandedQueries.keySet())
		{
			document.add(new Field(fieldName, descriptionExpansion.toLowerCase(), Field.Store.YES, Field.Index.ANALYZED));
		}
	}

	private List<Category> findCategoriesByName(List<String> categories_Name, Database db) throws DatabaseException
	{
		List<Category> listOfCategories = db.find(Category.class, new QueryRule(Category.NAME, Operator.IN,
				categories_Name));
		listOfCategories = listOfCategories != null ? listOfCategories : new ArrayList<Category>();
		return listOfCategories;
	}

	private List<Measurement> findFeaturesByInv(String investigationName, Database db) throws DatabaseException
	{
		List<Measurement> listOfMeasurements = db.find(Measurement.class, new QueryRule(Measurement.INVESTIGATION_NAME,
				Operator.EQUALS, investigationName));
		listOfMeasurements = (listOfMeasurements != null ? listOfMeasurements : new ArrayList<Measurement>());
		return listOfMeasurements;
	}
}