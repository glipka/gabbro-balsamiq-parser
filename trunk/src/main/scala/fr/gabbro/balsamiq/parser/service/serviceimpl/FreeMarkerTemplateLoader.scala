package fr.gabbro.balsamiq.parser.service.serviceimpl

import freemarker.cache.URLTemplateLoader
import freemarker.cache.TemplateLoader
import java.net.URL
import java.io.File
import java.net.URI

/**
 *  template loader pour charger un template
 * cette permet de rechercher un template dans une arborescence de répertoires
 * cette fonctionnalité va permettre de structurer l'emplacement des templates
 *
 * @author fra9972467
 *
 */
class ClassTemplateLoader extends URLTemplateLoader {

  def getURL(templateName: String): URL = {
    val baseURLDesTemplates = CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir
    val (ret, url) = rechercheFileInSubDirectories(baseURLDesTemplates, templateName.trim)
    url
  }
  // on recherche le fichier dnas le repertoire en cours en traitant les sous répertoires
  def rechercheFileInSubDirectories(directoryName: String, templateName: String): (Boolean, URL) = {
    val directory = new File(directoryName).listFiles().toList
    directory.foreach(file => {
      if (file.isDirectory()) {
        val (ret, url) = rechercheFileInSubDirectories(file.getAbsolutePath, templateName)
        if (ret) {
          return (true, url)
        }
      } else { // c'est un fichier
        if (file.getName().trim.toLowerCase() == templateName.toLowerCase()) {
          return (true, file.toURL)
        }
      }
    })
    (false, null)
  }
}