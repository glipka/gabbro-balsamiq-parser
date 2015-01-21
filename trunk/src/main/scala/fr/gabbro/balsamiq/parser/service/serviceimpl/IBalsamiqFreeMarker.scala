package fr.gabbro.balsamiq.parser.service.serviceimpl
import java.io.File
import java.io.FileInputStream
import java.util.Properties
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.propertiesAsScalaMap
import scala.collection.mutable.StringBuilder
import java.io.InputStreamReader
import fr.gabbro.balsamiq.parser.service.TIBalsamiqFreeMarker
import fr.gabbro.balsamiq.parser.modelimpl.CatalogAPlat
import fr.gabbro.balsamiq.parser.modelimpl.MockupContext
import fr.gabbro.balsamiq.parser.modelimpl.CatalogBalsamiq
import fr.gabbro.balsamiq.parser.modelimpl.CatalogDesComposants

// ==============================================================================================================================  
// *** Principe general IbalsamiqFreeMarker ***
// ==============================================================================================================================
// Scan du repertoire .asset pour mettre en table les composants Balsamiq partagés entre les différents projets
// Scan du repertoire contenant les maquettes de l'application
// Pour chaque fichier bmml trouvé :
//    Mise en table des widgets de l'écran en cours (format xml vers format plat)
//    A partir du format plat, pour chaque widget, détermination des fils (widgets inclus dans un container)
//    Puis constitution du catalogue final en respectant la hiérarchie container, fils.
//    le catalogue est un modele 1 n, chaque entrée d'un container pouvant être lui même un container
//    Enrichissement de chaque entrée du catalogue : calcul de position dans le container :
//    on détermine à la fois le n° de ligne et le numéro de cellule en 12eme par rapport au container.
// Exécution d'un premier traitement de validité des informations dans le catalogue
//    Test d'inclusion de l'ensemble des widgets dans un gabarit principal
//    Test de chevauchement de contenu dans un container
//    Test de duplication de contenu dans le catalogue (2 objets ayant la même position et la même taille
// Genération de code à partir du catalogue enrichi
//    Pour chaque container, extraction des composants triés par ligne et par colonne.
//    A chaque composant, on associe un template et on fait appel au moteur de templating Fréémarker pour exécuter le emaplte.
//    Le code source étant généré, ecriture des fichiers HTML et code (java, scala).
//    On génere 2 fichiers HTML, le 2eme contenant les clefs des libelles à traduire.
//    Remarque pour chaque template on peut associer un template javascript et un template code
//    POur un widget donné, les templates sont exécutés automatiquement à chaque instanciation d'un composant.
//    Ce mécanisme permet de générer pour un écran à la fois la partie HTML, la partie code java et la partie javascript
// ==============================================================================================================================
object IBalsamiqFreeMarker extends App with TIBalsamiqFreeMarker {
  if (!init()) { System.exit(99) }
  else {
    process() // process du batch
    destroy // destroy des ressources du batch
    System.exit(0)
  }
  // --------------------------------------------------------------------
  // *** initialiation des properties du programme ***
  // -------------------------------------------------------------------
  def init(): Boolean = {
    if (!initProperties()) {
      logBack.info(utilitaire.getContenuMessage("mes7"));
      false
    } else {
      moteurTemplateFreeMarker = new MoteurTemplatingFreeMarker(CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir, CommonObjectForMockupProcess.generationProperties.srcWebFilesDir, CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath, globalContext)
      globalContext.moteurTemplatingFreeMarker = moteurTemplateFreeMarker
      traitementMenu = new TraitementMenu(globalContext) // va servir à mettre en session les menuItem

      if (moteurTemplateFreeMarker.init()) { // init du moteur et chargement des templates
        moteurJericho = new MoteurAnalyseJericho(moteurTemplateFreeMarker, utilitaire) // les trad
        catalogDesComposantsCommuns = new CatalogDesComposants // catalogue commun à l'ensemble des écrans
        true
      } else { false }

    }
  }
  def destroy: Unit = {

  }
  // -------------------------------------------------------------------------------------------------------------------
  // process de l'ensemble des fichiers du repertoire balsamiq
  // En fin de traitement de l'ensemble des fichiers, on sauvegarde les clefs de traduction dans un fichier commun
  // --------------------------------------------------------------------------------------------------------------------
  def process(): Boolean = {
    logBack.info(utilitaire.getContenuMessage("mes40"))
    if (catalogDesComposantsCommuns.chargementDesCatalogues(CommonObjectForMockupProcess.generationProperties.balsamiqAssetDir)) { // chargement du catalog BootStrap  
      logBack.info(utilitaire.getContenuMessage("mes41"))
      traitementDesFichiersDuRepertoireBalsamiq(CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir) // traitement ensemble des fichiers
      logBack.info(utilitaire.getContenuMessage("mes59"))
      moteurJericho.sauvegardeDesClefsDeTraduction // ecriture dans fichier properties des clefs de traduction
      moteurJericho.traitementDeltaDesFichiersDeTraductionDesDifferentsPays; // mise à jours des fichiers properties internationalisés
      return { true }
    } else { false }

  }

