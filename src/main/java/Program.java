import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class Program {

    public static void main(String[] args) {
        Path inputsPath = FileSystems.getDefault().getPath("inputs");
        try{
            List<File> files = Files.list(inputsPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".pdf"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
            for (File file:files
                 ) {
               RunExtractor(file);
            }
        }catch (IOException e){
            System.out.println(e.toString());
        }
    }

    public static void RunExtractor(File pdfFile) throws IOException {
        PDDocument document = PDDocument.load(pdfFile);
        PDPageTree allPages = document.getDocumentCatalog().getPages();
        FileOutputStream outputStream = new FileOutputStream(pdfFile.getName().replaceAll(".pdf",".md"));
        for (PDPage page: allPages
             ) {
            List<PDAnnotation> annotations = page.getAnnotations();
            int pageNumber = allPages.indexOf(page);
            for(int i=0; i<annotations.size(); i++) {
                // check subType
                if(annotations.get(i).getSubtype().equals("Highlight")) {
                    // extract highlighted text
                    PDFTextStripperByArea stripperByArea = new PDFTextStripperByArea();

                    COSArray quadsArray = (COSArray) annotations.get(i).getCOSObject().getCOSArray(COSName.getPDFName("QuadPoints"));
                    String str = null;

                    for(int j=1, k=0; j<=(quadsArray.size()/8); j++) {

                        COSNumber ULX = (COSNumber) quadsArray.get(0+k);
                        COSNumber ULY = (COSNumber) quadsArray.get(1+k);
                        COSNumber URX = (COSNumber) quadsArray.get(2+k);
                        COSNumber URY = (COSNumber) quadsArray.get(3+k);
                        COSNumber LLX = (COSNumber) quadsArray.get(4+k);
                        COSNumber LLY = (COSNumber) quadsArray.get(5+k);
                        COSNumber LRX = (COSNumber) quadsArray.get(6+k);
                        COSNumber LRY = (COSNumber) quadsArray.get(7+k);

                        k+=8;

                        float ulx = ULX.floatValue() - 1;                           // upper left x.
                        float uly = ULY.floatValue();                               // upper left y.
                        float width = URX.floatValue() - LLX.floatValue();          // calculated by upperRightX - lowerLeftX.
                        float height = URY.floatValue() - LLY.floatValue();         // calculated by upperRightY - lowerLeftY.

                        PDRectangle pageSize = page.getMediaBox();
                        uly = pageSize.getHeight() - uly;

                        Rectangle2D.Float rectangle_2 = new Rectangle2D.Float(ulx, uly, width, height);
                        stripperByArea.addRegion("highlightedRegion", rectangle_2);
                        stripperByArea.extractRegions(page);
                        String highlightedText = stripperByArea.getTextForRegion("highlightedRegion");

                        if(j > 1) {
                            str = str.concat(highlightedText);
                        } else {
                            str = highlightedText;
                        }
                    }

                    str += "("+pageNumber+")  \n";
                    outputStream.write(str.getBytes());
                }
            }
        }
        outputStream.close();
        document.close();

    }
}
