<#if fragment??>
fragmentName=${fragment.getFragmentName()}
ucName=${fragment.getUcName()}
 location     =${fragment.getLocation().getLocation()}  
 </#if>
 
 

<#if variablesValidate??>
	<#list variablesValidate as validate>
		validate$$$$$ Name:${validate.getName()}  Valeur:${validate.getValeur()}
	</#list>
</#if>
<input  type="text" class="form-control" <#if widgetContainer??>id="${id(widget.shortWidgetName(),internalId,widget.getCustomId(),widgetContainer.getCustomId())}"</#if> placeholder="${text}"  autocomplete="on" maxlength="10" <#if variableBinding??>name=${variableBinding}</#if>  <#if variableBinding??>value=${variableBinding}</#if> >



<#function id  type internalId customId idPere  >   
  <#if idPere == ""><#return type+internalId> 
  <#elseif customId == "" ><#return type +internalId> 
   </#if>
   <#return idPere + "_" + type + "_" +customId + "_" + internalId>
</#function>
