package uws.ac.uk.bookmark;

public class CurrentBook {

    //store the book information
    private String title;
    private String author;
    private String coverUrl;
    private int progress;
    private String documentId;

    public CurrentBook( String documentId, String title, String author, String coverUrl, int progress){
        this.documentId = documentId;
        this.title = title;
        this.author = author;
        this.coverUrl = coverUrl;
        this.progress = progress;

    }
    public String getDocumentId(){return documentId;}
    public String getTitle(){
        return title;
    }
    public String getAuthor(){
        return author;
    }
    public String getCoverUrl(){
        return coverUrl;
    }
    public int getProgress(){
        return progress;
    }

    public void setProgress(int progress){
        this.progress = progress;
    }
}
