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

class TemplatingProperties {
  @BeanProperty var customProperty1 = "" // propriété custom1 
  @BeanProperty var customProperty2 = "" // propriété custom1 
  @BeanProperty var customProperty3 = "" // propriété custom1 
  @BeanProperty var freemarkerAutoImportFile = "" // auto import file
  @BeanProperty var freemarkerAutoImportNamespace = "" // auto import namespace
  @BeanProperty var freemarkerAutoIncludeFile = "" // auto include file
  @BeanProperty var freemarkerTemplatePropertiesFile = "" // emplacement fichier properties des templates
  @BeanProperty var freemarkerTemplatesDir = "" // repertoire des templates freemarker
  @BeanProperty var phase_debut = "" // suffix template debut
  @BeanProperty var phase_fin = "" // sufix template fin
  @BeanProperty var prefix_template_code = "code_" //prefix des templates code
  @BeanProperty var prefix_template_javascript = "js_" // prefix des templates javascript
  @BeanProperty var preserveSectionFrontBegin = "" // contenu preserve section debut pour le code HTML
  @BeanProperty var preserveSectionFrontEnd = "" // contenu preserve section fin pour le code HTML
  @BeanProperty var preserveSectionCodeBegin = "" // contenu preserve section debut pour le code HTML
  @BeanProperty var preserveSectionCodeEnd = "" // contenu preserve section fin pour le code HTML
  @BeanProperty var separator_template_file = "_" // separateur dans le nom du fichier template
  @BeanProperty var suffix_template_code = "" // suffix des templates code
  @BeanProperty var suffix_template_javascript = "" // suffix des templates javascript
  @BeanProperty var widgetsConsideredAsAForm = List[String]() // liste des widgets composants d'un formulaire
  @BeanProperty var widgetsListProcessedLocally = List[String]() // liste des composants à traiter localement (chaque widget est traité cas par cas)
  @BeanProperty var preserveCodeIhm = ""
  @BeanProperty var preserveCodeScript = ""
  @BeanProperty var preserveCodeJavaOrScala = ""

  /**
   * load template properties
   * @param propsMap
   */
  def loadProperties(propsMap: Map[String, String]): Unit = {
    freemarkerAutoImportFile = propsMap.getOrElse("config.templating.freemarkerAutoImportFile", "")
    freemarkerAutoIncludeFile = propsMap.getOrElse("config.templating.freemarkerAutoIncludeFile", "")
    freemarkerAutoImportNamespace = propsMap.getOrElse("config.templating.freemarkerAutoImportNamespace", "")
    widgetsListProcessedLocally = propsMap.getOrElse("config.templating.widgetsListProcessedLocally", "test2").split(",").toList.map(_.trim)
    widgetsConsideredAsAForm = propsMap.getOrElse("config.templating.widgetsConsideredAsAForm", "com.balsamiq.mockups::Canvas").split(",").toList.map(_.trim)
    // va servir pour les templates utilisant la position absolue et pour reprojeter les abcisses en fonction de l'écran cible
    customProperty1 = propsMap.getOrElse("config.templating.customProperty1", "")
    customProperty2 = propsMap.getOrElse("config.templating.customProperty2", "")
    customProperty3 = propsMap.getOrElse("config.templating.customProperty3", "")
    //     CommonObjectForMockupProcess .generatedJspOrHtmlDir = propsMap.getOrElse("config.generation.srcWebFilesDir", "html").trim.replace("%project%",  CommonObjectForMockupProcess .generationProperties.projectName)
    preserveSectionFrontBegin = propsMap.getOrElse("config.templating.preserveSectionFrontBegin", "<!--").trim // preserver Begin
    preserveSectionFrontEnd = propsMap.getOrElse("config.templating.preserveSectionFrontEnd", "-->").trim // preserver Begin
    preserveSectionCodeBegin = propsMap.getOrElse("config.templating.preserveSectionCodeBegin", "// begin-preserve").trim // preserver Begin
    preserveSectionCodeEnd = propsMap.getOrElse("config.templating.preserveSectionCodeEnd", "// end-preserve").trim // preserver Begin
    phase_debut = propsMap.getOrElse("config.templating.suffixTemplateBegin", "begin").trim // preserver Begin
    phase_fin = propsMap.getOrElse("config.templating.suffixTemplateEnd", "end").trim // preserver Begin
    prefix_template_javascript = propsMap.getOrElse("config.templating.prefixTemplateJavascript", "js_").trim // prefix des templates javascript
    suffix_template_javascript = propsMap.getOrElse("config.templating.suffixTemplateJavascript", "").trim // suffix des templates javascript
    prefix_template_code = propsMap.getOrElse("config.templating.prefixTemplateCode", "code_").trim //prefix des templates code
    suffix_template_code = propsMap.getOrElse("config.templating.suffixTemplateCode", "").trim // suffix des templates code
    separator_template_file = propsMap.getOrElse("config.templating.separatorTemplateFile", "_").trim // separateur dans le nom du fichier template
    preserveCodeIhm = propsMap.getOrElse("config.templating.preserveCodeIhm", "ihm").trim
    preserveCodeScript = propsMap.getOrElse("config.templating.preserveCodeScript", "javascript").trim
    preserveCodeJavaOrScala = propsMap.getOrElse("config.templating.preserveCodeJavaOrScala", "java").trim

  }
}