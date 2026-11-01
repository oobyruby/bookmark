package uws.ac.uk.bookmark;

public class Book {

    private String id;
    private String title;
    private String authors;
    private String description;
    private String coverUrl;

    public Book(String id, String title, String authors,
                String description, String coverUrl) {
        this.id = id;
        this.title = title;
        this.authors = authors;
        this.description = description;
        this.coverUrl = coverUrl;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthors() {
        return authors;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }
}
