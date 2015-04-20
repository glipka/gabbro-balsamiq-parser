package fr.gabbro.balsamiq.parser.modelimpl
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.ArrayBuffer
import org.jdom2.Element
import java.io.File
import java.io.PrintStream
import org.jdom2.Document
import org.jdom2.input.SAXBuilder
import scala.collection.JavaConversions._
import scala.collection.mutable._
import scala.beans.BeanProperty
import freemarker.template.TemplateHashModel
import java.util.ArrayList
import java.util.Properties
import java.io.FileInputStream
import javax.xml.bind.DatatypeConverter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess
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

class CatalogDesComposants {
  val logBack = LoggerFactory.getLogger(this.getClass());
  var catalogs = Map[String, ArrayBuffer[ComponentBalsamiq]]() // Une clef par nom de catalogue
  val utilitaire = new Utilitaire

  /**
   * <p>methode appelée dans le module principal afin de charger les différents catalogue de composants</p>
   * <p>on récupère l'ensemble des fichiers suffixés par .bmml dans le répertoire en cours.</p>
   * <p>les catalogues sont stockés dans une hashMap, la clef étant le nom du catalogueet la valeur : le catalogue en lui même</p>
   * <p>cette table sert à récupérer les attributs de base d'un composant utilisé dans une maquette.</p>
   * <p>le nom du catalogue, ainsi que le nom du composant sont récupérés dans l'attribut src du widget basé sur ce composant</p>
   *
   * @param repertoireDesComposantsBootstrap
   * @return true or false
   */
  def chargementDesCatalogues(repertoireDesComposantsBootstrap: String): Boolean = {
    val catalogues = new File(repertoireDesComposantsBootstrap).listFiles.toList
    catalogues.foreach(fichierCatalog => {
      if (fichierCatalog.getName().endsWith(CommonObjectForMockupProcess.constants.balsamiqFileSuffix)) {
        val (ret1, catalogRetour) = chargementCatalogBootstrap(fichierCatalog.getPath())
        if (ret1) {
          val catalogName = fichierCatalog.getName().trim.replace(".", ":") // bug dans split(,)
          catalogs += (catalogName.split("/").last.split(":").head -> catalogRetour)
        } else { return false }
      }

    })
    true

  }

  /**
   *  traitement du fichier des composants bootstrap bmml
   * @param fichierCatalogDesComposantsBootstrap
   * @return
   */
  private def chargementCatalogBootstrap(fichierCatalogDesComposantsBootstrap: String): (Boolean, ArrayBuffer[ComponentBalsamiq]) = {
    var doc: Document = null;
    var mockup: Element = null;
    val builder = new SAXBuilder();

    logBack.info(utilitaire.getContenuMessage("mes3"), fichierCatalogDesComposantsBootstrap)
    try {
      doc = builder.build(fichierCatalogDesComposantsBootstrap);
      mockup = doc.getRootElement();
    } catch {
      case e: Exception =>
        logBack.info(utilitaire.getContenuMessage("mes61"), e.getMessage());
        return (false, null)
    }

    var catalog = traitementWidgetCatalogue(mockup.getChild(CommonObjectForMockupProcess.constants.controls))
    catalog.foreach(entry => {
      logBack.debug("entry={}", entry.componentName)
      entry.mapExtendedAttribut.foreach(x => logBack.debug("catalog:{} key:{} value:{}", entry.componentName, x._1, x._2))
    })
    logBack.info(utilitaire.getContenuMessage("mes4"), fichierCatalogDesComposantsBootstrap)
    return (true, catalog)
  }
  
  /**
   * on ne met ici en table que les groupes qui seront réutilisés comme composants dans les maquettes
   * <p> --------------------------------------------------------------------<p>
   * <p>   <control controlID="302" controlTypeID="__group__" x="0" y="148" w="77" h="24" measuredW="77" measuredH="24" zOrder="3" locked="false" isInGroup="-1"></p>
   * <p>     <controlProperties></p>
   * <p>     <controlName>tb-btn</controlName></p>
   * <p>     </controlProperties></p>
   * <p>    <groupChildrenDescriptors></p>
   * <p>       <control controlID="0" controlTypeID="com.balsamiq.mockups::Button" x="0" y="0" w="-1" h="-1" measuredW="77" measuredH="24" zOrder="0" locked="false" isInGroup="302"></p>
   *  <p>        <controlProperties></p>
   * <p>           <menuIcon>true</menuIcon></p>
   * <p>           <text>Action</text></p>
   * <p>        </controlProperties></p>
   * <p>      </control></p>
   *  <p>    </groupChildrenDescriptors></p>
   *  <p>  </control></p>
   * ___________________________________________________________________________________________________
   *
   *
   * @param controlsXML : Element
   * @return ArrayBuffer[ComponentBalsamiq]  on retourne le catalogue des composants
   */
  private def traitementWidgetCatalogue(controlsXML: Element): ArrayBuffer[ComponentBalsamiq] = {
    val controlXML = controlsXML.getChildren(CommonObjectForMockupProcess.constants.control).toList
    var traitement_groupe = false
    var catalog = new ArrayBuffer[ComponentBalsamiq]
    controlXML.foreach(elementXML => {
      var controlTypeID: String = elementXML.getAttributeValue(CommonObjectForMockupProcess.constants.controlTypeID)
      if (controlTypeID == CommonObjectForMockupProcess.constants.groupConstante) {
        val groupeEnCours = new ComponentBalsamiq(elementXML) // traitement du contrôle en cours
        catalog = catalog += traitementGroupeCatalogue(elementXML, groupeEnCours)
      }

    }) // fin de control.foreach 
    catalog
  }

