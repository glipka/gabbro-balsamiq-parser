package fr.gabbro.balsamiq.parser.modelimpl
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
import java.util.ArrayList
import scala.beans.BeanProperty
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.ArrayBuffer
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementFormatageSourceJava
import fr.gabbro.balsamiq.parser.service.serviceimpl.MoteurTemplatingFreeMarker
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementPreserveSection
import scala.collection.JavaConversions._
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
import fr.gabbro.balsamiq.parser.service.serviceimpl.MoteurAnalyseJericho
import fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase
import org.slf4j.Logger
import org.slf4j.LoggerFactory
// Zone commune freemarker enrichie par l'ensemble du traitements des maquettes
//  va servir pour générer le menu par exemple.  
//  
//  
//  
class NomDesFichiersJavascript(@BeanProperty var path: String, @BeanProperty var useCase: String, @BeanProperty var fileName: String)
// ---------------------------------------------------------------
// content 
// shortPath
// ----------------------------------------------------------------
class ItemVar(@BeanProperty var content: String, @BeanProperty var shortPath: String)
class GlobalContext() {
  val utilitaire = new Utilitaire
  var moteurJericho: MoteurAnalyseJericho = null
  var globalSourceMenu = new StringBuilder() // va contenir le code HTML du menu
  var moteurTemplatingFreeMarker: MoteurTemplatingFreeMarker = _
  // modif le 22/4/15 par gl Itemsvars est une Map dont la clef est le usecase,ecran principla, fragmentName, identifiabt unique et la valeur itemsVar
  // cette table va servir à lister des listes des itemsvars pour un ecran principal et pour l'ensemble de ses fragments.
  var itemsVars = Map[(String, String, String, String), ItemVar]() // pour stocker les itemsvar
  @BeanProperty var firstLevelObject = new java.util.ArrayList[FormulaireCode]() // contient les sources pour instancier les classes du DTO dans le contrôleur  
  @BeanProperty var tableDesCodesDesClassesJavaouScala = Map[(String, String, String, String), String]() // table des classes : clef de la map: (useCaseName, nomduMockup,nom du sous package,nom de la classe),code de la classe
  @BeanProperty var tableDesContainersDesFragments = Map[(String, String, String), WidgetDeBase]() // table  : clef de la map: (useCaseName, nomduMockup,nom du fragment),widget du container

  // modif le 22/4/15 par georges 
  // bindedForms est une Map dont la clef est le useCase, l'ecran principal et le nom du fragment ainsi qu'un identifiant unique
  // pour un écran principal, le nom du fragment est vide 
  // cette table va servir à lister des listes des formulaires pour un ecran et pour l'ensemble de ses fragments.
  var bindedForms = Map[(String, String, String, String), FormulaireCode]() // contient les sources pour instancier les formulaires
  @BeanProperty var paths = new java.util.ArrayList[Location]() // contient la localisation des fichiers JSP générés.
  @BeanProperty var mapSourcesJavascript = scala.collection.mutable.Map[(String, String, String), String]() // clef = (usecase,filename,section) value = code javascript
  val logBack = LoggerFactory.getLogger(this.getClass());
  val gblTableTrace = ArrayBuffer[(String, String, String, String, String, String,String)]() // (bmml,templateID,componant,mes1,description,gravity,usecase)

  /**
   * @param bmml
   * @param templateID
   * @param componant
   * @param mes1
   * @param description
   * @param gravity
   */
  def addTraceToReport(bmml: String, templateID: String, componant: String, mes1: String, description: String, gravity: String): Unit = {
    var mes = s"bmml ${bmml} composant ${componant} templateId ${templateID} ${mes1}  ${description} "
    val trace = (bmml, templateID, componant, mes1, description, gravity,CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement)
    gblTableTrace += trace
   // gravity match {
   //   case "error" => logBack.error(mes)
   //   case "debug" => logBack.debug(mes)
   //   case _ => logBack.info(mes)
   // }

  }
  /**
   * <p>methode appelée par freeMarker pour mettre en table le code source des block java ou scala.</p>
   * <p> les blocks java ou scala seront récupérés dans les différents templates </p>
   * <p>subPackageName est le sous package dans lequel sera générée la classe</p>
   *
   * @param usecaseName
   * @param mockupName
   * @param subPackageName
   * @param blockName
   * @param classCode
   * @param subPackageName
   */
  def registerCodeBlock(usecaseName: String, mockupName: String, subPackageName: String, blockName: String, classCode: String): Unit = {
    // table des classes : clef de la map: (useCaseName, nomduMockup,nom du sous package,nom de la classe),code de la classe
    tableDesCodesDesClassesJavaouScala += (usecaseName, mockupName, subPackageName, blockName) -> classCode
  }

