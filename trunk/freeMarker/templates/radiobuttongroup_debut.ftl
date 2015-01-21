<#list items as node>
 
  <div class="radio"   id="radio${node.getId_interne()}">
  
          <input type="radio" value=""  
       <#if node.getState??>
	 	  <#switch node.getState()>
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
		  >  <label>${node.getText()}</label>
	</div>
 
 </#list>