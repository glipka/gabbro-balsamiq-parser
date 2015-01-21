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
import fr.gabbro.balsamiq.parser.modelimpl.CatalogDesComposants

class ColumnDefinition(@BeanProperty var columnName: String, @BeanProperty var sort: String, @BeanProperty var width: String, @BeanProperty var alignment: String, @BeanProperty var columnType: String, @BeanProperty var readonly: String, @BeanProperty var widget: WidgetDeBase)

class Datagrid(id_interne: Int, groupe_en_cours: WidgetDeBase, elementXML: Element, traitementBinding: TraitementBinding, catalogDesComposants: CatalogDesComposants, isAcomponent: Boolean) extends WidgetDeBase(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants, isAcomponent) {

  // ---------------------------------------------------------------------------------
  // Récupération des parametres d'un datatable 
  // on ne récupère que le nom des colonnes ainsi que leur largeur en % 
  // on ne se sert pas des données dans le composant pour les types des colonnes
  // Ces types seront déduits des widgets déposés dans les colonnes.
  // le contenu du texte du widget est : 
  // 
  // Name\r(job title) ^, Age ^v, Nickname, Employee v
  // \r\r\r\r
  // {30L, 30R, 35, 25C}
  // ----------------------------------------------------------------------------------

  override def enrichissementParametres(param1: String): (String, Object) = {
    val les3lignesDuTableau: List[String] = this.mapExtendedAttribut.getOrElse(CommonObjectForMockupProcess.constants.text, "").toString().split("\\n").toList
    val tableauDesNomsDesColonnes = les3lignesDuTableau.head.split(",").map(_.trim)
    // {30L, 30R, 35, 25C}
    // la ligne commence par { et se termine par } c'est la 3eme ligne du contenu de la table. 
    val tableauDesLargeursDesColonnes = if (les3lignesDuTableau.last.startsWith("{") && les3lignesDuTableau.last.endsWith("}"))
      les3lignesDuTableau.last.substring(1, les3lignesDuTableau.last.length - 1).split(",")
    else Array.empty[String]
    var numeroColonneEnCours = 0
    val tableauDesColonnes = new java.util.ArrayList[ColumnDefinition]
    var largeurTotaleEnpourcentage = 0

    // -----------------------------------------------------------------
    // Name\r(job title) ^, Age ^v, Nickname, Employee v
    // -----------------------------------------------------------------

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
      var largeuretAlignement = if (numeroColonneEnCours < tableauDesLargeursDesColonnes.size) { tableauDesLargeursDesColonnes(numeroColonneEnCours) }
      else { (100 / tableauDesNomsDesColonnes.size).toString + "C" } // largeur pas renseignée 
      if (largeuretAlignement.toUpperCase.endsWith(CommonObjectForMockupProcess.constants.center) || // align CommonObjectForMockupProcess.constants.center
        largeuretAlignement.toUpperCase.endsWith(CommonObjectForMockupProcess.constants.left) || // align CommonObjectForMockupProcess.constants.left
        largeuretAlignement.toUpperCase.endsWith(CommonObjectForMockupProcess.constants.right) // align Right
        ) {
        width = largeuretAlignement.substring(0, largeuretAlignement.length() - 1).trim
        alignment = largeuretAlignement.substring(largeuretAlignement.length() - 1, largeuretAlignement.length()) // on prend le dernier caractère
      } else {
        width = largeuretAlignement.substring(0, largeuretAlignement.length()).trim
      }

      numeroColonneEnCours += 1

      //on verifie que la largeur en % est numerique et que le total n'est pas > à 100%
      if (!width.forall(_.isDigit)) { logBack.error(utilitaire.getContenuMessage("mes19"),this.controlTypeID) }
      else { largeurTotaleEnpourcentage += width.toInt }
      if (largeurTotaleEnpourcentage > 100) { logBack.error(utilitaire.getContenuMessage("mes20"),this.controlTypeID) }

      val columnDefinition = new ColumnDefinition(this.formatText(columnName), sort, width, alignment, "", "", null)
      tableauDesColonnes.add(columnDefinition)

    })
    val x = enrichissementDuTableau(tableauDesColonnes)
    (CommonObjectForMockupProcess.constants.columns, enrichissementDuTableau(tableauDesColonnes))

  } // fin de la methode 
  // ------------------------------------------------------------------------------------------------
  // *** table des colonnes pour un datagrid qui sera utilisée directement dans les templates ***
  // Exposition de la liste des objets
  // ------------------------------------------------------------------------------------------------
  def getColumns(): java.util.ArrayList[ColumnDefinition] = {
    val (x, tableau) = this.enrichissementParametres("")
    tableau.asInstanceOf[java.util.ArrayList[ColumnDefinition]]
  }
  // ----------------------------------------------------------------------------------------------
  // Récupération tableau des colonnes enrichi par le type de colonnes
  // on parcourt la liste des widgets fils pour récupérer leur type ainsi que l'attribut readonly
  // ----------------------------------------------------------------------------------------------
  private def enrichissementDuTableau(tableau_des_colonnes: java.util.ArrayList[ColumnDefinition]): java.util.ArrayList[ColumnDefinition] = {
    // on récupère le header de chaque colonne
    // var tableau_des_colonnes = getColonneDefinition().toList
    val tableauEnrichi = new java.util.ArrayList[ColumnDefinition]
    val tableau_des_widgets_fils = this.tableau_des_fils
    var position = 0

    val text = CommonObjectForMockupProcess.constants.text

    tableau_des_colonnes.foreach(colonne => {
      var typeDeColonne = text
      var readonly = CommonObjectForMockupProcess.constants.falseString
      if (position < tableau_des_widgets_fils.size) {
        val widgetFils = tableau_des_widgets_fils(position)
        colonne.widget = widgetFils // on renseigne le widget du composant de la colonne
        val state = widgetFils.mapExtendedAttribut.getOrElse(CommonObjectForMockupProcess.constants.state, "")
        // test du type d'objet 
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

          case _ => logBack.info(utilitaire.getContenuMessage("mes18"),widgetFils.controlTypeID)

        }

        colonne.columnType = typeDeColonne
        colonne.readonly = readonly
      } else colonne.columnType = text

      tableauEnrichi.add(colonne)
      position += 1
    })

    tableauEnrichi

  }

}