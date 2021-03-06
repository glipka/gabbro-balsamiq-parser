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

import scala.collection.immutable.List
import org.jdom2.Element
import org.jdom2.output._
import scala.collection.mutable.ArrayBuffer
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._
import scala.beans.BeanProperty
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess
import fr.gabbro.balsamiq.parser.service.serviceimpl.IBalsamiqFreeMarker
import fr.gabbro.balsamiq.parser.modelimpl.CatalogDesComposants
import fr.gabbro.balsamiq.parser.modelimpl.Utilitaire
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementBinding
import fr.gabbro.balsamiq.parser.modelimpl.Fragment
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
import java.io.StringWriter
// ----------------------------------------------------------
//  controlID="6"
// controlTypeID="com.balsamiq.mockups::Label"
//  x="74" y="15" w="-1" h="-1"
//    measuredW="157"
//      measuredH="46"
//        zOrder="0"
//          locked="false"
//            isInGroup="-1"
// ---------------------------------------------------------------------------------
// attention à créer le composant enrichi au moment de l'init du widget pour les templates
// devant appeler une methode du composant enrichi
// -------------------------------------------------------------------------------------------
class Token(@BeanProperty var name: String, @BeanProperty var valeur: String)
class IconInWidget(@BeanProperty var iconName: String, @BeanProperty var fragment: Fragment)

abstract class WidgetDeBase(@BeanProperty val id_interne: Int, groupe_en_cours: WidgetDeBase, elementXML: Element, traitementBinding: TraitementBinding, catalogDesComposants: CatalogDesComposants, var isAComponent: Boolean) {
  val logBack = LoggerFactory.getLogger(this.getClass());
  var sourceXmldDeLelement: String = ""
  var componentSrc = "" // source du composant
  var componentName = "" // nom du composant bootStrap
  var componentXML: Element = null;
  var repositoryName = "" // nom du repostory contenant le composant
  var controlTypeID: String = ""; // type de composant
  @BeanProperty var shortWidgetName: String = ""
  var controlID: Int = 0;
  @BeanProperty var isFormulaireHTML = false // variable renseignée dans fileConverter
  @BeanProperty var typeDeFormulaire = "" // enrichi dans catalogBalsamiq  // types de formulaire :   inlineForm      basicForm     horizontalForm 

  var xAbsolute: Int = 0; // abscisse absolue du widget par rapport au début de la page
  var yAbsolute: Int = 0; // ordonnée absolue du widget par rapport au début de la page
  var z: Int = 0;
  var w: Int = 0;
  var h: Int = 0;

  var xRelative: Int = 0; // abscisse relative du widget par rapport à son conteneur
  var yRelative: Int = 0; // ordonnee relative du widget par rapport à son conteneur
  @BeanProperty var container: WidgetDeBase = null; // modif le 22/5/15 par gl : ajoutcontainer du widget en cours
  @BeanProperty var positionDansLeConteneur: Int = 0 // position du widget dans le conteneur (sans tenir compte des n° de lignes et colonnes)
  @BeanProperty var labelFor = "";
  @BeanProperty var labelForWidget: WidgetDeBase = null;
  @BeanProperty var labelForReferenceur: WidgetDeBase = null;
  @BeanProperty var customId: String = "" // id du composant
  @BeanProperty var iconNameList = new java.util.ArrayList[IconInWidget] // sera renseigné dans le traitement du catalogue si un widget de type icon est inclus dans le widget
  // largeur et hauteur du widget par rapport à la taille de la fenêtre 
  var percentageparRapportHauteurTotale: Double = 0
  var percentageparRapportLargeurTotale: Double = 0
  // largeur et hauteur du widget par rapport au parent 
  // le pourcentage haut = ((ordonnée  du composant - ordonnée du pere)/ hauteur du pere )
  // pourcentage bas = ((ordonnée  du composant + hauteur composant - ordonnée du pere + hauteur du pere)/ hauteur du pere ) 

  // pourcentage droite = ((abscisse   du composant + largeur du composant - (abscisse du pere + largeur du pere)/ largeur du pere )
  // pourcentage gauche = ((abscisse  du composant  - abscisse du pere )/ largeur du pere du pere )

