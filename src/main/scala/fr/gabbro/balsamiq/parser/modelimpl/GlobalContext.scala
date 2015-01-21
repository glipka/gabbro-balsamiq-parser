package fr.gabbro.balsamiq.parser.modelimpl
import java.util.ArrayList
import scala.beans.BeanProperty
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.ArrayBuffer
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementFormatageSourceJava
import fr.gabbro.balsamiq.parser.service.serviceimpl.MoteurTemplatingFreeMarker

// Zone commune freemarker enrichie par l'ensemble du traitements des maquettes
//  va servir pour générer le ùmenu par exemple.  
//  
//  
//  
class MenuItem(val itemName: String, var children: ArrayBuffer[MenuItem], var url: String, var usecaseName: String)
class NomDesFichiersJavascript(@BeanProperty var path: String, @BeanProperty var useCase: String, @BeanProperty var fileName: String)

class GlobalContext() {
  val utilitaire = new Utilitaire
  var tableauDesMenuItems = ArrayBuffer[MenuItem]() // mis à jour par 
  var globalSourceMenu = new StringBuilder() // va contenir le code HTML du menu
  var moteurTemplatingFreeMarker: MoteurTemplatingFreeMarker = _
  @BeanProperty var itemsVars = new java.util.ArrayList[ItemVar]()
  @BeanProperty var firstLevelObject = new java.util.ArrayList[FormulaireCode]() // contient les sources pour instancier les classes du DTO dans le contrôleur
  @BeanProperty var bindedForms = new java.util.ArrayList[FormulaireCode]() // contient les sources pour instancier les formulaires
  @BeanProperty var paths = new java.util.ArrayList[Location]() // contient la localisation des fichiers JSP générés.
  @BeanProperty var mapSourcesJavascript = scala.collection.mutable.Map[(String, String, String), String]() // clef = (usecase,filename,section) value = code javascript

  // ------------------------------------------------------------------------------------------------------
  // le code javascript est mis dans une hashMap afin de concaténer le code de l'ecran et des fragments
  // on met en cache le code javascript : Le code des fragments est cumulé avec le code du fichier
  // rajout le 16/1/15 de la section de code pour hiérarchiser le code javascript
  // cette procédure est appelée par les templates javascript freemarker
  // ------------------------------------------------------------------------------------------------------
  def mise_en_cache_code_javascript(fileName: String, codeJavascript: String, isAfragment: String, section: String): Unit = {
    // clef= (usecase,nom de fichier,section)
     val key =
      if (isAfragment == CommonObjectForMockupProcess.constants.trueString) {
        (CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement, CommonObjectForMockupProcess.ecranContenantLeSegment, section)
      } else {
        (CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, section)
      }
    // on met le code source du fichier avant le code source des fragments
    val codeDuFichier = if (isAfragment != CommonObjectForMockupProcess.constants.trueString) codeJavascript + mapSourcesJavascript.getOrElse(key, "")
    else mapSourcesJavascript.getOrElse(key, "") + codeJavascript
    mapSourcesJavascript.update(key, codeDuFichier)

  }
  // récupération de la section pour le usecase et le fichier
  def getJavascriptCodeForTheSection(useCase: String, fileName: String, section: String): String = {
    mapSourcesJavascript.getOrElse((useCase, fileName, section), "")
  }
  // --------------------------------------------------------
  // Exposition à freemarker des noms des fichiers javascript générés. 
  // Exposition à freemarker des informations sur les fichiers javascripts générés. 
  // ----------------------------------------------------------
  def getJavascripts(): ArrayList[NomDesFichiersJavascript] = {
    val tableauDesNomsDesFichiersJavascript = new ArrayList[NomDesFichiersJavascript]()
    mapSourcesJavascript.foreach(keyValue => {
      val useCaseFileNameSection = keyValue._1
      val codeJavascript = keyValue._2
      val useCase = useCaseFileNameSection._1
      val fileName = useCaseFileNameSection._2
      val section = useCaseFileNameSection._2

      val path = if (useCase != "") { CommonObjectForMockupProcess.generationProperties.srcJavascriptFilesDirWithOutPrefix + System.getProperty("file.separator") + useCase + System.getProperty("file.separator") + fileName + CommonObjectForMockupProcess.constants.suffixDesFichiersJavaScript }
      else { CommonObjectForMockupProcess.generationProperties.srcJavascriptFilesDirWithOutPrefix + System.getProperty("file.separator") + fileName + CommonObjectForMockupProcess.constants.suffixDesFichiersJavaScript }
      // verification qu'il n'y a pas des doublons (nom du fichier et useCase) du fait de l'ajout de la notion de sections 
      if (!tableauDesNomsDesFichiersJavascript.exists(nomDesFichiersJavascript => ((nomDesFichiersJavascript.fileName == fileName) && (nomDesFichiersJavascript.useCase == useCase)))) {
        tableauDesNomsDesFichiersJavascript.add(new NomDesFichiersJavascript(path, useCase, fileName))
      }
    })
    tableauDesNomsDesFichiersJavascript

  }

