package fr.gabbro.balsamiq.parser.service.serviceimpl
// Gabbro - scala program to manipulate balsamiq sketches files an generate code with FreeMarker
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

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import fr.gabbro.balsamiq.parser.service.TTraitementCommun
import java.text.SimpleDateFormat
import java.io.FileNotFoundException
import scala.collection.mutable.ListBuffer
import ch.qos.logback.classic.Logger
import java.util.Date
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._

/**
 * @author FRA9972467
 */

class UtilZip extends TTraitementCommun {

  /**
   * @param archive
   * @return list of tuple (name of file, size, date)
   * return list of files contained in the archive
   *
   */
  def getFiles(archive: String): ListBuffer[(String, String, String)] = {
    val fileList = new ListBuffer[(String, String, String)]();

    try {
      val zipInputStream = new ZipInputStream(new FileInputStream(archive));
      var zipEntry = zipInputStream.getNextEntry();

      while (zipEntry != null) {
        val fileName = zipEntry.getName();
        val size = (zipEntry.getSize() / 1024).toString + " ko"

        val simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        val date = simpleDateFormat.format(new Date(zipEntry.getTime()));

        fileList += ((fileName, size, date));
        zipEntry = zipInputStream.getNextEntry();
      }
      zipInputStream.close

    } catch {
      case ex: Exception => println(ex.getMessage)
    }

    return fileList
  }

  /**
   * @param archive : String
   * @param destPath : String
   * unzip des fichiers bmml dans le répertoire destPath
   */
  def extractArchive(archive: String, destPath: String) {
    val buffer = new Array[Byte](2048);

    var zipInputStream = new ZipInputStream(new FileInputStream(archive));
    var zipEntry = zipInputStream.getNextEntry();
    utilitaire.createRepostoriesIfNecessary(destPath)
    while (zipEntry != null) {
      if (zipEntry.getName().endsWith(cstBalsamiqFileSuffix)) { // on n'extrait que les fichiers bmml
        val fileoutputstream = new FileOutputStream(destPath + "/" + zipEntry.getName());
        logBack.info(utilitaire.getContenuMessage("mes66"), zipEntry.getName(), archive, "xxx");

        var numberOfBytes = zipInputStream.read(buffer, 0, 2048)
        while (numberOfBytes > -1) {
          fileoutputstream.write(buffer, 0, numberOfBytes);
          numberOfBytes = zipInputStream.read(buffer, 0, 2048)
        }

        fileoutputstream.close();

      }
      zipInputStream.closeEntry();
      zipEntry = zipInputStream.getNextEntry();
    }
    zipInputStream.close()
  }

  /**
   * @param repository
   * scan du repository pour extraire les bmml des fichiers projets zippés (fonction export projet de balsamiq)
   */
  def scanRepositoryToExtractBmmlFile(repository: List[File]): Unit = {
    repository.foreach(file => {
      // on traite de façon itérative les sous répertoires.
      if ((file.isDirectory()) && (file.getName() != cstAssets)) { scanRepositoryToExtractBmmlFile(file.listFiles.toList) }
      else {
        // on ne sélectionne que les fichiers se terminant par .zip
        if (file.getName.endsWith(cstZip)) {
          extractArchive(file.getPath, file.getAbsoluteFile().getParentFile().getAbsolutePath())
          // file.delete // suppression de l'archive
        }
      }

    })
  }

}
