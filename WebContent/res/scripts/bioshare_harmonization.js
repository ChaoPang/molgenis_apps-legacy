function monitorJobs(url)
{
	$.ajax({
		url : url + "&__action=download_json_monitorJobs",
		async: false,
	}).done(function(status){
		$('#processedTime').text("");
		$('#estimatedTime').text("");
		percentage = 0;
		processedTime = (status["currentTime"] - status["startTime"])/1000;
		estimatedTimeString = "calculating...";
		if(status["totalQuery"] != 0 && status["finishedQuery"] != 0)
		{
			percentage = status["finishedQuery"] / status["totalQuery"];

			estimatedTime = processedTime / percentage * (1 - percentage);

			estimatedTimeString = (estimatedTime/60).toFixed(0) + " mins " + (estimatedTime%60).toFixed(0) + " secs";
		}
		$('#processedTime').text((processedTime/60).toFixed(0) + " mins " + (processedTime%60).toFixed(0) + " secs");
		$('#estimatedTime').text(estimatedTimeString);
		$('#jobTitle').text(status["jobTitle"]);
		$('#progressBarMessage').text(" " + (percentage * 100).toFixed(2) + "%");
		$('#progressBar').progressbar({value: percentage * 100});
		if(status["totalQuery"] == status["finishedQuery"])
		{
			clearInterval(timer);
			$('#resultPanel').show();
		}			
	});
}

function retrieveResult(url)
{
	$.ajax({
		url : url + "&__action=download_json_retrieveResult&matchingValidationStudy=" + $('#matchingValidationStudy').val(),
		async: false,
	}).done(function(status){

		$('#beforeMapping').hide();
		$('#details').empty();
		$('#mappingResult').empty();
		$('#validatePredictors').empty();
		$('#matchingSelectedPredictor').text("");
		$('#afterMapping').show();
		$('#matchingPredictionModel').text($('#selectPredictionModel').val());
		$.each(status, function(key, Info){
			label = Info["label"];
			table = Info["mappingResult"];
			existingMapping = Info["existingMapping"];
			$('#mappingResult').append(table);
			$('#existingMappings').append(existingMapping);
			$('#validatePredictors').append("<option style=\"cursor:pointer;font-family:Verdana,Arial,sans-serif;\">" + label + "</option>");
			$('#validatePredictors option:last-child').data('table_ID', Info["table_ID"]);
		});
		
		$('#matchingSelectedPredictor').text($('#validatePredictors option:eq(0)').text());
		$('#validatePredictors option').each(function(){
			$(this).hover(
				function(){
					$(this).css({
						"font-weight" : "bolder",
						"color" : "grey"
					});
				},function(){
					$(this).css({
						"font-weight" : "normal",
						"color" : "black"
					});
				}
			);
		});
		
		$('#mappingResult tr td:first-child').each(function(){
			$(this).children('span:eq(0)').tooltip({
				title : 'click to check the variable'
			}).click(function(){
				var identifier = $(this).parents('tr:eq(0)').attr('id');
				getFeatureInfo(identifier, url);
			});
			var identifier = $(this).parent().attr('id').replace("_row", "_details");			
			$('#' + identifier).click(function(){
				var predictor = $(this).parents('table').eq(0).attr('id').replace("mapping_","");
				retrieveExpandedQueries(predictor, $(this), url);
			});	
		});

		$('#validatePredictors').change(function(){
			elementOption = $(this).find('option:selected');
			$('#matchingSelectedPredictor').text(elementOption.text());
			$('#mappingResult table').hide();
			$('#existingMappings table').hide();
			tableID = elementOption.data('table_ID');
			$('#' + tableID).show();
		});

		$('#existingMappings tr td:last-child >div').each(function(){
			$(this).click(function(){
				removeSingleMapping($(this), url);
			});
			span = $(this).parents('tr').eq(0).find('td >span');
			measurementName = $(span).text();
			$(span).click(function(){
				trackInTree($(this).text(), url);
			});
		});
		$('#validatePredictors option:first-child').attr('selected', true).click();
		$('#mappingResult table:first-child').show();
	});
}

