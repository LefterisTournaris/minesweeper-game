package com.example;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Minesweeper extends Application {

    private int[][] board; // the game board
    private int rows; // number of rows in the game board
    private int cols; // number of columns in the game board
    private int mines; // number of mines in the game board
    private int remainingMines = mines; // number of remaining mines
    private int revealedCells = 0; // number of cells revealed by the player
    private boolean gameOver = true; // game over flag
    private boolean[][] revealed; // stores the state of each cell (revealed or not)
    private boolean[][] flagged; // stores the state of each cell (flagged or not)
    private GridPane grid;
    private Timeline timeline;
    private int gameTime; // game time in seconds
    private int currentGameTime = gameTime; // current game time in seconds
    private final Label timeLabel = new Label("Time left: N/A"); // label to display the game time
    private final Label totalMinesLabel = new Label("Total Mines: 0"); // label to display the number of flagged mines
    private final Label flaggedCountLabel = new Label("Flagged: 0"); // label to display the number of flagged mines
    private String scenarioId;
    private final Queue<RoundData> roundDataQueue = new LinkedList<>(); // define a queue to store the last 5 game data
    private int clickCounter = 0;
    private Pair<Integer, Integer> isSuperMine;
    private boolean hasSuperMine = false;

    @Override
    public void start(Stage primaryStage) {
        readRoundDataFromFile();

        // Create the game board
        board = new int[rows][cols];
        revealed = new boolean[rows][cols];
        flagged = new boolean[rows][cols];

        // Create the game UI
        grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setDisable(true);

        // Add the remaining mines label and the game time label to the UI
        totalMinesLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        flaggedCountLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        timeLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 14));

        // Create the root pane
        HBox labelBox = new HBox();
        labelBox.getChildren().addAll(totalMinesLabel, flaggedCountLabel, timeLabel);
        labelBox.setAlignment(Pos.TOP_LEFT);
        labelBox.setSpacing(10);

        // Create a new HBox to contain the buttons
        HBox topBox = new HBox();
        topBox.setSpacing(10); // Set the spacing between buttons

        // Create menu bar
        MenuBar menuBar = new MenuBar();

        // Create Application menu
        Menu applicationMenu = new Menu("Application");

        // Create menu items
        MenuItem createMenuItem = new MenuItem("Create");
        MenuItem loadMenuItem = new MenuItem("Load");
        MenuItem startMenuItem = new MenuItem("Start");
        MenuItem resetMenuItem = new MenuItem("Reset");
        MenuItem exitMenuItem = new MenuItem("Exit");

        // Add menu items to Application menu
        applicationMenu.getItems().addAll(createMenuItem, loadMenuItem, startMenuItem, resetMenuItem, exitMenuItem);

        // Create Details menu
        Menu detailsMenu = new Menu("Details");

        // Create menu items
        MenuItem roundsMenuItem = new MenuItem("Rounds");
        MenuItem solutionMenuItem = new MenuItem("Solution");

        // Add menu items to Details menu
        detailsMenu.getItems().addAll(roundsMenuItem, solutionMenuItem);

        // Add menus to menu bar
        menuBar.getMenus().addAll(applicationMenu, detailsMenu);

        createMenuItem.setOnAction(e -> createMenuItemFunction());

        // Set the action for the load button, show a dialog to load an existing scenario
        loadMenuItem.setOnAction(e -> loadMenuItemFunction(primaryStage));

        // Set the action for the start button, start the timer
        startMenuItem.setOnAction(e -> {
            if (!gameOver) {
                timeline.play();
                grid.setDisable(false);
            }
        });

        // Reset the board
        resetMenuItem.setOnAction(e -> {
            try {
                resetBoard();
            } catch (IOException | InvalidValueException | InvalidDescriptionException ex) {
                throw new RuntimeException(ex);
            }
        });

        exitMenuItem.setOnAction(e -> {
            writeRoundDataToFile();

            primaryStage.close();
        });

        roundsMenuItem.setOnAction(event -> roundMenuItemFunction());

        solutionMenuItem.setOnAction(e -> solutionMenuItemFunction());

        // Add all the containers together
        VBox root = new VBox();
        root.getChildren().addAll(menuBar, topBox, labelBox, grid);

        // Create the scene and show the stage
        Scene scene = new Scene(root, 500, 500);
        scene.getStylesheets().add("minesweeper.css");
        primaryStage.setTitle("Medialab Minesweeper");
        primaryStage.setScene(scene);
        primaryStage.show();
        // implements the same function as the exit button when the "X" window button is pressed
        primaryStage.setOnCloseRequest(e -> writeRoundDataToFile());
    }

    private void readRoundDataFromFile() {
        try {
            File file = new File("roundData.txt");
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] values = line.split(" ");

                int totalMines = Integer.parseInt(values[0]);
                int totalClicks = Integer.parseInt(values[1]);
                int gameTime = Integer.parseInt(values[2]);
                boolean userWon = Boolean.parseBoolean(values[3]);

                RoundData roundData = new RoundData(totalMines, totalClicks, gameTime, userWon);
                addToQueue(roundData);
            }

            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + e.getMessage());
        }
    }

    private void writeRoundDataToFile() {
        try {
            FileWriter fileWriter = new FileWriter("roundData.txt");
            PrintWriter printWriter = new PrintWriter(fileWriter);
            while (!roundDataQueue.isEmpty()) {
                RoundData roundData = roundDataQueue.poll();
                String line = roundData.totalMines + " " + roundData.totalClicks + " "
                        + roundData.gameTime + " " + roundData.userWon;
                printWriter.println(line);
            }

            printWriter.close();
        } catch (IOException ex) {
            System.out.println("Error writing to file: " + ex.getMessage());
        }
    }

    private static int[] readScenario(String scenarioId) throws IOException, InvalidValueException, InvalidDescriptionException {
        int[] scenario = new int[4];
        File file = new File("medialab/" + scenarioId + ".txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        // read level, number of mines, and game time from file
        for (int i = 0; i < 4; i++) {
            line = br.readLine();
            if (line == null) {
                throw new InvalidDescriptionException("Scenario file is invalid.");
            }
            scenario[i] = Integer.parseInt(line.trim());
        }

        // validate scenario parameters
        if (scenario[0] != 1 && scenario[0] != 2) {
            throw new InvalidValueException("Invalid level specified in scenario file.");
        }
        if ((scenario[0] == 1 && (scenario[1] < 9 || scenario[1] > 11 || scenario[2] < 120 || scenario[2] > 180))
                || (scenario[0] == 2 && (scenario[1] < 35 || scenario[1] > 45 || scenario[2] < 240 || scenario[2] > 360))
                || (scenario[0] == 1 && (scenario[3] != 0)) || (scenario[0] == 2 && (scenario[3] < 0) || (scenario[3] > 1))) {
            throw new InvalidValueException("Invalid scenario parameters specified in scenario file.");
        }
        return scenario;
    }

    private void createMenuItemFunction() {
        // Create a GridPane to display the prompts and text fields
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20));

        // Add the prompts and text fields to the GridPane
        TextField scenarioIdTextField = new TextField();
        TextField levelTextField = new TextField();
        TextField numMinesTextField = new TextField();
        TextField gameTimeTextField = new TextField();
        TextField superMineTextField = new TextField();
        gridPane.add(new Label("Scenario ID(e.g. 1,kk,a2):"), 0, 0);
        gridPane.add(scenarioIdTextField, 1, 0);
        gridPane.add(new Label("Level (1 for Beginner, 2 for Intermediate):"), 0, 1);
        gridPane.add(levelTextField, 1, 1);
        gridPane.add(new Label("Number of Mines(Lvl1: 9-11, Lvl2:35-45):"), 0, 2);
        gridPane.add(numMinesTextField, 1, 2);
        gridPane.add(new Label("Game Time(Lvl1: 120-180, Lvl2:240-360):"), 0, 3);
        gridPane.add(gameTimeTextField, 1, 3);
        gridPane.add(new Label("Super Mine?(1 for yes, 0 for no):"), 0, 4);
        gridPane.add(superMineTextField, 1, 4);

        // Create a dialog to display the GridPane
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Scenario");
        dialog.getDialogPane().setContent(gridPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Show the dialog and wait for the user to input data
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String scenarioId = scenarioIdTextField.getText();
            String level = levelTextField.getText();
            String numMines = numMinesTextField.getText();
            String gameTime = gameTimeTextField.getText();
            String superMine = superMineTextField.getText();
            try {
                int levelNum = Integer.parseInt(level);
                int minesNum = Integer.parseInt(numMines);
                int timeNum = Integer.parseInt(gameTime);
                int hasSuperMine = Integer.parseInt(superMine);
                if (levelNum < 1 || levelNum > 2) {
                    alert("Invalid level.");
                    return;
                }
                if (hasSuperMine < 0 || hasSuperMine > 1) {
                    alert("Super Mine field must be 0 or 1.");
                    return;
                }
                if (levelNum == 1) {
                    if (minesNum < 9 || minesNum > 11) {
                        alert("Invalid number of mines.");
                        return;
                    }
                    if (timeNum < 120 || timeNum > 180) {
                        alert("Invalid game time.");
                        return;
                    }
                    if (hasSuperMine == 1) {
                        alert("Level 1 can't have a super mine.");
                        return;
                    }
                } else {
                    if (minesNum < 35 || minesNum > 45) {
                        alert("Invalid number of mines.");
                        return;
                    }
                    if (timeNum < 240 || timeNum > 360) {
                        alert("Invalid game time.");
                        return;
                    }
                }
                String fileName = "SCENARIO-" + scenarioId + ".txt";
                File file = new File(fileName);
                if (file.exists()) {
                    alert("Scenario ID already exists.");
                    return;
                }
                String[] lines = {level, numMines, gameTime, superMine};
                Files.write(Paths.get(fileName), Arrays.asList(lines));
            } catch (NumberFormatException ex) {
                alert("Invalid input.");
            } catch (IOException ex) {
                alert("Error writing to file.");
            }
        }
    }

    private void loadMenuItemFunction(Stage primaryStage) {
        // Create a GridPane to display the prompt and text field
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20));

        // Add the prompt and text field to the GridPane
        TextField scenarioIdTextField = new TextField();
        gridPane.add(new Label("Scenario ID:"), 0, 0);
        gridPane.add(scenarioIdTextField, 1, 0);

        // Create a dialog to display the GridPane
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Load Scenario");
        dialog.getDialogPane().setContent(gridPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Show the dialog and wait for the user to input data
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            scenarioId = scenarioIdTextField.getText();
            try {
                if (!gameOver) grid.setDisable(true);
                int[] scenario = readScenario("SCENARIO-" + scenarioId);
                if (scenario[0] == 1) {
                    rows = 9;
                    cols = 9;
                }
                if (scenario[0] == 2) {
                    rows = 16;
                    cols = 16;
                }
                gameOver = false;
                clickCounter = 0;
                mines = scenario[1];
                currentGameTime = gameTime = scenario[2];
                hasSuperMine = (scenario[3] == 1);
                // set the game board and labels based on the loaded scenario
                board = new int[rows][cols];
                revealed = new boolean[rows][cols];
                flagged = new boolean[rows][cols];
                remainingMines = mines;
                totalMinesLabel.setText("Total Mines: " + mines);
                flaggedCountLabel.setText("Flagged: 0");
                timeLabel.setText("Time: N/A");
                initializeButtons();
                initializeBoard();
                initializeTimeline();
                // custom size, probably not the best solution
                primaryStage.setHeight(rows * 41 + 105);
                primaryStage.setWidth(cols * 41 + 40);
            } catch (IOException | InvalidValueException | InvalidDescriptionException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Error loading saved game scenario");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void roundMenuItemFunction() {
        // create a popup window to show the round data
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Last 5 Rounds");
        popup.setResizable(false);

        // create a VBox to hold the round data
        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        vbox.setSpacing(10);

        // use a stack to reverse the order of elements in the queue
        Stack<RoundData> stack = new Stack<>();
        stack.addAll(roundDataQueue);

        // iterate over the last 5 game data in the queue
        int count = 0;
        while (!stack.isEmpty() && count < 5) {
            RoundData roundData = stack.pop();
            // create a label to display the round data as a string
            Label label = new Label((count + 1) + ") " + roundData.toString());
            label.setAlignment(Pos.CENTER);
            vbox.getChildren().add(label);
            count++;
        }

        // set a fixed size for the popup window
        popup.setMinWidth(400);
        popup.setMinHeight(300);

        // create a button to close the popup window
        Button closeButton = new Button("Close");
        closeButton.setAlignment(Pos.BOTTOM_CENTER);
        closeButton.setOnAction(closeEvent -> popup.close());

        // add the VBox and the close button to the popup window
        vbox.getChildren().add(closeButton);
        Scene popupScene = new Scene(vbox);
        popup.setScene(popupScene);
        popup.showAndWait();
    }

    private void solutionMenuItemFunction() {
        if (gameOver) return;
        // End the game
        timeline.stop();
        gameOver = true;
        grid.setDisable(true);
        RoundData roundData = new RoundData(mines, clickCounter, gameTime, false);
        addToQueue(roundData);
        // Loop through all the cells on the board
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                // If the cell contains a mine, mark it as revealed
                if (board[row][col] == -1) {
                    Button button = (Button) getNodeFromGridPane(row, col);
                    assert button != null;
                    button.getStyleClass().removeAll();
                    button.setText("");
                    button.getStyleClass().remove("hidden");
                    button.getStyleClass().add("solution");
                }
            }
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText("Solution.");
        alert.setContentText("You forfeited the game to see the mine positions.");
        alert.showAndWait();
    }

    private void alert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Recursive function to reveal a cell and its adjacent cells
    private void reveal(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols || revealed[row][col]) {
            return;
        }
        revealed[row][col] = true;
        revealedCells++;
        Button button = (Button) getNodeFromGridPane(row, col);
        if (flagged[row][col]) {
            flagged[row][col] = false;
            assert button != null;
            button.getStyleClass().remove("flag");
            button.getStyleClass().add("revealed");
            remainingMines++;
            flaggedCountLabel.setText("Flagged: " + (mines - remainingMines));
        }
        assert button != null;
        button.getStyleClass().remove("hidden");
        button.getStyleClass().add("revealed");
        if (board[row][col] == 0) {
            reveal(row - 1, col - 1);
            reveal(row - 1, col);
            reveal(row - 1, col + 1);
            reveal(row, col - 1);
            reveal(row, col + 1);
            reveal(row + 1, col - 1);
            reveal(row + 1, col);
            reveal(row + 1, col + 1);
        } else {
            button.setText(Integer.toString(board[row][col]));
            switch (board[row][col]) {
                case 1 -> button.setStyle("-fx-text-fill: blue;");
                case 2 -> button.setStyle("-fx-text-fill: green;");
                case 3 -> button.setStyle("-fx-text-fill: red;");
                case 4 -> button.setStyle("-fx-text-fill: darkblue;");
                case 5 -> button.setStyle("-fx-text-fill: darkred;");
                case 6 -> button.setStyle("-fx-text-fill: teal;");
                case 7 -> button.setStyle("-fx-text-fill: black;");
                case 8 -> button.setStyle("-fx-text-fill: gray;");
            }
        }
    }

    // Check if the player has won the game
    private void checkWin() {
        if (revealedCells == (rows * cols - mines)) {
            timeline.stop();
            gameOver = true;
            grid.setDisable(true);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("YOU WIN!");
            alert.setHeaderText("You won in " + (gameTime - currentGameTime) + " seconds!");
            alert.showAndWait();
            RoundData roundData = new RoundData(mines, clickCounter, gameTime, true);
            addToQueue(roundData);
        }
    }

    // Get a node from the grid pane using its row and column index
    private Node getNodeFromGridPane(int row, int col) {
        for (Node node : grid.getChildren()) {
            if (GridPane.getRowIndex(node) == row && GridPane.getColumnIndex(node) == col) {
                return node;
            }
        }
        return null;
    }

    private void resetBoard() throws IOException, InvalidValueException, InvalidDescriptionException {
        if (scenarioId == null) return;
        // reset all cells to their initial state
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                revealed[row][col] = false;
                flagged[row][col] = false;
                Button button = (Button) getNodeFromGridPane(row, col);
                assert button != null;
                // removes all css from the buttons
                button.getStyleClass().removeAll("flag", "mine", "revealed", "hidden", "solution");
                button.setStyle("-fx-background-color: #AFAFAFAF;");
                button.setText("");
                button.getStyleClass().add("hidden");
            }
        }

        // Place the mines randomly on the board
        initializeBoard();

        // reset game state variables
        gameOver = false;
        clickCounter = 0;
        mines = readScenario("SCENARIO-" + scenarioId)[1];
        remainingMines = mines;
        revealedCells = 0;
        currentGameTime = gameTime = readScenario("SCENARIO-" + scenarioId)[2];
        grid.setDisable(true);
        // update Labels
        totalMinesLabel.setText("Total Mines: " + mines);
        flaggedCountLabel.setText("Flagged: 0");
        timeLabel.setText("Time left: N/A");

        // Create a timeline to update the game time every second
        initializeTimeline();
    }

    private void initializeTimeline() {
        // Remove the old timeline
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        currentGameTime = gameTime;
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            currentGameTime--;
            timeLabel.setText("Time left: " + currentGameTime);
            if (currentGameTime == 0) {
                gameOver = true;
                grid.setDisable(true);
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Game Over");
                    alert.setHeaderText("Time's up!");
                    alert.setContentText("You lost the game because the time is up.");
                    alert.showAndWait();
                    RoundData roundData = new RoundData(mines, clickCounter, gameTime, false);
                    addToQueue(roundData);
                });
            }
        }));
        timeline.setCycleCount(gameTime);
    }

    private void initializeBoard() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                board[i][j] = 0; // Initialize all values to 0.
            }
        }

        int x1 = (int) (Math.random() * rows);
        int y1 = (int) (Math.random() * cols);
        board[x1][y1] = -1;
        // Initialize super mine
        if (hasSuperMine) isSuperMine = new Pair<>(x1, y1);

        // Place the rest of the mines randomly on the board
        int count = 0;
        while (count < mines - 1) {
            int x = (int) (Math.random() * rows);
            int y = (int) (Math.random() * cols);
            if (board[x][y] != -1) {
                board[x][y] = -1;
                count++;
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter("mines.txt"))) {
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    if (board[row][col] == -1) {
                        if (isSuperMine != null && isSuperMine.getKey() == row && isSuperMine.getValue() == col) {
                            writer.println(row + ", " + col + ", " + 1);
                            continue;
                        }
                        writer.println(row + ", " + col + ", " + 0);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }


        // Calculate the number of adjacent mines for each cell
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] != -1) {
                    int countMines = 0;
                    if (i > 0 && j > 0 && board[i - 1][j - 1] == -1) {
                        countMines++;
                    }
                    if (i > 0 && board[i - 1][j] == -1) {
                        countMines++;
                    }
                    if (i > 0 && j < cols - 1 && board[i - 1][j + 1] == -1) {
                        countMines++;
                    }
                    if (j > 0 && board[i][j - 1] == -1) {
                        countMines++;
                    }
                    if (j < cols - 1 && board[i][j + 1] == -1) {
                        countMines++;
                    }
                    if (i < rows - 1 && j > 0 && board[i + 1][j - 1] == -1) {
                        countMines++;
                    }
                    if (i < rows - 1 && board[i + 1][j] == -1) {
                        countMines++;
                    }
                    if (i < rows - 1 && j < cols - 1 && board[i + 1][j + 1] == -1) {
                        countMines++;
                    }
                    board[i][j] = countMines;
                }
            }
        }
    }

    private void initializeButtons() {
        grid.getChildren().clear();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Button button = new Button();
                button.setPrefSize(40, 40);
                button.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
                button.getStyleClass().removeAll();
                button.setText("");
                button.getStyleClass().add("hidden");
                // User left click
                button.setOnAction(e -> {
                    if (!gameOver) {
                        int row = GridPane.getRowIndex(button);
                        int col = GridPane.getColumnIndex(button);
                        if (revealed[row][col]) return;
                        if (flagged[row][col]) { // if cell is flagged
                            flagged[row][col] = false;
                            button.getStyleClass().remove("flag");
                            button.getStyleClass().add("hidden");
                            remainingMines++;
                            flaggedCountLabel.setText("Flagged: " + (mines - remainingMines));
                        } else if (board[row][col] == -1) { // if cell is a mine
                            // if mine is the super mine
                            if (clickCounter < 4 && isSuperMine != null && isSuperMine.getKey() == row && isSuperMine.getValue() == col) {
                                Button button2;
                                revealed[row][col] = true;
                                for (int k = 0; k < rows; k++) {
                                    button2 = (Button) getNodeFromGridPane(k, col);
                                    assert button2 != null;
                                    if (board[k][col] != -1) {
                                        reveal(k, col);
                                        button2.setStyle(button2.getStyle() + "-fx-background-color: rgba(23,140,0,0.35);");
                                    } else {
                                        button2.getStyleClass().remove("hidden");
                                        button2.getStyleClass().add("mine");
                                        button2.setStyle("-fx-background-color: #fffc00;");
                                        revealed[k][col] = true;
                                    }
                                    button.getStyleClass().remove("hidden");
                                    button.getStyleClass().add("mine");
                                    button.setStyle("-fx-background-color: #169300;");
                                }
                                for (int l = 0; l < cols; l++) {
                                    button2 = (Button) getNodeFromGridPane(row, l);
                                    assert button2 != null;
                                    if (board[row][l] != -1) {
                                        reveal(row, l);
                                        button2.setStyle(button2.getStyle() + "-fx-background-color: rgba(23,140,0,0.35);");
                                    } else {
                                        button2.getStyleClass().remove("hidden");
                                        button2.getStyleClass().add("mine");
                                        button2.setStyle("-fx-background-color: #fffc00;");
                                        revealed[row][l] = true;
                                    }
                                    button.getStyleClass().remove("hidden");
                                    button.getStyleClass().add("mine");
                                    button.setStyle("-fx-background-color: #169300;");
                                }
                                flaggedCountLabel.setText("Flagged: " + (mines - remainingMines));
                            } else {
                                timeline.stop();
                                button.getStyleClass().removeAll("hidden", "revealed");
                                button.setStyle("-fx-background-color: #8f0000;");
                                button.getStyleClass().add("mine");
                                gameOver = true;
                                grid.setDisable(true);
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Game Over");
                                alert.setHeaderText("You clicked on a mine!");
                                alert.setContentText("You lost the game because you clicked on a mine.");
                                alert.showAndWait();
                                RoundData roundData = new RoundData(mines, clickCounter, gameTime, false);
                                addToQueue(roundData);
                            }
                        } else {
                            clickCounter++;
                            reveal(row, col);
                            checkWin();
                        }
                    }
                });
                // User right click
                button.setOnMouseClicked(e -> {
                    if (!gameOver && e.getButton().toString().equals("SECONDARY")) {
                        int row = GridPane.getRowIndex(button);
                        int col = GridPane.getColumnIndex(button);
                        if (!revealed[row][col]) {
                            if (flagged[row][col]) {
                                flagged[row][col] = false;
                                button.getStyleClass().remove("flag");
                                button.getStyleClass().add("hidden");
                                remainingMines++;
                                flaggedCountLabel.setText("Flagged: " + (mines - remainingMines));
                            } else {
                                flagged[row][col] = true;
                                button.getStyleClass().remove("hidden");
                                button.getStyleClass().add("flag");
                                remainingMines--;
                                flaggedCountLabel.setText("Flagged: " + (mines - remainingMines));
                            }
                        }
                    }
                });
                GridPane.setRowIndex(button, i);
                GridPane.setColumnIndex(button, j);
                grid.getChildren().add(button);
            }
        }
    }

    // Add data to the queue
    private void addToQueue(RoundData roundData) {
        roundDataQueue.add(roundData);
        if (roundDataQueue.size() > 5) {
            roundDataQueue.poll();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}