  /**
   * @param usecaseName
   * @param mockupName
   * @param subPackageName
   * @param className
   * @param subPackageName
   * @return content of java code for the className
   */
  def retrieveCodeBlock(usecaseName: String, mockupName: String, blockName: String, subPackageName: String): String = {
    val codeJavaOrScala = tableDesCodesDesClassesJavaouScala.getOrElse((usecaseName, mockupName, subPackageName, blockName), "")
    codeJavaOrScala
  }
  /**
   * @param usecaseName
   * @param mockupName
   *
   * @param className
   * @param subPackageName
   * @return content of java code for the className
   */
  def retrieveCodeBlock(usecaseName: String, mockupName: String): String = {
    val tableResultat = tableDesCodesDesClassesJavaouScala.filter {
      case ((usecaseName1, mockupName1, subPackageName, blockName), content) => (usecaseName1 == usecaseName && mockupName1 == mockupName)

    }.map {
      case ((usecaseName1, mockupName1, subPackageName, blockName), content) => content
    }
    val resultat = tableResultat.toList.mkString("")
    resultat
  }

  /**
   * @param usecaseName
   * @param mockupName
   * @param subPackageName
   * @param className
   * @return true or false
   */
  def generateImportFor(usecaseName: String, mockupName: String, className: String, subPackageName: String): Boolean = {
    val codeJavaOrScala = tableDesCodesDesClassesJavaouScala.getOrElse((usecaseName, mockupName, subPackageName, className), "")
    if (codeJavaOrScala.trim.size > 0) { true }
    else { false }
  }

  def getNomDuFichierCodeJavaOuScala(nomDuUseCaseEnCoursDeTraitement: String, filenameAliasName: Tuple2[String, String]): String = {
    var ficJava = ""
    if (nomDuUseCaseEnCoursDeTraitement != "") {
      ficJava = CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath + System.getProperty("file.separator") + nomDuUseCaseEnCoursDeTraitement + System.getProperty("file.separator") + filenameAliasName._2 + System.getProperty("file.separator") + filenameAliasName._1.capitalize + "." + CommonObjectForMockupProcess.generationProperties.languageSource
    } else {
      ficJava = CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath + System.getProperty("file.separator") + filenameAliasName._2 + System.getProperty("file.separator") + filenameAliasName._1.capitalize + "." + CommonObjectForMockupProcess.generationProperties.languageSource
    }
    ficJava.replace("\\", "/").trim

  }

  /**
   * @param usecaseNmae
   * @param className
   * @return Array of String.
   */
  def listOfSubPackageInJavaOrScalaCode(usecaseNameAFiltrer: String, mockupNameAFiltrer: String): java.util.ArrayList[String] = {
    val tableDesPackages = new java.util.ArrayList[String]
    tableDesCodesDesClassesJavaouScala.foreach {
      case ((usecaseName, mockupName, subPackageName, className), code) => // la clef est un tuple de 4 parties : usecaseName,mockupName,subpackageName,className
        if (usecaseName == usecaseNameAFiltrer && mockupName == mockupNameAFiltrer) { tableDesPackages.add(subPackageName) } //  recupération non du package
    }
    tableDesPackages

  }

