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

import scala.beans.BeanProperty
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mutableMapAsJavaMap
import scala.collection.immutable.List
import scala.collection.mutable.ArrayBuffer
import org.jdom2.Element
import org.slf4j.LoggerFactory
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess
import fr.gabbro.balsamiq.parser.model.composantsetendus.DefaultWidget
import fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase
import fr.gabbro.balsamiq.parser.model.composantsetendus.TabsBar
import fr.gabbro.balsamiq.parser.model.composantsetendus.ListHTML
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementBinding
import fr.gabbro.balsamiq.parser.model.composantsetendus.RoundButton
import fr.gabbro.balsamiq.parser.model.composantsetendus.BreadCrumbsOrButtonBarEnrichissementParametres
import fr.gabbro.balsamiq.parser.model.composantsetendus.Datagrid
import fr.gabbro.balsamiq.parser.model.composantsetendus.CheckBoxRadioButton

// ---------------------------------------------------------------------------------------------------------------
//  controlID="6" 
// controlTypeID="com.balsamiq.mockups::Label" 
//  x="74" y="15" w="-1" h="-1" 
//    measuredW="157" 
//      measuredH="46" 
//        zOrder="0" 
//          locked="false" 
//            isInGroup="-1"
// cette classe permet de déterminer le type de widget en cours de traitement
// si le widget est un composant et est nommé dhtmlxgrid => instanciation du composant datagrid
// Sinon instanciation de l'objet en fonction de son nom.
// Remarque pour le moment, il n'y a pas de différenciation en terme d'objet entre un widget et un composant
// -------------------------------------------------------------------------------------------------------------

class InstanciationTypeDeWidget(val id_interne: Int, groupe_en_cours: WidgetDeBase, elementXML: Element, traitementBinding: TraitementBinding, catalogDesComposants: CatalogDesComposants) {
  val controlTypeID = elementXML.getAttributeValue(CommonObjectForMockupProcess.constants.controlTypeID)
  var componentName = ""
  //  var mapExtendedAttribut = scala.collection.mutable.Map[String, Object]()
  val utilitaire = new Utilitaire
  /**
   * <p>cette classe permet de déterminer le type de widget en cours de traitement</p>
   * <p>si le widget est un composant et est nommé dhtmlxgrid => instanciation du composant datagrid</p>
   * <p>Sinon instanciation de l'objet en fonction de son nom.</p>
   * <p>Remarque pour le moment, il n'y a pas de différenciation en terme d'objet entre un widget et un composant (classe WidgetDeBase)</p>
   * @return
   */
  def process(): WidgetDeBase = {
    controlTypeID match {
      // ----------------------------------------------------------------------------------------------------------------
      // src=./assets/bootstrap.bmml#tb-badge
      // traitement d'un composant : src contient le nom du compsant ainsi que 
      // le nom du repository.
      // ------------------------------------------------------------------------------------------------------------------
      case CommonObjectForMockupProcess.constants.componentBalsamiq => {
        val src = recuperationDesAttributsEtendus(elementXML).getOrElse(CommonObjectForMockupProcess.constants.src, "").toString()
        if (src.contains("#")) {
          val tab1 = src.split("#")
          componentName = tab1.last // nom du composant : tb_bagde
        }
        // rajouter à cet endroit pour des composants spécifiques
        if (componentName == CommonObjectForMockupProcess.constants.dhtmlxgrid) new Datagrid(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, true)
        else if (componentName == CommonObjectForMockupProcess.constants.customsteps) new TabsBar(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, true)
        else new DefaultWidget(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, true) {}

      }
      case CommonObjectForMockupProcess.constants.roundButton => new RoundButton(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, false) {}
      case CommonObjectForMockupProcess.constants.datagrid => new Datagrid(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, false) {}
      case CommonObjectForMockupProcess.constants.checkBoxGroup => new CheckBoxRadioButton(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, false) {}
      case CommonObjectForMockupProcess.constants.radioButtonGroup => new CheckBoxRadioButton(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, false) {}
      case CommonObjectForMockupProcess.constants.breadCrumbs => new BreadCrumbsOrButtonBarEnrichissementParametres(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, false) {}
      case CommonObjectForMockupProcess.constants.buttonBar => new BreadCrumbsOrButtonBarEnrichissementParametres(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, false) {}
      case CommonObjectForMockupProcess.constants.tabbar => new TabsBar(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, false) {}
      case CommonObjectForMockupProcess.constants.verticalTabbar => new TabsBar(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, false) {}
      case CommonObjectForMockupProcess.constants.listHTML => new ListHTML(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, false) {}
      case CommonObjectForMockupProcess.constants.comboBox => new ListHTML(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, false) {}

      case _ => new DefaultWidget(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, false) {}
    }

  }

  /**
   * @param e:Element
   * @return : Map[String, Object] Attributs étendus
   */
  private def recuperationDesAttributsEtendus(e: Element): scala.collection.mutable.Map[String, Object] = {
    var mapExtendedAttribut = scala.collection.mutable.Map[String, Object]()
    if (e.getChildren().size() != 0) {
      val controlProperties = e.getChild(CommonObjectForMockupProcess.constants.controlProperties);
      if (controlProperties != null) {
        val cp = controlProperties.getChildren().toList;
        cp.foreach(propertie => {
          val elementName = propertie.getName().trim
          var elementValue = utilitaire.remplaceHexa(propertie.getText().trim) // on remplace les %xy par leur valeur ascii
          mapExtendedAttribut += (elementName -> elementValue)
        }) // fin de cp.foreach
      }
    } // fin de if 
    mapExtendedAttribut
  } // fin de getCOntrolProperties

}

 