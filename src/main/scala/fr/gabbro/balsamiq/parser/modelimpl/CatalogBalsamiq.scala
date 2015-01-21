package fr.gabbro.balsamiq.parser.modelimpl

import scala.collection.mutable._
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementBinding
import fr.gabbro.balsamiq.parser.model.TCatalogBalsamiq
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess
import fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase
import fr.gabbro.balsamiq.parser.model.composantsetendus.IconInWidget
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

class CatalogBalsamiq(traitementBinding: TraitementBinding) extends TCatalogBalsamiq {
  // ----------------------------------------------------------------------
  // **** creation et enrichissemnt du catalogue ****
  // ----------------------------------------------------------------------
  def creation_catalog(catalogAPlat: ArrayBuffer[WidgetDeBase]): Unit = {
    catalog = constitutionDuCatalog(catalogAPlat)
    catalog = enrichissement_widget_branche(catalog, null)
    logBack.info(utilitaire.getContenuMessage("mes28"),CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement)
    impression_catalog(catalog)
  }
  // ---------------------------------------------------------------------------------------------------------------------------------
  // Appel récursif de cette procédure branche par branche.
  // pour chaque widget, on recalcule, son abscisse et ordonnée relative par rapport au père, 
  // sa hauteur et largeur par rapport au père (en poucentage) ainsi que l'espace haut, bas, droit, gauche par rapport au pere. 
  // ---------------------------------------------------------------------------------------------------------------------------------
  def enrichissement_widget_branche(branche: ArrayBuffer[WidgetDeBase], widgetPere: WidgetDeBase): ArrayBuffer[WidgetDeBase] = {
    var positionDansLeConteneur = 0
    val tableau = branche.map(widget => {
      if (CommonObjectForMockupProcess.mockupContext.global_max_width > 0) widget.percentageparRapportLargeurTotale = Math.ceil((widget.w.toDouble / CommonObjectForMockupProcess.mockupContext.global_max_width) * 100.0)
      if (CommonObjectForMockupProcess.mockupContext.global_max_height > 0) widget.percentageparRapportHauteurTotale = Math.ceil((widget.h.toDouble / CommonObjectForMockupProcess.mockupContext.global_max_height) * 100.0)
      // on recalcule les ratios des coordonnées et hauteur et largeur en fonction d'une dimension cible.
      // cela va servir essenteillement aux templates utilisant les coordonnées absolues. 
      if (CommonObjectForMockupProcess.engineProperties.projectionOfWidthInPx > 0 && CommonObjectForMockupProcess.engineProperties.projectionOfHeightInPx > 0) {
        val ratioW = (CommonObjectForMockupProcess.engineProperties.projectionOfWidthInPx / CommonObjectForMockupProcess.mockupContext.global_max_width).toInt
        val ratioH = (CommonObjectForMockupProcess.engineProperties.projectionOfHeightInPx / CommonObjectForMockupProcess.mockupContext.global_max_height).toInt
        widget.projectionW = widget.w * ratioW
        widget.projectionH = widget.h * ratioH
        widget.projectionX = widget.xAbsolute * ratioW
        widget.projectionY = widget.yAbsolute * ratioH
      }
      if (widgetPere != null) {
        if (widgetPere.w > 0) widget.percentageLargeurParRapportPere = Math.ceil((widget.w.toDouble / widgetPere.w.toDouble) * 100.0)
        if (widgetPere.h > 0) widget.percentageHauteurparRapportPere = Math.ceil((widget.h.toDouble / widgetPere.h) * 100.0)
        widget.percentageBandeauHautParRapportPere = Math.ceil(((widget.yAbsolute.toDouble - widgetPere.yAbsolute.toDouble) / widgetPere.h.toDouble) * 100.0)
        widget.percentageBandeauBasParRapportPere = Math.ceil((((widgetPere.yAbsolute.toDouble + widgetPere.h.toDouble) - (widget.yAbsolute.toDouble + widget.h.toDouble)) / widgetPere.h.toDouble) * 100.0)
        widget.percentageBandeauDroiteParRapportPere = Math.ceil((((widgetPere.xAbsolute.toDouble + widgetPere.w.toDouble) - (widget.xAbsolute.toDouble + widget.w.toDouble)) / widgetPere.w.toDouble) * 100.0)
        widget.percentageBandeauGaucheParRapportPere = Math.ceil(((widget.xAbsolute.toDouble - widgetPere.xAbsolute.toDouble) / widgetPere.w.toDouble) * 100.0)
        widget.xRelative = math.abs(widgetPere.xAbsolute - widget.xAbsolute)
        widget.yRelative = math.abs(widgetPere.yAbsolute - widget.yAbsolute)
        widget.positionDansLeConteneur = positionDansLeConteneur

      } else { // il n'y a pas de container père
        widget.percentageLargeurParRapportPere = Math.ceil((widget.w.toDouble / CommonObjectForMockupProcess.mockupContext.global_max_width.toDouble) * 100.0)
        widget.percentageHauteurparRapportPere = Math.ceil((widget.h.toDouble / CommonObjectForMockupProcess.mockupContext.global_max_height) * 100.0)
        widget.percentageBandeauHautParRapportPere = Math.ceil(((widget.yAbsolute.toDouble) / CommonObjectForMockupProcess.mockupContext.global_max_height.toDouble) * 100.0)
        widget.percentageBandeauBasParRapportPere = Math.ceil((((CommonObjectForMockupProcess.mockupContext.global_max_height) - (widget.yAbsolute.toDouble + widget.h.toDouble)) / CommonObjectForMockupProcess.mockupContext.global_max_height.toDouble) * 100.0)
        widget.percentageBandeauDroiteParRapportPere = Math.ceil((((CommonObjectForMockupProcess.mockupContext.global_max_width.toDouble) - (widget.xAbsolute.toDouble + widget.w.toDouble)) / CommonObjectForMockupProcess.mockupContext.global_max_width.toDouble) * 100.0)
        widget.percentageBandeauGaucheParRapportPere = Math.ceil(((widget.xAbsolute.toDouble) / CommonObjectForMockupProcess.mockupContext.global_max_width.toDouble) * 100.0)
        widget.xRelative = widget.xAbsolute
        widget.yRelative = widget.yAbsolute
        widget.positionDansLeConteneur = positionDansLeConteneur

      }
      positionDansLeConteneur = positionDansLeConteneur + 1

      // si le widget Pere a un fils qui est dans la table des widgets des formulaires, il est donc un formulaire
      // les widgets de type radiobuttonGroup induisent par defaut un formlaire (ils n'ont pas de fils
      // il faudra récupérer l'action d'un bouton et la stokcer dans mapExtendedAttribut
      // seuls les container de type canvas peuvent contenir un formulaire

      if ((List(widget.getWidgetNameOrComponentName()).intersect(CommonObjectForMockupProcess.templatingProperties.widgetsConsideredAsAForm).size > 0) &&
        (widget.tableau_des_fils.exists(widgetFils => CommonObjectForMockupProcess.engineProperties.widgetsEnablingContainerAsAForm.exists(x => (x == widgetFils.getWidgetNameOrComponentName()))))) {
        widget.isFormulaireHTML = true
        //   widget.mapExtendedAttribut += ("containerIsAformulaire" -> "true") // variable utilsée dans les templates
        var actionDuFormulaire = ListBuffer[String]()
        widget.tableau_des_fils.foreach(widgetFils => {
          // on verifie si le widget est un bouton et on recupre le champ hrefs
          if (CommonObjectForMockupProcess.engineProperties.buttonWidgetsList.exists(bouton => (bouton == widgetFils.getWidgetNameOrComponentName()))) {
            val hrefs = utilitaire.remplaceHexa(widgetFils.mapExtendedAttribut.getOrElse("href", "").toString())
            val action = hrefs.split(";").head.split("&").head
            if (action != "") actionDuFormulaire += action.toLowerCase()
          }
        })

        actionDuFormulaire.size match {
          case 0 => widget.actionDuFomulaire = ""
          case 1 => widget.actionDuFomulaire = actionDuFormulaire.head
          case _ => { // il y a plusieurs actions on priorise celle qui commence par action=
            if (!actionDuFormulaire.exists(action => action.startsWith("action="))) widget.actionDuFomulaire = actionDuFormulaire.head
            else {
              val action = actionDuFormulaire.filter(x => x.startsWith("action=")).head
              widget.actionDuFomulaire += action.substring(7)
            }
          }
        }
      }
      // si le widget est dans la table des widgets acceptant le traitement d'une icône, 
      //  on récupère le champ href de l'icône et ainsi que le nom de l'icone et on passe les attribut au niveau du widget en cours
      //  en supprimant l'icone de la liste des fils ainf qu'elle ne soit pas générée.
      //  L'icone donne la possiblité de faire du traitement ajax par exemple. 

      if ((List(widget.getWidgetNameOrComponentName()).intersect(CommonObjectForMockupProcess.engineProperties.widgetsAcceptingIcon).size > 0) &&
        (widget.tableau_des_fils.exists(widgetFils => widgetFils.getWidgetNameOrComponentName() == CommonObjectForMockupProcess.constants.icon))) {
        val tableauDesFilsSansIcone = new ArrayBuffer[WidgetDeBase]()

        widget.tableau_des_fils.foreach(widgetFils => {
          // on verifie si le widget est une icone. On récupère alors le nom d' l'icone ainsi que le fragment
          // et on met à jour la table des icons du widget. Puis on supprime le composant icone de la table des fils du widget en cours
          if (CommonObjectForMockupProcess.constants.icon == widgetFils.getWidgetNameOrComponentName()) {
            val iconAvecSize = widgetFils.mapExtendedAttribut.getOrElse(CommonObjectForMockupProcess.constants.iconShort, "").toString
            val iconName = if (iconAvecSize.contains("|")) { iconAvecSize.split("\\|").head.trim } else iconAvecSize // on supprime la taille
            // l'objet fragment est déjà calulé lors du traitement du widget et mis dans la table mapExTendedAttribute.
            // on met à jour la table iconNameList
            val fragment = widgetFils.mapExtendedAttribut.getOrElse(CommonObjectForMockupProcess.constants.fragment, null)
            if (fragment != null) widget.iconNameList.add(new IconInWidget(iconName, fragment.asInstanceOf[Fragment]))
            else widget.iconNameList.add(new IconInWidget(iconName, null))
          } else { tableauDesFilsSansIcone += widgetFils } // on ne supprime que les fils de type icon
        })
        widget.tableau_des_fils = tableauDesFilsSansIcone

      }

      // -----------------------------------------------------------------------------
      //  Pour un formualaire on vérifie que le champ bindé se termine par "Form"
      // -----------------------------------------------------------------------------
      if (widget.bind.trim.size > 0) {
        if (widget.isFormulaireHTML) { // pour un formulaire le champ se termine par Form
          if (CommonObjectForMockupProcess.generationProperties.generatedFormAlias != "" & !widget.bind.endsWith(CommonObjectForMockupProcess.generationProperties.generatedFormAlias.capitalize)) {
            widget.bind += CommonObjectForMockupProcess.generationProperties.generatedFormAlias.capitalize
          }
        } else { // ce n'est pas un formulaire
          // les variables du formulaire sont préfixées par DTO sauf la dernière qui est un champ
          if (widgetPere.isFormulaireHTML) {
            var tableauVariable1 = widget.bind.split("\\.").toList
            if (tableauVariable1.size > 1) {
              val tableauVariable2 = tableauVariable1.init.map(variable => {
                var variableModifiee = variable
                if (CommonObjectForMockupProcess.generationProperties.generatedDtoAlias != "" && !variable.endsWith(CommonObjectForMockupProcess.generationProperties.generatedDtoAlias.capitalize)) {
                  variableModifiee = variable + CommonObjectForMockupProcess.generationProperties.generatedDtoAlias.capitalize
                }
                variableModifiee
              })
              val tableauVariable3 = tableauVariable2 ++ List(tableauVariable1.last)
              widget.bind = tableauVariable3.mkString(".")
            }
          }
        }
        val (retCode, variableBinding) = traitementBinding.get_variable_binding(widget.bind, widget)
        if (retCode) { widget.variableBinding = variableBinding }
        if (variableBinding.contains(".")) { widget.variableBindingTail = variableBinding.split("\\.").tail.mkString(".") }
        else { widget.variableBindingTail = variableBinding }
        traitementBinding.mise_en_table_classes_binding(widget.bind, widgetPere, widget)
      }
      if (widget.tableau_des_fils.size > 0) widget.tableau_des_fils = enrichissement_widget_branche(widget.tableau_des_fils, widget)
      widget

    })
    // on enrichit les widgets en y mettant le nombre de colonnes
    val tableauEnrichi1 = calcul_numero_colonne_dans_la_branche(tableau, widgetPere)
    val tableauEnrichi2 = labelFor(tableauEnrichi1, null) // on reca
    tableauEnrichi2
  }
  // ----------------------------------------------------------------------------------------------------------------------
  // pour chaque widget de la branche enrichit l'attribut labelFor 
  // si le widget est un label est que le composant suivant est un composant du formulaire qui est sur la même ligne
  // on renseigne alors l'attribut labelfor du composant label.
  // -----------------------------------------------------------------------------------------------------------------------
  def labelFor(branche: ArrayBuffer[WidgetDeBase], widgetPere: WidgetDeBase): ArrayBuffer[WidgetDeBase] = {
    for (i <- 0 until branche.size) {
      //  CommonObjectForMockupProcess .generateLabelForAttributeForTheseWidgets.foreach { x => println("nnnnnnn" + x) }
      if ((List(branche(i).controlTypeID).intersect(CommonObjectForMockupProcess.generationProperties.generateLabelForAttributeForTheseWidgets).size > 0) || (List(branche(i).componentName).intersect(CommonObjectForMockupProcess.generationProperties.generateLabelForAttributeForTheseWidgets).size > 0)) {
        //    println("mmmmmmmmm" + List(branche(i).controlTypeID))
        if ((i + 1) < branche.size) { // pas fin de table pourl'élément suivant ? 
          // on verifie que le widget suivant est un element de la liste des widgets formulaire
          val l1 = List(branche(i + 1).controlTypeID).intersect(CommonObjectForMockupProcess.engineProperties.widgetsEnablingContainerAsAForm)
          val l2 = List(branche(i + 1).componentName).intersect(CommonObjectForMockupProcess.engineProperties.widgetsEnablingContainerAsAForm)
          if ((l1.size > 0 || l2.size > 0) && (branche(i).rowNumber == branche(i + 1).rowNumber)) {
            //   val controlIdPere= if (widgetPere != null) widgetPere.customId else ""
            val composant_associe = branche(i + 1)
            branche(i).labelFor = if (composant_associe.isAComponent) branche(i + 1).componentName else branche(i + 1).controlTypeID.split("::").last.toLowerCase()
            branche(i).labelForWidget = branche(i + 1) // widget référencé par le label
          }
        }
      }
      if (branche(i).tableau_des_fils.size > 0) branche(i).tableau_des_fils = labelFor(branche(i).tableau_des_fils, branche(i))

    } // fin du for
    branche

  }
  // ------------------------------------------------------------------------------------------------------------------------------
  // cette procédure traite les widgets d'une branche. elle est appelée branche par branche par enrichissement_widget_branche 
  // calcul des n° de colonnes dans la branche. 
  // on se base sur le principe de bootstrap : position du widget dans une cellule (12eme largeur du widget Pere)
  // pour chaque widget de la branche on détermine les widgets qui sont sur la même ligne (fonction voverlap)
  // on met les widgets en table et pour chaque widget d'une même ligne, on calcule son numero de colonne en 12eùe dans le container
  // les widgets traités sont flaggés pour ne plus être trités. 
  // --------------------------------------------------------------------------------------------------------------
  private def calcul_numero_colonne_dans_la_branche(branche: ArrayBuffer[WidgetDeBase], widgetPere: WidgetDeBase): ArrayBuffer[WidgetDeBase] = {
    var rowNumber = 0
    val tailleCelluleEnDouzieme = calculTailleCelluleEnDouzieme(widgetPere)
    // à corriger après tests
    val demitailleCelluleEnDouzieme = calculTailleCelluleEnDouzieme(widgetPere) / 2
    for (i <- 0 until branche.size) {
      val widget = branche(i)
      if (!widget.widgetDejaTraiteDansLaBranche) {
        val tableauIndiceWidgetsDuneMemeLigne = ArrayBuffer[Int]() // pour mettre en table les indices des widgets d'une même ligne
        tableauIndiceWidgetsDuneMemeLigne += i
        // on recherche les widgets sur la même ligne que le widget en cours
        for (j <- i + 1 until branche.size) {
          if (!branche(j).widgetDejaTraiteDansLaBranche && voverlap(branche(i), branche(j), 5)) {
            tableauIndiceWidgetsDuneMemeLigne += j
          }
        }
        // sizeEnDOuzieme va servir à générer les divs bootsTrap
        tableauIndiceWidgetsDuneMemeLigne.foreach(ind => {
          branche(ind).widgetDejaTraiteDansLaBranche = true
          branche(ind).rowNumber = rowNumber // n° de ligne dans la table
          branche(ind).tailleCellulePere = tailleCelluleEnDouzieme
          val reste = (branche(ind).xRelative % tailleCelluleEnDouzieme)
          if (reste > demitailleCelluleEnDouzieme) branche(ind).positionEnDouzieme = (branche(ind).xRelative / tailleCelluleEnDouzieme) + 1
          else branche(ind).positionEnDouzieme = (branche(ind).xRelative / tailleCelluleEnDouzieme)
          logBack.debug(utilitaire.getContenuMessage("mes35"),branche(ind).positionEnDouzieme, branche(ind).controlTypeID.toString) 
          logBack.debug(utilitaire.getContenuMessage("mes36"), branche(ind).xRelative, tailleCelluleEnDouzieme)
          logBack.debug(utilitaire.getContenuMessage("mes37"), branche(ind).w)
        })
        rowNumber = rowNumber + 1 // numero de la ligne dans la table
      }

    } // fin de la boule for
    branche

  }
  // le calcul de la taile de la cellule se fait par rapport à la taille du conteneur 
  // la taille de cellule est en pixel
  // 
  private def calculTailleCelluleEnDouzieme(widget: WidgetDeBase): Int = {
    var tailleCelluleEnDouzieme = if (widget != null) { widget.w / CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns } else { (CommonObjectForMockupProcess.mockupContext.global_max_width / CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns).toInt }
    if (tailleCelluleEnDouzieme < CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns) { tailleCelluleEnDouzieme = CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns }
    tailleCelluleEnDouzieme
  }
  // -------------------------------------------------------------------------------
  // Pour chaque branche, on determine les widgets qui sont alignés (donc dans une même row)
  // et ceci afin d'eviter de gérer grapqhiquement les tables n colonnes (on gère donc un seul format
  // de table et on detecte automatiquement le nmbre de colonnes dans la table
  // EN fonction de la taille de chaque widget on determine la classe, i<div class="container-fluid">
  // <div class="row">
  // <div class="col-xs-12 col-md-8">.col-xs-12 .col-md-8</div>
  //< div class="col-xs-6 col-md-4">.col-xs-6 .col-md-4</div>
  // </div>
  //  ON prend une marge de 5 pixels par defaut 
  // -------------------------------------------------------------------------------