  /**
   *  le code javascript est mis dans une hashMap afin de concaténer le code de l'ecran et des fragments
   * on met en cache le code javascript : Le code des fragments est cumulé avec le code du fichier
   * rajout le 16/1/15 de la section de code pour hiérarchiser le code javascript
   * cette procédure est appelée par les templates javascript freemarker
   * @param fileName
   * @param codeJavascript
   * @param isAfragment
   * @param section
   */
  def registerJavascriptSection(codeJavascript: String, isAfragment: String, section: String): Unit = {
    //FIXME deduire isAFragment  coté scala ?
    // clef= (usecase,nom de fichier,section)
    val key =
      if (isAfragment == cstTrueString) {
        (CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement, CommonObjectForMockupProcess.ecranContenantLeSegment, section)
      } else {
        (CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, section)
      }
    // on met le code source du fichier avant le code source des fragments
    val codeDuFichier = if (isAfragment != cstTrueString) codeJavascript + mapSourcesJavascript.getOrElse(key, "")
    else mapSourcesJavascript.getOrElse(key, "") + codeJavascript
    mapSourcesJavascript.update(key, codeDuFichier)

  }
  /**
   * Get a javascript section beforehand cached thanks to registerJavascriptSection function
   * @param useCase
   * @param fileName
   *  * @param section name of cached section
   * @return code of the section
   * //TODO deduire du code scala ?
   */
  def getJavascriptSection(useCase: String, fileName: String, section: String): String = {
    mapSourcesJavascript.getOrElse((useCase, fileName, section), "")

  }
  /**
   * Get a javascript section beforehand cached thanks to registerJavascriptSection function
   * @param section name of cached section
   * @return code of the section
   * //TODO deduire du code scala ?
   */
  def getJavascriptSection(section: String): String = {
    // clef de la Map = (usecase,nom de fichier,section)
    var sourceDeLaSection = ""
    mapSourcesJavascript.foreach {
      case ((clefUsecaseName, clefFicname, clefSection), code) => {
        if (clefSection.trim == section.trim) { // filtre sur la section en cours
          sourceDeLaSection += code // on cumule le code de la section
        }
      }
    }
    sourceDeLaSection
  }
  /**
   * Get a javascript section beforehand cached thanks to registerJavascriptSection function
   * @param section name of cached section
   * @return code of the section
   * //TODO deduire du code scala ?
   */
  def getJavascriptSection(usecaseName: String, section: String): String = {
    var sourceDeLaSection = ""
    mapSourcesJavascript.foreach {
      case ((clefUsecaseName, clefFicname, clefsection), code) => {
        if (clefsection.trim == section.trim && clefUsecaseName.trim == usecaseName.trim) { // filtre sur la section en cours et le usecaseName
          sourceDeLaSection += code // on cumule le code de la section
        }
      }
    }
    sourceDeLaSection
  }

  /**
   * Exposition à freemarker des noms des fichiers javascript générés. (sans les doublons)
   * @return  ArrayList[NomDesFichiersJavascript]
   */
  def getJavascripts(): ArrayList[NomDesFichiersJavascript] = {
    val tableauDesNomsDesFichiersJavascript = new ArrayList[NomDesFichiersJavascript]()
    mapSourcesJavascript.foreach {
      case ((useCase, fileName, section), code) =>
        val path = if (useCase != "") { CommonObjectForMockupProcess.generationProperties.srcJavascriptFilesDirWithOutPrefix + System.getProperty("file.separator") + useCase + System.getProperty("file.separator") + fileName + cstSuffixDesFichiersJavaScript }
        else { CommonObjectForMockupProcess.generationProperties.srcJavascriptFilesDirWithOutPrefix + System.getProperty("file.separator") + fileName + cstSuffixDesFichiersJavaScript }
        // verification qu'il n'y a pas des doublons (nom du fichier et useCase) du fait de l'ajout de la notion de sections 
        if (!tableauDesNomsDesFichiersJavascript.exists(nomDesFichiersJavascript => ((nomDesFichiersJavascript.fileName == fileName) && (nomDesFichiersJavascript.useCase == useCase)))) {
          tableauDesNomsDesFichiersJavascript.add(new NomDesFichiersJavascript(path, useCase, fileName))
        }
    }
    tableauDesNomsDesFichiersJavascript

  }

