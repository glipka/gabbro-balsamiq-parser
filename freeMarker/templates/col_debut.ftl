<#if containerName??>
	<#switch containerName>
	<#case "DataGrid">
				<td
		<#assign column= container.getColumns()[colNumber?number]!"">
		<#if column?has_content>
			<#assign columnAlignment=column.getAlignment()!"C">
		<#else>
			<#assign columnAlignment="C">
		</#if>
			
		<#if columnAlignment =="C">style="text-align:center" <#elseif columnAlignment=="L">style="text-align:left"<#elseif columnAlignment =="R">style="text-align:right"<#else>style="text-align:center"</#if> >
		<#break>
			
		<#default>
				 <div  class="col-md-${bootstrapColWidth} col-md-offset-${bootstrapColOffset}">
		</#switch>  
</#if>