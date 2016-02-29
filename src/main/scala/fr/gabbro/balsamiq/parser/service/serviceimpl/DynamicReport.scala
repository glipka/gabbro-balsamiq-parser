package fr.gabbro.balsamiq.parser.service.serviceimpl

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder
import net.sf.dynamicreports.report.builder.DynamicReports._
import net.sf.dynamicreports.report.builder.DynamicReports.{ `type` => type1 }
import net.sf.dynamicreports.report.datasource.DRDataSource
import net.sf.jasperreports.engine.JRDataSource
import net.sf.dynamicreports.report.constant._
import java.awt.Color
import java.io.FileOutputStream
import fr.gabbro.balsamiq.parser.modelimpl.GlobalContext
import fr.gabbro.balsamiq.parser.modelimpl.Utilitaire
import fr.gabbro.balsamiq.parser.service.serviceimpl.CommonObjectForMockupProcess.constants._
import net.sf.dynamicreports.report.builder.group.GroupBuilder
import javax.swing.GroupLayout
import java.text.DateFormat
import java.util.Date;
import java.text.SimpleDateFormat

//import net.sf.dynamicreports.report.builder.DynamicReports.{ `type` => type1 }

class DynamicReport(globalContext: GlobalContext, utilitaire: Utilitaire) {

  val format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
  // si le repertoire de generation des rapports est renseigné  => 
  val directory = if (CommonObjectForMockupProcess.generationProperties.reportGenerationDir != "") {
    CommonObjectForMockupProcess.generationProperties.reportGenerationDir
  } else {
    CommonObjectForMockupProcess.generationProperties.balsamiqMockupsDir + "/" + cstReporting

  }
  utilitaire.createRepostoriesIfNecessary(directory)
  val fileName = directory + "/" + cstReporting + "-" + format.format(new Date()) + cstSuffixPdf
  val f1 = new FileOutputStream(fileName)
  val rep1 = createReport()
  val pdf1 = rep1.toPdf(f1)
  // création d'un rapport 
  def createReport(): JasperReportBuilder = {

    try {

      val boldStyle = stl.style().bold();
      val boldCenteredStyle = stl.style(boldStyle).setHorizontalAlignment(HorizontalAlignment.CENTER);
      val columnTitleStyle = stl.style(boldCenteredStyle).setBorder(stl.pen1Point()).setBackgroundColor(Color.LIGHT_GRAY).setFontSize(8)
      val centeredStyle = stl.style().setHorizontalAlignment(HorizontalAlignment.CENTER).setFontSize(7)
      val leftStyle = stl.style().setHorizontalAlignment(HorizontalAlignment.LEFT).setFontSize(7)

      //("bmml", "templateID", "componant","message","description","gravity")
      val bmml = col.column(cstBmml, cstBmml, type1.stringType()).setStyle(centeredStyle).setWidth(20)
      val templateID = col.column(cstTemplateId, cstTemplateId, type1.stringType()).setStyle(leftStyle).setWidth(5)
      val componant = col.column(cstComponent, cstComponent, type1.stringType()).setStyle(leftStyle).setWidth(10)
      val message = col.column(cstMessage, cstMessage, type1.stringType()).setStyle(leftStyle).setWidth(40)
      val description = col.column(cstDescription, cstDescription, type1.stringType()).setStyle(centeredStyle).setWidth(15)
      val gravity = col.column(cstGravity, cstGravity, type1.stringType()).setStyle(leftStyle).setWidth(10)

      val condition1 = stl.conditionalStyle(cnd.equal(gravity, cstError)).setBackgroundColor(Color.red);
      val condition2 = stl.conditionalStyle(cnd.equal(gravity, cstWarning)).setBackgroundColor(Color.orange);
      val plainStyle = stl.style().setFontName("FreeUniversal").setFontSize(6)
      val groupStyle = stl.style().setFontSize(12).bold()
      val grpGravity = grp.group(gravity).startInNewPage();
      grpGravity.setStyle(groupStyle)

      val titleStyle = stl.style(boldCenteredStyle).setVerticalAlignment(VerticalAlignment.MIDDLE).setFontSize(15);

      val rep1 =
        report()
          .setColumnTitleStyle(columnTitleStyle)
          .highlightDetailEvenRows()
          .columns(bmml, message, gravity, templateID, componant, description)
          // .title(cmp.text("Execution Report").setStyle(boldCenteredStyle))
          .pageFooter(cmp.pageXofY().setStyle(boldCenteredStyle))
          .detailRowHighlighters(condition1, condition2)
          //.setGroupTitleStyle(groupStyle)
          .groupBy(grpGravity)
          .setDataSource(createDataSource()) //set datasource
          .setPageFormat(1000, 400, PageOrientation.PORTRAIT)

      rep1.title( //shows report title
        cmp.horizontalList()
          .add(
            cmp.image(getClass().getResourceAsStream("./bouvier.png")).setFixedDimension(80, 64),
            cmp.text("Results of generation").setStyle(titleStyle).setHorizontalAlignment(HorizontalAlignment.CENTER))
          .newRow()
          .add(cmp.filler().setStyle(stl.style().setTopBorder(stl.pen2Point())).setFixedHeight(10)))

      // .show();//create and show report

      return rep1
    } catch {

      case e: Exception =>
        e.printStackTrace();
        return null
    }

  } // fin de createReport 

  def createDataSource(): JRDataSource = {

    val dataSource = new DRDataSource(cstBmml, cstTemplateId, cstComponent, cstMessage, cstDescription, cstGravity)
    // on trie la table sur la gravité 
    globalContext.gblTableTrace.distinct.sortWith((x, y) => (x._6 < y._6)).foreach {
      case (bmml, templateID, componant, mes1, description, gravity) => dataSource.add(bmml, templateID, componant, mes1, description, gravity);
    }

    return dataSource;

  }

}



 

