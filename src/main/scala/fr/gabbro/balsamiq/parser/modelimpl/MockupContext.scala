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
import scala.collection.JavaConversions._
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess
import fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase
import fr.gabbro.balsamiq.parser.model.composantsetendus.DirectoryFile
// classe expostion de données au niveau de l'ecran 
// les widgets enrichissent les données au moment de la mise en table 
// les données sont alors accessibles pour l'ensemble des traitements 
// C'est une zone de communication transverse, qui est utilisée par exemple pour récupérer 
// les liens à inclure dans la page HTML
// un fragment est une sous partie de l'écran. 
class Fragment(@BeanProperty var fragmentName: String, @BeanProperty var ficName: String, @BeanProperty var ucName: String, @BeanProperty var location: Location, @BeanProperty var typeOfFragment: String) {
}
class FormulaireCode(@BeanProperty var classname: String, @BeanProperty var widget: WidgetDeBase, @BeanProperty var instanceCode: String, @BeanProperty var isAFormulaire: Boolean, @BeanProperty var shortPath: String)
class Location(@BeanProperty var location: String, @BeanProperty var shortPath: String, @BeanProperty var restUrl: String) {
  val utilitaire = new Utilitaire
}
class ItemVar(@BeanProperty var content: String, @BeanProperty var shortPath: String)

class MockupContext() {
  var global_max_width: Double = 0
  var global_max_height: Double = 0
  @BeanProperty var location: Location = _ // emplacement des jsp générés. 
  @BeanProperty var links = new java.util.ArrayList[DirectoryFile]()
  @BeanProperty var itemsVars = new java.util.ArrayList[ItemVar]()
  @BeanProperty var firstLevelObject = new java.util.ArrayList[FormulaireCode]() // contient les sources pour instancier les classes du DTO dans le contrôleur
  @BeanProperty var bindedForms = new java.util.ArrayList[FormulaireCode]() // contient les sources pour instancier les formulaires
  @BeanProperty var fragments = new java.util.ArrayList[Fragment]() // table des fragments
  @BeanProperty var tableDesCodesDesClassesJavaouScala = Map[(String, String), String]() // table des classes : nom de la classe, nom du sous package,code de la classe
  val utilitaire = new Utilitaire
  // ----------------------------------------------------------------------------------
  // methode appelée par freeMarker pour retourner le code source des classes java. 
  // subPackageName est le sous package dans lequel sera générée la classe
  // ----------------------------------------------------------------------------------
  def setCodeClasse(className: String, classCode: String, subPackageName: String): Unit = {
    tableDesCodesDesClassesJavaouScala += (className, subPackageName) -> classCode
  }
  // -------------------------------------------------------------------------------------------
  // ecriture du coce java ou scala genéré pour la page html en cours
  // la table tableDesCodesDesClassesJavaouScala contientl le code l'ensemble des classes java
  // -------------------------------------------------------------------------------------------
  def ecritureDuCodeJaveOuScala: Boolean = {
    val keysSet = tableDesCodesDesClassesJavaouScala.keys
    keysSet.foreach(classNameAliasName => {
      val ficJavaName = utilitaire.getNomDuFichierCodeJavaOuScala(classNameAliasName) // contient le nom dela classe et le nom du sous package
      utilitaire.ecrire_fichier(ficJavaName, CommonObjectForMockupProcess.mockupContext.tableDesCodesDesClassesJavaouScala.getOrElse(classNameAliasName, ""))
    })
    true
  }
  // récupération des fragments par leur type
  // cette méthode est appelée depuis les templates freemarker
  def getFragmentsByType(typeDeFragment: String): java.util.ArrayList[Fragment] = {
    val listeDesFragments = new java.util.ArrayList[Fragment]
    fragments.foreach(fragment => {
      if (fragment.typeOfFragment.toLowerCase() == typeDeFragment.toLowerCase()) {
        listeDesFragments.add(fragment)
      }
    })
    listeDesFragments
  }
}