  var percentageHauteurparRapportPere: Double = 0
  var percentageLargeurParRapportPere: Double = 0
  var percentageBandeauHautParRapportPere: Double = 0
  var percentageBandeauBasParRapportPere: Double = 0
  var percentageBandeauDroiteParRapportPere: Double = 0
  var percentageBandeauGaucheParRapportPere: Double = 0
  var tailleCellulePere = 0
  var measuredW: Int = 0
  var measuredH: Int = 0;
  var zOrder: Int = 0
  var locked: Boolean = false
  var isInGroup: Int = 0;
  var hauteurEstimeeEnCaractere = 0
  var largeurEstimeeEnCaractere = 0
  var widgetDejaTraiteDansLaBranche = false // champ technique temporaire : sert dans la determination du nombre de colonnes
  //  var sizeEnDouzieme: Int = 0 // taille en douzieme pour bootstrap
  var positionEnDouzieme: Int = 0; // position du widget en douzieme
  var largeurEnDouzieme: Int = 0; // largeur du composant en douzieme
  var rowNumber: Int = 0 //  n° de ligne calculé : utilisé pour les row dans les tables
  var columnNumber: Int = 0 //  n° de colonne dans la ligne  : utilisé pour les row dans les tables
  @BeanProperty var mapExtendedAttribut = scala.collection.mutable.Map[String, Object]()
  var pointer_pere = -1;
  var tableau_des_fils = new ArrayBuffer[WidgetDeBase]()
  var indice_des_fils = new ArrayBuffer[Int]()

  val utilitaire = new Utilitaire() // classe utilitaire de base

  protected var mapIndice = scala.collection.mutable.Map[String, String]()
  var actionDuFomulaire = "" // action renseignée dans le traitement catalogue
  val tableOverrideParams = scala.collection.mutable.Map[String, (String, String, String, String)]()
  // les projections servent à recalculer les coordonnées absolues par rapport à une largeur et hauteur de référence.
  var projectionX: Int = 0
  var projectionY: Int = 0
  var projectionW: Int = 0
  var projectionH: Int = 0
  @BeanProperty var bind: String = ""
  var itemsVar: String = ""
  @BeanProperty var variableBinding = "" // cette variable est enrichie dans le traitement du catalogue
  @BeanProperty var variableBindingTail = "" // cette variable est enrichie dans le traitement

  process;
  /*
  * <p>on récupere les attributs de base </p>
  * <p>on récupère les attibuts étendus </p> 
  * <p>si le widget est un composant,  on réécrit les attributs en concatenant ID du composant</p>
 * 
 */
  def process() {
    if (this.isAComponent) {

      recuperationAttributsDeBase(elementXML)
      mapExtendedAttribut = mapExtendedAttribut ++ this.recuperationDesAttributsEtendusDuComposant(elementXML)
      // si c'est un composant, on réécrit les attributs en concatenant ID du composant
      remplacementControlIDparCustomIdDuWidgetDuComponent(elementXML)

    } else { // ce n'est pas un composant 

      recuperationAttributsDeBase(elementXML)
      recuperationDesAttributsEtendus(elementXML)
    }
  }
  // cette classe est abstraite et sera surchargée dans chaque composant spécifique
  def enrichissementParametres(param: String): (String, Object)