function showExistingMapping(url)
{	
	if($('#listOfCohortStudies option').length == 0){

		showMessage("There are no cohort studies to validate the prediction model", false);

	}else if($('#selectPredictionModel option').length == 0){

		showMessage("There are no prediction models", false);

	}else{
		
		predictionModel = $('#selectPredictionModel').val();
	
		validationStudy = $('#listOfCohortStudies').val();
		
		$.ajax({
			
			url : url + "&__action=download_json_existingMapping&selectPredictionModel=" 
				+ predictionModel + "&listOfCohortStudies=" + validationStudy,
			async: false,
			
		}).done(function(status){
			
			retrieveResult(url);
		});
	}
}

function validateStudy(formName, whetherLucene)
{
	if($('#listOfCohortStudies').siblings('table').find('tr:gt(0)').length == 0){

		showMessage("There are no cohort studies to validate the prediction model", false);

	}else if($('#selectPredictionModel option').length == 0){

		showMessage("There are no prediction models", false);

	}else{

		$('#details').empty();		
		selectedStudies = {};
		$('#listOfCohortStudies').siblings('table').find('tr:gt(0)').each(function(){
			
			selectedStudies[$(this).children('td:eq(0)').text()] = $(this).children('td:eq(0)').text();
		});
		$('input[name=\"selectedStudiesToMatch\"]').val(JSON.stringify(selectedStudies));
		$('#existingMappings').empty();
		$('#mappingResult').empty();
		$('#validatePredictors').empty();
		$('#matchingSelectedPredictor').text("");
		if(whetherLucene == true){
			$("input[name=\"__action\"]").val("luceneMapping");
		}else{
			$("input[name=\"__action\"]").val("normalMapping");
		}
		$("form[name=\"" + formName + "\"]").submit();
	}
}

function addMappingFromTree(url)
{	
	mappedVariableId = $('#clickedVariable').val();

	measurementName = new Array();

	measurementName[0] = $('#' + mappedVariableId).find('span').text();

	predictor = $('#validatePredictors option:selected').attr('id');

	mappings = {};

	mappings[predictor] = measurementName;

	saveMapping(mappings, url);
}

function removeSingleMapping(element, url)
{
	data = {};

	data["measurementName"] = $(element).parents('tr').eq(0).find('td:first-child span').text();

	data["mappingIdentifier"] = $(element).attr('id').replace('_remove','');

	data["validationStudy"] = $('#matchingValidationStudy').text();

	data["predictionModel"] = $('#matchingPredictionModel').text();

	$.ajax({
		url : url + "&__action=download_json_removeMapping&predictor=" 
		+ $('#validatePredictors').attr('id'),
		async : false,
		data : data,
	}).done(function(status){

		if(status["success"] == true){
			showMessage(status["message"], true);
			$(element).parents('tr').eq(0).remove();
		}else{
			showMessage(status["message"], false);
		}
	});
}

function collectMappingFromCandidate(url)
{	
	mappings = {};

	$('#mappingResult input:checkbox').each(function(){

		if($(this).is(':checked')){

			predictor = $(this).parents('table').eq(0).attr('id').replace("mapping_", "");

			measurementName = $(this).parents('tr').eq(0).find('td>span').text();

			features = new Array();

			if(mappings[predictor] == null)
			{
				features[0] = measurementName;
			}else
			{
				features = mappings[predictor];
				index = features.length;
				features[index] = measurementName;
			}

			mappings[predictor] = features;	
		}
	});

	saveMapping(mappings, url);
}

function saveMapping(mappings, url)
{	
	validationStudy = $('#matchingValidationStudy').text();

	predictionModel = $('#matchingPredictionModel').text();

	createScreenMessage("Save mappings!", "50%");

	$.ajax({
		url : url + "&__action=download_json_saveMapping&mappingResult=" 
		+ JSON.stringify(mappings) + "&validationStudy=" + validationStudy 
		+ "&predictionModel=" + predictionModel,
		async: false,

	}).done(function(status){

		if(status["success"] == true)
		{
			$.each(status, function(key, table)
			{
				if(key != "success" & key != "message")
				{
					$('#matched_' + key).remove();

					$('#existingMappings').append(table);

					$('#matched_' + key + " tr td:last-child >div").each(function(){
						$(this).click(function(){
							removeSingleMapping($(this), url);
						});
					});
					$('#' + key).click(function(){
						$('#existingMappings table').hide();
						$('#matched_' + key).show();
					});

					$('#' + key).trigger('click');
				}
			});
			
			$('#candidateMapping input:checkbox').attr('checked', false);
			
			showMessage(status["message"], true);

		}else{
			showMessage(status["message"], false);
		}
	});

	destroyScreenMessage();
}

