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

import org.jdom2.Element
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import scala.beans.BeanProperty
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementBinding
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess
import fr.gabbro.balsamiq.parser.modelimpl.CatalogDesComposants
import fr.gabbro.balsamiq.parser.service.serviceimpl.IBalsamiqFreeMarker
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
class Adresse(@BeanProperty var href: java.lang.String,
  @BeanProperty var libelle: java.lang.String, @BeanProperty var id: java.lang.String)

class BreadCrumbsOrButtonBarEnrichissementParametres(id_interne: Int, groupe_en_cours: WidgetDeBase, elementXML: Element, traitementBinding: TraitementBinding, catalogDesComposants: CatalogDesComposants, isAcomponent: Boolean) extends WidgetDeBase(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, isAcomponent) {

  /* (non-Javadoc)
   * l'attribut text contient les libelles des urls séparés par "," 
   * l'attribut hrefs ccntient les uris séparés par "," 
   * constitution de la table des urls à l'aide des attributs text et hrefs
   * @see fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase#enrichissementParametres(java.lang.String)
 */
  override def enrichissementParametres(param: String): (String, Object) = {
    val parts: List[String] = this.mapExtendedAttribut.getOrElse(cstText, "").toString().split("\\n").toList
    val hrefs: Array[String] = this.mapExtendedAttribut.getOrElse(cstHrefs, "").toString().split(",")
    val libelles = this.mapExtendedAttribut.getOrElse(cstText, "").toString().split(", ")
    val tableDesUrls = new java.util.ArrayList[Href]();

    var i = 0
    var uri: String = ""
    libelles.foreach(libelle => {
      try {
        if (!hrefs(i).contains("&")) { uri = hrefs(i) } // http://
        else { uri = hrefs(i).substring(0, hrefs(i).indexOf("&")) }
      } catch {
        case ex: Exception => uri = null
      }
      tableDesUrls.add(new Href(libelle.trim, IBalsamiqFreeMarker.globalContext.createFragment(uri)))
      i += 1
    })
    // on renseigne la table des urls qui sera traite comme une liste dans le template
    (cstUrls, tableDesUrls)

  }

}