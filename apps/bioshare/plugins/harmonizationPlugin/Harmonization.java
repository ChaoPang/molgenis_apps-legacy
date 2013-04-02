/* Date:        December 3, 2010
 * Template:	PluginScreenJavaTemplateGen.java.ftl
 * generator:   org.molgenis.generators.ui.PluginScreenJavaTemplateGen 3.3.3
 * 
 */

package plugins.harmonizationPlugin;

import gcc.catalogue.MappingMeasurement;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jdom.IllegalNameException;
import org.json.JSONException;
import org.json.JSONObject;
import org.molgenis.compute.ComputeProtocol;
import org.molgenis.core.OntologyTerm;
import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.Database.DatabaseAction;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.framework.db.Query;
import org.molgenis.framework.db.QueryRule;
import org.molgenis.framework.db.QueryRule.Operator;
import org.molgenis.framework.server.MolgenisRequest;
import org.molgenis.framework.ui.EasyPluginController;
import org.molgenis.framework.ui.FreemarkerView;
import org.molgenis.framework.ui.ScreenController;
import org.molgenis.framework.ui.ScreenMessage;
import org.molgenis.framework.ui.ScreenView;
import org.molgenis.io.csv.CsvReader;
import org.molgenis.organization.Investigation;
import org.molgenis.pheno.Category;
import org.molgenis.pheno.Measurement;
import org.molgenis.pheno.ObservedValue;
import org.molgenis.protocol.Protocol;
import org.molgenis.util.tuple.Tuple;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;

import plugins.luceneMatching.LuceneMatching;
import plugins.normalMatching.NGramMatchingModel;
import plugins.ontologyTermInfo.OntologyTermContainer;
import plugins.quartzJob.StringMatchingJob;
import plugins.quartzJob.TermExpansionJob;
import uk.ac.ebi.ontocat.bioportal.BioportalOntologyService;

import com.google.gson.Gson;

public class Harmonization extends EasyPluginController<HarmonizationModel>
{
	private static final long serialVersionUID = 4255876428416189905L;

	private static final String PREDICTIONMODEL = "Prediction Model";

	public Harmonization(String name, ScreenController<?> parent)
	{
		super(name, parent);
		this.setModel(new HarmonizationModel(this)); // the default model
	}

	@Override
	public String getCustomHtmlHeaders()
	{
		StringBuilder s = new StringBuilder();
		s.append("<link rel=\"stylesheet\" href=\"bootstrap/css/bootstrap.min.css\" type=\"text/css\" />");
		s.append("<link rel=\"stylesheet\" href=\"bootstrap/css/bootstrap.css\" type=\"text/css\" />");
		s.append("<script type=\"text/javascript\" src=\"bootstrap/js/bootstrap.min.js\"></script>");
		return s.toString();
	}