  /**
   * <p>Freemarker communique avec les templates via une hashMap</p>
   * <p>cette methode est appelée lors de la génération du template pour ce widget.</p>
   * <p>on génère la hasMap contenant les attributs étendus en prenant en compte les spécificités
   * de certains composants.</p>
   * <p>les composants spécifiques héritent de la classe AbstratComposant et permettent de gérer
   * spécifiquement les attributs</p>
   * @return
   */
  def generationTableauDeParametresPourTemplates(): java.util.Map[String, Object] = {
    val tableau = scala.collection.mutable.Map[String, Object]()
    tableau += (
      cstInternalId -> id_interne.toString,
      cstControlTypeID -> controlTypeID.toString(),
      cstControlID -> controlID.toString,
      cstXAbsolute -> xAbsolute.toString,
      cstYAbsolute -> yAbsolute.toString,
      cstZ -> z.toString,
      cstW -> w.toString,
      cstH -> h.toString,
      cstXRelative -> xRelative.toString,
      cstYRelative -> yRelative.toString,
      cstPositionInContainer -> positionDansLeConteneur.toString,
      cstPercentageHeightWithRespectToTotalHeight -> percentageparRapportHauteurTotale.toInt.toString,
      cstPercentageWidthWithRespectToTotalWidth -> percentageparRapportLargeurTotale.toInt.toString,
      cstPercentageHeightWithRespectToContainerHeight -> percentageHauteurparRapportPere.toInt.toString,
      cstPercentageWidhtWithRespectToContainerWidth -> percentageLargeurParRapportPere.toInt.toString,
      cstPercentageTopBannerWithRespectToContainerHeight -> percentageBandeauHautParRapportPere.toInt.toString,
      cstPercentageBottomBannerWithRespectToContainerHeight -> percentageBandeauBasParRapportPere.toInt.toString,
      cstPercentageRightBannerWithRespectToContainerWidth -> percentageBandeauDroiteParRapportPere.toInt.toString,
      cstPercentageLeftBannerWithRespectToContainerWidth -> percentageBandeauGaucheParRapportPere.toInt.toString,
      cstEstimatedHeightInChar -> hauteurEstimeeEnCaractere.toString,
      cstEstimatedWidthInChar -> largeurEstimeeEnCaractere.toString,
      cstMeasuredW -> measuredW.toString,
      cstMeasuredH -> measuredH.toString,
      cstZOrder -> zOrder.toString,
      cstLocked -> locked.toString,
      cstIsInGroup -> isInGroup.toString,
      cstPositionIn12th -> positionEnDouzieme.toString,
      cstRowNumber -> rowNumber.toString,
      cstFormularAction -> actionDuFomulaire,
      cstColumnNumber -> columnNumber.toString)

    if (iconNameList.size > 0) { tableau += (cstIcons -> iconNameList) }
    if (variableBinding != "") { tableau += (cstVariableBinding -> variableBinding) }
    if (variableBindingTail != "") { tableau += (cstVariableBindingTail -> variableBindingTail) }

    mapExtendedAttribut.foreach {
      case (x, y) => tableau += (x -> y)
    }

    // -------------------------------------------------------------------------------------------
    // cas des controles spécifiques
    // attention ne pas oublier que le code est appelée au moment du traitement du template
    // et non au moment de la mise en table des composants
    // -------------------------------------------------------------------------------------------
    val (param, value) = enrichissementParametres("") // enrichissement spéifique par type 
    tableau += (param -> value) // parametres enrichis
    tableau

  }