function getFeatureInfo(identifier, url){
	$.ajax({
		url : url + "&__action=download_json_getFeatureInfo",
		data : {
			"id" : identifier,
		},
		async : false,
	}).done(function(status){
		var table = $('<table />').attr('class','table table-striped');
		table.append('<tr><th>ID</th><td>' + status.id + '</td></tr>');
		table.append('<tr><th>Name</th><td>' + status.name + '</td></tr>');
		if(status.label !== undefined)
			table.append('<tr><th>Label</th><td>' + status.label + '</td></tr>');
		if(status.description !== undefined)
			table.append('<tr><th>Description</th><td>' + status.description + '</td></tr>');
		table.append('<tr><th>Data type</th><td>' + status.dataType + '</td></tr>');
		var categories = status.categories;
		if(categories !== undefined){
			var showCategories = '';
			for(var i = 0; i < categories.length; i++){
				var eachCategory = categories[i];
				showCategories += eachCategory.codeString + ' = ' + eachCategory.description;
				if(i < categories.length - 1)
					showCategories += '</br>';
			}
			table.append('<tr><th>Categories</th><td>' + showCategories + '</td></tr>');
		}
		addElementToModal('showFeatureInfo', 'Variable information', table);
	});
}

function addElementToModal(modalId, modalTitle, insertedElement){
	if($('#' + modalId))
		$('#' + modalId).remove();
	var row = $('<div />').attr({
		'id' : modalId,
		'class' : 'modal show'
	});
	var header = $('<div />').attr('class', 'modal-header');
	$('<h3 />').text(modalTitle).appendTo(header);
	var footer = $('<div />').attr('class', 'modal-footer');
	$('<button />').attr({
		 'class' : 'btn', 
		 'data-dismiss' : 'modal',
		 'aria-hidden' : 'true'
	}).text('Close').appendTo(footer);
	var body = $('<div />').attr('class', 'modal-body');
	insertedElement.appendTo(body);
	header.appendTo(row);
	body.appendTo(row);
	footer.appendTo(row);
	row.appendTo('body');
	$('#' + modalId).modal('show');
}

function retrieveExpandedQueries(predictor, matchedVariable, url){	
	$.ajax({
		url : url + "&__action=download_json_retrieveExpandedQuery",
		data : {
			"predictor" : predictor,
			"identifier" : $(matchedVariable).parents('tr').eq(0).attr('id').replace('_row', ''),
			"matchedVariable" : $(matchedVariable).parents('td').eq(0).children('span:first-child').text(),
			"investigationName" : $('#matchingValidationStudy').val(),
		},
		async : false,
	}).done(function(status){
		table = status["table"];
		$('#expandedQueryTable').remove();
		$('#afterMapping').append(table);
		$('#expandedQueryTable').modal('show');
		$('#expandedQueryTable span').each(function(){
			$(this).css('cursor','pointer');
			$(this).tooltip({
				title : 'click to check ontology term'
			});
			$(this).click(function(){
				getOntologyTerm($(this), url);
			});
		});
	});
}

function getOntologyTerm(element, url){
	$.ajax({
		url : url + "&__action=download_json_getOntologyTermInfo",
		data : {
			"ontologyTermId" : $(element).attr('id'),
		},
		async : false,
	}).done(function(status){
		var ontologyTermID = status.ontologyTermID;
		var label = status.label;
		var listOfDefinitions = status.ontologyTermDefinition;
		var listOfSynonyms = status.synonyms;
		var table = $('<table />').attr('class','table table-striped');
		table.append('<tr><th>ID</th><td><a href="' + ontologyTermID + '" target="_blank">' + ontologyTermID + '</a></td></tr>');
		table.append('<tr><th>Label</th><td>' + label + '</td></tr>');
		if(listOfDefinitions !== null){
			var definitions = "";
			for(var i = 0; i < listOfDefinitions.length; i++){
				definitions += listOfDefinitions[i];
				if(i < listOfDefinitions.length - 1)
					definitions += "</br>";
			}
			table.append('<tr><th>Definitions</th><td>' + definitions + '</td></tr>');
		}
		if(listOfSynonyms !== null){
			var synonyms = "";
			for(var i = 0; i < listOfSynonyms.length; i++){
				synonyms += listOfSynonyms[i];
				if(i < listOfSynonyms.length - 1)
					synonyms += "</br>";
			}
			table.append('<tr><th>Synonyms</th><td>' + synonyms + '</td></tr>');
		}
		addElementToModal('showOntologyTerm', 'Ontology term', table);
	});
}

