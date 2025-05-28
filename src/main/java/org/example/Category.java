package org.example;

import com.db4o.config.annotations.Indexed;

public class Category {
    @Indexed
    private String name;

    public static final Category FICTION = new Category("Ficción");
    public static final Category SCIENCE = new Category("Ciencia");
    public static final Category HISTORY = new Category("Historia");
    public static final Category TECHNOLOGY = new Category("Tecnología");
    public static final Category ART = new Category("Arte");
    public static final Category OTHER = new Category("Otro");

    public Category() { }

    public Category(String name) { this.name = name;}

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    @Override
    public String toString() { return name; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Category category = (Category) obj;
        return name.equalsIgnoreCase(category.name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }

    public static Category[] values() {
        return new Category[]{FICTION, SCIENCE, HISTORY, TECHNOLOGY, ART, OTHER};
    }
}