  /**
   * ***  nom du fichier javascript ***
   * @param outputFileName
   * @param useCase
   * @return name of file
   */
  def getNomduFichierJavascript(outputFileName: String, useCase: String): String = {
    val ficPropertyName = if (useCase != "") {
      CommonObjectForMockupProcess.generationProperties.srcJavascriptFilesDir + System.getProperty("file.separator") + useCase + System.getProperty("file.separator") + outputFileName + cstSuffixDesFichiersJavaScript
    } else {
      CommonObjectForMockupProcess.generationProperties.srcJavascriptFilesDir + System.getProperty("file.separator") + outputFileName + cstSuffixDesFichiersJavaScript
    }
    ficPropertyName
  }

  /**
   *  Récupération des tous les formulaires bindés
   *  @return Array of FormulaireCode
   */
  def getBindedForms(): ArrayList[FormulaireCode] = {
    val array1 = new ArrayList[FormulaireCode]()
    bindedForms.foreach {
      case ((useCaseDuFormulaire, ecranDuFormulaire, fragmentDuFormulaire, uniqueIdDuFormulaire), formulaireCode) => array1.add(formulaireCode)
    }
    //FIXME do bindedForms.values.toList ?
    array1

  }

  /**
   *  Récupération des formulaires bindés pour un fragment d'un écran principal
   *  @param useCaseName
   * @param EcranPrincipal name
   * @param fragment name
   *  @return Array of FormulaireCode
   */
  def getBindedForms(useCaseName: String, ecranPrincipal: String, fragmentName: String): ArrayList[FormulaireCode] = {
    val listeDesFormulaires = bindedForms.filter {
      case ((useCaseDuFormulaire, ecranDuFormulaire, fragmentDuFormulaire, uniqueIdDuFormulaire), formulaireCode) =>
        (useCaseDuFormulaire == useCaseName && ecranDuFormulaire == ecranPrincipal && fragmentDuFormulaire == fragmentName)
    }
    val array1 = new ArrayList[FormulaireCode]()
    listeDesFormulaires.foreach {
      case ((useCaseDuFormulaire, ecranDuFormulaire, fragmentDuFormulaire, uniqueIdDuFormulaire), formulaireCode) =>
        array1.add(formulaireCode)
    }
    array1

  }
  /**
   *  Récupération des formulaires bindés pour un écran principal et l'ensemble de ses fragments
   *   // bindedForms est une Map dont la clef est le useCase, l'ecran principal et le nom du fragment ainsi qu'un identifiant unique
   *    // pour un écran principal, le nom du fragment est vide
   *    // cette table va servir à lister des listes des formulaires pour un ecran et pour l'ensemble de ses fragments.
   *
   *  @param useCaseName
   * @param EcranPrincipal name
   *
   *  @return Array of FormulaireCode
   */
  def getBindedForms(useCaseName: String, ecranPrincipal: String): ArrayList[FormulaireCode] = {
    val listeDesFormulaires = bindedForms.filter {
      case ((useCaseDuFormulaire, ecranDuFormulaire, fragmentDuFormulaire, uniqueIdDuFormulaire), formulaireCode) => (useCaseDuFormulaire == useCaseName && ecranDuFormulaire == ecranPrincipal)
    }
    val array1 = new ArrayList[FormulaireCode]()
    listeDesFormulaires.foreach {
      case ((useCaseDuFormulaire, ecranDuFormulaire, fragmentDuFormulaire, uniqueIdDuFormulaire), formulaireCode) =>
        array1.add(formulaireCode)
    }
    array1

  }