  /**
   * <p>on teste les 4 points</p>
   * <p>la mise en arbre des widgets est basé sur la notion de container</p>
   * <p>Cette methode est appelée lors de la constitution du catalogue A Plat.</p>
   *
   *
   * @param lePlusGrandRectangle
   * @return
   */
  def estCompletementInclusDans(lePlusGrandRectangle: WidgetDeBase): Boolean = {
    val x1 = lePlusGrandRectangle.xAbsolute
    val w1 = lePlusGrandRectangle.w
    val y1 = lePlusGrandRectangle.yAbsolute
    val h1 = lePlusGrandRectangle.h

    val x2 = this.xAbsolute
    val w2 = this.w
    val y2 = this.yAbsolute
    val h2 = this.h

    ((x1 <= x2) && ((x1 + w1 - 1) >= (x2 + w2 - 1)) && (y1 <= y2) && ((y1 + h1 - 1) >= (y2 + h2 - 1)))

  }
  // --------------------------------------------------------------------------------
  //  controlID="6" 
  // controlTypeID="com.balsamiq.mockups::Label" 
  //  x="74" y="15" w="-1" h="-1" 
  //    measuredW="157" 
  //      measuredH="46" 
  //        zOrder="0" 
  //          locked="false" 
  //            isInGroup="-1"
  // ------------------------------------------------------------------------------
  /**
   * <p>Récupération des attributs de base d'un widget : x,y,w,h, ...</p>
   * <p>Attention le controlID n'est unique que dans un groupe</p>
   * <p>Si le widget est dans un groupe, ses coordonnées sont relatives par rapport au groupe</p>
   * <p>on verifie qu'on est dans le bon groupe</p>
   * <p>Si le groupe est un composant, on recalcule les coordonnées du widget</p>
   * <p>     (les parametres en override ont été stockés dans le traitement du composant )</p>
   *
   * @param e : Element
   */
  protected def recuperationAttributsDeBase(e: Element) {
    val xmloutputter = new XMLOutputter
    val out = new StringWriter();
    xmloutputter.output(e, out)

    this.sourceXmldDeLelement = out.toString // code xource xml de l'élément en cours
    this.controlTypeID = e.getAttributeValue(cstControlTypeID) //.substring(22);
    if (this.controlTypeID.contains("::")) {
      this.shortWidgetName = this.controlTypeID.split("::").last
    } else {
      this.shortWidgetName = this.controlTypeID
    }
    //  if (controlTypeID == CommonObjectForMockupProcess .componentBalsamiq) isAComponent = true
    this.w = utilitaire.toInt(e.getAttributeValue(cstW));
    this.measuredW = utilitaire.toInt(e.getAttributeValue(cstMeasuredW));
    if (this.w < 1) { this.w = this.measuredW; }
    this.h = utilitaire.toInt(e.getAttributeValue(cstH));
    this.measuredH = utilitaire.toInt(e.getAttributeValue(cstMeasuredH));
    if (this.h < 1) { this.h = this.measuredH }
    this.xAbsolute = utilitaire.toInt(e.getAttributeValue(cstX));
    this.yAbsolute = utilitaire.toInt(e.getAttributeValue(cstY));
    this.z = utilitaire.toInt(e.getAttributeValue(cstZOrder));
    this.controlID = utilitaire.toInt(e.getAttributeValue(cstControlID));
    this.isInGroup = utilitaire.toInt(e.getAttributeValue(cstIsInGroup));
    if (CommonObjectForMockupProcess.engineProperties.viewportPxFontSizeInBalsamiqMockup.forall(_.isDigit) && CommonObjectForMockupProcess.engineProperties.viewportPxFontSizeInBalsamiqMockup.toInt != 0) {
      this.hauteurEstimeeEnCaractere = this.h / CommonObjectForMockupProcess.engineProperties.viewportPxFontSizeInBalsamiqMockup.toInt
      this.largeurEstimeeEnCaractere = this.w / CommonObjectForMockupProcess.engineProperties.viewportPxFontSizeInBalsamiqMockup.toInt
    }

    if (this.isInGroup != -1) { // l'élement est dans un groupe, ses coordonnées sont relatives par rapport au groupd
      // on verifie qu'on est dans le bon groupe 
      // si le groupe est un composant, on recalcule les coordonnées du widget
      if ((groupe_en_cours != null && groupe_en_cours.controlID == this.isInGroup) || (groupe_en_cours != null && groupe_en_cours.isAComponent)) {
        // les parametres en override ont été stockés dans le traitement du composant (groupd_en_cours)
        if (groupe_en_cours.isAComponent) {
          val (overrideX, overrideY, overrideW, overrideH) = groupe_en_cours.tableOverrideParams.getOrElse(this.controlID.toString(), ("", "", "", ""))
          if (overrideX != null && overrideX != "-1" && overrideX != "") { this.xAbsolute = overrideX.toInt }
          if (overrideY != null && overrideY != "-1" && overrideY != "") { this.yAbsolute = overrideY.toInt }
          if (overrideW != null && overrideW != "-1" && overrideW != "") { this.w = overrideW.toInt }
          if (overrideH != null && overrideH != "-1" && overrideH != "") { this.h = overrideH.toInt }
          this.xAbsolute = groupe_en_cours.xAbsolute + this.xAbsolute
          this.yAbsolute = groupe_en_cours.yAbsolute + this.yAbsolute
        } else {
          this.xAbsolute = groupe_en_cours.xAbsolute + this.xAbsolute
          this.yAbsolute = groupe_en_cours.yAbsolute + this.yAbsolute
        }
      } else {
        logBack.info(cstAttention + this.isInGroup)
        val mes = cstAttention + this.isInGroup
        if (CommonObjectForMockupProcess.globalContext != null) {
          CommonObjectForMockupProcess.globalContext.addTraceToReport(CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, "", this.getClass.toString().split("\\.").last, mes, "", cstError)
        }
      }

    }
  }