  // ***  nom du fichier javascript *** 
  // ------------------------------------------------------
  def getNomduFichierJavascript(outputFileName: String, useCase: String): String = {
    val ficPropertyName = if (useCase != "") {
      CommonObjectForMockupProcess.generationProperties.srcJavascriptFilesDir + System.getProperty("file.separator") + useCase + System.getProperty("file.separator") + outputFileName + CommonObjectForMockupProcess.constants.suffixDesFichiersJavaScript
    } else {
      CommonObjectForMockupProcess.generationProperties.srcJavascriptFilesDir + System.getProperty("file.separator") + outputFileName + CommonObjectForMockupProcess.constants.suffixDesFichiersJavaScript
    }
    ficPropertyName
  }
  // ---------------------------------------------------------------------------------
  // ecriture du coce javascript: On balaie lahaspMap contenant le nom des fichiers
  // ainsi que le code javascript
  // modif le 16/15 : rajout du traitement des sections 
  // cette procédure est appelée après le traitement de tous les mockups
  // ---------------------------------------------------------------------------------

  def generation_fichiers_javascript: Unit = {
    // regoupement des fichiers usecase  : on ne tient pas compte de la section 

    val liste_fichiers_javascript = mapSourcesJavascript.keys.map(key => (key._1, key._2)).toList.distinct
    val utilitaire = new Utilitaire
    val traitementFormatageSourceJava = new TraitementFormatageSourceJava
    liste_fichiers_javascript.foreach(useCasefileName => {
      val useCase = useCasefileName._1
      val fileName = useCasefileName._2
      val (ret6, source6, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateJavascript, CommonObjectForMockupProcess.templatingProperties.phase_debut, null,(CommonObjectForMockupProcess.constants.javascriptUseCase,useCase),(CommonObjectForMockupProcess.constants.javascriptFileName,fileName))
      val (ret7, source7, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateJavascript, CommonObjectForMockupProcess.templatingProperties.phase_fin, null,(CommonObjectForMockupProcess.constants.javascriptUseCase,useCase),(CommonObjectForMockupProcess.constants.javascriptFileName,fileName))
      val codeJavaScript = source6 + source7
      ecritureDuCodeJavascript(fileName, codeJavaScript, useCase, utilitaire, traitementFormatageSourceJava)

    })

    // ----------------------------------------------------------------------------
    def ecritureDuCodeJavascript(outputFileName: String, codeJavascript: String, useCase: String, utilitaire: Utilitaire, traitementFormatageSourceJava: TraitementFormatageSourceJava): Boolean = {
      //  val fileName = outputFileName.trim + CommonObjectForMockupProcess.constants.suffixDesFichiersJavaScript
      val ficPropertyName = getNomduFichierJavascript(outputFileName, useCase)
      val codeJavascriptFormatte = traitementFormatageSourceJava.indentSourceCodeJavaScript(codeJavascript, 5)
      utilitaire.ecrire_fichier(ficPropertyName, codeJavascriptFormatte)
    }

  }
  // -------------------------------------------------------------------------------
  // on indique la location dans la zone de l'écran pour le moteur de template
  //
  // -------------------------------------------------------------------------------

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
            utilitaire.getRepositoryContainingFragmentAndMainScreen() +
            System.getProperty("file.separator") +
            fragmentName + "." +
            CommonObjectForMockupProcess.generationProperties.generatedFrontFilesSuffix).replace("\\", "/"),

            // 2eme paramétre (SHORT_PATH)
            useCaseName.toUpperCase() + "_" +
              ecranContenantLeFragment.toUpperCase() + "_" +
              fragmentName.toUpperCase(),

            // 3eme parametre (REST URL)
            useCaseName + "/" +
              ecranContenantLeFragment + "/" +
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
  // ---------------------------------------------------------------------
  // *** creation d'un fragment ***
  // le parametre bookmark est sous la forme : usecase-ficname$fragment
  // ---------------------------------------------------------------------
  def createFragment(bookmark: String): Fragment = {
    var (filename, useCaseName, isAfragment, fragmentName, generateController, ecranContenantLeFragment, typeDeFragment) = utilitaire.getFileInformation(bookmark)
    if (isAfragment) {
      val location = retrieveLocation(bookmark)
      val fragment1 = new Fragment(fragmentName, bookmark, useCaseName, location, typeDeFragment)
      fragment1
    } else { null }
  }

  // ----------------------------------------------------------------------------
  // on génération du Menu de l'application
  // ----------------------------------------------------------------------------
  def generation_code_source_menu(): String = {
    globalSourceMenu = new StringBuilder // réinit du source du menu à blanc
    val (_, source1_debut, _, _) = moteurTemplatingFreeMarker.generationDuTemplate("header_menu", "debut", null)
    globalSourceMenu.append(source1_debut)
    // génération de chaque menu Item
    tableauDesMenuItems.foreach(menuItem => {
      globalSourceMenu.append(generation_code_source_menuItem(menuItem, 0, menuItem, ""))
    })
    val (_, source1_fin, _, _) = moteurTemplatingFreeMarker.generationDuTemplate("header_menu", "fin", null)
    globalSourceMenu.append(source1_fin)
    globalSourceMenu.toString()
  }
  // -----------------------------------------------------------------------
  // *** Génération du code source d'une classe ****
  // -----------------------------------------------------------------------

  private def generation_code_source_menuItem(menuItemEnCours: MenuItem, niveau: Int, pere: MenuItem, hierarchiePere: String): StringBuilder = {
    var codeDuMenu = new StringBuilder
    val tabulation = "\t" * niveau
    val (ret1, source1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate("branch_menu", "debut", null, ("itemname", menuItemEnCours.itemName.capitalize), ("tabulation", tabulation), ("url", menuItemEnCours.url), ("usecasename", menuItemEnCours.usecaseName))
    codeDuMenu.append(source1)
    // traitement de chaque champ de la classe      
    menuItemEnCours.children.foreach(item => {
      if (item.children.size > 0) {
        // traitement des items su sous menu
        codeDuMenu.append(generation_code_source_menuItem(item, niveau + 1, item, menuItemEnCours.itemName))
      } else { // c'est un champ 
        val (ret3, source3, _, _) = moteurTemplatingFreeMarker.generationDuTemplate("item_menu", "debut", null, ("itemname", item.itemName),
          ("tabulation", tabulation), ("url", item.url), ("usecasename", menuItemEnCours.usecaseName))

        val (ret4, source4, _, _) = moteurTemplatingFreeMarker.generationDuTemplate("item_menu", "fin", null, ("itemname", item.itemName),
          ("itemname", item.itemName), ("tabulation", tabulation), ("url", item.url), ("usecasename", menuItemEnCours.usecaseName))
        codeDuMenu.append(source3 + source4)

      }
    })
    // generation fin de classe. 
    val (ret2, source2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate("branch_menu", "fin", null, ("classname", menuItemEnCours.itemName), ("tabulation", tabulation))
    codeDuMenu.append(source2)
    codeDuMenu

  }

}