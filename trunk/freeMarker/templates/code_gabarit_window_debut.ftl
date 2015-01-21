<#assign className=generatedFileName + 'Sub1'>
<#assign contentStub1="ligne1\r\n"+ "ligne1\r\n" +  "ligne2\r\n" +"ligne3\r\n">
${mockupContext.setCodeClasse(className,contentStub1,generationProperties.generatedsubPackage1())}

<#assign className2=generatedFileName + 'Sub2'>
<#assign contentStub2="ligne1 \r\n"+
 "ligne1 \r\n" + 
 "ligne2 \r\n" +
 "ligne3\r\n ">
${mockupContext.setCodeClasse(className2,contentStub2,generationProperties.generatedsubPackage1())}
