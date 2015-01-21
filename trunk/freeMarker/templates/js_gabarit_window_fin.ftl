${templatingProperties.getPreserveSectionCodeBegin()}${templatingProperties.getDelimiterTemplateNameBeginInPreserveSection()}${templateName}${templatingProperties.getDelimiterTemplateNameEndInPreserveSection()}
<#if traitementPreserveSection??>
 ${traitementPreserveSection.getSectionContent(templateName)}${templatingProperties.getPreserveSectionCodeEnd()}
 <#else>
 ${commonObject.getPreserveSection("javascript","").getSectionContent(templateName)}${templatingProperties.getPreserveSectionCodeEnd()}
</#if>  