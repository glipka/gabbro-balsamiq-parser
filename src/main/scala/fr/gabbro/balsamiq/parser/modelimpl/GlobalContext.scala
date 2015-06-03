package fr.gabbro.balsamiq.parser.modelimpl
import java.util.ArrayList
import scala.beans.BeanProperty
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.ArrayBuffer
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementFormatageSourceJava
import fr.gabbro.balsamiq.parser.service.serviceimpl.MoteurTemplatingFreeMarker
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementPreserveSection

// Zone commune freemarker enrichie par l'ensemble du traitements des maquettes
//  va servir pour générer le menu par exemple.  
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
  // modif le 22/4/15 par gl Itemsvars est une Map dont la clef est le usecase,ecran principla, fragmentName, identifiabt unique et la valeur itemsVar
  // cette table va servir à lister des listes des itemsvars pour un ecran principal et pour l'ensemble de ses fragments.
  var itemsVars = Map[(String, String, String, String), ItemVar]() // pour stocker les itemsvar
  @BeanProperty var firstLevelObject = new java.util.ArrayList[FormulaireCode]() // contient les sources pour instancier les classes du DTO dans le contrôleur  
  @BeanProperty var tableDesCodesDesClassesJavaouScala = Map[(String, String), String]() // table des classes : nom de la classe, nom du sous package,code de la classe

  // modif le 22/4/15 par georges 
  // bindedForms est une Map dont la clef est le useCase, l'ecran principal et le nom du fragment ainsi qu'un identifiant unique
  // pour un écran principal, le nom du fragment est vide 
  // cette table va servir à lister des listes des formulaires pour un ecran et pour l'ensemble de ses fragments.
  var bindedForms = Map[(String, String, String, String), FormulaireCode]() // contient les sources pour instancier les formulaires
  @BeanProperty var paths = new java.util.ArrayList[Location]() // contient la localisation des fichiers JSP générés.
  @BeanProperty var mapSourcesJavascript = scala.collection.mutable.Map[(String, String, String), String]() // clef = (usecase,filename,section) value = code javascript
  /**
   * <p>methode appelée par freeMarker pour mettre en table le code source des classes java.</p>
   * <p>subPackageName est le sous package dans lequel sera générée la classe</p>
   *
   * @param className
   * @param classCode
   * @param subPackageName
   */
  def setCodeClasse(className: String, classCode: String, subPackageName: String): Unit = {
    tableDesCodesDesClassesJavaouScala += (className, subPackageName) -> classCode
  }

  /**
   * @param className
   * @param subPackageName
   * @return content of java code for the className
   */
  def RetrieveCodeJaveOuScala(className: String, subPackageName: String): String = {
    val codeJavaOrScala = tableDesCodesDesClassesJavaouScala.getOrElse((className, subPackageName), "")
    codeJavaOrScala
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
  /**
   * Get a javascript section beforehand cached thanks to registerJavascriptSection function
   * @param section name of cached section
   * @param useCase
   * @param fileName
   * @return code of the section
   * //TODO deduire du code scala ?
   */
  def getJavascriptSection(useCase: String, fileName: String, section: String): String = {
    mapSourcesJavascript.getOrElse((useCase, fileName, section), "")

  }

  /**
   * Exposition à freemarker des noms des fichiers javascript générés. (sans les doublons)
   * @return  ArrayList[NomDesFichiersJavascript]
   */
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

  /**
   * ***  nom du fichier javascript ***
   * @param outputFileName
   * @param useCase
   * @return name of file
   */
  def getNomduFichierJavascript(outputFileName: String, useCase: String): String = {
    val ficPropertyName = if (useCase != "") {
      CommonObjectForMockupProcess.generationProperties.srcJavascriptFilesDir + System.getProperty("file.separator") + useCase + System.getProperty("file.separator") + outputFileName + CommonObjectForMockupProcess.constants.suffixDesFichiersJavaScript
    } else {
      CommonObjectForMockupProcess.generationProperties.srcJavascriptFilesDir + System.getProperty("file.separator") + outputFileName + CommonObjectForMockupProcess.constants.suffixDesFichiersJavaScript
    }
    ficPropertyName
  }

  /**
   *  Récupération des tous les formulaires bindés
   *  @return Array of FormulaireCode
   */
  def getBindedForms(): ArrayList[FormulaireCode] = {
    val array1 = new ArrayList[FormulaireCode]()
    bindedForms.foreach(keyValue => { array1.add(keyValue._2) })
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
    val listeDesFormulaires = bindedForms.filter(keyValue => {
      (keyValue._1._1 == useCaseName && keyValue._1._2 == ecranPrincipal && keyValue._1._3 == fragmentName)
    })
    val array1 = new ArrayList[FormulaireCode]()
    listeDesFormulaires.foreach(keyValue => { array1.add(keyValue._2) })
    array1

  }
  /**
   *  Récupération des formulaires bindés pour un écran principal et l'ensemble de ses fragments
   *  @param useCaseName
   * @param EcranPrincipal name
   *
   *  @return Array of FormulaireCode
   */
  def getBindedForms(useCaseName: String, ecranPrincipal: String): ArrayList[FormulaireCode] = {
    val listeDesFormulaires = bindedForms.filter(keyValue => {
      (keyValue._1._1 == useCaseName && keyValue._1._2 == ecranPrincipal)
    })
    val array1 = new ArrayList[FormulaireCode]()
    listeDesFormulaires.foreach(keyValue => { array1.add(keyValue._2) })
    array1

  }

  /**
   *  Récupération de tous les itemsVars bindés
   *
   *  @return Array of ItemVar
   */
  def getItemsVars(): ArrayList[ItemVar] = {
    val array1 = new ArrayList[ItemVar]()
    itemsVars.foreach(keyValue => { array1.add(keyValue._2) }) // return an array containaing value of itemsvars
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
    val listeDesFormulaires = itemsVars.filter(keyValue => {
      (keyValue._1._1 == useCaseName && keyValue._1._2 == ecranPrincipal)
    })
    val array1 = new ArrayList[ItemVar]()
    listeDesFormulaires.foreach(keyValue => { array1.add(keyValue._2) })
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
    val listeDesFormulaires = itemsVars.filter(keyValue => {
      (keyValue._1._1 == useCaseName && keyValue._1._2 == ecranPrincipal && keyValue._1._3 == fragmentName)
    })
    val array1 = new ArrayList[ItemVar]()
    listeDesFormulaires.foreach(keyValue => { array1.add(keyValue._2) })
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
    // regoupement des fichiers usecase  : on ne tient pas compte de la section 
    val liste_fichiers_javascript = mapSourcesJavascript.keys.map(key => (key._1, key._2)).toList.distinct
    val utilitaire = new Utilitaire
    liste_fichiers_javascript.foreach(useCasefileName => {
      val useCase = useCasefileName._1
      val fileName = useCasefileName._2
      val (ret6, source6, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateJavascript, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.javascriptUseCase, useCase), (CommonObjectForMockupProcess.constants.javascriptFileName, fileName))
      val (ret7, source7, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(CommonObjectForMockupProcess.constants.templateJavascript, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.javascriptUseCase, useCase), (CommonObjectForMockupProcess.constants.javascriptFileName, fileName))
      val codeJavaScript = source6 + source7 // le code source est formaté par section
      ecritureDuCodeJavascript(fileName, codeJavaScript, useCase, utilitaire)
    })

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
  def createFragment(bookmark: String): Fragment = {
    var (filename, useCaseName, isAfragment, fragmentName, generateController, ecranContenantLeFragment, typeDeFragment) = utilitaire.getFileInformation(bookmark)
    if (isAfragment) {
      val location = retrieveLocation(bookmark)
      val fragment1 = new Fragment(fragmentName, bookmark, useCaseName, location, typeDeFragment)
      fragment1
    } else { null }
  }

  def printBindedForms(): Unit = {

    // useCase, l'ecran principal et le nom du fragment 
    bindedForms.foreach(f => {
      println("usecase %s ecran %s fragment%s identifiant %s".format(f._1._1, f._1._2, f._1._3, f._1._4))
    })
  }
  /**
   * on génération du Menu de l'application
   * @return source of menu
   */
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

  /**
   * @param menuItemEnCours
   * @param niveau
   * @param pere
   * @param hierarchiePere
   * @return source menu Item
   */
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
