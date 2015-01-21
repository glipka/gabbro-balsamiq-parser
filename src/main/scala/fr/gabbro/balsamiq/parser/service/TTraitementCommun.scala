package fr.gabbro.balsamiq.parser.service

import org.slf4j.LoggerFactory

import fr.gabbro.balsamiq.parser.modelimpl.Utilitaire
 

trait TTraitementCommun {
  protected val logBack = LoggerFactory.getLogger(this.getClass());
  protected val utilitaire = new Utilitaire
}