function filterTable(variableLabel){
	if(variableLabel == "" || variableLabel == null){
		$('#overviewTable tr').show();
	}else{
		$('#overviewTable tr:gt(0)').each(function(){
			if($(this).find('td:eq(1)').text().toLowerCase() != variableLabel.toLowerCase()){
				$(this).hide();
			}
		});
	}
}

function insertNewRow(url)
{	
	message = {};
	message["message"] = "You successfully added a new predictor!";
	message["success"] = true;

	if($('#' + $('#nameOfPredictor').val()).length == 0){

		data = {};
		
		data["selected"] = $('#selectPredictionModel').val();
		data["name"] = $('#nameOfPredictor').val() + "_" + data["selected"];
	
		if($('#labelOfPredictor').val() == "")
		{
			data["label"] = $('#nameOfPredictor').val();
		
		}else{
			data["label"] = $('#labelOfPredictor').val();
		}
		data["description"] = $('#descriptionOfPredictor').val();
		data["dataType"] = $('#dataTypeOfPredictor').val();
		data["unit_name"] = $('#unitOfPredictor').val();
		data["categories_name"] = $('#categoryOfPredictor').val();
//		data["categories_name"] = uniqueElementToString($('#categoryOfPredictor').val().split(","), ",");
//		buildingBlockString = uniqueElementToString($('#buildingBlocks').val().split(";"), ";");
//		data["buildingBlocks"] = uniqueElementToString(buildingBlockString.split(","), ",");
		data["buildingBlocks"] = $('#buildingBlocks').val();
		data["leadingElement"] = $('#leadingElement').val();
		//add the data to table
		identifier = data["name"].replace(/\s/g,"_");
		data["identifier"] = identifier;

		$.ajax({
			url :  url + "&__action=download_json_addPredictor&data=" + JSON.stringify(data),
			async: false,
		}).done(function(status){
			message["message"] = status["message"];
			message["success"] = status["success"];
			data["identifier"] = message["identifier"];
		});

		if(message["success"] == true){

			populateRowInTable(data, url);
		}

	}else{
		message["message"] = "Predictor already existed!";
		message["success"] = false;
	}

	return message;
}

