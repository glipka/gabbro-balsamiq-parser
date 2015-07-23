package fr.gabbro.balsamiq.parser.service.serviceimpl

import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Properties

import scala.annotation.migration
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.collection.JavaConversions.mutableMapAsJavaMap
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.JavaConversions._
import scala.collection.immutable.List
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map
import fr.gabbro.balsamiq.parser.modelimpl.Utilitaire
import fr.gabbro.balsamiq.parser.service.TMoteurAnalyseJericho
import net.htmlparser.jericho.Attribute
import net.htmlparser.jericho.Element
import net.htmlparser.jericho.OutputDocument
import net.htmlparser.jericho.Source
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
class MoteurAnalyseJericho(moteurTemplatingFreeMarker: MoteurTemplatingFreeMarker, utilitaire: Utilitaire) extends TMoteurAnalyseJericho {
  var (ok, counterClef) = recuperationDesClefsDeTraduction()
  val traitementFormatageSourceJava = new TraitementFormatageSourceJava

  /**
   * <p>Les clefs de traduction sont sauvegardées dans un fichier properties</p>
   * <p>on recharge les clefs de traduction depuis ce fichier properties dans 2 hashTables :
   *  clef <=> valeur et valeur <=> clef</p>
   *
   * <p>format de la clef de traduction : <useCase, Ecran, formulaire, htmlTag, internal number</p>
   * <p>Exemple: useCase2.ectestgl01.ssss.label.1=text1</p>
   * <p>les champs intermédiaires sont variables</p>
   *
   * @return true or false and last key number used</p>
   */
  def recuperationDesClefsDeTraduction(): (Boolean, Int) = {
    var ok = true
    tableDesClefsValeursDeTraduction.clear
    tableDesValeursClefsDeTraduction.clear
    var clefMaxi = 0;
    try {
      val props = new Properties();
      val ficPropertyName = CommonObjectForMockupProcess.generationProperties.srcI18nFilesDir + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedi18nFileName
      props.load(new InputStreamReader(new FileInputStream(ficPropertyName), cstUtf8));
      // on récupère l'ensemble des clefs
      val enuKeys = props.keys().toList
      enuKeys.foreach(clef => {
        val valeur = props.getProperty(clef.toString()).trim // valeur à traduire
        val clefNumerique = (clef.toString().split("\\.").last.toInt) // N° unique 
        val usecaseReference = (clef.toString().split("\\.").head.toString) // 1ere element de la clef = usecase Name
        val ecranReference = (clef.toString().split("\\.").tail.head.toString) // 2eme élément de la clef = nom de l'écran
        tableDesClefsValeursDeTraduction += (clef.toString -> valeur) // on enrichit la table des clefs valeurs
        tableDesValeursClefsDeTraduction += ((valeur, ecranReference, usecaseReference) -> clef.toString()) // on enrichit la table des valeurs clefs
        if (clefNumerique > clefMaxi) clefMaxi = clefNumerique // va servir pour generer les nouvelles clefs

      })
    } catch {
      // si le fichier des clefs n'existe pas, il sera créé

      case ex: Exception =>

    }
    (ok, clefMaxi)

  }
  /**
   * <p> ecriture dans le fichier properties des libelles à traduire</p>
   * <p>On lit l'ensemble des clefs que l'on trie par ordre descendant</p>
   * <p>on récupere la valeur de chaque clef</p>
   * <p>puis on écrit le tout dans un buffer.</p>
   */
  def sauvegardeDesClefsDeTraduction(): Unit = {
    val keysSet = tableDesClefsValeursDeTraduction.keys.toList.sortWith((x, y) => x < y)
    var fileWriter: java.io.FileWriter = null
    val sbuf = new StringBuilder
    keysSet.foreach(key => {
      val value = tableDesClefsValeursDeTraduction.getOrElse(key, " ")
      sbuf.append(key).append("=").append(value).append("\r\n");
    })
    val ficPropertyName = CommonObjectForMockupProcess.generationProperties.srcI18nFilesDir + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedi18nFileName
    utilitaire.ecrire_fichier(ficPropertyName, sbuf.toString())

  }

