<#if containerName??>
	<#switch containerName>
	<#case "DataGrid">
				<td
		<#assign column= container.getColumns()[colNumber?number]!"">
		<#if column?has_content>
			<#assign columnAlignment=column.getAlignment()!"C">
			<#assign bootstrapWidth=column.getBootstapWidth()!"0">
		<#else>
			<#assign columnAlignment="C">
		 	<#assign bootstrapWidth="0">
		</#if>
	    class="col-md-${bootstrapWidth}" 
		<#if columnAlignment =="C">style="text-align:center" <#elseif columnAlignment=="L">style="text-align:left"<#elseif columnAlignment =="R">style="text-align:right"<#else>style="text-align:center"</#if> >
		<#break>
			
		<#default>
				 <div  class="col-md-${bootstrapColWidth} col-md-offset-${bootstrapColOffset}">
		</#switch>  
</#if>


 