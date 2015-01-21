<!DOCTYPE html>
<html lang="en">
  <head>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta charset="UTF-8">
  <!-- Latest compiled and minified CSS -->
<link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css">

<!-- Optional theme -->
<link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap-theme.min.css">
<link rel="stylesheet" href="http://ivaynberg.github.io/select2/select2-3.5.1/select2.css">
<script src="http://code.jquery.com/jquery-1.11.0.min.js"></script>
<!-- Latest compiled and minified JavaScript -->
<script src="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js"></script>
<script src="http://ivaynberg.github.io/select2/select2-3.5.1/select2.js"></script>

  
   <style>

  body {
  font-size:100%;
   font: 12x Helvetica;
   background: #999999;
   @media (max-width: 640px) { body {font-size:1rem;} } 
@media (min-width: 640px) { body {font-size:0.6rem;} } 
@media (min-width:960px) { body {font-size:0.8rem;} } 
@media (min-width:1100px) { body {font-size:0.8rem;} } 
   
   
  }
  
  

  #main {
   margin: 0px;
   padding: 0px;
   width:100%;
   height:100%;
   
   }
 
  
  header, footer {
   display: block;
   margin: 4px;
   padding: 5px;
   min-height: 100px;
   border: 1px solid #eebb55;
   border-radius: 7pt;
   background: #ffeebb;
   } 
 </style> 
 


<script>
 $(function () {
 // Détection du support
if(typeof localStorage!='undefined') {
    alert("localStorage est supporté");
	var tab=new Array;
   tab[0]="Lundi";
   tab[1]="Mardi";
   tab[2]="Mercredi";
   tab[3]="Jeudi";
   tab[4]="Vendredi";
   tab[5]="Samedi";
   tab[6]="Dimanche";
   localStorage["objectSelect"] = JSON.stringify(tab);
   
	
	
	
} else {
  alert("localStorage n'est pas supporté");
}
 
})

</script>

<script>
${javascript}
</script>


  </head>
  <body>
 <div id='main'>
 <test>valeur du champ</test>
<div class="container-fluid"  style="background-color:#eee;border: 1px solid #888;border-radius:3px;">
${templatingProperties.getPreserveSectionFrontBegin()}${templatingProperties.getDelimiterTemplateNameBeginInPreserveSection()}${templateName}${templatingProperties.getDelimiterTemplateNameEndInPreserveSection()
}
<#if traitementPreserveSection??>
 ${traitementPreserveSection.getSectionContent(templateName)}
 ${templatingProperties.getPreserveSectionFrontEnd()}
 <#else>
 ${commonObject.getPreserveSection(templatingProperties.getPreserveCodeIhm(),"").getSectionContent(templateName)}
 ${templatingProperties.getPreserveSectionFrontEnd()}
</#if>  




