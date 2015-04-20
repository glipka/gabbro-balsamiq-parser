package fr.gabbro.balsamiq.parser.modelimpl

import java.io.File

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.ArrayBuffer

import org.jdom2.Element
import org.jdom2.input.SAXBuilder

import fr.gabbro.balsamiq.parser.model.TCatalogAPlat
import fr.gabbro.balsamiq.parser.model.composantsetendus.WidgetDeBase
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess
import fr.gabbro.balsamiq.parser.service.serviceimpl.MoteurTemplatingFreeMarker
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementBinding

class CatalogAPlat(fichierBalsamiq: File, moteurTemplateFreeMarker: MoteurTemplatingFreeMarker, traitementBinding: TraitementBinding, catalogDesComposants: CatalogDesComposants) extends TCatalogAPlat {
  /**
   * <p>chargement du catalog à plat : procédure appelée depuis IBalsamiqFreeMarker</p>
   * <p>on charge l'ensemble des widgets "à plat" sans se préoccuper de la notion de container</p>
   * <p>lors de cette 1ere phase de chargement, on prend en compte le cas des widgets regoupés, en particulier</p>
   * <p>en recalculant leur abscisse et ordonnée par rapport au début de la page</p>
   * <p>On vérifie qu'il n'y a pas des widgets qui ont la même taille et la même position (car l'algorithme ne peut fonctionner  correctement</p>
   * <p>On détermine les container pere incluant les widgets et pour chaque container, on met à jour les pointeurs fils (indice du widget).</p>
   * @return true or false,maxWidth, maxHeight
   */
  def chargementCatalog(): (Boolean, Int, Int) = {
    val builder = new SAXBuilder();
    try {
      doc = builder.build(fichierBalsamiq);
      mockup = doc.getRootElement();
    } catch {
      case e: Exception => {
        logBack.error(e.getMessage())
        return (false, 0, 0)
      }
    }
    traitementDesWidgets(mockup.getChild(CommonObjectForMockupProcess.constants.controls), null) // le groupe en cours est nul 
    rechercheDesFils(this.catalog) // pour chaque container, on renseigne les fils
    if (this.catalog.size > 0) { return (verification_doublon_catalogue, maxWidth(), maxHeight()) }
    else { return (false, 0, 0) } // catalogue vide

  }
  /**
   * <p>on verifie qu'il n'existe pas un widget qui a la même taille et la même position</p>
   * <p>dans ce cas, l'algorithme ne peut fonctionner</p>
   * @return true or false
   */
  def verification_doublon_catalogue(): Boolean = {
    for (i <- 0 until catalog.size) {
      for (j <- i + 1 until catalog.size) {
        if ((catalog(i).xAbsolute == catalog(j).xAbsolute) &&
          (catalog(i).yAbsolute == catalog(j).yAbsolute) &&
          (catalog(i).w == catalog(j).w) &&
          (catalog(i).h == catalog(j).h)) {
          logBack.error(utilitaire.getContenuMessage("mes26"), catalog(i).controlTypeID, catalog(i).xAbsolute.toString, catalog(i).yAbsolute.toString, catalog(j).controlTypeID, catalog(i).xAbsolute.toString(), catalog(i).yAbsolute.toString())
          return false
        }
      }
    }
    true
  }
  /**
   * **** Récupération des paramètres  ****
   * <p>Traitement de chaque widget du fichir mockup, widgets qui sont stockés dans la table catalog balsamiq à plat</p>
   * <p>On ne récupère que les attributs de chaque widget sans faire de traitement particulier</p>
   * <p>si le widget est un element d'un composant traité localement, on rcupere les
   * attributs en override du groupe. Les attributs en override sont alors stockés sous la forme "CustomIDuComposantCLef" -> valeur</p>
   * @param controlsXML : ELement
   * @param groupe_en_cours : WidgetDeBase
   */
  private def traitementDesWidgets(controlsXML: Element, groupe_en_cours: WidgetDeBase) {
    val controlXML = controlsXML.getChildren(CommonObjectForMockupProcess.constants.control).toList
    controlXML.foreach(elementXML => {
      val controle_en_cours = new InstanciationTypeDeWidget(id_interne, groupe_en_cours, elementXML, traitementBinding, catalogDesComposants).process() // traitement du contrôle en cours
      // bypass des widgets ayant l'attribut markup positionné à true
      //FIXME bug markup - 
      if (controle_en_cours.mapExtendedAttribut.getOrElse(CommonObjectForMockupProcess.constants.markup, "") != "true") { // if1
        // controle en cours est un composant qui doit être traité localement ?
        if (controle_en_cours.isAComponent && controle_en_cours.componentXML != null && List(controle_en_cours.componentName).intersect(CommonObjectForMockupProcess.templatingProperties.widgetsListProcessedLocally).size > 0) {
          traitementGroupe(controle_en_cours.componentXML, controle_en_cours) // on traite le code xml du composant qui a été récupéré dans le traitement du catalogue des composants
        } // le controle groupe n'est pas mis en table, mais va servir à recalculer les coordonnées du fils 
        // l'id interne n'est pas incrémenté après un grpupe car il sert à récuperer les adresses des élements
        else if (controle_en_cours.controlTypeID == CommonObjectForMockupProcess.constants.groupConstante) traitementGroupe(elementXML, controle_en_cours)
        else {
          // si le widget est un element d'un composant traité localement, on récupère les 
          // attributs en override du groupe.
          // les attributs en override sont stockés sous la forme "CustomIDuComposantCLef" -> valeur
          if (groupe_en_cours != null && groupe_en_cours.isAComponent) {
            groupe_en_cours.mapExtendedAttribut.foreach(valeur => {
              if(controle_en_cours.mapExtendedAttribut.getOrElse(CommonObjectForMockupProcess.constants.markup, "") == "true"){
              
                // la clef commence par le cutomID du widget du commposant ? 
                if (controle_en_cours.customId != "" && valeur._1.toString().startsWith(controle_en_cours.customId)) {
                  //on recupre le contenu de la clef
                  val clefRecherche = valeur._1.substring(controle_en_cours.customId.size).toLowerCase()
                  controle_en_cours.mapExtendedAttribut.remove(clefRecherche)
                  controle_en_cours.mapExtendedAttribut += (clefRecherche -> valeur._2)
                }
              }
            })

          }
          catalog += controle_en_cours
          id_interne = id_interne + 1
        }
      } // fin de if1 (CommonObjectForMockupProcess.constants.markup, "") != "true") 

    }) // fin de control.foreach 
  }
  /**
   * <p>traitement d'un groupe :<p>
   * <p>On traite les éléments du noeud : groupChildrenDescriptors</p>
   * <p><control controlID="0" controlTypeID="__group__" x="0" y="0" w="906" h="422" measuredW="906" measuredH="422" zOrder="0" locked="false" isInGroup="11"></p>
   * <p>      <groupChildrenDescriptors></p>
   * @param elementXML
   * @param groupe_en_cours
   */
  private def traitementGroupe(elementXML: Element, groupe_en_cours: WidgetDeBase): Unit = {
    val groupChildrenDescriptor = elementXML.getChild(CommonObjectForMockupProcess.constants.groupChildrenDescriptors);
    return traitementDesWidgets(groupChildrenDescriptor, groupe_en_cours)
  }
  /**
   *  largeur maximum ligne ecran (abscisse du widget  + largeur du widget)
   * @return mas Width
   */
  def maxWidth(): Int = {
    var max_width = 0 // détermination de la largeur maximum d'un écran (pour le calcul de la position relative.
    catalog.foreach(controle => {
      if ((controle.xAbsolute + controle.w) > max_width) { max_width = controle.xAbsolute + controle.w }
    })
    logBack.info(utilitaire.getContenuMessage("mes1"), max_width)
    max_width
  }
  /**
   * largeur maximum ligne ecran (abscisse du widget  + largeur du widget)
   * @return
   */
  def maxHeight(): Int = {
    var max_height = 0 // détermination de la largeur maximum d'un écran (pour le calcul de la position relative.
    catalog.foreach(controle => {
      if ((controle.yAbsolute + controle.h) > max_height) { max_height = controle.yAbsolute + controle.h }
    })
    logBack.info(utilitaire.getContenuMessage("mes2"), max_height)

    max_height
  }

