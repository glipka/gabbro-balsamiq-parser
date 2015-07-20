<#if containerName??>
	<#switch containerName>
	<#case "DataGrid">
				<td
		<#assign column= container.getColumns()[colNumber?number]!"">
		<#if column?has_content>
			<#assign columnAlignment=column.getAlignment()!"C">
			<#assign columnWidthIn12th=column.getWidthIn12Th()!"0">
		<#else>
			<#assign columnAlignment="C">
		 	<#assign columnWidthIn12th="0">
		</#if>
	    class="col-md-${columnWidthIn12th}" 
		<#if columnAlignment =="C">style="text-align:center" <#elseif columnAlignment=="L">style="text-align:left"<#elseif columnAlignment =="R">style="text-align:right"<#else>style="text-align:center"</#if> >
		<#break>
			
		<#default>
				 <div  class="col-md-${bootstrapColWidth} col-md-offset-${bootstrapColOffset}">
		</#switch>  
</#if>


 