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
import org.slf4j.Logger
import org.jdom2.Element
import scala.collection.JavaConversions._
import scala.beans.BeanProperty
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementBinding
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess
import fr.gabbro.balsamiq.parser.modelimpl.CatalogDesComposants //class ColumnDefinitionNewVersion(@BeanProperty var columnName: String, @BeanProperty var sort: String, @BeanProperty var width: String, @BeanProperty var alignment: String, @BeanProperty var columnType: String, @BeanProperty var readonly: String, @BeanProperty var widget: WidgetDeBase, var beginningPositionRelativeToContainer: Int, var endPositionRelativeToContainer: Int)
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
class WidgetInThisColumn(@BeanProperty var widgetType: String,
  @BeanProperty var readonly: String, @BeanProperty var widget: WidgetDeBase)

// pour simplification sur le traitement de dhmlxgrid, on garde les champs alignment,columntype,readonly,widget.
// le tableau widgetList sera utilisé pour les composants autre que dhtmlxgrid supportant nativement plusieurs composants
// dans une même colonne.
class ColumnDefinition(
  @BeanProperty var columnName: String,
  @BeanProperty var sort: String,
  @BeanProperty var width: String,
  @BeanProperty var bootstrapWidth: String,
  @BeanProperty var alignment: String,
  @BeanProperty var widgetList: java.util.ArrayList[WidgetInThisColumn],
  var beginningPositionRelativeToContainer: Int, // mémorisation offset début de la colonne utilisé pour détecter les widgets inclus dans cette colonne
  var endPositionRelativeToContainer: Int,
  @BeanProperty var columnType: String, // utilisé seulement pour compatibilité dhtmlxgrid
  @BeanProperty var readonly: String, // utilisé seulement pour compatibilité dhtmlxgrid
  @BeanProperty var widget: WidgetDeBase) // 1er widget de la colonne que l'on garde pour compatibilité avec dhtmlxgrid

/**
 * @author fra9972467
 *
 */
class Datagrid(id_interne: Int, groupe_en_cours: WidgetDeBase, elementXML: Element, traitementBinding: TraitementBinding, catalogDesComposants: CatalogDesComposants, isAcomponent: Boolean) extends WidgetDeBase(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, isAcomponent) {
  val text = cstText

