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
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
class ListItem(@BeanProperty var content: String, @BeanProperty var enabled: Boolean)
class ListHTML(id_interne: Int, groupe_en_cours: WidgetDeBase, elementXML: Element, traitementBinding: TraitementBinding, catalogDesComposants: CatalogDesComposants, isAcomponent: Boolean) extends WidgetDeBase(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, isAcomponent) {

  /* (non-Javadoc)
 * @see fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase#enrichissementParametres(java.lang.String)
 */
override def enrichissementParametres(param: String): (String, Object) = {
    var items = new java.util.ArrayList[ListItem]()
    val str1 = this.mapExtendedAttribut.getOrElse(cstText, "").toString().split("\n").toList
    var nbre_lignes = 0
    str1.foreach(item =>
      {
        val item2 = item.trim
        var enabled=if(item2.startsWith("-") && item2.endsWith("-")) false else true
        items.add(new ListItem(item2, enabled))
        nbre_lignes += 1

      })
    (cstItems, items)
  }
}