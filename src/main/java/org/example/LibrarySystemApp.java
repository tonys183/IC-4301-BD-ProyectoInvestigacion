package org.example;

import com.db4o.*;
import com.db4o.query.Predicate;
import com.formdev.flatlaf.intellijthemes.*;
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
        try { UIManager.setLookAndFeel(new FlatMonokaiProIJTheme());
        } catch (Exception e) { e.printStackTrace(); }
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
                new String[]{"Agregar", "Listar", "Actualizar", "Eliminar", "Disponibilidad", "Buscar"},
                new ActionListener[]{
                        e -> showBookDialog("Agregar"),
                        e -> listEntities(Book.class),
                        e -> showBookDialog("Actualizar"),
                        e -> deleteEntity(Book.class, "ISBN"),
                        e -> toggleBookAvailability(),
                        e -> showBookSearch()
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
                new String[]{"Nuevo", "Historial", "Actualizar", "Devolver", "Eliminar"},
                new ActionListener[]{
                        e -> createNewLending(),
                        e -> showLendingHistory(),
                        e -> updateLending(),
                        e -> registerDevolution(),
                        e -> deleteLending(),
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

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, scrollPane);
        splitPane.setResizeWeight(0.75);
        splitPane.setDividerSize(8);

        frame.getContentPane().add(splitPane, BorderLayout.CENTER);
        frame.setVisible(true);
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
        int i = 0;
        for (T entity : entities) {
            outputArea.append(entityClass.getSimpleName() + ": " + ++i + "\n");
            outputArea.append(entity.toString() + "\n\n");
        }
        outputArea.append("Total: " + entities.size() + "\n");
    }

    private <T> void deleteEntity(Class<T> entityClass, String idFieldName) {
        String id = JOptionPane.showInputDialog(frame,
                "Ingrese " + idFieldName + " a eliminar:",
                "Eliminar " + entityClass.getSimpleName(), JOptionPane.QUESTION_MESSAGE);

        if (id != null && !id.isEmpty()) {
            try {
                if (entityClass.equals(User.class)) { deleteUserById(id); }
                else if (entityClass.equals(Book.class)) { deleteBookByIsbn(id); }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame,
                        "Error al eliminar: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteUserById(String id) {
        List<User> users = db.query(new Predicate<User>() {
            @Override public boolean match(User user) { return id.equals(user.getId()); }
        });

        if (users.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Usuario no encontrado", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = users.getFirst();
        ObjectSet<Lending> activeLendings = db.query(new Predicate<Lending>() {
            @Override public boolean match(Lending l) {
                return l.getUser().equals(user) && l.getDevolutionDate() == null;
            }
        });

        if (activeLendings.hasNext()) {
            StringBuilder message = new StringBuilder();
            message.append("No se puede eliminar el usuario porque tiene préstamos activos:\n");

            while (activeLendings.hasNext()) {
                Lending l = activeLendings.next();
                message.append("- Título: ").append(l.getBook().getTitle()).append(" (desde ").append(l.getLendingDate()).append(")\n");
            }
            JOptionPane.showMessageDialog(frame, message.toString(), "Préstamos activos", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(frame,
                "¿Eliminar usuario " + user.getWholeName() + "?\n" +
                        "Se eliminarán los registros de préstamos históricos asociados.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            Lending lendingExample = new Lending();
            lendingExample.setUser(user);
            ObjectSet<Lending> allLendings = db.queryByExample(lendingExample);

            while (allLendings.hasNext()) { db.delete(allLendings.next()); }
            db.delete(user);
            outputArea.setText("");
            outputArea.append("Usuario y sus préstamos históricos eliminados: " + user + "\n");
        }
    }

    private void deleteBookByIsbn(String isbn) {
        outputArea.setText("");
        List<Book> books = db.query(new Predicate<Book>() {
            @Override
            public boolean match(Book book) { return isbn.equals(book.getISBN()); }
        });

        if (books.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "No se encontró un libro con el ISBN: " + isbn,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Book bookToDelete = books.getFirst();
        Lending lendingExample = new Lending();
        lendingExample.setBook(bookToDelete);
        ObjectSet<Lending> bookLendings = db.queryByExample(lendingExample);

        boolean hasActiveLendings = false;
        StringBuilder activeLendingsInfo = new StringBuilder();

        while (bookLendings.hasNext()) {
            Lending lending = bookLendings.next();
            if (lending.getDevolutionDate() == null) {
                hasActiveLendings = true;
                activeLendingsInfo.append("- Préstamo a ")
                        .append(lending.getUser().getWholeName())
                        .append(" (desde ")
                        .append(lending.getLendingDate())
                        .append(")\n");
            }
        }

        if (hasActiveLendings) {
            JOptionPane.showMessageDialog(frame,
                    "No se puede eliminar el libro porque tiene préstamos activos:\n\n" +
                            activeLendingsInfo.toString() + "\n",
                    "Error al eliminar",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean hasHistoricalLendings = !bookLendings.isEmpty();
        String confirmationMessage = "¿Está seguro de eliminar este libro?\n - Libro: " + bookToDelete.getTitle() + " - Autor: " + bookToDelete.getAuthor();

        if (hasHistoricalLendings) {
            confirmationMessage += "\n\nCuidado: Este libro tiene " + bookLendings.size() + " préstamos históricos que también serán eliminados";
        }

        int confirm = JOptionPane.showConfirmDialog(frame,
                confirmationMessage,
                "Confirmar Eliminación",
                JOptionPane.YES_NO_OPTION,
                hasHistoricalLendings ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (hasHistoricalLendings) {
                bookLendings.reset();
                int deletedLendings = 0;
                while (bookLendings.hasNext()) {
                    db.delete(bookLendings.next());
                    deletedLendings++;
                }
                outputArea.setText("");
                outputArea.append("Se eliminaron " + deletedLendings + " préstamos históricos asociados\n");
            }

            db.delete(bookToDelete);
            outputArea.append("Libro eliminado: " + bookToDelete.getTitle() + " (ISBN: " + isbn + ")\n");
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
                public boolean match(Book b) { return b.getISBN().equals(isbn); }
            });

            if (books.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Libro no encontrado", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            book = books.getFirst();
        } else {
            book = new Book("", "", "", "", 0, true, null);
        }

        JPanel panel = new JPanel(new GridLayout(0, 2, 0, 5));
        JTextField isbnField = new JTextField(book.getISBN());
        JTextField titleField = new JTextField(book.getTitle());
        JTextField authorField = new JTextField(book.getAuthor());
        JTextField publisherField = new JTextField(book.getPublisher());
        JTextField yearField = new JTextField(book.getPublishedYear() > 0 ? String.valueOf(book.getPublishedYear()) : "");
        JComboBox<Category> categoryCombo = getCategoryJComboBox(book);
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
                outputArea.setText("");
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
                outputArea.setText("");
                outputArea.append("Libro " + isbn + " ahora está " +
                        (newStatus ? "disponible" : "no disponible") + "\n");
            } else {
                JOptionPane.showMessageDialog(frame,
                        "No se encontró un libro con ese ISBN",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showBookSearch() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

        String[] opciones = {"Título", "Autor"};
        JComboBox<String> criterioCombo = new JComboBox<>(opciones);
        JTextField searchField = new JTextField();

        panel.add(new JLabel("Buscar por:"));
        panel.add(criterioCombo);
        panel.add(new JLabel("Texto a buscar:"));
        panel.add(searchField);

        int result = JOptionPane.showConfirmDialog(frame, panel,
                "Buscar Libro", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String criterio = (String) criterioCombo.getSelectedItem();
            String texto = searchField.getText().toLowerCase();

            List<Book> books = db.query(new Predicate<Book>() {
                @Override
                public boolean match(Book book) {
                    if (criterio.equals("Título")) {
                        return book.getTitle().toLowerCase().contains(texto);
                    } else {
                        return book.getAuthor().toLowerCase().contains(texto);
                    }
                }
            });

            outputArea.setText("");
            outputArea.append("--- Resultados de búsqueda por [" + criterio + "] ---\n");
            if (books.isEmpty()) {
                outputArea.append("No se encontraron libros que coincidan\n");
            } else {
                books.forEach(book -> outputArea.append(book.toString() + "\n"));
            }
            outputArea.append("Total: " + books.size() + "\n");
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
            outputArea.setText("");
            outputArea.append("Usuario " + (action.equals("Agregar") ? "agregado" : "actualizado") + ": " + user + "\n");
        }
    }



    // ------- Métodos CRUD para Préstamos -------
    private void createNewLending() {
        List<User> users = db.query(User.class);
        JComboBox<User> userCombo = new JComboBox<>(users.toArray(new User[0]));
        userCombo.setRenderer(new UserRenderer());

        if (JOptionPane.showConfirmDialog(frame, userCombo, "Seleccione el usuario:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        User selectedUser = (User) userCombo.getSelectedItem();

        List<Book> availableBooks = db.query(new Predicate<Book>() {
            @Override public boolean match(Book book) { return book.isAvailable(); }
        });

        JComboBox<Book> bookCombo = new JComboBox<>(availableBooks.toArray(new Book[0]));
        bookCombo.setRenderer(new BookRenderer());

        if (JOptionPane.showConfirmDialog(frame, bookCombo, "Seleccione el libro a prestar:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        Book selectedBook = (Book) bookCombo.getSelectedItem();
        Lending lending = new Lending();
        lending.setUser(selectedUser);
        lending.setBook(selectedBook);
        lending.setLendingDate(new Date());

        selectedBook.setAvailable(false);
        db.store(selectedBook);
        db.store(lending);

        outputArea.setText("");
        outputArea.append("Nuevo préstamo registrado:\n" + lending + "\n");
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
            outputArea.setText("");
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
                    outputArea.setText("");
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
                outputArea.setText("");
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

        outputArea.append("--- Historial de Préstamos ---\n");
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
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));

        JTextField nameField = new JTextField();
        JTextArea descriptionArea = new JTextArea(3, 20);
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);

        panel.add(new JLabel("Nombre de la categoría:"));
        panel.add(nameField);
        panel.add(new JLabel("Descripción:"));
        panel.add(descriptionScroll);

        int result = JOptionPane.showConfirmDialog(frame, panel,
                "Crear Nueva Categoría",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION && !nameField.getText().trim().isEmpty()) {
            Category newCategory = new Category(nameField.getText().trim(), descriptionArea.getText().trim());

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
            outputArea.setText("");
            outputArea.append("Categoría creada: " + newCategory.toString());
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
            JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));

            JTextField nameField = new JTextField(selectedCategory.getName());
            JTextArea descriptionArea = new JTextArea(selectedCategory.getDescription(), 3, 20);
            JScrollPane descriptionScroll = new JScrollPane(descriptionArea);

            panel.add(new JLabel("Nombre:"));
            panel.add(nameField);
            panel.add(new JLabel("Descripción:"));
            panel.add(descriptionScroll);

            int result = JOptionPane.showConfirmDialog(frame, panel,
                    "Actualizar Categoría",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION && !nameField.getText().trim().isEmpty()) {
                String newName = nameField.getText().trim();
                String newDescription = descriptionArea.getText().trim();

                if (!db.query(new Predicate<Category>() {
                    @Override
                    public boolean match(Category cat) {
                        return cat.getName().equalsIgnoreCase(newName) && !cat.equals(selectedCategory);
                    }
                }).isEmpty()) {
                    JOptionPane.showMessageDialog(frame,
                            "Ya existe otra categoría con ese nombre",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                selectedCategory.setName(newName);
                selectedCategory.setDescription(newDescription);
                db.store(selectedCategory);
                outputArea.setText("");
                outputArea.append("Categoría actualizada: " + selectedCategory.toString());
            }
        }
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
                outputArea.setText("");
                outputArea.append("Categoría eliminada: " + selectedCategory.getName());
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
            outputArea.append("--- Libros en categoría: " + selectedCategory.getName() + " ---\n");
            outputArea.append("Total: " + books.size() + " libros\n\n");

            if (books.isEmpty()) {
                outputArea.append("No hay libros en esta categoría\n");
            } else {
                books.forEach(book -> outputArea.append(book.toString() + "\n"));
            }
        }
    }

    private JComboBox<Category> getCategoryJComboBox(Book book) {
        List<Category> categories = db.query(Category.class);
        JComboBox<Category> categoryCombo = new JComboBox<>(categories.toArray(new Category[0]));
        categoryCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Category) { setText(((Category) value).getName()); }
                return this;
            }
        });
        if (book != null && book.getCategory() != null) { categoryCombo.setSelectedItem(book.getCategory()); }
        if (book.getCategory() != null) categoryCombo.setSelectedItem(book.getCategory());
        return categoryCombo;
    }

    private static class UserRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof User) {
                User user = (User) value;
                setText("Usuario: " + user.getWholeName() + " (" + user.getEmail() + ")");
            }
            return this;
        }
    }

    private static class BookRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Book) {
                Book book = (Book) value;
                setText("Título: " + book.getTitle() + " - Autor: " + book.getAuthor());
            }
            return this;
        }
    }
}