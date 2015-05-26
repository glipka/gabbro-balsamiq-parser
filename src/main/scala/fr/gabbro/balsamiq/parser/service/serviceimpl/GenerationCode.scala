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
import fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase
import fr.gabbro.balsamiq.parser.service.TTraitementCommun

class ModuleGenerationCode(moteurTemplateFreeMarker: MoteurTemplatingFreeMarker) extends TTraitementCommun {
  /**
   * dans le catalogue chaque container est matérialisé par une branche.
   * un container peut être lui même contenu dans un container.
   * La 1ere branche du catalogue est le gabarit principal qui contient l'ensemble des wigets de l'écran
   * On extrait dans chaque branche les widgets triés  par n° de ligne et par n° de colonne.
   * On chaque changement de branche => on appelle la genération du template "Canvas".
   * A chaque changement de ligne => on appelle le template row
   * A chaque changement de colonne => on appelle le template cols, on gère l'offset avec la colonne précédente
   * Pour chaque widget, il y a 2 phases : la phase début et la phase de fin, un widget pouvant être lui même un conteneur
   * l'appel aux templates se fait récursivement afin de traiter correctement l'inclusion des widgets.
   * @param branche_catalog : ArrayBuffer[WidgetDeBase]
   * @param rowNumber : Int
   * @param niveau : Int
   * @param branche_pere : ArrayBuffer[WidgetDeBase]
   * @param container : WidgetDeBase
   * @param forceFormulaire boolean
   * @return  (sourceHtml, sourceJavascript, sourceJavaOuScala) : (StringBuilder, StringBuilder, StringBuilder)
   */
  def traitement_widget_par_ligne_colonne(branche_catalog: ArrayBuffer[WidgetDeBase], rowNumber: Int, niveau: Int, branche_perex: ArrayBuffer[WidgetDeBase], container: WidgetDeBase, forceFormulaire: Boolean): (StringBuilder, StringBuilder, StringBuilder) = {
    var sourceHtml: StringBuilder = new StringBuilder()
    var sourceJavascript: StringBuilder = new StringBuilder()
    var sourceJavaOuScala: StringBuilder = new StringBuilder()
    // on ne génere le conteneur que pour le 1er appel
    val containerName = if (container != null) { container.getWidgetNameOrComponentName().split("::").last } else {
      null
    }
    traitementPrincipal
    return (sourceHtml, sourceJavascript, sourceJavaOuScala)
    // ------------------------------------------------
    // *** Traitement des fils du widget en cours ***
    // ------------------------------------------------
    def traitementDesFilsDuWidgetDeLaColonneEnCours(widget: WidgetDeBase): Unit = {
      // *** traitement des fils si nécessaire ***
      if (widget.tableau_des_fils.size > 0) {
        if ((container != null) && (container.isFormulaireHTML)) {
          val (source20, javascript20, codeEcran20) = traitement_widget_par_ligne_colonne(widget.tableau_des_fils, 0, niveau + 4, branche_catalog, widget, true)
          sourceHtml = sourceHtml.append(source20)
          sourceJavascript = sourceJavascript.append(javascript20)
          sourceJavaOuScala = sourceJavaOuScala.append(codeEcran20)

        } else { // le container est nul
          val (source21, javascript21, codeEcran21) = traitement_widget_par_ligne_colonne(widget.tableau_des_fils, 0, niveau + 4, branche_catalog, widget, false)
          sourceHtml = sourceHtml.append(source21)
          sourceJavascript = sourceJavascript.append(javascript21)
          sourceJavaOuScala = sourceJavaOuScala.append(codeEcran21)

        }

      }
    }
    // -------------------------------------------------------
    // *** traitement des colonnes de la ligne de la table en cours ***
    // -------------------------------------------------------
    def traitementDesColonnesDeLaTable(brancheFiltreeParLigne: ArrayBuffer[WidgetDeBase], container: WidgetDeBase): Unit = {
      val brancheFiltreeParPositionWidget = brancheFiltreeParLigne.sortWith((x, y) => x.positionDansLeConteneur < y.positionDansLeConteneur)
      // *** appel du template pour générer le séparateur de colonne début *** 
      var numeroColonne = 0
      // on traite chaque widget par n° de position 
      brancheFiltreeParPositionWidget.foreach(widget => {
        // template colonne début
        val (ret7, source7, sourceJavaScript7, codeEcran7) = moteurTemplateFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateCol, CommonObjectForMockupProcess.templatingProperties.phase_debut, container, (CommonObjectForMockupProcess.constants.containerName, containerName), (CommonObjectForMockupProcess.constants.container, container), (CommonObjectForMockupProcess.constants.colNumber, numeroColonne.toString))
        sourceHtml = sourceHtml.append(source7)
        sourceJavascript = sourceJavascript.append(sourceJavaScript7)
        sourceJavaOuScala = sourceJavaOuScala.append(codeEcran7)
        // template widget debut
        val (ret8, source8, sourceJavaScript8, codeEcran8) = if ((container != null) && (container.isFormulaireHTML || forceFormulaire)) moteurTemplateFreeMarker.generationDuTemplate(widget, CommonObjectForMockupProcess.templatingProperties.phase_debut, container, (CommonObjectForMockupProcess.constants.containerIsForm, CommonObjectForMockupProcess.constants.trueString))
        else moteurTemplateFreeMarker.generationDuTemplate(widget, CommonObjectForMockupProcess.templatingProperties.phase_debut, container, (CommonObjectForMockupProcess.constants.container, container))
        sourceHtml = sourceHtml.append(source8)
        sourceJavascript = sourceJavascript.append(sourceJavaScript8)
        sourceJavaOuScala = sourceJavaOuScala.append(codeEcran8)
        traitementDesFilsDuWidgetDeLaColonneEnCours(widget)
        // appel template widget fin
        val (ret9, source9, sourceJavaScript9, codeEcran9) = moteurTemplateFreeMarker.generationDuTemplate(widget, CommonObjectForMockupProcess.templatingProperties.phase_fin, container, (CommonObjectForMockupProcess.constants.container, container))
        sourceHtml = sourceHtml.append(source9)
        sourceJavascript = sourceJavascript.append(sourceJavaScript9)
        sourceJavaOuScala = sourceJavaOuScala.append(codeEcran9)
        // template colonne fin
        val (ret10, source10, sourceJavaScript10, codeEcran10) = moteurTemplateFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateCol, CommonObjectForMockupProcess.templatingProperties.phase_fin, container, (CommonObjectForMockupProcess.constants.containerName, containerName), (CommonObjectForMockupProcess.constants.container, container), (CommonObjectForMockupProcess.constants.colNumber, numeroColonne.toString))
        sourceHtml = sourceHtml.append(source10)
        sourceJavascript = sourceJavascript.append(sourceJavaScript10)
        sourceJavaOuScala = sourceJavaOuScala.append(codeEcran10)
        numeroColonne += 1

      }) // fin de calcul du widget  
      // appel du templateCol fin 

    }
    // traitement des colonnes du container bootstrap (qui n'est pas une table)
    def traitementDesColonnesBootstrap(brancheFiltreeParLigne: ArrayBuffer[WidgetDeBase], container: WidgetDeBase): Unit = {
      // --------------------------------------------------------------------
      // Pour la ligne sélectionnée on balaie chaque colonne en douzieme 
      // Les colonnes vides sont gérées par offset.
      // -------------------------------------------------------------------- 
      var widgetPrecedent: WidgetDeBase = null
      var numeroColonneEnDouziemeDeLecran = 0
      var numeroColonne = 0
      val tailleCelluleEnDouzieme = calculTailleCelluleEnDouzieme(container)
      val demiTailleCelluleEnDouzieme = tailleCelluleEnDouzieme / 2
      var tailleEnCoursEnDouzieme = 0
      var colspan = 0
      while (numeroColonneEnDouziemeDeLecran < CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns) {
        tailleEnCoursEnDouzieme = 0
        val brancheFiltreeParColonneEnDouzieme = brancheFiltreeParLigne.filter(widget => widget.positionEnDouzieme == numeroColonneEnDouziemeDeLecran)
        // si la colonne en douzieme est vide on incrémente le colspan.
        val colN = brancheFiltreeParColonneEnDouzieme.size
        if (brancheFiltreeParColonneEnDouzieme.size <= 0) {
          colspan += 1
          numeroColonneEnDouziemeDeLecran += 1
        } else { // la colonne en douzieme peut contenir plusieurs widgets.
          // on calcule la taille de la cellule en douzieme à partir de la position du dernier widget de cette cellule
          tailleEnCoursEnDouzieme = (brancheFiltreeParColonneEnDouzieme.last.w) / tailleCelluleEnDouzieme
          // on s'aligne à la cellule suivante si necessaire
          val reste = brancheFiltreeParColonneEnDouzieme.last.w % tailleCelluleEnDouzieme
          if (reste >= (tailleCelluleEnDouzieme / 2)) { tailleEnCoursEnDouzieme += 1 }
          if (tailleEnCoursEnDouzieme == 0) { tailleEnCoursEnDouzieme = 1 }

          // on verifie que l'on est au maxi sur 12 cellules
          // car du fait des arrondis, on peut dépasser 12
          val tailleTotaleEnDouzieme = numeroColonneEnDouziemeDeLecran + tailleEnCoursEnDouzieme
          if (tailleTotaleEnDouzieme > CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns) {
            var r1 = tailleTotaleEnDouzieme - CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns
            if (colspan >= r1) colspan -= r1 else {
              r1 = r1 - colspan;
              colspan = 0;
              if (tailleEnCoursEnDouzieme > r1) { tailleEnCoursEnDouzieme -= r1 }
              else {
                tailleEnCoursEnDouzieme = 1; // taille au minimum
                logBack.info(utilitaire.getContenuMessage("mes39"), tailleEnCoursEnDouzieme)
              }
            }
          }
          tailleEnCoursEnDouzieme = ajustementTailleDeLaColonne(brancheFiltreeParLigne, numeroColonneEnDouziemeDeLecran, tailleEnCoursEnDouzieme)
          val (ret7, source7, sourceJavaScript7, codeEcran7) = moteurTemplateFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateCol, CommonObjectForMockupProcess.templatingProperties.phase_debut, container, (CommonObjectForMockupProcess.constants.bootstrapColWidth, tailleEnCoursEnDouzieme.toString), (CommonObjectForMockupProcess.constants.bootstrapColOffset, colspan.toString), (CommonObjectForMockupProcess.constants.containerName, containerName), (CommonObjectForMockupProcess.constants.colNumber, numeroColonne.toString), (CommonObjectForMockupProcess.constants.container, container))
          sourceHtml = sourceHtml.append(source7)
          sourceJavascript = sourceJavascript.append(sourceJavaScript7)
          sourceJavaOuScala = sourceJavaOuScala.append(codeEcran7)
          colspan = 0 // on réinitalise le colspan après avoir généré md_offset
          // on traite chaque widget dans le div
          var positionWidget = 0
          brancheFiltreeParColonneEnDouzieme.foreach(widget => {
            val (ret8, source8, sourceJavaScript8, codeEcran8) = if ((container != null) && (container.isFormulaireHTML || forceFormulaire)) moteurTemplateFreeMarker.generationDuTemplate(widget, CommonObjectForMockupProcess.templatingProperties.phase_debut, container, (CommonObjectForMockupProcess.constants.containerIsForm, CommonObjectForMockupProcess.constants.trueString))
            else moteurTemplateFreeMarker.generationDuTemplate(widget, CommonObjectForMockupProcess.templatingProperties.phase_debut, container, (CommonObjectForMockupProcess.constants.container, container))
            sourceHtml = sourceHtml.append(source8)
            sourceJavascript = sourceJavascript.append(sourceJavaScript8)
            sourceJavaOuScala = sourceJavaOuScala.append(codeEcran8)
            traitementDesFilsDuWidgetDeLaColonneEnCours(widget)
            val (ret9, source9, sourceJavaScript9, codeEcran9) = moteurTemplateFreeMarker.generationDuTemplate(widget, CommonObjectForMockupProcess.templatingProperties.phase_fin, container, (CommonObjectForMockupProcess.constants.container, container))
            sourceHtml = sourceHtml.append(source9)
            sourceJavascript = sourceJavascript.append(sourceJavaScript9)
            sourceJavaOuScala = sourceJavaOuScala.append(codeEcran9)
            widgetPrecedent = widget
            positionWidget += 1
          }) // fin de calcul du widget  
          // Appel template col fin
          val (ret10, source10, sourceJavaScript10, codeEcran10) = moteurTemplateFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateCol, CommonObjectForMockupProcess.templatingProperties.phase_fin, container, (CommonObjectForMockupProcess.constants.containerName, containerName), (CommonObjectForMockupProcess.constants.colNumber, numeroColonne.toString), (CommonObjectForMockupProcess.constants.container, container))
          sourceHtml = sourceHtml.append(source10)
          sourceJavascript = sourceJavascript.append(sourceJavaScript10)
          sourceJavaOuScala = sourceJavaOuScala.append(codeEcran10)

          numeroColonne += 1 // numero de colonne va servir pour la balise td

        } // fin de else1 
        numeroColonneEnDouziemeDeLecran += tailleEnCoursEnDouzieme // traitement colonne en douzieme suivante
      } // fin de while numeroColonneEnDouzieme < 12
    }
    // ------------------------------------------------------------------------------
    // Traitement des colonnes de la table 
    // ------------------------------------------------------------------------------
    def traitementPrincipal: Unit = {
      var etoile = "*" * niveau
      // traitement de la ligne en cours
      val brancheFiltreeParLigne = branche_catalog.filter(widget => widget.rowNumber == rowNumber)
      if (brancheFiltreeParLigne.size > 0) { // il y a des colonnes à traiter ?

        // ------------------------------------------------------------------------------------------
        // __________________________________________________________________________________________

        val (ret1, source1, sourceJavaScript1, codeEcran1) = if ((container != null) && (container.isFormulaireHTML)) { moteurTemplateFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateRow, CommonObjectForMockupProcess.templatingProperties.phase_debut, container, (CommonObjectForMockupProcess.constants.containerIsForm, CommonObjectForMockupProcess.constants.trueString), (CommonObjectForMockupProcess.constants.containerName, containerName), (CommonObjectForMockupProcess.constants.container, container)) }
        else { moteurTemplateFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateRow, CommonObjectForMockupProcess.templatingProperties.phase_debut, container, (CommonObjectForMockupProcess.constants.containerName, containerName), (CommonObjectForMockupProcess.constants.container, container)) }
        sourceHtml = sourceHtml.append(source1)
        sourceJavascript = sourceJavascript.append(sourceJavaScript1)
        sourceJavaOuScala = sourceJavaOuScala.append(codeEcran1)

        // est-on dans le traitement d'une table ? le container est dans la liste des widget de type datatable ?
        if ((container != null) && (CommonObjectForMockupProcess.generationProperties.listDataTableWidget.exists(x => x.equalsIgnoreCase(container.getShortWidgetName)))) { // on est dans le traitement d'une table => on extrait les widgets 
          traitementDesColonnesDeLaTable(brancheFiltreeParLigne, container)
        } else { // on n'est pas dans le traitement d'une table => on extrait les widgets par cellule bootstrap
          traitementDesColonnesBootstrap(brancheFiltreeParLigne, container)
        }

        val (ret10, source10, sourceJavaScript10, codeEcran10) = if (container != null && container.isFormulaireHTML)
          moteurTemplateFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateRow, CommonObjectForMockupProcess.templatingProperties.phase_fin, container, (CommonObjectForMockupProcess.constants.containerName, containerName), (CommonObjectForMockupProcess.constants.container, container), (CommonObjectForMockupProcess.constants.containerIsForm, CommonObjectForMockupProcess.constants.trueString))
        else moteurTemplateFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateRow, CommonObjectForMockupProcess.templatingProperties.phase_fin, container, (CommonObjectForMockupProcess.constants.containerName, containerName), (CommonObjectForMockupProcess.constants.container, container))
        sourceHtml = sourceHtml.append(source10)
        sourceJavascript = sourceJavascript.append(sourceJavaScript10)
        sourceJavaOuScala = sourceJavaOuScala.append(codeEcran10)

        val (source30, javaScript30, codeEcran30) = traitement_widget_par_ligne_colonne(branche_catalog, rowNumber + 1, niveau, branche_catalog, container, forceFormulaire) // appel traitement ligne suivante
        sourceHtml = sourceHtml.append(source30)
        sourceJavascript = sourceJavascript.append(javaScript30)
        sourceJavaOuScala = sourceJavaOuScala.append(codeEcran30)

      }
    } // fin de la fonction traitement principal
    return (sourceHtml, sourceJavascript, sourceJavaOuScala)
  } // fin de la fonction balayage_catalog

  /**
   * le calcul de la taile de la cellule se fait par rapport à la taille du conteneur
   * la taille de cellule est en pixel
   * @param widget : widgetDeBase
   * @return : taille cellule en douzieme
   */
  def calculTailleCelluleEnDouzieme(widget: WidgetDeBase): Int = {
    var tailleCelluleEnDouzieme = if (widget != null) widget.w / CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns else (CommonObjectForMockupProcess.mockupContext.global_max_width / CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns).toInt
    if (tailleCelluleEnDouzieme < CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns) tailleCelluleEnDouzieme = CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns
    tailleCelluleEnDouzieme
  }
  /**
   * Terminologie : un colonne = un ensemble de cellules en 12eme bootstrap.
   * recadrage de la taille pour eviter de ne pas traiter des widgets
   * En effet, la taille des cellule en12Eme se fait par arrondi.
   *
   * si la taille de la cellule en 12 =1 => on est certain de ne rien perdre.
   * On se positionne sur les différentes cellules comprises entre la la cellule en cours et la cellule en cours+taille de la colonne
   * S'il y a des widgets inclus dans cet interval, on réactualise le nombre de cellules de la colonne afin de n'y inclure aucun widget.
   * (on prend la 1er celulle de la colonne ne comprenant pas de widgets.
   * @param brancheFiltreeParLigne : ArrayBuffer[WidgetDeBase]
   * @param numeroDeColonne : Int
   * @param tailleDeLaColonne : Int
   * @return tailleColonne:Int
   */
  def ajustementTailleDeLaColonne(brancheFiltreeParLigne: ArrayBuffer[WidgetDeBase], numeroDeColonne: Int, tailleDeLaColonne: Int): Int = {
    val branche = brancheFiltreeParLigne.filter(widget => widget.positionEnDouzieme == numeroDeColonne + tailleDeLaColonne)

    if (tailleDeLaColonne <= 1) { tailleDeLaColonne }
    else {

      // on verifie s'il y a des widgets entre la colonne en cours+1 et 
      val branche = brancheFiltreeParLigne.filter(widget => {
        ((widget.positionEnDouzieme > numeroDeColonne) && (widget.positionEnDouzieme < numeroDeColonne + tailleDeLaColonne))
      })
      if (branche.size > 0) {
        val position_prochaine_colonne = branche.head.positionEnDouzieme
        return position_prochaine_colonne - numeroDeColonne
      } else { tailleDeLaColonne }

    }
  }

}