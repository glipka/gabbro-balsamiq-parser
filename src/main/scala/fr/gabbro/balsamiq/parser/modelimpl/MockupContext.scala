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
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementPreserveSection
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
// -----------------------------------------------
// fragmentName : nom du fragment 
// ficName : Nom du fichier 
// ucName : usecaseName 
// location: location
// typeOfFragment: panel, popup, ...
// -----------------------------------------------
class Fragment(@BeanProperty var fragmentName: String, @BeanProperty var ficName: String, @BeanProperty var ucName: String, @BeanProperty var location: Location, @BeanProperty var typeOfFragment: String) {
}
// ----------------------------------------------
// classname: nom de la classe 
// instancename : variable pour l'instance générée
// widget: 
// instanceCode: code instanciation de la classe 
// isAFormulaire : true or false
// shortPath : shorPath
// ----------------------------------------------
class FormulaireCode(@BeanProperty var classname: String, @BeanProperty var instanceName: String, @BeanProperty var widget: WidgetDeBase, @BeanProperty var instanceCode: String, @BeanProperty var isAFormulaire: Boolean, @BeanProperty var shortPath: String)
// -----------------------------------------
// location : 
// shortPath :
// restUrl: adresse rest 
// ------------------------------------------
class Location(@BeanProperty var location: String, @BeanProperty var shortPath: String, @BeanProperty var restUrl: String) {
  val utilitaire = new Utilitaire
}
// ---------------------------------------------------------------
// content 
// shortPath
// ----------------------------------------------------------------
class ItemVar(@BeanProperty var content: String, @BeanProperty var shortPath: String)
/**
 * <p>classe expostion de données au niveau de l'ecran</p>
 * <p>les widgets enrichissent les données au moment de la mise en table</p>
 * <p>les données sont alors accessibles pour l'ensemble des traitements</p>
 * <p>C'est une zone de communication transverse, qui est utilisée par exemple pour récupérer</p>
 * <p>les liens à inclure dans la page HTML</p>
 * <p>un fragment est une sous partie de l'écran.</p>
 * @author fra9972467
 *
 */
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
  /**
   * <p>methode appelée par freeMarker pour mettre en table le code source des classes java.</p>
   * <p>subPackageName est le sous package dans lequel sera générée la classe</p>
   *
   * @param className
   * @param classCode
   * @param subPackageName
   */
  def setCodeClasse(className: String, classCode: String, subPackageName: String): Unit = {
    tableDesCodesDesClassesJavaouScala += (className.trim, subPackageName.trim) -> classCode
  }
  /**
   * <p>écriture du code java ou scala genéré pour la page html en cours</p>
   * <p>la table tableDesCodesDesClassesJavaouScala contientl le code l'ensemble des classes java</p>
   * <p> modif le 28 avril 2015 : ajout récupération des preserves sections au moment d'écrire le fichier
   * @return
   */
  def ecritureDuCodeJaveOuScala: Boolean = {
    val keysSet = tableDesCodesDesClassesJavaouScala.keys
    keysSet.foreach(classNameAliasName => {
      val className = classNameAliasName._1
      val subPackage = classNameAliasName._2
      val ficJavaName = utilitaire.getNomDuFichierCodeJavaOuScala(classNameAliasName) // contient le nom dela classe et le nom du sous package sous forme de tuple
      utilitaire.ecrire_fichier(ficJavaName, CommonObjectForMockupProcess.mockupContext.tableDesCodesDesClassesJavaouScala.getOrElse(classNameAliasName, ""))

    })
    true
  }
  /**
   * <p>récupération des fragments par leur type</p>
   * <p>cette méthode est appelée depuis les templates freemarker</p>
   *
   * @param typeDeFragment : Panel, Popup, ...
   * @return ArrayList[Fragment]
   */
  
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
