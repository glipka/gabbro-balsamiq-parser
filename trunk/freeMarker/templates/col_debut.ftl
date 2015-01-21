<#if containerName??>
	 	  <#switch containerName>
			<#case "DataGrid">
				<td
				
	<#if container.getColumns()[colNumber?number].getAlignment() =="C">style="text-align:center" <#elseif container.getColumns()[colNumber?number].getAlignment() =="L">style="text-align:left"<#elseif container.getColumns()[colNumber?number].getAlignment() =="R">style="text-align:right"</#if> >
				<#break>
			<#default>
				 <div  class="col-md-${bootstrapColWidth} col-md-offset-${bootstrapColOffset}">
		</#switch>  
</#if>