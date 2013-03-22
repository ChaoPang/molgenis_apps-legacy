<#macro plugins_luceneIndexer_LuceneIndexer screen>
	<script>
		$(document).ready(function(){
			$('#indexDataItem').click(function(){
				$('input[name="__action"]').val('indexDataItems');
				$('form[name="${screen.name}"]').submit();
			});
		});	
	</script>
	<form method="post" enctype="multipart/form-data" name="${screen.name}" action="">
	<!--needed in every form: to redirect the request to the right screen-->
	<input type="hidden" name="__target" value="${screen.name}">
	<!--needed in every form: to define the action. This can be set by the submit button-->
	<input type="hidden" name="__action">
	<#--optional: mechanism to show messages-->
	<div class="formscreen">
		<div class="form_header" id="${screen.getName()}">
			${screen.label}
		</div>
		Welcome!</br></br>
		<button id="indexDataItem" class="btn btn-primary" type="button">Index data items</button>
	</div>
	</form>
</#macro>