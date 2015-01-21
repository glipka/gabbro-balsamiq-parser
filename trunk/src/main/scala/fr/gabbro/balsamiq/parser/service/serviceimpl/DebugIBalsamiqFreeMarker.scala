package fr.gabbro.balsamiq.parser.service.serviceimpl
import java.io.File
import java.io.FileInputStream
import java.util.Properties
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.propertiesAsScalaMap
import scala.collection.mutable.StringBuilder
import java.io.InputStreamReader
import fr.gabbro.balsamiq.parser.service.TIBalsamiqFreeMarker

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
object DebugIBalsamiqFreeMarker extends App with TIBalsamiqFreeMarker {

    // lecture du fichier properties et des propriétés passées en system properties et mise en table
   def initProperties(): Boolean = {
    var ok = true
    //     CommonObjectForMockupProcess .generationProperties.projectName = System.getProperty("gencodefrombalsamiq.projectName").trim
    CommonObjectForMockupProcess.generationProperties.projectName = "projetBalsamiq2"
    CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatePropertiesFile = "c:/Temp/balsamiq/templates/freeMarkerTemplatesHTML.properties"
    CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir = "c:/temp/balsamiq/templates"
    CommonObjectForMockupProcess.engineProperties.messagesFile = "c:/temp/balsamiq/sourcesBalsamiq/messages.properties"
    CommonObjectForMockupProcess.generationProperties.generatedProjectDir = "C:/georges/projets/Zk/%project%".trim.replace("%project%", CommonObjectForMockupProcess.generationProperties.projectName)
    CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir = "c:/temp/balsamiq/sourcesBalsamiq"
    CommonObjectForMockupProcess.generationProperties.balsamiqAssetDir = "c:/temp/balsamiq/assets"
    CommonObjectForMockupProcess.generationProperties.configProperties = "C:/Temp/balsamiq/sourcesBalsamiq/config.properties"
    CommonObjectForMockupProcess.engineProperties.messagesFile = "C:/Temp/balsamiq/sourcesBalsamiq/messages.properties"
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
 
      val propsMap = props.toMap[String, String]
      CommonObjectForMockupProcess.engineProperties.loadProperties(propsMap)
      CommonObjectForMockupProcess.generationProperties.loadProperties(propsMap)
      CommonObjectForMockupProcess.templatingProperties.loadProperties(propsMap)
      val fic1 = new File(CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir)
      val fic3 = new File(CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir)
      if (!fic1.exists()) { println(utilitaire.getContenuMessage("mes10").format(CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir)); ok = false }
      if (!fic3.exists()) { println(utilitaire.getContenuMessage("mes10").format(CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir)); ok = false }
      //   if (!fic4.exists()) { println("GetpropertiesBalsamiq : fichier %s non trouve".format(CommonObjectForMockupProcess .sourceDesComposantsBalsamiqEtendus)); ok = false }
    }
    ok
  }

} // fin de la classe FileConvert