  /* (non-Javadoc)
   * @see fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase#enrichissementParametres(java.lang.String)
   *   Récupération des parametres d'un datatable 
   * on ne récupère que le nom des colonnes ainsi que leur largeur en % 
   * on ne se sert pas des données dans le composant pour les types des colonnes
   * Ces types seront déduits des widgets déposés dans les colonnes.
   * le contenu du texte du widget est : 
   * 
   * Name\r(job title) ^, Age ^v, Nickname, Employee v
   * \r\r\r\r
   * {30L, 30R, 35, 25C}
   * 
   * 
  */
  override def enrichissementParametres(param1: String): (String, Object) = {
    // récupération des infos du datatable depuis l'attribut text du datatable. 
    val les3lignesDuTableau: List[String] = this.mapExtendedAttribut.getOrElse(cstText, "").toString().split("\\n").toList
    // on récupère les noms des colonnes (ainsi que leur tri) dans la 1ere ligne.
    val tableauDesNomsDesColonnes = les3lignesDuTableau.head.split(",").map(_.trim)
    // {30L, 30R, 35, 25C}
    // la 3eme ligne (donc la dernière) commence par { et se termine par } c'est la 3eme ligne du contenu de la table. 
    // on supprime les caractères { et } et on splite pour récupérer en table la largeur de chaque colonne (avec alignement)
    val tableauDesLargeursDesColonnes = if (les3lignesDuTableau.last.startsWith("{") && les3lignesDuTableau.last.endsWith("}"))
      les3lignesDuTableau.last.substring(1, les3lignesDuTableau.last.length - 1).split(",")
    else Array.empty[String]
    var numeroColonneEnCours = 0
    val tableauDesColonnes = new java.util.ArrayList[ColumnDefinition]
    var largeurTotaleEnpourcentage = 0
    var largeurTotaleinDouzieme = 0

    // -----------------------------------------------------------------------------------
    // Name\r(job title) ^, Age ^v, Nickname, Employee v
    // on parcourt le tableau des noms des colonnes et pour chaque colonne on 
    // récupère la largeur de la colonne dans la table (
    // -----------------------------------------------------------------------------------
    var positionEnCoursDeLaColonne = 0
    tableauDesNomsDesColonnes.foreach(column => {
      var sort = " "
      var columnName = " "
      var width = " "
      var alignment = " "

      if (column.endsWith("^v") || column.endsWith("v^")) {
        sort = cstBidirect
        columnName = column.substring(0, column.length() - 2)
      } else if (column.endsWith("v")) {
        sort = cstDesc
        columnName = column.substring(0, column.length() - 1)
      } else if (column.endsWith("^")) {
        sort = cstAsc
        columnName = column.substring(0, column.length() - 1)
      } else columnName = column.substring(0, column.length())

      // on récupère la largeur de la colonne (en %)
      // le dernier caractere peut inidquer l'alignement dans la cellule
      //{30L, 30R, 35, 25C}
      var largeurEtAlignement = if (numeroColonneEnCours < tableauDesLargeursDesColonnes.size) { tableauDesLargeursDesColonnes(numeroColonneEnCours) }
      else { (100 / tableauDesNomsDesColonnes.size).toString + cstCenter } // largeur pas renseignée 
      // ------------------------------------------------------------------------------------------------------
      // *** si la largeur se termine par center, left ou right => on extrait la largeur et l'alignement ***
      // ------------------------------------------------------------------------------------------------------
      if (largeurEtAlignement.toUpperCase.endsWith(cstCenter) || // align center
        largeurEtAlignement.toUpperCase.endsWith(cstLeft) || // align left
        largeurEtAlignement.toUpperCase.endsWith(cstRight) // align Right
        ) {
        width = largeurEtAlignement.substring(0, largeurEtAlignement.length() - 1).trim
        alignment = largeurEtAlignement.substring(largeurEtAlignement.length() - 1, largeurEtAlignement.length()) // on prend le dernier caractère
      } else {
        width = largeurEtAlignement.substring(0, largeurEtAlignement.length()).trim
      }

      //on verifie que la largeur en % est numerique et que le total n'est pas > à 100%
      if (!width.forall(_.isDigit)) { logBack.error(utilitaire.getContenuMessage("mes19"), this.controlTypeID) }
      else { largeurTotaleEnpourcentage += width.toInt }
      if (largeurTotaleEnpourcentage > 100) { logBack.error(utilitaire.getContenuMessage("mes20"), this.controlTypeID) }
      // les positions départ et fin sont calculées à partir des pourcentages de la largeur des colonnes en rapport avec la largeur de la table en pixels.
      var positionDepart = positionEnCoursDeLaColonne
      var positionFin = positionDepart + (this.w.toInt * width.toInt) / 100 - 1
      positionEnCoursDeLaColonne = positionFin + 1
      // met en table la colonne  
      // taille de colonne en douzieme : pour la dernière colonne c'est le complément de 12 à la largeur totale en douzieme des précédentes colonnes
      var tailleColonneEnDouzieme = if (numeroColonneEnCours < (tableauDesNomsDesColonnes.size - 1)) { calculTailleColonneEnDouzieme(width.toInt) } else { CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns - largeurTotaleinDouzieme }
      // on vérifie que la taille totale en douzieme ne depasse pas 12, si c'est le cas, on fait un complément à 12 de la largeur de colonne.  
      if ((largeurTotaleinDouzieme + tailleColonneEnDouzieme) > CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns) { tailleColonneEnDouzieme = CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns - largeurTotaleinDouzieme }
      largeurTotaleinDouzieme += tailleColonneEnDouzieme
      logBack.debug("traitementcolonne n°:" + numeroColonneEnCours + 1 + " positionDepart=" + positionDepart + "px positionFin=" + positionFin + "px width en pixels" + this.w + "px")
      val columnDefinition = new ColumnDefinition(this.formatText(columnName), sort, width, tailleColonneEnDouzieme.toString, alignment, new java.util.ArrayList[WidgetInThisColumn], positionDepart, positionFin, null, null, null)
      tableauDesColonnes.add(columnDefinition)
      numeroColonneEnCours += 1

    })
    if (largeurTotaleEnpourcentage != 100  ) { logBack.error(utilitaire.getContenuMessage("mes20"), this.controlTypeID) }
    (cstColumns, enrichissementDuTableau(tableauDesColonnes))

  } // fin de la methode  
  /**
   * *** conversion du pourcentage en douzieme pour la largeur des colonnes de la table ***
   * taille colonne en douzieme = largeur en pourcentage * 12 /100 ( on fait + 1 si le reste est supérieur à 12/2)
   * Exposition de la liste des objets
   * @return
   */
  private def calculTailleColonneEnDouzieme(widthInPercentage: Int): Int = {
    //    val x=  (widthInPercentage * CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns) % 100
    //   println("valeur de x = %s   widht %s".format(x,widthInPercentage))
    var tailleCelluleEnDouzieme = if (((widthInPercentage * CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns) % 100) < 50) { widthInPercentage * CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns / 100 } else { (widthInPercentage * CommonObjectForMockupProcess.engineProperties.boostrapNumberOfColumns / 100) + 1 }
    if (tailleCelluleEnDouzieme == 0)  {tailleCelluleEnDouzieme=1}
    tailleCelluleEnDouzieme
  }
  /**
   * *** table des colonnes pour un datagrid qui sera utilisée directement dans les templates ***
   * Exposition de la liste des objets
   * @return
   */
  def getColumns(): java.util.ArrayList[ColumnDefinition] = {
    val (x, tableau) = this.enrichissementParametres("")
    tableau.asInstanceOf[java.util.ArrayList[ColumnDefinition]]
  }
  /**
   * Récupération tableau des colonnes enrichi par le type de colonnes
   * on parcourt la liste des widgets fils pour récupérer leur type ainsi que l'attribut readonly
   *
   * @param tableau_des_colonnes
   * @return tableau_des_colonnes enrichi
   */
  private def enrichissementDuTableau(tableau_des_colonnes: java.util.ArrayList[ColumnDefinition]): java.util.ArrayList[ColumnDefinition] = {
    // on récupère le header de chaque colonne
    // var tableau_des_colonnes = getColonneDefinition().toList
    val tableauEnrichi = new java.util.ArrayList[ColumnDefinition]
    // on récupère la table des fils du widget en cours 
    val tableau_des_widgets_fils = this.tableau_des_fils.sortWith((x, y) => x.positionDansLeConteneur < y.positionDansLeConteneur) // tableau des widgets Fils
    var position = 0
    tableau_des_colonnes.foreach(colonne => {
      // traitement spécifique à dhtmxgrid, qui ne sait générer nativement qu'un widget par colonne 
      if (position < tableau_des_widgets_fils.size) {
        colonne.widget = tableau_des_widgets_fils(position)
        val (typeDeWidget, readOnly) = getTypeAndReadOnly(colonne.widget)
        //      println("numero de colonne %s, columnType: %s, readonly :%s".format(colonne,typeDeWidget,readOnly))
        colonne.columnType = typeDeWidget
        colonne.readonly = readOnly
      }
      // ---------------------------------------------------------------------------------------------------------
      // pour chaque colonne, on récupère l'ensemble des widgets qui sont positionnés dans la colonne. 
      // on se sert pour chaque des positions relatives des widgets par rapport au container (donc la table)
      // ainsi que des postitions relatives de la colonne par rapport à la table.
      // les widgets inclus sont stcokés dans la table widgetList 
      // ---------------------------------------------------------------------------------------------------------
      tableau_des_widgets_fils.foreach(widget => {
        // on ne s'occupe que des abscisses, il faut donc faire attention que les widgets soient bien alignés dans le container 
        // pour chaque colonne on balaie systématiquement l'ensemble des widgets du container. 
        if (widget.xRelative >= colonne.beginningPositionRelativeToContainer && widget.xRelative < colonne.endPositionRelativeToContainer) {
          val (typeDeWidget, readOnly) = getTypeAndReadOnly(widget)
          colonne.widgetList.add(new WidgetInThisColumn(typeDeWidget, readOnly, widget))
          // println("n°de colonne %s ajout widget %s %s".format(position,typeDeWidget,readOnly))
        }
      }) // fin de tableau_des_widgets_fils.foreach(widge

      tableauEnrichi.add(colonne)
      position += 1
    })
    tableauEnrichi
  }

