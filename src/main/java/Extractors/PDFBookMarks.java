package Extractors;

public class PDFBookMarks {
    private final int level;
    private final int pageNum;
    private final String title;


    public PDFBookMarks(String title, int pageNum, int level){
        this.title = title;
        this.level = level;
        this.pageNum = pageNum;
    }

    public String getTitle(){
        return this.title;
    }

    public int getLevel(){
        return this.level;
    }

    public int getPageNum(){
        return this.pageNum;
    }

}
