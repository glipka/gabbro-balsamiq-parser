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
// See the individual licence texts for more details.package fr.gencodefrombalsamiq.service.serviceimpl

import freemarker.cache.URLTemplateLoader
import freemarker.cache.TemplateLoader
import java.net.URL
import java.io._
import java.nio.file.Paths
import java.net.URI
import java.io.FileReader
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.charset.Charset
import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import scala.collection.mutable.ArrayBuffer
import org.mozilla.javascript.BeanProperty
import scala.beans.BeanProperty
import java.nio.file.NoSuchFileException
import fr.gabbro.balsamiq.parser.service.TTraitementCommun

/**
 *
 * <p>Traitement des preserves section</p>
 * <p>chaque section commence par preserve debut et est suivie par le nom du template.</p>
 * <p>chaque section se termine par preserve fin</p>
 * <p>on récupére le code dans chaque section que l'on met en table sous forme de tuple</p>
 * <p>on utilise deux hashMap pour retrouver le contenu des sections </p>
 * <p>Une première Map dont la clef est le N° de section et le nom du template </p>
 * <p>Une deuxième Map permet de retrouver le n° de section pour le template en cours. </p>
 * <p>Pour mémoriser les n° de sections extraites par template (grace à la map mapDesPreserveSection)
 * @author georges Lipka
 *
 */
class TraitementPreserveSection extends TTraitementCommun {

  // la map des preservesections
  private var mapDesPreserveSection = Map[Int, String]() // clef = n° de section et nom de template
  //  private var maptemplateByKeyNumber = Map[String, Int]() // clef = templateName valeur=n° de clef
  private var numeroSection = 0
  var fichierEnCoursDeTraitement = ""
  @BeanProperty var fichierPresent = false
  var preserveSectionBegin = ""
  var preserveSectionEnd = ""

  /**
   * lecture du fichier pour extraire les preserve sections
   * @param filename : nom du fichier à traiter
   * @return traitementPreserveSection
   */
  def process(filename: String): TraitementPreserveSection = {
    var bufferATraiter = List[String]()
    fichierEnCoursDeTraitement = filename
    // la syntaxe des preserve section est différente selon le type de fichier (jsp ou java)
    if (fichierEnCoursDeTraitement.endsWith(CommonObjectForMockupProcess.generationProperties.generatedFrontFilesSuffix)) {
      preserveSectionBegin = CommonObjectForMockupProcess.templatingProperties.preserveSectionFrontBegin
      preserveSectionEnd = CommonObjectForMockupProcess.templatingProperties.preserveSectionFrontEnd
      println("*****valeur de preserve section %s  %s".format(preserveSectionBegin,fichierEnCoursDeTraitement))
    } else {
      preserveSectionBegin = CommonObjectForMockupProcess.templatingProperties.preserveSectionCodeBegin
      preserveSectionEnd = CommonObjectForMockupProcess.templatingProperties.preserveSectionCodeEnd
    }
    logBack.debug(utilitaire.getContenuMessage("mes50"), filename.toString)
    try {
      // on met en mémoire le fichier à traiter
      try {
        bufferATraiter = Files.readAllLines(Paths.get(filename.trim.replace("\\", "/")), Charset.defaultCharset()).toList
      } catch {

        case nsfex: NoSuchFileException => return this
        case ex: Exception =>
          logBack.error(utilitaire.getContenuMessage("mes51"), filename, ex.getMessage, "x");
          return this

        case ex: Exception =>
          val exception = ex;
          logBack.error(utilitaire.getContenuMessage("mes51"), filename, ex.getMessage, "x"); return this

      }
      fichierPresent = true
      numeroSection = 0
      traitementPreserve(bufferATraiter.mkString("\r\n")) // enrichissement de la table mapDesPreserveSection
      this // on retourne l'instance de l'objet

    } catch {
      case ex: Exception => {
        val exception = ex
        logBack.error(utilitaire.getContenuMessage("mes38"), filename.toString)
        null
      }
    }

  }
  /**
   * <p>On récupère le contenu entre le header debut et le header fin</p>
   * <p>le nom du template qui a généré la preserve section est juste derrière le header.</p>
   * <p>que l'on met dans une hashMap.</p>
   * <p>Attention la syntaxe des preserve sections est différente selon le type de fichier (jsp ou java)</p>
   *
   * @param bufferATraiter
   */
  private def traitementPreserve(bufferATraiter: String): Unit = {
    val positionPreserveDebut = bufferATraiter.indexOf(preserveSectionBegin)
    val positionPreserveFin = bufferATraiter.indexOf(preserveSectionEnd, 1)
    if (positionPreserveDebut >= 0) {
      if (positionPreserveFin > 0 && positionPreserveFin > positionPreserveDebut + preserveSectionBegin.size) {
        val content = bufferATraiter.substring(positionPreserveDebut + preserveSectionBegin.size, positionPreserveFin)
        mapDesPreserveSection += (numeroSection -> content)
        numeroSection += 1 // incrémentation de la clef de la Map.
        traitementPreserve(bufferATraiter.substring(positionPreserveFin + preserveSectionEnd.size))
      } else { return }
    } else { return }

  }
  /**
   * <p> *** Remplacement du contenu de la preserve section ***
   * <p> Lecture de la map mapDesPreserveSection par n° croissant de clef et récupération du contenu de la preserve section à remplacer.
   * <p> Positionnement sur la preserve section et remplacement du contenu précédemment lu.
   * <p>
   *
   * <p>
   *
   * @param bufferATraiter
   */
  def replacePreserveSection(bufferATraiter: String): String = {
    var bufferModifie = bufferATraiter
    var positionGlobaleDansLeBuffer = 0
    for (key <- 0 until mapDesPreserveSection.size) {
      if (positionGlobaleDansLeBuffer < bufferModifie.size) { // on est toujours dans les limites du buffer à traiter
        val valeurPreserveSectionSauvegardee = mapDesPreserveSection.getOrElse(key, "") // n° de preserve section
        val positionPreserveDebut = bufferModifie.indexOf(preserveSectionBegin, positionGlobaleDansLeBuffer)
        val positionPreserveFin = bufferModifie.indexOf(preserveSectionEnd, positionGlobaleDansLeBuffer)
        if ((positionPreserveDebut >= 0) && (positionPreserveFin > 0) && (positionPreserveFin >= positionPreserveDebut + preserveSectionBegin.size)) {
          val contenuARemplacer = bufferModifie.substring(positionPreserveDebut + preserveSectionBegin.size, positionPreserveFin)
          // on recopie le buffer en changeant le contenu de la preserve section
          bufferModifie = bufferModifie.substring(0, positionPreserveDebut + preserveSectionBegin.size) + valeurPreserveSectionSauvegardee + bufferModifie.substring(positionPreserveFin)
          positionGlobaleDansLeBuffer = positionPreserveDebut + preserveSectionBegin.size + valeurPreserveSectionSauvegardee.size + preserveSectionEnd.size // on se positionne après la fin de preserve section.
        }
      }

    }
    bufferModifie

  }

}