function populateRowInTable(data, url)
{
	name = data["name"];
	label = data["label"];
	description = data["description"];
	identifier = data["identifier"];

	newRow =  "<tr id=\"" + identifier + "\" name=\"" + identifier + "\" style=\"width:100%;\">";
	newRow += "<td name=\"variableName\" class=\"ui-corner-all\"><span style=\"float:left;cursor:pointer;\">" + name + "</span>";
	newRow += "<div id=\"" + identifier + "_remove\" style=\"cursor:pointer;float:right;margin:10px;margin-left:2px;height:16px;width:16px;\" "
	+ "title=\"remove this predictor\">"
	+ "<span class=\"icon-remove\"></span>"
	+ "</div></td>";
	newRow += "<td id=\"label\" name=\"label\" class=\"ui-corner-all\">" + label + "</td>";
	newRow += "<td id=\"description\" name=\"description\" class=\"ui-corner-all\">" + description + "</td></tr>";

	$('#overviewTable').append(newRow);

	$('#' + identifier + '_remove').click(function(){
		removePredictor($(this), url);
	});

	$('#showPredictorPanel table tr:last-child').css({
		'vertical-align':'middle',
		'text-align':'center',
		'font-size':'16px',
	});
	
	$('#' + identifier).data('dataObject', data);
	
	$('#' + identifier + ' td:first-child').click(function(){
		
		dataObject = $(this).parents('tr').eq(0).data("dataObject");
		
		$(document).data("selectedVariable", dataObject["name"]);
		
		identifier = dataObject["identifier"];
		label = dataObject["label"];
		name = dataObject["name"];
		description = dataObject["description"];
		dataType = data["dataType"];
		categories = data["category"];
		unit = data["unit"];
		buildingBlocks = data["buildingBlocks"];
		leadingElement = data["leadingElement"];
		
		row = "<tr><th class=\"ui-corner-all\">ID:</td>";
		row += "<td class=\"ui-corner-all\">" + identifier + "<div style=\"cursor:pointer;height:16px;width:16px;" 
			+ "float:right;margin:10px;margin-left:2px;\" "
			+ "title=\"remove this predictor\">"
			+ "<span class=\"icon-remove\"></span>"
			+ "</div></td></tr>";
		row += "<tr><th class=\"ui-corner-all\">Name:</td>";
		row += "<td class=\"ui-corner-all\"><span style=\"float:left;cursor:pointer;\">" + name + "</span></td></tr>";
		row += "<tr><th class=\"ui-corner-all\">Label:</td>";
		row += "<td class=\"ui-corner-all\">" + label + "</td></tr>";
		row += "<tr><th class=\"ui-corner-all\">Description:</td>";
		row += "<td id=\"variableDescription\" name=\"variableDescription\"  class=\"ui-corner-all\">" + description + "</td></tr>";

		if(buildingBlocks != "" && buildingBlocks != null)
		{
			var selectBlocks = "";
			for( var i = 0 ; i < buildingBlocks.split(";").length ; i++)
			{
				eachDefinition = buildingBlocks.split(";")[i];
				blocks = eachDefinition.split(",");
				selectBlocks += createMultipleSelect(blocks);		
			}
			
			row += "<tr><th class=\"ui-corner-all\">Building blocks:</td>"
				+ "<td id=\"variableBuildingBlocks\" name=\"variableBuildingBlocks\" class=\"ui-corner-all\">" + selectBlocks + "</td></tr>";
		}
		
		if(leadingElement != "" && leadingElement != null)
		{
			var selectElements = createMultipleSelect(leadingElement.split(";"));
			row += "<tr><th class=\"ui-corner-all\">Leading element:</td>"
				+ "<td id=\"variableLeadingElement\" name=\"variableLeadingElement\" class=\"ui-corner-all\">" + selectElements + "</td></tr>";
		}
		
		row += "<tr><td></td><td><input type=\"button\" id=\"matchSelectedVariable\"  style=\"margin-left:250px;\"" 
			+  "class=\"btn btn-info btn-small\" value=\"Match selected variable\"></td><tr>";
		
		$('#variableDetail').empty().append(row).show();
		$('td[name="variableBuildingBlocks"] >select').chosen();
		$('td[name="category"] >select').chosen();
		$('td[name="variableLeadingElement"] >select').chosen();
//		$('#variableDetail i').each(function(){
//			$(this).click(function(){
//				$(this).parent().children().hide();
//				var option = null;
//				$(this).parent().children('select option').each(function(){
//					option += $(this).text() + ";";
//				});
//				if(option != null)
//					option = option.subString(0, option.length - 1);
//				$(this).parent().append('<input type="textfield"/>');
//				var confirmButton = $('<button />').val();
//				input.val(option).click(function(){
//					var options = $(this).val().split(';');
//					var select = $(this).parent().children('select').empty();
//					for(var i = 0; i < options.length; i++){
//						select.append('<option select="selected">' + options[i] + '</option>');
//					}
//					select.trigger("liszt:updated");
//					$(this).parent().children().show();
//					$(this).remove();
//				});
//				
//			});
//		});
		$('#variableDetail tr:eq(0) div').click(function(){
			$('#overviewTable').parents('div').eq(0).width("100%").find('tr').children().show();
			$("input[name=\"selectedVariableID\"]").val(null);
			$('#variableDetail').empty().hide();
		});
		
		$('#matchSelectedVariable').click(function(){
			$("input[name=\"selectedVariableID\"]").val($(document).data('selectedVariable'));
			$('#whetherWholeSet').text($(this).parents('table:eq(0)').find('tr:eq(2) td').text());
			$('#selectCohortStudyPanel').modal('show');
		});
		
		$('#overviewTable tr').each(function(){
			var i = 0;
			$(this).children().each(function()
			{
				if(i > 0)
					$(this).hide();
				i++;
			});
		});
		
		$('#overviewTable').parents('div').eq(0).css('width', '35%');
	});

	cancelAddPredictorPanel();
}