  /**
   * <p>La propriété i18nLocales contient la liste des langues à internationaliser.</p>
   * <p>Pour chaque langue, on prend comme source le fichier des traductions et on rééecrit les clefs</p>
   * <p>non présentes dans la langue cible à traduire. Ce qui signifie que les clefs existantes ne sont pas écrasées.</p>
   * <p>@return true or false</p>
   */
  def traitementDeltaDesFichiersDeTraductionDesDifferentsPays: Boolean = {
    // fichier properties non loaclisé
    val propsLocal = new Properties();
    val ficPropertyLocal = CommonObjectForMockupProcess.generationProperties.srcI18nFilesDir + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedi18nFileName

    propsLocal.load(new InputStreamReader(new FileInputStream(ficPropertyLocal), cstUtf8));
    val listeDesclefDusFichierDeProprietesNonLocalise = propsLocal.keys().toList

    // pour chaque langue à traiter
    CommonObjectForMockupProcess.generationProperties.i18nLocales.foreach(country => {
      val ficPropertyPays = CommonObjectForMockupProcess.generationProperties.srcI18nFilesDir + System.getProperty("file.separator") + CommonObjectForMockupProcess.generationProperties.generatedi18nFileName.split("\\.").head + "_" + country + "." + CommonObjectForMockupProcess.generationProperties.generatedi18nFileName.split("\\.").last
      val propsPays = new java.util.Properties();
      val filePays = new File(ficPropertyPays)
      if (filePays.exists()) { propsPays.load(new InputStreamReader(new FileInputStream(ficPropertyPays), cstUtf8)); }
      val sbuf = new StringBuilder
      val propsPaysMap = propsPays.toMap[String, String] // on récupère les clefs du fichier properties du pays en cours

      // on verifie que chaque clef du fichier properties non localisé existe dans le fichier properties de la langue en cours
      listeDesclefDusFichierDeProprietesNonLocalise.foreach(clefnonlocalisee => { // pour chaque clef du fichier properties non localisé 
        if (propsPays.getProperty(clefnonlocalisee.toString) == null) { // la clef n'existe pas dans le fichier properties de la langue en cours
          sbuf.append(clefnonlocalisee).append("=").append(propsLocal.getProperty(clefnonlocalisee.toString)).append("\r\n"); // on rajoute la valeur du fichier properties non localisé
        } else {
          sbuf.append(clefnonlocalisee).append("=").append(propsPays.getProperty(clefnonlocalisee.toString)).append("\r\n"); // on rajoute la valeur du fichier properties de la langue en cours
        }
      })
      // écriture du fichier properties de la langue en cours
      val filewriter = new OutputStreamWriter(new FileOutputStream(ficPropertyPays), cstUtf8)
      filewriter.write(sbuf.toString())
      filewriter.close
    })
    true

  }

