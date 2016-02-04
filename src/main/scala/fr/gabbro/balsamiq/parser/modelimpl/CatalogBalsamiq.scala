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
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
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

class CatalogBalsamiq(traitementBinding: TraitementBinding) extends TCatalogBalsamiq {
  var global_max_width: Int = 0
  var global_max_heigth: Int = 0
  /**
   * **** création et enrichissemnt du catalogue ****
   * @param catalogAPlat
   */
  def creation_catalog(catalogAPlat: ArrayBuffer[WidgetDeBase], global_max_width: Int, global_max_heigth: Int): Unit = {
    this.global_max_width = global_max_width
    this.global_max_heigth = global_max_heigth
    catalog = constitutionDuCatalog(catalogAPlat)
    catalog = enrichissement_widget_branche(catalog, null)
    logBack.info(utilitaire.getContenuMessage("mes28"), CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement)
    impression_catalog(catalog)
  }
  /**
   * <p> Appel récursif de cette procédure branche par branche.</p>
   * <p> pour chaque widget, on recalcule, son abscisse et ordonnée relative par rapport au père,</p>
   * <p> sa hauteur et largeur par rapport au père (en poucentage) ainsi que l'espace haut, bas, droit, gauche par rapport au pere.</p>
   *
   * <p>  si le widget Pere a un fils qui est dans la table des widgets des formulaires, il est donc un formulaire</p>
   * <p> les widgets de type radiobuttonGroup induisent par defaut un formlaire (ils n'ont pas de fils)</p>
   * <p>seuls les containers de type canvas peuvent contenir un formulaire</p>
   * <p> Si le widget est un bouton et on recupère le champ hrefs<p>
   * <p> Si le widget est une icone. On récupère alors le nom d' l'icone ainsi que le fragment</p>
   * <p> et on met à jour la table des icons du widget. Puis on supprime le composant icone de la table des fils du widget en cours</p>
   * <p> Pour un formulaire on force la fin du champ bindé se termine par "Form"</p>
   * <p> Pour un DTO, on force la fin du champ bindé se termine par "DTO"</p>
   * <p>Pour chaque widget, on recalcule sa position en 12eme par rapport à son container.</p>
   * <p> Traitement particulier pour l'attribut labelFor</p>
   * @param branche : ArrayBuffer[WidgetDeBase] // branche en cours de traitement
   * @param widgetPere : widgetDeBase // container
   * @return ArrayBuffer[WidgetDeBase] // catalogue des widgets du mockup
   */
  def enrichissement_widget_branche(branche: ArrayBuffer[WidgetDeBase], container: WidgetDeBase): ArrayBuffer[WidgetDeBase] = {
    var positionDansLeConteneur = 0
    val tableau = branche.map(widget => {
      if (global_max_width > 0) { widget.percentageparRapportLargeurTotale = Math.ceil((widget.w.toDouble / global_max_width) * 100.0) }
      if (global_max_heigth > 0) { widget.percentageparRapportHauteurTotale = Math.ceil((widget.h.toDouble / global_max_heigth) * 100.0) }
      // on recalcule les ratios des coordonnées et hauteur et largeur en fonction d'une dimension cible.
      // cela va servir essentiellement aux templates utilisant les coordonnées absolues. 
      if (CommonObjectForMockupProcess.engineProperties.projectionOfWidthInPx > 0 && CommonObjectForMockupProcess.engineProperties.projectionOfHeightInPx > 0) {
        val ratioW = (CommonObjectForMockupProcess.engineProperties.projectionOfWidthInPx / global_max_width).toInt
        val ratioH = (CommonObjectForMockupProcess.engineProperties.projectionOfHeightInPx / global_max_heigth).toInt
        widget.projectionW = widget.w * ratioW
        widget.projectionH = widget.h * ratioH
        widget.projectionX = widget.xAbsolute * ratioW
        widget.projectionY = widget.yAbsolute * ratioH
      }
      // modif le 22/5 : sauvegarde du container du widget en cours
      widget.container = container // on renseigne le container du widget en cours 
      if (container != null) {
        if (container.w > 0) widget.percentageLargeurParRapportPere = Math.ceil((widget.w.toDouble / container.w.toDouble) * 100.0)
        if (container.h > 0) widget.percentageHauteurparRapportPere = Math.ceil((widget.h.toDouble / container.h) * 100.0)
        widget.percentageBandeauHautParRapportPere = Math.ceil(((widget.yAbsolute.toDouble - container.yAbsolute.toDouble) / container.h.toDouble) * 100.0)
        widget.percentageBandeauBasParRapportPere = Math.ceil((((container.yAbsolute.toDouble + container.h.toDouble) - (widget.yAbsolute.toDouble + widget.h.toDouble)) / container.h.toDouble) * 100.0)
        widget.percentageBandeauDroiteParRapportPere = Math.ceil((((container.xAbsolute.toDouble + container.w.toDouble) - (widget.xAbsolute.toDouble + widget.w.toDouble)) / container.w.toDouble) * 100.0)
        widget.percentageBandeauGaucheParRapportPere = Math.ceil(((widget.xAbsolute.toDouble - container.xAbsolute.toDouble) / container.w.toDouble) * 100.0)
        widget.xRelative = math.abs(container.xAbsolute - widget.xAbsolute)
        widget.yRelative = math.abs(container.yAbsolute - widget.yAbsolute)
        widget.positionDansLeConteneur = positionDansLeConteneur

      } else { // il n'y a pas de container père
        widget.percentageLargeurParRapportPere = Math.ceil((widget.w.toDouble / global_max_width.toDouble) * 100.0)
        widget.percentageHauteurparRapportPere = Math.ceil((widget.h.toDouble / global_max_heigth) * 100.0)
        widget.percentageBandeauHautParRapportPere = Math.ceil(((widget.yAbsolute.toDouble) / global_max_heigth.toDouble) * 100.0)
        widget.percentageBandeauBasParRapportPere = Math.ceil((((global_max_heigth) - (widget.yAbsolute.toDouble + widget.h.toDouble)) / global_max_heigth.toDouble) * 100.0)
        widget.percentageBandeauDroiteParRapportPere = Math.ceil((((global_max_width.toDouble) - (widget.xAbsolute.toDouble + widget.w.toDouble)) / global_max_width.toDouble) * 100.0)
        widget.percentageBandeauGaucheParRapportPere = Math.ceil(((widget.xAbsolute.toDouble) / global_max_width.toDouble) * 100.0)
        widget.xRelative = widget.xAbsolute
        widget.yRelative = widget.yAbsolute
        widget.positionDansLeConteneur = positionDansLeConteneur

      }
      positionDansLeConteneur = positionDansLeConteneur + 1

      // si le widget Pere a un fils qui est dans la table des widgets des formulaires, il est donc un formulaire
      // les widgets de type radiobuttonGroup induisent par defaut un formlaire (ils n'ont pas de fils
      // il faudra récupérer l'action d'un bouton et la stocker dans mapExtendedAttribut
      // seuls les containers de type canvas peuvent contenir un formulaire
      // modif le 22/5 un container n'est pas un formulaire s'il est déjà inclus dans un container de type formulaire

      val containerIsAformulaire = if (container != null && container.isFormulaireHTML) { true } else { false }
      if ((List(widget.getWidgetNameOrComponentName()).intersect(CommonObjectForMockupProcess.templatingProperties.widgetsConsideredAsAForm).size > 0) &&
        (!containerIsAformulaire) && // un formulaire ne peut être inclus dans un autre formulaire    
        (widget.tableau_des_fils.exists(widgetFils => CommonObjectForMockupProcess.engineProperties.widgetsEnablingContainerAsAForm.exists(x => (x == widgetFils.getWidgetNameOrComponentName()))))) {
        widget.isFormulaireHTML = true
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
      // si le widget est dans la table des widgets acceptant le traitement d'une icône et qu'il est container d'un widget de type icône, 
      // on récupère le champ href de l'icône et ainsi que le nom de l'icone et on passe les attributs au niveau du widget en cours
      // en supprimant l'icone de la liste des fils ainf qu'elle ne soit pas générée.
      // L'icône donne la possiblité de faire du traitement ajax par exemple. 
      // on met àjout la table iconNameList (widget.iconNameList) et on supprime le widget Icone de la table des fils (widget.tableau_des_fils)

      if ((List(widget.getWidgetNameOrComponentName()).intersect(CommonObjectForMockupProcess.engineProperties.widgetsAcceptingIcon).size > 0) &&
        (widget.tableau_des_fils.exists(widgetFils => widgetFils.getWidgetNameOrComponentName() == cstIcon))) {
        val tableauDesFilsSansIcone = new ArrayBuffer[WidgetDeBase]()

        widget.tableau_des_fils.foreach(widgetFils => {
          // on vérifie si le widget est une icône. On récupère alors le nom de l'icone ainsi que le fragment
          // et on met à jour la table des icons du widget. Puis on supprime le composant icone de la table des fils du widget en cours
          if (cstIcon == widgetFils.getWidgetNameOrComponentName()) {
            val iconAvecSize = widgetFils.mapExtendedAttribut.getOrElse(cstIconShort, "").toString
            val iconName = if (iconAvecSize.contains("|")) { iconAvecSize.split("\\|").head.trim } else iconAvecSize // on supprime la taille
            // l'objet fragment est déjà calculé lors du traitement du widgetIcon (l'attribut href génère un fragment) et mis dans la table mapExTendedAttribute.
            // on met à jour la table iconNameList
            val fragment = widgetFils.mapExtendedAttribut.getOrElse(cstFragment, null)
            if (fragment != null) { widget.iconNameList.add(new IconInWidget(iconName, fragment.asInstanceOf[Fragment])) }
            else widget.iconNameList.add(new IconInWidget(iconName, null))
          } else { tableauDesFilsSansIcone += widgetFils } // on ne supprime que les fils de type icon
        })
        widget.tableau_des_fils = tableauDesFilsSansIcone

      }
      // ----------------------------------------------------------------------------------
      // traitement du binding
      // ----------------------------------------------------------------------------------
      val (retcode, bind, variableBinding, variableBindingTail) = traitementBinding.process(widget, container)
      if (retcode) {
        widget.bind = bind
        widget.variableBinding = variableBinding
        widget.variableBindingTail = variableBindingTail
      }
      // traitement itératif des fils
      if (widget.tableau_des_fils.size > 0) widget.tableau_des_fils = enrichissement_widget_branche(widget.tableau_des_fils, widget)
      widget

    })
    // on enrichit les widgets en y mettant le nombre de colonnes
    val tableauEnrichi1 = calcul_numero_colonne_dans_la_branche(tableau, container)
    val tableauEnrichi2 = labelFor(tableauEnrichi1, null) // on reca
    // atention l'appel du type de formulaire est obligatoirement après l'enrichissemnt sur les n° de colonnes
    val tableauEnrichi3= determinationTypeDeFormulaire(tableauEnrichi2,null) // type de formulaire

    tableauEnrichi3
  }

  /**
   * <p>pour chaque widget de la branche enrichit l'attribut labelFor</p>
   * <p>si le widget est un label est que le composant suivant est un composant du formulaire qui est sur la même ligne</p>
   * <p>on renseigne alors l'attribut labelFor du composant label ainsi que l'attribut labelForWidget</p>
   * @param branche
   * @param widgetPerep
   * @return ArrayBuffer[WidgetDeBase]
   */
  def determinationTypeDeFormulaire(branche: ArrayBuffer[WidgetDeBase], widgetPere: WidgetDeBase): ArrayBuffer[WidgetDeBase] = {
    for (i <- 0 until branche.size) {
      // on ne traite que les widgets qui  sont des formulaires
      val widget = branche(i)
      if (widget.isFormulaireHTML) { 
        widget.typeDeFormulaire = scanDesfilsDuformulairePourDeterminerLeType(widget.tableau_des_fils)
      }
      else {
      // traitement itératif des fils,pour les widges qui ne sont pas des formulaires
      if (widget.tableau_des_fils.size > 0) {widget.tableau_des_fils = determinationTypeDeFormulaire(widget.tableau_des_fils, widget)}
      }

    } // fin du for
    branche

  }
  // le container en cours est de type formulaire 
  // tous les widgets sont sur une même ligne => type =0 
  // types de formulaire :
  //    inlineFform 
  //    basicForm 
  //    horizontalForm 
  // 
  def scanDesfilsDuformulairePourDeterminerLeType(branche: ArrayBuffer[WidgetDeBase]): String = {
    var typeDeFormulaire = cstHorizontalForm
    val ar1 = ArrayBuffer[(Int, Int)]()
    // on récupère les widgets du container (formulaire)

    branche.foreach(widget => {
      val rowNumber = widget.rowNumber
      val positionEnDouzieme = widget.positionEnDouzieme
      val tuple1 = (rowNumber, positionEnDouzieme)
      ar1 += tuple1

    })
    val maxRowNumber = ar1.filter { case (rowNumber, positionEnDouzieme) => rowNumber > 0 }.size
    // inline-form 
    if (maxRowNumber == 0) { typeDeFormulaire = cstInlineForm }
    else {
      val ar2 = ar1.groupBy {
        case (rowNumber, positionEnDouzieme) => {
          rowNumber
        }
      }
      // Type De Formulaire 
      // Un seul composant par ligne ?? 
      if (ar2.forall { case (rowNumber, ar3) => ar3.size <= 1 }) { typeDeFormulaire = cstBasicForm }

    }
    typeDeFormulaire
  }
  /**
   * <p>pour chaque widget de la branche enrichit l'attribut labelFor</p>
   * <p>si le widget est un label est que le composant suivant est un composant du formulaire qui est sur la même ligne</p>
   * <p>on renseigne alors l'attribut labelFor du composant label ainsi que l'attribut labelForWidget</p>
   * @param branche
   * @param widgetPerep
   * @return ArrayBuffer[WidgetDeBase]
   */
  def labelFor(branche: ArrayBuffer[WidgetDeBase], widgetPere: WidgetDeBase): ArrayBuffer[WidgetDeBase] = {
    for (i <- 0 until branche.size) {
      // on ne traite que les widgets qui  peuvent contenir l'attribut labelFor (table generateLabelForAttributeForTheseWidgets)
      if ((List(branche(i).controlTypeID).intersect(CommonObjectForMockupProcess.generationProperties.generateLabelForAttributeForTheseWidgets).size > 0) || (List(branche(i).componentName).intersect(CommonObjectForMockupProcess.generationProperties.generateLabelForAttributeForTheseWidgets).size > 0)) {
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
      // traitement itératif des fils. 
      if (branche(i).tableau_des_fils.size > 0) branche(i).tableau_des_fils = labelFor(branche(i).tableau_des_fils, branche(i))

    } // fin du for
    branche

  }
  /**
   * <p>cette procédure traite les widgets d'une branche. elle est appelée branche par branche par enrichissement_widget_branche</p>
   * <p>calcul des n° de colonnes dans la branche.</p>
   * <p>on se base sur le principe de bootstrap : position du widget dans une cellule (12eme largeur du widget Pere)</p>
   * <p>pour chaque widget de la branche on détermine les widgets qui sont sur la même ligne (fonction voverlap)</p>
   * <p>on met les widgets en table et pour chaque widget d'une même ligne, on calcule son numero de colonne en 12eùe dans le container</p>
   * <p>les widgets traités sont flaggés pour ne plus être traités.
   *
   *
   * @param branche
   * @param widgetPere
   * @return ArrayBuffer[WidgetDeBase]
   */
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
          if (reste > demitailleCelluleEnDouzieme) {branche(ind).positionEnDouzieme = (branche(ind).xRelative / tailleCelluleEnDouzieme) + 1}
          else {branche(ind).positionEnDouzieme = (branche(ind).xRelative / tailleCelluleEnDouzieme)}
          logBack.debug(utilitaire.getContenuMessage("mes35"), branche(ind).positionEnDouzieme, branche(ind).controlTypeID.toString)
          logBack.debug(utilitaire.getContenuMessage("mes36"), branche(ind).xRelative, tailleCelluleEnDouzieme)
          logBack.debug(utilitaire.getContenuMessage("mes37"), branche(ind).w)
        })
        rowNumber = rowNumber + 1 // numero de la ligne dans la table
      }

    } // fin de la boule for
    branche

  }
  /**
   * le calcul de la taile de la cellule se fait par rapport à la taille du conteneur
   * la taille de cellule est en pixel
   *
   * @param widget
   * @return taille de la cellule
   */
  private def calculTailleCelluleEnDouzieme(widget: WidgetDeBase): Int = {
    var tailleCelluleEnDouzieme = if (widget != null) { widget.w / CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns } else { (global_max_width / CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns).toInt }
    if (tailleCelluleEnDouzieme < CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns) { tailleCelluleEnDouzieme = CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns }
    tailleCelluleEnDouzieme
  }

  /**
   * verification si 2 widgets sont alignés
   * @param widget1 : WidgetDeBase
   * @param widget2 : WidgetDeBase
   * @param margeErreurPermise nbre de pixels
   * @return true or false
   */
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

  /**
   * *** Impression de chaque branche ***
   * @param branche : ArrayBuffer[WidgetDeBase]
   * @param niveau : niveau
   */
  def impression_branche(branche: ArrayBuffer[WidgetDeBase], niveau: Int) {
    val niveau1 = niveau + 4
    var etoile = "*" * niveau1
    branche.foreach(controle => {
      logBack.debug(etoile + "noeud branche: " + controle.controlID + " composant" + controle.getWidgetNameOrComponentName() + " colBootStrap:" + controle.positionEnDouzieme + " rowNumber: " + controle.rowNumber)
      logBack.debug(etoile + " ++++ extendedAttributes:");
      controle.mapExtendedAttribut.foreach(x => logBack.debug(" clef:" + x._1 + " value:" + x._2) + " "); logBack.debug("")
      if (controle.tableau_des_fils.size > 0) { impression_branche(controle.tableau_des_fils, niveau1) }

    })
  }

  /**
   * <p>on recherche les noeuds sans pere pour constituer le 1er niveau</p>
   * <p>et on recopie les fils dans le tableau des fils</p>
   * <p>catalogBalsamiqAplat contient les infos non triées regroupées par branche, mais non triées</p>
   *
   * @param catalogAPlat  : ArrayBuffer[WidgetDeBase]
   * @return : ArrayBuffer[WidgetDeBase]
   */
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
  /**
   * pour chaque controle, on recopie les fils dans la table des fils
   * @param controle : WidgetDeBase
   * @param catalogAPlat:ArrayBuffer[WidgetDeBase]
   * @return widget
   */
  private def recopieDesFils(controle: WidgetDeBase, catalogAPlat: ArrayBuffer[WidgetDeBase]): WidgetDeBase = {
    controle.indice_des_fils.foreach(indiceFils => controle.tableau_des_fils += catalogAPlat(indiceFils))
    if (controle.tableau_des_fils.size > 0) {
      val controleEnrichi = controle.tableau_des_fils.map(fils => recopieDesFils(fils, catalogAPlat))
      controle.tableau_des_fils = controleEnrichi
    }

    controle

  }
  /**
   * pour une branche, tri de la position de chaque widget.
   * appel itératif du traitement des fils
   * @param tableauDesFils
   * @return  ArrayBuffer[WidgetDeBase]
   */
  private def traitement_branche(tableauDesFils: ArrayBuffer[WidgetDeBase]): ArrayBuffer[WidgetDeBase] = {
    val tableauTrie = tableauDesFils.sortWith((widget1, widget2) => position_relative(widget1) < position_relative(widget2))
    val tableauFinal = tableauTrie.map(controle => {
      if (controle.tableau_des_fils.size > 0) { controle.tableau_des_fils = traitement_branche(controle.tableau_des_fils) }
      controle
    })
    tableauFinal
  }

  /**
   * calcul de la position relative par rapport au début de l'écran
   * a position relative = y*largeurMaxi + x
   * @param widget
   * @return
   */
  private def position_relative(widget: WidgetDeBase): Double = {
    val ret = (widget.yAbsolute * global_max_width) + widget.xAbsolute
    ret
  }

}