  /**0
   * return type of widget and readOnly true or False
   * @param widgetFils : WidgetDeBase
   * @return (typeOfWidget,ReadOnly)
   */
  def getTypeAndReadOnly(widgetFils: WidgetDeBase): (String, String) = {
    val state = widgetFils.mapExtendedAttribut.getOrElse(cstState, "") // on récupère l'état du composant
    var typeDeColonne = text
    var readonly = cstFalseString

    // on determine un type générique qui sera interprété par le template freeMarker. 
    widgetFils.controlTypeID match {
      case `cstNumericStepper` => { // NUmerifc 
        if (state == cstDisabled || state == cstDisabledSelected) {
          typeDeColonne = cstNumeric // type de colonne = ron
          readonly = cstTrueString
        } else {
          typeDeColonne = cstNumeric // type de colonne = edn
          readonly = cstFalseString // 
        }
      }

      case `cstTextInput` => { // Text

        if (state == cstDisabled || state == cstDisabledSelected) {
          typeDeColonne = text // type =ro
          readonly = cstTrueString
        } else {
          typeDeColonne = text // type="ed"
          readonly = cstFalseString

        }
      }
      case `cstLabel` => { // Text
        typeDeColonne = text // type =ro
        readonly = cstTrueString

      }
      case `cstTextArea` => { // TextArea
        typeDeColonne = cstTextareaShort
        readonly = cstFalseString
      }
      case `cstCheckBox` => { // checkbox
        typeDeColonne = cstCheckboxShort // type=ch
        if (state == cstDisabled || state == cstDisabledSelected) {
          readonly = cstTrueString
        } else {
          readonly = cstFalseString
        }
      }
      case `cstRadioButton` => { // RadioButton
        typeDeColonne = cstRadiobuttonShort // type=ra
        if (state == cstDisabled || state == "ra") {
          readonly = cstTrueString
        } else {
          readonly = cstFalseString
        }
      }
      case `cstComboBox` => { // combobox
        typeDeColonne = cstCombobox // type=combo
      }
      case `cstListHTML` => { // editable select box
        typeDeColonne = cstList //type=co
        readonly = cstFalseString
      }
      // -------------- Trouver le bon composant -------------------------
      case "com.balsamiq.mockups::Listx" => { // not editable select box
        typeDeColonne = cstList //type=coro
        readonly = cstTrueString
      }

      case `cstColorPicker` => { // ColorPicker
        typeDeColonne = cstColorPicker // type=cp
      }
      case `cstDateChooser` => { // datePicker
        typeDeColonne = cstCalendar //type=dhxCalendar
      }
      case `cstLink` => { // lien 
        typeDeColonne = cstLinkShort // type=link 
      }
      case `cstImage` => { // image
        typeDeColonne = cstImg // type=img
      }

      case `cstIcon` => { // iconShort
        typeDeColonne = cstIconShort // type=iconShort
      }

      case _ => logBack.info(utilitaire.getContenuMessage("mes18"), widgetFils.controlTypeID)

    }

    (typeDeColonne, readonly)
  }

}