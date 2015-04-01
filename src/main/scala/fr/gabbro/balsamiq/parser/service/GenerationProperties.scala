package fr.gabbro.balsamiq.parser.service

import scala.beans.BeanProperty
// IbalsamiqFreeMarker - scala program to manipulate balsamiq sketches files an generate code with FreeMarker
// Version 1.0
// Copyright (C) 2014 Georges Lipka
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of either one of the following licences:
//
// 1. The Eclipse Public License (EPL) version 1.0,
//   available at http://www.eclipse.org/legal/epl-v10.html
//
// 2. The GNU Lesser General Public License (LGPL) version 2.1 or later,
//    available at http://www.gnu.org/licenses/lgpl.txt
//
// This program is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the individual licence texts for more details.

class GenerationProperties {
  @BeanProperty var generatedFrontFilesSuffix = "" // jsp ou html 
  @BeanProperty var attributesToProcessI18n = List[String]() // attributes du tag html qui sera internationalisé
  @BeanProperty var balsamiqAssetDir: String = "" // répertoire source des composants
  @BeanProperty var balsamiqMockupsDir = "" // répertoire source des mockups
  @BeanProperty var concatenateContainerIdToWidgetId = true // ajout automatique to nom du formulaire dans le binding
  @BeanProperty var configProperties = "" // localisation du fichier properties
  @BeanProperty var generateControllerForPrefix = "" // prefix des écrans pour lequel il faut générer le controleur
  @BeanProperty var generateLabelForAttributeForTheseWidgets = List[String]() // liste des widgets pour lesquels on va générer l'attribut labelfor
  @BeanProperty var generatedControllerAlias = "" // alias du controleur
  @BeanProperty var generatedDtoAlias = "" // alias dto
  @BeanProperty var generatedFormAlias = "" // alias du formulaire
  @BeanProperty var generatedOtherAlias = "" // alias other
  @BeanProperty var generatedsubPackage1 = "" // sous package1 
  @BeanProperty var generatedsubPackage2 = "" // sous package2
  @BeanProperty var generatedsubPackage3 = "" // sous package3 
  @BeanProperty var generatedProjectDir = "" // répertoire du projet généré
  @BeanProperty var generatedi18nFileName = "" // nom du fichier généré
  @BeanProperty var generatedSuffixCodeFileName = "" // suffix des fichiers generes par les template code
  @BeanProperty var globalExecutionFilePath1 = "" // localistion du fichier généré par le template global
  @BeanProperty var globalExecutionFilePath2 = "" // localistion du fichier généré par le template global
  @BeanProperty var globalExecutionFilePath3 = "" // localistion du fichier généré par le template global
  @BeanProperty var globalExecutionTemplate1 = "" // nom du template à exécuter après le traitment de tous les mockups
  @BeanProperty var globalExecutionTemplate2 = "" // nom du template à exécuter après le traitment de tous les mockups
  @BeanProperty var globalExecutionTemplate3 = "" // nom du template à exécuter après le traitment de tous les mockups
  @BeanProperty var htmlContainerListForI18nGeneration = List[String]() // tag html pouvant être un container
  @BeanProperty var i18nLocales = List[String]() // liste des langues à générer lors de l'internationalisation
  @BeanProperty var languageSource = "" // language java ou scala
  @BeanProperty var localExecutionFilePath1 = "" // localistion du fichier généré par le template local
  @BeanProperty var localExecutionFilePath2 = "" // localistion du fichier généré par le template local
  @BeanProperty var localExecutionFilePath3 = "" // localistion du fichier généré par le template local
  @BeanProperty var localExecutionTemplate1 = "" // nom du template a exécuter après le traitement du mockup en cours
  @BeanProperty var localExecutionTemplate2 = "" // nom du template a exécuter après le traitement du mockup en cours
  @BeanProperty var localExecutionTemplate3 = "" // nom du template a exécuter après le traitement du mockup en cours
  @BeanProperty var processI18nInFiles = "" // internationalisation du fichier html ou jps = true ou false
  @BeanProperty var processI18nInScriptSection = "" // process i18N in script tag of html files
  @BeanProperty var projectName = "" // nom du projet
  @BeanProperty var srcBuildPathDir = "" // src/main/java
  @BeanProperty var srcDtoFilesDir = "" // sous répertoire des dto : com/auchan/%project%/web
  @BeanProperty var srcDtoFilesFullPath = "" // full path du répertoire des DTOs
  @BeanProperty var srcI18nFilesDir = "" // 
  @BeanProperty var srcJavascriptFilesDir = "" //répertoire des sources javascripts
  @BeanProperty var srcJavascriptFilesDirWithOutPrefix = "" //répertoire des sources javascripts sans prefix du projet
  @BeanProperty var srcWebFilesDir = "" // repertoire des fichiers html    
  // 
  /**
   * load generation properties
   * @param propsMap
   */
  def loadProperties(propsMap: Map[String, String]): Unit = {
    concatenateContainerIdToWidgetId = if (propsMap.getOrElse("config.generation.concatenateContainerIdToWidgetId", "true").trim == "true") { true } else { false }
    generatedi18nFileName = propsMap.getOrElse("config.generation.generatedi18nFileName", "dictionnaire_fr.properties")
    languageSource = propsMap.getOrElse("config.generation.languageSource", "scala")
    generatedFrontFilesSuffix = propsMap.getOrElse("config.generation.generatedFrontFilesSuffix", "html")
    generateLabelForAttributeForTheseWidgets = propsMap.getOrElse("config.generation.generateLabelForAttributeForTheseWidgets", "com.balsamiq.mockups::Label").split(",").toList.map(_.trim)
    htmlContainerListForI18nGeneration = propsMap.getOrElse("config.generation.htmlContainerListForI18nGeneration", "form").split(",").toList.map(_.trim)
    generatedFormAlias = propsMap.getOrElse("config.generation.generatedFormAlias", "").trim
    generatedDtoAlias = propsMap.getOrElse("config.generation.generatedDtoAlias", "").trim
    generatedControllerAlias = propsMap.getOrElse("config.generation.generatedControllerAlias", "").trim
    generateControllerForPrefix = propsMap.getOrElse("config.generation.generateControllerForPrefix", "ec").trim
    generatedOtherAlias = propsMap.getOrElse("config.generation.generatedOtherAlias", "").trim
    generatedsubPackage1 = propsMap.getOrElse("config.generation.generatedsubPackage1", "").trim
    generatedsubPackage2 = propsMap.getOrElse("config.generation.generatedsubPackage2", "").trim
    generatedsubPackage3 = propsMap.getOrElse("config.generation.generatedsubPackage3", "").trim
    processI18nInFiles = propsMap.getOrElse("config.generation.processI18nInFiles", "false").trim
    generatedSuffixCodeFileName = propsMap.getOrElse("config.generation.generatedSuffixCodeFileName", "_code").trim
    srcBuildPathDir = propsMap.getOrElse("config.generation.srcBuildPathDir", "").trim.replace("%project%", projectName)
    srcDtoFilesDir = propsMap.getOrElse("config.generation.srcDtoFilesDir", "").trim.replace("%project%", projectName)
    srcJavascriptFilesDir = generatedProjectDir + System.getProperty("file.separator") + propsMap.getOrElse("config.generation.srcJavascriptFilesDir", "js").trim.replace("%project%", projectName)
    srcJavascriptFilesDirWithOutPrefix = propsMap.getOrElse("config.generation.srcJavascriptFilesDir", "js").trim.replace("%project%", projectName)
    srcWebFilesDir = generatedProjectDir + System.getProperty("file.separator") + propsMap.getOrElse("config.generation.srcWebFilesDir", "html").trim.replace("%project%", projectName)
    srcI18nFilesDir = generatedProjectDir + System.getProperty("file.separator") + propsMap.getOrElse("config.generation.srcI18nFilesDir", "resources").trim.replace("%project%", projectName)
    globalExecutionTemplate1 = propsMap.getOrElse("config.generation.globalExecutionTemplate1", "").trim // execute this global template after generation of all screens
    globalExecutionFilePath1 = propsMap.getOrElse("config.generation.globalExecutionFilePath1", "").trim.replace("%project%", projectName).replace("%controller%", generatedControllerAlias).replace("%controller?capitalize%", generatedControllerAlias.capitalize)
    globalExecutionTemplate2 = propsMap.getOrElse("config.generation.globalExecutionTemplate2", "").trim // execute this global template after generation of all screens
    globalExecutionFilePath2 = propsMap.getOrElse("config.generation.globalExecutionFilePath2", "").trim.replace("%project%", projectName).replace("%controller%", generatedControllerAlias).replace("%controller?capitalize%", generatedControllerAlias.capitalize)
    globalExecutionTemplate3 = propsMap.getOrElse("config.generation.globalExecutionTemplate3", "").trim // execute this global template after generation of all screens
    globalExecutionFilePath3 = propsMap.getOrElse("config.generation.globalExecutionFilePath3", "").trim.replace("%project%", projectName).replace("%controller%", generatedControllerAlias).replace("%controller?capitalize%", generatedControllerAlias.capitalize)
    localExecutionTemplate1 = propsMap.getOrElse("config.generation.localExecutionTemplate1", "").trim // execute this global template after generation of all screens
    localExecutionFilePath1 = propsMap.getOrElse("config.generation.localExecutionFilePath1", "").trim.replace("%project%", projectName).replace("%controller%", generatedControllerAlias).replace("%controller?capitalize%", generatedControllerAlias.capitalize)
    localExecutionTemplate2 = propsMap.getOrElse("config.generation.localExecutionTemplate2", "").trim // execute this global template after generation of all screens
    localExecutionFilePath2 = propsMap.getOrElse("config.generation.localExecutionFilePath2", "").trim.replace("%project%", projectName).replace("%controller%", generatedControllerAlias).replace("%controller?capitalize%", generatedControllerAlias.capitalize)
    localExecutionTemplate3 = propsMap.getOrElse("config.generation.localExecutionTemplate3", "").trim // execute this global template after generation of all screens
    localExecutionFilePath3 = propsMap.getOrElse("config.generation.localExecutionFilePath3", "").trim.replace("%project%", projectName).replace("%controller%", generatedControllerAlias).replace("%controller?capitalize%", generatedControllerAlias.capitalize)
    attributesToProcessI18n = propsMap.getOrElse("config.generation.attributesToProcessI18n", "").split(",").toList.map(_.trim)
    processI18nInScriptSection = propsMap.getOrElse("config.generation.processI18nInScriptSection", "false")
    i18nLocales = propsMap.getOrElse("config.generation.i18nLocales", "").split(",").toList.map(_.trim)
    srcDtoFilesFullPath = generatedProjectDir + System.getProperty("file.separator") + srcBuildPathDir + System.getProperty("file.separator") + propsMap.getOrElse("config.generation.srcDtoFilesDir", "").trim.replace("%project%", projectName)

  }

}