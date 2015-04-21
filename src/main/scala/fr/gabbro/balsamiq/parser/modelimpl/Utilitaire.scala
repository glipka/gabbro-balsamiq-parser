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

import java.io.UnsupportedEncodingException
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.xml.bind.DatatypeConverter
import java.io.File
import java.util.Properties
import java.io.FileInputStream
import java.io.FileWriter
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.BufferedWriter
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess

class Utilitaire {
  val logBack = LoggerFactory.getLogger(this.getClass());
  val propsMessages = new Properties();
  //FIXME récuperer par défaut le fichier du classpath
  val ficPropertyName = if (System.getProperty("gencodefrombalsamiq.messagesFile") != null) System.getProperty("gencodefrombalsamiq.messagesFile") else "messages.properties" // appel direct du parametre car la classe utile est instanciée en début de programme  
  try { propsMessages.load(new FileInputStream(ficPropertyName)) }
  catch {
    case ex: Exception => logBack.error("Messages File not found {}", ficPropertyName)
  }

  /**
   * <p>les caractères hexa sont représentés sous forme %xy</p>
   * <p>il faut remplacer chaque séquence %xy par son équivalent en format caractère</p>
   * <p>exemple : abcd%30efg donne abcd0ef</p>
   * <p>char c = "\uFFFF".toCharArray()[0];</p>
   * @param contenu
   * @return content modified
   */
  def remplaceHexa(contenu: String): String = {
    var contenuInitial = contenu.replace("%25", "%");
    var contenuModifie = contenuInitial
    val regExp1 = "%[0-9A-Fa-f]{2}".r
    val regExp2 = "%u[0-9A-Fa-f]{2}".r
    regExp1.findAllIn(contenuModifie).foreach(valeurHexa => {
      val hexa = valeurHexa.substring(1)
      val intValue = Integer.valueOf(hexa, 16).intValue
      val stringValue = intValue.asInstanceOf[Char].toString()
      contenuModifie = contenuModifie.replace(valeurHexa, new String(stringValue.getBytes(CommonObjectForMockupProcess.engineProperties.codePageCaractereHexaBalsamiq),
        CommonObjectForMockupProcess.engineProperties.codePageCaractereHexaBalsamiq))
    })
    regExp2.findAllIn(contenuModifie).foreach(valeurUnicode => {
      // if (!valeurHexa.startsWith("%u")) 
      contenuModifie = contenuModifie.replace(valeurUnicode, valeurUnicode.replace("%", "\\").toCharArray)

    })

    contenuModifie
  }

