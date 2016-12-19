package es.uva.mangostas.sharedplaylist.Model;

/**
 * Created by root on 18/11/16.
 */

public abstract class ShpMediaObject {

    private String name;
    private String author;

    public void setName(String name) {
        this.name = name;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }
}