  private def voverlap(widget1: WidgetDeBase, widget2: WidgetDeBase, margeErreurPermise: Int): Boolean = {
    (widget2.yRelative >= (widget1.yRelative - margeErreurPermise) && widget2.yRelative <= (widget1.yRelative + margeErreurPermise))

  }
  def impression_catalog(catalog: ArrayBuffer[WidgetDeBase]) {
    catalog.foreach(controle => {
      logBack.debug("noeud maitre: " + controle.controlTypeID.split("::").last + " id: " + controle.id_interne + " colBootStrap:" + controle.positionEnDouzieme + " rowNumber: " + controle.rowNumber)
      logBack.debug(" ++++ extendedAttributes:");
      controle.mapExtendedAttribut.foreach(x => logBack.debug(" clef:" + x._1 + " value:" + x._2) + " "); logBack.debug("")
      if (controle.tableau_des_fils.size > 0) { impression_branche(controle.tableau_des_fils, 0) }

    })
  }

  // ------------------------------------------------------------------------------
  // *** Impression de chaque branche ***
  // ------------------------------------------------------------------------------
  def impression_branche(branche: ArrayBuffer[WidgetDeBase], niveau: Int) {
    val niveau1 = niveau + 4
    var etoile = "*" * niveau1
    branche.foreach(controle => {
      logBack.debug(etoile + "noeud branche: " + controle.controlTypeID.split("::").last + " id_interne:" + controle.id_interne + " colBootStrap:" + controle.positionEnDouzieme + " rowNumber: " + controle.rowNumber)
      logBack.debug(etoile + " ++++ extendedAttributes:");
      controle.mapExtendedAttribut.foreach(x => logBack.debug(" clef:" + x._1 + " value:" + x._2) + " "); logBack.debug("")
      if (controle.tableau_des_fils.size > 0) { impression_branche(controle.tableau_des_fils, niveau1) }

    })
  }