  /**
   *
   * <p>appel récursif de la liste des elements</p>
   * <p>si l'element a des fils, traitement récursif des fils</p>
   * <p>pour chaque élément, on récupère le texte à traduire.</p>
   * <p>On utilise la hashMap (valeur, clef) pour vérifier si cette valeur est déjà en table</p>
   * <p>Si oui => on récupère la valeur, sinon, on crée cette valeur dans les 2 hashmap (valeur clef et clef valeur)</p>
   * <p>la clef de traduction est recupérée par le template Freemarker templateClefDeTraduction</p>
   * <p>puis on un replace du segment à modifier</p>
   * <p>pour chaque valeur à traduire on détermine la hiérarchie de l'élément.</p>
   * <p>si l'2lément à traduire est dans un formulaire, on l'indique dans la clef de tradution.</p>
   * <p>La 1ere valeur de la clef est toujours le nom du fichier en cours de traitement, puis éventuelllement l'ID du fomulaire</p>
   * <p>contenant l'éelemnt à traduire puis le type d'élement et enfin un compteur unique.</p>
   * <p>exemple :</p>
   * <p>          testgl01.header.47=header</p>
   * <p>          testgl01.i.52=second</p>
   * <p>          testgl01.ins.53=row</p>
   * <p>          testgl01.personne.label.56=Adresse</p>
   * <p>          testgl01.personne.label.57=Personne</p>
   *
   * @param elements : List of Element to traduct
   * @param outputDocument
   */
  def extractMessages(elements: List[net.htmlparser.jericho.Element], outputDocument: net.htmlparser.jericho.OutputDocument): Unit = {
    elements.foreach(element => {
      // remplacement des attributs
      val textExtractor = element.getTextExtractor();
      // remplacement si nécessaire des attributs avec leurs valeurs traduites
      val (mapAttributes, modifAttribut) = traitementAttributsElement(element)
      if (mapAttributes.size > 0 && modifAttribut) { // des clefs à traduire ??
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
          val (ret1, source1, sourcejavsacript1, _) = moteurTemplatingFreeMarker.generationDuTemplate(cstTemplateClefDeTraduction, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (cstKey, key))
          val (ret2, source2, sourcejavascript2, _) = moteurTemplatingFreeMarker.generationDuTemplate(cstTemplateClefDeTraduction, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (cstKey, key))
          val clefDeTraduction = source1 + source2
          outputDocument.replace(segment, clefDeTraduction);
        }

      } // if1
    })
  }

  import scala.collection.mutable.Map
  /**
   * <p>*** traitement des attributs de l'élément en cours ***</p>
   * <p>si l'attribut est dans la table des attributs à traduire, on renseigne sa valeur traduite dans une hashMAP</p>
   * <p>Le template templateClefDeTraduction génère le source de traduction du token.</p>
   * <p> Jericho donne la possibilité de modifier les attributs d'un segment par la fonction replace</p>
   * @param element
   * @return Map of Attributes
   */
  def traitementAttributsElement(element: Element): (Map[String, String], Boolean) = {
    var modifAttribut = false // indicateur attribut modifie
    val attributes = if (element.getAttributes != null) element.getAttributes.toList else List[Attribute]()
    val elementName = element.getStartTag.getName
    var mapAttributes = scala.collection.mutable.Map[String, String]()
    attributes.foreach(attribute => {
      // le nom de l'attribut est-il dans la liste des attributs à traduire ? 
      if (List(attribute.getName).intersect(CommonObjectForMockupProcess.generationProperties.attributesToProcessI18n).size > 0) {
        modifAttribut = true
        val key = traduction_valeur(replaceSpecialCharKey(attribute.getValue), element, attribute.getName)
        if (key != "") {
          val (_, source1, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(cstTemplateClefDeTraduction, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (cstKey, key), (cstIsAttribute, "true"))
          val (_, source2, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(cstTemplateClefDeTraduction, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (cstKey, key), (cstIsAttribute, "true"))
          val clefDeTraduction = source1 + source2
          mapAttributes += (attribute.getName -> clefDeTraduction.trim)
        } else { mapAttributes += (attribute.getName -> replaceSpecialCharValue(attribute.getValue)) }
      } else { // l'attribut n'est pas modifé on le repasse tel quel 
        mapAttributes += (attribute.getName -> attribute.getValue)
      }
    })
    (mapAttributes, modifAttribut)
  }

  /**
   * <p><b>Traduction d'une valeur</b></p>
   * <p>Si la valeur à traduire est numérique, on ne fait rien</p>
   * <p>Si la valeur n'est pas déjà traduite pour le useCase et le mockup en cours,</p>
   *   <p>On récupère la hiérarchie des éléments contenant le tag en cours,</p>
   *   <p>on filtre cette hiérarchie avec les élements qui sont dans la liste des containers HTML (htmlContainerListForI18nGeneration) et on récupère l'id
   *    de chaque élément filtré.</p>
   *   <p>Appel du template BuildTraductionKey afin de générer la clef du fichier properties (usecase,Mockupname,FormName,HtmlTag,Index)</p>
   * <p> modif le 4 may 2015 : on ne traite la valeur de la clef à traduire que si le tag est dans body.
   * @param valeur : valeur à traduire
   * @param element
   * @param attributeName : nom de l'attribut
   * @return clef de traduction
   */
  def traduction_valeur(valeur: String, element: Element, attributeName: String): String = {
    var valeurATraduire = if (valeur != null) { replaceSpecialCharValue(valeur.trim()) } else { "" }
    // on ne fait pas de traduction pour les valeurs numériques
    if (valeurATraduire.length() > 0 && !valeurATraduire.forall(_.isDigit)) { //if1
      // valeur non déjà traduite pour le usecase et écran en cours. 
      if (!tableDesValeursClefsDeTraduction.contains(valeurATraduire, CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement)) {
        val table_hierachie = getHierarchie(element); // hiérarchie pour l'élément en cours
        // On en fait la traduction que si le tag n'est pas dans la liste des tags à bypasser.
        if (!table_hierachie.exists(element => { List(element.getStartTag.getName).intersect(CommonObjectForMockupProcess.generationProperties.bypassProcessI18nTagHierachy).size > 0 })) {
          counterClef += 1 // compteur unicité des clefs
          // on filtre la table hiérachie par les élements qui sont dans la liste des htmlContainerListForI18nGeneration
          val table_formulaire = table_hierachie.filter(element => {
            List(element.getStartTag.getName).intersect(CommonObjectForMockupProcess.generationProperties.htmlContainerListForI18nGeneration).size > 0
          })
          var container = ""
          // pour chaque élement de la table des formulaires, on récupère l'attribut id de l'élément dans le code html
          table_formulaire.foreach(formulaire => {
            val id = formulaire.getAttributeValue(cstId)
            if (id != null && id != "") { container = container + id + "." }
            else { container = container + formulaire.getStartTag().getName + "." }
          })
          if (container.endsWith(".")) { container = container.substring(0, container.size - 1) } // on supprime le .
          // on appelle le template templateBuildTraductionKey afin de générer la clef du fichier properties
          var isAttribute = if (attributeName != "") { true } else { false }
          val (_, source6, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(cstTemplateBuildTraductionKey, CommonObjectForMockupProcess.templatingProperties.phase_debut, null, (cstContainer, container), (cstIsAttribute, isAttribute.toString), (cstCurrentTag, table_hierachie.head.getStartTag.getName.toLowerCase()), (cstIndex, counterClef.toString), (cstAttributName, attributeName))
          val (_, source7, _, _) = moteurTemplatingFreeMarker.generationDuTemplate(cstTemplateBuildTraductionKey, CommonObjectForMockupProcess.templatingProperties.phase_fin, null, (cstContainer, container), (cstIsAttribute, isAttribute.toString), (cstCurrentTag, table_hierachie.head.getStartTag.getName.toLowerCase()), (cstIndex, counterClef.toString), (cstAttributName, attributeName))
          var key = replaceSpecialCharKey(source6.trim + source7.trim) // la clef de la propriété est fournie par le template  templateBuildTraductionKey
          tableDesClefsValeursDeTraduction += (key -> valeurATraduire);
          tableDesValeursClefsDeTraduction += ((valeurATraduire, CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement) -> key)
          return key
        }
      } else { // la clef existe déjà
        val clef = tableDesValeursClefsDeTraduction.getOrElse((valeurATraduire, CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement, CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement), "");
        return clef
      }
    }
    return ""
  }
  // ------------------------------------------------------------------------------------------
  // *** remplacement des caractères spéciaux dans les fichiers properties***
  // * remplacemnt des caractères spéciaux dans la clef des élements du fichier properties
  // ------------------------------------------------------------------------------------------- 
  def replaceSpecialCharKey(valeur: String): String = {
    //   0X22 = double quote
    valeur.replace("\n", "").replace("\t", "").replace("0x22", "")
    valeur
  }
  // --------------------------------------------------------------------------
  // *** remplacement des caractères spéciaux dans les fichiers properties***
  // ---------------------------------------------------------------------------
  def replaceSpecialCharValue(valeur: String): String = {
    //   0X22 = double quote
    valeur.replace("\n", "").replace("\t", "").replace("0x22", "")
    valeur
  }
  /**
   *  récupération de la hiérarchie de l'element en cours pour retrouver facilement le widget dans la page.
   * @param element
   * @return ArrayBuffer[Element]
   */
  def getHierarchie(element: Element): ArrayBuffer[Element] = {
    var tableHierarchie = new ArrayBuffer[Element]()
    tableHierarchie += element
    if (element.getParentElement() != null) { tableHierarchie = tableHierarchie ++ getHierarchie(element.getParentElement) }
    tableHierarchie

  }

  /*
   * <p>Lecture du fichier html extraction de l'ensemble des elements de la page. (fonction extractMessages)</p>
   * <p> les preserve sectios ont déjà été récupérées lors de la creation du fichier html
   * <p>et réécriture du fichier html.</p>
   * @param fileName
   * @param subDirectory
   * @param templateDirOut
   */
  def traductHtmlFile(fileName: String, subDirectory: String, templateDirOut: String): Unit = {
    var directoryName = templateDirOut
    val sourceHTML = utilitaire.getEmplacementFichierHtml(fileName, directoryName)
    val fileHTML = new File(sourceHTML)
    val source = new Source(new InputStreamReader(new FileInputStream(fileHTML), cstUtf8));
    source.fullSequentialParse();
    val outputDocument = new OutputDocument(source);
    val childElements = source.getChildElements().toList
    extractMessages(childElements, outputDocument); // on met à jour le fichier HTML
    val fichierHtmlTraduit = utilitaire.getEmplacementFichierHtml(fileName, directoryName)
    utilitaire.ecrire_fichier(fichierHtmlTraduit, outputDocument.toString(), false) // écriture du fichier sans traitement des preserve sections
  }

  /*
   * <p>Lecture du fichier html extraction de l'ensemble des elements de la page. (fonction extractMessages)</p>
   * <p> les preserve sectios ont déjà été récupérées lors de la creation du fichier html
   * <p>et réécriture du fichier html.</p>
   * @param fileName
   * @param subDirectory
   * @param templateDirOut
   */
  def traductHtmlFile(sourceEcran: String): String = {
    val source  = new Source(sourceEcran);
    source.fullSequentialParse();
    val outputDocument = new OutputDocument(source);
    val childElements = source.getChildElements().toList
    extractMessages(childElements, outputDocument); // on met à jour le fichier HTML
    return outputDocument.toString()
  }

} // fin de la classe