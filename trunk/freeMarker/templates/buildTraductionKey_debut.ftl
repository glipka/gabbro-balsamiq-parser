 <#if isAttribute == "true"><#t>
${usecaseName}.${generatedFileName}.<#t>
<#if container != "">${container}.</#if><#t>
${currentTag?replace("bstrap-c:","")?replace("bstrap-f:","")?replace("bstrap:","")}.${attributName}.${index}<#t>
<#else><#t>
${usecaseName}.${generatedFileName}.<#t>
<#if container != "">${container}.</#if><#t>
${currentTag?replace("bstrap-c:","")?replace("bstrap-f:","")?replace("bstrap:","")}.${index}<#t>
</#if><#t>