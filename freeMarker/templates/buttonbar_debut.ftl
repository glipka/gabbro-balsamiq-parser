
<div class="btn-group"  id="div${internalId}"> 
  <#list urls as url>
	<button type="button" class="btn btn-default" id="btn${url.getId()}">${url.getLibelle()}</button>	
 </#list>

</div>