  /**
   * <p>for italic, use _this notation_ </p>
   * <p>for a link, use [this notation]</p>
   * <p>for bold, use *this notation* </p>
   * <p>for disabled, use -this notation- </p>
   * <p>for underlined, use &this notation&</p>
   * <p>for strikethrough, use ~this notation~</p>
   * <p>for color, use {color:#FF0000}this notation{color}.</p>
   * <p>for font size, use {size:16}this notation{size}</p>
   * @param text
   * @return text html formatted
   */
  def textFormatting(text: String): String = {
    var outputString = remplaceHexa(text)
    outputString = outputString.replace("\\r", "<br>")
    outputString = outputString.replace("\n", "<br>")

    val l = extractColorList(outputString)
    l.foreach {
      case (retcode, tokenColorARemplacer, color, texte) if retcode == true => outputString = outputString.replace(tokenColorARemplacer, "<font color='" + color + "'>" + texte + "</font>")
      case _ =>
    }
    val m = extractFontList(outputString)
    m.foreach {
      case (retcode, tokenFontARemplacer, sizeFont, textFont) if retcode == true => outputString = outputString.replace(tokenFontARemplacer, "<font size='" + sizeFont + "'>" + textFont + "</font>")
      case _ =>
    }
    val o = extractLinkListAvecLien(outputString)
    o.foreach {
      case (retcode, tokenLinkARemplacer, textUrl, urlLink) if retcode == true => outputString = outputString.replace(tokenLinkARemplacer, " <a href='" + urlLink + "'>" + textUrl + "</a>")
      case _ =>
    }
    val p = extractLinkListSansLien(outputString)
    p.foreach {
      case (retcode, tokenLinkSansLienARemplacer, textUrl, urlLink) if retcode == true => outputString = outputString.replace(tokenLinkSansLienARemplacer, " <a href='" + urlLink + "'>" + textUrl + "</a>")
      case _ =>
    }
    //val (ok1, tokenColorARemplacer, color, texte) = extractColor(outputString)
    //if (ok1) { outputString = outputString.replace(tokenColorARemplacer, "<font color='" + color + "'>" + texte + "</font>") }
    //  val (ok2, tokenFontARemplacer, sizeFont, texteFont) = extractFont(outputString)
    //  if (ok2) { outputString = outputString.replace(tokenFontARemplacer, "<font size='" + sizeFont + "'>" + texteFont + "</font>") }

    // val (ok3, tokenLinkARemplacer, textUrl, urlLink) = extractLink(outputString)
    // il y a un bug dans expression langauge, on a dû retourner le 1er caractere 
    //if (ok3) { outputString = outputString.replace(tokenLinkARemplacer, " <a href='" + urlLink.substring(1, urlLink.size) + "'>" + textUrl.substring(1, textUrl.size) + "</a>") }
    extractSubString(outputString, '_').foreach(partString => outputString = replaceString(outputString, partString, "<i>", "</i>"))
    extractSubString(outputString, '*').foreach(partString => outputString = replaceString(outputString, partString, "<b>", "</b>"))
    extractSubString(outputString, '&').foreach(partString => outputString = replaceString(outputString, partString, "<ins>", "</ins>"))
    extractSubString(outputString, '~').foreach(partString => outputString = replaceString(outputString, partString, "<del>", "</del>"))
    extractSubString(outputString, '-').foreach(partString => outputString = replaceString(outputString, partString, "<del>", "</del>"))

    // recherche des bulled list 
    var indicateurUl = false
    val ligne2s = outputString.split("<br>")
    var outputStringBulledList = ""
    var cpt = 0
    ligne2s.foreach(ligne => {
      cpt += 1
      if (ligne.trim.startsWith("* ") || ligne.trim.startsWith("- ")) {
        if (!indicateurUl) {
          outputStringBulledList += "<ul>"
          indicateurUl = true
        }
        outputStringBulledList += "<li>"
        outputStringBulledList += ligne.trim.substring(2)
        outputStringBulledList += "</li>"
        if (cpt == ligne2s.size) { // derniere ligne de la table => on ferme la liste
          outputStringBulledList += "</ul>"
          indicateurUl = false
        }
        if (cpt < ligne2s.size) outputStringBulledList += "<br>"

      } else {
        if (indicateurUl) {
          outputStringBulledList += "</ul>"
          indicateurUl = false
        }

        outputStringBulledList += ligne.trim
        if (cpt < ligne2s.size) outputStringBulledList += "<br>"

      }

    })

    outputStringBulledList
  }
  /**
   * @param text
   * @param subPart
   * @param htmlDeb
   * @param htmlFin
   * @return text replaced
   */
  private def replaceString(text: String, subPart: String, htmlDeb: String, htmlFin: String): String = {
    var outputString = ""
    if (subPart != "") { outputString = text.replace(subPart, htmlDeb + subPart.substring(1, subPart.size - 1) + htmlFin) }
    outputString
  }

