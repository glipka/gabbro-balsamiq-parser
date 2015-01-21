

// section main 


// section1
 ${globalContext.getJavascriptCodeForTheSection(javascriptUseCase, javascriptFileName, "section1")}
  
// section2 


// section Commune
 ${globalContext.getJavascriptCodeForTheSection(javascriptUseCase, javascriptFileName, "commonSection")}

// preserve section
${templatingProperties.getPreserveSectionCodeBegin()}${templatingProperties.getDelimiterTemplateNameBeginInPreserveSection()}${templateName}${templatingProperties.getDelimiterTemplateNameEndInPreserveSection()}<#t>
<#if traitementPreserveSection??>
 ${traitementPreserveSection.getSectionContent(templateName)}${templatingProperties.getPreserveSectionCodeEnd()}
 <#else>
 ${commonObject.getPreserveSection(templatingProperties.getPreserveCodeScript(),"").getSectionContent(templateName)}${templatingProperties.getPreserveSectionCodeEnd()}
</#if>  