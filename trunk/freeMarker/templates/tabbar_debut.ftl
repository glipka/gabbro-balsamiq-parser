 <ul class="nav nav-tabs  tabs-left" role="tablist" id="tabbar${internalId}"> 
	<#list tabs as tab>
			<li><a href="${tab.getTab()}" role="tab" data-toggle="tab">${tab.getTab()}</a></li>
	 </#list> 
        
</ul>
	<div class="tabbable">
        <div class="tab-content">

			 <div class="tab-pane" id="tab1">
	