  /**
   *
   * <p>Les attributs étendus sont récupérés et stockés sous forme de clef, valeur.</p>
   * <p>POur les composants, il y a</p>
   * <p> Il y a un traitement particulier pour le binding des données ainsi que pour la validation</p>
   * <p> Pourle binding on utilise le champ customControlId et pour la validation le champ customControlData</p>
   * <p> Il y a 2 types de binding possibles :</p>
   * <p> Bind = class1.class2.valeur:Int    // on bind le widget avec le champ décrit</p>
   * <p>  bind=map(clef1,valeur1)  // on binde le contenu du widget avec un map existante, par exemple en localStorage.</p>
   * <p> pour la validation : validate=valeur1,valeur2,...</p>
   * <p>******  Exemple de contenu ******</p>
   * <p> <controlProperties></p>
   * <p>     <href>testgl01%26bm%3Btestgl01.bmml%26bm%3Btestgl01.bmml%26bm%3Btestgl01.bmml</href></p>
   * <p>     <map>%3Carea%20shape%3D%22rect%22%20coords%3D%22809%2C422%2C956%2C447%22%20href%3D%22testgl01.bmml%22%20alt%3D%22testgl01%22%20id%3D%2268%22/%3E</map></p>
   * <p>     <text>search</text></p>
   * <p>   </controlProperties></p>
   *
   * @param e:Element
   */
  private def recuperationDesAttributsEtendus(e: Element): Unit = {
    if (e.getChildren().size() != 0) {
      val controlProperties = e.getChild(cstControlProperties);
      if (controlProperties != null) {
        val cp = controlProperties.getChildren().toList;
        cp.foreach(propertie => {
          val elementName = propertie.getName().trim
          var elementValue = utilitaire.remplaceHexa(propertie.getText().trim) // on remplace les %xy par leur valeur ascii
          mapExtendedAttribut += (elementName -> elementValue)
          if (elementName == cstHref) { // c'est un lien ? si oui on génère un attribut fragment
            mapExtendedAttribut ++= traitementHref(elementValue)
          } // traitement de la propriété binding
          // 2 cas possibles :
          //   bind=map(key,value)
          //   bind=class1.classe2.var1:Int
          // plusieurs possibilités : validate=xxx;id=toto  
          // customId est standard par rapport 
          else if (elementName == cstCustomID) { // customId=wwwww   sert essentiellement pour identifier les formulaires 
            val id = if (elementValue != "") { elementValue.trim } else { "" }
            this.customId = id
            if (id != "" && CommonObjectForMockupProcess.mockupContext != null) { CommonObjectForMockupProcess.mockupContext.tableauDesIdsDesWidgets += this.customId } // on met en table le nom des formulaires

          } else if (elementName == cstCustomData) {
            val tableValue = elementValue.split(";").map(_.trim)

            tableValue.foreach(value => {
              // id ne doit être renseigné que pour un container. 
              if (value.startsWith(cstBind) && value.contains("(") && value.contains(",")) { //   bind=map(key,value)
                val (retCode, structureMap) = traitementBinding.traitementMap(value)
                if (retCode) { mapExtendedAttribut += (cstMapBinding -> structureMap) }

              } // --------------------------------------------------------------------------------------------
              // binding sur un champ. 
              // L'attribut customId d'un champ est déduit du binding de champ (on prend le nom du champ
              // Le template freemaker met en oeuvre cette norme. 
              // --------------------------------------------------------------------------------------------
              else if (value.startsWith(cstBind + "=")) {
                //  val (retCode, variableBinding) = traitementBinding.mise_en_table_classes_binding(value)
                this.bind = value.substring(cstBind.size + 1) // le bind sera retraité après constitution du catalogue pour concatener le bind du pere.    
              } // Traitement de la validation des données
              else if (value.startsWith((cstItemsVar + "="))) {
                //  val (retCode, variableBinding) = traitementBinding.mise_en_table_classes_binding(value)
                this.itemsVar = value.substring(cstItemsVar.size + 1) // itemsVar sera retraité après constitution du catalogue pour concatener le bind du pere.    
                val tab2 = value.split("=")
                if (tab2.size > 1) { mapExtendedAttribut += (tab2.head -> tab2.last) }
              } // Traitement de la validation des données
              else if (value.startsWith(cstValidate + "=")) {
                val (retCode, variablesValidate) = mise_en_table_validation_du_champ(value)
                if (retCode) { mapExtendedAttribut += (cstVariablesValidate -> variablesValidate) }
              } // hint a servir à l'infoBulle
              else { // on stocke la clef valeur directement
                val tab2 = value.split("=")
                if (tab2.size > 1) { mapExtendedAttribut += (tab2.head -> tab2.last) }
              }

            })
          }

        })

      }
    } // fin de if 
  } // fin de getCOntrolProperties

