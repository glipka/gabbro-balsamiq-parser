
<#assign js1="   
$(function () {
$(\'#select${internalId}\').empty();
var  tab = JSON.parse(localStorage[\"objectSelect\"]);
tab.forEach(function(y) { 
var option=\"<option>\" + y + \"</option>\";
$(\'#select${internalId}\').append(option);
 }); 
	
	$(\'#select${internalId}\').select2();
})

">

${globalContext.registerJavascriptSection(js1,isAFragment, "section1")}
 	 