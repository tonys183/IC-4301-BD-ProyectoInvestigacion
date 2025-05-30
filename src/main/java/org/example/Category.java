package org.example;

import com.db4o.config.annotations.Indexed;

public class Category {
    @Indexed
    private String name;
    private String description;

    public static final Category FICTION = new Category("Ficción", "Obras literarias imaginarias como novelas, cuentos o relatos.");
    public static final Category SCIENCE = new Category("Ciencia", "Libros sobre física, biología, química y otras disciplinas científicas.");
    public static final Category HISTORY = new Category("Historia", "Material relacionado con eventos históricos, biografías o civilizaciones.");
    public static final Category TECHNOLOGY = new Category("Tecnología", "Temas sobre informática, electrónica, ingeniería y avances técnicos.");
    public static final Category ART = new Category("Arte", "Obras o estudios sobre pintura, música, cine, escultura y otras expresiones artísticas.");
    public static final Category OTHER = new Category("Otro", "Categoría general para libros que no encajan en las anteriores.");

    public Category() { }
    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() { return "Nombre: " + name + " - Descripción: " + description; }

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