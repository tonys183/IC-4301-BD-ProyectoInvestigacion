package org.example;

public class Book {
    private String ISBN;
    private String title;
    private String author;
    private String publisher;
    private int publishedYear;
    private boolean available = true;
    private Category category;

    public Book(String ISBN, String title, String author, String publisher, int publishedYear, boolean available, Category category) {
        this.ISBN = ISBN;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.publishedYear = publishedYear;
        this.available = available;
        this.category = category;
    }

    public String getISBN() { return ISBN; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getPublisher() { return publisher; }
    public int getPublishedYear() { return publishedYear;}
    public boolean isAvailable() { return available;}
    public Category getCategory() { return category; }

    public void setISBN(String ISBN) { this.ISBN = ISBN; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String autor) { this.author = autor; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public void setPublishedYear(int publishedYear) { this.publishedYear = publishedYear; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setCategory(Category category) { this.category = category; }

    @Override
    public String toString() {
        return "ISBN: [" + ISBN + "] - Título: " + title + " - Autor: " + author + (available ? " (Disponible)" : " (Prestado)");
    }
}