  /**
   * format du texte : [Balsamiq Website](balsamiq.com)
   * @param text
   * @return ListBuffer(true or false, token, urlName, urlLink)
   */
  private def extractLinkListAvecLien(text: String): ListBuffer[(Boolean, String, String, String)] = {
    // URL renseignee ?? 
    val bufferRetour = new ListBuffer[(Boolean, String, String, String)]()

    val regExp1 = "\\[[^\\]]*\\]\\((.)*\\)".r
    val regExp2 = "\\[(.)*\\]".r
    val regExp20 = "[^\t\n\\]]*".r
    val regExp3 = "\\((.)*\\)".r
    val regExp30 = "[^\t\n\\)]*".r
    val expressionComplete = regExp1.findAllIn(text)
    expressionComplete.foreach(token => {
      if (token != "") {
        val urlName1 = regExp2.findFirstIn(token)
        var urlName = regExp20.findFirstIn(urlName1.getOrElse("")).getOrElse("")
        val urlLink1 = regExp3.findFirstIn(token)
        var urlLink = regExp30.findFirstIn(urlLink1.getOrElse("")).getOrElse("")
        // il y a un bug dans expression langauge, on a dû retourner le 1er caractere 
        if (urlName.startsWith("[")) urlName = urlName.substring(1)
        if (urlLink.startsWith("(")) urlLink = urlLink.substring(1)
        val retour = (true, token, urlName, urlLink)
        bufferRetour += retour

      } else {
        val retour = (false, "", "", "")
        bufferRetour += retour
      }
    })
    bufferRetour
  }
  // 
  /**
   * @param text : string
   * @return ListBuffer(true, token, urlName, "")
   */
  private def extractLinkListSansLien(text: String): ListBuffer[(Boolean, String, String, String)] = {
    val regExp1 = "\\[[^\\]]*\\]".r
    val regExp20 = "[^\t\n\\]]*".r
    val expressionComplete = regExp1.findAllIn(text)
    val bufferRetour = new ListBuffer[(Boolean, String, String, String)]()
    expressionComplete.foreach(token => {
      if (token != "") {
        var urlName = regExp20.findFirstIn(token).getOrElse("")
        if (urlName.startsWith("[")) urlName = urlName.substring(1)
        val retour = (true, token, urlName, "")
        bufferRetour += retour

      } else {
        val retour = (false, "", "", "")
        bufferRetour += retour
      }
    })
    bufferRetour
  }

  /**
   * Extract des fonts
   * @param text
   * @return ListBuffer(true, token, fontDecimal, texteDeLaFont)
   */
  private def extractFontList(text: String): ListBuffer[(Boolean, String, String, String)] = {
    //   val text = " // {color:#ffff} texte de la   couleur {color}"
    val regExp1 = "\\{size:\\d*\\}(.)*\\{size\\}".r
    val regExp2 = "\\{size:\\d*\\}".r
    val regExp20 = "(\\w*)$".r
    val regExp3 = "[a-zA-Z0-9_ ]*\\{size\\}".r
    val regExp30 = "[a-zA-Z0-9_ ]*".r
    val expressionComplete = regExp1.findAllIn(text)
    val bufferRetour = new ListBuffer[(Boolean, String, String, String)]()
    expressionComplete.foreach(token => {
      if (token != "") {
        val font = regExp2.findFirstIn(token)
        val fontDecimal = regExp20.findFirstIn(font.getOrElse("")).getOrElse("")
        val texteDeFin = regExp3.findFirstIn(token)
        val texteDeLaFont = regExp3.findFirstIn(texteDeFin.getOrElse("")).getOrElse("")

        val retour = (true, token, fontDecimal, texteDeLaFont)
        bufferRetour += retour
      } else {
        val retour = (false, "", "", "")
        bufferRetour += retour
      }

    })
    bufferRetour
  }
  /**
   * extraction de la couleur
   * format du texte: {color:#ffff}xxxxx{color}
   * @param text
   * @return Listbuffer((true or false, expressionComplete, colorHexa, texteDeLaCouleur))
   */
  private def extractColorList(text: String): ListBuffer[(Boolean, String, String, String)] = {
    //  val text = " // {color:#ffff} texte de la   couleur {color}"
    //   val regExp1 = "\\{color:#\\w*\\}([a-zA-Z0-9_ ]*)*\\{color\\}".r
    //  val regExp1 = "\\{color:#\\w*\\}[^\\{]*\\{color\\}".r
    val regExp1 = "\\{color:[a-z0-9#]*\\}(.)*\\{color\\}".r
    val regExp2 = "(\\{color:)[a-z0-9#]*".r
    val regExp20 = "(\\w*)$".r

    val regExp3 = "([a-zA-Z0-9@ ]*)(?:\\{color\\})".r
    val regExp30 = "[a-zA-Z0-9@_ ]*".r

    val bufferRetour = new ListBuffer[(Boolean, String, String, String)]()
    val x = regExp1.findFirstIn(text)

    val expressionComplete = regExp1.findAllIn(text)
    expressionComplete.foreach(token => {
      if (token != "") {
        val color = regExp2.findFirstIn(token)
        val colorHexa = regExp20.findFirstIn(color.getOrElse("")).getOrElse("")
        val texteDeFin = regExp3.findFirstIn(token)
        val texteDeLaCouleur = regExp30.findFirstIn(texteDeFin.getOrElse("")).getOrElse("")
        val retour = (true, token, colorHexa, texteDeLaCouleur)
        bufferRetour += retour
      } else {
        val retour = (false, "", "", "")
        bufferRetour += retour
      }

    })
    bufferRetour
  }

