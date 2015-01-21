 ${generateField(getType(fieldType,widgetName))}
 <#function generateField type2 >
 <#if type2 == "List<String>">
    <#return " 
	${tabulation}private ${type2} ${fieldName}; // composant: ${widgetName}  
	${tabulation}public ${type2} get${fieldName?capitalize}() {
	${tabulation}if (${fieldName} == null) {
			${fieldName} = new ArrayList<String>();
	${tabulation}}
	${tabulation}	return ${fieldName};
	${tabulation}}

	${tabulation}public void set${fieldName?capitalize}(${type2} ${fieldName}) {
		${tabulation}	this.${fieldName} = ${fieldName};
	${tabulation}}
	"
    >
	<#elseif type2 == "String" || type2 == "Int" || type2 == "Double" || type2 == "Boolean" >  
	<#return "
	${tabulation}private ${type2} ${fieldName}; // composant: ${widgetName} // composant: ${widgetName}  
	${tabulation}public ${type2} get${fieldName?capitalize}() {
		${tabulation}return ${fieldName};
	${tabulation}}
	${tabulation}public void set${fieldName?capitalize}(${type2} ${fieldName}) {
			${tabulation}this.${fieldName} = ${fieldName};
	${tabulation}}
		"
    >
	<#elseif type2 == "Date"> 
	<#return "
	${tabulation}private ${type2} ${fieldName}; // composant: ${widgetName} // composant: ${widgetName}  
	${tabulation}public ${type2} get${fieldName?capitalize}() {
	${tabulation}	if (${fieldName} == null) {
	${tabulation}	${fieldName} = new Date();
	${tabulation}}
	${tabulation}return ${fieldName};
	${tabulation}}
	${tabulation}public void set${fieldName?capitalize}(${type2} ${fieldName}) { 
	${tabulation}	this.${fieldName} = ${fieldName};
	${tabulation}}
		"
    >
	<#else>
	<#return "
	${tabulation}private ${type2} ${fieldName} = new ${type2?capitalize}(); // composant: ${widgetName} "
	>
 </#if>
</#function> 
 <#function getType type control>  
  <#if type != "????"><#return type> 
  <#elseif control == "TextInput"><#return "String"> 
  <#elseif control == "DateChooser"><#return "Date"> 
  <#elseif control == "Combobox"><#return "List<String>"> 
  <#elseif control == "List"><#return "List<String>">
  <#elseif control == "CheckBox"><#return "Boolean"> 
  </#if>
  <#return "String">
</#function> 