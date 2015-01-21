 
 <#if widgetName == "DataGrid" || widgetName == "dhtmlxgrid" > 
	var list${instance?capitalize} : List[${className}] = scala.collection.mutable.List[${className}]()    // composant=  ${widgetName}
<#else>
	var ${instance} : ${className} = new ${className}()   // composant=${widgetName}
</#if> 
 
 