<#assign js1>
 	 <dhtmlx:grid id="dhtmlxgrid${internalId}" width="100%" height="*"  skin="auchan" bundleName="${pageEnCours}_Basic\">
	 <dhtmlx:columns>
	 <dhtmlx:columnsrow>
	 <#list columns as column>
			<dhtmlx:column id="${column.getColumnName()}" align="${alignementColonne(column.getAlignment())}"  width="${column.getWidth()}%" type="${typeDeWidget(column.getcolumnType(), column.getReadonly())}" labelKey="${column.getColumnName()?cap_first}" ${generateSort(column.getSort())}  ${ typeDeTri(column.getcolumnType())}  > </dhtmlx:column>		 
	</#list>
	 </dhtmlx:columnsrow>
	</dhtmlx:columns>
 	<dhtmlx:paging pageSize="12" />   
 	<dhtmlx:dataprocessor serverUrl="${pageContext.request.contextPath}/${pageEnCours}_Basic.do?param=loadXML${pageEnCours}"/>
 	<dhtmlx:attachEvent eventName="onDhxCalendarCreated" eventHandler="dhxCalendarCreated"></dhtmlx:attachEvent>
 	<dhtmlx:attachEvent eventName="onXLE" eventHandler="onXLE"></dhtmlx:attachEvent> 
</dhtmlx:grid> 
</#assign>

${globalContext.mise_en_cache_code_javascript(generatedFileName,js1,isAFragment, "sectionDhtmlxgrid")}
 	 
<#function typeDeWidget type readonly>  
  <#if type == "numeric"><#return "ron"> 
  <#elseif type == "numeric" && readonly == "false"><#return "edn"> 
  <#elseif type == "text"><#return "ro"> 
  <#elseif type == "text" && readonly == "false"><#return "ed"> 
  <#elseif type == "textarea"><#return "text"> 
  <#elseif type == "textarea" && readonly == "false"><#return "text"> 
  <#elseif type == "checkbox"><#return "ch"> 
  <#elseif type == "checkbox" && readonly == "false"><#return "ch">
  <#elseif type == "radiobutton"><#return "ra"> 
  <#elseif type == "radiobutton" && readonly == "false"><#return "ra">  
  <#elseif type == "combobox"><#return "combo"> 
  <#elseif type == "combobox" && readonly == "false"><#return "combo">  
  <#elseif type == "list"><#return "co"> 
  <#elseif type == "list" && readonly == "false"><#return "coro">  
  <#elseif type == "colorpicker"><#return "cp"> 
  <#elseif type == "colorpicker" && readonly == "false"><#return "cp">  
  <#elseif type == "calendar"><#return "dhxCalendar"> 
  <#elseif type == "calendar" && readonly == "false"><#return "dhxCalendar">  
  <#elseif type == "link"><#return "link"> 
  <#elseif type == "link" && readonly == "false"><#return "link">  
  <#elseif type == "img"><#return "img"> 
  <#elseif type == "img" && readonly == "false"><#return "img"> 
   </#if>
   <#return "????">
</#function>


<#function alignementColonne alignement>  
  <#if alignement == "C"><#return "center"> 
  <#elseif alignement == "L"><#return "left"> 
  <#elseif alignement == "R"><#return "right"> 
  </#if>
  <#return "left">
</#function>

<#function generateSort sort>  
  <#if sort == "asc"><#return " sort='asc' "> 
  <#elseif sort == "desc"><#return " sort='desc' "> 
  <#elseif sort == "bidirect"><#return " sort='bi-direct' "> 
  </#if>
  <#return " ">
</#function> 

<#function typeDeTri type  >  
  <#if type == "numeric"><#return " sortType='numeric' "> 
 
  <#elseif type == "text"><#return " sortType='string' "> 
  
  <#elseif type == "textarea"><#return " sortType='string' ">
 
  <#elseif type == "checkbox"><#return " sortType='n/a' ">
  
  <#elseif type == "radiobutton"><#return " sortType='n/a' ">
    
  <#elseif type == "combobox"><#return " sortType='n/a' ">
   
  <#elseif type == "list"><#return " sortType='string' ">
   
  <#elseif type == "colorpicker"><#return " sortType='n/a' ">
   
  <#elseif type == "calendar"><#return " sortType='n/a' ">
  
  <#elseif type == "link"><#return " sortType='n/a' ">
  
  <#elseif type == "img"><#return " sortType='n/a' ">
   </#if>
   <#return "????">
</#function>