  /**
   * extraction de la couleur
   * format du texte: {color:#ffff}xxxxx{color}
   * @param text
   * @return (true or false, expressionComplete, colorHexa, texteDeLaCouleur)
   *
   */
  private def extractColor(text: String): (Boolean, String, String, String) = {
    //  val text = " // {color:#ffff} texte de la   couleur {color}"
    //   val regExp1 = "\\{color:#\\w*\\}([a-zA-Z0-9_ ]*)*\\{color\\}".r
    //  val regExp1 = "\\{color:#\\w*\\}[^\\{]*\\{color\\}".r
    val regExp1 = "\\{color:[a-z0-9#]*\\}(.)*\\{color\\}".r
    val regExp2 = "(\\{color:)[a-z0-9#]*".r
    val regExp20 = "(\\w*)$".r

    val regExp3 = "([a-zA-Z0-9@ ]*)(?:\\{color\\})".r
    val regExp30 = "[a-zA-Z0-9@_ ]*".r
    val expressionComplete = regExp1.findFirstIn(text)
    if (expressionComplete.getOrElse("") == "") {

      return (false, "", "", "")
    } else {
      logBack.debug("valeur expression complete {}", expressionComplete)
      val color = regExp2.findFirstIn(expressionComplete.getOrElse(""))
      val colorHexa = regExp20.findFirstIn(color.getOrElse(""))
      val texteDeFin = regExp3.findFirstIn(expressionComplete.getOrElse(""))
      val texteDeLaCouleur = regExp30.findFirstIn(texteDeFin.getOrElse(""))
      return (true, expressionComplete.getOrElse(""), colorHexa.getOrElse(""), texteDeLaCouleur.getOrElse(""))

    }
  }
  /**
   * <p>extration du lien dans un texte</p>
   * <p>format du texte: [[Balsamiq Website](balsamiq.com)</p>
   * @param text
   * @return (true, expressionComplete, urlName, urlLink))
   *
   */
  private def extractLink(text: String): (Boolean, String, String, String) = {
    // URL renseignee ?? 
    if (text.contains("(") && (text.contains(")"))) {
      //   val text = " // {color:#ffff} texte de la   couleur {color}"
      // [urlName] (urlLink)
      val regExp1 = "\\[(.)*\\](.)*\\((.)*\\)".r
      val regExp2 = "\\[(.)*\\]".r

      val regExp20 = "[^\t\n\\]]*".r

      val regExp3 = "\\((.)*\\)".r
      val regExp30 = "[^\t\n)]*".r
      val expressionComplete = regExp1.findFirstIn(text)
      if (expressionComplete.getOrElse("") == "") {
        logBack.debug("valeur de expression complete {}", expressionComplete)
        return (false, "", "", "")
      } else {
        val urlName1 = regExp2.findFirstIn(expressionComplete.getOrElse(""))
        val urlName = regExp20.findFirstIn(urlName1.getOrElse(""))
        val urlLink1 = regExp3.findFirstIn(expressionComplete.getOrElse(""))
        val urlLink = regExp30.findFirstIn(urlLink1.getOrElse(""))
        return (true, expressionComplete.getOrElse(""), urlName.getOrElse("  "), urlLink.getOrElse("  "))
      }

      // le lien n'est pas renseigné
    } else {
      val regExp1 = "\\[(.)*\\]".r

      val regExp20 = "[^\t\n\\]]*".r

      val expressionComplete = regExp1.findFirstIn(text)
      if (expressionComplete.getOrElse("") == "") {
        // logBack.debug("valeur de expression complete" + expressionComplete)
        return (false, "", "", "")
      } else {

        val urlName = regExp20.findFirstIn(expressionComplete.getOrElse(""))
        return (true, expressionComplete.getOrElse(""), urlName.getOrElse("  "), "  ")
      }
    }
  }

