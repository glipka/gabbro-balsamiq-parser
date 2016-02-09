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

//menu Item contenu Open,CTRL+O
//menu Item contenu Open Recent >
//menu Item contenu ---
//menu Item contenu o Option One
//menu Item contenu Option Two
//menu Item contenu =
//menu Item contenu x Toggle Item
//menu Item contenu -Disabled Item-
//menu Item contenu Exit,CTRL+Q
class ListItemMenu(
  @BeanProperty var libelle: String,
  @BeanProperty var url: String,
  @BeanProperty var disabled: Boolean,
  @BeanProperty var option: Boolean,
  @BeanProperty var separator: Boolean,
  @BeanProperty var checked: Boolean)

class MenuItem(id_interne: Int, groupe_en_cours: WidgetDeBase, elementXML: Element, traitementBinding: TraitementBinding, catalogDesComposants: CatalogDesComposants, isAcomponent: Boolean) extends WidgetDeBase(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, isAcomponent) {

  /* (non-Javadoc)
 * @see fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase#enrichissementParametres(java.lang.String)
 */
  override def enrichissementParametres(param: String): (String, Object) = {
    var items = new java.util.ArrayList[ListItemMenu]()
    val str1 = this.mapExtendedAttribut.getOrElse(cstText, "").toString().split("\n").toList
    val urls = this.mapExtendedAttribut.getOrElse(cstHrefs, "").toString().split(",").toList

    var numeroItemDansMenu = 0
    str1.foreach(item =>
      {
        val url = if (numeroItemDansMenu <  urls.size) { urls.get(numeroItemDansMenu) } else { "" }
        items.add(recupContent(item, url))
        numeroItemDansMenu += 1
      })

    (cstItems, items)
  }
  // ------------------------------------------------------
  // *** récupération du contenu de l'item de menu ***
  // -------------------------------------------------------
  def recupContent(item: String, url: String): ListItemMenu = {
    var libelle = ""

    var disabled = false
    var option = false
    var separator = false
    var checked = false
    if (item.startsWith("---") || item.startsWith("= ")) {
      separator = true
    } else if (item.startsWith("o ")) {
      option = true
      libelle = item.substring(2)
    } else if (item.startsWith("x ")) {
      checked = true
      libelle = item.substring(2)
    } else {
      libelle = item
    }
    new ListItemMenu(libelle, url, disabled, option, separator, checked)

  }
}