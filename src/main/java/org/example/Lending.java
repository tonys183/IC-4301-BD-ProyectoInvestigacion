package org.example;
import java.util.*;

public class Lending {
    private Book book;
    private User user;
    private Date lendingDate;
    private Date devolutionDate;
    public Lending() {}

    public Book getBook() {
        return book;
    }
    public User getUser() { return user; }
    public void setUser(User user) {this.user = user;}
    public Date getLendingDate() { return lendingDate; }
    public Date getDevolutionDate() { return devolutionDate; }

    public void setBook(Book book) { this.book = book; }
    public void setLendingDate(Date lendingDate) { this.lendingDate = lendingDate; }
    public void setDevolutionDate(Date devolutionDate) { this.devolutionDate = devolutionDate; }

    @Override
    public String toString() {
        return "Libro: " + book.getTitle() + ", prestado a (" + user.getWholeName() +
                ") el " + lendingDate +
                (devolutionDate != null ? " | Devuelto el " + devolutionDate : " | Aún no devuelto ");
    }
}