	@Override
	public Show handleRequest(Database db, MolgenisRequest request, OutputStream out)
	{
		if (out == null)
		{
			this.handleRequest(db, request);
		}
		else
		{
			JSONObject status = new JSONObject();
			Object src = null;
			try
			{
				db.beginTx();
				// Handle request that adds a new prediction model to db
				if ("download_json_newPredictionModel".equals(request.getAction()))
				{
					String nameOfModel = request.getString("name").trim();
					ComputeProtocol cp = new ComputeProtocol();
					cp.setName(nameOfModel);
					cp.setInvestigation_Name(PREDICTIONMODEL);
					cp.setScriptTemplate("Not provided");
					db.add(cp);
					status.put("message",
							"You successfully added a new prediction model</br>Please define the predictors");
					status.put("success", true);

				}
				else if ("download_json_removePredictionModel".equals(request.getAction()))
				{
					String nameOfModel = request.getString("name").trim();
					ComputeProtocol cp = db.find(ComputeProtocol.class,
							new QueryRule(ComputeProtocol.NAME, Operator.EQUALS, nameOfModel)).get(0);
					if (cp.getFeatures_Name().size() > 0) for (Measurement m : db.find(Measurement.class,
							new QueryRule(Measurement.NAME, Operator.IN, cp.getFeatures_Name())))
						this.removePredictor(m.getId(), nameOfModel, db);
					db.remove(cp);
					status.put("message", "You successfully removed a prediction model from the database!");
					status.put("success", true);

				}
				else if ("download_json_addPredictor".equals(request.getAction()))
				{
					JSONObject data = new JSONObject(request.getString("data"));
					Map<String, String> measurementInfo = new HashMap<String, String>();
					Iterator<?> iteratorFields = data.keys();
					while (iteratorFields.hasNext())
					{
						String fieldName = iteratorFields.next().toString();
						measurementInfo.put(fieldName.toLowerCase(), data.getString(fieldName));
					}
					Measurement m = addNewPredictorToDB(db, measurementInfo);
					status.put("message", "You successfully added a new predictor!");
					status.put("identifier", m.getId());
					status.put("success", true);

				}
				else if ("download_json_removePredictors".equals(request.getAction()))
				{
					String predictorName = request.getString("name");
					Integer predictorID = request.getInt("predictorID");
					String predictionModel = request.getString("predictionModel");
					this.removePredictor(predictorID, predictionModel, db);
					status.put("message", "You successfully deleted the predictor: " + predictorName);
					status.put("success", true);
				}
				else if ("download_json_showPredictors".equals(request.getAction()))
				{
					String predictionModel = request.getString("name");
					ComputeProtocol cp = db.find(ComputeProtocol.class,
							new QueryRule(ComputeProtocol.NAME, Operator.EQUALS, predictionModel)).get(0);
					List<JSONFeature> listOfFeatures = new ArrayList<JSONFeature>();

					if (cp.getFeatures_Name().size() > 0)
					{
						for (Measurement eachPredictor : db.find(Measurement.class, new QueryRule(Measurement.NAME,
								Operator.IN, cp.getFeatures_Name())))
						{
							List<JSONCategory> categories = new ArrayList<JSONCategory>();

							if (eachPredictor.getCategories_Name().size() > 0)
							{
								for (Category c : db.find(Category.class, new QueryRule(Category.NAME, Operator.IN,
										eachPredictor.getCategories_Name())))
								{
									JSONCategory jsonCategory = new JSONCategory(c);
									categories.add(jsonCategory);
								}
							}
							Query<ObservedValue> query = db.query(ObservedValue.class);
							query.addRules(new QueryRule(ObservedValue.TARGET_NAME, Operator.EQUALS, eachPredictor
									.getName()));
							// query.addRules(new
							// QueryRule(ObservedValue.FEATURE_NAME,
							// Operator.EQUALS, "BuildingBlocks"));
							String buildingBlocks = null;
							String leadingElement = null;
							if (query.find().size() > 0)
							{
								for (ObservedValue ov : query.find())
								{
									if (ov.getFeature_Name().equals("BuildingBlocks")) buildingBlocks = ov.getValue();
									if (ov.getFeature_Name().equals("LeadingElement")) leadingElement = ov.getValue();
								}
							}

							JSONFeature feature = new JSONFeature(eachPredictor, buildingBlocks, leadingElement,
									categories);
							listOfFeatures.add(feature);
						}

						src = new JSONProtocol(cp, listOfFeatures);
					}
				}
				else if ("download_json_retrieveExpandedQuery".equals(request.getAction()))
				{
					String predictorID = request.getString("predictor");
					String investigationName = request.getString("investigationName");
					Investigation inv = db.find(Investigation.class,
							new QueryRule(Investigation.NAME, Operator.EQUALS, investigationName)).get(0);
					predictorID = predictorID.replaceAll(inv.getId() + "_", "");
					String matchedVariable = request.getString("matchedVariable");
					String identifier = request.getString("identifier").replaceAll(predictorID + "_", "");
					List<Measurement> measurements = db.find(Measurement.class, new QueryRule(Measurement.ID,
							Operator.EQUALS, Integer.parseInt(identifier)));

					String table = retrieveExpandedQuery(
							this.getModel().getPredictors().get(Integer.parseInt(predictorID)),
							(measurements.size() > 0 ? measurements.get(0) : null), matchedVariable);

					status.put("table", table);
				}
				else if ("download_json_monitorJobs".equals(request.getAction()))
				{
					status.put("jobTitle", (this.getModel().isStringMatching() ? "Matching variables"
							: "Expanding terms"));
					status.put("finishedJobs", this.getModel().getFinishedJobs());
					status.put("totalJobs", this.getModel().getTotalJobs());
					status.put("startTime", this.getModel().getStartTime());
					status.put("validationStudy", this.getModel().getSelectedValidationStudy());
					status.put("predictionModel", this.getModel().getSelectedPredictionModel());
					status.put("startTime", this.getModel().getStartTime());
					status.put("currentTime", System.currentTimeMillis());

					if (this.getModel().isStringMatching())
					{
						status.put("finishedQuery", this.getModel().getFinishedNumber());
						status.put("totalQuery", this.getModel().getTotalNumber());

						if (this.getModel().getFinishedNumber() == this.getModel().getTotalNumber()) this.getModel()
								.setScheduler(null);
						else
							System.out.println("Finished: " + this.getModel().getFinishedNumber()
									+ ". Total number is " + this.getModel().getTotalNumber());
					}
					else
					{
						status.put("finishedQuery", this.getModel().getFinishedJobs());
						status.put("totalQuery", this.getModel().getTotalJobs());
					}

					System.out.println("Finished jobs: " + this.getModel().getFinishedJobs() + ". Total number is "
							+ this.getModel().getTotalJobs());

					status.put("success", true);
				}
				else if ("download_json_getOntologyTermInfo".equals(request.getAction()))
				{
					String ontologyTermId = request.getString("ontologyTermId");
					if (this.getModel().getCachedOntologyTerms().containsKey(ontologyTermId))
					{
						OntologyTermContainer container = this.getModel().getCachedOntologyTerms().get(ontologyTermId);
						src = container;
					}
				}
				else if ("download_json_getFeatureInfo".equals(request.getAction()))
				{
					String identifier = request.getString("id");
					String id = identifier.split("_").length > 1 ? identifier.split("_")[1] : StringUtils.EMPTY;
					Measurement m = db.findById(Measurement.class, Integer.parseInt(id));
					JSONFeature jsfeature = null;
					if (m != null)
					{
						List<JSONCategory> categories = null;
						if (m.getCategories_Name().size() > 0)
						{
							categories = new ArrayList<Harmonization.JSONCategory>();
							for (Category c : db.find(Category.class,
									new QueryRule(Category.NAME, Operator.IN, m.getCategories_Name())))
							{
								categories.add(new JSONCategory(c));
							}
						}
						jsfeature = new JSONFeature(m, null, null, categories);
					}
					src = jsfeature;
				}
				else if ("download_json_retrieveResult".equals(request.getAction()))
				{
					String investigationName = request.getString("matchingValidationStudy");

					Investigation inv = db.find(Investigation.class,
							new QueryRule(Investigation.NAME, Operator.EQUALS, investigationName)).get(0);

					for (PredictorInfo predictor : new ArrayList<PredictorInfo>(this.getModel().getPredictors()
							.values()))
					{
						JSONObject eachPredictor = new JSONObject();
						String mappingResult = makeMappingTable(predictor, inv, db);
						StringBuilder tableIdentifier = new StringBuilder();
						eachPredictor.put("table_ID",
								tableIdentifier.append(inv.getId()).append('_').append(predictor.getId()).toString());
						eachPredictor.put("label", predictor.getLabel());
						eachPredictor.put("mappingResult", mappingResult);
						eachPredictor.put("identifier", predictor.getId());
						eachPredictor.put("existingMapping", "");
						status.put(predictor.getId().toString(), eachPredictor);
					}
				}

				db.commitTx();
			}
			catch (Exception e)
			{
				try
				{
					status.put("message", e.getMessage());
					status.put("success", false);
					db.rollbackTx();
					e.printStackTrace();
				}
				catch (Exception exception)
				{
					exception.printStackTrace();
				}
			}

			PrintWriter writer = null;

			try
			{
				writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));

				if (src == null)
				{
					writer.write(status.toString());
					writer.flush();
				}
				else
				{
					new Gson().toJson(src, writer);
				}
			}
			catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (writer != null) writer.close();
			}
		}

		return Show.SHOW_MAIN;
	}

	@Override
	public void reload(Database db)
	{
		try
		{
			// clear the old variable content
			this.getModel().getPredictionModels().clear();
			this.getModel().getValidationStudies().clear();

			if (this.getModel().getReservedInv().size() == 0)
			{
				this.getModel().getReservedInv().add("catalogueCohortStudy");
				this.getModel().getReservedInv().add("cataloguePredictionModel");
				this.getModel().getReservedInv().add(PREDICTIONMODEL);
			}

			for (ComputeProtocol cp : db.find(ComputeProtocol.class, new QueryRule(ComputeProtocol.INVESTIGATION_NAME,
					Operator.EQUALS, PREDICTIONMODEL)))
			{
				this.getModel().getPredictionModels().add(cp.getName());
			}

			for (Investigation inv : db.find(Investigation.class))
			{
				if (!this.getModel().getReservedInv().contains(inv.getName()))
				{
					this.getModel().getValidationStudies().add(inv.getName());
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private String retrieveExpandedQuery(PredictorInfo predictorInfo, Measurement feature, String matchedVariable)
	{
		StringBuilder table = new StringBuilder();

		if (feature != null)
		{
			table.append("<div id=\"expandedQueryTable\" class=\"modal hide fade\">")
					.append("<div class=\"modal-header\">Expanded query for ").append(predictorInfo.getLabel())
					.append("</div>").append("<div class=\"modal-body\">")
					.append("<table class=\"table table-striped\" style=\"width:100%;overflow:auto;\">")
					.append("<tr style=\"font-size:12px;\"><th style=\"width:30%\">Expanded queries</th>")
					.append("<th style=\"width:40%\">Description</th>")
					.append("<th style=\"width:30%\">Similarity score</th></tr>");

			for (String query : predictorInfo.getExpandedQueryForOneMapping(feature.getId()))
			{
				table.append("<tr style=\"font-size:12px;text-align:center;\"><td>").append(query).append("</td><td>")
						.append(feature.getDescription()).append("</td><td>")
						.append(predictorInfo.getSimilarity(feature.getId(), query)).append(" %</td></tr>");
			}
			table.append("</table></div>").append("<div class=\"modal-footer\">")
					.append("<button class=\"btn\" data-dismiss=\"modal\" aria-hidden=\"true\">Close</button><div>");
		}

		return table.toString();
	}

	private String makeMappingTable(PredictorInfo predictor, Investigation inv, Database db) throws DatabaseException
	{
		StringBuilder table = new StringBuilder();

		if (predictor.hasMappingResult())
		{
			Integer predictorID = predictor.getId();
			table.append("<table id=\"")
					.append(inv.getId())
					.append("_")
					.append(predictor.getId())
					.append("\" style=\"display:none;position:relative;top:5px;width:100%;overflow:auto;\"")
					.append(" class=\"table table-striped table-hover\">")
					.append("<tr style=\"font-size:12px;\"><th style=\"width:30%;text-align:center\">Mapped data items")
					.append("</th><th style=\"width:50%;text-align:center\">Description</th>")
					.append("<th style=\"width:20%;text-align:center\">Similarity score</th></tr>");
			List<Measurement> mappedMeasurements = db.find(Measurement.class, new QueryRule(Measurement.ID,
					Operator.IN, predictor.getMappedVariableIDs()));
			Map<Integer, String> sortingMap = new HashMap<Integer, String>();
			for (Measurement measurement : mappedMeasurements)
			{
				if (measurement.getInvestigation_Name().equals(inv.getName()))
				{
					StringBuilder eachRow = new StringBuilder();
					String description = measurement.getDescription();
					String topQuery = null;
					double similarityScore = 0;
					if (predictor.getExpandedQueryForOneMapping(measurement.getId()).size() > 0)
					{
						topQuery = predictor.getExpandedQueryForOneMapping(measurement.getId()).get(0);
						similarityScore = predictor.getSimilarity(measurement.getId(), topQuery);
					}
					StringBuilder identifier = new StringBuilder();
					identifier.append(predictorID).append("_").append(measurement.getId());
					eachRow.append("<tr id=\"" + identifier.toString().replaceAll(" ", "_"))
							.append("_row\" style=\"font-size:12px;\"><td style=\"text-align:center;\">")
							.append("<span style=\"cursor:pointer;\">").append(measurement.getName())
							.append("</span><div id=\"").append(identifier.toString().replaceAll(" ", "_"))
							.append("_details\" style=\"cursor:pointer;float:right;margin-top:3px;\" ")
							.append("class=\"icon-plus\" title=\"Check expanded queries\">")
							.append("<span class=\"ui-icon-plus\"></span></div></td><td style=\"text-align:center;\">")
							.append(description).append("</td><td style=\"text-align:center;\">")
							.append(similarityScore).append(" %</td></tr>");
					sortingMap.put(measurement.getId(), eachRow.toString());
				}
			}
			for (Integer featureID : predictor.getMappedVariableIDs())
			{
				table.append(sortingMap.containsKey(featureID) ? sortingMap.get(featureID) : StringUtils.EMPTY);
			}
			table.append("</table>");
		}
		return table.toString();
	}

	private String makeExistingMappingTable(PredictorInfo predictor) throws JSONException
	{
		StringBuilder table = new StringBuilder();

		if (predictor.getFinalMappings().size() > 0)
		{
			table.append("<table id=\"matched_").append(predictor.getName().replaceAll(" ", "_"))
					.append("\" style=\"display:none;position:relative;top:5px;width:100%;overflow:auto;\"")
					.append(" class=\"ui-widget-content ui-corner-all\">")
					.append("<tr class=\"ui-widget-header ui-corner-all\"><th>Mapped varaibles")
					.append("</th><th>Description</th><th>Remove the mapping</th></tr>");

			String predictorName = predictor.getName();

			for (Measurement measurement : predictor.getFinalMappings().values())
			{
				String measurementName = measurement.getName();

				String description = measurement.getDescription();

				StringBuilder identifier = new StringBuilder();

				identifier.append(predictorName).append("_").append(measurementName);

				table.append("<tr id=\"").append(identifier.toString().replaceAll("[^(a-zA-Z0-9_\\s)]", "_"))
						.append("_matchedRow\"><td style=\"text-align:center;cursor:pointer;\"><span>")
						.append(measurementName).append("</span></td><td style=\"text-align:center;\">")
						.append(description).append("</td><td style=\"text-align:center;\"><div id=\"")
						.append(identifier.toString().replaceAll("[^(a-zA-Z0-9_\\s)]", "_"))
						.append("_remove\" style=\"cursor:pointer;height:18px;width:18px;margin-left:30px;\" ")
						.append("class=\"ui-state-default ui-corner-all\" title=\"remove\">")
						.append("<span class=\"ui-icon ui-icon-trash\"></span></div></td></tr>");
			}

			table.append("</table>");
		}
		return table.toString();
	}

	private void initializeMatchingState()
	{
		this.getModel().setPredictors(new HashMap<Integer, PredictorInfo>());
		this.getModel().setIsStringMatching(false);
		this.getModel().setTotalJobs(this.getModel().getPredictors().size());
		this.getModel().setTotalNumber(0);
		this.getModel().setInitialFinishedJob(0);
		this.getModel().setInitialFinishedQueries(0);
		this.getModel().setStartTime(System.currentTimeMillis());
		this.getModel().setCachedOntologyTerms(new HashMap<String, OntologyTermContainer>());
	}

	private void stringMatchingByLucene(MolgenisRequest request, Database db) throws Exception
	{
		if (this.getModel().getValidationStudies().size() > 0)
		{
			initializeMatchingState();
			this.collectPredictorInfo(db, request);
			LuceneMatching luceneMatchingModel = new LuceneMatching(this.getModel());
			luceneMatchingModel.getMatchingResult();
		}
	}

	private void stringMatching(MolgenisRequest request, Database db) throws Exception
	{
		if (this.getModel().getValidationStudies().size() > 0)
		{
			initializeMatchingState();
			this.getModel().setOs(new BioportalOntologyService());
			this.getModel().setMatchingModel(new NGramMatchingModel(3));
			this.collectPredictorInfo(db, request);
			this.createNGramMeasurements(db);
			Properties prop = new Properties();
			prop.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
			prop.setProperty("org.quartz.threadPool.threadCount", "4");
			this.getModel().setScheduler(new StdSchedulerFactory(prop).getScheduler());
			this.getModel().getScheduler().start();
		}

		LinkedHashMap<JobDetail, SimpleTrigger> listOfJobs = new LinkedHashMap<JobDetail, SimpleTrigger>();
		List<PredictorInfo> predictors = new ArrayList<PredictorInfo>(this.getModel().getPredictors().values());
		int count = 0;
		for (PredictorInfo predictor : predictors)
		{
			StringBuilder jobName = new StringBuilder();
			@SuppressWarnings("static-access")
			JobDetail job = new JobDetail(jobName.append("job_").append(count).toString(), this.getModel()
					.getScheduler().DEFAULT_GROUP, StringMatchingJob.class);
			SimpleTrigger trigger = new SimpleTrigger(jobName.append("_trigger").toString(), Scheduler.DEFAULT_GROUP,
					new Date(), null, 0, 0);
			job.getJobDataMap().put("predictor", predictor);
			job.getJobDataMap().put("model", this.getModel());
			job.getJobDataMap().put("matchingModel", this.getModel().getMatchingModel());
			listOfJobs.put(job, trigger);
			count++;
		}

		JobDetail termExpansion = new JobDetail("term_expansion_job", Scheduler.DEFAULT_GROUP, TermExpansionJob.class);
		termExpansion.getJobDataMap().put("predictors", predictors);
		termExpansion.getJobDataMap().put("jobs", listOfJobs);
		termExpansion.getJobDataMap().put("model", this.getModel());
		SimpleTrigger triggerTermExpasion = new SimpleTrigger("term_expansion_trigger", Scheduler.DEFAULT_GROUP,
				new Date(), null, 0, 0);
		TermExpansionListener listener = new TermExpansionListener("termExpansion");
		this.getModel().getScheduler().addJobListener(listener);
		termExpansion.addJobListener(listener.getName());
		this.getModel().getScheduler().scheduleJob(termExpansion, triggerTermExpasion);
	}

	private void clearAllVariables()
	{
		this.getModel().setOs(null);
		this.getModel().setPredictors(null);
		this.getModel().setMatchingModel(null);
		this.getModel().setCachedOntologyTerms(null);
		this.getModel().setNGramsMapForMeasurements(null);
		this.getModel().setIsStringMatching(false);
		this.getModel().setTotalJobs(0);
		this.getModel().setTotalNumber(0);
		this.getModel().setInitialFinishedJob(0);
		this.getModel().setInitialFinishedQueries(0);
		this.getModel().setStartTime(0);
	}

	private void collectPredictorInfo(Database db, MolgenisRequest request) throws DatabaseException
	{
		String predictionModel = request.getString("selectPredictionModel");
		String selectedVariableName = request.getString("selectedVariableID");
		this.getModel().getPredictors().clear();
		this.getModel().setSelectedPredictionModel(predictionModel);
		ComputeProtocol cp = db.find(ComputeProtocol.class,
				new QueryRule(ComputeProtocol.NAME, Operator.EQUALS, predictionModel)).get(0);
		List<String> listOfFeatureNames = cp.getFeatures_Name();
		if (listOfFeatureNames.size() > 0)
		{
			List<Measurement> listOfMeasurements = null;
			if (selectedVariableName != null)
			{
				listOfFeatureNames.clear();
				listOfFeatureNames.add(selectedVariableName);
			}
			listOfMeasurements = db.find(Measurement.class, new QueryRule(Measurement.NAME, Operator.IN,
					listOfFeatureNames));
			for (Measurement m : listOfMeasurements)
			{
				PredictorInfo predictor = new PredictorInfo();
				predictor.setName(m.getName());
				predictor.setId(m.getId());
				predictor.setLabel(m.getLabel());
				predictor.setIdentifier(m.getName().replaceAll(" ", "_"));
				this.getModel().getPredictors().put(m.getId(), predictor);
				Query<ObservedValue> query = db.query(ObservedValue.class);
				query.addRules(new QueryRule(ObservedValue.TARGET_NAME, Operator.EQUALS, m.getName()));
				// query.addRules(new QueryRule(ObservedValue.FEATURE_NAME,
				// Operator.EQUALS, "BuildingBlocks"));
				for (ObservedValue ov : query.find())
				{
					if (ov.getFeature_Name().equalsIgnoreCase("BuildingBlocks")) this.getModel().getPredictors()
							.get(m.getId()).setBuildingBlocks(ov.getValue().split(";"));
					if (ov.getFeature_Name().equalsIgnoreCase("LeadingElement")) this.getModel().getPredictors()
							.get(m.getId()).setLeadingElement(ov.getValue());
				}
				if (query.find().size() > 0) this.getModel().getPredictors().get(m.getId())
						.setBuildingBlocks(query.find().get(0).getValue().split(";"));
			}
		}
	}

	private Measurement addNewPredictorToDB(Database db, Map<String, String> data) throws DatabaseException
	{
		StringBuilder stringBuilder = new StringBuilder();
		String predictionModelName = data.get("selected").trim();
		String leadingElementString = data.get("leadingElement".toLowerCase()).trim();
		String buildingBlockString = data.get("buildingBlocks".toLowerCase()).trim();
		String unitName = data.get(Measurement.UNIT_NAME.toLowerCase()).trim();
		String categories = data.get(Measurement.CATEGORIES_NAME.toLowerCase()).trim();
		String dataType = data.get(Measurement.DATATYPE.toLowerCase()).trim();

		Measurement m = new Measurement();
		m.setName(stringBuilder.append(data.get(Measurement.NAME.toLowerCase()).trim()).toString());
		m.setLabel(data.get(Measurement.LABEL.toLowerCase()).trim());
		m.setDescription(data.get(Measurement.DESCRIPTION.toLowerCase()));
		if (m.getDataTypeOptions().contains(dataType)) m.setDataType(dataType);
		m.setUnit_Name(unitName);
		m.setInvestigation_Name(PREDICTIONMODEL);

		// Handle unit since it has to be an ontology term
		if (!unitName.isEmpty()
				&& db.find(OntologyTerm.class, new QueryRule(OntologyTerm.NAME, Operator.EQUALS, unitName)).size() == 0)
		{
			OntologyTerm ot = new OntologyTerm();
			ot.setName(unitName);
			db.add(ot);
		}

		List<String> categoryRefs = new ArrayList<String>();
		// Handle the categories
		if (!categories.isEmpty())
		{
			String categoryElements[] = categories.split(",");
			List<Category> newCategories = new ArrayList<Category>();
			for (String eachCategory : removeDuplicate(categoryElements))
			{
				String uniqueName = eachCategory.replaceAll("[^(a-zA-Z0-9_\\s)]", " ").trim();
				String codeAndString[] = eachCategory.split("=");
				Category c = new Category();
				stringBuilder = new StringBuilder();
				c.setName(stringBuilder.append(uniqueName).append("_").append(predictionModelName).toString());
				c.setCode_String(codeAndString[0].trim());
				c.setDescription(codeAndString[1].trim());
				c.setInvestigation_Name(PREDICTIONMODEL);
				newCategories.add(c);
				categoryRefs.add(c.getName());
			}
			db.update(newCategories, DatabaseAction.ADD_IGNORE_EXISTING, Category.NAME, Category.INVESTIGATION_NAME);
		}
		m.setCategories_Name(categoryRefs);
		db.add(m);
		ComputeProtocol cp = db.find(ComputeProtocol.class,
				new QueryRule(ComputeProtocol.NAME, Operator.EQUALS, predictionModelName)).get(0);

		List<Integer> listOfPredictors = cp.getFeatures_Id();
		listOfPredictors.add(m.getId());
		cp.setFeatures_Id(listOfPredictors);
		db.update(cp);

		// Deal with building blocks
		if (db.find(Measurement.class, new QueryRule(Measurement.NAME, Operator.EQUALS, "BuildingBlocks")).size() == 0)
		{
			Measurement buildingBlock = new Measurement();
			buildingBlock.setName("BuildingBlocks");
			buildingBlock.setLabel("Building blocks");
			buildingBlock.setInvestigation_Name(PREDICTIONMODEL);
			db.add(buildingBlock);
		}

		if (db.find(Measurement.class, new QueryRule(Measurement.NAME, Operator.EQUALS, "LeadingElement")).size() == 0)
		{
			Measurement leadingElement = new Measurement();
			leadingElement.setName("LeadingElement");
			leadingElement.setLabel("Leading element");
			leadingElement.setInvestigation_Name(PREDICTIONMODEL);
			db.add(leadingElement);
		}

		if (buildingBlockString != null && !buildingBlockString.isEmpty())
		{
			ObservedValue ov = new ObservedValue();
			ov.setTarget_Name(m.getName());
			ov.setFeature_Name("BuildingBlocks");
			ov.setValue(buildingBlockString);
			ov.setInvestigation_Name(PREDICTIONMODEL);
			db.add(ov);
		}

		if (leadingElementString != null && !leadingElementString.isEmpty())
		{
			ObservedValue ov = new ObservedValue();
			ov.setTarget_Name(m.getName());
			ov.setFeature_Name("LeadingElement");
			ov.setValue(leadingElementString);
			ov.setInvestigation_Name(PREDICTIONMODEL);
			db.add(ov);
		}

		return m;
	}

	@Override
	public void handleRequest(Database db, MolgenisRequest request)
	{
		try
		{
			if ("normalMapping".equals(request.getAction()))
			{
				JSONObject selectedStudiesInJson = new JSONObject(request.getString("selectedStudiesToMatch"));
				Iterator<?> iterator = selectedStudiesInJson.keys();
				List<String> selectedStudiesInList = new ArrayList<String>();
				while (iterator.hasNext())
				{
					String studyName = iterator.next().toString();
					selectedStudiesInList.add(studyName);
				}
				this.getModel().setSelectedValidationStudy(selectedStudiesInList);
				stringMatching(request, db);
				this.getModel().setFreeMakerTemplate("HarmonizationStatus.ftl");
			}
			else if ("luceneMapping".equals(request.getAction()))
			{
				JSONObject selectedStudiesInJson = new JSONObject(request.getString("selectedStudiesToMatch"));
				Iterator<?> iterator = selectedStudiesInJson.keys();
				List<String> selectedStudiesInList = new ArrayList<String>();
				while (iterator.hasNext())
				{
					String studyName = iterator.next().toString();
					selectedStudiesInList.add(studyName);
				}
				this.getModel().setSelectedValidationStudy(selectedStudiesInList);
				stringMatchingByLucene(request, db);
				this.getModel().setRetrieveResult(true);
			}
			else if ("batchUploadPredictors".equals(request.getAction()))
			{
				batchUploadPredictors(request, db);
			}
			else if ("startNewSession".equals(request.getAction()))
			{
				clearAllVariables();
				this.getModel().setFreeMakerTemplate("Harmonization.ftl");
				this.getModel().setRetrieveResult(false);
			}
			else if ("retrieveResult".equals(request.getAction()))
			{
				this.getModel().setFreeMakerTemplate("Harmonization.ftl");
				this.getModel().setRetrieveResult(true);
			}
		}
		catch (Exception e)
		{
			this.setMessages(new ScreenMessage(e.getMessage(), false));

			e.printStackTrace();
		}
	}

	private void batchUploadPredictors(MolgenisRequest request, Database db) throws IOException
	{
		String selectedPredictionModel = request.getString("selectPredictionModel");
		String batchUploadFilePath = request.getString("batchUploadPredictors");
		this.getModel().setSelectedPredictionModel(selectedPredictionModel);
		System.out.println("The selected prediction model is : " + selectedPredictionModel);
		System.out.println("The uploaded file is called : " + batchUploadFilePath);
		final String[] colNames =
		{ "Name", "Label", "Description", "Definition" };
		final List<String> listOfColNames = Arrays.asList(colNames);
		CsvReader reader = null;
		try
		{
			reader = new CsvReader(new File(batchUploadFilePath), ',');
			Iterator<String> colNamesIterator = reader.colNamesIterator();
			int counter = 0;
			while (colNamesIterator.hasNext())
			{
				String colName = colNamesIterator.next();
				if (!listOfColNames.contains(colName))
				{
					this.setMessages(new ScreenMessage("The column name " + colName + " is illegal", false));
					throw new IllegalNameException("The column name " + colName + " is illegal");
				}
				else
					System.out.println("The column is " + colName);
				counter++;
			}

			if (counter != 4)
			{
				this.setMessages(new ScreenMessage("There are columns missing, please include all the columns "
						+ listOfColNames.toArray(), false));
				throw new IllegalNameException("There are columns missing, please include all the columns "
						+ listOfColNames.toArray());
			}

			Iterator<Tuple> listOfRows = reader.iterator();
			Map<String, String> measurementInfo = new HashMap<String, String>();
			while (listOfRows.hasNext())
			{
				Tuple eachRow = listOfRows.next();
				for (String eachColumn : eachRow.getColNames())
				{
					String value = null;
					if (!eachRow.isNull(eachColumn)) value = eachRow.getString(eachColumn).toString();
					else
						value = StringUtils.EMPTY;

					if (eachColumn.equalsIgnoreCase(Measurement.NAME))
					{
						StringBuilder builder = new StringBuilder();
						builder.append(value.replaceAll("[^(a-zA-Z0-9_)]", " ").trim()).append('_')
								.append(selectedPredictionModel);
						value = builder.toString();
					}
					measurementInfo.put(eachColumn.toLowerCase(), value);
				}

				for (Field field : Measurement.class.getFields())
				{
					String eachColumn = field.getName().toLowerCase();
					String value = null;
					if (!eachRow.isNull(eachColumn)) value = eachRow.getString(eachColumn).toString();
					else
						value = StringUtils.EMPTY;
					if (!measurementInfo.containsKey(eachColumn)) measurementInfo.put(eachColumn, value);
				}

				measurementInfo.put("buildingblocks",
						eachRow.isNull("Definition") ? StringUtils.EMPTY : eachRow.get("Definition").toString());
				measurementInfo.put("selected", selectedPredictionModel);
				addNewPredictorToDB(db, measurementInfo);
			}
		}
		catch (Exception e)
		{
			this.setMessages(new ScreenMessage(e.getMessage(), false));
			e.printStackTrace();
		}
		finally
		{
			if (reader != null) reader.close();
		}
	}

	private void removePredictor(Integer predictorID, String predictionModel, Database db) throws DatabaseException
	{
		Measurement m = db.find(Measurement.class, new QueryRule(Measurement.ID, Operator.EQUALS, predictorID)).get(0);
		ComputeProtocol cp = db.find(ComputeProtocol.class,
				new QueryRule(ComputeProtocol.NAME, Operator.EQUALS, predictionModel)).get(0);
		cp.getFeatures_Id().remove(m.getId());
		cp.getFeatures_Name().remove(m.getName());
		db.update(cp);
		List<MappingMeasurement> listOfMappings = db.find(MappingMeasurement.class, new QueryRule(
				MappingMeasurement.MAPPING_NAME, Operator.EQUALS, m.getName()));

		if (listOfMappings.size() > 0)
		{
			for (MappingMeasurement mapping : listOfMappings)
			{
				String validationStudy = mapping.getInvestigation_Name();
				String derivedVariableName = mapping.getTarget_Name();
				db.remove(mapping);
				StringBuilder modelAndStudy = new StringBuilder();
				modelAndStudy.append(predictionModel).append("_").append(validationStudy);
				Protocol p = db.find(Protocol.class,
						new QueryRule(Protocol.NAME, Operator.EQUALS, modelAndStudy.toString())).get(0);
				Measurement derivedVariable = db.find(Measurement.class,
						new QueryRule(Measurement.NAME, Operator.EQUALS, derivedVariableName)).get(0);
				p.getFeatures_Name().remove(derivedVariable.getName());
				p.getFeatures_Id().remove(derivedVariable.getId());
				if (p.getFeatures_Id().size() == 0) db.remove(p);
				else
					db.update(p);
				db.remove(derivedVariable);
			}
		}

		Query<ObservedValue> query = db.query(ObservedValue.class);
		query.addRules(new QueryRule(ObservedValue.TARGET_NAME, Operator.EQUALS, m.getName()));
		// query.addRules(new QueryRule(ObservedValue.FEATURE_NAME,
		// Operator.EQUALS, "BuildingBlocks"));

		if (query.find().size() > 0) db.remove(query.find());

		String unit = m.getUnit_Name();
		List<String> categories = m.getCategories_Name();
		db.remove(m);
		// Check if unit is used by other measurements
		if (unit != null && !unit.equals(""))
		{
			if (db.find(Measurement.class, new QueryRule(Measurement.UNIT_NAME, Operator.EQUALS, unit)).size() == 0)
			{
				OntologyTerm ot = db.find(OntologyTerm.class, new QueryRule(OntologyTerm.NAME, Operator.EQUALS, unit))
						.get(0);
				db.remove(ot);
			}
		}
		// Check if categories are used by other measurements
		if (categories.size() > 0)
		{
			for (String category : categories)
			{
				if (db.find(Measurement.class, new QueryRule(Measurement.CATEGORIES_NAME, Operator.EQUALS, category))
						.size() == 0)
				{
					Category c = db.find(Category.class, new QueryRule(Category.NAME, Operator.EQUALS, category))
							.get(0);
					db.remove(c);
				}
			}
		}
	}

	private void createNGramMeasurements(Database db) throws DatabaseException
	{
		Map<String, Map<Integer, List<Set<String>>>> nGramsMapForMeasurements = new HashMap<String, Map<Integer, List<Set<String>>>>();

		for (Measurement m : db.find(Measurement.class, new QueryRule(Measurement.INVESTIGATION_NAME, Operator.IN, this
				.getModel().getSelectedValidationStudy())))
		{
			String invesigationName = m.getInvestigation_Name();
			List<String> fields = new ArrayList<String>();
			if (!StringUtils.isEmpty(m.getDescription()))
			{
				fields.add(m.getDescription());
				StringBuilder combinedString = new StringBuilder();
				if (!m.getCategories_Name().isEmpty())
				{
					for (Category c : findCategoriesByName(m.getCategories_Name(), db))
					{
						fields.add(c.getDescription() == null ? StringUtils.EMPTY : c.getDescription());
						combinedString.delete(0, combinedString.length());
						combinedString.append(c.getDescription()).append(' ').append(m.getDescription());
						fields.add(combinedString.toString());
					}
					// for (String categoryName : m.getCategories_Name())
					// {
					// combinedString.delete(0, combinedString.length());
					// combinedString.append(categoryName.replaceAll(m.getInvestigation_Name(),
					// "")).append(' ')
					// .append(m.getDescription());
					// fields.add(combinedString.toString().replace('_', ' '));
					// }
				}
			}
			List<Set<String>> listOfNGrams = new ArrayList<Set<String>>();
			for (String eachEntry : fields)
			{
				Set<String> dataItemTokens = this.getModel().getMatchingModel()
						.createNGrams(eachEntry.toLowerCase().trim().replaceAll("[^a-zA-Z ]", ""), true);
				listOfNGrams.add(dataItemTokens);
			}
			Map<Integer, List<Set<String>>> nGramsMap = null;
			if (nGramsMapForMeasurements.containsKey(invesigationName))
			{
				nGramsMap = nGramsMapForMeasurements.get(invesigationName);
			}
			else
			{
				nGramsMap = new HashMap<Integer, List<Set<String>>>();
			}
			nGramsMap.put(m.getId(), listOfNGrams);
			nGramsMapForMeasurements.put(invesigationName, nGramsMap);
		}
		this.getModel().setNGramsMapForMeasurements(nGramsMapForMeasurements);
	}

	private List<Category> findCategoriesByName(List<String> categories_Name, Database db) throws DatabaseException
	{
		List<Category> listOfCategories = db.find(Category.class, new QueryRule(Category.NAME, Operator.IN,
				categories_Name));
		if (listOfCategories == null) listOfCategories = new ArrayList<Category>();
		return listOfCategories;
	}

	private List<String> removeDuplicate(String... elements)
	{
		List<String> uniqueList = new ArrayList<String>();

		for (String eachElement : elements)
		{

			if (!uniqueList.contains(eachElement.trim()))
			{
				uniqueList.add(eachElement.trim());
			}
		}

		return uniqueList;
	}

	@Override
	public ScreenView getView()
	{
		FreemarkerView freeMarkerView = new FreemarkerView(this.getModel().getFreeMakerTemplate(), this.getModel());
		return freeMarkerView;
	}

	private static class JSONProtocol
	{
		private final String name;
		private final String formula;
		private final List<JSONFeature> predictorObjects;

		public JSONProtocol(ComputeProtocol p, List<JSONFeature> features)
		{
			this.name = p.getName();
			this.formula = p.getScriptTemplate();
			this.predictorObjects = features;
		}
	}

	private static class JSONCategory
	{
		private final String name;
		private final String codeString;
		private final String label;
		private final String description;

		public JSONCategory(Category c)
		{
			this.name = c.getName();
			this.codeString = c.getCode_String();
			this.label = c.getLabel();
			this.description = c.getDescription();
		}
	}

	private static class JSONFeature
	{
		private final Integer id;
		private final String name;
		private final String label;
		private final String description;
		private final String dataType;
		private final String unit;
		private final Integer identifier;
		private final String buildingBlocks;
		private final String leadingElement;
		private final List<JSONCategory> categories;

		public JSONFeature(Measurement m, String buildingBlocks, String leadingElement, List<JSONCategory> categories)
		{
			this.id = m.getId();
			this.name = m.getName();
			this.label = m.getLabel();
			this.description = m.getDescription();
			this.identifier = m.getId();
			this.dataType = m.getDataType();
			this.unit = m.getUnit_Name();
			this.categories = categories;
			this.buildingBlocks = buildingBlocks;
			this.leadingElement = leadingElement;
		}
	}

	public class StringMatchingListener implements JobListener
	{
		private String name;

		public StringMatchingListener(String name)
		{
			this.name = name;
		}

		public String getName()
		{
			return name;
		}

		public void jobToBeExecuted(JobExecutionContext arg0)
		{
		}

		public void jobWasExecuted(JobExecutionContext context, JobExecutionException exception)
		{
			try
			{
				HarmonizationModel model = (HarmonizationModel) context.getJobDetail().getJobDataMap().get("model");
				if (model.getScheduler() != null)
				{
					model.getScheduler().deleteJob(context.getJobDetail().getName(), context.getJobDetail().getGroup());
					model.getScheduler().removeJobListener(this.getName());
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		@Override
		public void jobExecutionVetoed(JobExecutionContext arg0)
		{
		}
	}

	public class TermExpansionListener implements JobListener
	{
		private String name;

		public TermExpansionListener(String name)
		{
			this.name = name;
		}

		public String getName()
		{
			return name;
		}

		public void jobToBeExecuted(JobExecutionContext arg0)
		{
		}

		@SuppressWarnings("unchecked")
		public void jobWasExecuted(JobExecutionContext context, JobExecutionException exception)
		{
			try
			{
				LinkedHashMap<JobDetail, SimpleTrigger> listOfJobs = (LinkedHashMap<JobDetail, SimpleTrigger>) context
						.getJobDetail().getJobDataMap().get("jobs");

				HarmonizationModel model = (HarmonizationModel) context.getJobDetail().getJobDataMap().get("model");

				model.setIsStringMatching(true);

				model.setInitialFinishedJob(0);

				for (JobDetail eachJob : listOfJobs.keySet())
				{
					StringMatchingListener listener = new StringMatchingListener(eachJob.getName() + "_listener");

					model.getScheduler().addJobListener(listener);

					eachJob.addJobListener(listener.getName());

					model.getScheduler().scheduleJob(eachJob, listOfJobs.get(eachJob));
				}

				model.getScheduler().deleteJob(context.getJobDetail().getName(), context.getJobDetail().getGroup());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		@Override
		public void jobExecutionVetoed(JobExecutionContext arg0)
		{
		}
	}
}