  // ---------------------------------------------------------------------------------------------
  // scan du répertoire contenant les sources de balsamiq 
  // traitement du repertoire et du sous repertoire
  // le répertoire "assets" contient les fichiers balsamiq communs et ne doit pas être traité
  // ---------------------------------------------------------------------------------------------
  def traitementDesFichiersDuRepertoireBalsamiq(directory1: String): Int = {
    var compteur_fichiers_traites = 0
    logBack.info(utilitaire.getContenuMessage("mes60"), directory1)
    val fichiersBalsamiqAtraiter = new File(directory1).listFiles.toList
    fichiersBalsamiqAtraiter.foreach(file => {
      if ((file.isDirectory()) && (file.getName() != CommonObjectForMockupProcess.constants.assets)) { compteur_fichiers_traites += traitementDesFichiersDuRepertoireBalsamiq(file.getPath()) }
      else {
        if (file.getName.endsWith(CommonObjectForMockupProcess.constants.balsamiqFileSuffix)) {
          compteur_fichiers_traites += 1;
          traitementFichierBalsamiq(file)
        }
      }
    })
    logBack.info(utilitaire.getContenuMessage("mes42"))
    // *** templates à exécuter après le traitement de tous les écrans ***
    traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.globalExecutionTemplate1, CommonObjectForMockupProcess.generationProperties.globalExecutionFilePath1, traitementFormatageSourceJava)
    traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.globalExecutionTemplate2, CommonObjectForMockupProcess.generationProperties.globalExecutionFilePath2, traitementFormatageSourceJava)
    traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.globalExecutionTemplate3, CommonObjectForMockupProcess.generationProperties.globalExecutionFilePath3, traitementFormatageSourceJava)

    // ------------------------------------------------------------
    // on génère les fichiers javascript 
    // les fichiers javascript sont stockés dans une hashMap 
    // ------------------------------------------------------------
    logBack.info(utilitaire.getContenuMessage("mes44"))
    globalContext.generation_fichiers_javascript
    // A modifier dès qu'une solution pou rle menu est trouvée
    val menu = globalContext.generation_code_source_menu
    logBack.info(utilitaire.getContenuMessage("mes14"), directory1, compteur_fichiers_traites)
    compteur_fichiers_traites
  }
  // ------------------------------------------------------------------------------------
  //
  // traitement principal d'un fichier BPML  
  // On traite d'abord les widgets de la maquette, puis le gabarit de la page.
  // 
  // --------------------------------------------------------------------------------------
  def traitementFichierBalsamiq(fichierBalsamiq: File): Boolean =
    {
      logBack.info(utilitaire.getContenuMessage("mes8"), fichierBalsamiq.getCanonicalPath)
      var sourceEcran = new StringBuilder() // contiendra le code genere depuis les templates
      CommonObjectForMockupProcess.mockupContext = new MockupContext // zone commune de communication pour l'ensemble des widgets de la page
      var traitementBinding = new TraitementBinding(moteurTemplateFreeMarker, globalContext) // init de la classe binding
      // fileNameComplet contient 
      val (fic, rep, useCase, fileNameComplet, isAfragment, fragmentName, generateController, ecranContenantLeFragment, typeDeFragment) = utilitaire.getFileInformation(fichierBalsamiq)
      CommonObjectForMockupProcess.isAfragment = isAfragment
      CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement = if (isAfragment) { fragmentName } else { fic }
      CommonObjectForMockupProcess.nomDuRepertoirerEnCoursDeTraitement = rep
      CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement = useCase
      CommonObjectForMockupProcess.generateController = generateController
      CommonObjectForMockupProcess.ecranContenantLeSegment = if (ecranContenantLeFragment != "") ecranContenantLeFragment else CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement
      CommonObjectForMockupProcess.typeDuFragmentEnCoursDeTraitement = typeDeFragment

      new DetectDependencies(CommonObjectForMockupProcess.mockupContext).process() // mise en table des dépendances
      if (!isAfragment) { // si ce n'est pas un fragment => mise en table menu et recherche des fragments
        CommonObjectForMockupProcess.mockupContext.fragments ++= new DetectFragments(utilitaire).processEtMiseEntable() // mise en table des fragments
        // si ce n'est pas un fragment, on génère le traitement preserve section pour le fichier javascript qui sera généré au niveau de l'écran principal
        //     CommonObjectForMockupProcess.traitementPreserveSectionTemplateJavascript = new TraitementPreserveSection().process(globalContext.getNomduFichierJavascript(CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement)) // on met en table le contenu du fichier javascript pour traitment des preserve section
        CommonObjectForMockupProcess.mapDesTraitementsPreserveSection += ((CommonObjectForMockupProcess.constants.javascript, "") -> new TraitementPreserveSection().process(globalContext.getNomduFichierJavascript(CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement)))
        // on ne met en table les menus que pour les écrans. 
        // les noms des écrans démarrent par "ec"
        // la dépendance est sous la forme : uc1.ecr1.ecr2
        if (CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement.toLowerCase().startsWith(CommonObjectForMockupProcess.generationProperties.generateControllerForPrefix.toLowerCase())) {
          traitementMenu.mise_en_table_classes_menu_item(fileNameComplet, useCase, "url")
        }
      } // fin de isAFragment
      // ---------------------------------------------------------------------------------------------
      //        *** une preserve section pour le fichier code java par sous package***
      // il faudra remodifier cette liste à chaque ajout de sous package de code java ou scala 
      // ----------------------------------------------------------------------------------------------
      val listeDesSubPackageCode = List(CommonObjectForMockupProcess.generationProperties.generatedOtherAlias,
        CommonObjectForMockupProcess.generationProperties.generatedsubPackage1,
        CommonObjectForMockupProcess.generationProperties.generatedsubPackage2,
        CommonObjectForMockupProcess.generationProperties.generatedsubPackage3)
      setPreserveSection(CommonObjectForMockupProcess.constants.code, listeDesSubPackageCode)
      CommonObjectForMockupProcess.mapDesTraitementsPreserveSection += ((CommonObjectForMockupProcess.constants.html, "") -> new TraitementPreserveSection().process(utilitaire.getEmplacementFichierHtml(CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.generationProperties.srcWebFilesDir))) // on met en table le contenu du fichier javascript pour traitment des preserve section
      // ----------------------------------------------------------------------------------
      // *** on indique la location dans la zone de l'écran pour le moteur de template ***
      // ----------------------------------------------------------------------------------
      val location = globalContext.retrieveLocation(fichierBalsamiq.getName)
      CommonObjectForMockupProcess.mockupContext.location = location
      globalContext.paths.add(location)
      logBack.info(utilitaire.getContenuMessage("mes55"), fichierBalsamiq.getName())

      val catalogAPlat = new CatalogAPlat(fichierBalsamiq, moteurTemplateFreeMarker, traitementBinding, catalogDesComposantsCommuns)
      var sourceJavascript = new StringBuilder() // contiendra le code généré depuis les templates
      var codeDesComposants = new StringBuilder() // contiendra le code java généré pour la page
      val catalogBalsamiq = new CatalogBalsamiq(traitementBinding) // catalogBalsamiq final

      val (ok, w, h) = catalogAPlat.chargementCatalog()
      if (!ok) {
        logBack.info(utilitaire.getContenuMessage("mes9"), fichierBalsamiq.getName())
        false
      } else {
        logBack.info(utilitaire.getContenuMessage("mes56"), fichierBalsamiq.getName())

        CommonObjectForMockupProcess.mockupContext.global_max_width = w
        CommonObjectForMockupProcess.mockupContext.global_max_height = h
        catalogBalsamiq.creation_catalog(catalogAPlat.catalog) // creation et enrichissemebt du catalogue balamiq
        val controleValidite = new ControleValidite(catalogBalsamiq.catalog, traitementBinding, globalContext)
        controleValidite.controle
        controleValidite.mise_en_table_des_formulaires_pour_templates_et_RetraitementDesBinds(catalogBalsamiq.catalog, null)
        val generationDeCode = new ModuleGenerationCode(moteurTemplateFreeMarker)
        // traitement de l'ensemble des composants
        val (source2, sourceJavascript2, code2) = generationDeCode.traitement_widget_par_ligne_colonne(catalogBalsamiq.catalog(0).tableau_des_fils, 0, 0, null, catalogBalsamiq.catalog(0), false)
        // catalogBalsamiq.catalog(0) contient le widget gabarit de la pagel
        // qui doit être généré après le contenu de la page afin de traiter le javascript et le code
        // de l'ensemble des widgets de la maquette.
        val (_, source1, sourceJavascript1, code1) = moteurTemplateFreeMarker.generationDuTemplate(catalogBalsamiq.catalog(0), CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.javascript, sourceJavascript))
        sourceEcran = sourceEcran.append(source1); // header
        sourceEcran = sourceEcran.append(source2); // contenu
        sourceJavascript = sourceJavascript.append(sourceJavascript1)
        sourceJavascript = sourceJavascript.append(sourceJavascript2)
        codeDesComposants = codeDesComposants.append(code1) // code java ou scala début du gabarit
        codeDesComposants = codeDesComposants.append(code2) // code java ou scala début du gabarit
        val (_, source3, sourceJavascript3, code3) = moteurTemplateFreeMarker.generationDuTemplate(catalogBalsamiq.catalog(0), CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.javascript, sourceJavascript))
        sourceEcran = sourceEcran.append(source3); // footer
        sourceJavascript = sourceJavascript.append(sourceJavascript3)
        codeDesComposants = codeDesComposants.append(code3) // code java ou scala fin du gabarit
        logBack.info(utilitaire.getContenuMessage("mes45"))

        CommonObjectForMockupProcess.mockupContext.ecritureDuCodeJaveOuScala // ecriture des fichiers contenant les classes java ou scala
        moteurTemplateFreeMarker.ecritureDuFichierHTML(CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, sourceEcran.toString) // generation du fichier HTML et extraction des libelles à traduire

        // internationalisation du fichier ??
        if (CommonObjectForMockupProcess.generationProperties.processI18nInFiles == "true") {
          logBack.info(utilitaire.getContenuMessage("mes46"))
          moteurJericho.traductHtmlFile(CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuRepertoirerEnCoursDeTraitement, CommonObjectForMockupProcess.generationProperties.srcWebFilesDir) // extraction des clefs de traduction
        }
        logBack.info(utilitaire.getContenuMessage("mes47"))
        traitementBinding.generationDuSourceDesClassesEtCreationDuFichier
        // ---------------------------------------------------------------------
        // *** template à exécuter après le traitement de chaque écran ***
        // attention oligatoirement après le traitement des widgets
        // ---------------------------------------------------------------------
        logBack.info(utilitaire.getContenuMessage("mes43"))
        traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.localExecutionTemplate1, CommonObjectForMockupProcess.generationProperties.localExecutionFilePath1, traitementFormatageSourceJava)
        traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.localExecutionTemplate2, CommonObjectForMockupProcess.generationProperties.localExecutionFilePath2, traitementFormatageSourceJava)
        traitementLocalOuGlobalTemplate(CommonObjectForMockupProcess.generationProperties.localExecutionTemplate3, CommonObjectForMockupProcess.generationProperties.localExecutionFilePath3, traitementFormatageSourceJava)
        logBack.info(utilitaire.getContenuMessage("mes9"), fichierBalsamiq.getName())
        true
      }
    } // fin de la fonction processFile

  // -------------------------------------------------------------------------------
  // mise à jour table des preserves section pour le code 
  // on passe en parametre la liste des sous package définis dans le programme
  // -------------------------------------------------------------------------------
  def setPreserveSection(typeDePreserve: String, subPackageList: List[String]): Unit = {
    subPackageList.foreach(subPackage => {
      CommonObjectForMockupProcess.mapDesTraitementsPreserveSection += ((typeDePreserve, subPackage) -> new TraitementPreserveSection().process(utilitaire.getNomDuFichierCodeJavaOuScala(CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, subPackage)))
    })

  }

  // ----------------------------------------------
  // *** appel d'un template local ou global ***
  // ----------------------------------------------
  def traitementLocalOuGlobalTemplate(localExecutionTemplate: String, localExecutionPath: String, traitementFormatageSourceJava: TraitementFormatageSourceJava): Unit = {
    if (localExecutionTemplate != "") {
      val filename = CommonObjectForMockupProcess.generationProperties.generatedProjectDir + System.getProperty("file.separator") + utilitaire.substituteKeywords(localExecutionPath).replace(System.getProperty("file.separator"), "/").replace("//", "/")
      val traitementPreserveSection = new TraitementPreserveSection().process(filename) // utilisé pour récupérer le contenu des preserves section
      val (ret1, source1, _, _) = moteurTemplateFreeMarker.generationDuTemplate(localExecutionTemplate, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (CommonObjectForMockupProcess.constants.traitementPreserveSection, traitementPreserveSection))
      val (ret2, source2, _, _) = moteurTemplateFreeMarker.generationDuTemplate(localExecutionTemplate, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (CommonObjectForMockupProcess.constants.traitementPreserveSection, traitementPreserveSection))
      if ((source1 + source2).trim.size > 0) {
        var codeFormatte = ""
        if (filename.endsWith(CommonObjectForMockupProcess.generationProperties.languageSource)) {
          codeFormatte = traitementFormatageSourceJava.indentSourceCodeJava(source1 + source2)
        } else if (filename.endsWith(CommonObjectForMockupProcess.constants.suffixDesFichiersJavaScript)) {
          codeFormatte = traitementFormatageSourceJava.indentSourceCodeJavaScript(source1 + source2, 5)
        } else { codeFormatte = source1 + source2 }
        utilitaire.ecrire_fichier(filename, codeFormatte)
      }
    }
  }
  // ----------------------------------------------------------------------------------------------------
  // lecture du fichier properties et des propriétés passées en system properties et mise en table
  // ----------------------------------------------------------------------------------------------------
  def initProperties(): Boolean = {
    var ok = true
    logBack.info(utilitaire.getContenuMessage("mes58"))
    //     CommonObjectForMockupProcess .generationProperties.projectName = System.getProperty("gencodefrombalsamiq.projectName").trim
    CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatePropertiesFile = System.getProperty("gencodefrombalsamiq.freemarkerTemplatesPropertiesFile")
    CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir = System.getProperty("gencodefrombalsamiq.freemarkerTemplatesDir")
    CommonObjectForMockupProcess.engineProperties.messagesFile = System.getProperty("gencodefrombalsamiq.messagesFile")
    CommonObjectForMockupProcess.generationProperties.projectName = System.getProperty("gencodefrombalsamiq.projectName")
    CommonObjectForMockupProcess.generationProperties.generatedProjectDir = System.getProperty("gencodefrombalsamiq.generatedProjectDir").trim.replace("%project%", CommonObjectForMockupProcess.generationProperties.projectName)
    CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir = System.getProperty("gencodefrombalsamiq.freemarkerTemplatesDir")
    CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir = System.getProperty("gencodefrombalsamiq.balsamiqBmmlDir")
    CommonObjectForMockupProcess.generationProperties.balsamiqAssetDir = System.getProperty("gencodefrombalsamiq.freemarkerCatalogComposantsDir")
    CommonObjectForMockupProcess.generationProperties.configProperties = System.getProperty("gencodefrombalsamiq.propertiesFile")

    if (CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir == null ||
      CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir == null ||
      CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir == null ||
      CommonObjectForMockupProcess.engineProperties.messagesFile == null ||
      CommonObjectForMockupProcess.generationProperties.configProperties == null) {
      logBack.error(utilitaire.getContenuMessage("mes31"))
      ok = false

    } else {
      val props = new Properties();
      val ficPropertyName = CommonObjectForMockupProcess.generationProperties.configProperties
      props.load(new InputStreamReader(new FileInputStream(ficPropertyName), CommonObjectForMockupProcess.constants.utf8));
      //    props.load(new FileInputStream(ficPropertyName))
      val propsMap = props.toMap[String, String]
      CommonObjectForMockupProcess.engineProperties.loadProperties(propsMap)
      CommonObjectForMockupProcess.generationProperties.loadProperties(propsMap)
      CommonObjectForMockupProcess.templatingProperties.loadProperties(propsMap)
      val fic1 = new File(CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir)
      val fic3 = new File(CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir)
      if (!fic1.exists()) { logBack.error(utilitaire.getContenuMessage("mes10"), Array(CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir)); ok = false }
      if (!fic3.exists()) { logBack.error(utilitaire.getContenuMessage("mes10"), Array(CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir)); ok = false }
    }
    ok
  }

} // fin de la classe FileConvert

