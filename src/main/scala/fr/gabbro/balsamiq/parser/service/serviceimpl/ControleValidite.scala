package fr.gabbro.balsamiq.parser.service.serviceimpl
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
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._
import fr.gabbro.balsamiq.parser.modelimpl.GlobalContext
import fr.gabbro.balsamiq.parser.service.TControleValidite
import fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase
import fr.gabbro.balsamiq.parser.modelimpl.ItemVar

/**
 * *** verification de la validité de la position des widgets ***
 * @author fra9972467
 *
 */
class ControleValidite(catalog: ArrayBuffer[WidgetDeBase], traitementBinding: TraitementBinding, globalContext: GlobalContext) extends TControleValidite {
  def process: Boolean = {
    controle
    mise_en_table_items_vars(catalog, null)
    true

  }

  /**
   * <p>le 1er élement du catalogue contient le container principal (mainWIndow par exemple)</p>
   * <p>s'il y a plus d'une entrée => des widgets sont hors gabarit et ne seront pas traités</p>
   * <p>on vérifie que les widgets d'un formulaire sont bindés à une variable</p>
   * <p>détection des widgets d'une même branche ayant une intersection commune</p>
   * @return true or false
   */
  def controle: Boolean = {
    var erreur = false
    logBack.info(utilitaire.getContenuMessage("mes24"))
    // le 1ere element du catalogue contient le container principal (mainWIndow par exemple)
    // s'il y a plus d'une entrée => des widgets sont hors gabarit et ne seront pas traités

    if (catalog.size > 1) {
      logBack.error(utilitaire.getContenuMessage("mes22"))
      erreur = true
    } else {
      // on vérifie que les widgets d'un formulaire sont bindés à une variable
      verification_binding_variable(catalog(0).tableau_des_fils, catalog(0))
      // detection des widgets d'une même branche ayant une intersection commune
      verification_intersection_entre_widgets_branche(catalog(0).tableau_des_fils)
    }

    return !erreur
  }

  /**
   * detection des widgets d'une même branche ayant une intersection commune
   * @param branche : ArrayBuffer[WidgetDeBase]
   */
  private def verification_intersection_entre_widgets_branche(branche: ArrayBuffer[WidgetDeBase]) {

    for (i <- 0 until branche.size) {
      for (j <- i + 1 until branche.size) {
        branche(i).xAbsolute
        if (intersection(branche(i), branche(j))) { logBack.error(utilitaire.getContenuMessage("mes25"), branche(i).controlTypeID.toString(), branche(i).xAbsolute.toString(), branche(i).yAbsolute.toString(), branche(j).controlTypeID.toString(), branche(i).xAbsolute.toString(), branche(i).yAbsolute.toString()) }
      }
      if (branche(i).tableau_des_fils.size > 0) { verification_intersection_entre_widgets_branche(branche(i).tableau_des_fils) }

    }

  }

  /**
   * intersection of area between 2 widgets.
   * @param zone1 : widget1
   * @param zone2 : widget2
   * @return true or false
   */
  private def intersection(zone1: WidgetDeBase, zone2: WidgetDeBase): Boolean = {
    val hoverlap = (zone1.xRelative < (zone2.xRelative + zone2.w) && zone2.xRelative < (zone1.xRelative + zone1.w)) // horizontal
    val voverlap = (zone1.yRelative < (zone2.yRelative + zone2.h) && zone2.yRelative < (zone1.yRelative + zone1.h)) // vertical
    return hoverlap && voverlap
  }

  /**
   *  on traite branche par branche du catalogue afin de verifier que les widgets d'un container
   * sont bindés à une variable
   * Vérification que chaque formulaire a un id déclaré.
   * On vérifie qu'il n'existe pas plus d'un formulaire avec le même customId
   *
   * @param branche : table des widgetDeBase à traiter.
   * @param widgetPere
   */
  private def verification_binding_variable(branche: ArrayBuffer[WidgetDeBase], widgetPere: WidgetDeBase) {
    branche.foreach(controle => {
      // si le container Pere est un formulaire, on vérifie que chaque widget de type formulaire est bindé à une valeur
      if (widgetPere.isFormulaireHTML && CommonObjectForMockupProcess.engineProperties.widgetsEnablingContainerAsAForm.exists(x => (x == controle.controlTypeID || x == controle.componentName))) {
        val v1 = controle.mapExtendedAttribut.getOrElse(CommonObjectForMockupProcess.constants.variableBinding, "").toString()
        val v2 = controle.mapExtendedAttribut.getOrElse(CommonObjectForMockupProcess.constants.mapBinding, "").toString()
        if (v1 == "" && v2 == "") { logBack.error(utilitaire.getContenuMessage("mes23"), controle.controlTypeID.split("::").last) }
      }
      if (controle.isFormulaireHTML) {
        // id formulaire renseigné ? 
        if (controle.customId == "") { logBack.error(utilitaire.getContenuMessage("mes29"), controle.controlTypeID.split("::").last) }
        // plus de 1 formulaire avec le même Nom ? 
        else if (CommonObjectForMockupProcess.listeNomdesFormulaires.filter(_ == controle.customId).size > 1) { logBack.error(utilitaire.getContenuMessage("mes30"), controle.controlTypeID.split("::").last) }
      }
      if (controle.tableau_des_fils.size > 0) { verification_binding_variable(controle.tableau_des_fils, controle) }

    })
  }

  /**
   * on met aussi en table (mockupContext et globalContext) le contenu des variables itemsVar qui sera traite par les templates
   * @param branche
   * @param containerPere
   */
  def mise_en_table_items_vars(branche: ArrayBuffer[WidgetDeBase], containerPere: WidgetDeBase) {
    //   val listeDesengineProperties.widgetsEnablingContainerAsAForm = CommonObjectForMockupProcess .engineProperties.widgetsEnablingContainerAsAForm.split(",").map(_.trim).toList
    branche.foreach(controle => {
      val itemsVar = controle.mapExtendedAttribut.getOrElse(CommonObjectForMockupProcess.constants.itemsVar, "").toString()
      if (itemsVar != "" && !CommonObjectForMockupProcess.mockupContext.itemsVars.exists { item => item.content == itemsVar }) { CommonObjectForMockupProcess.mockupContext.itemsVars.add(new ItemVar(itemsVar, itemsVar.toUpperCase())) }
      if (itemsVar != "" && !globalContext.itemsVars.exists { item => item._2.content == itemsVar }) {
        val itemVar = new ItemVar(itemsVar, itemsVar.toUpperCase())
        if (CommonObjectForMockupProcess.isAfragment) { globalContext.itemsVars += (CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement, CommonObjectForMockupProcess.ecranContenantLeSegment, CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, itemVar.content) -> itemVar }
        else { globalContext.itemsVars += (CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, "", itemVar.content) -> itemVar }

      }
      // traitement itératif des fils
      if (controle.tableau_des_fils.size > 0) { mise_en_table_items_vars(controle.tableau_des_fils, controle) }
    })
  }

} // fin de la classe 