  /**
   * <p>***** balayage de la table des contrôles *****</p>
   * <p>Pour chaque widget on recherche l'ensemble des containers de ce widget</p>
   * <p>on met à jour la table des fils du plus petit widget incluant le widget en cours</p>
   * <p>on fait une premiere passe pour renseigner le pointeur pere, puis on trie branche par branche les fils par position (abscisse,ordonnée)</p>
   * <p>la table des contrôles est triée par taille de surface croissante.</p>
   * @param catalogAPlat : ArrayBuffer[WidgetDeBase]
   */
  private def rechercheDesFils(catalogAPlat: ArrayBuffer[WidgetDeBase]): Unit = {
    var position_table_controle = 0
    var j: Int = 0
    // on verifie que chaque widgt est inclus dans l'autre
    while (position_table_controle < catalogAPlat.size) { // while1
      //var fin_recherche = false
      val tableauDesPlusGrandsRectangles = new ArrayBuffer[WidgetDeBase]
      j = 0;
      catalogAPlat(position_table_controle).pointer_pere = -1
      // tableau desplusgrandsRectangles contient les + grands rectangles incluant le rectangle en cours
      while (j < catalogAPlat.size) {
        if ((j != position_table_controle) && catalogAPlat(position_table_controle).estCompletementInclusDans(catalogAPlat(j))) // intersection des rectangles 
        {
          tableauDesPlusGrandsRectangles += catalogAPlat(j)
          catalogAPlat(position_table_controle).pointer_pere = 0

          val m1 = catalogAPlat(position_table_controle).controlID.toString
          val m2 = catalogAPlat(position_table_controle).controlTypeID.toString()
          val m3 = catalogAPlat(j).controlID.toString
          val m4 = catalogAPlat(j).controlTypeID.toString()

          logBack.debug(m1 + " " + m2 + " is included " + " in " + m3 + " " + m4)

        }
        j = j + 1

      } // fin du while2
      // on a potentiellement plusieurs rectangles incluant le widget. 
      // 
      tableauDesPlusGrandsRectangles.size match {
        case 0 =>

        case _ => { // on est dans le cas ou l'on a plusieurs rectangles

          // on prend le + petit rectangle incluant le widget
          val tableauDesPlusGrandsRectanglesTrie = tableauDesPlusGrandsRectangles.sortWith((widget1, widget2) => (widget1.w * widget1.h) < (widget2.w * widget2.h))
          val lePlusPetitRectanglePere = tableauDesPlusGrandsRectanglesTrie(0)
          // on indique dans le pere  l'indice du fils (Le pere est le + petit rectangle incluant le fils
          lePlusPetitRectanglePere.indice_des_fils += catalogAPlat(position_table_controle).id_interne
          //     logBack.debug("id plus petit rectangle pere " + lePlusPetitRectanglePere.id_interne + " perecopie  " + lePlusPetitRectanglePere.controlID + "  " + catalogAPlat(lePlusPetitRectanglePere.id_interne).controlID)
          catalogAPlat(lePlusPetitRectanglePere.id_interne) = lePlusPetitRectanglePere
        }
      }
      position_table_controle = position_table_controle + 1

    } // fin de while1

  }

}