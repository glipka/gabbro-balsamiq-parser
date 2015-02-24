

// section main 


// section1
 ${globalContext.getJavascriptCodeForTheSection(javascriptUseCase, javascriptFileName, "section1")}
  
// section2 


// section Commune
 ${globalContext.getJavascriptCodeForTheSection(javascriptUseCase, javascriptFileName, "commonSection")}
 
<#assign initialContent>

 	 // content1 ligne1
	 // content1 ligne2
</#assign>
${templatingProperties.getPreserveSectionCodeBegin()}${templatingProperties.getDelimiterTemplateNameBeginInPreserveSection()}${templateName}${templatingProperties.getDelimiterTemplateNameEndInPreserveSection()}<#t>
<#if traitementPreserveSection??>
 ${traitementPreserveSection.getSectionContent(templateName,initialContent)}${templatingProperties.getPreserveSectionCodeEnd()}
 <#else>
${globalContext.getPreserveSection(usecaseName,generatedFileName,templatingProperties.getPreserveCodeScript(),"").getSectionContent(templateName,initialContent)}${templatingProperties.getPreserveSectionCodeEnd()}</#if>
  