package fr.gabbro.balsamiq.parser.service.serviceimpl
// Gabbro - scala program to manipulate balsamiq sketches files an generate code with FreeMarker
// Version 1.0
// Copyright (C) 2015 Georges Lipka
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

//import scala.reflect.io.Directory
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

  /* (non-Javadoc)
 * @see freemarker.cache.URLTemplateLoader#getURL(java.lang.String)
 */
def getURL(templateName: String): URL = {
    val baseURLDesTemplates = CommonObjectForMockupProcess.templatingProperties.freemarkerTemplatesDir
    val (ret, url) = rechercheFileInSubDirectories(baseURLDesTemplates, templateName.trim)
    url
  }
   /**
   * on recherche le fichier dnas le repertoire en cours en traitant les sous répertoires
 * @param directoryName : String
 * @param templateName : String  
 * @return (true or false, url:URL)
 */
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