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

import java.io.File
import java.io.FileWriter
import scala.collection.mutable.ArrayBuffer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.beans.BeanProperty
import fr.gabbro.balsamiq.parser.modelimpl.GlobalContext
import fr.gabbro.balsamiq.parser.modelimpl.Utilitaire
import fr.gabbro.balsamiq.parser.modelimpl.MenuItem

// -------------------------------------------------------------------------- 
// cette classe est instanciée dans le module principal IbalsamiqFreeMarker
//  la tables des menuItemsATraiter tableauDesVariables stockera l'ensemble des menuItemsATraiter référencés la méthode bind=
//  et servira à la géneration des DTO 
//
// traitement du binding entre zones 
// Le champ CustomControl ID doit commencer par bind=obj1.obj2.champ1:Int 
//     les types permis sont :
//     datetime, date, Int, long,double, string 
//     les listes se terminent par * (pas encore implémenté)
//     Exemple bind=personne/adresse:Int
//
//
// le champ CustomControl ID peut être aussi du type : bind=map1("nom","adresse") 
// pour binder un champ à une map en localstorage par exemple.Ce type de binding 
// est utilisé conjointement avec les templates javascript js_xxxxx afin de coder l'alimentationn
// de l'objet. (execmple mis en place avec le template select)
// 
// La génération des DTO se fait par 2 templates Freemarker : class et field 
// On gnénère un fichier par écran
// --------------------------------------------------------------------------
class TraitementMenu(sessionBalsamiq: GlobalContext) {
  // var tableauDesVariables = new ArrayBuffer[MenuItem]
  protected val logBack = LoggerFactory.getLogger(this.getClass());
  protected val utilitaire = new Utilitaire
  var url = ""
  var usecaseName = ""

  // ----------------------------------------------------------------------------------------------
  // methode appelée dans la classe WidgetDeBase, 
  // on va enrichir la table "tableauDesVariables" qui sera utilisée pour générer le code source
  // des classes utilisées dans le binding.
  // le traitement des objets "class1.class2.nomObjet" se fait dans la fonction traitement branche 
  // ----------------------------------------------------------------------------------------------

  def mise_en_table_classes_menu_item(input: String, usecase: String, url: String): Unit = {
    val value = input.trim
    var nomDesObjets = value

    // si le containerPere est un container qui contient une valeur de bind valide  
    // on concatène a
    if (nomDesObjets.contains(CommonObjectForMockupProcess .engineProperties.usecaseSeparator)) {
      val menusItemsATraiter = nomDesObjets.split(CommonObjectForMockupProcess .engineProperties.usecaseSeparator)
      if (menusItemsATraiter.size > 1) {
        // menusItemsATraiter.init va contenir la hierarchie des objets
        sessionBalsamiq.tableauDesMenuItems = traitement_branche(sessionBalsamiq.tableauDesMenuItems, menusItemsATraiter, usecase, url)
      } else { (logBack.error(utilitaire.getContenuMessage("mes8"),nomDesObjets)) }
    } else { // un suel objet =>  c'est une classe
      sessionBalsamiq.tableauDesMenuItems = traitement_branche(sessionBalsamiq.tableauDesMenuItems, Array(nomDesObjets), usecase, url)
    }

  } // fin traitment_custom_controlId 
  // --------------------------------------------------------------------------- 
  //  
  // l'expression "a.b.c" est splitée par "."
  // si on ne trouve pas l'element "a" dans la branche en cours
  //   si la taille du tableau des variables > 1  
  //     mise en table du 1er élément qui est une classe.
  //     traitmement itératif de la queue de la table "b.c" pour renseigner le champ children de l'element de la table en cours
  //   sinon l'element est un champ, on récupère son type (split par ":")
  //       le type de chmap 
  // sinon  on se repositionne sur l'element de la table en cours et traitement itératif de "b.c" pour renseigner le champ children de l'element en cours
  // ---------------------------------------------------------------------------
  private def traitement_branche(brancheEnCours: ArrayBuffer[MenuItem], menuItemsATraiter: Array[String], usecase: String, url: String): ArrayBuffer[MenuItem] = {
    //  champ non trouve dans la branche en cours 
    var fieldName = menuItemsATraiter.head.trim
    // le champ en cours n'existe pas dans la branche
    if (!brancheEnCours.exists(field => { (field.itemName == fieldName) })) {
      // s'il y a plusieurs menuItemsATraiter on crée le 1er champ dans la table et on relit les enfants
      if (menuItemsATraiter.size > 1) {
        val newField = new MenuItem(fieldName, new ArrayBuffer[MenuItem](), url, usecaseName)
        newField.children = traitement_branche(newField.children, menuItemsATraiter.tail, usecase, url)
        brancheEnCours += newField
        brancheEnCours
      } // un seul champ on le cree
      else {
        val newField = new MenuItem(fieldName, new ArrayBuffer[MenuItem](), url, usecaseName)
        brancheEnCours += newField
        brancheEnCours
      }
    } else { // le champ existe dans la branche
      // on se repositionne sur l'objet déjà défini afin d'enrichir les fils
      // on vérifie qye l'objet déjà défini est du même type

      val brancheEnrichie = brancheEnCours.map(field => {
        if (field.itemName == fieldName) {
          if (menuItemsATraiter.size > 1) { field.children = traitement_branche(field.children, menuItemsATraiter.tail, usecase, url) }
        }

        field
      })
      brancheEnrichie

    }

  }

}  // fin de la classe TraitementBinding