function addNewPredictionModel(url)
{	
	if($('#addPredictionModel').val() != ""){
		selected = $('#addPredictionModel').val();
		if($('#selectPredictionModel').find("option[name=\"" + selected +"\"]").length > 0){
			message = "The prediction model already existed!";
			showMessage(message, false);
		}else{
			message = "";
			success = "";
			$.ajax({
				url : url + "&__action=download_json_newPredictionModel&name=" + selected,
				async: false,
			}).done(function(status){
				message = status["message"];
				success= status["success"];
			});
			if(success == true){
				element = "<option name=\"" + selected + "\" selected=\"selected\">" + selected + "</option>";
				$('#selectPredictionModel').append(element);
				$('#selectPredictionModel').trigger("liszt:updated");
				$('#addPredictionModel').val('');
				$('#selectedPrediction >span').empty().text(selected);
				showPredictors(selected, url);
			}
			showMessage(message, success);
		}
	}
}

function removePredictionModel(url)
{	
	if($('#selectPredictionModel option').length > 0)
	{
		selected = $('#selectPredictionModel').val();

		$.ajax({
			url : url + "&__action=download_json_removePredictionModel&name=" + selected,
			async: false,
		}).done(function(status){
			message = status["message"];
			success= status["success"];
		});
		if(success == true){
			$('#confirmWindow').dialog('close');
			$('#selectPredictionModel option').filter(':selected').remove();
			$('#selectPredictionModel').trigger("liszt:updated");
			selected = $('#selectPredictionModel').val();
			$('#selectedPrediction >span').empty().text(selected);
			showPredictors(selected, url);
		}
		showMessage(message, success);
	}
}

function addPredictor(url)
{
	message = "";
	success = "";

	if( $('#selectPredictionModel option').length == 0){
		message = "Please define a prediction model first!";
		success = false;
	}else if($('#nameOfPredictor').val() == ""){
		message = "The name of predictor cannot be empty!";
		success = false;
	}else{
		result = insertNewRow(url);
		message = result["message"];
		success = result["success"];
		predictor = result["identifier"];
	}
	
	showMessage(message, success);
	
	showPredictors($('#selectPredictionModel').val(), url);
}

function removePredictor(element, url)
{	
	predictorName = $(element).parents('td').eq(0).children('span:first-child').text();
	
	predictorID = $(element).parents('tr').eq(0).attr('id');
	
	selected = $('#selectPredictionModel').val();
	
	$.ajax({
		url : url + "&__action=download_json_removePredictors&name=" 
		+ predictorName + "&predictorID=" + predictorID + "&predictionModel=" + selected,
		async: false,
	}).done(function(status){
		
		message = status["message"];
		
		success = status["success"];
		
		showMessage(message, success);
		
		if(success == true)
		{
			$(element).parents('tr').eq(0).remove();
		}
	});
}

function showPredictors(predictionModelName, url)
{	
	$.ajax({
		url : url + "&__action=download_json_showPredictors&name=" + predictionModelName,
		async: false,
	}).done(function(status){
		$('#overviewTable').parents('div').eq(0).width("100%").find('tr').children().show();
		$('#selectedPredictionModelName').val(status["name"]);
		$('#formula').val(status["formula"]);
		$('#showFormula').val(status["formula"]);
		$('#showPredictorPanel table tr:gt(0)').remove();
		listOfFeatures = status["predictorObjects"];
		var autoCompleteSource = new Array();
		if(listOfFeatures != null){
			for(var i = 0; i < listOfFeatures.length; i++){
				var eachVariableInfo = listOfFeatures[i];
				autoCompleteSource.push(eachVariableInfo["label"]);
				populateRowInTable(eachVariableInfo, url);
			}
			$('#searchVariable').typeahead({
			      source: autoCompleteSource
		    });
		}
	});
}

