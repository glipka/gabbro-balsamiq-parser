package fr.gabbro.balsamiq.parser.model.composantsetendus
// IbalsamiqFreeMarker - scala program to manipulate balsamiq sketches files an generate code with FreeMarker
// Version 1.0
// CopyCommonObjectForMockupProcess.constants.right (C) 2014 Georges Lipka
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
class WidgetInThisColumn (@BeanProperty var widgetType: String,
  @BeanProperty var readonly: String, @BeanProperty var widget: WidgetDeBase)

// pour simplification sur le traitement de dhmlxgrid, on garde les champs alignment,columntype,readonly,widget.
// le tableau widgetList sera utilisé pour les composants autre que dhtmlxgrid supportant nativement plusieurs composants
// dans une même colonne.
class ColumnDefinition(
  @BeanProperty var columnName: String,
  @BeanProperty var sort: String,
  @BeanProperty var width: String,
  @BeanProperty var alignment: String,
  @BeanProperty var widgetList: java.util.ArrayList[WidgetInThisColumn ],
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
  val text = CommonObjectForMockupProcess.constants.text

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
    val les3lignesDuTableau: List[String] = this.mapExtendedAttribut.getOrElse(CommonObjectForMockupProcess.constants.text, "").toString().split("\\n").toList
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
        sort = CommonObjectForMockupProcess.constants.bidirect
        columnName = column.substring(0, column.length() - 2)
      } else if (column.endsWith("v")) {
        sort = CommonObjectForMockupProcess.constants.desc
        columnName = column.substring(0, column.length() - 1)
      } else if (column.endsWith("^")) {
        sort = CommonObjectForMockupProcess.constants.asc
        columnName = column.substring(0, column.length() - 1)
      } else columnName = column.substring(0, column.length())

      // on récupère la largeur de la colonne (en %)
      // le dernier caractere peut inidquer l'alignement dans la cellule
      //{30L, 30R, 35, 25C}
      var largeurEtAlignement = if (numeroColonneEnCours < tableauDesLargeursDesColonnes.size) { tableauDesLargeursDesColonnes(numeroColonneEnCours) }
      else { (100 / tableauDesNomsDesColonnes.size).toString + CommonObjectForMockupProcess.constants.center } // largeur pas renseignée 
      // ------------------------------------------------------------------------------------------------------
      // *** si la largeur se termine par center, left ou right => on extrait la largeur et l'alignement ***
      // ------------------------------------------------------------------------------------------------------
      if (largeurEtAlignement.toUpperCase.endsWith(CommonObjectForMockupProcess.constants.center) || // align CommonObjectForMockupProcess.constants.center
        largeurEtAlignement.toUpperCase.endsWith(CommonObjectForMockupProcess.constants.left) || // align CommonObjectForMockupProcess.constants.left
        largeurEtAlignement.toUpperCase.endsWith(CommonObjectForMockupProcess.constants.right) // align Right
        ) {
        width = largeurEtAlignement.substring(0, largeurEtAlignement.length() - 1).trim
        alignment = largeurEtAlignement.substring(largeurEtAlignement.length() - 1, largeurEtAlignement.length()) // on prend le dernier caractère
      } else {
        width = largeurEtAlignement.substring(0, largeurEtAlignement.length()).trim
      }
      numeroColonneEnCours += 1

      //on verifie que la largeur en % est numerique et que le total n'est pas > à 100%
      if (!width.forall(_.isDigit)) { logBack.error(utilitaire.getContenuMessage("mes19"), this.controlTypeID) }
      else { largeurTotaleEnpourcentage += width.toInt }
      if (largeurTotaleEnpourcentage > 100) { logBack.error(utilitaire.getContenuMessage("mes20"), this.controlTypeID) }
      // les positions départ et fin sont calculées à partir des pourcentages de la largeur des colonnes en rapport avec la largeur de la table en pixels.
      var positionDepart = positionEnCoursDeLaColonne
      var positionFin = positionDepart + (this.w.toInt * width.toInt) / 100 - 1
      positionEnCoursDeLaColonne = positionFin + 1
      // met en table la colonne 
      logBack.debug("traitementcolonne n°:" + numeroColonneEnCours + " positionDepart=" + positionDepart + "px positionFin=" + positionFin + "px width en pixels" + this.w + "px")
      val columnDefinition = new ColumnDefinition(this.formatText(columnName), sort, width, alignment, new java.util.ArrayList[WidgetInThisColumn ], positionDepart, positionFin,null,null,null)
      tableauDesColonnes.add(columnDefinition)

    })
    (CommonObjectForMockupProcess.constants.columns, enrichissementDuTableau(tableauDesColonnes))

  } // fin de la methode 
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
         colonne.readonly=readOnly
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
          colonne.widgetList.add(new WidgetInThisColumn (typeDeWidget, readOnly, widget))
         // println("n°de colonne %s ajout widget %s %s".format(position,typeDeWidget,readOnly))
        }
      }) // fin de tableau_des_widgets_fils.foreach(widge

      tableauEnrichi.add(colonne)
      position += 1
    })
    tableauEnrichi
  }

  /**
   * return type of widget and readOnly true or False
   * @param widgetFils : WidgetDeBase
   * @return (typeOfWidget,ReadOnly)
   */
  def getTypeAndReadOnly(widgetFils: WidgetDeBase): (String, String) = {
    val state = widgetFils.mapExtendedAttribut.getOrElse(CommonObjectForMockupProcess.constants.state, "") // on récupère l'état du composant
    var typeDeColonne = text
    var readonly = CommonObjectForMockupProcess.constants.falseString

    // on determine un type générique qui sera interprété par le template freeMarker. 
    widgetFils.controlTypeID match {
      case CommonObjectForMockupProcess.constants.numericStepper => { // NUmerifc 
        if (state == CommonObjectForMockupProcess.constants.disabled || state == CommonObjectForMockupProcess.constants.disabledSelected) {
          typeDeColonne = CommonObjectForMockupProcess.constants.numeric // type de colonne = ron
          readonly = CommonObjectForMockupProcess.constants.trueString
        } else {
          typeDeColonne = CommonObjectForMockupProcess.constants.numeric // type de colonne = edn
          readonly = CommonObjectForMockupProcess.constants.falseString // 
        }
      }

      case CommonObjectForMockupProcess.constants.textInput => { // Text

        if (state == CommonObjectForMockupProcess.constants.disabled || state == CommonObjectForMockupProcess.constants.disabledSelected) {
          typeDeColonne = text // type =ro
          readonly = CommonObjectForMockupProcess.constants.trueString
        } else {
          typeDeColonne = text // type="ed"
          readonly = CommonObjectForMockupProcess.constants.falseString

        }
      }
      case CommonObjectForMockupProcess.constants.label => { // Text
        typeDeColonne = text // type =ro
        readonly = CommonObjectForMockupProcess.constants.trueString

      }
      case CommonObjectForMockupProcess.constants.textArea => { // TextArea
        typeDeColonne = CommonObjectForMockupProcess.constants.textareaShort
        readonly = CommonObjectForMockupProcess.constants.falseString
      }
      case CommonObjectForMockupProcess.constants.checkBox => { // checkbox
        typeDeColonne = CommonObjectForMockupProcess.constants.checkboxShort // type=ch
        if (state == CommonObjectForMockupProcess.constants.disabled || state == CommonObjectForMockupProcess.constants.disabledSelected) {
          readonly = CommonObjectForMockupProcess.constants.trueString
        } else {
          readonly = CommonObjectForMockupProcess.constants.falseString
        }
      }
      case CommonObjectForMockupProcess.constants.radioButton => { // RadioButton
        typeDeColonne = CommonObjectForMockupProcess.constants.radiobuttonShort // type=ra
        if (state == CommonObjectForMockupProcess.constants.disabled || state == "ra") {
          readonly = CommonObjectForMockupProcess.constants.trueString
        } else {
          readonly = CommonObjectForMockupProcess.constants.falseString
        }
      }
      case CommonObjectForMockupProcess.constants.comboBox => { // combobox
        typeDeColonne = CommonObjectForMockupProcess.constants.combobox // type=combo
      }
      case CommonObjectForMockupProcess.constants.listHTML => { // editable select box
        typeDeColonne = CommonObjectForMockupProcess.constants.list //type=co
        readonly = CommonObjectForMockupProcess.constants.falseString
      }
      // -------------- Trouver le bon composant -------------------------
      case "com.balsamiq.mockups::Listx" => { // not editable select box
        typeDeColonne = CommonObjectForMockupProcess.constants.list //type=coro
        readonly = CommonObjectForMockupProcess.constants.trueString
      }

      case CommonObjectForMockupProcess.constants.colorPicker => { // ColorPicker
        typeDeColonne = CommonObjectForMockupProcess.constants.colorPicker // type=cp
      }
      case CommonObjectForMockupProcess.constants.dateChooser => { // datePicker
        typeDeColonne = CommonObjectForMockupProcess.constants.calendar //type=dhxCalendar
      }
      case CommonObjectForMockupProcess.constants.link => { // lien 
        typeDeColonne = CommonObjectForMockupProcess.constants.linkShort // type=link 
      }
      case CommonObjectForMockupProcess.constants.image => { // image
        typeDeColonne = CommonObjectForMockupProcess.constants.img // type=img
      }

      case CommonObjectForMockupProcess.constants.icon => { // iconShort
        typeDeColonne = CommonObjectForMockupProcess.constants.iconShort // type=iconShort
      }

      case _ => logBack.info(utilitaire.getContenuMessage("mes18"), widgetFils.controlTypeID)

    }

    (typeDeColonne, readonly)
  }

}