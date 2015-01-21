 
  
                 
 <#if globalContext??> 
 
  composants formulaires
   <#list globalContext.getPaths  () as location>
    location = =${location.getLocation()}  
	shortPath = =${location.getShortPath()}  
 </#list> 
 <#list globalContext.getBindedForms () as formulaire>
    classe du Formulaire = =${formulaire.getClassname()}  
    Instancecode du Formulaire = =${formulaire.getInstanceCode()} 
	shortPath du Formulaire = =${formulaire.getShortPath()}
    customID widget du Formulaire = =${formulaire.getWidget().getCustomId()}	 
 </#list> 
  
</#if>