//Make the add predictor panel disappear
function cancelAddPredictorPanel()
{	
	$('#defineVariablePanel').modal('hide');
	$('#defineVariablePanel input[type="text"]').val('');
	$('#dataTypeOfPredictor option:first-child').attr('selected',true);
	$('#categoryOfPredictor').attr('disabled', true);
}

//Create list with unique elements
function uniqueElements(anArray)
{
	var result = [];
	$.each(anArray, function(i,v){
		if ($.inArray(v, result) == -1) result.push(v);
	});
	return result;
}

function uniqueElementToString(anArray, separator){	
	unique = uniqueElements(anArray);
	backToString = "";
	for(var i = 0; i < unique.length; i++){
		if(separator != null){
			backToString += unique[i] + separator;
		}else{
			backToString += unique[i] + ",";
		}
	}
	backToString = backToString.substring(0, backToString.length - 1);
	return backToString;
}

//Create a jquery chosen multiple select element
function createMultipleSelect(listOfTerms){	
	listOfTerms = uniqueElements(listOfTerms);
	selectBlocks = "<select multiple=\"true\" style=\"width:90%;\">";
	for(var i = 0; i < listOfTerms.length; i++){
		selectBlocks += "<option selected=\"selected\">" + listOfTerms[i] + "</option>";
	}
	selectBlocks += "</select>";
//	selectBlocks += "<i class=\"icon-edit\"></i>"
	return selectBlocks;
}

function editJqueryChosen(element){
	$(element).append('<input type="textfield" value="change your code"/>');
}

function showMessage(message, success)
{	
	messagePanel = "<fieldset id=\"statusMessage\" class=\"ui-tabs-panel ui-widget-content ui-corner-bottom\">"
		+ "<legend style=\"font-style:italic;\">Status</legend>"
		+ "<div align=\"justify\" style=\"font-size:14px;padding:4px;\">"
		+ "Please choose an existing prediction model or add a new prediction model </div></fieldset>";		

	$('#messagePanel').empty().append(messagePanel);

	$('#statusMessage').css({
		position : "absolute",
		width : "250px",
		height : "140px",
		top : 150,
		right : 30
	});

	element = "";

	if(success == true){
		element = "<p style=\"color:green;\">";
	}else{
		element = "<p style=\"color:red;\">";
	}

	element += message + "</p>";

	$('#statusMessage >div').empty().append(element);

	$('#statusMessage').effect('bounce').delay(2000).fadeOut().delay(2000);
}

function createScreenMessage(showMessage, height)
{	
	if(height == null)
	{
		height = "50%";
	}

	showModal();

	$('body').append("<div id=\"progressbar\" class=\"middleBar\">" + showMessage + "</div>");

	$('.middleBar').css({
		'left' : '46%',
		'top' : height,
		'height' : 50,
		'width' : 300,
		'position' : 'absolute',
		'z-index' : 1501,
	});
}

function destroyScreenMessage()
{			
	$('.modalWindow').remove();

	$('#progressbar').remove();
}

function createProgressBar(showMessage)
{	
	showModal();

	$('body').append("<div id=\"progressbar\" class=\"middleBar\"></div>");
	$('body').append("<div id=\"LoadPredictor\" class=\"middleBar\" style=\"font-size:25px;padding-left:50px;padding-top:15px;font-style:italic;\">" 
			+ showMessage + "</div>");

	$("#progressbar").progressbar({value: 0});

	$('.middleBar').css({
		'left' : '38%',
		'top' : '50%',
		'height' : 50,
		'width' : 300,
		'position' : 'absolute',
		'z-index' : 1501,
	});
}

function updateProgressBar(percentage)
{
	$("#progressbar").progressbar({
		value: percentage * 100
	});
}

function destroyProgressBar()
{	
	$('.middleBar').remove();
	$('.modalWindow').remove();
	$('#progressbar').remove();
}

function showModal()
{	
	$('body').append("<div class=\"modalWindow\"></div>");

	$('.modalWindow').css({
		'left' : 0,
		'top' : 0,
		'height' : $('.formscreen').height() + 200,
		'width' : 2 * $('body').width(),
		'position' : 'absolute',
		'z-index' : '1500',
		'opacity' : '0.5',
		background : 'grey'
	});
}