  /**
   * format du texte à extraire: {color:#ffff}xxxxx{color}
   * @param text
   * @return (true, expressionComplete, fontDecimal, texteDeLaFont)
   */
  private def extractFont(text: String): (Boolean, String, String, String) = {
    //   val text = " // {color:#ffff} texte de la   couleur {color}"
    val regExp1 = "\\{size:\\d*\\}(.)*\\{size\\}".r
    val regExp2 = "\\{size:\\d*\\}".r
    val regExp20 = "(\\w*)$".r
    val regExp3 = "[a-zA-Z0-9_ ]*\\{size\\}".r
    val regExp30 = "[a-zA-Z0-9_ ]*".r
    val expressionComplete = regExp1.findFirstIn(text)
    if (expressionComplete.getOrElse("") == "") {
      return (false, "", "", "")
    } else {
      val font = regExp2.findFirstIn(expressionComplete.getOrElse(""))
      val fontDecimal = regExp20.findFirstIn(font.getOrElse(""))
      val texteDeFin = regExp3.findFirstIn(expressionComplete.getOrElse(""))
      val texteDeLaFont = regExp3.findFirstIn(texteDeFin.getOrElse(""))
      return (true, expressionComplete.getOrElse(""), fontDecimal.getOrElse(""), texteDeLaFont.getOrElse(""))

    }

  }
  /**
   *
   * Les champs sont de type seperateur xxxxxxx separateur
   * expresssion regulière pour extraire le contenu du champ.
   *
   * @param text with separator
   * @param separateur
   * @return text without separator
   */
  private def extractSubString(text: String, separateur: Char): List[String] = {
    var sep = separateur.toString
    if (sep == "*") sep = "\\*"

    val str1 = sep + "[^" + sep + "]*" + sep
    // val str1="_([^_])* _".r
    val regExp1 = new scala.util.matching.Regex(str1)
    val list1 = new ListBuffer[String]()
    regExp1.findAllIn(text).foreach(valeur => {
      list1 += valeur
    })
    list1.toList
  }

  /**
   * @param s
   * @return s.toInt
   */
  def toInt(s: String): Int = {
    var i = if ((s != null) && (s.length() > 0)) { s.toInt }
    else 0;
    return i;
  }

