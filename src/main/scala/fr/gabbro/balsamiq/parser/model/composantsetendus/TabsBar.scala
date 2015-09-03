package fr.gabbro.balsamiq.parser.model.composantsetendus
// Gabbro - scala program to manipulate balsamiq sketches files an generate code with FreeMarker
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

import scala.collection.JavaConversions._
import scala.beans.BeanProperty
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import org.jdom2.Element
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementBinding
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess
import fr.gabbro.balsamiq.parser.modelimpl.CatalogDesComposants
import fr.gabbro.balsamiq.parser.modelimpl.Fragment
import fr.gabbro.balsamiq.parser.service.serviceimpl.IBalsamiqFreeMarker
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
class Href(@BeanProperty var tab: String, @BeanProperty var fragment: Fragment)
class TabsBar(id_interne: Int, groupe_en_cours: WidgetDeBase, elementXML: Element, traitementBinding: TraitementBinding, catalogDesComposants: CatalogDesComposants, isAcomponent: Boolean) extends WidgetDeBase(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, isAcomponent) {
  /* (non-Javadoc)
 * @see fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase#enrichissementParametres(java.lang.String)
 */
override def enrichissementParametres(param1: String): (String, Object) = {
    val horizontalVertical = if (this.controlTypeID == cstVerticalTabbar) cstVertical else cstHorizontal
    var tabs = new java.util.ArrayList[Href]()
    val tableauHref = this.mapExtendedAttribut.getOrElse(cstHrefs, "").toString.split(",")
    // on met en table le texte de  chaque tabulation
    val tab1 = if (horizontalVertical == cstVertical) { this.mapExtendedAttribut.getOrElse(cstText, "").toString().split("\\n").toList }
    else { this.mapExtendedAttribut.getOrElse(cstText, "").toString().split(",").toList }
    var indice = 0
    tab1.foreach(tab =>
      {
        val bookmark = if (indice < tableauHref.size) { tableauHref(indice).split(";").head.split("&").head }
        else { "#" }
        tabs.add(new Href(tab.trim, IBalsamiqFreeMarker.globalContext.createFragment(bookmark)))
        indice += 1
      })
    (cstTabs, tabs)
  }
}