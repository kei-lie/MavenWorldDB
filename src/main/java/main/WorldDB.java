package main;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import atlantafx.base.theme.PrimerLight;
import db.DatabaseConnection;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import operations.SelectOperation;

public class WorldDB extends Application{
	private Connection con;
	private TableView<ObservableList<String>> table = new TableView<>();

	private List<String> currentColumns = new ArrayList<>();
	private List<String> databaseView = new ArrayList<>();
	private ComboBox<String> tableSelector
	=new ComboBox<>();
	
	private Button btnSelect = new Button("Atlasīt (SELECT)");
	private Button btnInsert = new Button("Ievietot (INSERT)");
	private Button btnUpdate = new Button("Atjaunot (UPDATE)");
	private Button btnDelete = new Button("Dzēst (DELETE)");
	
	
	public static void main(String[] args) {
		launch(args);
	}
	@Override
	public void start(Stage stage) throws Exception {
		Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
		
		try {
			con = DatabaseConnection.getConnection();
			//databaseView = ViewManager.getDatabaseViews();
		}catch (Exception e) {
			showAlert("Savienojuma kļūda: ", e.getMessage());
		}
		
		tableSelector.getItems().add("--- Tabulas ---");
		tableSelector.getItems().addAll("city");		//Pašai jāpieliek pārējās tabulas
		
		if(databaseView.isEmpty()) {
			tableSelector.getItems().add("--- NAV ATRASTS NEVIENS SKATS ---");
			showAlert("Informācija", "Datubāzē pagaidām nav neviens skats");
		} else {
			tableSelector.getItems().add("--- SKATI ---");
			tableSelector.getItems().addAll(databaseView);
		}
		
		btnSelect.setOnAction(e -> loadData());
		//Šeit vēl jāturpina kopā...
		
		tableSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			boolean isSeperator = (newVal == null || newVal.startsWith("---"));
			btnSelect.setDisable(isSeperator);
			//Vēlāk jāpieliek pārējās pogas
		});
		
		tableSelector.getSelectionModel().select(1);
		HBox topBox = new HBox(10, new Label("Tabula/Skats:"), tableSelector, btnSelect);
		
		topBox.setPadding(new Insets(10));
		
		//Pēc tam jāpieliek bottomBox pārējām pogām
		
		VBox root = new VBox(10, topBox, table);
		stage.setScene(new Scene(root, 900, 600));
		stage.setTitle("World DB APP");
		stage.getIcons().add(new Image(getClass().getResourceAsStream("/icon.png")));
		stage.show();
		loadData();
	}
	
	private void loadData() {
		String selectedTable = tableSelector.getValue();
		if(selectedTable == null || selectedTable.startsWith("---")) return;
		
		try {
			currentColumns = SelectOperation.populateTable(con, selectedTable, table);
		} catch(Exception e) {
			showAlert("Kļūda atlasot datus", e.getMessage());
		}
	}
	
	private void showAlert(String title, String msg) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.showAndWait();
	}

}
