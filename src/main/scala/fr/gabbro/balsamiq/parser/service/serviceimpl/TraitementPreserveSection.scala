package fr.gabbro.balsamiq.parser.service.serviceimpl

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths

import scala.beans.BeanProperty
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.Map

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
  private var mapDesPreserveSectionDuFichierAvantEcriture: Map[Int, (Boolean, String, String)] = Map() //= n° de section valeur : indicaeur deja traite, contenu de la preserve, sceau de la preserve //  private var maptemplateByKeyNumber = Map[String, Int]() // clef = templateName valeur=n° de clef
  //  private var numeroSection = 0
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
    if ((fichierEnCoursDeTraitement.endsWith(CommonObjectForMockupProcess.generationProperties.generatedFrontFilesSuffix)) ||
      (CommonObjectForMockupProcess.generationProperties.generatedOtherConfFilesSuffix.exists(suffix => fichierEnCoursDeTraitement.endsWith(suffix)))) {
      preserveSectionBegin = CommonObjectForMockupProcess.templatingProperties.preserveSectionFrontBegin
      preserveSectionEnd = CommonObjectForMockupProcess.templatingProperties.preserveSectionFrontEnd
      //   println("*****valeur de preserve section %s  %s".format(preserveSectionBegin,fichierEnCoursDeTraitement))
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
      mapDesPreserveSectionDuFichierAvantEcriture = traitementPreserve(bufferATraiter.mkString("\r\n"), 0, Map[Int, (Boolean, String, String)]()) // enrichissement de la table mapDesPreserveSection
      this // on retourne l'instance de l'objet

    } catch {
      case ex: Exception => {
        val exception = ex
        println(ex.getMessage)
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
   * @return  void
   */
  private def traitementPreserve(bufferATraiter: String, numeroSection: Int, mapPreserveSection: Map[Int, (Boolean, String, String)]): Map[Int, (Boolean, String, String)] = {
    val positionPreserveDebut = bufferATraiter.indexOf(preserveSectionBegin)
    val positionPreserveFin = bufferATraiter.indexOf(preserveSectionEnd, 1)
    if (positionPreserveDebut >= 0) {
      if (positionPreserveFin > 0 && positionPreserveFin > positionPreserveDebut + preserveSectionBegin.size) {
        val content = bufferATraiter.substring(positionPreserveDebut + preserveSectionBegin.size, positionPreserveFin)
        val sceau = recuperationNpremiersCaracteresAvantLaPreserve(bufferATraiter, positionPreserveDebut)
        mapPreserveSection += (numeroSection -> (false, content, sceau))
        traitementPreserve(bufferATraiter.substring(positionPreserveFin + preserveSectionEnd.size), numeroSection + 1, mapPreserveSection)
      } else { return mapPreserveSection }
    } else { return mapPreserveSection }

  }
  /**
   * <p>On récupère les n premiers caractères non vides avant le contenu de la preserve section
   *
   * @param bufferATraiter
   * @param position départ dans le buffer
   * @return content of n premier caracteres
   */
  private def recuperationNpremiersCaracteresAvantLaPreserve(bufferATraiter: String, positionDepart: Int): String = {
    var sceau = ""
    val tailleMaxiSceau = 10
    var position = positionDepart
    while (position > 0 && sceau.size < tailleMaxiSceau) {
      if (bufferATraiter.substring(position, position + 1).trim != "") {
        sceau += bufferATraiter.substring(position, position + 1)
      }
      position = position - 1 // caractère précédent dans le buffer 
    }
    sceau

  }
  /**
   * <p> *** Remplacement du contenu de la preserve section ***
   * <p> Afin d'éviter un décalage dans le remplacement du contenu des preserve sections.
   * <p> <b>1er passe </b>: on balaie l'ensemble des preserve sections du fichier à générer et on sélectionne les
   * <p> preserves sections ayant une correspondance avec les preserve sections de la précédente génération sur le contenu du  sceau et le n° de section.
   * <p> Pour les preserve sections concernées,
   * <p> on met à jour l'indicateur de traitement et on récupère le contenu
   *
   * <p> <b>2eme passe </b>: on balaie les preserve sections du fichier à générer pas traitées dans la 1ere passe et on sélectionne les
   * <p> preserves sections ayant une correspondance avec les preserve sections de la précédente génération sur le contenu du  sceau et n'ayant pas déjà été traitées.
   * <p> Pour les preserve sections concernées, on met à jour l'indicateur de traitement et on récupère le contenu
   * <p>
   *
   * <p> <b>3eme passe </b>: on balaie les preserve sections du fichier à générer pas traitées dans la 2eme passe et on sélectionne les
   * <p> preserves sections  de la précédente génération qui ne sont pas encore traitées. (on prend la 1ere disponible)
   * <p> Pour les preserve sections concernées, on met à jour l'indicateur de traitement et on récupère le contenu
   * <p>
   *
   *
   *
   * @param bufferATraiter
   * @return buffer avec contenu des preserve sections
   */
  def replacePreserveSection(bufferATraiter: String): String = {
    var mapDesPreserveSectionDuFichierCible =
      traitementPreserve(bufferATraiter, 0, Map[Int, (Boolean, String, String)]()) // enrichissement de la table mapDesPreserveSection

    // Premiere passe : même sceau et même indice
    // on selectionne les preserve section du fichier cible et source et on vérifie entrée par entrée s'il y a correspondance avec le sceau. 
    // si oui => on récupère le contenu source que l'on met en cible et on met à jour l'indicateur de modifs pour la 
    // source et la cible. 
    for (ind1 <- 0 until mapDesPreserveSectionDuFichierCible.size) {
      val preserveApres = mapDesPreserveSectionDuFichierCible.getOrElse(ind1, (false, "", ""))
      val preserveAvant = mapDesPreserveSectionDuFichierAvantEcriture.getOrElse(ind1, (false, "", ""))
      val sceauApres = preserveApres._3
      val sceauAvant = preserveAvant._3

      if (sceauApres == sceauAvant) {
        mapDesPreserveSectionDuFichierCible(ind1) = (true, preserveAvant._2, preserveAvant._3)
        mapDesPreserveSectionDuFichierAvantEcriture(ind1) = (true, preserveAvant._2, preserveAvant._3) // on met l'indicateur de traitement à true
      }
    }

    // Deuxieme passe : même sceau mais pas même position
    // on selectionne les preserve section du fichier cible.
    // pour chaque entrée de la cible pas encore traitée on cherche sur l'ensemble des entrées pas encore traitées de la source  s'il y a correspondance avec le sceau. 
    // si oui => on récupère le contenu source que l'on met en cible et on met à jour l'indicateur de modifs pour la 
    // source et la cible. 

    for (ind1 <- 0 until mapDesPreserveSectionDuFichierCible.size) {
      val preserveApres = mapDesPreserveSectionDuFichierCible.getOrElse(ind1, (false, "", ""))
      val sceauApres = preserveApres._3
      val contenuApres = preserveApres._2
      val modifApres = preserveApres._1
      if (!modifApres) { // enrgt pas encore modifie dans la passe précédente
        var finRecherche = false
        for (ind2 <- 0 until mapDesPreserveSectionDuFichierAvantEcriture.size) {
          if (!finRecherche) { //pas de break en scala
            val preserveAvant = mapDesPreserveSectionDuFichierAvantEcriture.getOrElse(ind2, (false, "", ""))
            val sceauAvant = preserveAvant._3
            val contenuAvant = preserveAvant._2
            val modifAvant = preserveAvant._1
            // preserve pas déjà utilisé et même sceau avant et après
            if (!modifAvant && sceauAvant == sceauApres) {
              mapDesPreserveSectionDuFichierCible(ind1) = (true, contenuAvant, sceauAvant)
              mapDesPreserveSectionDuFichierAvantEcriture(ind2) = (true, contenuAvant, sceauAvant) // on met l'indicateur de traitement à true
              finRecherche = true
            }
          }
        }
      }
    } // fin de la 2eme passe 
    // Troisième passe : premiere valeur disponible
    // Deuxieme passe : même sceau mais pas même position
    // on selectionne les preserve section du fichier cible pas encore traitées
    // pour chaque entrée de la cible la premiere entrée de la source pas encore traitée.
    // puis on récupère le contenu source que l'on met en cible et on met à jour l'indicateur de modifs pour la 
    // source et la cible. 

    for (ind1 <- 0 until mapDesPreserveSectionDuFichierCible.size) {
      val preserveApres = mapDesPreserveSectionDuFichierCible.getOrElse(ind1, (false, "", ""))
      val sceauApres = preserveApres._3
      val contenuApres = preserveApres._2
      val modifApres = preserveApres._1
      if (!modifApres) { // enrgt pas encore modifie dans la passe précédente
        var finRecherche = false
        for (ind2 <- 0 until mapDesPreserveSectionDuFichierAvantEcriture.size) {
          if (!finRecherche) { //pas de break en scala
            val preserveAvant = mapDesPreserveSectionDuFichierAvantEcriture.getOrElse(ind2, (false, "", ""))
            val sceauAvant = preserveAvant._3
            val contenuAvant = preserveAvant._2
            val modifAvant = preserveAvant._1
            // preserve pas déjà utilisé et même sceau avant et après
            if (!modifAvant) {
              mapDesPreserveSectionDuFichierCible(ind1) = (true, contenuAvant, sceauAvant)
              mapDesPreserveSectionDuFichierAvantEcriture(ind2) = (true, contenuAvant, sceauAvant) // on met l'indicateur de traitement à true
              finRecherche = true
            }
          }
        }
      }
    } // fin de la 3eme passe

    var bufferModifie = bufferATraiter
    var positionGlobaleDansLeBuffer = 0
    // ----------------------------------------------------------------------
    // insertion du contenu des  preserve sections dans le code
    // ----------------------------------------------------------------------
    for (key <- 0 until mapDesPreserveSectionDuFichierCible.size) {
      if (positionGlobaleDansLeBuffer < bufferModifie.size) { // on est toujours dans les limites du buffer à traiter
        val valeurPreserveSectionSauvegardee = mapDesPreserveSectionDuFichierCible.getOrElse(key, (false, "", ""))._2 // n° de preserve section
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
