/* Date:        March 11, 2010
 * Template:	PluginScreenJavaTemplateGen.java.ftl
 * generator:   org.molgenis.generators.ui.PluginScreenJavaTemplateGen 3.3.2-testing
 * 
 * THIS FILE IS A TEMPLATE. PLEASE EDIT :-)
 */

package org.molgenis.animaldb.plugins.settings;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.molgenis.animaldb.commonservice.CommonService;
import org.molgenis.framework.db.Database;
import org.molgenis.framework.ui.PluginModel;
import org.molgenis.framework.ui.ScreenController;
import org.molgenis.pheno.ObservationTarget;
import org.molgenis.util.Entity;
import org.molgenis.util.Tuple;


public class BackgroundPlugin extends PluginModel<Entity>
{
	private static final long serialVersionUID = 6637437260773077373L;
	private List<ObservationTarget> backgroundList;
	private List<ObservationTarget> speciesList;
	private String action = "init";
	private CommonService ct = CommonService.getInstance();
	private Map<Integer, String> speciesMap;
	
	public BackgroundPlugin(String name, ScreenController<?> parent)
	{
		super(name, parent);
	}
	
	public String getCustomHtmlHeaders()
    {
		return "<link rel=\"stylesheet\" style=\"text/css\" href=\"res/css/animaldb.css\">";
    }
	
	public List<ObservationTarget> getBackgroundList() {
		return backgroundList;
	}
	public void setBackgroundList(List<ObservationTarget> backgroundList) {
		this.backgroundList = backgroundList;
	}

	public List<ObservationTarget> getSpeciesList() {
		return speciesList;
	}

	public void setSpeciesList(List<ObservationTarget> speciesList) {
		this.speciesList = speciesList;
	}

	@Override
	public String getViewName()
	{
		return "plugins_settings_BackgroundPlugin";
	}

	@Override
	public String getViewTemplate()
	{
		return "plugins/settings/BackgroundPlugin.ftl";
	}

	public String getAction()
	{
		return action;
	}

	public void setAction(String action)
	{
		this.action = action;
	}
	
	public String getSpecies(int backgroundId) {
		return speciesMap.get(backgroundId);
	}

	@Override
	public void handleRequest(Database db, Tuple request)
	{
		ct.setDatabase(db);
		try {
			action = request.getString("__action");
			
			if (action.equals("Add")) {
				//
			}
			
			if (action.equals("Import")) {
				//
			}
			
			if (action.equals("addBackground")) {
				String bkgName = request.getString("name");
				int speciesId = request.getInt("species");
				int investigationId = ct.getOwnUserInvestigationId(this.getLogin().getUserName());
				int bkgId = ct.makePanel(investigationId, bkgName, this.getLogin().getUserId());
				int protocolId = ct.getProtocolId("SetTypeOfGroup");
				int measurementId = ct.getMeasurementId("TypeOfGroup");
				db.add(ct.createObservedValueWithProtocolApplication(investigationId, new Date(), null, 
						protocolId, measurementId, bkgId, "Background", 0));
				protocolId = ct.getProtocolId("SetSpecies");
				measurementId = ct.getMeasurementId("Species");
				db.add(ct.createObservedValueWithProtocolApplication(investigationId, new Date(), null, 
						protocolId, measurementId, bkgId, null, speciesId));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			if (e.getMessage() != null) {
				this.setError(e.getMessage());
			}
		}
	}

	public void reload(Database db)
	{
		ct.setDatabase(db);
		
		// Populate background and species list/map
		try {
			List<Integer> investigationIds = ct.getAllUserInvestigationIds(this.getLogin().getUserName());
			
			backgroundList = ct.getAllMarkedPanels("Background", investigationIds);
			
			speciesList = ct.getAllMarkedPanels("Species", investigationIds);
			
			speciesMap = new HashMap<Integer, String>();
			for (ObservationTarget background : backgroundList) {
				int backgroundId = background.getId();
				int speciesId = ct.getMostRecentValueAsXref(backgroundId, "Species");
				if (speciesId != -1) {
					String speciesName = ct.getObservationTargetLabel(speciesId);
					speciesMap.put(backgroundId, speciesName);
				}
			}
			
		} catch (Exception e) {
			String message = "Something went wrong while loading backgrounds and species";
			if (e.getMessage() != null) {
				message += (": " + e.getMessage());
			}
			this.setError(message);
			e.printStackTrace();
		}
	}
	
}
