package fr.gabbro.balsamiq.parser.service

import org.mozilla.javascript.BeanProperty
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

class EngineProperties {
  @BeanProperty var boostrapNumberOfColumns = 12 // nombre de colonne dans bootsrap
  @BeanProperty var buttonWidgetsList = List[String]() // list des composants de type bouton
  @BeanProperty var bypassGenerationTemplateForChildren = List[String]() // list des composants pour lesquels le moteur ne génère pas les fils
  @BeanProperty var codePageCaractereHexaBalsamiq = "" // code page dse caracteres hexa dans le fichier bmml
  @BeanProperty var fragmentSeparator = "" // separateur de fragments dans le nom du fichier
  @BeanProperty var fragmentTypeSeparator = "" // type de fragments dans le nom du fragment
  @BeanProperty var freemarkerVariablePrefix = ""
  @BeanProperty var messagesFile = "" // fichier des messages
  @BeanProperty var projectionOfHeightInPx = 1200 // hauteur en pixels matériel cible
  @BeanProperty var projectionOfWidthInPx = 1200 // largeur en pixels matériel cible
  @BeanProperty var usecaseSeparator = "" // separateur de usecase dans le nom du fichier
  @BeanProperty var viewportPxFontSizeInBalsamiqMockup = "" // average fontsize of widget in balsamiq 
  @BeanProperty var widgetsEnablingContainerAsAForm = List[String]() // liste des composants de type formulaire
  @BeanProperty var widgetsAcceptingIcon = List[String]() // liste des composants de acceptant un widget type icon
  
  /**
   * load engine properties
 * @param propsMap:Map[String, String]
 */
def loadProperties(propsMap: Map[String, String]): Unit = {
    boostrapNumberOfColumns = propsMap.getOrElse("config.engine.boostrapNumberOfColumns", "12").toInt
    widgetsEnablingContainerAsAForm = propsMap.getOrElse("config.engine.widgetsEnablingContainerAsAForm", "").split(",").toList.map(_.trim)
    buttonWidgetsList = propsMap.getOrElse("config.engine.buttonWidgetsList", "tb-btn").split(",").toList.map(_.trim)
    bypassGenerationTemplateForChildren = propsMap.getOrElse("config.engine.bypassGenerationTemplateForChildren", "dhtmlxgrid").split(",").toList.map(_.trim)
    freemarkerVariablePrefix = propsMap.getOrElse("config.engine.freemarkerVariablePrefix", "")
    viewportPxFontSizeInBalsamiqMockup = propsMap.getOrElse("config.engine.viewportPxFontSizeInBalsamiqMockup", "13")
    projectionOfHeightInPx = propsMap.getOrElse("config.engine.projectionOfHeightInPx", "1000").toInt
    projectionOfWidthInPx = propsMap.getOrElse("config.engine.projectionOfWidthInPx", "1000").toInt
    usecaseSeparator = propsMap.getOrElse("config.engine.usecaseSeparator", "-").trim.replace(".", "-")
    fragmentSeparator = propsMap.getOrElse("config.engine.fragmentSeparator", "$").trim
    fragmentTypeSeparator = propsMap.getOrElse("config.engine.fragmentTypeSeparator", "§").trim
    codePageCaractereHexaBalsamiq = propsMap.getOrElse("config.engine.codePageCaractereHexaBalsamiq", "iso-8859-1").trim
    widgetsAcceptingIcon = propsMap.getOrElse("config.engine.widgetsAcceptingIcon", "").split(",").toList.map(_.trim)

  }

}