  /**
   * @param s : String
   * @return s.toDouble
   */
  def toDouble(s: String): Double = {
    var d = 1.0D;
    if ((s != null) && (s.length() > 0)) { d = s.toDouble }
    return d;
  }
  /**
   * Recup contenu message erreur
   * @param mesId
   * @return content of message
   */
  def getContenuMessage(mesId: String): String = {
    try {
      val mes = propsMessages.getProperty(mesId);
      return mesId + ":" + mes
    } catch {
      case ex: Exception => return ex.getMessage

    }
  }
  /**
   * <p>*** attention le nom du fichier ne doit pas contenir le nom du répertoire ***</p>
   * <p>Détermination si le widget est un fragment</p>
   * <p>récupération du nom du fragment avec son type</p>
   * <p>Si fragment, récupération nom de l'écran contenant le fragment</p>
   * <p>Détermination s'il faut générer ou pas le contrôleur</p>
   *
   * @param DirectoryfileName
   * @return  (filename, useCaseName, isAfragment, fragment, generateController, ecranContenantLefragment, typeDeFragment)
   *
   */
  def getFileInformation(DirectoryfileName: String): (String, String, Boolean, String, Boolean, String, String) = {
    val filenameCompletSansSuffix = DirectoryfileName.split("\\.").head
    var filename = ""
    var useCaseName = ""
    var generateController = false
    // NomduFIchierEtUseCase
    val val1 = filenameCompletSansSuffix.replace(System.getProperty("file.separator"), "/")
    var fileNameAvecUsecase = if (val1.contains("/")) {
      val1.split("/").last // nom du fichier 
    } else val1
    // détermination du file Name et UseCase
    if (!fileNameAvecUsecase.contains(CommonObjectForMockupProcess.engineProperties.usecaseSeparator)) { filename = fileNameAvecUsecase }
    else {
      filename = fileNameAvecUsecase.split(CommonObjectForMockupProcess.engineProperties.usecaseSeparator).last
      useCaseName = fileNameAvecUsecase.split(CommonObjectForMockupProcess.engineProperties.usecaseSeparator).head
    }
    // <p>détermination si le widget est un fragment</p>
    // <p>récupération du nom du fragment avec son type</p> 
    // <p>Si fragment, récupération nom de l'écran contenant le fragment </p>
    // <p>Détermination s'il faut générer ou pas le contrôleur</p>
    val directoryName = ""
    val isAfragment = if (filename.toLowerCase().startsWith(CommonObjectForMockupProcess.generationProperties.generateControllerForPrefix.toLowerCase()) && filename.contains(CommonObjectForMockupProcess.engineProperties.fragmentSeparator)) true else false
    val fragmentAvecType = if (isAfragment) {
      filename.replace(CommonObjectForMockupProcess.engineProperties.fragmentSeparator, "/").split("/").last
    } else ""
    val ecranContenantLefragment = if (isAfragment) {
      filename.replace(CommonObjectForMockupProcess.engineProperties.fragmentSeparator, "/").split("/").head
    } else ""
    if (filename.toLowerCase().startsWith(CommonObjectForMockupProcess.generationProperties.generateControllerForPrefix.toLowerCase()) && !filename.contains(CommonObjectForMockupProcess.engineProperties.fragmentSeparator)) {
      generateController = true
    } else {
      generateController = false
    }
    // on split le nom du fragment pour récupérer le type
    val typeDeFragment = if (isAfragment) {
      if (fragmentAvecType.contains(CommonObjectForMockupProcess.engineProperties.fragmentTypeSeparator)) { fragmentAvecType.split(CommonObjectForMockupProcess.engineProperties.fragmentTypeSeparator).last }
      else { "" }
    } else { "" }
    val fragmentTypeSeparator = CommonObjectForMockupProcess.engineProperties.fragmentTypeSeparator
    val fragment = if (fragmentAvecType.contains(CommonObjectForMockupProcess.engineProperties.fragmentTypeSeparator)) { fragmentAvecType.split(CommonObjectForMockupProcess.engineProperties.fragmentTypeSeparator).head }
    else { fragmentAvecType }
    (filename, useCaseName, isAfragment, fragment, generateController, ecranContenantLefragment, typeDeFragment)

  }

