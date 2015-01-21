package fr.gabbro.balsamiq.parser.service

import org.slf4j.LoggerFactory
import scala.beans.BeanProperty
import fr.gabbro.balsamiq.parser.service.serviceimpl.MoteurAnalyseJericho
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementMenu
import fr.gabbro.balsamiq.parser.service.serviceimpl.TraitementFormatageSourceJava
import fr.gabbro.balsamiq.parser.modelimpl.CatalogDesComposants
import fr.gabbro.balsamiq.parser.modelimpl.GlobalContext
import fr.gabbro.balsamiq.parser.modelimpl.Utilitaire
import fr.gabbro.balsamiq.parser.service.serviceimpl.MoteurTemplatingFreeMarker

 
// trait
trait TIBalsamiqFreeMarker {
  protected var moteurTemplateFreeMarker: MoteurTemplatingFreeMarker = _
  protected var moteurJericho: MoteurAnalyseJericho = _
  protected val logBack = LoggerFactory.getLogger(this.getClass());
  protected var utilitaire: Utilitaire = new Utilitaire
  protected var catalogDesComposantsCommuns: CatalogDesComposants = _
  @BeanProperty val globalContext = new GlobalContext() // zone de partage pour l'ensemble des maquettes
  protected var traitementMenu: TraitementMenu = _
  val traitementFormatageSourceJava = new TraitementFormatageSourceJava

}