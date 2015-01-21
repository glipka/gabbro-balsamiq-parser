  <div class="checkbox"   id="checkbox${internalId}">
          <input type="checkbox" value=""  
       <#if state??>
	 	  <#switch state>
			<#case "disabled">
				disabled='true'
			<#break>
			<#case "disabledSelected">
				disabled='true' checked='yes'
			<#break>
			<#case "selected">
				checked='yes'
			<#break>
			<#default>
     
		</#switch>  
		</#if>
		  ><label> <#if widget??>  ${widget.formatText(text)} <#else>${text}</#if>
 