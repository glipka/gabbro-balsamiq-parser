 <#if generateController == "true"> 
 <#if usecaseName != "">
 package ${generationProperties.getSrcDtoFilesDir()}.${usecaseName}.${generationProperties.getGeneratedControllerAlias()}
 <#else>
 package ${generationProperties.getSrcDtoFilesDir()}.${generationProperties.getGeneratedControllerAlias()}
 </#if>
 class  ${generatedFileName?cap_first}${generationProperties.getGeneratedControllerAlias()?cap_first} 
 
 template controleur
 genertadscala:${generationProperties.srcJavascriptFilesDir()}
                 
 <#if mockupContext??> 
 location: ${mockupContext.getLocation().getLocation()}
 location: ${mockupContext.getLocation().getShortPath()}
  composants formulaires
 <#list mockupContext.getBindedForms () as formulaire>
    classe du Formulaire = =${formulaire.getClassname()}  
    Instancecode du Formulaire = =${formulaire.getInstanceCode()} 
	shortPath du Formulaire = =${formulaire.getShortPath()}
    customID widget du Formulaire = =${formulaire.getWidget().getCustomId()}	
 customData widget du Formulaire = =${formulaire.getWidget().getBind()}		
	action du formulaire ${formulaire.getWidget().getExtendedAttributes("actionDuFormulaire")}
 </#list> 
 composant non formulaire
 <#list mockupContext.getFirstLevelObject() as notFormulaire>
     classe du Formulaire = =${notFormulaire.getClassname()}  
     instancecode du Formulaire = =${notFormulaire.getInstanceCode()} 
     customID widget du Formulaire = =${notFormulaire.getWidget().getCustomId()}		 
 </#list> 

 
${templatingProperties.getPreserveSectionCodeBegin()}${templatingProperties.getDelimiterTemplateNameBeginInPreserveSection()}${templateName}${templatingProperties.getDelimiterTemplateNameEndInPreserveSection()}
    ${traitementPreserveSection.getSectionContent(templateName)}
${templatingProperties.getPreserveSectionCodeEnd()}


 nom des fragments
 <#list mockupContext.getFragments() as fragment>
   nom du fragment   =${fragment.getFragmentName()}    
   nom du fichier  =${fragment.getFicName()} 
   usecase Name  =${fragment.getUcName()} 
   location     =${fragment.getLocation()}    
   Type De Fragment     =${fragment.getTypeOfFragment()}  
 </#list> 
</#if>
</#if>

 
${templatingProperties.getPreserveSectionCodeBegin()}${templatingProperties.getDelimiterTemplateNameBeginInPreserveSection()}${templateName}${templatingProperties.getDelimiterTemplateNameEndInPreserveSection()}
    ${traitementPreserveSection.getSectionContent(templateName)}
${templatingProperties.getPreserveSectionCodeEnd()}


