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

import net.htmlparser.jericho._
import java.net.URL
import scala.collection.JavaConversions._
import java.io.FileInputStream
import scala.collection.immutable.List
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.util.Properties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import fr.gabbro.balsamiq.parser.service.TMoteurAnalyseJericho
import fr.gabbro.balsamiq.parser.modelimpl.Utilitaire
class MoteurAnalyseJericho(moteurTemplatingFreeMarker: MoteurTemplatingFreeMarker, utilitaire: Utilitaire) extends TMoteurAnalyseJericho {
  var (ok, counterClef) = recuperationDesClefsDeTraduction()

  // ---------------------------------------------------------------------------------------------
  // Les clefs de traduction sont sauvegardées dans un fichier properties
  // on recharge les clefs de traduction depuis ce fichier properties dans 2 hashTables :
  // clef <=> valeur et valeur <=> clef
  // ======================================================
  // format de la clef de traduction :
  // ======================================================
  // ecran.form1.label1.key.1=valeur1
  // le 1er champ est le nom de l'écrran, le dernier champ est le compteur unique
  // les champs intermédiaires sont variables
  // ----------------------------------------------------------------------------------------------
  def recuperationDesClefsDeTraduction(): (Boolean, Int) = {
    var ok = true
    tableDesClefsValeursDeTraduction.clear
    tableDesValeursClefsDeTraduction.clear
    var clefMaxi = 0;
    try {

      val props = new Properties();
      val ficPropertyName = CommonObjectForMockupProcess.generationProperties.srcI18nFilesDir + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedi18nFileName
      props.load(new InputStreamReader(new FileInputStream(ficPropertyName), CommonObjectForMockupProcess.constants.utf8));

      val enuKeys = props.keys().toList
      enuKeys.foreach(clef => {
        val valeur = props.getProperty(clef.toString()).trim
        val clefNumerique = (clef.toString().split("\\.").last.toInt) // bypass Key
        val usecaseReference = (clef.toString().split("\\.").head.toString)
        val ecranReference = (clef.toString().split("\\.").tail.head.toString) // 2eme élément de la liste
        tableDesClefsValeursDeTraduction += (clef.toString -> valeur)
        tableDesValeursClefsDeTraduction += ((valeur, ecranReference, usecaseReference) -> clef.toString())
        if (clefNumerique > clefMaxi) clefMaxi = clefNumerique // va servir pour generer les nouvelles clefs

      })

    } catch {
      // si le fichier des clefs n'existe pas, il sera créé

      case ex: Exception =>

    }
    (ok, clefMaxi)

  }
  // ------------------------------------------------------------------------------
  // ecriture dans le fichier properties des libelles à traduire
  // On lit l'ensemble des clef que l'on trie par ordre descendant
  // on récupere la valeur de chaque clef  
  // puis on écrit le tout dans un buffer.
  // ------------------------------------------------------------------------------
  def sauvegardeDesClefsDeTraduction(): Unit = {
    val msgsEntrySet = tableDesClefsValeursDeTraduction.keys.toList.sortWith((x, y) => x < y)
    var fileWriter: java.io.FileWriter = null
    val sbuf = new StringBuilder
    msgsEntrySet.foreach(key => {
      val value = tableDesClefsValeursDeTraduction.getOrElse(key, " ")
      sbuf.append(key).append("=").append(value).append("\r\n");
    })
    val ficPropertyName = CommonObjectForMockupProcess.generationProperties.srcI18nFilesDir + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedi18nFileName
    utilitaire.ecrire_fichier(ficPropertyName, sbuf.toString())

  }
  // ------------------------------------------------------------------------
  // On recopie les nouvelles clefs dans les fichiers pays traduits
  // ------------------------------------------------------------------------
  def traitementDeltaDesFichiersDeTraductionDesDifferentsPays: Boolean = {
    val propsLocal = new Properties();
    val ficPropertyLocal = CommonObjectForMockupProcess.generationProperties.srcI18nFilesDir + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedi18nFileName

    propsLocal.load(new InputStreamReader(new FileInputStream(ficPropertyLocal), CommonObjectForMockupProcess.constants.utf8));
    val clefDuFichierDeProprietesNonLocalise = propsLocal.keys().toList
    // pour chaque langue à traiter
    CommonObjectForMockupProcess.generationProperties.i18nLocales.foreach(country => {
      val ficPropertyPays = CommonObjectForMockupProcess.generationProperties.srcI18nFilesDir + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedi18nFileName.split("\\.").head + "_" + country + "." + CommonObjectForMockupProcess.generationProperties.generatedi18nFileName.split("\\.").last
      val propsPays = new Properties();
      val filePays = new File(ficPropertyPays)
      if (filePays.exists()) { propsPays.load(new InputStreamReader(new FileInputStream(ficPropertyPays), CommonObjectForMockupProcess.constants.utf8)); }
      // on verifie que chaque clef du fichier properties non localisé existe dans le fichier properties du Pays
      clefDuFichierDeProprietesNonLocalise.foreach(clefnonlocalisee => {
        if (propsPays.getProperty(clefnonlocalisee.toString) == null) {
          propsPays.setProperty(clefnonlocalisee.toString, propsLocal.getProperty(clefnonlocalisee.toString.toString()))
        }
      })
      val filewriter = new OutputStreamWriter(new FileOutputStream(ficPropertyPays), CommonObjectForMockupProcess.constants.utf8)
      propsPays.store(filewriter, "generatedByGabbro")
    })
    true

  }
  // -------------------------------------------------------------------------------------------------
  // appel récursif de la liste des elements
  // si l'element a des fils, traitement récursif des fils
  // pour chaque élément, on récupère le texte à traduire.
  // On utilise la hashMap (valeur, clef) pour vérifier si cette valeur est déjà en table
  // Si oui => on récupère la valeur, sinon, on crée cette valeur dans les 2 hashmap (valeur clef et clef valeur)
  // la clef de traduction est recupérée par le template Freemarker CommonObjectForMockupProcess.constants.templateClefDeTraduction
  // puis on un replace du segment à modifier
  // pour chaque valeur à traduire on détermine la hiérarchie de l'élément.
  // si l'2lément à traduire est dans un formulaire, on l'indique dans la clef de tradution.
  // La 1ere valeur de la clef est toujours le nom du fichier en cours de traitement, puis éventuelllement l'ID du fomulaire
  // contenant l'éelemnt à traduire puis le type d'élement et enfin un compteur unique.
  // exemple :
  //           testgl01.header.47=header
  //           testgl01.i.52=second
  //           testgl01.ins.53=row
  //           testgl01.personne.label.56=Adresse
  //           testgl01.personne.label.57=Personne
  // --------------------------------------------------------------------------------------------------
  def extractMessages(elements: List[net.htmlparser.jericho.Element], outputDocument: net.htmlparser.jericho.OutputDocument): Unit = {
    elements.foreach(element => {
      // remplacement des attributs
      val textExtractor = element.getTextExtractor();
      val mapAttributes = traitementAttributsElement(element)
      if (mapAttributes.size > 0) { // des clefs à traduire ??
        outputDocument.replace(element.getAttributes, mapAttributes)
      }

      val childElements = element.getChildElements().toList
      if (childElements != null && childElements.size() > 0) {
        extractMessages(childElements, outputDocument);
      } else {
        var valeurATraduire = textExtractor.toString(); // texte à traduire
        var key = traduction_valeur(valeurATraduire, element, "")
        if (key != "") {
          val segment = element.getContent()
          val (ret1, source1, sourcejavsacript1, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateClefDeTraduction, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.key, key))
          val (ret2, source2, sourcejavascript2, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateClefDeTraduction, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.key, key))
          val clefDeTraduction = source1 + source2
          outputDocument.replace(segment, clefDeTraduction);
        }

      } // if1
    })
  }
  // ---------------------------------------------------------------------------------------------------------
  // *** traitement des attributs de l'élément en cours ***
  // si l'attibut est dans la table des atributs à traduire, on renseigne sa valeur traduite dans une hasMAP
  // Jericho donne la possibnlité de modifier les attributs d'un segment par la fonction replace
  // ----------------------------------------------------------------------------------------------------------
  import scala.collection.mutable.Map
  def traitementAttributsElement(element: Element): Map[String, String] = {
    val attributes = if (element.getAttributes != null) element.getAttributes.toList else List[Attribute]()
    val elementName = element.getStartTag.getName
    var mapAttributes = scala.collection.mutable.Map[String, String]()
    attributes.foreach(attribute => {
      // le nom de l'attribut est dans la liste des attributs à traduire ? 
      if (List(attribute.getName).intersect(CommonObjectForMockupProcess.generationProperties.attributesToProcessI18n).size > 0) {
        val key = traduction_valeur(replaceSpecialChar(attribute.getValue), element, attribute.getName)
        if (key != "") {
          val (_, source1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateClefDeTraduction, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.key, key), (CommonObjectForMockupProcess.constants.isAttribute, "true"))
          val (_, source2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateClefDeTraduction, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.key, key), (CommonObjectForMockupProcess.constants.isAttribute, "true"))
          val clefDeTraduction = source1 + source2
          mapAttributes += (attribute.getName -> clefDeTraduction.trim)
        } else { mapAttributes += (attribute.getName -> replaceSpecialCharValue(attribute.getValue)) }
      } else { // l'attribut n'est pas modifé on le repasse tel quel 
        mapAttributes += (attribute.getName -> replaceSpecialCharValue(attribute.getValue))
      }
    })
    mapAttributes
  }
  // ---------------------------------------------------------------------
  // *** Traduction d'une valeur ***
  // ---------------------------------------------------------------------
  def traduction_valeur(valeur: String, element: Element, attributeName: String): String = {
    var valeurATraduire = if (valeur != null) { replaceSpecialCharValue(valeur.trim()) } else { "" }
    if (valeurATraduire.length() > 0 && !valeurATraduire.forall(_.isDigit)) { //if1
      if (!tableDesValeursClefsDeTraduction.contains(valeurATraduire, CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement)) {
        counterClef += 1 // compteur unicité des clefs
        val table_hierachie = getHierarchie(element);
        val table_formulaire = table_hierachie.filter(element => {
          val x1 = element.getStartTag.getName
          List(element.getStartTag.getName).intersect(CommonObjectForMockupProcess.generationProperties.htmlContainerListForI18nGeneration).size > 0
        })

        var container = ""
        table_formulaire.foreach(formulaire => {
          val id = formulaire.getAttributeValue(CommonObjectForMockupProcess.constants.id)
          if (id != null && id != "") { container = container + id + "." }
          else { container = container + formulaire.getStartTag().getName + "." }
        })
        if (container.endsWith(".")) { container = container.substring(0, container.size - 1) } // on supprime le .
        // on appelle le template CommonObjectForMockupProcess.constants.templateBuildTraductionKey afin de générer la clef du fichier properties
        var isAttribute = if (attributeName != "") { true } else { false }
        val (_, source6, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateBuildTraductionKey, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.container, container), (CommonObjectForMockupProcess.constants.isAttribute, isAttribute.toString), (CommonObjectForMockupProcess.constants.currentTag, table_hierachie.head.getStartTag.getName.toLowerCase()), (CommonObjectForMockupProcess.constants.index, counterClef.toString), (CommonObjectForMockupProcess.constants.attributName, attributeName))
        val (_, source7, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateBuildTraductionKey, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.container, container), (CommonObjectForMockupProcess.constants.isAttribute, isAttribute.toString), (CommonObjectForMockupProcess.constants.currentTag, table_hierachie.head.getStartTag.getName.toLowerCase()), (CommonObjectForMockupProcess.constants.index, counterClef.toString), (CommonObjectForMockupProcess.constants.attributName, attributeName))
        var key = replaceSpecialChar(source6.trim + source7.trim)
        tableDesClefsValeursDeTraduction += (key -> valeurATraduire);
        tableDesValeursClefsDeTraduction += ((valeurATraduire, CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement) -> key)
        return key

      } else { // la clef existe déjà
        val clef = tableDesValeursClefsDeTraduction.getOrElse((valeurATraduire, CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement), "");
        return clef
      }
    }
    return ""
  }
  // --------------------------------------------------------------------------
  // *** remplacement des caractères spéciaux dans les fichiers properties***
  // ---------------------------------------------------------------------------
  def replaceSpecialChar(valeur: String): String = {
    //   0X22 = double quote
    valeur.replace("\n", "").replace("\t", "").replace(":", "-").replace("0x22", "")
  }
  // --------------------------------------------------------------------------
  // *** remplacement des caractères spéciaux dans les fichiers properties***
  // ---------------------------------------------------------------------------
  def replaceSpecialCharValue(valeur: String): String = {
    //   0X22 = double quote
    valeur.replace("\n", "").replace("\t", "").replace("0x22", "").replace(":", "¨")
  }
  // -------------------------------------------------------

  // --------------------------------------------------------------------------------------------------------
  // récupération de la hiérarchie de l'element en cours pour retrouver facilement le widget dans la page.
  // --------------------------------------------------------------------------------------------------------
  def getHierarchie(element: Element): ArrayBuffer[Element] = {
    var tableHierarchie = new ArrayBuffer[Element]()
    tableHierarchie += element
    if (element.getParentElement() != null) { tableHierarchie = tableHierarchie ++ getHierarchie(element.getParentElement) }
    tableHierarchie

  }
  // --------------------------------------------------------------------------------------------------------
  // Lecture du fichier html extraction de l'ensemble des elements de la page.
  // SI l'element contient du texte à traduire, on vérifie dans la table "valeur clef" si l'élement
  // est déjà renseigné. Si c'est le cas, on remplace le texte par la valeur de la clef de traduction  (Appel du
  // template freemarker pour remplacer la clef de traduction).
  // si l'élement n'est pas présent dans la hashTab, on le rajoute
  //
  // ---------------------------------------------------------------------------------------------------------

  def traductHtmlFile(fileName: String, subDirectory: String, templateDirOut: String): Unit = {
    var directoryName = templateDirOut
    val sourceHTML = utilitaire.getEmplacementFichierHtml(fileName, directoryName)
    val fileHTML = new File(sourceHTML)
    val source = new Source(new InputStreamReader(new FileInputStream(fileHTML), CommonObjectForMockupProcess.constants.utf8));
    source.fullSequentialParse();
    val outputDocument = new OutputDocument(source);
    val childElements = source.getChildElements().toList
    extractMessages(childElements, outputDocument); // on met à jour le fichier HTML
    val fichierHtmlTraduit = utilitaire.getEmplacementFichierHtml(fileName, directoryName)
    // création du fichier
    val rep1 = fichierHtmlTraduit.replace(System.getProperty("file.separator"), "/").split("/").init.mkString(System.getProperty("file.separator"))
    utilitaire.createRepostoriesIfNecessary(rep1)
    val fileWriter = new FileWriter(new File(fichierHtmlTraduit))
    outputDocument.writeTo(fileWriter);
    fileWriter.close();

  }

} // fin de la classe