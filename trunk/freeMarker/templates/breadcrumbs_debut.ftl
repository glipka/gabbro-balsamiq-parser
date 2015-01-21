<ul id="ul${internalId}"  class="breadcrumb">
<#list urls as url>
		<li id="li${url.getId()}">  
				<a href="${url.getHref()}">${url.getLibelle()}</a> <span class="divider"></span>  
		</li>  
 </#list>