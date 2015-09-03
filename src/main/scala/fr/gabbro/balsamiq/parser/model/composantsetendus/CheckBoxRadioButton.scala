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

import org.slf4j.LoggerFactory
import org.jdom2.Element
import scala.beans.BeanProperty
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementBinding
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess
import fr.gabbro.balsamiq.parser.modelimpl.CatalogDesComposants
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
class CheckBoxOrRadioButton(@BeanProperty var text: String, @BeanProperty var state: String, @BeanProperty var id_interne: String)

class CheckBoxRadioButton(id_interne: Int, groupe_en_cours: WidgetDeBase, elementXML: Element, traitementBinding: TraitementBinding, catalogDesComposants: CatalogDesComposants, isAcomponent: Boolean) extends WidgetDeBase(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, isAcomponent) {

  /* (non-Javadoc)
 * @see fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase#enrichissementParametres(java.lang.String)
 * traitement particulier du checkboxGroup
 * on analyse le champ text qui contient plusieurs checkbox
 */
  override def enrichissementParametres(param1: String): (String, Object) = {
    val checkboxOrRadioButton =
      if (this.controlTypeID == cstCheckBoxGroup) { cstCheckboxShort }
      else if (this.controlTypeID == cstRadioButtonGroup) { cstRadio }
      else { cstUnknown }
    val parts: List[String] = this.mapExtendedAttribut.getOrElse(cstText, "").toString().split("\\n").toList
    val tableauDesCheckBox = new java.util.ArrayList[CheckBoxOrRadioButton]()

    var checkboxName: String = "????"
    var state: String = "????"
    var i: Int = 0

    parts.foreach(part => {
      i += 1
      if (part.contains("-[x") || part.contains("-(o)")) {
        checkboxName = part.substring(4);
        checkboxName = checkboxName.substring(0, checkboxName.indexOf("-"));
        state = cstDisabledSelected
      } else if (part.contains("[x") || part.contains("(o)")) {
        checkboxName = part.substring(3);
        state = cstSelected
      } else if (part.contains("-[") || part.contains("-( )") || part.contains("-(-)")) {
        checkboxName = part.substring(4);
        checkboxName = checkboxName.substring(0, checkboxName.indexOf("-"));
        state = cstDisabled
      } else if (part.contains("[") || part.contains("( )") || part.contains("(-)")) {
        checkboxName = part.substring(3);
        state = cstUp
      } else {
        // boxes = boxes + part + "<br>";
      }
      var id_interne_component = this.id_interne + cstCptString + i.toString
      val checkBoxOrRadioButton = new CheckBoxOrRadioButton(this.formatText(checkboxName), state, id_interne_component)
      tableauDesCheckBox.add(checkBoxOrRadioButton)

      checkboxName = "????"
      state = "????"

    })

    (cstItems, tableauDesCheckBox)

  }

}