  /**
   * *** utilitaireage et enrichissement du texte utilisé directement dans les templates
   * cette methode est aussi appelée depuis FreeMarker
   * @param text
   * @return
   */
  def formatText(text: String): String = {
    return utilitaire.textFormatting(text)
  }

  /**
   * ** utilitaireage et enrichissement du texte utilisé directement dans les templates
   * cette methode est aussi appelée depuis FreeMarker
   * @param key : String
   * @return
   */
  def getExtendedAttributes(key: String): String = {
    return this.mapExtendedAttribut.getOrElse(key, "").asInstanceOf[String]
  }
  /**
   *
   * <p>Les attributs étendus sont récupérés et stockés sous forme de clef, valeur.</p>
   * <p>POur les composants,il y a un traitement particulier pour le binding des données ainsi que pour la validation</p>
   * <p>Pourle binding on utilise le champ customControlId et pour la validation le champ customControlData</p>
   * <p>Il y a 2 types de binding possibles :</p>
   * <p> Bind = class1.class2.valeur:Int    // on bind le widget avec le champ décrit</p>
   * <p> bind=map(clef1,valeur1)  // on binde le contenu du widget avec un map existante, par exemple en localStorage.</p>
   * <p>pour la validation : validate=valeur1,valeur2,...</p>
   * <p>On récupère par défaut les attributs du composant dans le catalogue des composants et si
   * les attributs ne sont surchargés au niveau du widget en cours, ils seronts alors mis en table</p>
   *
   * <p>--------------------------------------------------------------------------------------------</p>
   *       <p>    Cas d'un composant : on récupère les attributs étendus dans la balise override</p>
   * <p>--------------------------------------------------------------------------------------------</p>
   *     <p>    control controlID="24" controlTypeID="com.balsamiq.mockups::Component" x="544" y="389" w="97" h="19" measuredW="97" measuredH="19" zOrder="5" locked="false" isInGroup="-1"></p>
   *   <p>   <controlProperties></p>
   *  <p>    <override controlID="0" x="0" y="0" w="97" h="19"></p>
   *   <p>     <text>Information</text></p>
   *   <p>  </override></p>
   *  <p>      <override controlID="-1"/></p>
   * <p>       <src>./assets/bootstrap.bmml#tb-label-info</src></p>
   *  <p>     </controlProperties></p>
   *  <p>    ----------------------------------------------------------------------------------------</p>
   *
   * @param e:Element
   */
  private def recuperationDesAttributsEtendusDuComposant(e: Element): scala.collection.mutable.Map[String, Object] = {
    var mapExtendedAttributDuComposant = scala.collection.mutable.Map[String, Object]()
    if (e.getChildren().size() != 0) {
      val controlProperties = e.getChild(cstControlProperties);
      if (controlProperties != null) {
        val cp = controlProperties.getChildren().toList;
        cp.foreach(propertie => {
          val elementName = propertie.getName().trim
          var elementValue = utilitaire.remplaceHexa(propertie.getText().trim) // on remplace les %xy par leur valeur ascii
          // -----------------------------------------------------------------------------------
          // Cas d'un composant : on récupère les attributs étendus dans la balise override 
          // ----------------------------------------------------------------------------------
          //    control controlID="24" controlTypeID="com.balsamiq.mockups::Component" x="544" y="389" w="97" h="19" measuredW="97" measuredH="19" zOrder="5" locked="false" isInGroup="-1">
          // <controlProperties>
          //  <override controlID="0" x="0" y="0" w="97" h="19">
          //    <text>Information</text>
          //  </override>
          //   <override controlID="-1"/>
          //   <src>./assets/bootstrap.bmml#tb-label-info</src>
          //  </controlProperties>
          // ------------------------------------------------------------------------------------

          if (elementName == cstSrc) { this.componentSrc = elementValue }
          else if (elementName == cstOverrideString) {
            val (idParam, overrideX, overrideY, overrideW, overrideH) = getOverrideProperties(propertie)
            // la tableOverrideParam contient les parametres overridé pour chaque widget du composant (pour les composants traités localement)
            tableOverrideParams += (idParam -> (overrideX, overrideY, overrideW, overrideH))
            val parametresOverrides = propertie.getChildren().toList
            parametresOverrides.foreach(param => {
              val paramName = utilitaire.remplaceHexa(param.getName().trim())
              val paramValue = utilitaire.remplaceHexa(param.getValue().trim())
              if (paramName == cstHref) { // c'est un lien ? si oui on génère un attribut fragment
                mapExtendedAttributDuComposant ++= traitementHref(paramValue)
              } else if (paramName == cstCustomID) {
                val id = if (paramValue != "") {
                  paramValue.trim
                } else {
                  ""
                }
                this.customId = id // custom ID

                if (id != ""  && CommonObjectForMockupProcess.mockupContext != null) { CommonObjectForMockupProcess.mockupContext.tableauDesIdsDesWidgets += this.customId } // on met en table le nom des formulaires

                mapExtendedAttributDuComposant += (cstCustomID -> id)
              } else if (paramName == cstCustomData) {
                val tableValue = paramValue.split(";").map(_.trim)
                tableValue.foreach(value => {
                  // id ne doit être renseigné que pour un container.
                  if (value.startsWith(cstBind) && value.contains("(") && value.contains(",")) {
                    val (retCode, structureMap) = traitementBinding.traitementMap(value)
                    if (retCode) { mapExtendedAttributDuComposant += (cstMapBinding -> structureMap) }

                  } // --------------------------------------------------------------------------------------------
                  // binding sur un champ. 
                  // L'attribut customId d'un champ est déduit du binding de champ (on prend le nom du champ
                  // Le template freemaker met en oeuvre cette norme. 
                  // --------------------------------------------------------------------------------------------
                  else if (value.startsWith(cstBind)) {
                    this.bind = value.substring(5) // le bind sera retraité après constitution du catalogue pour concatener le bind du pere.

                  } // Traitement de la validation des données
                  else if (value.startsWith(cstValidate)) {
                    val (retCode, variablesValidate) = mise_en_table_validation_du_champ(value)
                    if (retCode) { mapExtendedAttributDuComposant += (cstVariablesValidate -> variablesValidate) }
                  } // Les autres valeurs sont passées par défaut.
                  else { // on stocke la clef valeur directement
                    val tab2 = value.split("=")
                    if (tab2.size > 1) { mapExtendedAttributDuComposant += (tab2.head -> tab2.last) }
                  }

                })
              }

            })
          } // fin de traitement override

        })

      }
    } // fin de if 

    val src = this.componentSrc
    if (src.contains("#")) {
      val tab1 = src.split("#")
      componentName = tab1.last // nom du composant : tb_bagde
      repositoryName = tab1.head.split("/").last.replace(".", ":").split(":").head

      logBack.debug(utilitaire.getContenuMessage("mes5"), Array(componentName, repositoryName))
      // ------------------------------------------------------------------------------------------------------------
      // la liste va contenir le composant, on va alors recupérer les attributs de base 
      // et si les attributs ne sont surchargés au niveau du widget en cours, ils seronts alors mis en table
      // ------------------------------------------------------------------------------------------------------------
      val listFiltree = catalogDesComposants.catalogs.getOrElse(repositoryName, List.empty).filter(widget => widget.componentName == componentName)
      if (listFiltree != List.empty) {
        val composant = listFiltree.head
        this.mapIndice = composant.mapIndice // table des indices ID du composant
        this.componentXML = composant.element // Code XML du composant
        val attributsDuComposant = composant.mapExtendedAttribut
        val clefsDesAttributsDuComposant = attributsDuComposant.keys
        // on recopie dans le widget en cours les attributs du composant qui n'existent pas dans le widget
        clefsDesAttributsDuComposant.foreach(clefDuParametreDuComposant => {
          if (!mapExtendedAttributDuComposant.containsKey(clefDuParametreDuComposant)) {
            val valeurDuParametreDuComposant = attributsDuComposant.getOrElse(clefDuParametreDuComposant, "")
            logBack.debug("clef recuperee du composant=" + clefDuParametreDuComposant + ": " + valeurDuParametreDuComposant)
            mapExtendedAttributDuComposant += (clefDuParametreDuComposant -> utilitaire.remplaceHexa(valeurDuParametreDuComposant))
          }
        })
      }
    } else repositoryName = src.split("/").last
    logBack.debug("impression mapExtendedAttributes")
    mapExtendedAttributDuComposant.foreach(x => logBack.debug(x._1 + ":" + x._2))
    mapExtendedAttributDuComposant

  } // fin de getCOntrolProperties

