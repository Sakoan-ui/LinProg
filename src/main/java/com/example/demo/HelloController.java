package com.example.demo;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import javafx.util.converter.IntegerStringConverter;

import java.io.*;
import java.net.URL;
import java.util.*;

public class HelloController implements Initializable {

    public static boolean calculationStopped = false;
    public static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private ComboBox<String> minMax, fraction;
    @FXML
    private Spinner<Integer> countVariables, countRestrictions;
    @FXML
    private TableView<ObservableList<String>> restrictions, target, tableSimplex, tableBasis;
    @FXML
    private Label answerSimplexText, answerBasisText;
    @FXML
    private Button nextSimplex, answerSimplex, backSimplex, nextBasis, answerBasis, backBasis;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        minMax.setItems(FXCollections.observableArrayList("Минимизировать", "Максимизировать"));
        fraction.setItems(FXCollections.observableArrayList("Обыкновенные", "Десятичные"));
        countVariables.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 16, 2));
        TextFormatter<Integer> formatterCountVariables = new TextFormatter<>(new IntegerStringConverter(), 2, change -> {
                    String newText = change.getControlNewText();

                    // Разрешаем пустую строку (для стирания перед новым вводом)
                    if (newText.isEmpty()) {
                        return change;
                    }

                    // Если не цифры — отклоняем
                    if (!newText.matches("\\d+")) {
                        return null;
                    }

                    // Парсим и проверяем диапазон
                    try {
                        int value = Integer.parseInt(newText);
                        if (value > 16) {
                            // Заменяем весь ввод на "16"
                            change.setRange(0, change.getControlText().length());
                            change.setText("16");
                            // Устанавливаем курсор в конец
                            change.selectRange(2, 2);
                        }
                        return change;
                    } catch (NumberFormatException e) {
                        // Очень большое число — принудительно ставим 16
                        change.setRange(0, change.getControlText().length());
                        change.setText("16");
                        change.selectRange(2, 2);
                        return change;
                    }
                }
        );
        // связываем форматер и спинер
        countVariables.getEditor().setTextFormatter(formatterCountVariables);
        ((SpinnerValueFactory.IntegerSpinnerValueFactory) countVariables.getValueFactory())
                .valueProperty()
                .bindBidirectional(formatterCountVariables.valueProperty());

        //то же самое для ограничений
        countRestrictions.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 16, 2));
        TextFormatter<Integer> formatterCountRestrictions = new TextFormatter<>(new IntegerStringConverter(), 2, change -> {
            String newText = change.getControlNewText();

            // Разрешаем пустую строку (для стирания перед новым вводом)
            if (newText.isEmpty()) {
                return change;
            }

            // Если не цифры — отклоняем
            if (!newText.matches("\\d+")) {
                return null;
            }

            // Парсим и проверяем диапазон
            try {
                int value = Integer.parseInt(newText);
                if (value > 16) {
                    // Заменяем весь ввод на "16"
                    change.setRange(0, change.getControlText().length());
                    change.setText("16");
                    // Устанавливаем курсор в конец
                    change.selectRange(2, 2);
                }
                return change;
            } catch (NumberFormatException e) {
                // Очень большое число — принудительно ставим 16
                change.setRange(0, change.getControlText().length());
                change.setText("16");
                change.selectRange(2, 2);
                return change;
            }
        }
        );
        countRestrictions.getEditor().setTextFormatter(formatterCountRestrictions);
        ((SpinnerValueFactory.IntegerSpinnerValueFactory) countRestrictions.getValueFactory())
                .valueProperty()
                .bindBidirectional(formatterCountRestrictions.valueProperty());

        //слушатели(автоматически меняют таблицы при изменении countVariables countRestrictions)
        countVariables.valueProperty().addListener((observable, oldValue, newValue) -> {createRestrictionsTable(); createTargetTable();});
        countRestrictions.valueProperty().addListener((observable, oldValue, newValue) -> createRestrictionsTable());
        createRestrictionsTable();

        createTargetTable();
    }


    private void createRestrictionsTable() {

        int cols = countVariables.getValue();
        int rows = countRestrictions.getValue();

        //стирание старой таблицы
        restrictions.getColumns().clear();
        restrictions.getItems().clear();

        //столбец fx
        TableColumn<ObservableList<String>, String> fxColumn = new TableColumn<>();
        fxColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(0)));

        //обработчик редактирования
        fxColumn.setOnEditCommit(event -> {
            ObservableList<String> row = event.getRowValue();
            row.set(0, event.getNewValue());
        });

        restrictions.getColumns().add(fxColumn);//добавление столбца

        //заполнение столбцов
        for (int row = 0; row < rows; row++) {
            ObservableList<String> rowData = FXCollections.observableArrayList();
            rowData.add("f" + (row + 1) + "(x)");
            for (int col = 0; col < cols+1; col++) {
                rowData.add("");
            }
            restrictions.getItems().add(rowData);
        }

        //создание столбцов a
        for (int col = 0; col < cols; col++) {
            final int colIndex = col+1;
            TableColumn<ObservableList<String>, String> column = new TableColumn<>("a" + (col + 1));

            column.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().get(colIndex)));
            column.setCellFactory(TextFieldTableCell.forTableColumn());//ячейки редактируемые

            column.setOnEditCommit(event -> {
                ObservableList<String> row = event.getRowValue();
                row.set(colIndex, event.getNewValue());
            });

            restrictions.getColumns().add(column);
        }

        //добавление столбца b
        TableColumn<ObservableList<String>, String> column = new TableColumn<>("b");
        final int colIndex = cols+1;
        column.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().get(colIndex)));

        column.setCellFactory(TextFieldTableCell.forTableColumn());

        column.setOnEditCommit(event -> {
            ObservableList<String> row = event.getRowValue();
            row.set(colIndex, event.getNewValue());
        });

        restrictions.getColumns().add(column);

        restrictions.setEditable(true);
    }

    private void createTargetTable() {
        int cols = countVariables.getValue();


        target.getColumns().clear();
        target.getItems().clear();

        TableColumn<ObservableList<String>, String> fxColumn = new TableColumn<>();
        fxColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(0)));

        fxColumn.setOnEditCommit(event -> {
            ObservableList<String> row = event.getRowValue();
            row.set(0, event.getNewValue());
        });

        target.getColumns().add(fxColumn);

        for (int col = 0; col < cols; col++) {
            final int colIndex = col+1;
            TableColumn<ObservableList<String>, String> column = new TableColumn<>("c" + (col + 1));

            column.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().get(colIndex)));

            column.setCellFactory(TextFieldTableCell.forTableColumn());

            column.setOnEditCommit(event -> {
                ObservableList<String> row = event.getRowValue();
                row.set(colIndex, event.getNewValue());
            });

            target.getColumns().add(column);
        }

        TableColumn<ObservableList<String>, String> column = new TableColumn<>("c");
        final int colIndex = cols+1;
        column.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().get(colIndex)));

        column.setCellFactory(TextFieldTableCell.forTableColumn());

        column.setOnEditCommit(event -> {
            ObservableList<String> row = event.getRowValue();
            row.set(colIndex, event.getNewValue());
        });

        target.getColumns().add(column);

        target.setEditable(true);


            ObservableList<String> rowData = FXCollections.observableArrayList();
            rowData.add("f" + "(x)");
            for (int col = 0; col < cols+1; col++) {
                rowData.add("");
            }
            target.getItems().add(rowData);

    }

    private Set<Pair<Integer, Integer>> selectableCellsSimplex = new HashSet<>();//возможные опорные элементы
    private Pair<Integer, Integer> selectedCellSimplex = null;//выбранный опорный элемент
    private Pair<Integer, Integer> bestCellSimplex = null;//лучший опорный элемент
    private Set<Pair<Integer, Integer>> selectableCellsBasis = new HashSet<>();//возможные опорные элементы
    private Pair<Integer, Integer> selectedCellBasis = null;//выбранный опорный элемент
    private Pair<Integer, Integer> bestCellBasis = null;//лучший опорный элемент

    private void createTableSimplex() {
        if(calculationStopped)return;
        int variableCount = countVariables.getValue();
        tableSimplex.getColumns().clear();
        tableSimplex.getItems().clear();

        tableSimplex.setItems(FXCollections.observableArrayList(stepsSimplex.getLast()));//добавляется последняя таблица из steps

        int totalColumns = variableCount + 2;

        for (int col = 0; col < totalColumns; col++) {
            final int colIndex = col;

            String columnName;
            if (col == 0) {
                columnName = "";
            } else if (col == totalColumns - 1) {
                columnName = "b";
            } else {
                columnName = "x" + col;
            }

            TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnName);
            column.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().get(colIndex)));

            column.setCellFactory(colu -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || getIndex() >= getTableView().getItems().size()) {
                        setText(null);
                        setStyle("");
                        return;
                    }

                    setText(item);
                    Pair<Integer, Integer> cellCoord = new Pair<>(getIndex(), colIndex);//координаты текущего элемента

                    if (cellCoord.equals(selectedCellSimplex)) {
                        setStyle("-fx-background-color: #b3d8ff; -fx-border-color: GREEN; -fx-border-width: 2;");// Выбранная
                    } else if (cellCoord.equals(bestCellSimplex)) {
                        setStyle("-fx-border-color: BLUE; -fx-border-width: 2; -fx-border-style: solid;");//лучшая
                    } else if (selectableCellsSimplex.contains(cellCoord)) {
                        setStyle("-fx-border-color: GREEN; -fx-border-width: 2; -fx-border-style: solid;");// Выделяемая
                    }  else {
                        setStyle("");//остальные
                    }

                    //при нажатии на элемент
                    setOnMouseClicked(event -> {
                        if (selectableCellsSimplex.contains(cellCoord)) {
                            selectedCellSimplex = cellCoord;
                            tableSimplex.refresh();
                        }
                    });
                }
            });

            tableSimplex.getColumns().add(column);
        }

        tableSimplex.refresh();
    }


    private void createTableBasis(){
        if(calculationStopped)return;
        tableBasis.getColumns().clear();
        tableBasis.getItems().clear();

        tableBasis.setItems(FXCollections.observableArrayList(stepsBasis.getLast()));//добавляется последняя таблица из steps

        int totalColumns = stepsBasis.getLast().getFirst().size();

        for (int col = 0; col < totalColumns; col++) {
            //не добавляем базисные столбцы(полностью "0")
            boolean basiscilumn=true;
            for (int i = 0; i < stepsBasis.getLast().size(); i++) {
                if(!stepsBasis.getLast().get(i).get(col).equals("0")) {
                    basiscilumn=false;
                    break;
                }
            }
            if(basiscilumn)continue;

            final int colIndex = col;

            String columnName;
            if (col == 0) {
                columnName = "";
            } else if (col == totalColumns - 1) {
                columnName = "b";
            } else {
                columnName = "x" + col;
            }

            TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnName);
            column.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().get(colIndex)));

            column.setCellFactory(colu -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || getIndex() >= getTableView().getItems().size()) {
                        setText(null);
                        setStyle("");
                        return;
                    }

                    setText(item);
                    Pair<Integer, Integer> cellCoord = new Pair<>(getIndex(), colIndex);//координаты текущего элемента

                    if (cellCoord.equals(selectedCellBasis)) {
                        setStyle("-fx-background-color: #b3d8ff; -fx-border-color: GREEN; -fx-border-width: 2;");// Выбранная
                    } else if (cellCoord.equals(bestCellBasis)) {
                        setStyle("-fx-border-color: BLUE; -fx-border-width: 2; -fx-border-style: solid;");//лучшая
                    } else if (selectableCellsBasis.contains(cellCoord)) {
                        setStyle("-fx-border-color: GREEN; -fx-border-width: 2; -fx-border-style: solid;");// Выделяемая
                    }  else {
                        setStyle("");//остальные
                    }

                    //при нажатии на элемент
                    setOnMouseClicked(event -> {
                        if (selectableCellsBasis.contains(cellCoord)) {
                            selectedCellBasis = cellCoord;
                            tableBasis.refresh();
                        }
                    });
                }
            });

            tableBasis.getColumns().add(column);
        }

        tableBasis.refresh();
    }


    private ObservableList<ObservableList<String>> tableRestrictions;//список для хранения таблицы ограничений
    private ObservableList<String> tableTarget;//для целевой ф-ии
    private Calculations calculations;//объект класса Calculations для универсальной работы с разными видами дробей
    private ObservableList<ObservableList<ObservableList<String>>> stepsSimplex = FXCollections.observableArrayList();//список шагов симплекс метода
    private ObservableList<ObservableList<ObservableList<String>>> stepsBasis = FXCollections.observableArrayList();//список шагов метода искусственного базиса


    private void inputCheck(){
        boolean tableTragetAllZero=true;
        for (int i = 1; i < tableTarget.size(); i++) {
            if(!tableTarget.get(i).matches("^-?\\d+([./]\\d+)?$")){
                calculationStopped = true;
                showAlert("Ошибка", "Некорректный ввод целевой функции");
                return;
            }
            if(!tableTarget.get(i).equals("0")&&i!=(tableTarget.size()-1)){
                tableTragetAllZero=false;
            }
        }
        if(tableTragetAllZero){
            calculationStopped = true;
            showAlert("Ошибка", "Некорректный ввод целевой функции");
            return;
        }

        boolean tableRestrictionsAllZero=true;
        for (int i = 0; i < tableRestrictions.size(); i++) {
            for (int j = 1; j < tableRestrictions.getFirst().size(); j++) {
                if(!tableRestrictions.get(i).get(j).matches("^-?\\d+([/]\\d+)?$")&&fraction.getValue().equals("Обыкновенные")){
                    calculationStopped = true;
                    showAlert("Ошибка", "Некорректный ввод ограничений");
                    return;
                }
                if(!tableRestrictions.get(i).get(j).matches("^-?\\d+([.]\\d+)?$")&&fraction.getValue().equals("Десятичные")){
                    calculationStopped = true;
                    showAlert("Ошибка", "Некорректный ввод ограничений");
                    return;
                }

                if(!tableRestrictions.get(i).get(j).equals("0")&&j!=(tableRestrictions.getFirst().size()-1)){
                    tableRestrictionsAllZero=false;
                }
            }
        }
        if (tableRestrictionsAllZero){
            calculationStopped = true;
            showAlert("Ошибка", "Некорректный ввод ограничений");
        }
    }

    @FXML
    private void onAboutButtonClicked(){
        String text = "Программа предназначена для решения задач линейного программирования симплекс-методом и методом искусственного базиса. Чтобы воспользоваться программой, нужно ввести условие. При вводе данных в таблицы сначала первым кликом выбирается строка, а следующим кликом — ячейка. Также есть возможность сохранить введённое условие (вводится условие, нажимается кнопка \"Применить\", Файл — Сохранить, в открывшемся окне необходимо выбрать путь и название) и открыть уже сохранённое (Файл — Открыть, в открывшемся окне необходимо выбрать файл; условие будет автоматически применено). В каждом методе есть кнопка \"Ответ\" для автоматического вычисления результата с текущего шага, кнопка \"Вперёд\" для перехода к следующему шагу и кнопка \"Назад\" для возврата к предыдущему шагу. На каждом шаге есть возможность выбора опорного элемента. Если он не будет выбран вручную, автоматически выберется элемент, выделенный синей рамкой.";
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Справка");
        alert.setHeaderText("Справка");

        TextArea textArea = new TextArea(text);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(600, 200);

        alert.getDialogPane().setContent(textArea);

        alert.showAndWait();
    }

    @FXML
    private void onSaveFileButtonClicked(){
        if (tableTarget==null){
            showAlert("Ошибка сохранения", "Условие не применено");
            return;
        }
        String selectedFilePath="";
        // Создаем FileChooser
        FileChooser fileChooser = new FileChooser();

        // Устанавливаем фильтры для типа файлов
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Text Files (*.cnd)", "*.cnd");
        fileChooser.getExtensionFilters().add(extFilter);

        // Устанавливаем начальное имя для файла
        fileChooser.setInitialFileName("condition.cnd");

        // Открываем диалог выбора файла
        File selectedFile = fileChooser.showSaveDialog(null);

        // Проверяем, что файл выбран
        if (selectedFile != null) {
            selectedFilePath = selectedFile.getAbsolutePath();
            File file = new File(selectedFilePath);
            try {
                file.createNewFile();
            } catch (IOException e) {
                showAlert("Ошибка сохранения", "Ошибка создания файла");
                return;
            }
        } else {
            showAlert("Ошибка сохранения", "Файл не был выбран.");
            return;
        }

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFilePath))) {
            writer.write(String.valueOf(countVariables.getValue().intValue()));
            writer.newLine();
            writer.write(String.valueOf(countRestrictions.getValue().intValue()));
            writer.newLine();
            writer.write(minMax.getValue());
            writer.newLine();
            writer.write(fraction.getValue());
            writer.newLine();
            for (int i = 0; i < tableTarget.size(); i++) {
                writer.write(tableTarget.get(i));
                if(i!=tableTarget.size()-1)writer.write(" ");
            }
            writer.newLine();
            for (int i = 0; i < tableRestrictions.size(); i++) {
                for (int j = 0; j < tableRestrictions.getFirst().size(); j++) {
                    writer.write(tableRestrictions.get(i).get(j));
                    if(j!=tableRestrictions.getFirst().size()-1)writer.write(" ");
                }
                if(i!=tableRestrictions.size()-1)writer.newLine();
            }
        } catch (IOException e) {
            showAlert("Ошибка сохранения", "Ошибка записи в файл");
        }
    }

    @FXML
    private void onOpenFileButtonClicked(){
        String selectedFilePath="";
        // Создаем FileChooser
        FileChooser fileChooser = new FileChooser();

        // Устанавливаем фильтры для типа файлов
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Text Files (*.cnd)", "*.cnd");
        fileChooser.getExtensionFilters().add(extFilter);


        // Открываем диалог выбора файла
        File selectedFile = fileChooser.showOpenDialog(null);

        // Проверяем, что файл выбран
        if (selectedFile != null) {
            selectedFilePath = selectedFile.getAbsolutePath();
            try(BufferedReader reader = new BufferedReader(new FileReader(selectedFilePath))) {
                //countVariables
                int cv = 0;
                try {
                    cv = Integer.parseInt(reader.readLine());
                } catch (IOException e) {
                    showAlert("Ошибка открытия", "Некорректные данные в файле");
                    return;
                }
                if (cv <= 16 && cv >= 2) {
                    countVariables.getValueFactory().setValue(cv);
                }
                else {
                    showAlert("Ошибка открытия", "Некорректные данные в файле");
                    return;
                }

                //countRestrictions
                int cr = 0;
                try {
                    cr = Integer.parseInt(reader.readLine());
                } catch (IOException e) {
                    showAlert("Ошибка открытия", "Некорректные данные в файле");
                    return;
                }
                if (cr <= 16 && cr >= 2) {
                    countRestrictions.getValueFactory().setValue(cr);
                }
                else{
                    showAlert("Ошибка открытия", "Некорректные данные в файле");
                    return;
                }

                //minMax
                String s = reader.readLine();
                if(s==null){
                    showAlert("Ошибка открытия", "Некорректные данные в файле");
                    return;
                }
                if(s.equals("Минимизировать")||s.equals("Максимизировать")){
                    minMax.setValue(s);
                }
                else {
                    showAlert("Ошибка открытия", "Некорректные данные в файле");
                    return;
                }

                //fraction
                s = reader.readLine();
                if(s==null){
                    showAlert("Ошибка открытия", "Некорректные данные в файле");
                    return;
                }
                if(s.equals("Обыкновенные")||s.equals("Десятичные")){
                    fraction.setValue(s);
                }
                else {
                    showAlert("Ошибка открытия", "Некорректные данные в файле");
                    return;
                }

                //traget
                target.getItems().clear();
                ObservableList<String> line = FXCollections.observableArrayList();
                s = reader.readLine();
                if(s==null){
                    showAlert("Ошибка открытия", "Некорректные данные в файле");
                    return;
                }
                String[] m = s.split(" ");
                line.addAll(m);
                target.getItems().add(line);

                //restrictions
                restrictions.getItems().clear();
                ObservableList<ObservableList<String>> lines = FXCollections.observableArrayList();
                for (int i = 0; i< countRestrictions.getValue(); i++){
                    ObservableList<String> list = FXCollections.observableArrayList();
                    s = reader.readLine();
                    if(s==null){
                        showAlert("Ошибка открытия", "Некорректные данные в файле");
                        return;
                    }
                    String[] mm = s.split(" ");
                    list.addAll(mm);
                    lines.add(list);
                }

                restrictions.getItems().addAll(lines);

            } catch (Exception e) {
                showAlert("Ошибка открытия", "Файл невозможно прочесть");
                return;
            }
        } else {
            showAlert("Ошибка открытия", "Файл не был выбран.");
            return;
        }
        onApplyButtonClicked();
    }

    @FXML
    private void onApplyButtonClicked() {
        calculationStopped=false;
        if(minMax.getSelectionModel().isEmpty()||fraction.getSelectionModel().isEmpty()){
            calculationStopped=true;
            showAlert("Ошибка", "Условие не задано");
        }
        tableRestrictions = restrictions.getItems();
        tableTarget = target.getItems().getFirst();
        inputCheck();
        calculations = new Calculations(fraction.getValue());

        //симплекс
        selectableCellsSimplex = new HashSet<>();//возможные опорные элементы
        selectedCellSimplex = null;//выбранный опорный элемент
        bestCellSimplex = null;//лучший опорный элемент
        answerSimplexText.setText("");
        stepsSimplex = FXCollections.observableArrayList();

        formationBasisSimplex();
        negativeCoefficients(stepsSimplex);
        addDeltsSimplex();
        if(!calculationStopped){
            if(!optimalSimplex()) supportElementsSimplex();
            createTableSimplex();
            answerSimplex.setDisable(false);
            nextSimplex.setDisable(false);
        }


        //искусственный базис
        selectableCellsBasis = new HashSet<>();//возможные опорные элементы
        selectedCellBasis = null;//выбранный опорный элемент
        bestCellBasis = null;//лучший опорный элемент
        answerBasisText.setText("");
        stepsBasis = FXCollections.observableArrayList();
        answerCount=0;

        formationBasisBasis();
        negativeCoefficients(stepsBasis);
        addDeltsBasisAdd();
        if(!calculationStopped){
            if(!optimalBasisAdd()) supportElementsBasisAdd();
            createTableBasis();
            answerBasis.setDisable(false);
            nextBasis.setDisable(false);
        }
    }

    @FXML
    private void onAnswerSimplexButtonClicked(){
        if(stepsSimplex.isEmpty()){
            showAlert("Ошибка", "Условие не задано");
            return;
        }
        while (!optimalSimplex()){
            if (calculationStopped) break;
            supportElementsSimplex();
            selectedCellSimplex = bestCellSimplex;
            moreOptimalSimplex();
        }

        bestCellSimplex =null;
        selectedCellSimplex =null;
        selectableCellsSimplex =new HashSet<Pair<Integer, Integer>>();
        createTableSimplex();
        answerSimplexText.setText(formationAnswer(stepsSimplex));
        answerSimplex.setDisable(true);
        nextSimplex.setDisable(true);
        backSimplex.setDisable(false);
    }

    @FXML
    private void onNextSimplexButtonClicked(){
        if(stepsSimplex.isEmpty()){
            showAlert("Ошибка", "Условие не задано");
            return;
        }
        if (!optimalSimplex()) {
            if(selectedCellSimplex ==null){
                selectedCellSimplex = bestCellSimplex;
            }
            moreOptimalSimplex();
            selectedCellSimplex =null;
            selectableCellsSimplex =new HashSet<>();
            bestCellSimplex =null;
            if(!optimalSimplex()){
                supportElementsSimplex();
            }
            else {
                answerSimplexText.setText(formationAnswer(stepsSimplex));
                answerSimplex.setDisable(true);
                nextSimplex.setDisable(true);
                backSimplex.setDisable(false);
            }
            createTableSimplex();
        }else {
            answerSimplexText.setText(formationAnswer(stepsSimplex));
            answerSimplex.setDisable(true);
            nextSimplex.setDisable(true);
            backSimplex.setDisable(false);
        }
    }

    @FXML
    private void onBackSimplexButtonClicked(){
        if(stepsSimplex.isEmpty()){
            showAlert("Ошибка", "Условие не задано");
            return;
        }
        answerSimplexText.setText("");
        if(stepsSimplex.size()>3){
            stepsSimplex.removeLast();
            selectedCellSimplex =null;
            selectableCellsSimplex =new HashSet<>();
            bestCellSimplex =null;
            supportElementsSimplex();
            createTableSimplex();
        } else if (stepsSimplex.size()==3) {
            backSimplex.setDisable(true);
        }
        nextSimplex.setDisable(false);
        answerSimplex.setDisable(false);
    }

    @FXML
    private void onAnswerBasisButtonClicked(){
        if(stepsBasis.isEmpty()){
            showAlert("Ошибка", "Условие не задано");
            return;
        }
        if(answerCount==0){
            while (!optimalBasisAdd()){
                if (calculationStopped) break;
                supportElementsBasisAdd();
                if(selectedCellBasis ==null){
                    selectedCellBasis = bestCellBasis;
                }
                moreOptimalBasisAdd();
                selectedCellBasis =null;
                selectableCellsBasis =new HashSet<>();
                bestCellBasis =null;
            }
            answerCount=1;
            bestCellBasis =null;
            selectedCellBasis =null;
            selectableCellsBasis =new HashSet<Pair<Integer, Integer>>();
            deleteAddVariables();
        }

        if(answerCount==1){
            while (answerCount!=2){
                if(optimalBasisAdd()){//создаём основную(сейчас последний шаг - доп задача)
                    stepsBasis.add(deepCopyTable(stepsBasis.getLast()));
                    ObservableList<ObservableList<String>> cur = stepsBasis.getLast();
                    //вычисляем новые дельты
                    cur.removeLast();//удаляет с текущего шага дельты
                    addDeltsBasis();//добавляет в steps новый шаг с обновлёнными дельтами
                    stepsBasis.remove(stepsBasis.size()-2);//удаляет текущий шаг без дельт
                    if (!optimalBasis()){
                        supportElementsBasis();
                        answerBasisText.setText("");
                    }
                    else{//созданная оптимальна
                        answerBasisText.setText(formationAnswer(stepsBasis));
                        answerCount=2;
                    }
                    createTableBasis();
                }
                else {//последний шаг неоптимальная основная
                    if(selectedCellBasis ==null){
                        selectedCellBasis = bestCellBasis;
                    }
                    moreOptimalBasis();
                    selectedCellBasis =null;
                    selectableCellsBasis =new HashSet<>();
                    bestCellBasis =null;
                    if(!optimalBasis()){
                        supportElementsBasis();
                    }
                    else {
                        answerBasisText.setText(formationAnswer(stepsBasis));
                        answerCount=2;
                    }
                    createTableBasis();
                }
            }
        }

        answerBasis.setDisable(true);
        nextBasis.setDisable(true);
        backBasis.setDisable(false);
    }

    //временный метод для отладки
    private void printSteps(ObservableList<ObservableList<ObservableList<String>>> steps){
        if (calculationStopped){
            return;
        }
        for (int i = 0; i < steps.size(); i++) {
            steps.get(i).forEach(System.out::println);
            System.out.println("-".repeat(30));
        }
    }

    private int answerCount = 0;
    @FXML
    private void onNextBasisButtonClicked(){
        if(stepsBasis.isEmpty()){
            showAlert("Ошибка", "Условие не задано");
            return;
        }
        if(answerCount==0){
            if (!optimalBasisAdd()) {
                if(selectedCellBasis ==null){
                    selectedCellBasis = bestCellBasis;
                }
                moreOptimalBasisAdd();
                selectedCellBasis =null;
                selectableCellsBasis =new HashSet<>();
                bestCellBasis =null;
                if(!optimalBasisAdd()){
                    supportElementsBasisAdd();
                }
                else {
                    deleteAddVariables();
                    answerBasisText.setText(formationAnswerBasisAdd());
                    answerCount+=1;
                }
                createTableBasis();
            }else {
                deleteAddVariables();
                answerBasisText.setText(formationAnswerBasisAdd());
                answerCount+=1;
            }
        }else if(answerCount==1){//решена доп задача
            if(optimalBasisAdd()){//создаём основную(сейчас последний шаг - доп задача)
                stepsBasis.add(deepCopyTable(stepsBasis.getLast()));
                ObservableList<ObservableList<String>> cur = stepsBasis.getLast();
                //вычисляем новые дельты
                cur.removeLast();//удаляет с текущего шага дельты
                addDeltsBasis();//добавляет в steps новый шаг с обновлёнными дельтами
                stepsBasis.remove(stepsBasis.size()-2);//удаляет текущий шаг без дельт
                if (!optimalBasis()){
                    supportElementsBasis();
                    answerBasisText.setText("");
                }
                else{//созданная оптимальна
                    answerBasisText.setText(formationAnswer(stepsBasis));
                    answerCount+=1;
                    answerBasis.setDisable(true);
                    nextBasis.setDisable(true);
                    backBasis.setDisable(false);
                }
                createTableBasis();
            }
            else {//последний шаг неоптимальная основная
                if(selectedCellBasis ==null){
                    selectedCellBasis = bestCellBasis;
                }
                moreOptimalBasis();
                selectedCellBasis =null;
                selectableCellsBasis =new HashSet<>();
                bestCellBasis =null;
                if(!optimalBasis()){
                    supportElementsBasis();
                }
                else {
                    answerBasisText.setText(formationAnswer(stepsBasis));
                    answerCount+=1;
                    answerBasis.setDisable(true);
                    nextBasis.setDisable(true);
                    backBasis.setDisable(false);
                }
                createTableBasis();
            }
        }
        for (int i = 0; i < stepsBasis.getLast().size()-1; i++) {
            boolean zero = true;
            for (int j = 1; j < stepsBasis.getLast().get(i).size(); j++) {
                if(!stepsBasis.getLast().get(i).get(j).equals("0")){
                    zero=false;
                    break;
                }
            }
            if(zero)stepsBasis.getLast().remove(i);
        }
        backBasis.setDisable(false);
    }

    @FXML
    private void onBackBasisButtonClicked(){
        if(stepsBasis.isEmpty()){
            showAlert("Ошибка", "Условие не задано");
            return;
        }
        answerBasisText.setText("");
        if(stepsBasis.size()>3){
            selectedCellBasis =null;
            selectableCellsBasis =new HashSet<>();
            bestCellBasis =null;
            if(answerCount==2){
                answerCount=1;
                stepsBasis.removeLast();
                if (!optimalBasisAdd()){
                    supportElementsBasis();
                }else {//после удаления текущий шаг-решённая доп задача
                    answerBasisText.setText(formationAnswerBasisAdd());
                }
            } else if (answerCount == 1) {
                if(optimalBasis())answerCount=0;//назад нажали на решённой доп задаче
                stepsBasis.removeLast();
                if (!optimalBasisAdd()){
                    supportElementsBasis();
                }else {//текущий шаг - решённая доп задача
                    answerBasisText.setText(formationAnswerBasisAdd());
                    answerCount=0;
                }
            }else {
                stepsBasis.removeLast();
                supportElementsBasisAdd();
            }
            createTableBasis();
        }
        if (stepsBasis.size()==3) {
            backBasis.setDisable(true);
        }
        nextBasis.setDisable(false);
        answerBasis.setDisable(false);
    }

    private String formationAnswer(ObservableList<ObservableList<ObservableList<String>>> steps){
        String answer = "X: ";
        String[] X = new String[countVariables.getValue()];
        for (int i = 0; i < countVariables.getValue(); i++) {
            X[i]="0";
        }
        ObservableList<ObservableList<String>> cur = steps.getLast();
        for (int i = 0; i < cur.size()-1; i++) {
            X[Integer.valueOf(cur.get(i).getFirst().substring(1))-1]=cur.get(i).getLast();
        }
        answer+= Arrays.toString(X);
        //String F="0";
        //for (int i=1; i<tableTarget.size()-1; i++) {
        //    F=calculations.plus(F,calculations.multiply(tableTarget.get(i), X[i-1]));
        //}
        answer+="; F="+cur.getLast().getLast();
        return answer;
    }

    private String formationAnswerBasisAdd(){
        String answer = "Доп. задача решена X: ";
        String[] X = new String[countVariables.getValue()];
        for (int i = 0; i < countVariables.getValue(); i++) {
            X[i]="0";
        }
        ObservableList<ObservableList<String>> cur = stepsBasis.getLast();
        for (int i = 0; i < cur.size()-1; i++) {
            if(Integer.valueOf(cur.get(i).getFirst().substring(1))<=countVariables.getValue()){
                X[Integer.valueOf(cur.get(i).getFirst().substring(1))-1]=cur.get(i).getLast();
            }

        }
        answer+= Arrays.toString(X);
        return answer;
    }

    //глубокое копирование таблиц, во избежание редактирования двух одновременно
    public static ObservableList<ObservableList<String>> deepCopyTable(ObservableList<ObservableList<String>> table) {
        ObservableList<ObservableList<String>> copy = FXCollections.observableArrayList();

        for (ObservableList<String> row : table) {
            ObservableList<String> newRow = FXCollections.observableArrayList();
            for (String cell : row) {
                newRow.add(cell);
            }
            copy.add(newRow);
        }

        return copy;
    }

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private void formationBasisSimplex(){
        if(calculationStopped)return;
        stepsSimplex.add(deepCopyTable(tableRestrictions));
        stepsSimplex.getFirst().forEach(x -> x.set(0, "?"));
        ObservableList<ObservableList<String>> cur = stepsSimplex.get(0);
        ArrayList<Integer> columnbasisindex = new ArrayList<>();
        //ищем столбцы из одной 1 и остальных нулей
        for (int i = 1; i < cur.get(0).size()-1; i++) {
            ArrayList<String> column = new ArrayList<>();
            for (int j = 0; j < cur.size(); j++) {
                column.add(cur.get(j).get(i));
            }
            if(column.contains("1")&&column.stream().filter(x->x.equals("0")).count()==column.size()-1){
                cur.get(column.indexOf("1")).set(0, "x"+i);
                columnbasisindex.add(i);
            }
        }

        //ищем столбцы полностью из нулей кроме одного числа
        if (columnbasisindex.size()<cur.size()){
            for (int i = 1; i < cur.get(0).size()-1; i++) {//столбец
                ArrayList<String> column = new ArrayList<>();
                for (int j = 0; j < cur.size(); j++) {
                    column.add(cur.get(j).get(i));
                }
                if(!column.contains("1")&&column.stream().filter(x->x.equals("0")).count()==column.size()-1){
                    String curbasis = "";
                    int curbasisindex = 0;//строка
                    for (int j = 0; j < column.size(); j++) {//строка
                        if(!column.get(j).equals("0")){
                            curbasis=column.get(j);
                            curbasisindex=j;
                        }
                    }
                    //делим строку на базисную переменную
                    if(!curbasis.equals("")){
                        for (int j = 1; j < cur.get(0).size(); j++) {
                            cur.get(curbasisindex).set(j, calculations.divide(cur.get(curbasisindex).get(j), curbasis));
                        }
                        cur.get(curbasisindex).set(0, "x"+i);
                        columnbasisindex.add(i);
                    }
                }
            }
        }
        //делим строку на базисный элемент и вычитаем из остальных
        if(columnbasisindex.size()<cur.size()){
            for (int i = 1; i < cur.get(0).size()-1; i++) {//столбец
                if(!columnbasisindex.contains(i)&&columnbasisindex.size()<cur.size()){
                    String curBasis = "";
                    int curBasisRowIndex = 0;
                    int curBasisColIndex = i;
                    for (int j = 0; j < cur.size(); j++) {//строка
                        if(!columnbasisindex.contains(i)&&cur.get(j).get(0).equals("?")&&!cur.get(j).get(i).equals("0")){
                            curBasis = cur.get(j).get(i);
                            curBasisRowIndex=j;
                            cur.get(curBasisRowIndex).set(0, "x"+i);
                            columnbasisindex.add(i);
                            break;
                        }
                    }
                    if(!curBasis.equals("")){
                        for (int j = 1; j < cur.get(0).size(); j++) {
                            cur.get(curBasisRowIndex).set(j, calculations.divide(cur.get(curBasisRowIndex).get(j), curBasis));
                        }
                        for (int j = 0; j < cur.size(); j++) {//строка
                            if(j!=curBasisRowIndex){
                                String valueForMultiply = cur.get(j).get(curBasisColIndex);
                                for (int k = 1; k < cur.get(0).size(); k++) {
                                    cur.get(j).set(k, calculations.minus(cur.get(j).get(k), calculations.multiply(cur.get(curBasisRowIndex).get(k), valueForMultiply)));
                                }
                            }
                        }
                    }
                }
            }
        }
        //удаление нулевых строк
        for (int i = cur.size() - 1; i >= 0; i--) {
            boolean zeroRow = true;
            for (int j = 1; j < cur.get(i).size()-1; j++) {
                if (!cur.get(i).get(j).equals("0")) {
                    zeroRow = false;
                }
            }
            if (zeroRow) {
                cur.remove(i);
            }
        }
    }
    private void negativeCoefficients(ObservableList<ObservableList<ObservableList<String>>> steps){
        if(calculationStopped)return;
        steps.add(deepCopyTable(steps.getLast()));
        ObservableList<ObservableList<String>> cur = steps.getLast();
        boolean containsNegativeCoefficients = false;//отрицательные коэффициенты b
        for (int i = 0; i < cur.size(); i++) {
            if(cur.get(i).get(cur.get(i).size()-1).startsWith("-")){
                containsNegativeCoefficients=true;
                break;
            }
        }

        while (containsNegativeCoefficients){
            String maxAbsB = "0";//максимальный по модулю отрицательный коэффициент b
            int indexMaxAbsB = 0;
            //поиск maxAbsB
            for (int i = 0; i < cur.size(); i++) {//строка
                String curB=cur.get(i).get(cur.getFirst().size()-1);
                if(curB.startsWith("-")){
                    int compare = Calculations.compareByAbsoluteValue(curB, maxAbsB);
                    if(compare>0){
                        maxAbsB=curB;
                        indexMaxAbsB=i;
                    }
                }
            }
            boolean containsnegativ = false;//строка с maxAbsB содержит отрицательные элементы
            for (int j = 1; j < cur.getFirst().size() - 1; j++) {//столбец
                if(cur.get(indexMaxAbsB).get(j).startsWith("-")){
                    containsnegativ=true;
                    break;
                }
            }
            if(!containsnegativ){
                calculationStopped = true;
                showAlert("Ошибка", "Решения задачи не существует. Введите новые данные и повторите попытку.");
                steps =FXCollections.observableArrayList();
                return;
            }

            String maxValueInAbsB = "0";//максимальный по модулю отрицательный элемент в этой строке
            int indexValueInAbsB = 0;
            for (int i = 1; i < cur.getFirst().size()-1; i++) {
                String curValueInAbsB=cur.get(indexMaxAbsB).get(i);
                if(curValueInAbsB.startsWith("-")){
                    int compare = Calculations.compareByAbsoluteValue(curValueInAbsB, maxValueInAbsB);
                    if(compare>0){
                        maxValueInAbsB=curValueInAbsB;
                        indexValueInAbsB=i;
                    }
                }
            }
            for (int i = 1; i < cur.get(0).size(); i++) {
                cur.get(indexMaxAbsB).set(i, calculations.divide(cur.get(indexMaxAbsB).get(i), maxValueInAbsB));//делим строку на maxValueInAbsB
                cur.get(indexMaxAbsB).set(0, "x"+indexValueInAbsB);//новый базис
            }
            for (int i = 0; i < cur.size(); i++) {//строка
                if(i!=indexMaxAbsB){
                    String valueForMultiply = cur.get(i).get(indexValueInAbsB);
                    for (int j = 1; j < cur.get(0).size(); j++) {
                        cur.get(i).set(j, calculations.minus(cur.get(i).get(j), calculations.multiply(valueForMultiply, cur.get(indexMaxAbsB).get(j))));//вычитаем из остальных строк
                    }
                }
            }
            containsNegativeCoefficients=false;
            for (int i = 0; i < cur.size(); i++) {
                if(cur.get(i).get(cur.get(i).size()-1).startsWith("-")){
                    containsNegativeCoefficients=true;
                    break;
                }
            }
        }
    }
    private void addDeltsSimplex(){
        if(calculationStopped)return;
        stepsSimplex.add(deepCopyTable(stepsSimplex.getLast()));
        ObservableList<ObservableList<String>> cur = stepsSimplex.getLast();
        ObservableList<String> delts = FXCollections.observableArrayList();
        delts.add("Δ");
        for (int i = 1; i < cur.get(0).size(); i++) {//столбец
            String curDelta = "0";
            for (int j = 0; j < cur.size(); j++) {//строка
                String curC = tableTarget.get(Integer.parseInt(cur.get(j).getFirst().substring(1)));
                curDelta=calculations.plus(curDelta, calculations.multiply(cur.get(j).get(i), curC));
            }
            curDelta=calculations.minus(curDelta, tableTarget.get(i));
            delts.add(curDelta);
        }
        cur.add(delts);
    }
    private boolean optimalSimplex(){
        ObservableList<ObservableList<String>> cur = stepsSimplex.getLast();
        if(minMax.getValue().equals("Максимизировать")){
            for (int i = 1; i < cur.getFirst().size()-1; i++) {
                if(cur.getLast().get(i).startsWith("-")){
                    return false;
                }
            }
            return true;
        }
        else {
            for (int i = 1; i < cur.getFirst().size()-1; i++) {
                if(!cur.getLast().get(i).startsWith("-")&&!cur.getLast().get(i).equals("0")){
                    return false;
                }
            }
            return true;
        }
    }

    //нахождение возможных опорных элементов и лучшего
    private void supportElementsSimplex(){
        if(calculationStopped)return;
        ObservableList<ObservableList<String>> cur = stepsSimplex.getLast();
        int needDeltIndex=0;//при максимизации минимальная дельта и наоборот
        if (minMax.getValue().equals("Максимизировать")){
            String minDelt=String.valueOf(Integer.MAX_VALUE/2);
            for (int i = 1; i < cur.getFirst().size()-1; i++) {
                int parse = Calculations.compare(cur.getLast().get(i), minDelt);
                if (parse<0&&!cur.getLast().get(i).equals("0")){
                    needDeltIndex=i;
                    minDelt=cur.getLast().get(i);
                }
            }
        }
        else {
            String maxDelt=String.valueOf(Integer.MIN_VALUE/2);
            for (int i = 1; i < cur.getFirst().size()-1; i++) {
                int parse = Calculations.compare(cur.getLast().get(i), maxDelt);
                if (parse>0&&!cur.getLast().get(i).equals("0")){
                    needDeltIndex=i;
                    maxDelt=cur.getLast().get(i);
                }
            }
        }
        ArrayList<String> q = new ArrayList<>();//симплекс-отношения
        for (int i = 0; i < cur.size()-1; i++) {//строка
            if(!cur.get(i).get(needDeltIndex).equals("0")){
                String curQ = calculations.divide(cur.get(i).getLast(), cur.get(i).get(needDeltIndex));
                if(!curQ.startsWith("-")){
                    q.add(curQ);
                }
                else{
                    q.add("-");
                }
            }
            else {
                q.add("-");
            }
        }
        if(q.stream().allMatch(x -> x.equals("-"))){
            calculationStopped = true;
            showAlert("Ошибка", "Целевая функция не ограничена и решения не существует. Введите новые данные и повторите попытку.");
            stepsSimplex =FXCollections.observableArrayList();
            return;
        }
        else {
            String minq=String.valueOf(Integer.MAX_VALUE);
            for (int i = 0; i < q.size(); i++) {
                if(!q.get(i).equals("-")){
                    int compare = Calculations.compare(q.get(i), minq);
                    if(compare<0){
                        bestCellSimplex =new Pair<>(i, needDeltIndex);
                        minq=q.get(i);
                    }
                }
            }
            for (int i = 1; i < cur.getFirst().size()-1; i++) {//столбец
                //если максимизировать то ищем столбцы с отрицательной дельтой
                if(cur.getLast().get(i).startsWith("-")&&minMax.getValue().equals("Максимизировать")){
                    String maxValue = String.valueOf(Integer.MIN_VALUE);
                    int maxValueIndex=0;
                    //в этом столбце максимальное значение
                    for (int j = 0; j < cur.size()-1; j++) {
                        int compare = Calculations.compare(cur.get(j).get(i), maxValue);
                        if(compare>0){
                            maxValue=cur.get(j).get(i);
                            maxValueIndex=j;
                        }
                    }
                    selectableCellsSimplex.add(new Pair<>(maxValueIndex, i));
                //с положительной дельтой
                } else if (!cur.getLast().get(i).startsWith("-")&&!cur.getLast().get(i).equals("0")&& minMax.getValue().equals("Минимизировать")) {
                    String maxValue = String.valueOf(Integer.MIN_VALUE);
                    int maxValueIndex=0;
                    for (int j = 0; j < cur.size()-1; j++) {
                        int compare = Calculations.compare(cur.get(j).get(i), maxValue);
                        if(compare>0){
                            maxValue=cur.get(j).get(i);
                            maxValueIndex=j;
                        }
                    }
                    selectableCellsSimplex.add(new Pair<>(maxValueIndex, i));
                }
            }
        }
    }

    private void moreOptimalSimplex(){
        if(calculationStopped)return;
        stepsSimplex.add(deepCopyTable(stepsSimplex.getLast()));
        ObservableList<ObservableList<String>> cur = stepsSimplex.getLast();
        String selectedCellValue = cur.get(selectedCellSimplex.getKey()).get(selectedCellSimplex.getValue());
        cur.get(selectedCellSimplex.getKey()).set(0, "x"+(selectedCellSimplex.getValue()));//значение базиса в первом столбце
        //делим разрешающую строку на разрешающий элемент
        for (int i = 1; i < cur.getFirst().size(); i++) {//столбец
            cur.get(selectedCellSimplex.getKey()).set(i, calculations.divide(cur.get(selectedCellSimplex.getKey()).get(i), selectedCellValue));
        }
        //вычитаем из остальных строк разрешающую строку
        for (int i = 0; i < cur.size()-1; i++) {//строка
            if (i!= selectedCellSimplex.getKey()){
                String valueForMultiply = cur.get(i).get(selectedCellSimplex.getValue());
                for (int j = 1; j < cur.getFirst().size(); j++) {//столбец
                    cur.get(i).set(j, calculations.minus(cur.get(i).get(j), calculations.multiply(cur.get(selectedCellSimplex.getKey()).get(j), valueForMultiply)));
                }
            }
        }
        //вычисляем новые дельты
        cur.removeLast();//удаляет с текущего шага дельты
        addDeltsSimplex();//добавляет в steps новый шаг с обновлёнными дельтами
        stepsSimplex.remove(stepsSimplex.size()-2);//удаляет текущий шаг без дельт
    }

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private void formationBasisBasis(){
        if(calculationStopped)return;
        stepsBasis.add(deepCopyTable(tableRestrictions));
        stepsBasis.getFirst().forEach(x -> x.set(0, "?"));
        ObservableList<ObservableList<String>> cur = stepsBasis.getFirst();
        ArrayList<Integer> columnbasisindex = new ArrayList<>();
        //ищем столбцы из одной 1 и остальных нулей
        for (int i = 1; i < cur.getFirst().size()-1; i++) {//столбец
            ArrayList<String> column = new ArrayList<>();
            for (int j = 0; j < cur.size(); j++) {
                column.add(cur.get(j).get(i));
            }
            if(column.contains("1")&&column.stream().filter(x->x.equals("0")).count()==column.size()-1){
                cur.get(column.indexOf("1")).set(0, "x"+i);
                columnbasisindex.add(i);
            }
        }
        //добавляем искусственный базис
        int curCountX=countVariables.getValue();
        while (columnbasisindex.size()<cur.size()){
            cur.forEach(x->x.add(x.size()-1, "0"));//добавляем столбец
            //добавляем базисную переменную
            for (int i = 0; i < cur.size(); i++) {//строка
                if(cur.get(i).getFirst().equals("?")){
                    curCountX+=1;
                    cur.get(i).set(cur.getFirst().size()-2, "1");
                    cur.get(i).set(0, "x"+curCountX);
                    columnbasisindex.add(cur.getFirst().size()-2);
                    break;
                }
            }
        }
    }

    //добавление дельт в дополнительную задачу
    private void addDeltsBasisAdd(){
        if(calculationStopped)return;
        stepsBasis.add(deepCopyTable(stepsBasis.getLast()));
        ObservableList<ObservableList<String>> cur = stepsBasis.getLast();
        ObservableList<String> delts = FXCollections.observableArrayList();
        delts.add("Δ");
        int[] curTarget = new int[cur.getFirst().size()];//целевая для доп
        for (int i = countVariables.getValue()+1; i < cur.getFirst().size()-1; i++) {
            curTarget[i]=1;
        }
        for (int i = 1; i < cur.get(0).size(); i++) {//столбец
            String curDelta = "0";
            for (int j = 0; j < cur.size(); j++) {//строка
                String curC = String.valueOf(curTarget[Integer.parseInt(cur.get(j).getFirst().substring(1))]);
                curDelta=calculations.plus(curDelta, calculations.multiply(cur.get(j).get(i), curC));
            }
            curDelta=calculations.minus(curDelta, String.valueOf(curTarget[i]));
            delts.add(curDelta);
        }
        cur.add(delts);
    }

    //нахождение возможных опорных элементов и лучшего
    private void supportElementsBasisAdd(){
        if(calculationStopped)return;
        ObservableList<ObservableList<String>> cur = stepsBasis.getLast();
        int needDeltIndex=0;//при максимизации минимальная дельта и наоборот
        String maxDelt=String.valueOf(Integer.MIN_VALUE/2);
        for (int i = 1; i < cur.getFirst().size()-1; i++) {
            int parse = Calculations.compare(cur.getLast().get(i), maxDelt);
            if (parse>0&&!cur.getLast().get(i).equals("0")){
                needDeltIndex=i;
                maxDelt=cur.getLast().get(i);
            }
        }
        ArrayList<String> q = new ArrayList<>();//симплекс-отношения
        for (int i = 0; i < cur.size()-1; i++) {//строка
            if(!cur.get(i).get(needDeltIndex).equals("0")&&Integer.parseInt(cur.get(i).getFirst().substring(1))>countVariables.getValue()){//если значение "0" или в этой строке базис это не добавленный x то q="-"
                String curQ = calculations.divide(cur.get(i).getLast(), cur.get(i).get(needDeltIndex));
                if(!curQ.startsWith("-")){
                    q.add(curQ);
                }
                else{
                    q.add("-");
                }
            }
            else {
                q.add("-");
            }
        }
        if(q.stream().allMatch(x -> x.equals("-"))){
            calculationStopped = true;
            showAlert("Ошибка", "Целевая функция не ограничена и решения не существует. Введите новые данные и повторите попытку.");
            stepsBasis =FXCollections.observableArrayList();
            return;
        }
        else {
            String minq=String.valueOf(Integer.MAX_VALUE);
            for (int i = 0; i < q.size(); i++) {
                if(!q.get(i).equals("-")){
                    int compare = Calculations.compare(q.get(i), minq);
                    if(compare<0){
                        bestCellBasis =new Pair<>(i, needDeltIndex);
                        minq=q.get(i);
                    }
                }
            }
            for (int i = 1; i < cur.getFirst().size()-1; i++) {//столбец
                if (!cur.getLast().get(i).startsWith("-")&&!cur.getLast().get(i).equals("0")) {
                    String maxValue = String.valueOf(Integer.MIN_VALUE);
                    int maxValueIndex=0;
                    for (int j = 0; j < cur.size()-1; j++) {
                        int compare = Calculations.compare(cur.get(j).get(i), maxValue);
                        if(compare>0){
                            maxValue=cur.get(j).get(i);
                            maxValueIndex=j;
                        }
                    }
                    selectableCellsBasis.add(new Pair<>(maxValueIndex, i));
                }
            }
        }
    }

    private boolean optimalBasisAdd(){
        ObservableList<ObservableList<String>> cur = stepsBasis.getLast();
        for (int i = 1; i < cur.getFirst().size()-1; i++) {
            if(!cur.getLast().get(i).startsWith("-")&&!cur.getLast().get(i).equals("0")){
                return false;
            }
        }
        return true;
    }

    private void moreOptimalBasisAdd(){
        if(calculationStopped)return;
        stepsBasis.add(deepCopyTable(stepsBasis.getLast()));
        ObservableList<ObservableList<String>> cur = stepsBasis.getLast();
        String selectedCellValue = cur.get(selectedCellBasis.getKey()).get(selectedCellBasis.getValue());
        cur.get(selectedCellBasis.getKey()).set(0, "x"+(selectedCellBasis.getValue()));//значение базиса в первом столбце
        //делим разрешающую строку на разрешающий элемент
        for (int i = 1; i < cur.getFirst().size(); i++) {//столбец
            cur.get(selectedCellBasis.getKey()).set(i, calculations.divide(cur.get(selectedCellBasis.getKey()).get(i), selectedCellValue));
        }
        //вычитаем из остальных строк разрешающую строку
        for (int i = 0; i < cur.size()-1; i++) {//строка
            if (i!= selectedCellBasis.getKey()){
                String valueForMultiply = cur.get(i).get(selectedCellBasis.getValue());
                for (int j = 1; j < cur.getFirst().size(); j++) {//столбец
                    cur.get(i).set(j, calculations.minus(cur.get(i).get(j), calculations.multiply(cur.get(selectedCellBasis.getKey()).get(j), valueForMultiply)));
                }
            }
        }
        //вычисляем новые дельты
        cur.removeLast();//удаляет с текущего шага дельты
        addDeltsBasisAdd();//добавляет в steps новый шаг с обновлёнными дельтами
        stepsBasis.remove(stepsBasis.size()-2);//удаляет текущий шаг без дельт
    }

    private void deleteAddVariables(){
        if(calculationStopped)return;
        stepsBasis.add(deepCopyTable(stepsBasis.getLast()));
        ObservableList<ObservableList<String>> cur = stepsBasis.getLast();
        int size = cur.getLast().size();
        for (int i = 0; i < cur.size(); i++) {
            for (int j = 0; j < size - countVariables.getValue() - 2; j++) {
                cur.get(i).remove(cur.get(i).size()-2);
            }
        }
        stepsBasis.remove(stepsBasis.size()-2);
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private void addDeltsBasis(){
        if(calculationStopped)return;
        stepsBasis.add(deepCopyTable(stepsBasis.getLast()));
        ObservableList<ObservableList<String>> cur = stepsBasis.getLast();
        ObservableList<String> delts = FXCollections.observableArrayList();
        delts.add("Δ");
        for (int i = 1; i < cur.get(0).size(); i++) {//столбец
            String curDelta = "0";
            for (int j = 0; j < cur.size(); j++) {//строка
                String curC = tableTarget.get(Integer.parseInt(cur.get(j).getFirst().substring(1)));
                curDelta=calculations.plus(curDelta, calculations.multiply(cur.get(j).get(i), curC));
            }
            curDelta=calculations.minus(curDelta, tableTarget.get(i));
            delts.add(curDelta);
        }
        cur.add(delts);
    }

    private boolean optimalBasis(){
        ObservableList<ObservableList<String>> cur = stepsBasis.getLast();
        if(minMax.getValue().equals("Максимизировать")){
            for (int i = 1; i < cur.getFirst().size()-1; i++) {
                if(cur.getLast().get(i).startsWith("-")){
                    return false;
                }
            }
            return true;
        }
        else {
            for (int i = 1; i < cur.getFirst().size()-1; i++) {
                if(!cur.getLast().get(i).startsWith("-")&&!cur.getLast().get(i).equals("0")){
                    return false;
                }
            }
            return true;
        }
    }

    private void supportElementsBasis(){
        if(calculationStopped)return;
        ObservableList<ObservableList<String>> cur = stepsBasis.getLast();
        int needDeltIndex=0;//при максимизации минимальная дельта и наоборот
        if (minMax.getValue().equals("Максимизировать")){
            String minDelt=String.valueOf(Integer.MAX_VALUE/2);
            for (int i = 1; i < cur.getFirst().size()-1; i++) {
                int parse = Calculations.compare(cur.getLast().get(i), minDelt);
                if (parse<0&&!cur.getLast().get(i).equals("0")){
                    needDeltIndex=i;
                    minDelt=cur.getLast().get(i);
                }
            }
        }
        else {
            String maxDelt=String.valueOf(Integer.MIN_VALUE/2);
            for (int i = 1; i < cur.getFirst().size()-1; i++) {
                int parse = Calculations.compare(cur.getLast().get(i), maxDelt);
                if (parse>0&&!cur.getLast().get(i).equals("0")){
                    needDeltIndex=i;
                    maxDelt=cur.getLast().get(i);
                }
            }
        }
        ArrayList<String> q = new ArrayList<>();//симплекс-отношения
        for (int i = 0; i < cur.size()-1; i++) {//строка
            if(!cur.get(i).get(needDeltIndex).equals("0")){
                String curQ = calculations.divide(cur.get(i).getLast(), cur.get(i).get(needDeltIndex));
                if(!curQ.startsWith("-")){
                    q.add(curQ);
                }
                else{
                    q.add("-");
                }
            }
            else {
                q.add("-");
            }
        }
        if(q.stream().allMatch(x -> x.equals("-"))){
            calculationStopped = true;
            showAlert("Ошибка", "Целевая функция не ограничена и решения не существует. Введите новые данные и повторите попытку.");
            stepsBasis =FXCollections.observableArrayList();
            return;
        }
        else {
            String minq=String.valueOf(Integer.MAX_VALUE);
            for (int i = 0; i < q.size(); i++) {
                if(!q.get(i).equals("-")){
                    int compare = Calculations.compare(q.get(i), minq);
                    if(compare<0){
                        bestCellBasis =new Pair<>(i, needDeltIndex);
                        minq=q.get(i);
                    }
                }
            }
            for (int i = 1; i < cur.getFirst().size()-1; i++) {//столбец
                //если максимизировать то ищем столбцы с отрицательной дельтой
                if(cur.getLast().get(i).startsWith("-")&&minMax.getValue().equals("Максимизировать")){
                    String maxValue = String.valueOf(Integer.MIN_VALUE);
                    int maxValueIndex=0;
                    //в этом столбце максимальное значение
                    for (int j = 0; j < cur.size()-1; j++) {
                        int compare = Calculations.compare(cur.get(j).get(i), maxValue);
                        if(compare>0){
                            maxValue=cur.get(j).get(i);
                            maxValueIndex=j;
                        }
                    }
                    selectableCellsBasis.add(new Pair<>(maxValueIndex, i));
                    //с положительной дельтой
                } else if (!cur.getLast().get(i).startsWith("-")&&!cur.getLast().get(i).equals("0")&& minMax.getValue().equals("Минимизировать")) {
                    String maxValue = String.valueOf(Integer.MIN_VALUE);
                    int maxValueIndex=0;
                    for (int j = 0; j < cur.size()-1; j++) {
                        int compare = Calculations.compare(cur.get(j).get(i), maxValue);
                        if(compare>0){
                            maxValue=cur.get(j).get(i);
                            maxValueIndex=j;
                        }
                    }
                    selectableCellsBasis.add(new Pair<>(maxValueIndex, i));
                }
            }
        }
    }
    private void moreOptimalBasis(){
        if(calculationStopped)return;
        stepsBasis.add(deepCopyTable(stepsBasis.getLast()));
        ObservableList<ObservableList<String>> cur = stepsBasis.getLast();
        String selectedCellValue = cur.get(selectedCellBasis.getKey()).get(selectedCellBasis.getValue());
        cur.get(selectedCellBasis.getKey()).set(0, "x"+(selectedCellBasis.getValue()));//значение базиса в первом столбце
        //делим разрешающую строку на разрешающий элемент
        for (int i = 1; i < cur.getFirst().size(); i++) {//столбец
            cur.get(selectedCellBasis.getKey()).set(i, calculations.divide(cur.get(selectedCellBasis.getKey()).get(i), selectedCellValue));
        }
        //вычитаем из остальных строк разрешающую строку
        for (int i = 0; i < cur.size()-1; i++) {//строка
            if (i!= selectedCellBasis.getKey()){
                String valueForMultiply = cur.get(i).get(selectedCellBasis.getValue());
                for (int j = 1; j < cur.getFirst().size(); j++) {//столбец
                    cur.get(i).set(j, calculations.minus(cur.get(i).get(j), calculations.multiply(cur.get(selectedCellBasis.getKey()).get(j), valueForMultiply)));
                }
            }
        }
        //вычисляем новые дельты
        cur.removeLast();//удаляет с текущего шага дельты
        addDeltsBasis();//добавляет в steps новый шаг с обновлёнными дельтами
        stepsBasis.remove(stepsBasis.size()-2);//удаляет текущий шаг без дельт
    }
}