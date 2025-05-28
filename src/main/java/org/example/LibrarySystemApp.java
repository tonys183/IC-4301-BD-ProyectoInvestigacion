package org.example;

import com.db4o.*;
import com.db4o.query.Predicate;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class LibrarySystemApp {
    private static final String DB_FILE = "library.db4o";
    private ObjectContainer db;
    private JFrame frame;
    private JTextArea outputArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LibrarySystemApp().initialize());
    }

    public void initialize() {
        db = Db4oEmbedded.openFile(Db4oEmbedded.newConfiguration(), DB_FILE);

        // ------- Swing -------
        frame = new JFrame("Sistema de Biblioteca DB4o");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel bookPanel = createEntityPanel("Libros",
                new String[]{"Agregar", "Listar", "Actualizar", "Eliminar", "Disponibilidad"},
                new ActionListener[]{
                        e -> showBookDialog("Agregar"),
                        e -> listEntities(Book.class),
                        e -> showBookDialog("Actualizar"),
                        e -> deleteEntity(Book.class, "ISBN"),
                        e -> toggleBookAvailability()
                });

        JPanel userPanel = createEntityPanel("Usuarios",
                new String[]{"Agregar", "Listar", "Actualizar", "Eliminar"},
                new ActionListener[]{
                        e -> showUserDialog("Agregar"),
                        e -> listEntities(User.class),
                        e -> showUserDialog("Actualizar"),
                        e -> deleteEntity(User.class, "id")
                });

        JPanel lendingPanel = createEntityPanel("Préstamos",
                new String[]{"Nuevo", "Listar", "Actualizar", "Devolver", "Eliminar", "Historial"},
                new ActionListener[]{
                        e -> createNewLending(),
                        e -> listEntities(Lending.class),
                        e -> updateLending(),
                        e -> registerDevolution(),
                        e -> deleteLending(),
                        e -> showLendingHistory()
                });

        JPanel categoryPanel = createEntityPanel("Categorías",
                new String[]{"Agregar", "Listar", "Actualizar", "Eliminar", "Ver Libros"},
                new ActionListener[]{
                        e -> createCategory(),
                        e -> listEntities(Category.class),
                        e -> updateCategory(),
                        e -> deleteCategory(),
                        e -> showBooksByCategory()
                });

        tabbedPane.addTab("Libros", bookPanel);
        tabbedPane.addTab("Categorías", categoryPanel);
        tabbedPane.addTab("Usuarios", userPanel);
        tabbedPane.addTab("Préstamos", lendingPanel);

        initializeDefaultCategories();

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.add(scrollPane, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void initializeDefaultCategories() {
        for (Category category : Category.values()) {
            if (db.query(new Predicate<Category>() {
                @Override
                public boolean match(Category cat) {
                    return cat.equals(category);
                }
            }).isEmpty()) {
                db.store(category);
            }
        }
    }

    private JPanel createEntityPanel(String title, String[] buttonLabels, ActionListener[] actions) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder(title));

        for (int i = 0; i < buttonLabels.length; i++) {
            JButton button = new JButton(buttonLabels[i]);
            button.addActionListener(actions[i]);
            panel.add(button);
        }

        return panel;
    }

    private <T> void listEntities(Class<T> entityClass) {
        outputArea.setText("");
        List<T> entities = db.query(entityClass);
        outputArea.append("--- Listado de " + entityClass.getSimpleName() + "s ---\n");
        for (T entity : entities) {
            outputArea.append(entity.toString() + "\n");
        }
        outputArea.append("Total: " + entities.size() + "\n");
    }

    private <T> void deleteEntity(Class<T> entityClass, String idFieldName) {
        String id = JOptionPane.showInputDialog(frame,
                "Ingrese " + idFieldName + " a eliminar:",
                "Eliminar " + entityClass.getSimpleName(), JOptionPane.QUESTION_MESSAGE);

        if (id != null && !id.isEmpty()) {
            try {
                if (entityClass.equals(User.class)) {
                    deleteUserById(id);
                } else if (entityClass.equals(Book.class)) {
                    deleteBookByIsbn(id);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame,
                        "Error al eliminar: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteUserById(String id) {
        List<User> users = db.query(new Predicate<User>() {
            @Override
            public boolean match(User user) {
                return id.equals(user.getId());
            }
        });

        if (!users.isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "¿Está seguro de eliminar este usuario?\n" + users.getFirst(),
                    "Confirmar Eliminación",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                db.delete(users.getFirst());
                outputArea.append("Usuario eliminado: " + id + "\n");
            }
        } else {
            JOptionPane.showMessageDialog(frame,
                    "No se encontró un usuario con el ID: " + id,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteBookByIsbn(String isbn) {
        List<Book> books = db.query(new Predicate<Book>() {
            @Override
            public boolean match(Book book) {
                return isbn.equals(book.getISBN());
            }
        });

        if (!books.isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "¿Está seguro de eliminar este libro?\n" + books.getFirst(),
                    "Confirmar Eliminación",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                db.delete(books.getFirst());
                outputArea.append("Libro eliminado: " + isbn + "\n");
            }
        } else {
            JOptionPane.showMessageDialog(frame,
                    "No se encontró un libro con el ISBN: " + isbn,
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showBookDialog(String action) {
        String isbn = action.equals("Actualizar") ?
                JOptionPane.showInputDialog(frame, "Ingrese ISBN del libro a actualizar:", action + " Libro", JOptionPane.QUESTION_MESSAGE) :
                null;

        Book book = null;
        if (action.equals("Actualizar")) {
            if (isbn == null || isbn.isEmpty()) return;

            List<Book> books = db.query(new Predicate<Book>() {
                @Override
                public boolean match(Book b) {
                    return b.getISBN().equals(isbn);
                }
            });

            if (books.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Libro no encontrado", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            book = books.getFirst();
        } else {
            book = new Book("", "", "", "", 0, true, null);
        }

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        JTextField isbnField = new JTextField(book.getISBN());
        JTextField titleField = new JTextField(book.getTitle());
        JTextField authorField = new JTextField(book.getAuthor());
        JTextField publisherField = new JTextField(book.getPublisher());
        JTextField yearField = new JTextField(book.getPublishedYear() > 0 ? String.valueOf(book.getPublishedYear()) : "");
        JComboBox<Category> categoryCombo = new JComboBox<>(Category.values());
        if (book.getCategory() != null) categoryCombo.setSelectedItem(book.getCategory());
        JCheckBox availableCheck = new JCheckBox("Disponible", book.isAvailable());

        if (action.equals("Actualizar")) isbnField.setEditable(false);

        panel.add(new JLabel("ISBN:"));
        panel.add(isbnField);
        panel.add(new JLabel("Título:"));
        panel.add(titleField);
        panel.add(new JLabel("Autor:"));
        panel.add(authorField);
        panel.add(new JLabel("Editorial:"));
        panel.add(publisherField);
        panel.add(new JLabel("Año Publicación:"));
        panel.add(yearField);
        panel.add(new JLabel("Categoría:"));
        panel.add(categoryCombo);
        panel.add(new JLabel("Disponibilidad:"));
        panel.add(availableCheck);

        int result = JOptionPane.showConfirmDialog(frame, panel,
                action + " Libro", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                book.setISBN(isbnField.getText());
                book.setTitle(titleField.getText());
                book.setAuthor(authorField.getText());
                book.setPublisher(publisherField.getText());
                book.setPublishedYear(Integer.parseInt(yearField.getText()));
                book.setCategory((Category) categoryCombo.getSelectedItem());
                book.setAvailable(availableCheck.isSelected());

                db.store(book);
                outputArea.append("Libro " + (action.equals("Agregar") ? "agregado" : "actualizado") + ": " + book + "\n");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "Año debe ser un número válido", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void toggleBookAvailability() {
        String isbn = JOptionPane.showInputDialog(frame,
                "Ingrese ISBN del libro para cambiar disponibilidad:",
                "Cambiar Disponibilidad", JOptionPane.QUESTION_MESSAGE);

        if (isbn != null && !isbn.isEmpty()) {
            List<Book> books = db.query(new Predicate<Book>() {
                @Override
                public boolean match(Book book) {
                    return book.getISBN().equals(isbn);
                }
            });

            if (!books.isEmpty()) {
                Book book = books.getFirst();
                boolean newStatus = !book.isAvailable();
                book.setAvailable(newStatus);
                db.store(book);
                outputArea.append("Libro " + isbn + " ahora está " +
                        (newStatus ? "disponible" : "no disponible") + "\n");
            } else {
                JOptionPane.showMessageDialog(frame,
                        "No se encontró un libro con ese ISBN",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showUserDialog(String action) {
        String id = action.equals("Actualizar") ?
                JOptionPane.showInputDialog(frame, "Ingrese ID del usuario a actualizar:", action + " Usuario", JOptionPane.QUESTION_MESSAGE) :
                null;

        User user = null;
        if (action.equals("Actualizar")) {
            if (id == null || id.isEmpty()) return;

            List<User> users = db.query(new Predicate<User>() {
                @Override
                public boolean match(User u) {
                    return u.getId().equals(id);
                }
            });

            if (users.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Usuario no encontrado", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            user = users.getFirst();
        } else {
            user = new User("", "");
        }

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

        JTextField idField = new JTextField(user.getId());
        JTextField nameField = new JTextField(user.getWholeName());
        JTextField emailField = new JTextField(user.getEmail() != null ? user.getEmail() : "");
        JTextField phoneField = new JTextField(user.getTelephoneNumber() != null ? user.getTelephoneNumber() : "");

        if (action.equals("Actualizar")) idField.setEditable(false);

        panel.add(new JLabel("ID:"));
        panel.add(idField);
        panel.add(new JLabel("Nombre Completo:"));
        panel.add(nameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Teléfono:"));
        panel.add(phoneField);

        int result = JOptionPane.showConfirmDialog(frame, panel,
                action + " Usuario", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            user.setId(idField.getText());
            user.setWholeName(nameField.getText());
            user.setEmail(emailField.getText());
            user.setTelephoneNumber(phoneField.getText());

            db.store(user);
            outputArea.append("Usuario " + (action.equals("Agregar") ? "agregado" : "actualizado") + ": " + user + "\n");
        }
    }



    // ------- Métodos CRUD para Préstamos -------
    private void createNewLending() {
        List<User> users = db.query(User.class);
        User selectedUser = (User) JOptionPane.showInputDialog(frame,
                "Seleccione el usuario:",
                "Nuevo Préstamo - Paso 1/2",
                JOptionPane.QUESTION_MESSAGE,
                null,
                users.toArray(),
                null);

        if (selectedUser == null) return;

        List<Book> availableBooks = db.query(new Predicate<Book>() {
            @Override
            public boolean match(Book book) {
                return book.isAvailable();
            }
        });

        Book selectedBook = (Book) JOptionPane.showInputDialog(frame,
                "Seleccione el libro a prestar:",
                "Nuevo Préstamo - Paso 2/2",
                JOptionPane.QUESTION_MESSAGE,
                null,
                availableBooks.toArray(),
                null);

        if (selectedBook != null) {
            Lending lending = new Lending();
            lending.setUser(selectedUser);
            lending.setBook(selectedBook);
            lending.setLendingDate(new Date());

            selectedBook.setAvailable(false);
            db.store(selectedBook);

            db.store(lending);
            outputArea.append("Nuevo préstamo registrado:\n" + lending + "\n");
        }
    }

    private void registerDevolution() {
        List<Lending> activeLendings = db.query(new Predicate<Lending>() {
            @Override
            public boolean match(Lending lending) {
                return lending.getDevolutionDate() == null;
            }
        });

        if (activeLendings.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No hay préstamos activos",
                    "Devolución",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Lending selectedLending = (Lending) JOptionPane.showInputDialog(frame,
                "Seleccione el préstamo a devolver:",
                "Registrar Devolución",
                JOptionPane.QUESTION_MESSAGE,
                null,
                activeLendings.toArray(),
                null);

        if (selectedLending != null) {
            selectedLending.setDevolutionDate(new Date());

            Book book = selectedLending.getBook();
            book.setAvailable(true);
            db.store(book);

            db.store(selectedLending);
            outputArea.append("Devolución registrada:\n" + selectedLending + "\n");
        }
    }

    private void updateLending() {
        List<Lending> allLendings = db.query(Lending.class);

        if (allLendings.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No hay préstamos registrados",
                    "Actualizar Préstamo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Lending selectedLending = (Lending) JOptionPane.showInputDialog(frame,
                "Seleccione el préstamo a actualizar:",
                "Actualizar Préstamo",
                JOptionPane.QUESTION_MESSAGE,
                null,
                allLendings.toArray(),
                null);

        if (selectedLending != null) {
            JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

            JLabel currentInfo = new JLabel("<html><b>Préstamo seleccionado:</b><br>" +
                    selectedLending.toString().replace("\n", "<br>") + "</html>");

            List<User> allUsers = db.query(User.class);
            JComboBox<User> userCombo = new JComboBox<>(allUsers.toArray(new User[0]));
            userCombo.setSelectedItem(selectedLending.getUser());

            List<Book> allBooks = db.query(Book.class);
            JComboBox<Book> bookCombo = new JComboBox<>(allBooks.toArray(new Book[0]));
            bookCombo.setSelectedItem(selectedLending.getBook());

            JTextField lendingDateField = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(selectedLending.getLendingDate()));

            JTextField devolutionDateField = new JTextField(
                    selectedLending.getDevolutionDate() != null ?
                            new SimpleDateFormat("yyyy-MM-dd").format(selectedLending.getDevolutionDate()) : "");

            panel.add(currentInfo);
            panel.add(new JLabel());
            panel.add(new JLabel("Usuario:"));
            panel.add(userCombo);
            panel.add(new JLabel("Libro:"));
            panel.add(bookCombo);
            panel.add(new JLabel("Fecha préstamo (yyyy-MM-dd):"));
            panel.add(lendingDateField);
            panel.add(new JLabel("Fecha devolución (yyyy-MM-dd):"));
            panel.add(devolutionDateField);

            int result = JOptionPane.showConfirmDialog(frame, panel,
                    "Actualizar Préstamo",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                try {
                    User newUser = (User) userCombo.getSelectedItem();
                    Book newBook = (Book) bookCombo.getSelectedItem();

                    if (!selectedLending.getBook().equals(newBook)) {
                        selectedLending.getBook().setAvailable(true);
                        db.store(selectedLending.getBook());

                        newBook.setAvailable(false);
                        db.store(newBook);
                    }

                    selectedLending.setUser(newUser);
                    selectedLending.setBook(newBook);
                    selectedLending.setLendingDate(new SimpleDateFormat("yyyy-MM-dd").parse(lendingDateField.getText()));

                    if (!devolutionDateField.getText().isEmpty()) {
                        selectedLending.setDevolutionDate(new SimpleDateFormat("yyyy-MM-dd").parse(devolutionDateField.getText()));
                    } else {
                        selectedLending.setDevolutionDate(null);
                    }

                    db.store(selectedLending);
                    outputArea.append("Préstamo actualizado:\n" + selectedLending + "\n");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(frame,
                            "Error en formato de fecha: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void deleteLending() {
        List<Lending> allLendings = db.query(Lending.class);

        if (allLendings.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No hay préstamos registrados",
                    "Eliminar Préstamo",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Lending selectedLending = (Lending) JOptionPane.showInputDialog(frame,
                "Seleccione el préstamo a eliminar:",
                "Eliminar Préstamo",
                JOptionPane.QUESTION_MESSAGE,
                null,
                allLendings.toArray(),
                null);

        if (selectedLending != null) {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "¿Está seguro de eliminar este préstamo?\n" + selectedLending,
                    "Confirmar Eliminación",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                if (selectedLending.getDevolutionDate() == null) {
                    Book book = selectedLending.getBook();
                    book.setAvailable(true);
                    db.store(book);
                }

                db.delete(selectedLending);
                outputArea.append("Préstamo eliminado:\n" + selectedLending + "\n");
            }
        }
    }

    private void showLendingHistory() {
        outputArea.setText("");
        List<Lending> allLendings = new ArrayList<>(db.query(Lending.class));

        long activeCount = allLendings.stream()
                .filter(l -> l.getDevolutionDate() == null)
                .count();

        outputArea.append("=== HISTORIAL DE PRÉSTAMOS ===\n");
        outputArea.append("Total: " + allLendings.size() + " préstamos\n");
        outputArea.append("Activos: " + activeCount + "\n");
        outputArea.append("Devueltos: " + (allLendings.size() - activeCount) + "\n\n");

        allLendings.sort((l1, l2) -> l2.getLendingDate().compareTo(l1.getLendingDate()));

        for (Lending lending : allLendings) {
            outputArea.append(lending.toString() + "\n");
        }
    }



    // ------- Métodos CRUD para Categorías -------
    private void createCategory() {
        JTextField nameField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Nombre de la categoría:"));
        panel.add(nameField);

        int result = JOptionPane.showConfirmDialog(frame, panel,
                "Crear Nueva Categoría",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION && !nameField.getText().trim().isEmpty()) {
            Category newCategory = new Category(nameField.getText().trim());

            if (!db.query(new Predicate<Category>() {
                @Override
                public boolean match(Category cat) {
                    return cat.equals(newCategory);
                }
            }).isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        "Ya existe una categoría con ese nombre",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            db.store(newCategory);
            outputArea.append("Categoría creada: " + newCategory + "\n");
        }
    }

    private void updateCategory() {
        List<Category> categories = db.query(Category.class);

        if (categories.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No hay categorías registradas",
                    "Actualizar Categoría",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Category selectedCategory = (Category) JOptionPane.showInputDialog(frame,
                "Seleccione la categoría a actualizar:",
                "Actualizar Categoría",
                JOptionPane.QUESTION_MESSAGE,
                null,
                categories.toArray(),
                null);

        if (selectedCategory != null) {
            if (isDefaultCategory(selectedCategory)) {
                JOptionPane.showMessageDialog(frame,
                        "No se pueden modificar las categorías predefinidas",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            JTextField nameField = new JTextField(selectedCategory.getName());

            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(new JLabel("Nuevo nombre:"));
            panel.add(nameField);

            int result = JOptionPane.showConfirmDialog(frame, panel,
                    "Actualizar Categoría",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION && !nameField.getText().trim().isEmpty()) {
                String newName = nameField.getText().trim();

                if (!db.query(new Predicate<Category>() {
                    @Override
                    public boolean match(Category cat) {
                        return cat.getName().equalsIgnoreCase(newName) &&
                                !cat.equals(selectedCategory);
                    }
                }).isEmpty()) {
                    JOptionPane.showMessageDialog(frame,
                            "Ya existe otra categoría con ese nombre",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                selectedCategory.setName(newName);
                db.store(selectedCategory);
                outputArea.append("Categoría actualizada: " + selectedCategory + "\n");
            }
        }
    }

    private boolean isDefaultCategory(Category category) {
        for (Category defaultCat : Category.values()) {
            if (defaultCat.equals(category)) {
                return true;
            }
        }
        return false;
    }

    private void deleteCategory() {
        List<Category> categories = db.query(Category.class);

        if (categories.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No hay categorías registradas",
                    "Eliminar Categoría",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Category selectedCategory = (Category) JOptionPane.showInputDialog(frame,
                "Seleccione la categoría a eliminar:",
                "Eliminar Categoría",
                JOptionPane.QUESTION_MESSAGE,
                null,
                categories.toArray(),
                null);

        if (selectedCategory != null) {
            if (isDefaultCategory(selectedCategory)) {
                JOptionPane.showMessageDialog(frame,
                        "No se pueden eliminar las categorías predefinidas",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            long bookCount = db.query(new Predicate<Book>() {
                @Override
                public boolean match(Book book) {
                    return selectedCategory.equals(book.getCategory());
                }
            }).size();

            if (bookCount > 0) {
                JOptionPane.showMessageDialog(frame,
                        "No se puede eliminar: Hay " + bookCount + " libros asociados a esta categoría",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(frame,
                    "¿Está seguro de eliminar la categoría?\n" + selectedCategory,
                    "Confirmar Eliminación",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                db.delete(selectedCategory);
                outputArea.append("Categoría eliminada: " + selectedCategory + "\n");
            }
        }
    }

    private void showBooksByCategory() {
        List<Category> categories = db.query(Category.class);

        if (categories.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No hay categorías registradas",
                    "Libros por Categoría",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Category selectedCategory = (Category) JOptionPane.showInputDialog(frame,
                "Seleccione la categoría:",
                "Libros por Categoría",
                JOptionPane.QUESTION_MESSAGE,
                null,
                categories.toArray(),
                null);

        if (selectedCategory != null) {
            List<Book> books = db.query(new Predicate<Book>() {
                @Override
                public boolean match(Book book) {
                    return selectedCategory.equals(book.getCategory());
                }
            });

            outputArea.setText("");
            outputArea.append("=== LIBROS EN CATEGORÍA: " + selectedCategory + " ===\n");
            outputArea.append("Total: " + books.size() + " libros\n\n");

            if (books.isEmpty()) {
                outputArea.append("No hay libros en esta categoría\n");
            } else {
                books.forEach(book -> outputArea.append(book.toString() + "\n"));
            }
        }
    }

    private void exitApplication() {
        db.close();
        frame.dispose();
        System.exit(0);
    }
}