  import scala.collection.mutable.Map
  /**
   * traitement de l'attribut href
   * On génère un attribut fragment
   * si le fichier n'est pas un fragment, on génère un attribut location
   * @param paramValue : String
   * @return
   */
  protected def traitementHref(paramValue: String): Map[String, Object] = {
    val mapFragment = Map[String, Object]()
    val bookmark = paramValue.split("&").head
    val fragment = IBalsamiqFreeMarker.globalContext.createFragment(bookmark)

    if (fragment != null) { mapFragment += (cstFragment -> fragment) }
    else {
      val location = IBalsamiqFreeMarker.globalContext.retrieveLocation(bookmark)
      mapFragment += (cstLocation -> location)
    }
    return mapFragment
  }
  /**
   *  *** nom du widget ou du composant ***
   * @return name: String
   */
  def getWidgetNameOrComponentName(): String = {
    val name = if (!isAComponent) controlTypeID
    else componentName
    name
  }
  /**
   *  récupération des attributes de la balise override
   * @param e
   * @return  (controlID, overrideX, overrideY, overrideW, overrideH)
   */
  protected def getOverrideProperties(e: Element): (String, String, String, String, String) = {
    // <override controlID="1" x="96" y="0" w="61" h="27">
    val controlID = e.getAttributeValue(cstControlID)
    val overrideX = e.getAttributeValue(cstX)
    val overrideY = e.getAttributeValue(cstY)
    val overrideW = e.getAttributeValue(cstW)
    val overrideH = e.getAttributeValue(cstH)

    (controlID, overrideX, overrideY, overrideW, overrideH)

  } // fin de getOverrideProperties