  /**
   *  Récupération de tous les itemsVars bindés
   *
   *  @return Array of ItemVar
   */
  def getItemsVars(): ArrayList[ItemVar] = {
    //Itemsvars est une Map dont la clef est le usecase,ecran principal, fragmentName, identifiabt unique et la valeur itemsVar
    val array1 = new ArrayList[ItemVar]()
    val l1 = List(1, 2, 3)

    itemsVars.foreach {
      // modif le 9/2/16 ajout controle unicité
      case ((usecase, ecranPrincipal, fragmentName, unqiueId), itemVar) => {
        if (array1.filter(x => x.content == itemVar.content).size == 0) { array1.add(itemVar) }
      }
    } // return an array containaing value of itemsvars

    array1
  }

  /**
   *  Récupération des itemsVars bindés pour un écran principal et l'ensemble de ses fragments
   *  @param useCaseName
   * @param EcranPrincipal name
   *
   *  @return Array of FormulaireCode
   */
  def getItemsVars(useCaseName: String, ecranPrincipal: String): ArrayList[ItemVar] = {
    val listeDesFormulaires = itemsVars.filter {
      case ((usecaseItemVar, ecranPrincipalItemVar, fragmentNameItemVar, uniqueIdItemVar), itemVar) =>
        (usecaseItemVar == useCaseName && ecranPrincipalItemVar == ecranPrincipal)
    }
    val array1 = new ArrayList[ItemVar]()
    listeDesFormulaires.foreach {
      case ((usecaseItemVar, ecranPrincipalItemVar, fragmentNameItemVar, uniqueIdItemVar), itemVar) =>
        array1.add(itemVar)
    }
    array1

  }

  /**
   *  Récupération des itemsVars bindés pour un fragment d'un écran principal
   *  @param useCaseName
   * @param EcranPrincipal name
   * @param fragment name
   *  @return Array of FormulaireCode
   */
  def getItemsVars(useCaseName: String, ecranPrincipal: String, fragmentName: String): ArrayList[ItemVar] = {
    val listeDesFormulaires = itemsVars.filter {
      case ((usecaseItemVar, ecranPrincipalItemVar, fragmentNameItemVar, uniqueIdItemVar), itemVar) =>
        (usecaseItemVar == useCaseName && ecranPrincipalItemVar == ecranPrincipal && fragmentNameItemVar == fragmentName)
    }
    val array1 = new ArrayList[ItemVar]()
    listeDesFormulaires.foreach {
      case ((usecaseItemVar, ecranPrincipalItemVar, fragmentNameItemVar, uniqueIdItemVar), itemVar) => array1.add(itemVar)

    }
    array1

  }

