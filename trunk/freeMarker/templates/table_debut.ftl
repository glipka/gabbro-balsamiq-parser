<div id="div${internalId}" class="table-responsive"> 
<table id="table${internalId}" class="table table-bordered">
 <thead>   <tr>            
<#list columns as column>
		<th <#if column.getAlignment() =="C">style="text-align:center" <#elseif column.getAlignment()  =="L">style="text-align:left"<#elseif column.getAlignment() =="R">style="text-align:right"</#if> >
				 ${column.getColumnName()?replace("\\r", "<br>")}
		</th> 
 </#list> </tr>
     </thead>