  /**
   * on regénère les attributs en remplaçant le controlID du widget dans le composant par le
   * custom attribut du widget du composant (table mapIndice mise à jour dans le chargement des catalogues de composants)
   *
   * @param e : Element
   */
  private def remplacementControlIDparCustomIdDuWidgetDuComponent(e: Element): Unit = {
    if (e.getChildren().size() != 0) {
      val controlProperties = e.getChild(cstControlProperties);
      if (controlProperties != null) {
        val cp = controlProperties.getChildren().toList;
        cp.foreach(propertie => {
          val elementName = propertie.getName().trim
          var elementValue = utilitaire.remplaceHexa(propertie.getText().trim) // on remplace les %xy par leur valeur ascii
          // *** on ne remplace que les attributs overridés ***
          if (elementName == cstOverrideString) {
            val (idParam, overrideX, overrideY, overrideW, overrideH) = getOverrideProperties(propertie)
            val params = propertie.getChildren().toList
            params.foreach(param => {
              if (param.getName.trim.toLowerCase() != cstCustomID.toLowerCase() && param.getName.trim.toLowerCase() != cstCustomData.toLowerCase()) {
                // mapIndice = (controleID -> customID) 
                val idDuWidgetDuComposant = mapIndice.getOrElse(idParam.trim, "")
                val paramName = if (idDuWidgetDuComposant != "") { idDuWidgetDuComposant + param.getName().trim().capitalize }
                else { param.getName().trim() }
                val paramValue = param.getValue().trim()
                mapExtendedAttribut += (paramName -> utilitaire.remplaceHexa(paramValue))
              }
            })

          }

        })
      }
    }

  }

  /**
   * on renseigne les token de validation dans la table tableauValidation
   * la syntaxe autorisée est :
   * validate=token1,token2=valeur2,token3
   * @param input
   * @return (true or false, Array of Token
   */
  def mise_en_table_validation_du_champ(input: String): (Boolean, java.util.ArrayList[Token]) = {
    val validate = cstValidate + "="
    val value = input.trim
    val tableauValidation = new java.util.ArrayList[Token]
    if (value.toLowerCase().startsWith(validate)) {
      val tokenDeValidation = value.substring(validate.length()).split(",")
      tokenDeValidation.foreach(token => {
        if (token.contains("=")) {
          val clef = token.split("=").head.trim
          val valeur = token.split("=").last.trim
          tableauValidation.add(new Token(clef, valeur))
        } else {
          tableauValidation.add(new Token(token.trim, ""))
        }
      })
      (true, tableauValidation)

    } else (false, null)

  }
}