  /**
   * ecriture du coce javascript: On balaie la hashMap contenant le nom des fichiers
   * ainsi que le code javascript
   * modif le 16/15 : rajout du traitement des sections
   * modif le 28/4/15 : ajout remplacement des preserve sections.
   * cette procédure est appelée après le traitement de tous les mockups
   *
   */
  def generation_fichiers_javascript: Unit = {
    // clef = (usecase,filename,section) 
    // regoupement des fichiers usecase  : on ne tient pas compte de la section 
    val liste_fichiers_javascript = mapSourcesJavascript.keys.map { case (useCase, fileName, section) => (useCase, fileName) }.toList.distinct
    val utilitaire = new Utilitaire
    liste_fichiers_javascript.foreach {
      case (useCase, fileName) => {
        val (ret6, source6, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(cstTemplateJavascript, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (cstJavascriptUseCase, useCase), (cstJavascriptFileName, fileName))
        val (ret7, source7, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(cstTemplateJavascript, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (cstJavascriptUseCase, useCase), (cstJavascriptFileName, fileName))
        val codeJavaScript = source6 + source7 // le code source est formaté par section
        ecritureDuCodeJavascript(fileName, codeJavaScript, useCase, utilitaire)
      }
    }

    /**
     * écriture du code javascript
     * @param outputFileName
     * @param codeJavascript
     * @param useCase
     * @param utilitaire
     * @param traitementFormatageSourceJava
     * @return true or false
     */
    def ecritureDuCodeJavascript(outputFileName: String, codeJavaScript: String, useCase: String, utilitaire: Utilitaire): Boolean = {
      val ficPropertyName = getNomduFichierJavascript(outputFileName, useCase)
      utilitaire.ecrire_fichier(ficPropertyName, codeJavaScript)
    }

  }
  /**
   * on indique la location dans la zone de l'écran pour le moteur de template
   * @param bookmark
   * @return Location
   */
  def retrieveLocation(bookmark: String): Location = {
    var (filename, useCaseName, isAfragment, fragmentName, generateController, ecranContenantLeFragment, typeDeFragment) = utilitaire.getFileInformation(bookmark)
    val location =
      if (useCaseName != "") {
        if (isAfragment) {
          new Location((CommonObjectForMockupProcess.generationProperties.srcWebFilesDir +
            // 1er paramétre (location)
            System.getProperty("file.separator") +
            useCaseName +
            System.getProperty("file.separator") +
            utilitaire.getRepositoryContainingFragmentAndMainScreen(filename, isAfragment, typeDeFragment, ecranContenantLeFragment) +
            System.getProperty("file.separator") +
            fragmentName + "." +
            CommonObjectForMockupProcess.generationProperties.generatedFrontFilesSuffix).replace("\\", "/"),

            // 2eme paramétre (SHORT_PATH)
            useCaseName.toUpperCase() + "_" +
              ecranContenantLeFragment.toUpperCase() + "_" +
              fragmentName.toUpperCase(),

            // 3eme parametre (REST URL)

            useCaseName + "/" +
              utilitaire.getRepositoryContainingFragmentAndMainScreen(filename, isAfragment, typeDeFragment, ecranContenantLeFragment) +

              "/" +

              fragmentName)

        } else { // ce n'est pas un fragment
          new Location((
            // 1er paramétre (location)
            CommonObjectForMockupProcess.generationProperties.srcWebFilesDir +
            System.getProperty("file.separator") +
            useCaseName +
            System.getProperty("file.separator") + filename + "." +
            CommonObjectForMockupProcess.generationProperties.generatedFrontFilesSuffix).replace("\\", "/"),
            // 2eme paramétre (SHORT_PATH)
            useCaseName.toUpperCase() + "_" +
              filename.toUpperCase(),
            // 3eme parametre (REST URL)
            useCaseName + "/" + filename + "/" +
              filename)

        }
      } else { // ce n'est pas un useCase

        new Location((
          // 1er paramétre (location)
          CommonObjectForMockupProcess.generationProperties.srcWebFilesDir +
          System.getProperty("file.separator") + filename + "." +
          CommonObjectForMockupProcess.generationProperties.generatedFrontFilesSuffix).replace("\\", "/"),
          // 2eme paramétre (SHORT_PATH)
          if (CommonObjectForMockupProcess.isAfragment) ecranContenantLeFragment + "/" else "" + filename.toUpperCase(),
          // 3eme parametre (REST URL)
          filename + "/" + filename)

      }
    location
  }
  /**
   * *** creation d'un fragment ***
   * le parametre bookmark est sous la forme : usecase-ficname$fragment
   * @param bookmark
   * @return Fragment
   */
  def createFragment(bookmark: String, widgetDuContainer: WidgetDeBase): Fragment = {
    var (filename, useCaseName, isAfragment, fragmentName, generateController, ecranContenantLeFragment, typeDeFragment) = utilitaire.getFileInformation(bookmark)
    if (isAfragment) {
      val location = retrieveLocation(bookmark)
      val fragment1 = new Fragment(fragmentName, bookmark, useCaseName, location, typeDeFragment.toUpperCase(), widgetDuContainer)
      fragment1
    } else { null }
  }
  def createFragment(bookmark: String): Fragment = {
    createFragment(bookmark, null)

  }

  def printBindedForms(): Unit = {
    // useCase, l'ecran principal et le nom du fragment 
    bindedForms.foreach {
      case ((useCaseDuFormulaire, ecranDuFormulaire, fragmentDuFormulaire, uniqueIdDuFormulaire), formulaireCode) =>
        println("usecase %s ecran %s fragment%s identifiant %s".format(useCaseDuFormulaire, ecranDuFormulaire, fragmentDuFormulaire, uniqueIdDuFormulaire))
    }
  }

}
