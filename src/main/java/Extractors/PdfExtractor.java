package Extractors;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfExtractor {
    private File pdfFile;
    private PDDocument document;
    private PDPageTree allPages;
    private boolean isSucceed;

    private String folderPath;

    private List<PDFBookMarks> bookMarks;

    public PdfExtractor(File pdfFile){
        this.pdfFile = pdfFile;
        this.bookMarks = new ArrayList<PDFBookMarks>();
        try{
            this.initialise();
        }catch (Exception e){
            this.isSucceed = false;
        }
    }

    private void initialise() throws IOException {
        this.document = PDDocument.load(this.pdfFile);
        String folderPath = this.pdfFile.getName().replaceAll(".pdf","").replaceAll(" ", "-");
        this.folderPath = String.format("%s%s", folderPath, System.getProperty("file.separator"));
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.allPages = document.getDocumentCatalog().getPages();
        PDDocumentOutline documentOutlines = this.document.getDocumentCatalog().getDocumentOutline();
        PDOutlineItem firstChild = documentOutlines.getFirstChild();
        if (firstChild != null){
            this.setBookmarks(firstChild, 1);
        }
    }

    private void setBookmarks(PDOutlineItem current, int currentLevel) throws IOException{
        while(current != null){
            int pageNum = 0;
            if (current.getDestination() instanceof PDPageDestination){
                PDPageDestination dest = (PDPageDestination) current.getDestination();
                pageNum = dest.retrievePageNumber();
            }else if(current.getDestination() instanceof PDNamedDestination){
                PDPageDestination dest = this.document.getDocumentCatalog().findNamedDestinationPage((PDNamedDestination) current.getDestination());
                if (dest != null){
                    pageNum = dest.retrievePageNumber();
                }
            }

            if (current.getAction() instanceof PDActionGoTo){
                PDActionGoTo gta = (PDActionGoTo) current.getAction();
                if (gta.getDestination() instanceof PDPageDestination){
                    PDPageDestination dest = (PDPageDestination) gta.getDestination();
                    pageNum = dest.retrievePageNumber();
                }else if (gta.getDestination() instanceof PDNamedDestination){
                    PDPageDestination dest = this.document.getDocumentCatalog().findNamedDestinationPage((PDNamedDestination) gta.getDestination());
                    if (dest != null){
                        pageNum = dest.retrievePageNumber();
                    }
                }
            }

            String title = current.getTitle();
            this.bookMarks.add(new PDFBookMarks(title, pageNum, currentLevel));
            PDOutlineItem firstChildItem = current.getFirstChild();
            if (firstChildItem != null){
                this.setBookmarks(firstChildItem, currentLevel + 1);
            }
            current = current.getNextSibling();
        }
    }


    public boolean isSucceed(){
        return this.isSucceed;
    }

    public void extract() throws IOException{
        File mdFile = new File(String.format("%s%s", this.folderPath, this.pdfFile.getName().replaceAll(".pdf",".md").replaceAll(" ", "-")));
        FileOutputStream outputStream = new FileOutputStream(mdFile);
        StringBuilder sb = new StringBuilder();
        int bookmarkIndex = 0;
        PDFBookMarks currentBookmark = this.bookMarks.size() == 0? null : this.bookMarks.get(bookmarkIndex);

        for (PDPage page : this.allPages){
            List<PDAnnotation> annotations = page.getAnnotations();
            int pageNumber = allPages.indexOf(page);

            sb.setLength(0);

            while ((currentBookmark != null) && (currentBookmark.getPageNum() <= pageNumber)){
                if (currentBookmark.getLevel() == 1){
                    if (sb.length() > 0){
                        outputStream.write(sb.toString().getBytes());
                    }
                    outputStream.close();
                    sb.setLength(0);
                    String fileTitle = currentBookmark.getTitle().replaceAll("[^a-zA-Z\\d.]", "-");
                    mdFile = new File(String.format("%s%s.md", this.folderPath,fileTitle));
                    outputStream = new FileOutputStream(mdFile);
                }
                String titleBuilder = "#".repeat(currentBookmark.getLevel());
                sb.append(String.format("%s %s  \n\n", titleBuilder, currentBookmark.getTitle()));
                bookmarkIndex++;
                currentBookmark = (bookmarkIndex >= bookMarks.size()) ? null : bookMarks.get(bookmarkIndex);
            }

            for (PDAnnotation annotation : annotations) {
                // check subType
                if (annotation.getSubtype().equals("Highlight")) {
                    // extract highlighted text
                    PDFTextStripperByArea stripperByArea = new PDFTextStripperByArea();

                    COSArray quadsArray = annotation.getCOSObject().getCOSArray(COSName.getPDFName("QuadPoints"));

                    for (int j = 1, k = 0; j <= (quadsArray.size() / 8); j++) {

                        COSNumber ULX = (COSNumber) quadsArray.get(k);
                        COSNumber ULY = (COSNumber) quadsArray.get(1 + k);
                        COSNumber URX = (COSNumber) quadsArray.get(2 + k);
                        COSNumber URY = (COSNumber) quadsArray.get(3 + k);
                        COSNumber LLX = (COSNumber) quadsArray.get(4 + k);
                        COSNumber LLY = (COSNumber) quadsArray.get(5 + k);
                        COSNumber LRX = (COSNumber) quadsArray.get(6 + k);
                        COSNumber LRY = (COSNumber) quadsArray.get(7 + k);

                        k += 8;

                        float ulx = ULX.floatValue() - 1;                           // upper left x.
                        float uly = ULY.floatValue();                               // upper left y.
                        float width = URX.floatValue() - LLX.floatValue();          // calculated by upperRightX - lowerLeftX.
                        float height = URY.floatValue() - LLY.floatValue();         // calculated by upperRightY - lowerLeftY.

                        PDRectangle pageSize = page.getMediaBox();
                        uly = pageSize.getHeight() - uly;

                        Rectangle2D.Float rectangle_2 = new Rectangle2D.Float(ulx, uly, width, height);
                        stripperByArea.addRegion("highlightedRegion", rectangle_2);
                        stripperByArea.extractRegions(page);
                        String highlightedText = stripperByArea.getTextForRegion("highlightedRegion").replace("\r\n", " ");

                        sb.append(highlightedText);
                    }

                }

                sb.append("  \n");

            }
            if (sb.length() > 0){
                outputStream.write(sb.toString().getBytes());
            }
        }
    }


}