  /**
   * <p>Récupération des informations du fichier en cours  : nom du fichier et nom du repertoire en cours</p>
   * <p>le nom du fichier est sous la forme usecase-fic-fic2</p>
   * @param file
   * @return (filename, directoryName, useCaseName, filenameComplet, isAfragment, fragmentName, generateController, ecranContenantLeFragment, typeDeFragment)
   * }
   */
  def getFileInformation(file: File): (String, String, String, String, Boolean, String, Boolean, String, String) = {
    val filenameComplet = file.getName().split("\\.").head
    var (filename, useCaseName, isAfragment, fragmentName, generateController, ecranContenantLeFragment, typeDeFragment) = getFileInformation(filenameComplet)
    val directoryName = file.getPath().replace(System.getProperty("file.separator"), "/").split("/").init.last // avant dernier 
    (filename, directoryName, useCaseName, filenameComplet, isAfragment, fragmentName, generateController, ecranContenantLeFragment, typeDeFragment)
  }
  /**
   * <p>creation de l'aborescence d'un répertoire avec appel récursif pour les sous répertoires</p>
   * <p>Le nom du répertoire est splitté (séparateur). Les répertoires sont créés s'ils n'existent pas.</p>
   * @param repositoryName
   */
  def createRepostoriesIfNecessary(repositoryName: String): Unit = {
    var tableauRepository = repositoryName.replace("\\", "/").split("/")
    var repertoireTravail = ""
    tableauRepository.foreach(repository => {
      if (repository == "") { repertoireTravail = repertoireTravail + System.getProperty("file.separator") } // cas du mac ou le repertoir commence /
      else {
        if (repertoireTravail != "") { repertoireTravail = repertoireTravail + System.getProperty("file.separator") + repository }
        else repertoireTravail = repository
        val file = new File(repertoireTravail)
        if (!file.exists()) {
          file.mkdir()
          logBack.info(getContenuMessage("mes33"), repertoireTravail);
        }
      }
    })

  }

  /**
   * @param fileName  : nom du Mockup ou du fragment
   * @param directoryName : nom du répertoire
   * @return emplacement du fichier html
   */
  def getEmplacementFichierHtml(fileName: String, directoryName: String): String = {
    val sourceHTML = if (CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement != "") {
      directoryName + System.getProperty("file.separator") +
        CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + System.getProperty("file.separator") +
        this.getRepositoryContainingFragmentAndMainScreen(CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement,CommonObjectForMockupProcess.isAfragment,CommonObjectForMockupProcess.typeDuFragmentEnCoursDeTraitement,CommonObjectForMockupProcess.ecranContenantLeSegment) + System.getProperty("file.separator") +
        fileName + "." + CommonObjectForMockupProcess.generationProperties.generatedFrontFilesSuffix;
    } else {
      directoryName + System.getProperty("file.separator") + fileName + "." + CommonObjectForMockupProcess.generationProperties.generatedFrontFilesSuffix
    }
    sourceHTML.trim
  }
  // nom du fichier java ou scala
  def getNomDuFichierCodeJavaOuScala(filenameAliasName: Tuple2[String, String]): String = {
    var ficJava = ""
    if (CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement != "") {
      ficJava = CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath + System.getProperty("file.separator") + CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement + System.getProperty("file.separator") + filenameAliasName._2 + System.getProperty("file.separator") + filenameAliasName._1.capitalize + "." + CommonObjectForMockupProcess.generationProperties.languageSource
    } else {
      ficJava = CommonObjectForMockupProcess.generationProperties.srcDtoFilesFullPath + System.getProperty("file.separator") + filenameAliasName._2 + System.getProperty("file.separator") + filenameAliasName._1.capitalize + "." + CommonObjectForMockupProcess.generationProperties.languageSource
    }
    ficJava.replace("\\", "/").trim

  }