  /**
  <p> * traitement d'un groupe</p>
  <p> * On récupère les enfants du groupe.</p>
  <p> * il faudra prévoir un traitement spécifique si l'enfant est lui même un groupe</p>
  <p> * on cumule les attributs étendus de chaque enfant du groupe</p>
  <p> * attention aux attributs des enfants ayant le même nom.</p>
  <p> * on met en table le controlId  de l'enfant et L'ID du widget du composant</p>
    * @param elementXML : Element
    * @param groupeEnCours : ComponentBalsamiq 
   * @return ComponentBalsamiq
   */
  private def traitementGroupeCatalogue(elementXML: Element, groupeEnCours: ComponentBalsamiq): ComponentBalsamiq = {
    val groupChildrenDescriptor = elementXML.getChild(CommonObjectForMockupProcess.constants.groupChildrenDescriptors);
    val nombreEnfants = groupChildrenDescriptor.getChildren(CommonObjectForMockupProcess.constants.control).size
    val enfants = groupChildrenDescriptor.getChildren(CommonObjectForMockupProcess.constants.control)
    enfants.foreach(enfant => {
      var controlTypeID: String = enfant.getAttributeValue(CommonObjectForMockupProcess.constants.controlTypeID)
      var controleID: String = enfant.getAttributeValue(CommonObjectForMockupProcess.constants.controlID).trim // n°enfant
      var (attributsDeLEnfant, customID) = recuperationDesAttributsEtendus(enfant, controleID)
      groupeEnCours.mapExtendedAttribut = groupeEnCours.mapExtendedAttribut ++ attributsDeLEnfant
      // on met en table le n° d'enfant et L'ID du widget du composant  
      if (customID != "") { groupeEnCours.mapIndice += (controleID -> customID) }
    })
    groupeEnCours

  }

  /**
  <p> *  *** récupération des propriétés du contrôle en cours ***</p>
  <p> * On fait une premire passe pour récupere dans une map l'ensemble des attributs (clef, valeur)</p>
  <p> * Si l'attribut customId est présent, on modifie la clef de chaque attribut en concaténant le customId avec le nom de l'attribut (capitalized)</p>
  <p> * modification 15 janvier : traitement de l'attribut markup d'un élément du composant</p>
  <p> * si le markup est positionné à true sur un élément du composant, on ne tient pas compte de l'attribut markup</p>
  <p> * et ceci pour éviter que l'attribut markup remonte au niveau du composant et que le composant ne soit pas généré par le moteur</p>
  <p> *</p>
   * @param e : Element
   * @param controleID : String
   * @return (map des Attributs, customId)
   */
  private def recuperationDesAttributsEtendus(e: Element, controleID: String): (scala.collection.mutable.Map[String, String], String) = {
    var mapExtendedAttributDuWidgetDuComposant = scala.collection.mutable.Map[String, String]()
    var customID = ""
    //FIXME ne pas stocker les propriétés des enfants markés en tant que markup
    if (e.getChildren().size() != 0) {
      val controlProperties = e.getChild(CommonObjectForMockupProcess.constants.controlProperties);
      if (controlProperties != null) {
        val cp = controlProperties.getChildren().toList;
        cp.foreach(propertie => {
          val elementName = propertie.getName().trim
          val elementValue = utilitaire.remplaceHexa(propertie.getText().trim) // on remplace les %xy par leur valeur ascii
          // *** le markup n'est pas mis en table ***
          if (elementName != CommonObjectForMockupProcess.constants.markup) {
            mapExtendedAttributDuWidgetDuComposant += (elementName -> elementValue)
          }
          if (elementName == CommonObjectForMockupProcess.constants.customID && elementValue.trim() != "") { customID = elementValue }
        }) // fin de cp.foreach
      }
    } // fin de if 
    // ------------------------------------------------------------------------------
    // *** si le customID est renseigné, on concatène la clef avec le customId ***
    // ------------------------------------------------------------------------------
    if (customID == "") { (mapExtendedAttributDuWidgetDuComposant, "") }
    else {
      val map1 = mapExtendedAttributDuWidgetDuComposant.map(x => {
        if ((x._1.toLowerCase() == CommonObjectForMockupProcess.constants.customData.toLowerCase()) || (x._1.toLowerCase() == CommonObjectForMockupProcess.constants.customID.toLowerCase())) { (x) }
        else {
          (customID + x._1.capitalize, x._2)
        }
      })
      (map1, customID)
    }
  } // fin de getCOntrolProperties

} 