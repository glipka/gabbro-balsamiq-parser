package fr.gabbro.balsamiq.parser.modelimpl
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

import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable._
import scala.collection.JavaConversions._
import org.jdom2.Element
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess
import fr.gabbro.balsamiq.parser.model.TComponentBalsamiq
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
// --------------------------------------------------------------------------------
// Pour un composant Balsamiq, on ne récupère que le nom du composant 
// --------------------------------------------------------------------------------
class ComponentBalsamiq(elementXML: Element) extends TComponentBalsamiq {
  var controlTypeID: String = elementXML.getAttributeValue(cstControlTypeID)
  if (controlTypeID == cstGroupConstante) {
    componentName = getComponentName(elementXML)
    if (componentName != "") {is_a_group = true}
    element = elementXML // sauvegarde du code XML pour récupération 
  }
  
  /**
   * récupération du nom du composant
   * @param e : Element
   * @return name of component
   */
  private def getComponentName(e: Element): String = {
    var name = ""
    if (e.getChildren().size() != 0) {
      val controlProperties = e.getChild(cstControlProperties);
      if (controlProperties != null) {
        val cp = controlProperties.getChildren().toList;
        cp.foreach(propertie => {
          val elementName = propertie.getName().trim
          val elementValue = utilitaire.remplaceHexa(propertie.getText().trim) // on remplace les %xy par leur valeur ascii
          if (elementName == cstControlName) { name = elementValue; }
        }) // fin de cp.foreach
      }
    } // fin de if 
    return name
  } // fin de getCOntrolProperties

}
 