  /**
   * modif le 9 avril 2015 : si le fichier est un fragment et que ce fragment à un type déclaré alors
   * le fragment sera localisé dans un sous répertoire récupéré de la table CommonObjectForMockupProcess.generationProperties.lookupTableTypeFragment
   * Pour un fragment: le répertoire est le nom du fichier principal contenant le fragment et le sous répertoire et le type de fragment
   * Modif le 20 avril 2015 : paramétrage de la fonction pour utilisation avec un ensemble de fragments
   * @return localisation du fichier
   */
  def getRepositoryContainingFragmentAndMainScreen(nomDuFichierEnCoursDeTraitement:String,isAfragment:Boolean,typeDuFragment:String,ecranContenantLeFragment:String): String = {
    val repertoireDeLEcranPrincipal = if (isAfragment) {
      // Si le fragment a un type déclaré on récupere le sous repertoire du type
      if (typeDuFragment != "") {
       val subDirectoryDuFragment= CommonObjectForMockupProcess.generationProperties.fragmentTypesList.getOrElse(typeDuFragment.toUpperCase(),"")
       if (subDirectoryDuFragment != "") {(ecranContenantLeFragment +  System.getProperty("file.separator") + subDirectoryDuFragment)}
       else {ecranContenantLeFragment}
      } else {ecranContenantLeFragment }
    } else {nomDuFichierEnCoursDeTraitement}
   
    repertoireDeLEcranPrincipal
  }
  
  /**
   * écriture d'un fichier sur disque (création du répertoire s'il n'existe pas).
   * @param filename : nom du fichier
   * @param buffer : buffer à écrire
   * @return
   */
  def ecrire_fichier(filename: String, buffer: String): Boolean = {

    val rep1 = filename.replace(System.getProperty("file.separator"), "/").split("/").init.mkString(System.getProperty("file.separator"))
    createRepostoriesIfNecessary(rep1)
    logBack.debug(getContenuMessage("mes48"), filename.toString)

    try {
      val fileWriter =
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename.replace("\\", "/").trim), CommonObjectForMockupProcess.constants.utf8));
      fileWriter.write(buffer)
      fileWriter.close
      true
    } catch {
      case ex: Exception =>
        logBack.error(getContenuMessage("mes49"), filename.toString, ex.getMessage().toString(), "x")
        false
    }
  }
  /**
   * <p>remplacement des mots clefs par leur contenu</p>
   * <p>mots clefs supportés :</p>
   *  <p>               %usecase% %ficname% %project% %controller% %controller?capitalize% ficname?capitalize%" %customProperty2% %customProperty3% %mainScreen%</p>
   * @param variable1
   * @return
   */
  def substituteKeywords(variable1: String): String = {
    val v1 = variable1.replace("%usecase%", CommonObjectForMockupProcess.nomDuUseCaseEnCoursDeTraitement)
    val v2 = v1.replace("%ficname%", CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement)
    val v3 = v2.replace("%project%", CommonObjectForMockupProcess.generationProperties.projectName)
    val v4 = v3.replace("%controller%", CommonObjectForMockupProcess.generationProperties.generatedControllerAlias)
    val v5 = v4.replace("%controller?capitalize%", CommonObjectForMockupProcess.generationProperties.generatedControllerAlias.capitalize)
    val v6 = v5.replace("%ficname?capitalize%", CommonObjectForMockupProcess.nomDuFichierEnCoursDeTraitement.capitalize)
    val v7 = v6.replace("%customProperty1%", CommonObjectForMockupProcess.templatingProperties.customProperty1)
    val v8 = v7.replace("%customProperty2%", CommonObjectForMockupProcess.templatingProperties.customProperty2)
    val v9 = v8.replace("%customProperty3%", CommonObjectForMockupProcess.templatingProperties.customProperty3)
    val v10 = v9.replace("%mainScreen%", CommonObjectForMockupProcess.ecranContenantLeSegment)
    v10
  }
  // -------------------------------------------------------------------------------

}