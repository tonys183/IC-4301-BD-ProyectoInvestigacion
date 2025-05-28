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
        return "📘 " + book.getTitle() +
                " Lend to " + user.getWholeName() +
                " the " + lendingDate +
                (devolutionDate != null ? " | Returned the " + devolutionDate : " | Not returned yet");
    }
}
