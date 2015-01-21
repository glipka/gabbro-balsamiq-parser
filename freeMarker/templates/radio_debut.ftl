<div class="radio"   id="radio${internalId}">
          <input type="radio" value=""
		  
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
		  >  <label><#if widget??> ${widget.formatText(text)}<#else>${text}</#if>