  private def constitutionDuCatalog(catalogAPlat: ArrayBuffer[WidgetDeBase]): ArrayBuffer[WidgetDeBase] = {
    val catalogIntermediaire = new ArrayBuffer[WidgetDeBase]
    // on recherche les noeuds sans pere pour constituer le 1er niveau 
    // et on recopie les fils dans le tableau des fils 
    // c'est catalogBalsamiqAplat qui contient les infos non triées regroupées par branche, mais non triées
    catalogAPlat.foreach(controle => {
      if (controle.pointer_pere == -1) {
        val controleEnrichi = recopieDesFils(controle, catalogAPlat)
        catalogIntermediaire += controleEnrichi
      }
    })
    // catalog Intermediaire contient les noeuds pere. les fils sont recopiés dans la table des fils
    traitement_branche(catalogIntermediaire) // traitement récursif de chaque branche

  }
  // --------------------------------------------------------------------------------------
  // pour chaque controle, on recopie les fils dans la table des fils 
  // on verifie aussi si le widget est un formulaire ou pas
  // ---------------------------------------------------------------------------------------
  private def recopieDesFils(controle: WidgetDeBase, catalogAPlat: ArrayBuffer[WidgetDeBase]): WidgetDeBase = {
    controle.indice_des_fils.foreach(indiceFils => controle.tableau_des_fils += catalogAPlat(indiceFils))
    if (controle.tableau_des_fils.size > 0) {
      val controleEnrichi = controle.tableau_des_fils.map(fils => recopieDesFils(fils, catalogAPlat))
      controle.tableau_des_fils = controleEnrichi
    }

    controle

  }
  // --------------------------------------------------------------------------------------
  // **** On trie chaque branche par rapport à la position relative de chaque widget ****
  // la position relative = y*largeurMaxi + x
  // --------------------------------------------------------------------------------------
  private def traitement_branche(tableauDesFils: ArrayBuffer[WidgetDeBase]): ArrayBuffer[WidgetDeBase] = {
    val tableauTrie = tableauDesFils.sortWith((widget1, widget2) => position_relative(widget1) < position_relative(widget2))
    val tableauFinal = tableauTrie.map(controle => {
      if (controle.tableau_des_fils.size > 0) { controle.tableau_des_fils = traitement_branche(controle.tableau_des_fils) }
      controle
    })
    tableauFinal
  }

  private def position_relative(widget: WidgetDeBase): Double = {

    val ret = (widget.yAbsolute * CommonObjectForMockupProcess.mockupContext.global_max_width) + widget.xAbsolute
    ret
  }

}